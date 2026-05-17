package net.sourceforge.moonstone.components.impl

import androidx.compose.runtime.Composable
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import kotlin.reflect.KClass

/**
 * Draw primitive for a filled circle inside a game-canvas.
 *
 * Rendered by GameCanvasComponent in DrawScope; has no visible output
 * if placed outside a game-canvas.
 *
 * Scheme usage:
 * (circle #:cx 50 #:cy 50 #:radius 20 #:color "#00FF00")
 */
class CircleDrawComponent : AbstractComponent() {
    override val name = "circle"
    override val acceptsChildren = false

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "cx" to Number::class,
            "cy" to Number::class,
            "radius" to Number::class,
            "color" to String::class,
        )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        // Rendered by GameCanvasComponent inside a DrawScope
    }
}
