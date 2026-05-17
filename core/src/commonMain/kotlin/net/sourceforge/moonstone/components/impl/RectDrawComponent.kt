package net.sourceforge.moonstone.components.impl

import androidx.compose.runtime.Composable
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import kotlin.reflect.KClass

/**
 * Draw primitive for a filled rectangle inside a game-canvas.
 *
 * Rendered by GameCanvasComponent in DrawScope; has no visible output
 * if placed outside a game-canvas.
 *
 * Scheme usage:
 * (rect #:x 10 #:y 20 #:width 30 #:height 30 #:color "#FF0000")
 */
class RectDrawComponent : AbstractComponent() {
    override val name = "rect"
    override val acceptsChildren = false

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "x" to Number::class,
            "y" to Number::class,
            "width" to Number::class,
            "height" to Number::class,
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
