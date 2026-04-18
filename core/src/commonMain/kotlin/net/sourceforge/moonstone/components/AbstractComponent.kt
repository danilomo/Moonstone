package net.sourceforge.moonstone.components

import net.sourceforge.moonstone.error.ErrorContext
import net.sourceforge.moonstone.error.MissingPropException
import net.sourceforge.moonstone.error.PropValidationException
import net.sourceforge.moonstone.error.ValidationWarning
import net.sourceforge.moonstone.runtime.ParseResult
import net.sourceforge.moonstone.runtime.PropParser
import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.objects.JavaObject

/**
 * Base implementation for components providing common functionality.
 */
abstract class AbstractComponent : ComponentFactory {
    protected val propParser = PropParser()

    /**
     * When true, validation errors will throw exceptions.
     * When false (default), validation errors are reported as warnings.
     */
    var strictMode: Boolean = false

    /**
     * Callback for handling validation warnings.
     * If not set, warnings are reported through ErrorContext.
     */
    var warningHandler: ((ValidationWarning) -> Unit)? = null

    override fun create(params: Array<LispObject>): LispObject {
        // Use parseWithValidation for enhanced error reporting
        val result = propParser.parseWithValidation(
            params = params,
            componentName = name,
            expectedProps = propTypes,
            requiredProps = requiredProps
        )

        // Report warnings through custom handler if set
        result.warnings.forEach { warning ->
            warningHandler?.invoke(warning)
        }

        // In strict mode, throw on missing required props
        if (strictMode) {
            for (required in requiredProps) {
                if (result.props[required] == null) {
                    throw MissingPropException(
                        componentName = name,
                        propName = required,
                        availableProps = propTypes.keys.toList() + PropParser.COMMON_MODIFIER_PROPS.toList(),
                        scriptPath = ErrorContext.getScriptPath()
                    )
                }
            }
        }

        // Validate using legacy method for backwards compatibility
        validateProps(result.props)
        validateChildren(result.children)

        val element = ComponentElement(
            type = name,
            props = result.props,
            children = result.children
        )

        return JavaObject(UIElementWrapper(element))
    }

    /**
     * Legacy prop validation. Override to add custom validation logic.
     */
    protected open fun validateProps(props: Map<String, Any?>) {
        for (required in requiredProps) {
            if (props[required] == null) {
                throw MissingPropException(
                    componentName = name,
                    propName = required,
                    availableProps = propTypes.keys.toList() + PropParser.COMMON_MODIFIER_PROPS.toList(),
                    scriptPath = ErrorContext.getScriptPath()
                )
            }
        }
    }

    /**
     * Validate children. Override to add custom child validation logic.
     */
    protected open fun validateChildren(children: List<UIElement>) {
        if (!acceptsChildren && children.isNotEmpty()) {
            throw PropValidationException(
                componentName = name,
                propName = "children",
                value = "${children.size} children",
                hint = "Component '$name' does not accept children",
                scriptPath = ErrorContext.getScriptPath()
            )
        }
    }
}
