package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import kotlin.reflect.KClass

/**
 * Lazy column component for efficient vertical scrolling lists.
 *
 * Scheme usage:
 * (lazy-column #:spacing 8 #:padding 16
 *   (list-item #:key "1" (text #:value "Item 1"))
 *   (list-item #:key "2" (text #:value "Item 2")))
 */
class LazyColumnComponent : AbstractComponent() {
    override val name = "lazy-column"
    override val acceptsChildren = true

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "spacing" to Number::class,
            "padding" to Number::class,
            "padding-horizontal" to Number::class,
            "padding-vertical" to Number::class,
            "vertical-arrangement" to String::class,
            "horizontal-alignment" to String::class,
            "fill-max-size" to Boolean::class,
            "fill-max-width" to Boolean::class,
            "fill-max-height" to Boolean::class,
            "width" to Number::class,
            "height" to Number::class,
        )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        val modifier = ModifierBuilder.build(element.props)
        val spacing = (element.props["spacing"] as? Number)?.toInt() ?: 0
        val contentPadding = buildContentPadding(element.props)
        val arrangement = parseVerticalArrangement(element.props["vertical-arrangement"], spacing)
        val alignment = parseHorizontalAlignment(element.props["horizontal-alignment"])

        // Extract keys from list-item children
        val itemsWithKeys =
            element.children.map { child ->
                val key = child.props["key"]?.toString() ?: child.hashCode().toString()
                key to child
            }

        LazyColumn(
            modifier = modifier,
            contentPadding = contentPadding,
            verticalArrangement = arrangement,
            horizontalAlignment = alignment,
        ) {
            items(
                items = itemsWithKeys,
                key = { it.first },
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

    private fun parseVerticalArrangement(
        value: Any?,
        spacing: Int,
    ): Arrangement.Vertical =
        when (value) {
            "top" -> Arrangement.Top
            "center" -> Arrangement.Center
            "bottom" -> Arrangement.Bottom
            "space-between" -> Arrangement.SpaceBetween
            "space-around" -> Arrangement.SpaceAround
            "space-evenly" -> Arrangement.SpaceEvenly
            else -> if (spacing > 0) Arrangement.spacedBy(spacing.dp) else Arrangement.Top
        }

    private fun parseHorizontalAlignment(value: Any?): Alignment.Horizontal =
        when (value) {
            "start" -> Alignment.Start
            "center" -> Alignment.CenterHorizontally
            "end" -> Alignment.End
            else -> Alignment.Start
        }
}
