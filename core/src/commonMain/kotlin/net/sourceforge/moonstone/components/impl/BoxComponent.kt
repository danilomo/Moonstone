package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import kotlin.reflect.KClass

/**
 * Box layout component.
 */
class BoxComponent : AbstractComponent() {
    override val name = "box"

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "padding" to Number::class,
            "content-alignment" to String::class,
            "fill-max-size" to Boolean::class,
            "fill-max-width" to Boolean::class,
            "fill-max-height" to Boolean::class,
            "width" to Number::class,
            "height" to Number::class,
            "background" to String::class,
        )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        val modifier = ModifierBuilder.build(element.props)
        val alignment = parseAlignment(element.props["content-alignment"])

        Box(
            modifier = modifier,
            contentAlignment = alignment,
        ) {
            element.children.forEach { child ->
                renderChild(child)
            }
        }
    }

    private fun parseAlignment(value: Any?): Alignment =
        when (value) {
            "center" -> Alignment.Center
            "top-start" -> Alignment.TopStart
            "top-center" -> Alignment.TopCenter
            "top-end" -> Alignment.TopEnd
            "center-start" -> Alignment.CenterStart
            "center-end" -> Alignment.CenterEnd
            "bottom-start" -> Alignment.BottomStart
            "bottom-center" -> Alignment.BottomCenter
            "bottom-end" -> Alignment.BottomEnd
            else -> Alignment.TopStart
        }
}
