package net.sourceforge.moonstone.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.UIRenderer
import net.sourceforge.moonstone.runtime.MoonstoneRuntime
import net.sourceforge.moonstone.runtime.Platform
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Activity for loading and running external Scheme scripts.
 *
 * This activity can be launched via:
 * - Intent with ACTION_VIEW and a file:// or content:// URI
 * - Intent with EXTRA_SCRIPT_PATH containing a file path
 * - Intent with EXTRA_SCRIPT_CODE containing script code directly
 *
 * Example usage from another app:
 * ```kotlin
 * val intent = Intent(Intent.ACTION_VIEW).apply {
 *     data = Uri.parse("file:///sdcard/myapp/app.scm")
 *     setClass(context, ScriptActivity::class.java)
 * }
 * startActivity(intent)
 * ```
 */
class ScriptActivity : ComponentActivity() {

    companion object {
        /**
         * Extra key for passing script code directly.
         */
        const val EXTRA_SCRIPT_CODE = "net.sourceforge.moonstone.SCRIPT_CODE"

        /**
         * Extra key for passing a script file path.
         */
        const val EXTRA_SCRIPT_PATH = "net.sourceforge.moonstone.SCRIPT_PATH"
    }

    private var runtime: MoonstoneRuntime? = null
    private var backPressedCallback: OnBackPressedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setupBackPressedHandler()

        val (rootElement, error) = loadScript()

        setContent {
            MoonstoneTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (error != null) {
                        ErrorScreen(error)
                    } else if (rootElement != null) {
                        val renderer = UIRenderer(
                            runtime!!.componentRegistry,
                            runtime!!.stateManager
                        )
                        renderer.RenderRoot(rootElement)
                    } else {
                        LoadingScreen()
                    }
                }
            }
        }
    }

    private fun setupBackPressedHandler() {
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressedCallback!!)
    }

    /**
     * Load a script from the intent data.
     *
     * Priority:
     * 1. EXTRA_SCRIPT_CODE - direct script code
     * 2. EXTRA_SCRIPT_PATH - file path
     * 3. Intent data URI - file:// or content:// URI
     */
    private fun loadScript(): Pair<UIElement?, String?> {
        return try {
            val code = getScriptCode()
            if (code == null) {
                return Pair(null, "No script provided. Use EXTRA_SCRIPT_CODE, EXTRA_SCRIPT_PATH, or provide a data URI.")
            }

            runtime = MoonstoneRuntime(Platform.ANDROID)
            registerAllComponents(runtime!!)
            AndroidExtensions.register(runtime!!, this)

            val element = runtime!!.loadCode(code)
            Pair(element, null)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, e.message ?: "Unknown error loading script")
        }
    }

    /**
     * Get script code from the intent.
     */
    private fun getScriptCode(): String? {
        // Check for direct script code
        intent.getStringExtra(EXTRA_SCRIPT_CODE)?.let { return it }

        // Check for file path
        intent.getStringExtra(EXTRA_SCRIPT_PATH)?.let { path ->
            return try {
                java.io.File(path).readText()
            } catch (e: Exception) {
                throw Exception("Failed to read script file: ${e.message}")
            }
        }

        // Check for data URI
        intent.data?.let { uri ->
            return readUri(uri)
        }

        return null
    }

    /**
     * Read content from a URI.
     */
    private fun readUri(uri: Uri): String {
        return when (uri.scheme) {
            "file" -> {
                val path = uri.path
                    ?: throw Exception("Invalid file URI: no path")
                java.io.File(path).readText()
            }
            "content" -> {
                contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                } ?: throw Exception("Failed to open content URI")
            }
            else -> throw Exception("Unsupported URI scheme: ${uri.scheme}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runtime = null
        backPressedCallback?.remove()
    }
}
