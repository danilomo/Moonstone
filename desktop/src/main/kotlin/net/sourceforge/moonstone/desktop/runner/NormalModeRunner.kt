package net.sourceforge.moonstone.desktop.runner

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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
        val runtime = setupRuntime(scriptPath)

        try {
            loadScript(runtime, scriptPath)
            val windowConfig = readWindowConfig(runtime.environment())
            launchApplication(runtime, windowConfig.width, windowConfig.height)
        } catch (e: Exception) {
            handleStartupError(e)
        }
    }

    private fun setupRuntime(scriptPath: String): MoonstoneRuntime {
        val runtime = MoonstoneRuntime(Platform.DESKTOP_JVM)
        ComponentRegistrar.registerAll(runtime)
        DesktopExtensionsRegistrar.register(runtime)

        loadAppConfig(scriptPath, runtime)
        DatabaseSetup.register(runtime, scriptPath)

        return runtime
    }

    private fun loadAppConfig(
        scriptPath: String,
        runtime: MoonstoneRuntime,
    ) {
        val appFolder = File(scriptPath).parentFile
        if (appFolder != null) {
            val configFile = File(appFolder, "app.conf")
            ConfigLoader.loadConfig(configFile, runtime.environment())
        }
    }

    private fun loadScript(
        runtime: MoonstoneRuntime,
        scriptPath: String,
    ) {
        runtime.loadScriptForReactiveMode(Path.of(scriptPath))
    }

    private fun launchApplication(
        runtime: MoonstoneRuntime,
        width: Int,
        height: Int,
    ) {
        application {
            createWindow(runtime, width, height, ::exitApplication)
        }
    }

    @Composable
    private fun createWindow(
        runtime: MoonstoneRuntime,
        width: Int,
        height: Int,
        onExit: () -> Unit,
    ) {
        val windowState = rememberWindowState(width = width.dp, height = height.dp)
        Window(
            onCloseRequest = onExit,
            title = "Moonstone",
            state = windowState,
        ) {
            renderWindowContent(runtime)
        }
    }

    @Composable
    private fun renderWindowContent(runtime: MoonstoneRuntime) {
        val renderer = UIRenderer(runtime.componentRegistry, runtime.stateManager)

        MaterialTheme {
            Surface {
                val rootElement = runtime.rootElement.value
                if (rootElement != null) {
                    renderer.RenderRoot(rootElement)
                }
            }
        }
    }

    private fun handleStartupError(e: Exception) {
        System.err.println("Error: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}
