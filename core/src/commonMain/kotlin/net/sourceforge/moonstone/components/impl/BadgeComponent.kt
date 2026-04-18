package net.sourceforge.moonstone.components.impl

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import net.sourceforge.moonstone.runtime.DerivedStateCell
import net.sourceforge.moonstone.runtime.StateCell
import kotlin.reflect.KClass

/**
 * Badge component for displaying notification counts or status indicators.
 * Wraps content with a badge in the corner.
 *
 * Scheme usage:
 * (badge (icon #:name "notifications"))                    ; Simple dot badge
 * (badge #:count 5 (icon #:name "mail"))                  ; Badge with count
 * (badge #:count notifications-state (icon #:name "chat")) ; Reactive count
 * (badge #:color "red" #:count 99 (icon #:name "shopping-cart"))
 */
class BadgeComponent : AbstractComponent() {
    override val name = "badge"
    override val acceptsChildren = true

    override val propTypes: Map<String, KClass<*>> = mapOf(
        "count" to Any::class,
        "color" to String::class,
        "content-color" to String::class,
        "max-count" to Number::class,
        "visible" to Boolean::class
    )

    @Composable
    override fun Render(element: UIElement, renderChild: @Composable (UIElement) -> Unit) {
        val count = resolveNumber(element.props["count"])?.toInt()
        val color = ModifierBuilder.parseColor(element.props["color"])
            ?: MaterialTheme.colorScheme.error
        val contentColor = ModifierBuilder.parseColor(element.props["content-color"])
            ?: MaterialTheme.colorScheme.onError
        val maxCount = (element.props["max-count"] as? Number)?.toInt() ?: 99
        val visible = element.props["visible"]?.let { ModifierBuilder.isTruthy(it) } ?: true

        BadgedBox(
            badge = {
                if (visible && (count == null || count > 0)) {
                    Badge(
                        containerColor = color,
                        contentColor = contentColor
                    ) {
                        if (count != null) {
                            val displayCount = if (count > maxCount) "$maxCount+" else count.toString()
                            Text(displayCount)
                        }
                    }
                }
            }
        ) {
            element.children.forEach { child -> renderChild(child) }
        }
    }

    private fun resolveNumber(value: Any?): Number? {
        return when (value) {
            is StateCell -> {
                val lispValue = value.value
                lispValue.asInt()?.value ?: lispValue.asDouble()?.value
            }
            is DerivedStateCell -> {
                val lispValue = value.value
                lispValue.asInt()?.value ?: lispValue.asDouble()?.value
            }
            is Number -> value
            else -> null
        }
    }
}
