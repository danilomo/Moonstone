package net.sourceforge.moonstone.desktop.runner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import net.sourceforge.moonstone.config.ConfigLoader
import net.sourceforge.moonstone.debug.DebugPanel
import net.sourceforge.moonstone.debug.ReloadableRuntime
import net.sourceforge.moonstone.debug.TreeInspector
import net.sourceforge.moonstone.desktop.config.CliArgs
import net.sourceforge.moonstone.desktop.config.readWindowConfig
import net.sourceforge.moonstone.desktop.setup.ComponentRegistrar
import net.sourceforge.moonstone.desktop.setup.DatabaseSetup
import net.sourceforge.moonstone.desktop.setup.DesktopExtensionsRegistrar
import net.sourceforge.moonstone.render.UIRenderer
import net.sourceforge.moonstone.runtime.Platform
import java.io.File
import java.nio.file.Path

/**
 * Runner for debug mode (with hot reload and inspector features).
 */
object DebugModeRunner {
    fun run(cliArgs: CliArgs) {
        val runtime = ReloadableRuntime(Platform.DESKTOP_JVM)
        ComponentRegistrar.registerAll(runtime)
        DesktopExtensionsRegistrar.register(runtime)

        // Load app.conf if it exists (before database setup to allow *db-location* override)
        val appFolder = File(cliArgs.scriptPath!!).parentFile
        if (appFolder != null) {
            val configFile = File(appFolder, "app.conf")
            ConfigLoader.loadConfig(configFile, runtime.baseRuntime.environment())
        }

        // Register database extensions (will use *db-location* if defined)
        // Note: Hot reload will not re-register database extensions, so db-table
        // definitions may be lost after reload. Use normal mode for database apps.
        DatabaseSetup.register(runtime, cliArgs.scriptPath!!)

        // Load script with optional hot reload
        runtime.loadScript(
            path = Path.of(cliArgs.scriptPath!!),
            enableHotReload = cliArgs.hotReload,
        )

        // Read window configuration from script (after loading)
        val windowConfig = readWindowConfig(runtime.baseRuntime.environment())
        // In debug mode, add extra width for the inspector panel
        val debugWidth = if (cliArgs.debugMode) windowConfig.width + 400 else windowConfig.width

        // Add shutdown hook to clean up hot reloader
        Runtime.getRuntime().addShutdownHook(
            Thread {
                runtime.dispose()
            },
        )

        application {
            val windowState =
                rememberWindowState(
                    width = debugWidth.dp,
                    height = windowConfig.height.dp,
                )
            var inspectorVisible by remember { mutableStateOf(cliArgs.debugMode) }

            Window(
                onCloseRequest = {
                    runtime.dispose()
                    exitApplication()
                },
                title = if (cliArgs.debugMode) "Moonstone [DEBUG]" else "Moonstone",
                state = windowState,
            ) {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Debug panel at top
                            if (cliArgs.debugMode) {
                                DebugPanel(
                                    reloadCount = runtime.reloadCount.value,
                                    lastError = runtime.lastError.value,
                                    hotReloadEnabled = runtime.hotReloadEnabled.value,
                                    onReload = { runtime.reload() },
                                    onToggleInspector = { inspectorVisible = !inspectorVisible },
                                    inspectorVisible = inspectorVisible,
                                )
                            }

                            // Main content area
                            Row(modifier = Modifier.weight(1f)) {
                                // App content
                                Surface(modifier = Modifier.weight(1f)) {
                                    val rootElement = runtime.rootElement.value
                                    if (rootElement != null) {
                                        val renderer =
                                            UIRenderer(
                                                runtime.componentRegistry,
                                                runtime.stateManager,
                                            )
                                        renderer.RenderRoot(rootElement)
                                    } else {
                                        val error = runtime.lastError.value
                                        if (error != null) {
                                            Text("Error loading script: ${error.message}")
                                        } else {
                                            Text("Loading...")
                                        }
                                    }
                                }

                                // Tree inspector (side panel)
                                if (inspectorVisible && cliArgs.debugMode) {
                                    TreeInspector(
                                        rootElement = runtime.rootElement.value,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
