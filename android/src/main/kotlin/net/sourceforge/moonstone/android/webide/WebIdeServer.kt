package net.sourceforge.moonstone.android.webide

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import net.sourceforge.moonstone.android.webide.api.ApiRouter
import java.io.File

/**
 * Embedded HTTP server for the Web IDE.
 * Serves the React frontend and handles API requests.
 */
class WebIdeServer(
    private val context: Context,
    private val appsFolder: File,
    port: Int,
) : NanoHTTPD(port) {
    companion object {
        private const val TAG = "WebIdeServer"
        private const val WEBIDE_ASSETS_PATH = "webide"
    }

    private val apiRouter = ApiRouter(appsFolder)

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        Log.d(TAG, "Request: $method $uri")

        return try {
            when {
                uri.startsWith("/api/") -> handleApiRequest(session)
                else -> serveStaticFile(uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling request: $uri", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT,
                "Internal Server Error: ${e.message}",
            )
        }
    }

    private fun handleApiRequest(session: IHTTPSession): Response = apiRouter.route(session)

    private fun serveStaticFile(uri: String): Response {
        val path =
            when {
                uri == "/" -> "index.html"
                uri.startsWith("/") -> uri.substring(1)
                else -> uri
            }

        val assetPath = "$WEBIDE_ASSETS_PATH/$path"

        return try {
            val inputStream = context.assets.open(assetPath)
            val mimeType = getMimeType(path)
            newChunkedResponse(Response.Status.OK, mimeType, inputStream)
        } catch (e: Exception) {
            // Try serving index.html for SPA routing
            if (!path.contains(".")) {
                try {
                    val inputStream = context.assets.open("$WEBIDE_ASSETS_PATH/index.html")
                    newChunkedResponse(Response.Status.OK, "text/html", inputStream)
                } catch (e2: Exception) {
                    newFixedLengthResponse(
                        Response.Status.NOT_FOUND,
                        MIME_PLAINTEXT,
                        "Not Found: $path",
                    )
                }
            } else {
                newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    MIME_PLAINTEXT,
                    "Not Found: $path",
                )
            }
        }
    }

    private fun getMimeType(path: String): String =
        when {
            path.endsWith(".html") -> "text/html"
            path.endsWith(".css") -> "text/css"
            path.endsWith(".js") -> "application/javascript"
            path.endsWith(".json") -> "application/json"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            path.endsWith(".gif") -> "image/gif"
            path.endsWith(".svg") -> "image/svg+xml"
            path.endsWith(".ico") -> "image/x-icon"
            path.endsWith(".woff") -> "font/woff"
            path.endsWith(".woff2") -> "font/woff2"
            path.endsWith(".ttf") -> "font/ttf"
            else -> "application/octet-stream"
        }
}
