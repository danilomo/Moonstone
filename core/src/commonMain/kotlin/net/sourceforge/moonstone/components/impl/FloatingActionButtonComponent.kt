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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    private data class FABConfig(
        val modifier: Modifier,
        val style: String,
        val onClickHandler: FunctionObject?,
        val label: String?,
        val expanded: Boolean,
        val shape: Shape,
        val containerColor: Color,
        val contentColor: Color,
    )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        val config = parseFABConfig(element.props)
        val clickHandler: () -> Unit = {
            config.onClickHandler?.function()?.evaluate(emptyArray())
        }

        when (config.style) {
            "small" -> RenderSmallFAB(config, clickHandler, element, renderChild)
            "large" -> RenderLargeFAB(config, clickHandler, element, renderChild)
            "extended" -> RenderExtendedFAB(config, clickHandler, element, renderChild)
            else -> RenderStandardFAB(config, clickHandler, element, renderChild)
        }
    }

    @Composable
    private fun parseFABConfig(props: Map<String, Any?>): FABConfig =
        FABConfig(
            modifier = ModifierBuilder.build(props),
            style = props["style"]?.toString() ?: "standard",
            onClickHandler = props["on-click"] as? FunctionObject,
            label = props["label"]?.toString(),
            expanded = props["expanded"]?.let { ModifierBuilder.isTruthy(it) } ?: true,
            shape = parseShape(props["shape"]),
            containerColor =
                ModifierBuilder.parseColor(props["container-color"])
                    ?: MaterialTheme.colorScheme.primaryContainer,
            contentColor =
                ModifierBuilder.parseColor(props["content-color"])
                    ?: MaterialTheme.colorScheme.onPrimaryContainer,
        )

    @Composable
    private fun RenderSmallFAB(
        config: FABConfig,
        clickHandler: () -> Unit,
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        SmallFloatingActionButton(
            onClick = clickHandler,
            modifier = config.modifier,
            shape = config.shape,
            containerColor = config.containerColor,
            contentColor = config.contentColor,
        ) {
            element.children.forEach { child -> renderChild(child) }
        }
    }

    @Composable
    private fun RenderLargeFAB(
        config: FABConfig,
        clickHandler: () -> Unit,
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        LargeFloatingActionButton(
            onClick = clickHandler,
            modifier = config.modifier,
            shape = config.shape,
            containerColor = config.containerColor,
            contentColor = config.contentColor,
        ) {
            element.children.forEach { child -> renderChild(child) }
        }
    }

    @Composable
    private fun RenderExtendedFAB(
        config: FABConfig,
        clickHandler: () -> Unit,
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        ExtendedFloatingActionButton(
            onClick = clickHandler,
            modifier = config.modifier,
            shape = config.shape,
            containerColor = config.containerColor,
            contentColor = config.contentColor,
            expanded = config.expanded,
            icon = {
                element.children.forEach { child -> renderChild(child) }
            },
            text = {
                if (config.label != null) {
                    Text(config.label)
                }
            },
        )
    }

    @Composable
    private fun RenderStandardFAB(
        config: FABConfig,
        clickHandler: () -> Unit,
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        FloatingActionButton(
            onClick = clickHandler,
            modifier = config.modifier,
            shape = config.shape,
            containerColor = config.containerColor,
            contentColor = config.contentColor,
        ) {
            element.children.forEach { child -> renderChild(child) }
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
