package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import net.sourceforge.moonstone.runtime.DerivedStateCell
import net.sourceforge.moonstone.runtime.StateCell
import net.sourceforge.kleinlisp.objects.BooleanObject
import net.sourceforge.kleinlisp.objects.FunctionObject
import kotlin.reflect.KClass

/**
 * Radio button component with optional label children.
 *
 * Scheme usage (reactive with state cell):
 * (radio-button #:selected my-state #:value 1 #:on-select (lambda () (state-set! my-state 1))
 *   (text #:value "Option A"))
 *
 * The #:selected prop can be:
 * - A StateCell: compared against #:value prop for reactive selection
 * - A boolean/number: static selection (1 = selected, 0 = not selected)
 */
class RadioButtonComponent : AbstractComponent() {
    override val name = "radio-button"
    override val acceptsChildren = true

    override val propTypes: Map<String, KClass<*>> = mapOf(
        "selected" to Any::class,
        "value" to Any::class,
        "on-select" to Any::class,
        "enabled" to Boolean::class
    )

    @Composable
    override fun Render(element: UIElement, renderChild: @Composable (UIElement) -> Unit) {
        val selected = resolveSelected(element.props["selected"], element.props["value"])
        val onSelectHandler = element.props["on-select"] as? FunctionObject
        val enabled = element.props["enabled"]?.let { ModifierBuilder.isTruthy(it) } ?: true

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            RadioButton(
                selected = selected,
                onClick = {
                    onSelectHandler?.function()?.evaluate(emptyArray())
                },
                enabled = enabled
            )
            element.children.forEach { renderChild(it) }
        }
    }

    /**
     * Resolve whether this radio button is selected.
     * If selectedProp is a StateCell/DerivedStateCell and valueProp is provided, compare them.
     * Otherwise, treat selectedProp as a boolean.
     */
    private fun resolveSelected(selectedProp: Any?, valueProp: Any?): Boolean {
        // If selected is a StateCell and we have a value prop, compare them
        if (selectedProp is StateCell && valueProp != null) {
            val stateValue = extractValue(selectedProp.value)
            val compareValue = valueProp
            return valuesEqual(stateValue, compareValue)
        }

        // If selected is a DerivedStateCell and we have a value prop, compare them
        if (selectedProp is DerivedStateCell && valueProp != null) {
            val stateValue = extractValue(selectedProp.value)
            val compareValue = valueProp
            return valuesEqual(stateValue, compareValue)
        }

        // Otherwise treat as boolean
        return resolveBoolean(selectedProp)
    }

    private fun extractValue(lispValue: net.sourceforge.kleinlisp.LispObject): Any? {
        return when {
            lispValue.asInt() != null -> lispValue.asInt().value
            lispValue.asDouble() != null -> lispValue.asDouble().value
            lispValue.asString() != null -> lispValue.asString().value()
            lispValue.asAtom() != null -> lispValue.asAtom().toString()
            else -> lispValue.asObject()
        }
    }

    private fun valuesEqual(a: Any?, b: Any?): Boolean {
        if (a == b) return true
        // Compare numbers flexibly
        if (a is Number && b is Number) {
            return a.toDouble() == b.toDouble()
        }
        // Compare strings
        return a?.toString() == b?.toString()
    }

    private fun resolveBoolean(value: Any?): Boolean {
        return when (value) {
            is StateCell -> lispObjectToBoolean(value.value)
            is DerivedStateCell -> lispObjectToBoolean(value.value)
            is Boolean -> value
            is Number -> value.toInt() != 0
            else -> ModifierBuilder.isTruthy(value)
        }
    }

    private fun lispObjectToBoolean(lispValue: net.sourceforge.kleinlisp.LispObject): Boolean {
        return when {
            lispValue.asInt() != null -> lispValue.asInt().value != 0
            lispValue.asObject(Boolean::class.java) != null ->
                lispValue.asObject(Boolean::class.java) == true
            lispValue == BooleanObject.TRUE -> true
            lispValue == BooleanObject.FALSE -> false
            else -> ModifierBuilder.isTruthy(lispValue.asObject())
        }
    }
}
