package net.sourceforge.moonstone.components.impl

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import net.sourceforge.moonstone.runtime.DerivedStateCell
import net.sourceforge.moonstone.runtime.StateCell
import kotlin.reflect.KClass

/**
 * Progress indicator component for showing loading or progress states.
 * Supports both linear and circular styles, with determinate or indeterminate progress.
 *
 * Scheme usage:
 * (progress-indicator)                                    ; Indeterminate circular
 * (progress-indicator #:style 'linear)                   ; Indeterminate linear
 * (progress-indicator #:value progress-state)            ; Determinate circular (0.0-1.0)
 * (progress-indicator #:style 'linear #:value 0.5)       ; Determinate linear at 50%
 * (progress-indicator #:color "blue" #:track-color "gray")
 */
class ProgressIndicatorComponent : AbstractComponent() {
    override val name = "progress-indicator"
    override val acceptsChildren = false

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "style" to String::class,
            "value" to Any::class,
            "color" to String::class,
            "track-color" to String::class,
            "stroke-width" to Number::class,
            "size" to Number::class,
            "fill-max-width" to Boolean::class,
            "width" to Number::class,
            "padding" to Number::class,
        )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        val modifier = ModifierBuilder.build(element.props)
        val style = element.props["style"]?.toString() ?: "circular"
        val value = resolveNumber(element.props["value"])?.toFloat()
        val color = resolveProgressColor(element.props["color"])
        val trackColor = resolveTrackColor(element.props["track-color"])
        val strokeWidth = (element.props["stroke-width"] as? Number)?.toInt()?.dp ?: 4.dp

        when (style) {
            "linear" -> renderLinearProgress(value, modifier, color, trackColor)
            "circular" -> renderCircularProgress(value, modifier, color, trackColor, strokeWidth)
            else -> renderCircularProgress(value, modifier, color, trackColor, strokeWidth)
        }
    }

    @Composable
    private fun resolveProgressColor(colorProp: Any?): Color =
        ModifierBuilder.parseColor(colorProp) ?: MaterialTheme.colorScheme.primary

    @Composable
    private fun resolveTrackColor(colorProp: Any?): Color =
        ModifierBuilder.parseColor(colorProp) ?: MaterialTheme.colorScheme.surfaceVariant

    @Composable
    private fun renderLinearProgress(
        value: Float?,
        modifier: Modifier,
        color: Color,
        trackColor: Color,
    ) {
        if (value != null) {
            LinearProgressIndicator(
                progress = { value.coerceIn(0f, 1f) },
                modifier = modifier,
                color = color,
                trackColor = trackColor,
            )
        } else {
            LinearProgressIndicator(
                modifier = modifier,
                color = color,
                trackColor = trackColor,
            )
        }
    }

    @Composable
    private fun renderCircularProgress(
        value: Float?,
        modifier: Modifier,
        color: Color,
        trackColor: Color,
        strokeWidth: Dp,
    ) {
        if (value != null) {
            CircularProgressIndicator(
                progress = { value.coerceIn(0f, 1f) },
                modifier = modifier,
                color = color,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
            )
        } else {
            CircularProgressIndicator(
                modifier = modifier,
                color = color,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
            )
        }
    }

    private fun resolveNumber(value: Any?): Number? =
        when (value) {
            is StateCell -> {
                val lispValue = value.value
                lispValue.asDouble()?.value ?: lispValue.asInt()?.value
            }
            is DerivedStateCell -> {
                val lispValue = value.value
                lispValue.asDouble()?.value ?: lispValue.asInt()?.value
            }
            is Number -> value
            else -> null
        }
}
