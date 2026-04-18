package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.ComponentElement
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.components.UIElementWrapper
import net.sourceforge.moonstone.render.ModifierBuilder
import net.sourceforge.moonstone.runtime.DerivedStateCell
import net.sourceforge.moonstone.runtime.StateCell
import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.Seq
import net.sourceforge.kleinlisp.objects.FunctionObject
import kotlin.reflect.KClass

/**
 * Dynamic list component that renders items from a state cell.
 * Re-evaluates the render function when the state changes.
 *
 * Supports any seqable collection type (lists, vectors, sets, lazy sequences).
 *
 * Scheme usage:
 * (dynamic-list #:items todos-state #:render-item render-todo-fn)
 *
 * Where:
 * - todos-state is a state cell containing any seqable (list, vector, set, etc.)
 * - render-todo-fn is a function that takes an item and returns a UI element
 */
class DynamicListComponent : AbstractComponent() {
    override val name = "dynamic-list"
    override val acceptsChildren = false

    override val propTypes: Map<String, KClass<*>> = mapOf(
        "items" to Any::class,
        "render-item" to Any::class,
        "spacing" to Number::class,
        "vertical-arrangement" to String::class,
        "horizontal-alignment" to String::class
    )

    override val requiredProps: List<String> = listOf("items", "render-item")

    @Composable
    override fun Render(element: UIElement, renderChild: @Composable (UIElement) -> Unit) {
        val itemsSource = element.props["items"]
        val renderFn = element.props["render-item"] as? FunctionObject

        if (renderFn == null) {
            return
        }

        // Get items from state cell, derived cell, or direct seqable
        val items: List<LispObject> = when (itemsSource) {
            is StateCell -> {
                val value = itemsSource.value
                extractListItems(value)
            }
            is DerivedStateCell -> {
                val value = itemsSource.value
                extractListItems(value)
            }
            is LispObject -> {
                extractListItems(itemsSource)
            }
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                itemsSource.filterIsInstance<LispObject>()
            }
            else -> emptyList()
        }

        val spacing = (element.props["spacing"] as? Number)?.toInt() ?: 0
        val verticalArrangement = parseVerticalArrangement(
            element.props["vertical-arrangement"]?.toString(),
            spacing
        )
        val horizontalAlignment = parseHorizontalAlignment(
            element.props["horizontal-alignment"]?.toString()
        )

        val contentPadding = buildContentPadding(element.props)
        val modifier = ModifierBuilder.build(element.props)

        LazyColumn(
            modifier = modifier,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            contentPadding = contentPadding
        ) {
            items(
                items = items,
                key = { item -> getItemKey(item) }
            ) { item ->
                // Call the render function for each item
                val result = renderFn.function().evaluate(arrayOf(item))
                val wrapper = result.asObject(UIElementWrapper::class.java)
                if (wrapper != null) {
                    renderChild(wrapper.element)
                }
            }
        }
    }

    private fun extractListItems(value: LispObject): List<LispObject> {
        var seq: Seq? = value.asSeq() ?: return emptyList()

        val items = mutableListOf<LispObject>()
        while (seq != null && !seq.isEmpty) {
            val first = seq.first()
            if (first != null) {
                items.add(first)
            }
            seq = seq.rest()
        }
        return items
    }

    private fun getItemKey(item: LispObject): Any {
        // Try to extract a key from the item
        // If it's a seqable (list, vector, etc.), use the first element as the key
        val seq = item.asSeq()
        if (seq != null && !seq.isEmpty) {
            val first = seq.first()
            if (first != null) {
                return first.asInt()?.value
                    ?: first.asString()?.value()
                    ?: first.asAtom()?.toString()
                    ?: item.hashCode()
            }
        }
        return item.asInt()?.value
            ?: item.asString()?.value()
            ?: item.hashCode()
    }

    private fun parseVerticalArrangement(value: String?, spacing: Int): Arrangement.Vertical {
        val spacedBy = Arrangement.spacedBy(spacing.dp)
        return when (value?.lowercase()) {
            "top" -> Arrangement.Top
            "center" -> Arrangement.Center
            "bottom" -> Arrangement.Bottom
            "space-between" -> Arrangement.SpaceBetween
            "space-around" -> Arrangement.SpaceAround
            "space-evenly" -> Arrangement.SpaceEvenly
            else -> if (spacing > 0) spacedBy else Arrangement.Top
        }
    }

    private fun parseHorizontalAlignment(value: String?): Alignment.Horizontal {
        return when (value?.lowercase()) {
            "start" -> Alignment.Start
            "center" -> Alignment.CenterHorizontally
            "end" -> Alignment.End
            else -> Alignment.Start
        }
    }

    private fun buildContentPadding(props: Map<String, Any?>): PaddingValues {
        val padding = (props["padding"] as? Number)?.toInt() ?: 0
        val paddingH = (props["padding-horizontal"] as? Number)?.toInt() ?: padding
        val paddingV = (props["padding-vertical"] as? Number)?.toInt() ?: padding
        return PaddingValues(horizontal = paddingH.dp, vertical = paddingV.dp)
    }
}
