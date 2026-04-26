package net.sourceforge.moonstone.desktop.runner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        val runtime = setupRuntime(cliArgs)
        loadScriptWithHotReload(runtime, cliArgs)
        val windowConfig = readWindowConfig(runtime.baseRuntime.environment())
        val debugWidth = calculateDebugWidth(windowConfig.width, cliArgs.debugMode)

        registerShutdownHook(runtime)
        launchApplication(runtime, cliArgs, debugWidth, windowConfig.height)
    }

    private fun setupRuntime(cliArgs: CliArgs): ReloadableRuntime {
        val runtime = ReloadableRuntime(Platform.DESKTOP_JVM)
        ComponentRegistrar.registerAll(runtime)
        DesktopExtensionsRegistrar.register(runtime)

        loadAppConfig(cliArgs, runtime)
        DatabaseSetup.register(runtime, cliArgs.scriptPath!!)

        return runtime
    }

    private fun loadAppConfig(
        cliArgs: CliArgs,
        runtime: ReloadableRuntime,
    ) {
        val appFolder = File(cliArgs.scriptPath!!).parentFile
        if (appFolder != null) {
            val configFile = File(appFolder, "app.conf")
            ConfigLoader.loadConfig(configFile, runtime.baseRuntime.environment())
        }
    }

    private fun loadScriptWithHotReload(
        runtime: ReloadableRuntime,
        cliArgs: CliArgs,
    ) {
        runtime.loadScript(
            path = Path.of(cliArgs.scriptPath!!),
            enableHotReload = cliArgs.hotReload,
        )
    }

    private fun calculateDebugWidth(
        baseWidth: Int,
        debugMode: Boolean,
    ): Int = if (debugMode) baseWidth + 400 else baseWidth

    private fun registerShutdownHook(runtime: ReloadableRuntime) {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                runtime.dispose()
            },
        )
    }

    @Composable
    private fun createDebugWindow(
        runtime: ReloadableRuntime,
        cliArgs: CliArgs,
        width: Int,
        height: Int,
        onExit: () -> Unit,
    ) {
        val windowState = rememberWindowState(width = width.dp, height = height.dp)
        var inspectorVisible by remember { mutableStateOf(cliArgs.debugMode) }

        Window(
            onCloseRequest = {
                runtime.dispose()
                onExit()
            },
            title = if (cliArgs.debugMode) "Moonstone [DEBUG]" else "Moonstone",
            state = windowState,
        ) {
            renderWindowContent(runtime, cliArgs, inspectorVisible) { inspectorVisible = !inspectorVisible }
        }
    }

    @Composable
    private fun renderWindowContent(
        runtime: ReloadableRuntime,
        cliArgs: CliArgs,
        inspectorVisible: Boolean,
        onToggleInspector: () -> Unit,
    ) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (cliArgs.debugMode) {
                        renderDebugPanel(runtime, inspectorVisible, onToggleInspector)
                    }

                    Row(modifier = Modifier.weight(1f)) {
                        renderAppContent(runtime)

                        if (inspectorVisible && cliArgs.debugMode) {
                            TreeInspector(rootElement = runtime.rootElement.value)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun renderDebugPanel(
        runtime: ReloadableRuntime,
        inspectorVisible: Boolean,
        onToggleInspector: () -> Unit,
    ) {
        DebugPanel(
            reloadCount = runtime.reloadCount.value,
            lastError = runtime.lastError.value,
            hotReloadEnabled = runtime.hotReloadEnabled.value,
            onReload = { runtime.reload() },
            onToggleInspector = onToggleInspector,
            inspectorVisible = inspectorVisible,
        )
    }

    @Composable
    private fun androidx.compose.foundation.layout.RowScope.renderAppContent(runtime: ReloadableRuntime) {
        Surface(modifier = Modifier.weight(1f)) {
            val rootElement = runtime.rootElement.value
            if (rootElement != null) {
                val renderer = UIRenderer(runtime.componentRegistry, runtime.stateManager)
                renderer.RenderRoot(rootElement)
            } else {
                renderErrorOrLoading(runtime.lastError.value)
            }
        }
    }

    @Composable
    private fun renderErrorOrLoading(error: Throwable?) {
        if (error != null) {
            Text("Error loading script: ${error.message}")
        } else {
            Text("Loading...")
        }
    }

    private fun launchApplication(
        runtime: ReloadableRuntime,
        cliArgs: CliArgs,
        width: Int,
        height: Int,
    ) {
        application {
            createDebugWindow(runtime, cliArgs, width, height, ::exitApplication)
        }
    }
}
