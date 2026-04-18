package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import kotlin.reflect.KClass

/**
 * Lazy row component for efficient horizontal scrolling lists.
 *
 * Scheme usage:
 * (lazy-row #:spacing 8 #:padding 16
 *   (list-item #:key "a" (surface ...))
 *   (list-item #:key "b" (surface ...)))
 */
class LazyRowComponent : AbstractComponent() {
    override val name = "lazy-row"
    override val acceptsChildren = true

    override val propTypes: Map<String, KClass<*>> = mapOf(
        "spacing" to Number::class,
        "padding" to Number::class,
        "padding-horizontal" to Number::class,
        "padding-vertical" to Number::class,
        "horizontal-arrangement" to String::class,
        "vertical-alignment" to String::class,
        "fill-max-size" to Boolean::class,
        "fill-max-width" to Boolean::class,
        "fill-max-height" to Boolean::class,
        "width" to Number::class,
        "height" to Number::class
    )

    @Composable
    override fun Render(element: UIElement, renderChild: @Composable (UIElement) -> Unit) {
        val modifier = ModifierBuilder.build(element.props)
        val spacing = (element.props["spacing"] as? Number)?.toInt() ?: 0
        val contentPadding = buildContentPadding(element.props)
        val arrangement = parseHorizontalArrangement(element.props["horizontal-arrangement"], spacing)
        val alignment = parseVerticalAlignment(element.props["vertical-alignment"])

        // Extract keys from list-item children
        val itemsWithKeys = element.children.map { child ->
            val key = child.props["key"]?.toString() ?: child.hashCode().toString()
            key to child
        }

        LazyRow(
            modifier = modifier,
            contentPadding = contentPadding,
            horizontalArrangement = arrangement,
            verticalAlignment = alignment
        ) {
            items(
                items = itemsWithKeys,
                key = { it.first }
            ) { (_, child) ->
                renderChild(child)
            }
        }
    }

    private fun buildContentPadding(props: Map<String, Any?>): PaddingValues {
        val all = (props["padding"] as? Number)?.toInt()?.dp ?: 0.dp
        val horizontal = (props["padding-horizontal"] as? Number)?.toInt()?.dp ?: all
        val vertical = (props["padding-vertical"] as? Number)?.toInt()?.dp ?: all

        return PaddingValues(horizontal = horizontal, vertical = vertical)
    }

    private fun parseHorizontalArrangement(value: Any?, spacing: Int): Arrangement.Horizontal {
        return when (value) {
            "start" -> Arrangement.Start
            "center" -> Arrangement.Center
            "end" -> Arrangement.End
            "space-between" -> Arrangement.SpaceBetween
            "space-around" -> Arrangement.SpaceAround
            "space-evenly" -> Arrangement.SpaceEvenly
            else -> if (spacing > 0) Arrangement.spacedBy(spacing.dp) else Arrangement.Start
        }
    }

    private fun parseVerticalAlignment(value: Any?): Alignment.Vertical = when (value) {
        "top" -> Alignment.Top
        "center" -> Alignment.CenterVertically
        "bottom" -> Alignment.Bottom
        else -> Alignment.CenterVertically
    }
}
