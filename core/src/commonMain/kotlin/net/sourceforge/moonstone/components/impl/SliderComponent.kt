package net.sourceforge.moonstone.components.impl

import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import net.sourceforge.kleinlisp.objects.DoubleObject
import net.sourceforge.kleinlisp.objects.FunctionObject
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import net.sourceforge.moonstone.runtime.DerivedStateCell
import net.sourceforge.moonstone.runtime.StateCell
import kotlin.reflect.KClass

/**
 * Slider component for selecting values from a range.
 * Ideal for adjusting settings such as volume, brightness, or applying filters.
 *
 * Scheme usage:
 * (slider #:value volume-state #:on-change (lambda (v) (state-set! volume-state v)))
 * (slider #:value brightness #:min 0 #:max 100 #:steps 10 #:on-change handler)
 */
class SliderComponent : AbstractComponent() {
    override val name = "slider"
    override val acceptsChildren = false

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "value" to Any::class,
            "on-change" to Any::class,
            "min" to Number::class,
            "max" to Number::class,
            "steps" to Number::class,
            "enabled" to Boolean::class,
            "fill-max-width" to Boolean::class,
            "width" to Number::class,
            "padding" to Number::class,
            "padding-horizontal" to Number::class,
        )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        val modifier = ModifierBuilder.build(element.props)
        val value = resolveNumber(element.props["value"])?.toFloat() ?: 0f
        val onChangeHandler = element.props["on-change"] as? FunctionObject
        val minValue = (element.props["min"] as? Number)?.toFloat() ?: 0f
        val maxValue = (element.props["max"] as? Number)?.toFloat() ?: 1f
        val steps = (element.props["steps"] as? Number)?.toInt() ?: 0
        val enabled = element.props["enabled"]?.let { ModifierBuilder.isTruthy(it) } ?: true

        Slider(
            value = value.coerceIn(minValue, maxValue),
            onValueChange = { newValue ->
                onChangeHandler?.function()?.evaluate(arrayOf(DoubleObject(newValue.toDouble())))
            },
            modifier = modifier,
            enabled = enabled,
            valueRange = minValue..maxValue,
            steps = if (steps > 0) steps - 1 else 0, // Compose expects intervals, not total steps
        )
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
