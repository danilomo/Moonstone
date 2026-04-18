package net.sourceforge.moonstone.android

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.UIRenderer
import net.sourceforge.moonstone.runtime.MoonstoneRuntime
import net.sourceforge.moonstone.runtime.Platform

/**
 * Main Activity for Moonstone Android app.
 *
 * This activity loads a Scheme script from the assets folder and renders it
 * using the Moonstone runtime.
 *
 * By default, it looks for `app.scm` in the assets folder. You can customize
 * this by setting the `SCRIPT_NAME` constant or by subclassing this activity.
 */
class MainActivity : ComponentActivity() {

    companion object {
        /**
         * The name of the Scheme script to load from assets.
         * Override this in a subclass to load a different script.
         */
        const val SCRIPT_NAME = "app.scm"
    }

    private var runtime: MoonstoneRuntime? = null
    private var backPressedCallback: OnBackPressedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Set up back button handling
        setupBackPressedHandler()

        // Load the script from assets
        val (rootElement, error) = loadScriptFromAssets(SCRIPT_NAME)

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

    /**
     * Set up Android back button handling.
     *
     * For now, this implements simple back-to-exit behavior.
     * Future implementations could integrate with navigation state.
     */
    private fun setupBackPressedHandler() {
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Default behavior: just finish the activity
                // Future: Could check navigation state and go back in app
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressedCallback!!)
    }

    /**
     * Load a Scheme script from the assets folder.
     *
     * @param scriptName The name of the script file in assets
     * @return A pair of (UIElement?, error message?)
     */
    private fun loadScriptFromAssets(scriptName: String): Pair<UIElement?, String?> {
        return try {
            // Read script from assets
            val code = assets.open(scriptName).bufferedReader().use { it.readText() }

            // Create runtime
            runtime = MoonstoneRuntime(Platform.ANDROID)
            registerAllComponents(runtime!!)
            AndroidExtensions.register(runtime!!, this)

            // Load and evaluate the script
            val element = runtime!!.loadCode(code)
            Pair(element, null)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, e.message ?: "Unknown error loading script")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runtime = null
        backPressedCallback?.remove()
    }
}

/**
 * Composable theme wrapper for Moonstone apps.
 *
 * Supports Material You dynamic colors on Android 12+ for a modern,
 * personalized appearance that adapts to the user's wallpaper.
 */
@Composable
fun MoonstoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

/**
 * Loading screen shown while the script is being loaded.
 */
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Loading...",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

/**
 * Error screen shown when script loading fails.
 */
@Composable
fun ErrorScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Error: $message",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Red
        )
    }
}
