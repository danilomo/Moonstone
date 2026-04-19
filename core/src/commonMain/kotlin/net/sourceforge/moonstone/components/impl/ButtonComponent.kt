package net.sourceforge.moonstone.components.impl

import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import net.sourceforge.kleinlisp.objects.BooleanObject
import net.sourceforge.kleinlisp.objects.FunctionObject
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import net.sourceforge.moonstone.runtime.DerivedStateCell
import net.sourceforge.moonstone.runtime.StateCell
import kotlin.reflect.KClass

/**
 * Button component with click handling and multiple styles.
 *
 * Scheme usage:
 * (button #:on-click (lambda () ...) (text #:value "Click Me"))
 * (button #:style 'outlined #:enabled 1 #:on-click handler (text #:value "Submit"))
 */
class ButtonComponent : AbstractComponent() {
    override val name = "button"
    override val acceptsChildren = true

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "on-click" to Any::class,
            "enabled" to Boolean::class,
            "style" to String::class,
        )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        val onClickHandler = element.props["on-click"] as? FunctionObject
        val enabled = resolveBoolean(element.props["enabled"])
        val style = element.props["style"]?.toString() ?: "filled"

        val clickHandler: () -> Unit = {
            onClickHandler?.function()?.evaluate(emptyArray())
        }

        when (style) {
            "filled" ->
                Button(
                    onClick = clickHandler,
                    enabled = enabled,
                ) {
                    element.children.forEach { renderChild(it) }
                }
            "outlined" ->
                OutlinedButton(
                    onClick = clickHandler,
                    enabled = enabled,
                ) {
                    element.children.forEach { renderChild(it) }
                }
            "text" ->
                TextButton(
                    onClick = clickHandler,
                    enabled = enabled,
                ) {
                    element.children.forEach { renderChild(it) }
                }
            "elevated" ->
                ElevatedButton(
                    onClick = clickHandler,
                    enabled = enabled,
                ) {
                    element.children.forEach { renderChild(it) }
                }
            "tonal" ->
                FilledTonalButton(
                    onClick = clickHandler,
                    enabled = enabled,
                ) {
                    element.children.forEach { renderChild(it) }
                }
            else ->
                Button(
                    onClick = clickHandler,
                    enabled = enabled,
                ) {
                    element.children.forEach { renderChild(it) }
                }
        }
    }

    private fun resolveBoolean(value: Any?): Boolean {
        if (value == null) return true // Default to enabled
        return when (value) {
            is StateCell -> lispObjectToBoolean(value.value)
            is DerivedStateCell -> lispObjectToBoolean(value.value)
            is Boolean -> value
            is Number -> value.toInt() != 0
            else -> ModifierBuilder.isTruthy(value)
        }
    }

    private fun lispObjectToBoolean(lispValue: net.sourceforge.kleinlisp.LispObject): Boolean =
        when {
            lispValue.asInt() != null -> lispValue.asInt().value != 0
            lispValue.asObject(Boolean::class.java) != null ->
                lispValue.asObject(Boolean::class.java) == true
            lispValue == BooleanObject.TRUE -> true
            lispValue == BooleanObject.FALSE -> false
            else -> ModifierBuilder.isTruthy(lispValue.asObject())
        }
}
