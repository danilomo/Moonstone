package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import kotlin.reflect.KClass

/**
 * Surface component for Material Design surfaces.
 */
class SurfaceComponent : AbstractComponent() {
    override val name = "surface"

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "fill-max-size" to Boolean::class,
            "fill-max-width" to Boolean::class,
            "fill-max-height" to Boolean::class,
            "color" to String::class,
            "shape" to String::class,
            "elevation" to Number::class,
            "padding" to Number::class,
        )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        val modifier = ModifierBuilder.build(element.props)
        val color =
            ModifierBuilder.parseColor(element.props["color"])
                ?: MaterialTheme.colorScheme.surface
        val shape = parseShape(element.props["shape"])
        val elevation = (element.props["elevation"] as? Number)?.toInt()?.dp ?: 0.dp
        val innerPadding = (element.props["content-padding"] as? Number)?.toInt()?.dp ?: 0.dp

        Surface(
            modifier = modifier,
            color = color,
            shape = shape,
            tonalElevation = elevation,
        ) {
            Box(modifier = if (innerPadding.value > 0) Modifier.padding(innerPadding) else Modifier) {
                element.children.forEach { child ->
                    renderChild(child)
                }
            }
        }
    }

    @Composable
    private fun parseShape(value: Any?): Shape =
        when (value) {
            "rectangle" -> RoundedCornerShape(0.dp)
            "rounded" -> MaterialTheme.shapes.medium
            "rounded-small" -> MaterialTheme.shapes.small
            "rounded-medium" -> MaterialTheme.shapes.medium
            "rounded-large" -> MaterialTheme.shapes.large
            "rounded-extra-large" -> MaterialTheme.shapes.extraLarge
            "circle" -> RoundedCornerShape(50)
            is Number -> RoundedCornerShape(value.toInt().dp)
            else -> MaterialTheme.shapes.medium
        }
}
