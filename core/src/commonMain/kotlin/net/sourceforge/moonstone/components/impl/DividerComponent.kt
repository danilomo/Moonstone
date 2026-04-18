package net.sourceforge.moonstone.components.impl

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import kotlin.reflect.KClass

/**
 * Divider component for separating content.
 * Supports horizontal (default) and vertical orientations.
 *
 * Scheme usage:
 * (divider)                                    ; Horizontal divider
 * (divider #:orientation 'vertical)           ; Vertical divider
 * (divider #:thickness 2 #:color "gray")      ; Custom thickness and color
 */
class DividerComponent : AbstractComponent() {
    override val name = "divider"
    override val acceptsChildren = false

    override val propTypes: Map<String, KClass<*>> = mapOf(
        "orientation" to String::class,
        "thickness" to Number::class,
        "color" to String::class,
        "padding" to Number::class,
        "padding-horizontal" to Number::class,
        "padding-vertical" to Number::class
    )

    @Composable
    override fun Render(element: UIElement, renderChild: @Composable (UIElement) -> Unit) {
        val modifier = ModifierBuilder.build(element.props)
        val orientation = element.props["orientation"]?.toString() ?: "horizontal"
        val thickness = (element.props["thickness"] as? Number)?.toInt()?.dp ?: 1.dp
        val color = ModifierBuilder.parseColor(element.props["color"])
            ?: MaterialTheme.colorScheme.outlineVariant

        when (orientation) {
            "vertical" -> {
                VerticalDivider(
                    modifier = modifier,
                    thickness = thickness,
                    color = color
                )
            }
            else -> {
                // Default to horizontal
                HorizontalDivider(
                    modifier = modifier,
                    thickness = thickness,
                    color = color
                )
            }
        }
    }
}
