package net.sourceforge.moonstone.android.webide.api

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response
import java.io.File

/**
 * Routes API requests to appropriate handlers.
 */
class ApiRouter(
    private val appsFolder: File,
) {
    private val appsHandler = AppsApiHandler(appsFolder)
    private val filesHandler = FilesApiHandler(appsFolder)

    fun route(session: NanoHTTPD.IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        return when {
            uri == API_STATUS -> handleStatus()
            else -> routeApiEndpoint(uri, method, session)
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun routeApiEndpoint(
        uri: String,
        method: NanoHTTPD.Method,
        session: NanoHTTPD.IHTTPSession,
    ): Response =
        when {
            uri == API_APPS && method == NanoHTTPD.Method.GET -> appsHandler.listApps()
            uri == API_APPS && method == NanoHTTPD.Method.POST -> appsHandler.createApp(session)
            uri.matches(REGEX_APP_ID) && method == NanoHTTPD.Method.DELETE -> {
                appsHandler.deleteApp(uri.substringAfterLast("/"))
            }
            uri.matches(REGEX_APP_FILES) && method == NanoHTTPD.Method.GET -> {
                filesHandler.listFiles(extractAppId(uri))
            }
            uri.matches(REGEX_APP_FILES) && method == NanoHTTPD.Method.POST -> {
                filesHandler.createFile(extractAppId(uri), session)
            }
            uri.matches(REGEX_APP_FILE_PATH) && method == NanoHTTPD.Method.GET -> {
                val (appId, filePath) = extractAppIdAndPath(uri)
                filesHandler.readFile(appId, filePath)
            }
            uri.matches(REGEX_APP_FILE_PATH) && method == NanoHTTPD.Method.PUT -> {
                val (appId, filePath) = extractAppIdAndPath(uri)
                filesHandler.writeFile(appId, filePath, session)
            }
            uri.matches(REGEX_APP_FILE_PATH) && method == NanoHTTPD.Method.DELETE -> {
                val (appId, filePath) = extractAppIdAndPath(uri)
                filesHandler.deleteFile(appId, filePath)
            }
            else -> jsonResponse(Response.Status.NOT_FOUND, ERROR_NOT_FOUND)
        }

    private fun handleStatus(): Response = jsonResponse(Response.Status.OK, """{"status": "ok", "version": "1.0"}""")

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
        private const val API_STATUS = "/api/status"
        private const val API_APPS = "/api/apps"
        private val REGEX_APP_ID = Regex("/api/apps/[^/]+")
        private val REGEX_APP_FILES = Regex("/api/apps/[^/]+/files")
        private val REGEX_APP_FILE_PATH = Regex("/api/apps/[^/]+/files/.+")
        private const val ERROR_NOT_FOUND = """{"error": "Not Found"}"""

        fun jsonResponse(
            status: Response.Status,
            json: String,
        ): Response = NanoHTTPD.newFixedLengthResponse(status, "application/json", json)
    }
}
