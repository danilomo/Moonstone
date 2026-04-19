package net.sourceforge.moonstone.components.impl

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
        val color =
            ModifierBuilder.parseColor(element.props["color"])
                ?: MaterialTheme.colorScheme.primary
        val trackColor =
            ModifierBuilder.parseColor(element.props["track-color"])
                ?: MaterialTheme.colorScheme.surfaceVariant
        val strokeWidth = (element.props["stroke-width"] as? Number)?.toInt()?.dp ?: 4.dp

        when (style) {
            "linear" -> {
                if (value != null) {
                    // Determinate linear progress
                    LinearProgressIndicator(
                        progress = { value.coerceIn(0f, 1f) },
                        modifier = modifier,
                        color = color,
                        trackColor = trackColor,
                    )
                } else {
                    // Indeterminate linear progress
                    LinearProgressIndicator(
                        modifier = modifier,
                        color = color,
                        trackColor = trackColor,
                    )
                }
            }
            "circular" -> {
                if (value != null) {
                    // Determinate circular progress
                    CircularProgressIndicator(
                        progress = { value.coerceIn(0f, 1f) },
                        modifier = modifier,
                        color = color,
                        trackColor = trackColor,
                        strokeWidth = strokeWidth,
                    )
                } else {
                    // Indeterminate circular progress
                    CircularProgressIndicator(
                        modifier = modifier,
                        color = color,
                        trackColor = trackColor,
                        strokeWidth = strokeWidth,
                    )
                }
            }
            else -> {
                // Default to circular
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
