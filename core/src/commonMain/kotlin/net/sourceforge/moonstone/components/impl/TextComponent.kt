package net.sourceforge.moonstone.components.impl

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.objects.JavaObject
import net.sourceforge.moonstone.components.ComponentElement
import net.sourceforge.moonstone.components.ComponentFactory
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.components.UIElementWrapper
import net.sourceforge.moonstone.runtime.DerivedStateCell
import net.sourceforge.moonstone.runtime.PropParser
import net.sourceforge.moonstone.runtime.StateCell
import kotlin.reflect.KClass

/**
 * Text display component.
 */
class TextComponent : ComponentFactory {
    override val name = "text"
    override val acceptsChildren = false

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "value" to Any::class,
            "style" to String::class,
            "color" to String::class,
            "font-size" to Number::class,
            "font-weight" to String::class,
            "max-lines" to Number::class,
        )

    private val propParser = PropParser()

    override fun create(params: Array<LispObject>): LispObject {
        val (props, _) = propParser.parse(params)
        val finalProps = props.toMutableMap()

        // Handle positional string argument for value
        if (!finalProps.containsKey("value") && params.isNotEmpty()) {
            val firstArg = params[0]
            if (firstArg.asKeyword() == null) {
                val value =
                    when {
                        firstArg.asString() != null -> firstArg.asString().value()
                        firstArg.asInt() != null -> firstArg.asInt().value.toString()
                        firstArg.asDouble() != null -> firstArg.asDouble().value.toString()
                        else -> firstArg.asObject()?.toString() ?: ""
                    }
                finalProps["value"] = value
            }
        }

        val element =
            ComponentElement(
                type = name,
                props = finalProps,
                children = emptyList(),
            )

        return JavaObject(UIElementWrapper(element))
    }

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        // Read state values directly in the composable to ensure Compose tracks the dependency
        val rawValue = element.props["value"]
        val value =
            when (rawValue) {
                is StateCell -> {
                    // Read the state value here in the composable context
                    val stateValue = rawValue.value
                    stateValue.asObject() ?: ""
                }
                is DerivedStateCell -> {
                    val stateValue = rawValue.value
                    stateValue.asObject() ?: ""
                }
                null -> ""
                else -> rawValue
            }

        val parsedStyle = parseTextStyle(element.props["style"])
        val parsedColor = parseColor(element.props["color"])
        val parsedFontSize = (element.props["font-size"] as? Number)?.toInt()?.sp
        val parsedFontWeight = parseFontWeight(element.props["font-weight"])
        val maxLines = (element.props["max-lines"] as? Number)?.toInt() ?: Int.MAX_VALUE

        val hasIndividualProps = parsedColor != null || parsedFontSize != null || parsedFontWeight != null

        when {
            // Style is explicitly set - use only the style, ignore individual props
            parsedStyle != null ->
                Text(
                    text = value.toString(),
                    style = parsedStyle,
                    maxLines = maxLines,
                )
            // Individual props are set but no style - use them without forcing a default style
            hasIndividualProps ->
                Text(
                    text = value.toString(),
                    color = parsedColor ?: Color.Unspecified,
                    fontSize = parsedFontSize ?: TextUnit.Unspecified,
                    fontWeight = parsedFontWeight,
                    maxLines = maxLines,
                )
            // Nothing is set - use default style
            else ->
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = maxLines,
                )
        }
    }

    @Composable
    private fun parseTextStyle(value: Any?): TextStyle? =
        when (value) {
            "display-large" -> MaterialTheme.typography.displayLarge
            "display-medium" -> MaterialTheme.typography.displayMedium
            "display-small" -> MaterialTheme.typography.displaySmall
            "headline-large" -> MaterialTheme.typography.headlineLarge
            "headline-medium" -> MaterialTheme.typography.headlineMedium
            "headline-small" -> MaterialTheme.typography.headlineSmall
            "title-large" -> MaterialTheme.typography.titleLarge
            "title-medium" -> MaterialTheme.typography.titleMedium
            "title-small" -> MaterialTheme.typography.titleSmall
            "body-large" -> MaterialTheme.typography.bodyLarge
            "body-medium" -> MaterialTheme.typography.bodyMedium
            "body-small" -> MaterialTheme.typography.bodySmall
            "label-large" -> MaterialTheme.typography.labelLarge
            "label-medium" -> MaterialTheme.typography.labelMedium
            "label-small" -> MaterialTheme.typography.labelSmall
            else -> null
        }

    private fun parseColor(value: Any?): Color? =
        when (value) {
            "red" -> Color.Red
            "green" -> Color.Green
            "blue" -> Color.Blue
            "white" -> Color.White
            "black" -> Color.Black
            "gray", "grey" -> Color.Gray
            "cyan" -> Color.Cyan
            "magenta" -> Color.Magenta
            "yellow" -> Color.Yellow
            is String ->
                if (value.startsWith("#")) {
                    parseHexColor(value)
                } else {
                    null
                }
            else -> null
        }

    private fun parseHexColor(hex: String): Color? {
        return try {
            val colorStr = hex.removePrefix("#")
            val colorLong =
                when (colorStr.length) {
                    6 -> colorStr.toLong(16) or 0xFF000000
                    8 -> colorStr.toLong(16)
                    else -> return null
                }
            Color(colorLong.toInt())
        } catch (e: Exception) {
            null
        }
    }

    private fun parseFontWeight(value: Any?): FontWeight? =
        when (value) {
            "bold" -> FontWeight.Bold
            "normal" -> FontWeight.Normal
            "light" -> FontWeight.Light
            "thin" -> FontWeight.Thin
            "medium" -> FontWeight.Medium
            "semi-bold" -> FontWeight.SemiBold
            "extra-bold" -> FontWeight.ExtraBold
            else -> null
        }
}
