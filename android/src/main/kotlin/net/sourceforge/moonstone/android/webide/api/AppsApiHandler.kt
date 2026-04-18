package net.sourceforge.moonstone.android.webide.api

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response
import net.sourceforge.moonstone.android.service.AppDiscoveryService
import java.io.File

/**
 * Handles /api/apps endpoints for managing KleinLisp applications.
 */
class AppsApiHandler(private val appsFolder: File) {

    /**
     * GET /api/apps - List all apps
     */
    fun listApps(): Response {
        val apps = AppDiscoveryService.discoverApps(appsFolder)
        val json = buildString {
            append("[")
            apps.forEachIndexed { index, app ->
                if (index > 0) append(",")
                append("{")
                append(""""id":"${escapeJson(app.id)}",""")
                append(""""name":"${escapeJson(app.name)}"""")
                app.description?.let { append(""","description":"${escapeJson(it)}"""") }
                app.version?.let { append(""","version":"${escapeJson(it)}"""") }
                app.author?.let { append(""","author":"${escapeJson(it)}"""") }
                append(",\"hasIcon\":${app.iconPath != null}")
                append("}")
            }
            append("]")
        }
        return ApiRouter.jsonResponse(Response.Status.OK, json)
    }

    /**
     * POST /api/apps - Create a new app
     * Body: {"name": "app-name"}
     */
    fun createApp(session: NanoHTTPD.IHTTPSession): Response {
        val body = getRequestBody(session)
        val name = extractJsonString(body, "name")
            ?: return ApiRouter.jsonResponse(
                Response.Status.BAD_REQUEST,
                """{"error": "Missing 'name' field"}"""
            )

        // Sanitize app name for folder
        val folderId = name.lowercase()
            .replace(Regex("[^a-z0-9-_]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')

        if (folderId.isEmpty()) {
            return ApiRouter.jsonResponse(
                Response.Status.BAD_REQUEST,
                """{"error": "Invalid app name"}"""
            )
        }

        val appFolder = File(appsFolder, folderId)
        if (appFolder.exists()) {
            return ApiRouter.jsonResponse(
                Response.Status.CONFLICT,
                """{"error": "App already exists"}"""
            )
        }

        // Ensure apps folder exists
        if (!appsFolder.exists()) {
            appsFolder.mkdirs()
        }

        // Create app folder and default app.scm
        if (!appFolder.mkdirs()) {
            return ApiRouter.jsonResponse(
                Response.Status.INTERNAL_ERROR,
                """{"error": "Failed to create app folder"}"""
            )
        }

        val scriptFile = File(appFolder, "app.scm")
        val defaultScript = """
            |(define (app)
            |  (column #:padding 16 #:spacing 8
            |    (text #:value "$name" #:style 'headline-medium)
            |    (text #:value "Edit this file to build your app!")))
        """.trimMargin()

        scriptFile.writeText(defaultScript)

        return ApiRouter.jsonResponse(
            Response.Status.CREATED,
            """{"id": "${escapeJson(folderId)}", "name": "${escapeJson(name)}"}"""
        )
    }

    /**
     * DELETE /api/apps/{id} - Delete an app
     */
    fun deleteApp(appId: String): Response {
        val appFolder = File(appsFolder, appId)

        // Security: Verify path is within apps folder
        if (!isPathWithinAppsFolder(appFolder)) {
            return ApiRouter.jsonResponse(
                Response.Status.FORBIDDEN,
                """{"error": "Invalid app path"}"""
            )
        }

        if (!appFolder.exists() || !appFolder.isDirectory) {
            return ApiRouter.jsonResponse(
                Response.Status.NOT_FOUND,
                """{"error": "App not found"}"""
            )
        }

        // Delete folder recursively
        if (!appFolder.deleteRecursively()) {
            return ApiRouter.jsonResponse(
                Response.Status.INTERNAL_ERROR,
                """{"error": "Failed to delete app"}"""
            )
        }

        return ApiRouter.jsonResponse(Response.Status.OK, """{"success": true}""")
    }

    private fun isPathWithinAppsFolder(file: File): Boolean {
        val canonicalApps = appsFolder.canonicalPath
        val canonicalFile = file.canonicalPath
        return canonicalFile.startsWith(canonicalApps)
    }

    private fun getRequestBody(session: NanoHTTPD.IHTTPSession): String {
        // Read body directly from input stream (parseBody can be unreliable for PUT/POST)
        val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
        if (contentLength > 0) {
            try {
                val buffer = ByteArray(contentLength)
                var totalRead = 0
                while (totalRead < contentLength) {
                    val read = session.inputStream.read(buffer, totalRead, contentLength - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                return String(buffer, 0, totalRead, Charsets.UTF_8)
            } catch (e: Exception) {
                // Fall through to return empty string
            }
        }
        return ""
    }

    private fun extractJsonString(json: String, key: String): String? {
        val regex = """"$key"\s*:\s*"([^"]*)"""".toRegex()
        return regex.find(json)?.groupValues?.get(1)
    }

    private fun escapeJson(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
