package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import kotlin.reflect.KClass

/**
 * View component for use within switch-view. Wraps content with a value identifier.
 *
 * Scheme usage:
 * (view #:value 0 (column ...))
 * (view #:value "home" (home-content))
 */
class ViewComponent : AbstractComponent() {
    override val name = "view"
    override val acceptsChildren = true

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "value" to Any::class,
            "padding" to Number::class,
            "fill-max-size" to Boolean::class,
            "fill-max-width" to Boolean::class,
            "fill-max-height" to Boolean::class,
        )

    override val requiredProps = listOf("value")

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        val modifier = ModifierBuilder.build(element.props)

        Box(modifier = modifier) {
            element.children.forEach { child ->
                renderChild(child)
            }
        }
    }
}
