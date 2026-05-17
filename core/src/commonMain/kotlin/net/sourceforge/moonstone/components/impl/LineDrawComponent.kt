package net.sourceforge.moonstone.components.impl

import androidx.compose.runtime.Composable
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import kotlin.reflect.KClass

/**
 * Draw primitive for a line segment inside a game-canvas.
 *
 * Rendered by GameCanvasComponent in DrawScope; has no visible output
 * if placed outside a game-canvas.
 *
 * Scheme usage:
 * (line #:x1 0 #:y1 0 #:x2 100 #:y2 100 #:color "#FFFFFF" #:stroke-width 2)
 */
class LineDrawComponent : AbstractComponent() {
    override val name = "line"
    override val acceptsChildren = false

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "x1" to Number::class,
            "y1" to Number::class,
            "x2" to Number::class,
            "y2" to Number::class,
            "color" to String::class,
            "stroke-width" to Number::class,
        )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        // Rendered by GameCanvasComponent inside a DrawScope
    }
}
