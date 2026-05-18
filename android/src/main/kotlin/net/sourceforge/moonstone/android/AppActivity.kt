package net.sourceforge.moonstone.android

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.objects.StringObject
import net.sourceforge.kleinlisp.objects.VoidObject
import net.sourceforge.moonstone.android.repl.ReplServerManager
import net.sourceforge.moonstone.android.service.SettingsRepository
import net.sourceforge.moonstone.config.ConfigLoader
import net.sourceforge.moonstone.persistence.DatabaseExtensions
import net.sourceforge.moonstone.persistence.DatabaseHandler
import net.sourceforge.moonstone.render.UIRenderer
import net.sourceforge.moonstone.runtime.MoonstoneRuntime
import net.sourceforge.moonstone.runtime.Platform
import java.io.File

/**
 * Activity for running a KleinLisp app in an isolated runtime.
 *
 * Each app launch creates a fresh runtime instance. When the user
 * presses back, the activity finishes and the runtime is destroyed.
 */
class AppActivity : ComponentActivity() {
    companion object {
        /**
         * Extra key for the app folder path.
         */
        const val EXTRA_APP_FOLDER = "net.sourceforge.moonstone.APP_FOLDER"

        /**
         * Extra key for the app name (for display purposes).
         */
        const val EXTRA_APP_NAME = "net.sourceforge.moonstone.APP_NAME"

        private val KEY_CODE_MAP =
            mapOf(
                KeyEvent.KEYCODE_DPAD_UP to "dpad-up",
                KeyEvent.KEYCODE_DPAD_DOWN to "dpad-down",
                KeyEvent.KEYCODE_DPAD_LEFT to "dpad-left",
                KeyEvent.KEYCODE_DPAD_RIGHT to "dpad-right",
                KeyEvent.KEYCODE_BUTTON_A to "a",
                KeyEvent.KEYCODE_BUTTON_B to "b",
                KeyEvent.KEYCODE_BUTTON_X to "x",
                KeyEvent.KEYCODE_BUTTON_Y to "y",
                KeyEvent.KEYCODE_BUTTON_L1 to "l1",
                KeyEvent.KEYCODE_BUTTON_R1 to "r1",
                KeyEvent.KEYCODE_BUTTON_L2 to "l2",
                KeyEvent.KEYCODE_BUTTON_R2 to "r2",
                KeyEvent.KEYCODE_BUTTON_START to "start",
                KeyEvent.KEYCODE_BUTTON_SELECT to "select",
            )
    }

    private var runtime: MoonstoneRuntime? = null
    private var databaseExtensions: DatabaseExtensions? = null
    private var databaseHandler: DatabaseHandler? = null
    private var backPressedCallback: OnBackPressedCallback? = null
    private var appFolder: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setupBackPressedHandler()

        val folderPath = intent.getStringExtra(EXTRA_APP_FOLDER)
        if (folderPath == null) {
            setContent {
                MoonstoneTheme {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .systemBarsPadding(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        ErrorScreen("No app folder provided")
                    }
                }
            }
            return
        }

        appFolder = File(folderPath)
        val error = loadApp()

        // Start REPL server if enabled and app loaded successfully
        if (error == null && runtime != null) {
            startReplServerIfEnabled()
        }

        setContent {
            MoonstoneTheme {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .systemBarsPadding(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (error != null) {
                        ErrorScreen(error)
                    } else {
                        // Observe runtime.rootElement for reactive updates
                        // This enables update-app to work via REPL
                        val rootElement = runtime?.rootElement?.value
                        if (rootElement != null) {
                            val renderer =
                                UIRenderer(
                                    runtime!!.componentRegistry,
                                    runtime!!.stateManager,
                                )
                            renderer.RenderRoot(rootElement)
                        } else {
                            LoadingScreen()
                        }
                    }
                }
            }
        }
    }

    private fun setupBackPressedHandler() {
        backPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Finish and destroy runtime, returning to launcher
                    finish()
                }
            }
        onBackPressedDispatcher.addCallback(this, backPressedCallback!!)
    }

    /**
     * Load the app from the app folder.
     * Returns null on success (rootElement is available via runtime.rootElement),
     * or an error message on failure.
     */
    private fun loadApp(): String? =
        try {
            val folder = appFolder ?: throw Exception("App folder not set")
            val scriptFile = File(folder, "app.scm")

            if (!scriptFile.exists()) {
                throw Exception("app.scm not found in ${folder.absolutePath}")
            }

            val code = scriptFile.readText()

            // Create fresh runtime
            runtime = MoonstoneRuntime(Platform.ANDROID)
            registerAllComponents(runtime!!)
            AndroidExtensions.register(runtime!!, this)
            registerAppExtensions(runtime!!, folder)

            val env = runtime!!.environment()

            // Load app.conf if it exists (before database setup to allow *db-location* override)
            val configFile = File(folder, "app.conf")
            ConfigLoader.loadConfig(configFile, env)

            // Register database extensions
            databaseExtensions = DatabaseExtensions(env)
            databaseExtensions!!.register()

            // Determine database path (check for *db-location* override)
            val dbPath = readDbLocation(env, folder)
            databaseHandler = databaseExtensions!!.createHandler(dbPath)
            env.set(env.atomOf("*db*"), databaseHandler!!)

            // Load and evaluate the script in reactive mode
            // This enables update-app to work via REPL
            runtime!!.loadCodeForReactiveMode(code, scriptFile.absolutePath)
            null // Success
        } catch (e: Exception) {
            e.printStackTrace()
            e.message ?: "Unknown error loading app"
        }

    /**
     * Register app-specific Scheme functions.
     */
    private fun registerAppExtensions(
        runtime: MoonstoneRuntime,
        folder: File,
    ) {
        val env = runtime.environment()

        registerAppFolderFunction(env, folder)
        registerReadAppFileFunction(env, folder)
        registerWriteAppFileFunction(env, folder)
        registerAppFileExistsFunction(env, folder)
    }

    private fun registerAppFolderFunction(
        env: net.sourceforge.kleinlisp.LispEnvironment,
        folder: File,
    ) {
        env.registerFunction("app-folder") { _ ->
            StringObject(folder.absolutePath)
        }
    }

    private fun registerReadAppFileFunction(
        env: net.sourceforge.kleinlisp.LispEnvironment,
        folder: File,
    ) {
        env.registerFunction("read-app-file") { params ->
            if (params.isEmpty()) {
                throw Exception("read-app-file requires a filename argument")
            }

            val filename = extractStringParam(params[0])
            val file = File(folder, filename)
            if (!file.exists()) {
                throw Exception("File not found: $filename")
            }

            StringObject(file.readText())
        }
    }

    private fun registerWriteAppFileFunction(
        env: net.sourceforge.kleinlisp.LispEnvironment,
        folder: File,
    ) {
        env.registerFunction("write-app-file") { params ->
            if (params.size < 2) {
                throw Exception("write-app-file requires filename and content arguments")
            }

            val filename = extractStringParam(params[0])
            val content = extractStringParam(params[1])
            val file = File(folder, filename)
            file.writeText(content)

            VoidObject.VOID
        }
    }

    private fun registerAppFileExistsFunction(
        env: net.sourceforge.kleinlisp.LispEnvironment,
        folder: File,
    ) {
        env.registerFunction("app-file-exists?") { params ->
            if (params.isEmpty()) {
                throw Exception("app-file-exists? requires a filename argument")
            }

            val filename = extractStringParam(params[0])
            val file = File(folder, filename)
            if (file.exists()) {
                net.sourceforge.kleinlisp.objects.BooleanObject.TRUE
            } else {
                net.sourceforge.kleinlisp.objects.BooleanObject.FALSE
            }
        }
    }

    private fun extractStringParam(param: LispObject): String =
        when (param) {
            is StringObject -> param.value()
            else -> param.toString()
        }

    /**
     * Determine the database path.
     * Checks for *db-location* variable in the environment first,
     * otherwise defaults to app.db in the app's folder.
     *
     * Relative paths (e.g., "../shared.db") are resolved relative to the app's folder.
     */
    private fun readDbLocation(
        env: net.sourceforge.kleinlisp.LispEnvironment,
        folder: File,
    ): File {
        val customPath = readCustomDbLocation(env, folder)
        return customPath ?: File(folder, "app.db")
    }

    private fun readCustomDbLocation(
        env: net.sourceforge.kleinlisp.LispEnvironment,
        folder: File,
    ): File? {
        try {
            val atom = env.atomOf("*db-location*")
            val value = env.lookupValueOrNull(atom) ?: return null

            val pathStr = value.asString()?.value()
            if (pathStr.isNullOrEmpty()) return null

            return resolveDbPath(pathStr, folder)
        } catch (_: Exception) {
            return null
        }
    }

    private fun resolveDbPath(
        pathStr: String,
        folder: File,
    ): File {
        val dbFile = File(pathStr)
        val resolved =
            if (dbFile.isAbsolute) {
                dbFile
            } else {
                File(folder, pathStr)
            }
        return resolved.canonicalFile
    }

    /**
     * Start the REPL server if enabled in settings.
     */
    private fun startReplServerIfEnabled() {
        val settings = SettingsRepository(this).loadSettings()
        if (settings.replServerEnabled) {
            val lisp = runtime?.lisp() ?: return
            val success = ReplServerManager.start(lisp, settings.replServerPort)
            if (success) {
                val url = ReplServerManager.serverUrl
                if (url != null) {
                    Toast
                        .makeText(
                            this,
                            "REPL server started: $url",
                            Toast.LENGTH_LONG,
                        ).show()
                }
            } else {
                Toast
                    .makeText(
                        this,
                        "Failed to start REPL server. Port may be in use.",
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        val keyName = keyCodeToName(keyCode) ?: return super.onKeyDown(keyCode, event)
        val handled = runtime?.dispatchKeyDown(keyName) ?: false
        return if (handled) true else super.onKeyDown(keyCode, event)
    }

    private fun keyCodeToName(keyCode: Int): String? = KEY_CODE_MAP[keyCode]

    override fun onDestroy() {
        super.onDestroy()
        // Stop REPL server if running
        ReplServerManager.stop()
        // Close database and clean up resources
        databaseHandler?.close()
        databaseHandler = null
        databaseExtensions = null
        runtime = null
        backPressedCallback?.remove()
    }
}
