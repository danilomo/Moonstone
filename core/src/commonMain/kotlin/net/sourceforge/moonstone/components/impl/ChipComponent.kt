package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import net.sourceforge.moonstone.runtime.DerivedStateCell
import net.sourceforge.moonstone.runtime.StateCell
import net.sourceforge.kleinlisp.objects.BooleanObject
import net.sourceforge.kleinlisp.objects.FunctionObject
import net.sourceforge.kleinlisp.objects.IntObject
import kotlin.reflect.KClass

/**
 * Chip component for tags, filters, and selections.
 * Supports filter, assist, input, and suggestion styles.
 *
 * Scheme usage:
 * (chip #:label "Tag" #:on-click handler)
 * (chip #:style 'filter #:label "Active" #:selected active-state #:on-select handler)
 * (chip #:style 'input #:label "Item" #:on-dismiss dismiss-handler)
 * (chip #:style 'suggestion #:label "Try this" #:on-click handler)
 */
class ChipComponent : AbstractComponent() {
    override val name = "chip"
    override val acceptsChildren = false

    override val propTypes: Map<String, KClass<*>> = mapOf(
        "style" to String::class,
        "label" to String::class,
        "selected" to Any::class,
        "on-click" to Any::class,
        "on-select" to Any::class,
        "on-dismiss" to Any::class,
        "enabled" to Boolean::class,
        "show-check-icon" to Boolean::class,
        "padding" to Number::class
    )

    @Composable
    override fun Render(element: UIElement, renderChild: @Composable (UIElement) -> Unit) {
        val modifier = ModifierBuilder.build(element.props)
        val style = element.props["style"]?.toString() ?: "assist"
        val label = resolveString(element.props["label"]) ?: "Chip"
        val selected = resolveBoolean(element.props["selected"])
        val onClickHandler = element.props["on-click"] as? FunctionObject
        val onSelectHandler = element.props["on-select"] as? FunctionObject
        val onDismissHandler = element.props["on-dismiss"] as? FunctionObject
        val enabled = element.props["enabled"]?.let { ModifierBuilder.isTruthy(it) } ?: true
        val showCheckIcon = element.props["show-check-icon"]?.let { ModifierBuilder.isTruthy(it) } ?: true

        when (style) {
            "filter" -> {
                FilterChip(
                    selected = selected,
                    onClick = {
                        val newValue = !selected
                        val lispValue = IntObject.valueOf(if (newValue) 1 else 0)
                        onSelectHandler?.function()?.evaluate(arrayOf(lispValue))
                            ?: onClickHandler?.function()?.evaluate(emptyArray())
                    },
                    label = { Text(label) },
                    modifier = modifier,
                    enabled = enabled,
                    leadingIcon = if (selected && showCheckIcon) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
            "elevated-filter" -> {
                ElevatedFilterChip(
                    selected = selected,
                    onClick = {
                        val newValue = !selected
                        val lispValue = IntObject.valueOf(if (newValue) 1 else 0)
                        onSelectHandler?.function()?.evaluate(arrayOf(lispValue))
                            ?: onClickHandler?.function()?.evaluate(emptyArray())
                    },
                    label = { Text(label) },
                    modifier = modifier,
                    enabled = enabled,
                    leadingIcon = if (selected && showCheckIcon) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
            "input" -> {
                InputChip(
                    selected = selected,
                    onClick = {
                        onClickHandler?.function()?.evaluate(emptyArray())
                    },
                    label = { Text(label) },
                    modifier = modifier,
                    enabled = enabled,
                    trailingIcon = if (onDismissHandler != null) {
                        {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.clickable {
                                    onDismissHandler.function()?.evaluate(emptyArray())
                                }
                            )
                        }
                    } else null
                )
            }
            "suggestion" -> {
                SuggestionChip(
                    onClick = {
                        onClickHandler?.function()?.evaluate(emptyArray())
                    },
                    label = { Text(label) },
                    modifier = modifier,
                    enabled = enabled
                )
            }
            "assist" -> {
                AssistChip(
                    onClick = {
                        onClickHandler?.function()?.evaluate(emptyArray())
                    },
                    label = { Text(label) },
                    modifier = modifier,
                    enabled = enabled
                )
            }
            else -> {
                // Default to assist chip
                AssistChip(
                    onClick = {
                        onClickHandler?.function()?.evaluate(emptyArray())
                    },
                    label = { Text(label) },
                    modifier = modifier,
                    enabled = enabled
                )
            }
        }
    }

    private fun resolveString(value: Any?): String? {
        return when (value) {
            is StateCell -> value.value.asString()?.value() ?: value.value.asObject()?.toString()
            is DerivedStateCell -> value.value.asString()?.value() ?: value.value.asObject()?.toString()
            is String -> value
            else -> value?.toString()
        }
    }

    private fun resolveBoolean(value: Any?): Boolean {
        return when (value) {
            null -> false
            is StateCell -> lispObjectToBoolean(value.value)
            is DerivedStateCell -> lispObjectToBoolean(value.value)
            is Boolean -> value
            is Number -> value.toInt() != 0
            else -> ModifierBuilder.isTruthy(value)
        }
    }

    private fun lispObjectToBoolean(lispValue: net.sourceforge.kleinlisp.LispObject): Boolean {
        return when {
            lispValue.asInt() != null -> lispValue.asInt().value != 0
            lispValue.asObject(Boolean::class.java) != null ->
                lispValue.asObject(Boolean::class.java) == true
            lispValue == BooleanObject.TRUE -> true
            lispValue == BooleanObject.FALSE -> false
            else -> ModifierBuilder.isTruthy(lispValue.asObject())
        }
    }
}
