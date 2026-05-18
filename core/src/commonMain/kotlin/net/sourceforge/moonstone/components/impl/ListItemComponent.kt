package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import kotlin.reflect.KClass

/**
 * List item component for use within LazyColumn and LazyRow.
 *
 * Scheme usage:
 * (list-item #:key "unique-id"
 *   (text #:value "Content"))
 */
class ListItemComponent : AbstractComponent() {
    override val name = "list-item"
    override val acceptsChildren = true

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "key" to String::class,
            "padding" to Number::class,
            "fill-max-width" to Boolean::class,
        )

    override val requiredProps = emptyList<String>()

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
