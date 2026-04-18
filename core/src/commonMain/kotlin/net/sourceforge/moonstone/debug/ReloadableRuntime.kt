package net.sourceforge.moonstone.debug

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import net.sourceforge.moonstone.components.ComponentFactory
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.error.ErrorContext
import net.sourceforge.moonstone.runtime.MoonstoneRuntime
import net.sourceforge.moonstone.runtime.Platform
import java.nio.file.Path

/**
 * A wrapper around MoonstoneRuntime that supports hot reloading.
 * Provides observable state for use with Compose.
 */
class ReloadableRuntime(
    val platform: Platform = Platform.detect()
) {
    private var runtime: MoonstoneRuntime = MoonstoneRuntime(platform)
    private var scriptPath: Path? = null
    private var hotReloader: HotReloader? = null
    private val registeredComponents = mutableListOf<ComponentFactory>()

    /**
     * The current root UI element. Triggers recomposition when changed.
     */
    val rootElement: MutableState<UIElement?> = mutableStateOf(null)

    /**
     * Number of times the script has been reloaded.
     */
    val reloadCount: MutableState<Int> = mutableStateOf(0)

    /**
     * The last error that occurred during loading/reloading.
     */
    val lastError: MutableState<Throwable?> = mutableStateOf(null)

    /**
     * Whether hot reload is currently enabled.
     */
    val hotReloadEnabled: MutableState<Boolean> = mutableStateOf(false)

    /**
     * Register a component factory. These will be re-registered on each reload.
     */
    fun registerComponent(factory: ComponentFactory) {
        registeredComponents.add(factory)
        runtime.registerComponent(factory)
    }

    /**
     * Get the underlying runtime instance.
     * Useful for registering extensions that need direct access to the runtime.
     */
    val baseRuntime: MoonstoneRuntime get() = runtime

    /**
     * Get the underlying runtime's component registry.
     */
    val componentRegistry get() = runtime.componentRegistry

    /**
     * Get the underlying runtime's state manager.
     */
    val stateManager get() = runtime.stateManager

    /**
     * Load a script and optionally start watching for changes.
     */
    fun loadScript(path: Path, enableHotReload: Boolean = false): UIElement? {
        scriptPath = path
        hotReloadEnabled.value = enableHotReload

        // Clear previous error
        lastError.value = null
        ErrorContext.clear()

        return try {
            val element = runtime.loadScript(path)
            rootElement.value = element

            if (enableHotReload) {
                startHotReload(path)
            }

            element
        } catch (e: Throwable) {
            lastError.value = e
            null
        }
    }

    /**
     * Manually reload the current script.
     */
    fun reload(): UIElement? {
        val path = scriptPath ?: return null

        // Create a fresh runtime
        runtime = MoonstoneRuntime(platform)

        // Re-register all components
        registeredComponents.forEach { factory ->
            runtime.registerComponent(factory)
        }

        // Clear error context
        ErrorContext.clear()
        lastError.value = null

        return try {
            val element = runtime.loadScript(path)
            rootElement.value = element
            reloadCount.value++
            element
        } catch (e: Throwable) {
            lastError.value = e
            null
        }
    }

    /**
     * Start hot reloading for the given script path.
     */
    private fun startHotReload(path: Path) {
        // Stop any existing watcher
        hotReloader?.stop()

        hotReloader = HotReloader(
            filePath = path,
            debounceMs = 100,
            onReload = { reload() },
            onError = { e -> lastError.value = e }
        )
        hotReloader?.start()
    }

    /**
     * Stop hot reloading.
     */
    fun stopHotReload() {
        hotReloader?.stop()
        hotReloader = null
        hotReloadEnabled.value = false
    }

    /**
     * Cleanup resources when the runtime is no longer needed.
     */
    fun dispose() {
        stopHotReload()
        ErrorContext.clear()
    }
}
