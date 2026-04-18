package net.sourceforge.moonstone.android.webide.api

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response
import java.io.File

/**
 * Routes API requests to appropriate handlers.
 */
class ApiRouter(private val appsFolder: File) {

    private val appsHandler = AppsApiHandler(appsFolder)
    private val filesHandler = FilesApiHandler(appsFolder)

    fun route(session: NanoHTTPD.IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        return when {
            // Status endpoint
            uri == "/api/status" -> handleStatus()

            // Apps endpoints
            uri == "/api/apps" && method == NanoHTTPD.Method.GET -> appsHandler.listApps()
            uri == "/api/apps" && method == NanoHTTPD.Method.POST -> appsHandler.createApp(session)
            uri.matches(Regex("/api/apps/[^/]+")) && method == NanoHTTPD.Method.DELETE -> {
                val appId = uri.substringAfterLast("/")
                appsHandler.deleteApp(appId)
            }

            // Files endpoints
            uri.matches(Regex("/api/apps/[^/]+/files")) && method == NanoHTTPD.Method.GET -> {
                val appId = extractAppId(uri)
                filesHandler.listFiles(appId)
            }
            uri.matches(Regex("/api/apps/[^/]+/files")) && method == NanoHTTPD.Method.POST -> {
                val appId = extractAppId(uri)
                filesHandler.createFile(appId, session)
            }
            uri.matches(Regex("/api/apps/[^/]+/files/.+")) && method == NanoHTTPD.Method.GET -> {
                val (appId, filePath) = extractAppIdAndPath(uri)
                filesHandler.readFile(appId, filePath)
            }
            uri.matches(Regex("/api/apps/[^/]+/files/.+")) && method == NanoHTTPD.Method.PUT -> {
                val (appId, filePath) = extractAppIdAndPath(uri)
                filesHandler.writeFile(appId, filePath, session)
            }
            uri.matches(Regex("/api/apps/[^/]+/files/.+")) && method == NanoHTTPD.Method.DELETE -> {
                val (appId, filePath) = extractAppIdAndPath(uri)
                filesHandler.deleteFile(appId, filePath)
            }

            else -> jsonResponse(Response.Status.NOT_FOUND, """{"error": "Not Found"}""")
        }
    }

    private fun handleStatus(): Response {
        return jsonResponse(Response.Status.OK, """{"status": "ok", "version": "1.0"}""")
    }

    private fun extractAppId(uri: String): String {
        // /api/apps/{appId}/files -> extract appId
        val parts = uri.split("/")
        return parts[3] // [0]="", [1]="api", [2]="apps", [3]=appId
    }

    private fun extractAppIdAndPath(uri: String): Pair<String, String> {
        // /api/apps/{appId}/files/{path} -> extract appId and path
        val prefix = "/api/apps/"
        val withoutPrefix = uri.substring(prefix.length)
        val appId = withoutPrefix.substringBefore("/")
        val path = withoutPrefix.substringAfter("/files/")
        return Pair(appId, path)
    }

    companion object {
        fun jsonResponse(status: Response.Status, json: String): Response {
            return NanoHTTPD.newFixedLengthResponse(status, "application/json", json)
        }
    }
}
