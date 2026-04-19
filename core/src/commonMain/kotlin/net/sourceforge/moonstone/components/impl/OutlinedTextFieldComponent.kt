package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import net.sourceforge.kleinlisp.objects.FunctionObject
import net.sourceforge.kleinlisp.objects.StringObject
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import net.sourceforge.moonstone.runtime.DerivedStateCell
import net.sourceforge.moonstone.runtime.StateCell
import kotlin.reflect.KClass

/**
 * Outlined text input field component with Material Design outlined style.
 *
 * Scheme usage:
 * (outlined-text-field #:value name-state #:label "Name" #:on-change handler)
 * (outlined-text-field #:value email #:label "Email" #:keyboard-type 'email #:on-change handler)
 */
class OutlinedTextFieldComponent : AbstractComponent() {
    override val name = "outlined-text-field"
    override val acceptsChildren = false

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "value" to Any::class,
            "on-change" to Any::class,
            "label" to String::class,
            "placeholder" to String::class,
            "enabled" to Boolean::class,
            "single-line" to Boolean::class,
            "max-lines" to Number::class,
            "keyboard-type" to String::class,
            "fill-max-width" to Boolean::class,
        )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        val currentValue = resolveValue(element.props["value"])
        val onChangeHandler = element.props["on-change"] as? FunctionObject
        val label = element.props["label"] as? String
        val placeholder = element.props["placeholder"] as? String
        val enabled = element.props["enabled"]?.let { ModifierBuilder.isTruthy(it) } ?: true
        val singleLine = element.props["single-line"]?.let { ModifierBuilder.isTruthy(it) } ?: true
        val maxLines = (element.props["max-lines"] as? Number)?.toInt() ?: if (singleLine) 1 else Int.MAX_VALUE
        val keyboardType = parseKeyboardType(element.props["keyboard-type"])

        var modifier: Modifier = Modifier
        if (ModifierBuilder.isTruthy(element.props["fill-max-width"])) {
            modifier = modifier.fillMaxWidth()
        }

        val visualTransformation =
            if (element.props["keyboard-type"]?.toString() == "password") {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            }

        OutlinedTextField(
            value = currentValue,
            onValueChange = { newValue ->
                onChangeHandler?.function()?.evaluate(arrayOf(StringObject(newValue)))
            },
            label = label?.let { { Text(it) } },
            placeholder = placeholder?.let { { Text(it) } },
            enabled = enabled,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            modifier = modifier,
        )
    }

    private fun resolveValue(value: Any?): String =
        when (value) {
            is StateCell -> lispObjectToString(value.value)
            is DerivedStateCell -> lispObjectToString(value.value)
            is String -> value
            null -> ""
            else -> value.toString()
        }

    private fun lispObjectToString(lispValue: net.sourceforge.kleinlisp.LispObject): String =
        when {
            lispValue.asString() != null -> lispValue.asString().value()
            lispValue.asInt() != null -> lispValue.asInt().value.toString()
            lispValue.asDouble() != null -> lispValue.asDouble().value.toString()
            else -> lispValue.asObject()?.toString() ?: ""
        }

    private fun parseKeyboardType(value: Any?): KeyboardType =
        when (value?.toString()) {
            "text" -> KeyboardType.Text
            "number" -> KeyboardType.Number
            "phone" -> KeyboardType.Phone
            "email" -> KeyboardType.Email
            "password" -> KeyboardType.Password
            "uri", "url" -> KeyboardType.Uri
            "decimal" -> KeyboardType.Decimal
            else -> KeyboardType.Text
        }
}
