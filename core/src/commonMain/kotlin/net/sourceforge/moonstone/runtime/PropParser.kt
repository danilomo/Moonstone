package net.sourceforge.moonstone.runtime

import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.components.UIElementWrapper
import net.sourceforge.moonstone.error.ErrorContext
import net.sourceforge.moonstone.error.ValidationWarning
import kotlin.reflect.KClass

/**
 * Result of parsing with validation.
 */
data class ParseResult(
    val props: Map<String, Any?>,
    val children: List<UIElement>,
    val warnings: List<ValidationWarning>,
)

/**
 * Parses keyword arguments from Scheme function calls.
 * Separates props (keyword arguments) from children (UI elements).
 */
class PropParser {
    /**
     * Parse parameters into props map and children list.
     *
     * Input: [#:padding, 16, #:alignment, 'center, <ui-element>, <ui-element>]
     * Output: Pair(
     *   mapOf("padding" to 16, "alignment" to "center"),
     *   listOf(<ui-element>, <ui-element>)
     * )
     */
    fun parse(params: Array<LispObject>): Pair<Map<String, Any?>, List<UIElement>> {
        val props = mutableMapOf<String, Any?>()
        val children = mutableListOf<UIElement>()

        var i = 0
        while (i < params.size) {
            i = processParam(params, i, props, children)
            i++
        }

        return props to children
    }

    private fun processParam(
        params: Array<LispObject>,
        index: Int,
        props: MutableMap<String, Any?>,
        children: MutableList<UIElement>,
    ): Int {
        val param = params[index]
        return when {
            param.asKeyword() != null -> processKeywordParam(params, index, props)
            isUIElement(param) -> processUIElementParam(param, children, index)
            else -> processOtherParam(param, children, index)
        }
    }

    private fun processKeywordParam(
        params: Array<LispObject>,
        index: Int,
        props: MutableMap<String, Any?>,
    ): Int {
        val key = params[index].asKeyword().name()
        return if (index + 1 < params.size) {
            val value = params[index + 1]
            props[key] = convertValue(value)
            index + 1
        } else {
            index
        }
    }

    private fun processUIElementParam(
        param: LispObject,
        children: MutableList<UIElement>,
        index: Int,
    ): Int {
        children.add(extractUIElement(param))
        return index
    }

    private fun processOtherParam(
        param: LispObject,
        children: MutableList<UIElement>,
        index: Int,
    ): Int {
        val converted = tryConvertToElement(param)
        if (converted != null) {
            children.add(converted)
        } else {
            tryExtractElementList(param)?.let { children.addAll(it) }
        }
        return index
    }

    private fun convertValue(value: LispObject): Any? =
        when {
            value.asInt() != null -> value.asInt().value
            value.asDouble() != null -> value.asDouble().value
            value.asString() != null -> value.asString().value()
            value.asAtom() != null -> value.asAtom().toString()
            value.asFunction() != null -> value.asFunction()
            safeAsList(value) != null -> convertList(safeAsList(value)!!)
            value.asObject(UIElementWrapper::class.java) != null ->
                value.asObject(UIElementWrapper::class.java).element
            value.asObject(StateCell::class.java) != null ->
                value.asObject(StateCell::class.java)
            value.asObject(DerivedStateCell::class.java) != null ->
                value.asObject(DerivedStateCell::class.java)
            else -> value.asObject()
        }

    /**
     * Safe wrapper for asList() that handles nil represented as JavaObject(null).
     * KleinLisp can represent '() as JavaObject(null), causing NPE in asList().
     */
    private fun safeAsList(obj: LispObject): net.sourceforge.kleinlisp.objects.ListObject? =
        try {
            obj.asList()
        } catch (_: NullPointerException) {
            net.sourceforge.kleinlisp.objects.ListObject.NIL
        }

    private fun convertList(list: net.sourceforge.kleinlisp.objects.ListObject): List<Any?> {
        val result = mutableListOf<Any?>()
        var current: net.sourceforge.kleinlisp.objects.ListObject? = list
        while (current != null && current !== net.sourceforge.kleinlisp.objects.ListObject.NIL) {
            result.add(convertValue(current.car()))
            current = current.cdr()?.asList()
        }
        return result
    }

    private fun isUIElement(obj: LispObject): Boolean = obj.asObject(UIElementWrapper::class.java) != null

    private fun extractUIElement(obj: LispObject): UIElement = obj.asObject(UIElementWrapper::class.java)!!.element

    private fun tryConvertToElement(obj: LispObject): UIElement? {
        val wrapper = obj.asObject(UIElementWrapper::class.java)
        return wrapper?.element
    }

    /**
     * Try to extract a list of UI elements from a ListObject.
     * Returns null if the list doesn't contain UI elements.
     * Handles KleinLisp nil represented as JavaObject(null).
     */
    private fun tryExtractElementList(obj: LispObject): List<UIElement>? {
        val list = safeAsList(obj) ?: return null
        if (list === net.sourceforge.kleinlisp.objects.ListObject.NIL) {
            return emptyList()
        }

        val elements = mutableListOf<UIElement>()
        var current: net.sourceforge.kleinlisp.objects.ListObject? = list

        while (current != null && current !== net.sourceforge.kleinlisp.objects.ListObject.NIL) {
            val item = current.car()
            val wrapper = item.asObject(UIElementWrapper::class.java)
            if (wrapper != null) {
                elements.add(wrapper.element)
            } else {
                // If any item is not a UIElement, this isn't a list of children
                return null
            }
            current = current.cdr()?.asList()
        }

        return elements
    }

    /**
     * Parse parameters with validation against expected props.
     *
     * @param params The raw parameters from the Scheme call
     * @param componentName The name of the component (for error messages)
     * @param expectedProps Map of expected prop names to their types
     * @param requiredProps List of required prop names
     * @return ParseResult containing props, children, and any validation warnings
     */
    fun parseWithValidation(
        params: Array<LispObject>,
        componentName: String,
        expectedProps: Map<String, KClass<*>> = emptyMap(),
        requiredProps: List<String> = emptyList(),
    ): ParseResult {
        val (props, children) = parse(params)
        val warnings = mutableListOf<ValidationWarning>()
        val scriptPath = ErrorContext.getScriptPath()

        // Check for unknown props
        val knownProps = expectedProps.keys + COMMON_MODIFIER_PROPS
        for (propName in props.keys) {
            if (propName !in knownProps) {
                warnings.add(
                    ValidationWarning(
                        componentName = componentName,
                        message = "Unknown property '$propName'. Known properties: ${knownProps.sorted().joinToString(
                            ", ",
                        )}",
                        propName = propName,
                        scriptPath = scriptPath,
                    ),
                )
            }
        }

        // Check for missing required props
        for (required in requiredProps) {
            if (props[required] == null) {
                warnings.add(
                    ValidationWarning(
                        componentName = componentName,
                        message = "Missing required property '$required'",
                        propName = required,
                        scriptPath = scriptPath,
                    ),
                )
            }
        }

        // Check prop types
        for ((propName, expectedType) in expectedProps) {
            val value = props[propName]
            if (value != null && !isTypeCompatible(value, expectedType)) {
                warnings.add(
                    ValidationWarning(
                        componentName = componentName,
                        message =
                            "Property '$propName' has unexpected type. " +
                                "Expected: ${expectedType.simpleName}, got: ${value::class.simpleName}",
                        propName = propName,
                        scriptPath = scriptPath,
                    ),
                )
            }
        }

        // Report warnings through ErrorContext
        warnings.forEach { ErrorContext.reportWarning(it) }

        return ParseResult(props, children, warnings)
    }

    /**
     * Check if a value is compatible with an expected type.
     */
    private fun isTypeCompatible(
        value: Any,
        expectedType: KClass<*>,
    ): Boolean =
        when {
            expectedType == Any::class -> true
            expectedType == Number::class -> value is Number
            expectedType == String::class -> value is String || value.toString().isNotEmpty()
            expectedType == Boolean::class -> value is Boolean || value is Number
            expectedType.isInstance(value) -> true
            else -> false
        }

    companion object {
        /**
         * Common modifier properties that all components can accept.
         */
        val COMMON_MODIFIER_PROPS =
            setOf(
                "padding",
                "padding-horizontal",
                "padding-vertical",
                "fill-max-size",
                "fill-max-width",
                "fill-max-height",
                "width",
                "height",
                "background",
            )
    }
}
