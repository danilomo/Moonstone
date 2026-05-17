package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.delay
import net.sourceforge.kleinlisp.objects.FunctionObject
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import kotlin.reflect.KClass

/**
 * A fixed-size drawing canvas with an optional game loop.
 *
 * Children must be draw primitives: rect, circle, or line.
 * Scheme lists of draw primitives are also accepted as positional arguments.
 *
 * Scheme usage:
 * (game-canvas
 *   #:width 240
 *   #:height 480
 *   #:background "#111111"
 *   #:on-tick tick-fn       ; called every tick-interval ms
 *   #:tick-interval 50      ; ms between ticks (default 16)
 *   (list-of-rect-elements)
 *   (rect #:x 10 #:y 10 #:width 20 #:height 20 #:color "#FF0000"))
 */
class GameCanvasComponent : AbstractComponent() {
    override val name = "game-canvas"
    override val acceptsChildren = true

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "width" to Number::class,
            "height" to Number::class,
            "background" to String::class,
            "on-tick" to Any::class,
            "tick-interval" to Number::class,
        )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        val onTick = element.props["on-tick"] as? FunctionObject
        val tickInterval = (element.props["tick-interval"] as? Number)?.toLong() ?: 16L

        // Keep tick function and interval refs fresh across recompositions
        // without restarting the game loop coroutine
        val tickFnRef = remember { mutableStateOf(onTick) }
        val tickIntervalRef = remember { mutableStateOf(tickInterval) }
        SideEffect {
            tickFnRef.value = onTick
            tickIntervalRef.value = tickInterval
        }

        // Game loop: runs for the lifetime of this composable
        LaunchedEffect(Unit) {
            while (true) {
                delay(tickIntervalRef.value)
                tickFnRef.value?.function()?.evaluate(emptyArray())
            }
        }

        val modifier = ModifierBuilder.build(element.props)
        Canvas(modifier = modifier) {
            ModifierBuilder.parseColor(element.props["background"])?.let { bg ->
                drawRect(bg)
            }
            element.children.forEach { child ->
                renderDrawPrimitive(child)
            }
        }
    }

    private fun DrawScope.renderDrawPrimitive(element: UIElement) {
        when (element.type) {
            "rect" -> drawRectPrimitive(element)
            "circle" -> drawCirclePrimitive(element)
            "line" -> drawLinePrimitive(element)
        }
    }

    private fun DrawScope.drawRectPrimitive(element: UIElement) {
        val x = (element.props["x"] as? Number)?.toFloat() ?: 0f
        val y = (element.props["y"] as? Number)?.toFloat() ?: 0f
        val w = (element.props["width"] as? Number)?.toFloat() ?: 0f
        val h = (element.props["height"] as? Number)?.toFloat() ?: 0f
        val color = ModifierBuilder.parseColor(element.props["color"]) ?: Color.White
        drawRect(color, topLeft = Offset(x, y), size = Size(w, h))
    }

    private fun DrawScope.drawCirclePrimitive(element: UIElement) {
        val cx = (element.props["cx"] as? Number)?.toFloat() ?: 0f
        val cy = (element.props["cy"] as? Number)?.toFloat() ?: 0f
        val r = (element.props["radius"] as? Number)?.toFloat() ?: 0f
        val color = ModifierBuilder.parseColor(element.props["color"]) ?: Color.White
        drawCircle(color, radius = r, center = Offset(cx, cy))
    }

    private fun DrawScope.drawLinePrimitive(element: UIElement) {
        val x1 = (element.props["x1"] as? Number)?.toFloat() ?: 0f
        val y1 = (element.props["y1"] as? Number)?.toFloat() ?: 0f
        val x2 = (element.props["x2"] as? Number)?.toFloat() ?: 0f
        val y2 = (element.props["y2"] as? Number)?.toFloat() ?: 0f
        val sw = (element.props["stroke-width"] as? Number)?.toFloat() ?: 1f
        val color = ModifierBuilder.parseColor(element.props["color"]) ?: Color.White
        drawLine(color, Offset(x1, y1), Offset(x2, y2), sw)
    }
}
