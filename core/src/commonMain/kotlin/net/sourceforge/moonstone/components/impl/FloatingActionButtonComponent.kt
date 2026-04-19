package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import net.sourceforge.kleinlisp.objects.FunctionObject
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import kotlin.reflect.KClass

/**
 * Floating Action Button (FAB) component for primary actions.
 * Supports standard, small, large, and extended FAB variants.
 *
 * Scheme usage:
 * (fab #:on-click handler (icon #:name "add"))
 * (fab #:style 'small #:on-click handler (icon #:name "edit"))
 * (fab #:style 'large #:on-click handler (icon #:name "share"))
 * (fab #:style 'extended #:label "Create" #:on-click handler (icon #:name "add"))
 */
class FloatingActionButtonComponent : AbstractComponent() {
    override val name = "fab"
    override val acceptsChildren = true

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "style" to String::class,
            "on-click" to Any::class,
            "label" to String::class,
            "expanded" to Boolean::class,
            "shape" to String::class,
            "container-color" to String::class,
            "content-color" to String::class,
        )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        val modifier = ModifierBuilder.build(element.props)
        val style = element.props["style"]?.toString() ?: "standard"
        val onClickHandler = element.props["on-click"] as? FunctionObject
        val label = element.props["label"]?.toString()
        val expanded = element.props["expanded"]?.let { ModifierBuilder.isTruthy(it) } ?: true
        val shape = parseShape(element.props["shape"])
        val containerColor =
            ModifierBuilder.parseColor(element.props["container-color"])
                ?: MaterialTheme.colorScheme.primaryContainer
        val contentColor =
            ModifierBuilder.parseColor(element.props["content-color"])
                ?: MaterialTheme.colorScheme.onPrimaryContainer

        val clickHandler: () -> Unit = {
            onClickHandler?.function()?.evaluate(emptyArray())
        }

        when (style) {
            "small" -> {
                SmallFloatingActionButton(
                    onClick = clickHandler,
                    modifier = modifier,
                    shape = shape,
                    containerColor = containerColor,
                    contentColor = contentColor,
                ) {
                    element.children.forEach { child -> renderChild(child) }
                }
            }
            "large" -> {
                LargeFloatingActionButton(
                    onClick = clickHandler,
                    modifier = modifier,
                    shape = shape,
                    containerColor = containerColor,
                    contentColor = contentColor,
                ) {
                    element.children.forEach { child -> renderChild(child) }
                }
            }
            "extended" -> {
                ExtendedFloatingActionButton(
                    onClick = clickHandler,
                    modifier = modifier,
                    shape = shape,
                    containerColor = containerColor,
                    contentColor = contentColor,
                    expanded = expanded,
                    icon = {
                        element.children.forEach { child -> renderChild(child) }
                    },
                    text = {
                        if (label != null) {
                            Text(label)
                        }
                    },
                )
            }
            else -> {
                // Standard FAB
                FloatingActionButton(
                    onClick = clickHandler,
                    modifier = modifier,
                    shape = shape,
                    containerColor = containerColor,
                    contentColor = contentColor,
                ) {
                    element.children.forEach { child -> renderChild(child) }
                }
            }
        }
    }

    @Composable
    private fun parseShape(value: Any?): Shape =
        when (value) {
            "circle" -> CircleShape
            "rounded" -> MaterialTheme.shapes.medium
            "rounded-small" -> MaterialTheme.shapes.small
            "rounded-medium" -> MaterialTheme.shapes.medium
            "rounded-large" -> MaterialTheme.shapes.large
            is Number ->
                androidx.compose.foundation.shape
                    .RoundedCornerShape(value.toInt().dp)
            else -> FloatingActionButtonDefaults.shape
        }
}
