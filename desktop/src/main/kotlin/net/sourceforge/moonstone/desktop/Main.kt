package net.sourceforge.moonstone.desktop

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
import net.sourceforge.kleinlisp.LispEnvironment
import net.sourceforge.kleinlisp.objects.BooleanObject
import net.sourceforge.kleinlisp.objects.IntObject
import net.sourceforge.kleinlisp.objects.StringObject
import net.sourceforge.kleinlisp.objects.VoidObject
import net.sourceforge.moonstone.components.impl.AlertDialogComponent
import net.sourceforge.moonstone.components.impl.BadgeComponent
import net.sourceforge.moonstone.components.impl.BottomNavigationComponent
import net.sourceforge.moonstone.components.impl.BottomSheetComponent
import net.sourceforge.moonstone.components.impl.BoxComponent
import net.sourceforge.moonstone.components.impl.ButtonComponent
import net.sourceforge.moonstone.components.impl.CardComponent
import net.sourceforge.moonstone.components.impl.CheckboxComponent
import net.sourceforge.moonstone.components.impl.ChipComponent
import net.sourceforge.moonstone.components.impl.ColumnComponent
import net.sourceforge.moonstone.components.impl.DividerComponent
import net.sourceforge.moonstone.components.impl.DynamicListComponent
import net.sourceforge.moonstone.components.impl.ErrorBoundaryComponent
import net.sourceforge.moonstone.components.impl.FloatingActionButtonComponent
import net.sourceforge.moonstone.components.impl.IconComponent
import net.sourceforge.moonstone.components.impl.ImageComponent
import net.sourceforge.moonstone.components.impl.LazyColumnComponent
import net.sourceforge.moonstone.components.impl.LazyRowComponent
import net.sourceforge.moonstone.components.impl.ListItemComponent
import net.sourceforge.moonstone.components.impl.NavItemComponent
import net.sourceforge.moonstone.components.impl.OutlinedTextFieldComponent
import net.sourceforge.moonstone.components.impl.ProgressIndicatorComponent
import net.sourceforge.moonstone.components.impl.RadioButtonComponent
import net.sourceforge.moonstone.components.impl.RowComponent
import net.sourceforge.moonstone.components.impl.ScaffoldComponent
import net.sourceforge.moonstone.components.impl.SliderComponent
import net.sourceforge.moonstone.components.impl.SnackbarComponent
import net.sourceforge.moonstone.components.impl.SpacerComponent
import net.sourceforge.moonstone.components.impl.SurfaceComponent
import net.sourceforge.moonstone.components.impl.SwitchComponent
import net.sourceforge.moonstone.components.impl.SwitchViewComponent
import net.sourceforge.moonstone.components.impl.TextComponent
import net.sourceforge.moonstone.components.impl.TextFieldComponent
import net.sourceforge.moonstone.components.impl.TopAppBarComponent
import net.sourceforge.moonstone.components.impl.ViewComponent
import net.sourceforge.moonstone.config.ConfigLoader
import net.sourceforge.moonstone.debug.DebugPanel
import net.sourceforge.moonstone.debug.ReloadableRuntime
import net.sourceforge.moonstone.debug.TreeInspector
import net.sourceforge.moonstone.persistence.DatabaseExtensions
import net.sourceforge.moonstone.render.UIRenderer
import net.sourceforge.moonstone.runtime.MoonstoneRuntime
import net.sourceforge.moonstone.runtime.Platform
import java.io.File
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * Window size configuration read from Scheme script.
 * Scripts can define *window-width* and *window-height* to customize the window size.
 * This only affects desktop; Android ignores these values.
 */
data class WindowConfig(
    val width: Int = 800,
    val height: Int = 900,
)

/**
 * Read window configuration from the Lisp environment.
 * Looks for optional *window-width* and *window-height* variables.
 */
fun readWindowConfig(env: LispEnvironment): WindowConfig {
    val defaultWidth = 800
    val defaultHeight = 900

    val width = readIntVariable(env, "*window-width*") ?: defaultWidth
    val height = readIntVariable(env, "*window-height*") ?: defaultHeight

    return WindowConfig(width, height)
}

/**
 * Read an optional integer variable from the Lisp environment.
 * Returns null if the variable is not defined or is not a number.
 */
private fun readIntVariable(
    env: LispEnvironment,
    name: String,
): Int? {
    return try {
        val atom = env.atomOf(name)
        val value = env.lookupValueOrNull(atom) ?: return null

        // Try to get as integer
        val intObj = value.asInt()
        if (intObj != null) {
            return intObj.value().toInt()
        }

        // Try to get as double and convert
        val doubleObj = value.asDouble()
        if (doubleObj != null) {
            return doubleObj.value().toInt()
        }

        null
    } catch (e: Exception) {
        null
    }
}

/**
 * Command-line arguments configuration.
 */
data class CliArgs(
    val scriptPath: String?,
    val debugMode: Boolean = false,
    val hotReload: Boolean = false,
)

/**
 * Parse command-line arguments.
 */
fun parseArgs(args: Array<String>): CliArgs {
    var scriptPath: String? = null
    var debugMode = false
    var hotReload = false

    val iterator = args.iterator()
    while (iterator.hasNext()) {
        when (val arg = iterator.next()) {
            "--debug", "-d" -> debugMode = true
            "--hot-reload", "--watch", "-w" -> hotReload = true
            "--help", "-h" -> {
                printUsage()
                exitProcess(0)
            }
            else -> {
                if (!arg.startsWith("-")) {
                    scriptPath = arg
                } else {
                    System.err.println("Unknown option: $arg")
                    printUsage()
                    exitProcess(1)
                }
            }
        }
    }

    return CliArgs(scriptPath, debugMode, hotReload)
}

fun printUsage() {
    println(
        """
        Moonstone - Scheme-based UI Framework

        Usage: kleinlisp-gui [options] <script.scm>

        Options:
          --debug, -d        Enable debug mode with inspector panel
          --hot-reload, -w   Enable hot reload (watches script for changes)
          --watch            Alias for --hot-reload
          --help, -h         Show this help message

        Examples:
          kleinlisp-gui samples/counter/app.scm
          kleinlisp-gui --debug samples/counter/app.scm
          kleinlisp-gui --hot-reload samples/counter/app.scm
          kleinlisp-gui -d -w samples/counter/app.scm
        """.trimIndent(),
    )
}

/**
 * Register all standard components to a runtime.
 */
fun registerAllComponents(runtime: ReloadableRuntime) {
    // Register layout components
    runtime.registerComponent(BoxComponent())
    runtime.registerComponent(ColumnComponent())
    runtime.registerComponent(RowComponent())
    runtime.registerComponent(SurfaceComponent())
    runtime.registerComponent(SpacerComponent())

    // Register display components
    runtime.registerComponent(TextComponent())

    // Register interactive components (Phase 3)
    runtime.registerComponent(ButtonComponent())
    runtime.registerComponent(TextFieldComponent())
    runtime.registerComponent(OutlinedTextFieldComponent())
    runtime.registerComponent(CheckboxComponent())
    runtime.registerComponent(SwitchComponent())
    runtime.registerComponent(RadioButtonComponent())

    // Register advanced components (Phase 6)
    // Icon
    runtime.registerComponent(IconComponent())

    // Lists
    runtime.registerComponent(LazyColumnComponent())
    runtime.registerComponent(LazyRowComponent())
    runtime.registerComponent(ListItemComponent())
    runtime.registerComponent(DynamicListComponent())

    // Navigation
    runtime.registerComponent(ScaffoldComponent())
    runtime.registerComponent(TopAppBarComponent())
    runtime.registerComponent(BottomNavigationComponent())
    runtime.registerComponent(NavItemComponent())

    // Dialogs
    runtime.registerComponent(AlertDialogComponent())
    runtime.registerComponent(BottomSheetComponent())
    runtime.registerComponent(SnackbarComponent())

    // Conditional rendering
    runtime.registerComponent(SwitchViewComponent())
    runtime.registerComponent(ViewComponent())

    // Error handling (Phase 7)
    runtime.registerComponent(ErrorBoundaryComponent())

    // New Material 3 components (Phase 8)
    runtime.registerComponent(CardComponent())
    runtime.registerComponent(SliderComponent())
    runtime.registerComponent(ProgressIndicatorComponent())
    runtime.registerComponent(ChipComponent())
    runtime.registerComponent(DividerComponent())
    runtime.registerComponent(ImageComponent())
    runtime.registerComponent(FloatingActionButtonComponent())
    runtime.registerComponent(BadgeComponent())
}

/**
 * Register all standard components to a non-reloadable runtime.
 */
fun registerAllComponents(runtime: MoonstoneRuntime) {
    // Register layout components
    runtime.registerComponent(BoxComponent())
    runtime.registerComponent(ColumnComponent())
    runtime.registerComponent(RowComponent())
    runtime.registerComponent(SurfaceComponent())
    runtime.registerComponent(SpacerComponent())

    // Register display components
    runtime.registerComponent(TextComponent())

    // Register interactive components (Phase 3)
    runtime.registerComponent(ButtonComponent())
    runtime.registerComponent(TextFieldComponent())
    runtime.registerComponent(OutlinedTextFieldComponent())
    runtime.registerComponent(CheckboxComponent())
    runtime.registerComponent(SwitchComponent())
    runtime.registerComponent(RadioButtonComponent())

    // Register advanced components (Phase 6)
    // Icon
    runtime.registerComponent(IconComponent())

    // Lists
    runtime.registerComponent(LazyColumnComponent())
    runtime.registerComponent(LazyRowComponent())
    runtime.registerComponent(ListItemComponent())
    runtime.registerComponent(DynamicListComponent())

    // Navigation
    runtime.registerComponent(ScaffoldComponent())
    runtime.registerComponent(TopAppBarComponent())
    runtime.registerComponent(BottomNavigationComponent())
    runtime.registerComponent(NavItemComponent())

    // Dialogs
    runtime.registerComponent(AlertDialogComponent())
    runtime.registerComponent(BottomSheetComponent())
    runtime.registerComponent(SnackbarComponent())

    // Conditional rendering
    runtime.registerComponent(SwitchViewComponent())
    runtime.registerComponent(ViewComponent())

    // Error handling (Phase 7)
    runtime.registerComponent(ErrorBoundaryComponent())

    // New Material 3 components (Phase 8)
    runtime.registerComponent(CardComponent())
    runtime.registerComponent(SliderComponent())
    runtime.registerComponent(ProgressIndicatorComponent())
    runtime.registerComponent(ChipComponent())
    runtime.registerComponent(DividerComponent())
    runtime.registerComponent(ImageComponent())
    runtime.registerComponent(FloatingActionButtonComponent())
    runtime.registerComponent(BadgeComponent())
}

/**
 * Register desktop-specific Scheme functions.
 * These provide desktop equivalents of Android-specific functions for cross-platform compatibility.
 */
fun registerDesktopExtensions(runtime: MoonstoneRuntime) {
    val env = runtime.environment()

    // (toast message) - Print message to console (desktop equivalent of Android toast)
    env.registerFunction("toast") { params ->
        if (params.isNotEmpty()) {
            val message =
                when (val first = params[0]) {
                    is StringObject -> first.value()
                    else -> first.toString()
                }
            println("[Toast] $message")
        }
        VoidObject.VOID
    }

    // (vibrate duration) - No-op on desktop
    env.registerFunction("vibrate") { _ ->
        VoidObject.VOID
    }

    // (dark-mode?) - Always returns false on desktop (could be improved)
    env.registerFunction("dark-mode?") { _ ->
        BooleanObject.FALSE
    }

    // (screen-width) - Return a default width
    env.registerFunction("screen-width") { _ ->
        IntObject(1200)
    }

    // (screen-height) - Return a default height
    env.registerFunction("screen-height") { _ ->
        IntObject(800)
    }

    // (android-version) - Return 0 on desktop
    env.registerFunction("android-version") { _ ->
        IntObject(0)
    }

    // (device-model) - Return "Desktop" on desktop
    env.registerFunction("device-model") { _ ->
        StringObject("Desktop")
    }
}

/**
 * Register desktop-specific Scheme functions for a reloadable runtime.
 */
fun registerDesktopExtensions(runtime: ReloadableRuntime) {
    registerDesktopExtensions(runtime.baseRuntime)
}

/**
 * Register database extensions with the runtime.
 * Creates a database handler and sets it as *db*.
 *
 * If `*db-location*` is defined in the environment (e.g., from app.conf),
 * that path is used for the database. Otherwise, defaults to `app.db`
 * in the script's directory.
 */
fun registerDatabaseExtensions(
    runtime: MoonstoneRuntime,
    scriptPath: String,
): DatabaseExtensions {
    val env = runtime.environment()
    val databaseExtensions = DatabaseExtensions(env)
    databaseExtensions.register()

    // Check for *db-location* override from config
    val dbPath = readDbLocation(env, scriptPath)
    val handler = databaseExtensions.createHandler(dbPath)
    env.set(env.atomOf("*db*"), handler)

    return databaseExtensions
}

/**
 * Determine the database path.
 * Checks for *db-location* variable in the environment first,
 * otherwise defaults to app.db in the script's folder.
 *
 * Relative paths (e.g., "../shared.db") are resolved relative to the script's directory.
 */
private fun readDbLocation(
    env: LispEnvironment,
    scriptPath: String?,
): File {
    // Check for *db-location* override
    try {
        val atom = env.atomOf("*db-location*")
        val value = env.lookupValueOrNull(atom)
        if (value != null) {
            val pathStr = value.asString()?.value()
            if (pathStr != null && pathStr.isNotEmpty()) {
                val dbFile = File(pathStr)
                // If relative path, resolve from script directory
                val resolved =
                    if (dbFile.isAbsolute) {
                        dbFile
                    } else {
                        val appFolder = determineAppFolder(scriptPath)
                        File(appFolder, pathStr)
                    }
                // Canonicalize to resolve ".." and "." segments
                return resolved.canonicalFile
            }
        }
    } catch (_: Exception) {
        // Variable not defined or invalid, use default
    }

    // Default to app.db in script's folder
    val appFolder = determineAppFolder(scriptPath)
    return File(appFolder, "app.db")
}

/**
 * Register database extensions with a reloadable runtime.
 * Creates a database handler and sets it as *db*.
 *
 * If `*db-location*` is defined in the environment (e.g., from app.conf),
 * that path is used for the database. Otherwise, defaults to `app.db`
 * in the script's directory.
 */
fun registerDatabaseExtensions(
    runtime: ReloadableRuntime,
    scriptPath: String,
): DatabaseExtensions {
    val env = runtime.baseRuntime.environment()
    val databaseExtensions = DatabaseExtensions(env)
    databaseExtensions.register()

    // Check for *db-location* override from config
    val dbPath = readDbLocation(env, scriptPath)
    val handler = databaseExtensions.createHandler(dbPath)
    env.set(env.atomOf("*db*"), handler)

    return databaseExtensions
}

/**
 * Determine the app folder based on the script path.
 * Uses the script's directory, or ~/.kleinlisp if no script is provided.
 */
private fun determineAppFolder(scriptPath: String?): File =
    if (scriptPath != null) {
        File(scriptPath).parentFile ?: File(System.getProperty("user.home"), ".kleinlisp")
    } else {
        File(System.getProperty("user.home"), ".kleinlisp")
    }

fun main(args: Array<String>) {
    val cliArgs = parseArgs(args)

    if (cliArgs.scriptPath == null) {
        System.err.println("Error: No script file specified")
        printUsage()
        exitProcess(1)
    }

    // Use debug/hot-reload mode if either flag is set
    val useDebugRuntime = cliArgs.debugMode || cliArgs.hotReload

    if (useDebugRuntime) {
        runWithDebugMode(cliArgs)
    } else {
        runNormalMode(cliArgs.scriptPath)
    }
}

/**
 * Run in normal mode (no debug features).
 */
fun runNormalMode(scriptPath: String) {
    val runtime = MoonstoneRuntime(Platform.DESKTOP_JVM)
    registerAllComponents(runtime)
    registerDesktopExtensions(runtime)

    // Load app.conf if it exists (before database setup to allow *db-location* override)
    val appFolder = File(scriptPath).parentFile
    if (appFolder != null) {
        val configFile = File(appFolder, "app.conf")
        ConfigLoader.loadConfig(configFile, runtime.environment())
    }

    // Register database extensions (will use *db-location* if defined)
    val databaseExtensions = registerDatabaseExtensions(runtime, scriptPath)

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

/**
 * Run with debug mode features (hot reload, inspector).
 */
fun runWithDebugMode(cliArgs: CliArgs) {
    val runtime = ReloadableRuntime(Platform.DESKTOP_JVM)
    registerAllComponents(runtime)
    registerDesktopExtensions(runtime)

    // Load app.conf if it exists (before database setup to allow *db-location* override)
    val appFolder = File(cliArgs.scriptPath!!).parentFile
    if (appFolder != null) {
        val configFile = File(appFolder, "app.conf")
        ConfigLoader.loadConfig(configFile, runtime.baseRuntime.environment())
    }

    // Register database extensions (will use *db-location* if defined)
    // Note: Hot reload will not re-register database extensions, so db-table
    // definitions may be lost after reload. Use normal mode for database apps.
    registerDatabaseExtensions(runtime, cliArgs.scriptPath!!)

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
