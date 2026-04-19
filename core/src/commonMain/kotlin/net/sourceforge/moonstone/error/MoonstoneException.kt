package net.sourceforge.moonstone.error

/**
 * Base exception for all Moonstone errors.
 * Provides rich context for debugging including component name, prop name, and script path.
 */
sealed class MoonstoneException(
    message: String,
    val componentName: String? = null,
    val propName: String? = null,
    val scriptPath: String? = null,
    cause: Throwable? = null,
) : RuntimeException(buildMessage(message, componentName, propName, scriptPath), cause) {
    companion object {
        private fun buildMessage(
            message: String,
            componentName: String?,
            propName: String?,
            scriptPath: String?,
        ): String =
            buildString {
                append(message)
                val context = mutableListOf<String>()
                componentName?.let { context.add("component: $it") }
                propName?.let { context.add("prop: $it") }
                scriptPath?.let { context.add("script: $it") }
                if (context.isNotEmpty()) {
                    append(" [")
                    append(context.joinToString(", "))
                    append("]")
                }
            }
    }
}

/**
 * Thrown when a required property is missing from a component.
 */
class MissingPropException(
    componentName: String,
    propName: String,
    val availableProps: List<String> = emptyList(),
    scriptPath: String? = null,
) : MoonstoneException(
        message =
            buildString {
                append("Missing required property '$propName' for component '$componentName'.")
                if (availableProps.isNotEmpty()) {
                    append("\nAvailable properties: ${availableProps.joinToString(", ")}")
                }
            },
        componentName = componentName,
        propName = propName,
        scriptPath = scriptPath,
    )

/**
 * Thrown when a property value has an invalid type.
 */
class InvalidPropTypeException(
    componentName: String,
    propName: String,
    expectedType: String,
    actualType: String,
    actualValue: Any? = null,
    scriptPath: String? = null,
) : MoonstoneException(
        message =
            buildString {
                append("Invalid type for property '$propName' in component '$componentName'.")
                append("\nExpected: $expectedType")
                append("\nActual: $actualType")
                actualValue?.let { append(" (value: $it)") }
            },
        componentName = componentName,
        propName = propName,
        scriptPath = scriptPath,
    )

/**
 * Thrown when a property value fails validation.
 */
class PropValidationException(
    componentName: String,
    propName: String,
    val value: Any?,
    val validValues: List<String>? = null,
    val hint: String? = null,
    scriptPath: String? = null,
) : MoonstoneException(
        message =
            buildString {
                append("Invalid value for property '$propName' in component '$componentName'")
                value?.let { append(": '$it'") }
                append(".")
                validValues?.let {
                    append("\nValid values: ${it.joinToString(", ")}")
                }
                hint?.let { append("\nHint: $it") }
            },
        componentName = componentName,
        propName = propName,
        scriptPath = scriptPath,
    )

/**
 * Thrown when a component fails to render.
 */
class ComponentRenderException(
    componentName: String,
    message: String,
    val componentStack: List<String> = emptyList(),
    scriptPath: String? = null,
    cause: Throwable? = null,
) : MoonstoneException(
        message =
            buildString {
                append("Render error in component '$componentName': $message")
                if (componentStack.isNotEmpty()) {
                    append("\nComponent stack:")
                    componentStack.forEachIndexed { index, component ->
                        append("\n  ${"  ".repeat(index)}-> $component")
                    }
                }
            },
        componentName = componentName,
        scriptPath = scriptPath,
        cause = cause,
    )

/**
 * Thrown when an unknown component is referenced.
 */
class UnknownComponentException(
    componentName: String,
    val availableComponents: List<String> = emptyList(),
    scriptPath: String? = null,
) : MoonstoneException(
        message =
            buildString {
                append("Unknown component: '$componentName'.")
                if (availableComponents.isNotEmpty()) {
                    append("\nAvailable components: ${availableComponents.sorted().joinToString(", ")}")
                }
            },
        componentName = componentName,
        scriptPath = scriptPath,
    )

/**
 * Thrown when loading a script fails.
 */
class ScriptLoadException(
    override val message: String,
    scriptPath: String? = null,
    val lineNumber: Int? = null,
    cause: Throwable? = null,
) : MoonstoneException(
        message =
            buildString {
                append("Failed to load script")
                scriptPath?.let { append(" '$it'") }
                lineNumber?.let { append(" at line $it") }
                append(": $message")
            },
        scriptPath = scriptPath,
        cause = cause,
    )

/**
 * Thrown when a state operation fails.
 */
class StateException(
    message: String,
    val operation: String,
    val hint: String? = null,
    scriptPath: String? = null,
    cause: Throwable? = null,
) : MoonstoneException(
        message =
            buildString {
                append("State error in '$operation': $message")
                hint?.let { append("\nHint: $it") }
            },
        scriptPath = scriptPath,
        cause = cause,
    )
