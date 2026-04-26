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

    @Suppress("CyclomaticComplexMethod")
    private fun getMimeType(path: String): String =
        when {
            path.endsWith(".html") -> MIME_HTML
            path.endsWith(".css") -> MIME_CSS
            path.endsWith(".js") -> MIME_JS
            path.endsWith(".json") -> MIME_JSON
            path.endsWith(".png") -> MIME_PNG
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> MIME_JPEG
            path.endsWith(".gif") -> MIME_GIF
            path.endsWith(".svg") -> MIME_SVG
            path.endsWith(".ico") -> MIME_ICO
            path.endsWith(".woff") -> MIME_WOFF
            path.endsWith(".woff2") -> MIME_WOFF2
            path.endsWith(".ttf") -> MIME_TTF
            else -> MIME_OCTET_STREAM
        }

    companion object {
        private const val TAG = "WebIdeServer"
        private const val WEBIDE_ASSETS_PATH = "webide"

        private const val MIME_HTML = "text/html"
        private const val MIME_CSS = "text/css"
        private const val MIME_JS = "application/javascript"
        private const val MIME_JSON = "application/json"
        private const val MIME_PNG = "image/png"
        private const val MIME_JPEG = "image/jpeg"
        private const val MIME_GIF = "image/gif"
        private const val MIME_SVG = "image/svg+xml"
        private const val MIME_ICO = "image/x-icon"
        private const val MIME_WOFF = "font/woff"
        private const val MIME_WOFF2 = "font/woff2"
        private const val MIME_TTF = "font/ttf"
        private const val MIME_OCTET_STREAM = "application/octet-stream"
    }
}
