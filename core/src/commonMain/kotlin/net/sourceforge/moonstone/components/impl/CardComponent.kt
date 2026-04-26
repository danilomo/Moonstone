package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import net.sourceforge.kleinlisp.objects.BooleanObject
import net.sourceforge.kleinlisp.objects.FunctionObject
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import net.sourceforge.moonstone.runtime.DerivedStateCell
import net.sourceforge.moonstone.runtime.StateCell
import kotlin.reflect.KClass

/**
 * Card component for Material Design containers.
 * Cards typically present a single coherent piece of content.
 *
 * Scheme usage:
 * (card (text #:value "Card content"))
 * (card #:style 'elevated #:on-click handler (text #:value "Clickable card"))
 * (card #:style 'outlined #:shape 'rounded-large (text #:value "Outlined card"))
 */
class CardComponent : AbstractComponent() {
    override val name = "card"
    override val acceptsChildren = true

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "style" to String::class,
            "shape" to String::class,
            "on-click" to Any::class,
            "enabled" to Boolean::class,
            "fill-max-size" to Boolean::class,
            "fill-max-width" to Boolean::class,
            "fill-max-height" to Boolean::class,
            "padding" to Number::class,
            "padding-horizontal" to Number::class,
            "padding-vertical" to Number::class,
            "width" to Number::class,
            "height" to Number::class,
        )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        val config =
            CardConfig(
                modifier = ModifierBuilder.build(element.props),
                style = element.props["style"]?.toString() ?: "filled",
                shape = parseShape(element.props["shape"]),
                enabled = resolveBoolean(element.props["enabled"]),
                clickHandler = createClickHandler(element.props["on-click"] as? FunctionObject),
            )

        val content: @Composable ColumnScope.() -> Unit = {
            element.children.forEach { child -> renderChild(child) }
        }

        renderCardByStyle(config, content)
    }

    private fun createClickHandler(onClickHandler: FunctionObject?): (() -> Unit)? =
        if (onClickHandler != null) {
            { onClickHandler.function()?.evaluate(emptyArray()) }
        } else {
            null
        }

    @Composable
    private fun renderCardByStyle(
        config: CardConfig,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        val cardComposable: @Composable (Boolean) -> Unit = { isClickable ->
            when (config.style) {
                "elevated" -> renderElevatedCard(config, content, isClickable)
                "outlined" -> renderOutlinedCard(config, content, isClickable)
                else -> renderFilledCard(config, content, isClickable)
            }
        }

        cardComposable(config.clickHandler != null)
    }

    @Composable
    private fun renderFilledCard(
        config: CardConfig,
        content: @Composable ColumnScope.() -> Unit,
        isClickable: Boolean,
    ) {
        if (isClickable && config.clickHandler != null) {
            Card(
                onClick = config.clickHandler,
                modifier = config.modifier,
                enabled = config.enabled,
                shape = config.shape,
                content = content,
            )
        } else {
            Card(
                modifier = config.modifier,
                shape = config.shape,
                content = content,
            )
        }
    }

    @Composable
    private fun renderElevatedCard(
        config: CardConfig,
        content: @Composable ColumnScope.() -> Unit,
        isClickable: Boolean,
    ) {
        if (isClickable && config.clickHandler != null) {
            ElevatedCard(
                onClick = config.clickHandler,
                modifier = config.modifier,
                enabled = config.enabled,
                shape = config.shape,
                content = content,
            )
        } else {
            ElevatedCard(
                modifier = config.modifier,
                shape = config.shape,
                content = content,
            )
        }
    }

    @Composable
    private fun renderOutlinedCard(
        config: CardConfig,
        content: @Composable ColumnScope.() -> Unit,
        isClickable: Boolean,
    ) {
        if (isClickable && config.clickHandler != null) {
            OutlinedCard(
                onClick = config.clickHandler,
                modifier = config.modifier,
                enabled = config.enabled,
                shape = config.shape,
                content = content,
            )
        } else {
            OutlinedCard(
                modifier = config.modifier,
                shape = config.shape,
                content = content,
            )
        }
    }

    private data class CardConfig(
        val modifier: androidx.compose.ui.Modifier,
        val style: String,
        val shape: Shape,
        val enabled: Boolean,
        val clickHandler: (() -> Unit)?,
    )

    @Composable
    private fun parseShape(value: Any?): Shape =
        when (value) {
            "rectangle" -> RoundedCornerShape(0.dp)
            "rounded" -> MaterialTheme.shapes.medium
            "rounded-small" -> MaterialTheme.shapes.small
            "rounded-medium" -> MaterialTheme.shapes.medium
            "rounded-large" -> MaterialTheme.shapes.large
            "rounded-extra-large" -> MaterialTheme.shapes.extraLarge
            is Number -> RoundedCornerShape(value.toInt().dp)
            else -> CardDefaults.shape
        }

    private fun resolveBoolean(value: Any?): Boolean {
        if (value == null) return true // Default to enabled
        return when (value) {
            is StateCell -> lispObjectToBoolean(value.value)
            is DerivedStateCell -> lispObjectToBoolean(value.value)
            is Boolean -> value
            is Number -> value.toInt() != 0
            else -> ModifierBuilder.isTruthy(value)
        }
    }

    private fun lispObjectToBoolean(lispValue: net.sourceforge.kleinlisp.LispObject): Boolean =
        when {
            lispValue.asInt() != null -> lispValue.asInt().value != 0
            lispValue.asObject(Boolean::class.java) != null ->
                lispValue.asObject(Boolean::class.java) == true
            lispValue == BooleanObject.TRUE -> true
            lispValue == BooleanObject.FALSE -> false
            else -> ModifierBuilder.isTruthy(lispValue.asObject())
        }
}
