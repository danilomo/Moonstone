package net.sourceforge.moonstone.android

import android.os.Bundle
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

        // (app-folder) - Returns the app's folder path
        env.registerFunction("app-folder") { _ ->
            StringObject(folder.absolutePath)
        }

        // (read-app-file filename) - Read a file from the app folder
        env.registerFunction("read-app-file") { params ->
            if (params.isEmpty()) {
                throw Exception("read-app-file requires a filename argument")
            }

            val filename =
                when (val first = params[0]) {
                    is StringObject -> first.value()
                    else -> first.toString()
                }

            val file = File(folder, filename)
            if (!file.exists()) {
                throw Exception("File not found: $filename")
            }

            StringObject(file.readText())
        }

        // (write-app-file filename content) - Write a file to the app folder
        env.registerFunction("write-app-file") { params ->
            if (params.size < 2) {
                throw Exception("write-app-file requires filename and content arguments")
            }

            val filename =
                when (val first = params[0]) {
                    is StringObject -> first.value()
                    else -> first.toString()
                }

            val content =
                when (val second = params[1]) {
                    is StringObject -> second.value()
                    else -> second.toString()
                }

            val file = File(folder, filename)
            file.writeText(content)

            VoidObject.VOID
        }

        // (app-file-exists? filename) - Check if a file exists in the app folder
        env.registerFunction("app-file-exists?") { params ->
            if (params.isEmpty()) {
                throw Exception("app-file-exists? requires a filename argument")
            }

            val filename =
                when (val first = params[0]) {
                    is StringObject -> first.value()
                    else -> first.toString()
                }

            val file = File(folder, filename)
            if (file.exists()) {
                net.sourceforge.kleinlisp.objects.BooleanObject.TRUE
            } else {
                net.sourceforge.kleinlisp.objects.BooleanObject.FALSE
            }
        }
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
        // Check for *db-location* override
        try {
            val atom = env.atomOf("*db-location*")
            val value = env.lookupValueOrNull(atom)
            if (value != null) {
                val pathStr = value.asString()?.value()
                if (pathStr != null && pathStr.isNotEmpty()) {
                    val dbFile = File(pathStr)
                    // If relative path, resolve from app folder
                    val resolved =
                        if (dbFile.isAbsolute) {
                            dbFile
                        } else {
                            File(folder, pathStr)
                        }
                    // Canonicalize to resolve ".." and "." segments
                    return resolved.canonicalFile
                }
            }
        } catch (_: Exception) {
            // Variable not defined or invalid, use default
        }

        // Default to app.db in app's folder
        return File(folder, "app.db")
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
