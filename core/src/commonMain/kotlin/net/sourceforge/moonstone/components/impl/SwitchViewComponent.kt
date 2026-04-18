package net.sourceforge.moonstone.components.impl

import androidx.compose.runtime.Composable
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.runtime.DerivedStateCell
import net.sourceforge.moonstone.runtime.StateCell
import net.sourceforge.kleinlisp.LispObject
import kotlin.reflect.KClass

/**
 * Switch view component for conditionally showing one of multiple views based on state.
 *
 * Scheme usage:
 * (switch-view #:selected tab-state
 *   (view #:value 0 (home-content))
 *   (view #:value 1 (search-content))
 *   (view #:value 2 (profile-content)))
 */
class SwitchViewComponent : AbstractComponent() {
    override val name = "switch-view"
    override val acceptsChildren = true

    override val propTypes: Map<String, KClass<*>> = mapOf(
        "selected" to Any::class
    )

    @Composable
    override fun Render(element: UIElement, renderChild: @Composable (UIElement) -> Unit) {
        val selectedValue = element.props["selected"]
        val currentSelection = resolveSelection(selectedValue)

        // Find the child view that matches the selected value
        val matchingView = element.children.find { child ->
            val childValue = child.props["value"]
            when {
                currentSelection is Number && childValue is Number ->
                    currentSelection.toInt() == childValue.toInt()
                else -> currentSelection == childValue
            }
        }

        // Render the matching view's children
        matchingView?.children?.forEach { child ->
            renderChild(child)
        }
    }

    private fun resolveSelection(selectedValue: Any?): Any? {
        return when (selectedValue) {
            is StateCell -> extractValue(selectedValue.value)
            is DerivedStateCell -> extractValue(selectedValue.value)
            is Number -> selectedValue.toInt()
            is String -> selectedValue
            else -> selectedValue
        }
    }

    private fun extractValue(v: LispObject): Any? {
        return when {
            v.asInt() != null -> v.asInt().value
            v.asString() != null -> v.asString().value()
            else -> v.asObject()
        }
    }
}
