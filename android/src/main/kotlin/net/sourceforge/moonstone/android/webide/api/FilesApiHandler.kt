package net.sourceforge.moonstone.android.webide.api

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response
import java.io.File

/**
 * Handles /api/apps/{id}/files endpoints for file operations.
 */
class FilesApiHandler(
    private val appsFolder: File,
) {
    /**
     * GET /api/apps/{id}/files - List files in an app
     */
    fun listFiles(appId: String): Response {
        val appFolder = File(appsFolder, appId)

        if (!isPathWithinAppsFolder(appFolder)) {
            return ApiRouter.jsonResponse(
                Response.Status.FORBIDDEN,
                ERROR_INVALID_PATH,
            )
        }

        if (!appFolder.exists() || !appFolder.isDirectory) {
            return ApiRouter.jsonResponse(
                Response.Status.NOT_FOUND,
                ERROR_APP_NOT_FOUND,
            )
        }

        val files = listFilesRecursively(appFolder, "")
        val json =
            buildString {
                append("[")
                files.forEachIndexed { index, file ->
                    if (index > 0) append(",")
                    append("{")
                    append(""""name":"${escapeJson(file.name)}",""")
                    append(""""path":"${escapeJson(file.path)}",""")
                    append(""""isDirectory":${file.isDirectory}""")
                    if (!file.isDirectory) {
                        append(""","size":${file.size}""")
                    }
                    append("}")
                }
                append("]")
            }
        return ApiRouter.jsonResponse(Response.Status.OK, json)
    }

    /**
     * GET /api/apps/{id}/files/{path} - Read file content
     */
    fun readFile(
        appId: String,
        filePath: String,
    ): Response {
        val appFolder = File(appsFolder, appId)
        val file = File(appFolder, filePath)

        if (!isPathWithinAppsFolder(file)) {
            return ApiRouter.jsonResponse(
                Response.Status.FORBIDDEN,
                ERROR_INVALID_PATH,
            )
        }

        if (!file.exists() || !file.isFile) {
            return ApiRouter.jsonResponse(
                Response.Status.NOT_FOUND,
                """{"error": "File not found"}""",
            )
        }

        val content = file.readText()
        val json = """{"content": "${escapeJson(content)}"}"""
        return ApiRouter.jsonResponse(Response.Status.OK, json)
    }

    /**
     * PUT /api/apps/{id}/files/{path} - Write file content
     * Body: {"content": "file contents"}
     */
    fun writeFile(
        appId: String,
        filePath: String,
        session: NanoHTTPD.IHTTPSession,
    ): Response {
        val appFolder = File(appsFolder, appId)
        val file = File(appFolder, filePath)

        if (!isPathWithinAppsFolder(file)) {
            return ApiRouter.jsonResponse(
                Response.Status.FORBIDDEN,
                ERROR_INVALID_PATH,
            )
        }

        if (!appFolder.exists()) {
            return ApiRouter.jsonResponse(
                Response.Status.NOT_FOUND,
                ERROR_APP_NOT_FOUND,
            )
        }

        val body = getRequestBody(session)
        val content =
            extractJsonContent(body)
                ?: return ApiRouter.jsonResponse(
                    Response.Status.BAD_REQUEST,
                    """{"error": "Missing 'content' field"}""",
                )

        // Ensure parent directories exist
        file.parentFile?.mkdirs()

        file.writeText(content)
        return ApiRouter.jsonResponse(Response.Status.OK, """{"success": true}""")
    }

    /**
     * POST /api/apps/{id}/files - Create a new file
     * Body: {"name": "filename.scm", "content": "optional initial content"}
     */
    fun createFile(
        appId: String,
        session: NanoHTTPD.IHTTPSession,
    ): Response {
        val appFolder = File(appsFolder, appId)

        if (!isPathWithinAppsFolder(appFolder)) {
            return ApiRouter.jsonResponse(
                Response.Status.FORBIDDEN,
                ERROR_INVALID_PATH,
            )
        }

        if (!appFolder.exists()) {
            return ApiRouter.jsonResponse(
                Response.Status.NOT_FOUND,
                ERROR_APP_NOT_FOUND,
            )
        }

        val body = getRequestBody(session)
        val fileName =
            extractJsonString(body, "name")
                ?: return ApiRouter.jsonResponse(
                    Response.Status.BAD_REQUEST,
                    """{"error": "Missing 'name' field"}""",
                )

        // Sanitize filename
        if (fileName.contains("..") || fileName.startsWith("/")) {
            return ApiRouter.jsonResponse(
                Response.Status.BAD_REQUEST,
                """{"error": "Invalid filename"}""",
            )
        }

        val file = File(appFolder, fileName)

        if (!isPathWithinAppsFolder(file)) {
            return ApiRouter.jsonResponse(
                Response.Status.FORBIDDEN,
                """{"error": "Invalid path"}""",
            )
        }

        if (file.exists()) {
            return ApiRouter.jsonResponse(
                Response.Status.CONFLICT,
                """{"error": "File already exists"}""",
            )
        }

        // Ensure parent directories exist
        file.parentFile?.mkdirs()

        val content = extractJsonContent(body) ?: ""
        file.writeText(content)

        return ApiRouter.jsonResponse(
            Response.Status.CREATED,
            """{"name": "${escapeJson(fileName)}", "path": "${escapeJson(fileName)}"}""",
        )
    }

    /**
     * DELETE /api/apps/{id}/files/{path} - Delete a file
     */
    fun deleteFile(
        appId: String,
        filePath: String,
    ): Response {
        val appFolder = File(appsFolder, appId)
        val file = File(appFolder, filePath)

        if (!isPathWithinAppsFolder(file)) {
            return ApiRouter.jsonResponse(
                Response.Status.FORBIDDEN,
                ERROR_INVALID_PATH,
            )
        }

        if (!file.exists()) {
            return ApiRouter.jsonResponse(
                Response.Status.NOT_FOUND,
                """{"error": "File not found"}""",
            )
        }

        // Prevent deleting app.scm
        if (file.name == "app.scm" && file.parentFile == appFolder) {
            return ApiRouter.jsonResponse(
                Response.Status.FORBIDDEN,
                """{"error": "Cannot delete main app.scm file"}""",
            )
        }

        val deleted =
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }

        return if (deleted) {
            ApiRouter.jsonResponse(Response.Status.OK, """{"success": true}""")
        } else {
            ApiRouter.jsonResponse(
                Response.Status.INTERNAL_ERROR,
                """{"error": "Failed to delete file"}""",
            )
        }
    }

    private data class FileInfo(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long = 0,
    )

    private fun listFilesRecursively(
        folder: File,
        basePath: String,
    ): List<FileInfo> {
        val result = mutableListOf<FileInfo>()
        val files = folder.listFiles()?.sortedBy { it.name.lowercase() } ?: return result

        for (file in files) {
            val path = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"
            result.add(
                FileInfo(
                    name = file.name,
                    path = path,
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0,
                ),
            )
            if (file.isDirectory) {
                result.addAll(listFilesRecursively(file, path))
            }
        }
        return result
    }

    private fun isPathWithinAppsFolder(file: File): Boolean {
        val canonicalApps = appsFolder.canonicalPath
        val canonicalFile = file.canonicalPath
        return canonicalFile.startsWith(canonicalApps)
    }

    private fun getRequestBody(session: NanoHTTPD.IHTTPSession): String {
        val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
        if (contentLength <= 0) return ""

        return readBodyContent(session.inputStream, contentLength)
    }

    private fun readBodyContent(
        inputStream: java.io.InputStream,
        contentLength: Int,
    ): String {
        try {
            val buffer = ByteArray(contentLength)
            var totalRead = 0
            while (totalRead < contentLength) {
                val read = inputStream.read(buffer, totalRead, contentLength - totalRead)
                if (read == -1) break
                totalRead += read
            }
            return String(buffer, 0, totalRead, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading request body", e)
            return ""
        }
    }

    private fun extractJsonString(
        json: String,
        key: String,
    ): String? {
        val regex = """"$key"\s*:\s*"([^"]*)"""".toRegex()
        return regex.find(json)?.groupValues?.get(1)
    }

    private fun extractJsonContent(json: String): String? {
        // Find the "content" field value, handling escaped characters
        // Support both "content":"..." and "content": "..." (with optional space)
        val regex = """"content"\s*:\s*"""".toRegex()
        val match = regex.find(json) ?: return null

        val contentStart = match.range.last + 1
        val sb = StringBuilder()
        var i = contentStart
        while (i < json.length) {
            val c = json[i]
            if (c == '"') {
                // Check if escaped
                var backslashes = 0
                var j = i - 1
                while (j >= contentStart && json[j] == '\\') {
                    backslashes++
                    j--
                }
                if (backslashes % 2 == 0) {
                    // Unescaped quote, end of content
                    break
                }
            }
            sb.append(c)
            i++
        }

        // Unescape the content
        return sb
            .toString()
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    private fun escapeJson(str: String): String =
        str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    companion object {
        private const val TAG = "FilesApiHandler"
        private const val ERROR_INVALID_PATH = """{"error": "Invalid path"}"""
        private const val ERROR_APP_NOT_FOUND = """{"error": "App not found"}"""
    }
}
