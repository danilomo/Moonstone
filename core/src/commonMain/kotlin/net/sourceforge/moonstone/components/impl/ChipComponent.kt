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
import net.sourceforge.kleinlisp.objects.BooleanObject
import net.sourceforge.kleinlisp.objects.FunctionObject
import net.sourceforge.kleinlisp.objects.IntObject
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import net.sourceforge.moonstone.runtime.DerivedStateCell
import net.sourceforge.moonstone.runtime.StateCell
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

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "style" to String::class,
            "label" to String::class,
            "selected" to Any::class,
            "on-click" to Any::class,
            "on-select" to Any::class,
            "on-dismiss" to Any::class,
            "enabled" to Boolean::class,
            "show-check-icon" to Boolean::class,
            "padding" to Number::class,
        )

    private data class ChipConfig(
        val modifier: Modifier,
        val style: String,
        val label: String,
        val selected: Boolean,
        val enabled: Boolean,
        val showCheckIcon: Boolean,
        val onClickHandler: FunctionObject?,
        val onSelectHandler: FunctionObject?,
        val onDismissHandler: FunctionObject?,
    )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        val config = parseChipConfig(element.props)

        when (config.style) {
            "filter" -> RenderFilterChip(config, false)
            "elevated-filter" -> RenderFilterChip(config, true)
            "input" -> RenderInputChip(config)
            "suggestion" -> RenderSuggestionChip(config)
            else -> RenderAssistChip(config)
        }
    }

    private fun parseChipConfig(props: Map<String, Any?>): ChipConfig =
        ChipConfig(
            modifier = ModifierBuilder.build(props),
            style = props["style"]?.toString() ?: "assist",
            label = resolveString(props["label"]) ?: "Chip",
            selected = resolveBoolean(props["selected"]),
            onClickHandler = props["on-click"] as? FunctionObject,
            onSelectHandler = props["on-select"] as? FunctionObject,
            onDismissHandler = props["on-dismiss"] as? FunctionObject,
            enabled = props["enabled"]?.let { ModifierBuilder.isTruthy(it) } ?: true,
            showCheckIcon = props["show-check-icon"]?.let { ModifierBuilder.isTruthy(it) } ?: true,
        )

    @Composable
    private fun RenderFilterChip(
        config: ChipConfig,
        elevated: Boolean,
    ) {
        val chipComponent: @Composable (
            Boolean,
            () -> Unit,
            @Composable () -> Unit,
            Modifier,
            Boolean,
            (@Composable () -> Unit)?,
        ) -> Unit =
            if (elevated) {
                { selected, onClick, label, modifier, enabled, leadingIcon ->
                    ElevatedFilterChip(selected, onClick, label, modifier, enabled, leadingIcon = leadingIcon)
                }
            } else {
                { selected, onClick, label, modifier, enabled, leadingIcon ->
                    FilterChip(selected, onClick, label, modifier, enabled, leadingIcon = leadingIcon)
                }
            }

        chipComponent(
            config.selected,
            {
                val newValue = !config.selected
                val lispValue = IntObject.valueOf(if (newValue) 1 else 0)
                config.onSelectHandler?.function()?.evaluate(arrayOf(lispValue))
                    ?: config.onClickHandler?.function()?.evaluate(emptyArray())
            },
            { Text(config.label) },
            config.modifier,
            config.enabled,
            if (config.selected && config.showCheckIcon) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else {
                null
            },
        )
    }

    @Composable
    private fun RenderInputChip(config: ChipConfig) {
        InputChip(
            selected = config.selected,
            onClick = { config.onClickHandler?.function()?.evaluate(emptyArray()) },
            label = { Text(config.label) },
            modifier = config.modifier,
            enabled = config.enabled,
            trailingIcon =
                if (config.onDismissHandler != null) {
                    {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier =
                                Modifier.clickable {
                                    config.onDismissHandler.function()?.evaluate(emptyArray())
                                },
                        )
                    }
                } else {
                    null
                },
        )
    }

    @Composable
    private fun RenderSuggestionChip(config: ChipConfig) {
        SuggestionChip(
            onClick = { config.onClickHandler?.function()?.evaluate(emptyArray()) },
            label = { Text(config.label) },
            modifier = config.modifier,
            enabled = config.enabled,
        )
    }

    @Composable
    private fun RenderAssistChip(config: ChipConfig) {
        AssistChip(
            onClick = { config.onClickHandler?.function()?.evaluate(emptyArray()) },
            label = { Text(config.label) },
            modifier = config.modifier,
            enabled = config.enabled,
        )
    }

    private fun resolveString(value: Any?): String? =
        when (value) {
            is StateCell -> value.value.asString()?.value() ?: value.value.asObject()?.toString()
            is DerivedStateCell -> value.value.asString()?.value() ?: value.value.asObject()?.toString()
            is String -> value
            else -> value?.toString()
        }

    private fun resolveBoolean(value: Any?): Boolean =
        when (value) {
            null -> false
            is StateCell -> lispObjectToBoolean(value.value)
            is DerivedStateCell -> lispObjectToBoolean(value.value)
            is Boolean -> value
            is Number -> value.toInt() != 0
            else -> ModifierBuilder.isTruthy(value)
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
