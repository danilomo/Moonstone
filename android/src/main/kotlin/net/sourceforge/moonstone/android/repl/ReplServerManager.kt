package net.sourceforge.moonstone.android.repl

import android.util.Log
import net.sourceforge.kleinlisp.Lisp
import net.sourceforge.kleinlisp.SocketReplServer
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Singleton manager for the REPL server lifecycle.
 *
 * The REPL server allows connecting to a running app via Emacs/geiser
 * or any other Scheme REPL client that supports the geiser protocol.
 *
 * The server shares the Lisp instance with the running app, so state
 * changes made via the REPL are immediately reflected in the UI.
 */
object ReplServerManager {
    private const val TAG = "ReplServerManager"

    private var server: SocketReplServer? = null
    private var serverThread: Thread? = null
    private var currentPort: Int = 0
    private var currentLisp: Lisp? = null

    val isRunning: Boolean
        get() = server != null && serverThread?.isAlive == true

    val serverUrl: String?
        get() {
            if (!isRunning) return null
            val ip = getLocalIpAddress() ?: return null
            return "$ip:$currentPort"
        }

    /**
     * Start the REPL server with the given Lisp instance.
     *
     * @param lisp The Lisp instance to use for evaluating expressions
     * @param port The port to listen on
     * @return true if server started successfully
     */
    @Synchronized
    fun start(
        lisp: Lisp,
        port: Int,
    ): Boolean {
        if (isRunning) {
            if (currentPort == port && currentLisp === lisp) {
                Log.d(TAG, "REPL server already running on port $port")
                return true
            }
            // Different port or Lisp instance, restart
            stop()
        }

        return try {
            // Share the app's Lisp instance so state changes are reflected
            server = SocketReplServer(port) { lisp }
            currentLisp = lisp
            currentPort = port

            serverThread =
                Thread({
                    try {
                        server?.start()
                    } catch (e: Exception) {
                        Log.e(TAG, "REPL server error", e)
                    }
                }, "ReplServerThread")
            serverThread?.isDaemon = true
            serverThread?.start()

            Log.i(TAG, "REPL server started on port $port")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start REPL server on port $port", e)
            server = null
            serverThread = null
            currentLisp = null
            false
        }
    }

    /**
     * Stop the REPL server.
     */
    @Synchronized
    fun stop() {
        try {
            server?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping REPL server", e)
        }
        server = null
        serverThread = null
        currentLisp = null
        currentPort = 0
        Log.i(TAG, "REPL server stopped")
    }

    /**
     * Get the local IP address of the device, preferring WiFi interface.
     */
    fun getLocalIpAddress(): String? {
        try {
            return findLocalIpAddress()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get local IP address", e)
            return null
        }
    }

    private fun findLocalIpAddress(): String? {
        var fallbackAddress: String? = null
        val interfaces = NetworkInterface.getNetworkInterfaces()

        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            val foundAddress = scanNetworkInterface(networkInterface, fallbackAddress)

            if (foundAddress is WifiAddress) {
                return foundAddress.address
            }

            if (foundAddress is FallbackAddress && fallbackAddress == null) {
                fallbackAddress = foundAddress.address
            }
        }

        return fallbackAddress
    }

    private sealed class AddressResult

    private data class WifiAddress(
        val address: String,
    ) : AddressResult()

    private data class FallbackAddress(
        val address: String,
    ) : AddressResult()

    private fun scanNetworkInterface(
        networkInterface: NetworkInterface,
        currentFallback: String?,
    ): AddressResult? {
        val addresses = networkInterface.inetAddresses
        val interfaceName = networkInterface.name.lowercase()

        while (addresses.hasMoreElements()) {
            val address = addresses.nextElement()
            if (!address.isLoopbackAddress && address is Inet4Address) {
                val hostAddress = address.hostAddress ?: continue

                // Prefer WiFi interfaces (wlan0, wlan1, etc.)
                if (interfaceName.startsWith("wlan")) {
                    return WifiAddress(hostAddress)
                }

                // Return first valid address as fallback
                if (currentFallback == null) {
                    return FallbackAddress(hostAddress)
                }
            }
        }

        return null
    }
}
