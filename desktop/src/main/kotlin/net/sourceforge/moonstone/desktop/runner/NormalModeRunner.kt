package net.sourceforge.moonstone.desktop.runner

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import net.sourceforge.moonstone.config.ConfigLoader
import net.sourceforge.moonstone.desktop.config.readWindowConfig
import net.sourceforge.moonstone.desktop.setup.ComponentRegistrar
import net.sourceforge.moonstone.desktop.setup.DatabaseSetup
import net.sourceforge.moonstone.desktop.setup.DesktopExtensionsRegistrar
import net.sourceforge.moonstone.render.UIRenderer
import net.sourceforge.moonstone.runtime.MoonstoneRuntime
import net.sourceforge.moonstone.runtime.Platform
import java.io.File
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * Runner for normal mode (production mode without debug features).
 */
object NormalModeRunner {
    fun run(scriptPath: String) {
        val runtime = MoonstoneRuntime(Platform.DESKTOP_JVM)
        ComponentRegistrar.registerAll(runtime)
        DesktopExtensionsRegistrar.register(runtime)

        // Load app.conf if it exists (before database setup to allow *db-location* override)
        val appFolder = File(scriptPath).parentFile
        if (appFolder != null) {
            val configFile = File(appFolder, "app.conf")
            ConfigLoader.loadConfig(configFile, runtime.environment())
        }

        // Register database extensions (will use *db-location* if defined)
        DatabaseSetup.register(runtime, scriptPath)

        try {
            // Load and evaluate the script
            runtime.loadScriptForReactiveMode(Path.of(scriptPath))

            // Read window configuration from script (after loading)
            val windowConfig = readWindowConfig(runtime.environment())

            val renderer = UIRenderer(runtime.componentRegistry, runtime.stateManager)

            application {
                val windowState =
                    rememberWindowState(
                        width = windowConfig.width.dp,
                        height = windowConfig.height.dp,
                    )
                Window(
                    onCloseRequest = ::exitApplication,
                    title = "Moonstone",
                    state = windowState,
                ) {
                    MaterialTheme {
                        Surface {
                            // Re-evaluate app function on each recomposition
                            // The rootElement state triggers recomposition when changed
                            val rootElement = runtime.rootElement.value
                            if (rootElement != null) {
                                renderer.RenderRoot(rootElement)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Error: ${e.message}")
            e.printStackTrace()
            exitProcess(1)
        }
    }
}
