package net.sourceforge.moonstone.android.webide

import android.content.Context
import android.util.Log
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Singleton manager for the Web IDE server lifecycle.
 * Ensures only one server instance runs at a time.
 */
object WebIdeServerManager {
    private const val TAG = "WebIdeServerManager"

    private var server: WebIdeServer? = null
    private var currentPort: Int = 0

    val isRunning: Boolean
        get() = server?.isAlive == true

    val serverUrl: String?
        get() {
            if (!isRunning) return null
            val ip = getLocalIpAddress() ?: return null
            return "http://$ip:$currentPort"
        }

    /**
     * Start the Web IDE server.
     *
     * @param context Android context for asset access
     * @param appsFolder The folder containing KleinLisp apps
     * @param port The port to listen on
     * @return true if server started successfully
     */
    @Synchronized
    fun start(
        context: Context,
        appsFolder: File,
        port: Int,
    ): Boolean {
        if (isRunning) {
            if (currentPort == port) {
                Log.d(TAG, "Server already running on port $port")
                return true
            }
            // Port changed, restart
            stop()
        }

        return try {
            server = WebIdeServer(context.applicationContext, appsFolder, port)
            server?.start()
            currentPort = port
            Log.i(TAG, "Server started on port $port")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server on port $port", e)
            server = null
            false
        }
    }

    /**
     * Stop the Web IDE server.
     */
    @Synchronized
    fun stop() {
        server?.stop()
        server = null
        currentPort = 0
        Log.i(TAG, "Server stopped")
    }

    /**
     * Get the local IP address of the device, preferring WiFi interface.
     */
    fun getLocalIpAddress(): String? {
        try {
            var fallbackAddress: String? = null
            val interfaces = NetworkInterface.getNetworkInterfaces()

            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                val interfaceName = networkInterface.name.lowercase()

                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val hostAddress = address.hostAddress

                        // Prefer WiFi interfaces (wlan0, wlan1, etc.)
                        if (interfaceName.startsWith("wlan")) {
                            return hostAddress
                        }

                        // Store first valid address as fallback
                        if (fallbackAddress == null) {
                            fallbackAddress = hostAddress
                        }
                    }
                }
            }

            return fallbackAddress
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get local IP address", e)
        }
        return null
    }
}
