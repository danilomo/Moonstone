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
 * Launches a Compose window for the REPL-based app runner.
 *
 * @param initialElement The initial UI tree to render
 * @param componentRegistry Registry of available components
 * @param stateManager State manager for reactive updates
 * @param onRootCreated Callback invoked with the MutableState for the root element,
 *                      allowing external code to update the UI tree
 * @param onClosed Callback invoked when the window is closed
 * @param windowWidth Optional window width in dp (default: 800)
 * @param windowHeight Optional window height in dp (default: 900)
 */
fun runReplAppWindow(
    initialElement: UIElement,
    componentRegistry: ComponentRegistry,
    stateManager: StateManager,
    onRootCreated: (MutableState<UIElement>) -> Unit,
    onClosed: () -> Unit,
    windowWidth: Int = 800,
    windowHeight: Int = 900,
) {
    // Create the mutable state for the root element
    val rootElement = mutableStateOf(initialElement)

    // Notify caller of the root state so they can update it
    onRootCreated(rootElement)

    val renderer = UIRenderer(componentRegistry, stateManager)

    application {
        val windowState =
            rememberWindowState(
                width = windowWidth.dp,
                height = windowHeight.dp,
            )

        Window(
            onCloseRequest = {
                onClosed()
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
