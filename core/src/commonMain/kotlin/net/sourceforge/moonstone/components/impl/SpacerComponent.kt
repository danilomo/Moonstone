package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.runtime.DerivedStateCell
import net.sourceforge.moonstone.runtime.StateCell
import kotlin.reflect.KClass

/**
 * Spacer component for adding space between elements.
 *
 * Note: The `weight` property for flexible space in Row/Column is not directly
 * supported here as it requires RowScope/ColumnScope. Use explicit width/height instead.
 */
class SpacerComponent : AbstractComponent() {
    override val name = "spacer"
    override val acceptsChildren = false

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "width" to Number::class,
            "height" to Number::class,
        )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        var modifier: Modifier = Modifier

        resolveNumber(element.props["width"])?.let {
            modifier = modifier.width(it.toInt().dp)
        }
        resolveNumber(element.props["height"])?.let {
            modifier = modifier.height(it.toInt().dp)
        }

        Spacer(modifier = modifier)
    }

    private fun resolveNumber(value: Any?): Number? =
        when (value) {
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
