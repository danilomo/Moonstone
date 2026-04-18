package net.sourceforge.moonstone.error

/**
 * Thread-local context for tracking the current script path and component stack.
 * Used to enrich error messages with location information.
 */
object ErrorContext {
    @PublishedApi
    internal val currentScriptPath = ThreadLocal<String?>()
    @PublishedApi
    internal val componentStack = ThreadLocal<MutableList<String>>()
    @PublishedApi
    internal val warningHandler = ThreadLocal<((ValidationWarning) -> Unit)?>()

    /**
     * Get the current script path being processed.
     */
    fun getScriptPath(): String? = currentScriptPath.get()

    /**
     * Set the current script path.
     */
    fun setScriptPath(path: String?) {
        currentScriptPath.set(path)
    }

    /**
     * Get the current component stack.
     */
    fun getComponentStack(): List<String> = componentStack.get()?.toList() ?: emptyList()

    /**
     * Push a component onto the stack. Call when entering a component's Render function.
     */
    fun pushComponent(name: String) {
        val stack = componentStack.get() ?: mutableListOf<String>().also { componentStack.set(it) }
        stack.add(name)
    }

    /**
     * Pop a component from the stack. Call when exiting a component's Render function.
     */
    fun popComponent() {
        componentStack.get()?.removeLastOrNull()
    }

    /**
     * Get the current component name (top of stack).
     */
    fun getCurrentComponent(): String? = componentStack.get()?.lastOrNull()

    /**
     * Set a warning handler to receive validation warnings.
     */
    fun setWarningHandler(handler: ((ValidationWarning) -> Unit)?) {
        warningHandler.set(handler)
    }

    /**
     * Report a validation warning.
     */
    fun reportWarning(warning: ValidationWarning) {
        warningHandler.get()?.invoke(warning)
    }

    /**
     * Execute a block with the given script path context.
     */
    inline fun <T> withScriptPath(path: String?, block: () -> T): T {
        val previous = currentScriptPath.get()
        currentScriptPath.set(path)
        try {
            return block()
        } finally {
            currentScriptPath.set(previous)
        }
    }

    /**
     * Execute a block with the given component on the stack.
     */
    inline fun <T> withComponent(name: String, block: () -> T): T {
        pushComponent(name)
        try {
            return block()
        } finally {
            popComponent()
        }
    }

    /**
     * Clear all context (call when starting a new script load).
     */
    fun clear() {
        currentScriptPath.remove()
        componentStack.remove()
        warningHandler.remove()
    }
}

/**
 * Represents a validation warning that doesn't prevent rendering.
 */
data class ValidationWarning(
    val componentName: String,
    val message: String,
    val propName: String? = null,
    val scriptPath: String? = null
) {
    override fun toString(): String = buildString {
        append("[Warning] ")
        append(message)
        append(" (component: $componentName")
        propName?.let { append(", prop: $it") }
        scriptPath?.let { append(", script: $it") }
        append(")")
    }
}
