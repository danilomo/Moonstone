package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import net.sourceforge.kleinlisp.objects.FunctionObject
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import net.sourceforge.moonstone.runtime.DerivedStateCell
import net.sourceforge.moonstone.runtime.StateCell
import kotlin.reflect.KClass

/**
 * Image component for displaying images.
 * Supports various content scaling modes and shapes.
 *
 * Note: For loading remote images, use platform-specific image loaders like Coil.
 * This component provides a placeholder/fallback display.
 *
 * Scheme usage:
 * (image #:source "path/to/image.png" #:width 100 #:height 100)
 * (image #:source "photo.jpg" #:scale 'crop #:shape 'circle)
 * (image #:placeholder "Loading..." #:width 200 #:height 150)
 */
class ImageComponent : AbstractComponent() {
    override val name = "image"
    override val acceptsChildren = false

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "source" to String::class,
            "placeholder" to String::class,
            "content-description" to String::class,
            "scale" to String::class,
            "shape" to String::class,
            "width" to Number::class,
            "height" to Number::class,
            "size" to Number::class,
            "on-click" to Any::class,
            "background" to String::class,
            "fill-max-size" to Boolean::class,
            "fill-max-width" to Boolean::class,
            "padding" to Number::class,
        )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        var modifier = ModifierBuilder.build(element.props)

        @Suppress("UNUSED_VARIABLE") // TODO: Use when image loading is implemented
        val source = resolveString(element.props["source"])
        val placeholder = resolveString(element.props["placeholder"]) ?: "Image"

        @Suppress("UNUSED_VARIABLE") // TODO: Use when image loading is implemented
        val contentDescription = resolveString(element.props["content-description"])

        @Suppress("UNUSED_VARIABLE") // TODO: Use when image loading is implemented
        val scale = parseContentScale(element.props["scale"])
        val shape = parseShape(element.props["shape"])
        val size = (element.props["size"] as? Number)?.toInt()
        val onClickHandler = element.props["on-click"] as? FunctionObject

        // Apply size if specified
        if (size != null) {
            modifier = modifier.size(size.dp)
        }

        // Apply shape clipping
        if (shape != null) {
            modifier = modifier.clip(shape)
        }

        // Apply click handler
        if (onClickHandler != null) {
            modifier =
                modifier.clickable {
                    onClickHandler.function()?.evaluate(emptyArray())
                }
        }

        // For now, show a placeholder box since actual image loading
        // requires platform-specific implementations (Coil on Android, etc.)
        // The actual image loading can be implemented in platform-specific code
        Box(
            modifier =
                modifier
                    .background(
                        ModifierBuilder.parseColor(element.props["background"])
                            ?: MaterialTheme.colorScheme.surfaceVariant,
                        shape ?: RoundedCornerShape(8.dp),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    private fun resolveString(value: Any?): String? =
        when (value) {
            is StateCell -> value.value.asString()?.value() ?: value.value.asObject()?.toString()
            is DerivedStateCell -> value.value.asString()?.value() ?: value.value.asObject()?.toString()
            is String -> value
            else -> value?.toString()
        }

    private fun parseContentScale(value: Any?): ContentScale =
        when (value) {
            "crop" -> ContentScale.Crop
            "fit" -> ContentScale.Fit
            "fill-bounds" -> ContentScale.FillBounds
            "fill-width" -> ContentScale.FillWidth
            "fill-height" -> ContentScale.FillHeight
            "inside" -> ContentScale.Inside
            "none" -> ContentScale.None
            else -> ContentScale.Fit
        }

    @Composable
    private fun parseShape(value: Any?): Shape? =
        when (value) {
            "circle" -> CircleShape
            "rectangle" -> RoundedCornerShape(0.dp)
            "rounded" -> MaterialTheme.shapes.medium
            "rounded-small" -> MaterialTheme.shapes.small
            "rounded-medium" -> MaterialTheme.shapes.medium
            "rounded-large" -> MaterialTheme.shapes.large
            is Number -> RoundedCornerShape(value.toInt().dp)
            else -> null
        }
}
