package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import kotlin.reflect.KClass

/**
 * Column layout component for vertical arrangements.
 */
class ColumnComponent : AbstractComponent() {
    override val name = "column"

    override val propTypes: Map<String, KClass<*>> = mapOf(
        "padding" to Number::class,
        "spacing" to Number::class,
        "vertical-arrangement" to String::class,
        "horizontal-alignment" to String::class,
        "fill-max-size" to Boolean::class,
        "fill-max-width" to Boolean::class,
        "fill-max-height" to Boolean::class,
        "width" to Number::class,
        "height" to Number::class
    )

    @Composable
    override fun Render(element: UIElement, renderChild: @Composable (UIElement) -> Unit) {
        val modifier = ModifierBuilder.build(element.props)
        val spacing = (element.props["spacing"] as? Number)?.toInt() ?: 0
        val arrangement = parseVerticalArrangement(element.props["vertical-arrangement"], spacing)
        val alignment = parseHorizontalAlignment(element.props["horizontal-alignment"])

        Column(
            modifier = modifier,
            verticalArrangement = arrangement,
            horizontalAlignment = alignment
        ) {
            element.children.forEach { child ->
                renderChild(child)
            }
        }
    }

    private fun parseVerticalArrangement(value: Any?, spacing: Int): Arrangement.Vertical {
        return when (value) {
            "top" -> Arrangement.Top
            "center" -> Arrangement.Center
            "bottom" -> Arrangement.Bottom
            "space-between" -> Arrangement.SpaceBetween
            "space-around" -> Arrangement.SpaceAround
            "space-evenly" -> Arrangement.SpaceEvenly
            else -> if (spacing > 0) Arrangement.spacedBy(spacing.dp) else Arrangement.Top
        }
    }

    private fun parseHorizontalAlignment(value: Any?): Alignment.Horizontal = when (value) {
        "start" -> Alignment.Start
        "center" -> Alignment.CenterHorizontally
        "end" -> Alignment.End
        else -> Alignment.Start
    }
}
