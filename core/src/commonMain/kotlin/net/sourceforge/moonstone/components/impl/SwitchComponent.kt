package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import net.sourceforge.moonstone.runtime.DerivedStateCell
import net.sourceforge.moonstone.runtime.StateCell
import net.sourceforge.kleinlisp.objects.BooleanObject
import net.sourceforge.kleinlisp.objects.FunctionObject
import net.sourceforge.kleinlisp.objects.IntObject
import kotlin.reflect.KClass

/**
 * Switch (toggle) component with optional label children.
 *
 * Scheme usage:
 * (switch #:checked enabled-state #:on-change (lambda (v) (state-set! enabled-state v)))
 * (switch #:checked dark-mode #:on-change handler (text #:value "Dark Mode"))
 */
class SwitchComponent : AbstractComponent() {
    override val name = "switch"
    override val acceptsChildren = true

    override val propTypes: Map<String, KClass<*>> = mapOf(
        "checked" to Any::class,
        "on-change" to Any::class,
        "enabled" to Boolean::class
    )

    @Composable
    override fun Render(element: UIElement, renderChild: @Composable (UIElement) -> Unit) {
        val checked = resolveBoolean(element.props["checked"])
        val onChangeHandler = element.props["on-change"] as? FunctionObject
        val enabled = element.props["enabled"]?.let { ModifierBuilder.isTruthy(it) } ?: true

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            element.children.forEach { renderChild(it) }
            Switch(
                checked = checked,
                onCheckedChange = { newValue ->
                    val lispValue = IntObject.valueOf(if (newValue) 1 else 0)
                    onChangeHandler?.function()?.evaluate(arrayOf(lispValue))
                },
                enabled = enabled
            )
        }
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
