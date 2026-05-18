package net.sourceforge.moonstone.components.impl

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.objects.JavaObject
import net.sourceforge.moonstone.components.ComponentElement
import net.sourceforge.moonstone.components.ComponentFactory
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.components.UIElementWrapper
import net.sourceforge.moonstone.render.ModifierBuilder
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
            PROP_VALUE to Any::class,
            "style" to String::class,
            "color" to String::class,
            "font-size" to Number::class,
            "font-weight" to String::class,
            "max-lines" to Number::class,
            "text-align" to String::class,
            "strikethrough" to Boolean::class,
        )

    private val propParser = PropParser()

    private data class TextRenderProps(
        val color: Color? = null,
        val fontSize: TextUnit? = null,
        val fontWeight: FontWeight? = null,
        val maxLines: Int = Int.MAX_VALUE,
        val textAlign: TextAlign? = null,
        val textDecoration: TextDecoration? = null,
    )

    companion object {
        private const val PROP_VALUE = "value"
    }

    override fun create(params: Array<LispObject>): LispObject {
        val (props, _) = propParser.parse(params)
        val finalProps = props.toMutableMap()

        // Handle positional string argument for value
        if (!finalProps.containsKey(PROP_VALUE) && params.isNotEmpty()) {
            val firstArg = params[0]
            if (firstArg.asKeyword() == null) {
                val value =
                    when {
                        firstArg.asString() != null -> firstArg.asString().value()
                        firstArg.asInt() != null -> firstArg.asInt().value.toString()
                        firstArg.asDouble() != null -> firstArg.asDouble().value.toString()
                        else -> firstArg.asObject()?.toString() ?: ""
                    }
                finalProps[PROP_VALUE] = value
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
        val value = readTextValue(element.props[PROP_VALUE])
        val parsedStyle = parseTextStyle(element.props["style"])
        val strikethrough = ModifierBuilder.isTruthy(element.props["strikethrough"])
        val props =
            TextRenderProps(
                color = parseColor(element.props["color"]),
                fontSize = (element.props["font-size"] as? Number)?.toInt()?.sp,
                fontWeight = parseFontWeight(element.props["font-weight"]),
                maxLines = (element.props["max-lines"] as? Number)?.toInt() ?: Int.MAX_VALUE,
                textAlign = parseTextAlign(element.props["text-align"]),
                textDecoration = if (strikethrough) TextDecoration.LineThrough else null,
            )
        val hasIndividualProps = props.color != null || props.fontSize != null || props.fontWeight != null
        when {
            parsedStyle != null -> renderStyledText(value, parsedStyle, props)
            hasIndividualProps -> renderCustomText(value, props)
            else -> renderDefaultText(value, props)
        }
    }

    @Composable
    private fun readTextValue(rawValue: Any?): Any =
        when (rawValue) {
            is StateCell -> rawValue.value.asObject() ?: ""
            is DerivedStateCell -> rawValue.value.asObject() ?: ""
            null -> ""
            else -> rawValue
        }

    @Composable
    private fun renderStyledText(
        value: Any,
        style: TextStyle,
        props: TextRenderProps,
    ) {
        Text(
            text = value.toString(),
            style = style,
            maxLines = props.maxLines,
            textAlign = props.textAlign,
            textDecoration = props.textDecoration,
        )
    }

    @Composable
    private fun renderCustomText(
        value: Any,
        props: TextRenderProps,
    ) {
        Text(
            text = value.toString(),
            color = props.color ?: Color.Unspecified,
            fontSize = props.fontSize ?: TextUnit.Unspecified,
            fontWeight = props.fontWeight,
            maxLines = props.maxLines,
            textAlign = props.textAlign,
            textDecoration = props.textDecoration,
        )
    }

    @Composable
    private fun renderDefaultText(
        value: Any,
        props: TextRenderProps,
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = props.maxLines,
            textAlign = props.textAlign,
            textDecoration = props.textDecoration,
        )
    }

    private fun parseTextAlign(value: Any?): TextAlign? =
        when (value?.toString()) {
            "start", "left" -> TextAlign.Start
            "end", "right" -> TextAlign.End
            "center" -> TextAlign.Center
            "justify" -> TextAlign.Justify
            else -> null
        }

    @Composable
    private fun parseTextStyle(value: Any?): TextStyle? =
        when (value) {
            "display-large", "display-medium", "display-small" -> parseDisplayStyle(value as String)
            "headline-large", "headline-medium", "headline-small" -> parseHeadlineStyle(value as String)
            "title-large", "title-medium", "title-small" -> parseTitleStyle(value as String)
            "body-large", "body-medium", "body-small" -> parseBodyStyle(value as String)
            "label-large", "label-medium", "label-small" -> parseLabelStyle(value as String)
            else -> null
        }

    @Composable
    private fun parseDisplayStyle(value: String): TextStyle =
        when (value) {
            "display-large" -> MaterialTheme.typography.displayLarge
            "display-medium" -> MaterialTheme.typography.displayMedium
            else -> MaterialTheme.typography.displaySmall
        }

    @Composable
    private fun parseHeadlineStyle(value: String): TextStyle =
        when (value) {
            "headline-large" -> MaterialTheme.typography.headlineLarge
            "headline-medium" -> MaterialTheme.typography.headlineMedium
            else -> MaterialTheme.typography.headlineSmall
        }

    @Composable
    private fun parseTitleStyle(value: String): TextStyle =
        when (value) {
            "title-large" -> MaterialTheme.typography.titleLarge
            "title-medium" -> MaterialTheme.typography.titleMedium
            else -> MaterialTheme.typography.titleSmall
        }

    @Composable
    private fun parseBodyStyle(value: String): TextStyle =
        when (value) {
            "body-large" -> MaterialTheme.typography.bodyLarge
            "body-medium" -> MaterialTheme.typography.bodyMedium
            else -> MaterialTheme.typography.bodySmall
        }

    @Composable
    private fun parseLabelStyle(value: String): TextStyle =
        when (value) {
            "label-large" -> MaterialTheme.typography.labelLarge
            "label-medium" -> MaterialTheme.typography.labelMedium
            else -> MaterialTheme.typography.labelSmall
        }

    @Suppress("CyclomaticComplexMethod")
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
