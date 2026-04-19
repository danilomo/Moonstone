package net.sourceforge.moonstone.runtime

import androidx.compose.runtime.mutableStateOf
import net.sourceforge.kleinlisp.Lisp
import net.sourceforge.kleinlisp.LispEnvironment
import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.objects.JavaObject
import net.sourceforge.kleinlisp.objects.VoidObject
import net.sourceforge.moonstone.components.ComponentFactory
import net.sourceforge.moonstone.components.ComponentRegistry
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.components.UIElementWrapper
import net.sourceforge.moonstone.error.ErrorContext
import net.sourceforge.moonstone.error.ScriptLoadException
import net.sourceforge.moonstone.error.StateException
import java.nio.file.Path

/**
 * Main runtime class for Moonstone.
 * Manages the Lisp environment, state, and component registry.
 */
class MoonstoneRuntime(
    val platform: Platform = Platform.detect(),
) {
    private val lisp = Lisp()
    private val env: LispEnvironment = lisp.environment()
    val stateManager = StateManager()
    val componentRegistry = ComponentRegistry()

    /**
     * The path of the currently loaded script.
     * Used to provide context in error messages.
     */
    var currentScriptPath: String? = null
        private set

    /**
     * The current root UI element (reactive state for Compose).
     * Changes when state updates trigger re-evaluation of the app function.
     */
    val rootElement = mutableStateOf<UIElement?>(null)

    /**
     * Reference to the entry point function for re-evaluation.
     */
    private var entryPointFunction: net.sourceforge.kleinlisp.Function? = null

    init {
        registerCoreFunctions()
    }

    /**
     * Load a script in reactive mode - the app function will be re-evaluated
     * whenever state changes.
     */
    fun loadScriptForReactiveMode(path: Path) {
        currentScriptPath = path.toString()
        ErrorContext.setScriptPath(currentScriptPath)

        try {
            val code =
                try {
                    path.toFile().readText()
                } catch (e: Exception) {
                    throw ScriptLoadException(
                        message = "Failed to read script file: ${e.message}",
                        scriptPath = currentScriptPath,
                        cause = e,
                    )
                }

            // Evaluate the script to define functions (with path context for relative loads)
            lisp.evaluate(code, path)

            // Find and store the entry point function
            entryPointFunction = findEntryPointFunction()

            // Set up recomposition callback to re-evaluate app on state changes
            stateManager.setRecompositionCallback {
                reEvaluateApp()
            }

            // Initial evaluation
            reEvaluateApp()
        } catch (e: ScriptLoadException) {
            throw e
        } catch (e: Exception) {
            throw ScriptLoadException(
                message = e.message ?: "Unknown error",
                scriptPath = currentScriptPath,
                cause = e,
            )
        }
    }

    /**
     * Re-evaluate the app function and update the root element.
     */
    private fun reEvaluateApp() {
        val fn = entryPointFunction ?: return
        try {
            val result = fn.evaluate(emptyArray())
            val element = buildUITree(result)
            rootElement.value = element
        } catch (e: Exception) {
            System.err.println("Error re-evaluating app: ${e.message}")
        }
    }

    /**
     * Update the app's entry point function and immediately re-render.
     * Used for iterative development in REPL mode.
     *
     * @param newFunction The new app function to use
     * @return true if update succeeded, false on error
     */
    fun updateApp(newFunction: net.sourceforge.kleinlisp.Function): Boolean =
        try {
            entryPointFunction = newFunction
            reEvaluateApp()
            true
        } catch (e: Exception) {
            System.err.println("Error updating app: ${e.message}")
            false
        }

    /**
     * Check if the runtime is in reactive mode (has a recomposition callback).
     */
    fun isReactiveMode(): Boolean = entryPointFunction != null

    /**
     * Find the entry point function without evaluating it.
     */
    private fun findEntryPointFunction(): net.sourceforge.kleinlisp.Function {
        val entryPoints = listOf("app", "main", "my-app", "root")

        for (name in entryPoints) {
            val atom = env.atomOf(name)
            val value = env.lookupValueOrNull(atom)
            if (value?.asFunction() != null) {
                return value.asFunction().function()
            }
        }

        throw IllegalStateException(
            "No entry point found. Define one of: ${entryPoints.joinToString(", ")}",
        )
    }

    /**
     * Load and execute a Scheme script from a file, returning the root UI element.
     * @throws ScriptLoadException if the script cannot be loaded or evaluated
     */
    fun loadScript(path: Path): UIElement {
        currentScriptPath = path.toString()
        ErrorContext.setScriptPath(currentScriptPath)

        try {
            val code =
                try {
                    path.toFile().readText()
                } catch (e: Exception) {
                    throw ScriptLoadException(
                        message = "Failed to read script file: ${e.message}",
                        scriptPath = currentScriptPath,
                        cause = e,
                    )
                }
            return loadCode(code, path)
        } catch (e: ScriptLoadException) {
            throw e
        } catch (e: Exception) {
            throw ScriptLoadException(
                message = e.message ?: "Unknown error",
                scriptPath = currentScriptPath,
                cause = e,
            )
        }
    }

    /**
     * Load and execute Scheme code from a string, returning the root UI element.
     * @throws ScriptLoadException if the code cannot be evaluated
     */
    fun loadCode(code: String): UIElement = loadCode(code, null)

    /**
     * Load and execute Scheme code from a string with a source path context.
     * The source path enables relative (load ...) calls within the code.
     * @throws ScriptLoadException if the code cannot be evaluated
     */
    fun loadCode(
        code: String,
        sourcePath: Path?,
    ): UIElement {
        ErrorContext.setScriptPath(currentScriptPath)

        try {
            if (sourcePath != null) {
                lisp.evaluate(code, sourcePath)
            } else {
                lisp.evaluate(code)
            }
            return findEntryPoint()
        } catch (e: ScriptLoadException) {
            throw e
        } catch (e: Exception) {
            throw ScriptLoadException(
                message = e.message ?: "Unknown error during script evaluation",
                scriptPath = currentScriptPath,
                cause = e,
            )
        }
    }

    /**
     * Load code in reactive mode - the app function will be re-evaluated
     * whenever state changes or update-app is called.
     * @throws ScriptLoadException if the code cannot be evaluated
     */
    fun loadCodeForReactiveMode(
        code: String,
        scriptPath: String? = null,
    ) {
        currentScriptPath = scriptPath
        ErrorContext.setScriptPath(currentScriptPath)

        try {
            // Evaluate the script to define functions (with path context if available)
            if (scriptPath != null) {
                lisp.evaluate(
                    code,
                    java.nio.file.Paths
                        .get(scriptPath),
                )
            } else {
                lisp.evaluate(code)
            }

            // Find and store the entry point function
            entryPointFunction = findEntryPointFunction()

            // Set up recomposition callback to re-evaluate app on state changes
            stateManager.setRecompositionCallback {
                reEvaluateApp()
            }

            // Initial evaluation
            reEvaluateApp()
        } catch (e: ScriptLoadException) {
            throw e
        } catch (e: Exception) {
            throw ScriptLoadException(
                message = e.message ?: "Unknown error",
                scriptPath = currentScriptPath,
                cause = e,
            )
        }
    }

    /**
     * Evaluate a single Scheme expression.
     */
    fun evaluate(expression: String): LispObject = lisp.evaluate(expression)

    /**
     * Register a custom component.
     */
    fun registerComponent(
        name: String,
        factory: ComponentFactory,
    ) {
        componentRegistry.register(name, factory)
        env.registerFunction(name) { params -> factory.create(params) }
    }

    /**
     * Register a component factory.
     */
    fun registerComponent(factory: ComponentFactory) {
        registerComponent(factory.name, factory)
    }

    /**
     * Get the Lisp environment.
     */
    fun environment(): LispEnvironment = env

    /**
     * Get the underlying Lisp interpreter instance.
     * Useful for sharing state with external tools like REPL servers.
     */
    fun lisp(): Lisp = lisp

    private fun registerCoreFunctions() {
        registerStateFunctions()
        registerUtilityFunctions()
    }

    private fun registerStateFunctions() {
        // state - Create a new state cell with initial value
        env.registerFunction("state") { params ->
            val initialValue = if (params.isNotEmpty()) params[0] else VoidObject.VOID
            JavaObject(stateManager.createCell(initialValue))
        }

        // state-ref - Read current value from state cell or derived cell
        env.registerFunction("state-ref") { params ->
            if (params.isEmpty()) {
                throw StateException(
                    message = "No argument provided",
                    operation = "state-ref",
                    hint = "Usage: (state-ref my-state) - pass a state cell created with (state initial-value)",
                    scriptPath = currentScriptPath,
                )
            }

            // Check for StateCell first
            val cell = params[0].asObject(StateCell::class.java)
            if (cell != null) {
                return@registerFunction cell.value
            }

            // Check for DerivedStateCell
            val derived = params[0].asObject(DerivedStateCell::class.java)
            if (derived != null) {
                return@registerFunction derived.value
            }

            val actualType = params[0]::class.simpleName ?: "unknown"
            throw StateException(
                message = "Expected a state cell or derived cell, got: $actualType",
                operation = "state-ref",
                hint = "Create a state cell with (state value) or derived cell with (derived (lambda () ...))",
                scriptPath = currentScriptPath,
            )
        }

        // derived - Create a derived/computed state cell
        env.registerFunction("derived") { params ->
            if (params.isEmpty()) {
                throw StateException(
                    message = "No computation function provided",
                    operation = "derived",
                    hint = "Usage: (derived (lambda () (+ (state-ref a) (state-ref b))))",
                    scriptPath = currentScriptPath,
                )
            }
            val fn = params[0].asFunction()
            if (fn == null) {
                val actualType = params[0]::class.simpleName ?: "unknown"
                throw StateException(
                    message = "Expected a function, got: $actualType",
                    operation = "derived",
                    hint = "Usage: (derived (lambda () <computation>))",
                    scriptPath = currentScriptPath,
                )
            }
            JavaObject(DerivedStateCell(fn.function()))
        }

        // state-set! - Set new value to state cell
        env.registerFunction("state-set!") { params ->
            if (params.size < 2) {
                throw StateException(
                    message = "Expected 2 arguments, got ${params.size}",
                    operation = "state-set!",
                    hint = "Usage: (state-set! my-state new-value)",
                    scriptPath = currentScriptPath,
                )
            }
            val cell = params[0].asObject(StateCell::class.java)
            if (cell == null) {
                val actualType = params[0]::class.simpleName ?: "unknown"
                throw StateException(
                    message = "First argument must be a state cell, got: $actualType",
                    operation = "state-set!",
                    hint =
                        "Create a state cell first with (define my-state (state \"initial-value\")), " +
                            "then use (state-set! my-state new-value)",
                    scriptPath = currentScriptPath,
                )
            }
            val newValue = params[1]
            cell.value = newValue
            VoidObject.VOID
        }

        // state-update! - Update state cell with function
        env.registerFunction("state-update!") { params ->
            if (params.size < 2) {
                throw StateException(
                    message = "Expected 2 arguments, got ${params.size}",
                    operation = "state-update!",
                    hint = "Usage: (state-update! my-state (lambda (x) (+ x 1)))",
                    scriptPath = currentScriptPath,
                )
            }
            val cell = params[0].asObject(StateCell::class.java)
            if (cell == null) {
                val actualType = params[0]::class.simpleName ?: "unknown"
                throw StateException(
                    message = "First argument must be a state cell, got: $actualType",
                    operation = "state-update!",
                    hint =
                        "Create a state cell first with (define my-state (state 0)), " +
                            "then use (state-update! my-state (lambda (x) (+ x 1)))",
                    scriptPath = currentScriptPath,
                )
            }
            val updateFn = params[1].asFunction()
            if (updateFn == null) {
                val actualType = params[1]::class.simpleName ?: "unknown"
                throw StateException(
                    message = "Second argument must be a function, got: $actualType",
                    operation = "state-update!",
                    hint =
                        "The update function should transform the current value: " +
                            "(state-update! my-state (lambda (x) (+ x 1)))",
                    scriptPath = currentScriptPath,
                )
            }
            val currentValue = cell.value
            cell.value = updateFn.function().evaluate(arrayOf(currentValue))
            VoidObject.VOID
        }
    }

    private fun registerUtilityFunctions() {
        // platform - Get current platform
        env.registerFunction("platform") { _ ->
            JavaObject(platform)
        }

        // platform? - Check if running on specific platform
        env.registerFunction("platform?") { params ->
            val platformName =
                params[0].asAtom()?.toString()
                    ?: params[0].asString()?.value()
                    ?: throw IllegalArgumentException("platform? expects a symbol or string")
            val matches =
                when (platformName.lowercase()) {
                    "android" -> platform == Platform.ANDROID
                    "desktop", "desktop-jvm", "jvm" -> platform == Platform.DESKTOP_JVM
                    else -> false
                }
            if (matches) params[0] else net.sourceforge.kleinlisp.objects.BooleanObject.FALSE
        }

        // update-app - Update the running app's entry point function
        // Used for iterative development in REPL mode
        env.registerFunction("update-app") { params ->
            if (params.isEmpty()) {
                throw IllegalArgumentException(
                    "update-app requires an app function argument.\n" +
                        "Usage: (update-app new-app-function)",
                )
            }

            val fn = params[0].asFunction()
            if (fn == null) {
                val actualType = params[0]::class.simpleName ?: "unknown"
                throw IllegalArgumentException(
                    "update-app expects a function, got: $actualType",
                )
            }

            val success = updateApp(fn.function())
            if (success) {
                net.sourceforge.kleinlisp.objects.BooleanObject.TRUE
            } else {
                net.sourceforge.kleinlisp.objects.BooleanObject.FALSE
            }
        }
    }

    private fun findEntryPoint(): UIElement {
        // Try to find an entry point function
        val entryPoints = listOf("app", "main", "my-app", "root")

        for (name in entryPoints) {
            val atom = env.atomOf(name)
            val value = env.lookupValueOrNull(atom)
            if (value?.asFunction() != null) {
                val result = value.asFunction().function().evaluate(emptyArray())
                return buildUITree(result)
            }
        }

        throw IllegalStateException(
            "No entry point found. Define one of: ${entryPoints.joinToString(", ")}",
        )
    }

    private fun buildUITree(result: LispObject): UIElement {
        // Check if the result is a UIElementWrapper
        val wrapper = result.asObject(UIElementWrapper::class.java)
        if (wrapper != null) {
            return wrapper.element
        }

        throw IllegalStateException(
            "Entry point must return a UI element, got: $result",
        )
    }
}
