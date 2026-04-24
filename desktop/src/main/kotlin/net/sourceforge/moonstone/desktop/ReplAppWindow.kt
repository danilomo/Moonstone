package net.sourceforge.moonstone.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import net.sourceforge.moonstone.components.ComponentRegistry
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.UIRenderer
import net.sourceforge.moonstone.runtime.StateManager

/**
 * Configuration for window size.
 */
data class WindowSize(
    val width: Int = 800,
    val height: Int = 900
)

/**
 * Configuration for the REPL window.
 */
data class ReplWindowConfig(
    val initialElement: UIElement,
    val componentRegistry: ComponentRegistry,
    val stateManager: StateManager,
    val windowSize: WindowSize = WindowSize()
)

/**
 * Callbacks for the REPL window lifecycle.
 */
data class ReplWindowCallbacks(
    val onRootCreated: (MutableState<UIElement>) -> Unit,
    val onClosed: () -> Unit
)

/**
 * Launches a Compose window for the REPL-based app runner.
 *
 * @param config Window configuration including initial element, registries, and window size
 * @param callbacks Lifecycle callbacks for root creation and window close events
 */
fun runReplAppWindow(
    config: ReplWindowConfig,
    callbacks: ReplWindowCallbacks
) {
    // Create the mutable state for the root element
    val rootElement = mutableStateOf(config.initialElement)

    // Notify caller of the root state so they can update it
    callbacks.onRootCreated(rootElement)

    val renderer = UIRenderer(config.componentRegistry, config.stateManager)

    application {
        val windowState =
            rememberWindowState(
                width = config.windowSize.width.dp,
                height = config.windowSize.height.dp,
            )

        Window(
            onCloseRequest = {
                callbacks.onClosed()
                exitApplication()
            },
            title = "Moonstone [REPL]",
            state = windowState,
        ) {
            MaterialTheme {
                Surface {
                    val element = rootElement.value
                    renderer.RenderRoot(element)
                }
            }
        }
    }
}
