package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import kotlin.reflect.KClass

/**
 * Row layout component for horizontal arrangements.
 */
class RowComponent : AbstractComponent() {
    override val name = "row"

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "padding" to Number::class,
            "spacing" to Number::class,
            "horizontal-arrangement" to String::class,
            "vertical-alignment" to String::class,
            "fill-max-size" to Boolean::class,
            "fill-max-width" to Boolean::class,
            "fill-max-height" to Boolean::class,
            "width" to Number::class,
            "height" to Number::class,
            "modifier" to Any::class,
        )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        val modifier = ModifierBuilder.build(element.props)
        val spacing = (element.props["spacing"] as? Number)?.toInt() ?: 0
        val arrangement = parseHorizontalArrangement(element.props["horizontal-arrangement"], spacing)
        val alignment = parseVerticalAlignment(element.props["vertical-alignment"])

        Row(
            modifier = modifier,
            horizontalArrangement = arrangement,
            verticalAlignment = alignment,
        ) {
            element.children.forEach { child ->
                val weight = extractWeight(child)
                if (weight != null) {
                    Box(modifier = Modifier.weight(weight)) { renderChild(child) }
                } else {
                    renderChild(child)
                }
            }
        }
    }

    private fun extractWeight(child: UIElement): Float? {
        val modList = child.props["modifier"] as? List<*> ?: return null
        for (entry in modList) {
            val pair = entry as? List<*> ?: continue
            if (pair.size >= 2 && pair[0]?.toString() == "weight") {
                return (pair[1] as? Number)?.toFloat()
            }
        }
        return null
    }

    private fun parseHorizontalArrangement(
        value: Any?,
        spacing: Int,
    ): Arrangement.Horizontal =
        when (value) {
            "start" -> Arrangement.Start
            "center" -> Arrangement.Center
            "end" -> Arrangement.End
            "space-between" -> Arrangement.SpaceBetween
            "space-around" -> Arrangement.SpaceAround
            "space-evenly" -> Arrangement.SpaceEvenly
            else -> if (spacing > 0) Arrangement.spacedBy(spacing.dp) else Arrangement.Start
        }

    private fun parseVerticalAlignment(value: Any?): Alignment.Vertical =
        when (value) {
            "top" -> Alignment.Top
            "center" -> Alignment.CenterVertically
            "bottom" -> Alignment.Bottom
            else -> Alignment.Top
        }
}
