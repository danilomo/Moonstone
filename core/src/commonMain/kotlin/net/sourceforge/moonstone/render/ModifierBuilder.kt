package net.sourceforge.moonstone.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.objects.BooleanObject

/**
 * Utility object for building Compose modifiers from property maps.
 */
object ModifierBuilder {
    /**
     * Build a Modifier from a property map.
     */
    fun build(props: Map<String, Any?>): Modifier {
        var modifier: Modifier = Modifier

        // Fill modifiers
        if (isTruthy(props["fill-max-size"])) {
            modifier = modifier.fillMaxSize()
        }
        if (isTruthy(props["fill-max-width"])) {
            modifier = modifier.fillMaxWidth()
        }
        if (isTruthy(props["fill-max-height"])) {
            modifier = modifier.fillMaxHeight()
        }

        // Size modifiers
        (props["width"] as? Number)?.let {
            modifier = modifier.width(it.toInt().dp)
        }
        (props["height"] as? Number)?.let {
            modifier = modifier.height(it.toInt().dp)
        }

        // Padding modifiers
        (props["padding"] as? Number)?.let {
            modifier = modifier.padding(it.toInt().dp)
        }
        (props["padding-horizontal"] as? Number)?.let {
            modifier = modifier.padding(horizontal = it.toInt().dp)
        }
        (props["padding-vertical"] as? Number)?.let {
            modifier = modifier.padding(vertical = it.toInt().dp)
        }

        // Background
        props["background"]?.let {
            parseColor(it)?.let { color ->
                modifier = modifier.background(color)
            }
        }

        return modifier
    }

    /**
     * Check if a value is truthy (non-null, non-zero, non-empty).
     */
    fun isTruthy(value: Any?): Boolean = when (value) {
        null -> false
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value.isNotEmpty() && value != "false"
        is LispObject -> {
            when {
                value.asInt() != null -> value.asInt().value != 0
                value == BooleanObject.TRUE -> true
                value == BooleanObject.FALSE -> false
                value.asString() != null -> value.asString().value().isNotEmpty()
                else -> true
            }
        }
        else -> true
    }

    /**
     * Parse a color from various formats.
     */
    fun parseColor(value: Any?): Color? = when (value) {
        "red" -> Color.Red
        "green" -> Color.Green
        "blue" -> Color.Blue
        "white" -> Color.White
        "black" -> Color.Black
        "gray", "grey" -> Color.Gray
        "cyan" -> Color.Cyan
        "magenta" -> Color.Magenta
        "yellow" -> Color.Yellow
        "transparent" -> Color.Transparent
        is String -> if (value.startsWith("#")) {
            parseHexColor(value)
        } else null
        else -> null
    }

    /**
     * Parse a hex color string.
     */
    fun parseHexColor(hex: String): Color? {
        return try {
            val colorStr = hex.removePrefix("#")
            val colorLong = when (colorStr.length) {
                6 -> colorStr.toLong(16) or 0xFF000000
                8 -> colorStr.toLong(16)
                else -> return null
            }
            Color(colorLong.toInt())
        } catch (e: Exception) {
            null
        }
    }
}
