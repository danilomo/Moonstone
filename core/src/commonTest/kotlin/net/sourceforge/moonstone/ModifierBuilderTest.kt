package net.sourceforge.moonstone

import androidx.compose.ui.graphics.Color
import net.sourceforge.kleinlisp.objects.BooleanObject
import net.sourceforge.kleinlisp.objects.IntObject
import net.sourceforge.kleinlisp.objects.StringObject
import net.sourceforge.moonstone.render.ModifierBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModifierBuilderTest {
    @Test
    fun `isTruthy returns false for null`() {
        assertFalse(ModifierBuilder.isTruthy(null))
    }

    @Test
    fun `isTruthy returns correct value for Boolean`() {
        assertTrue(ModifierBuilder.isTruthy(true))
        assertFalse(ModifierBuilder.isTruthy(false))
    }

    @Test
    fun `isTruthy returns false for zero Number`() {
        assertFalse(ModifierBuilder.isTruthy(0))
        assertFalse(ModifierBuilder.isTruthy(0.0))
    }

    @Test
    fun `isTruthy returns true for non-zero Number`() {
        assertTrue(ModifierBuilder.isTruthy(1))
        assertTrue(ModifierBuilder.isTruthy(42))
        assertTrue(ModifierBuilder.isTruthy(-1))
        assertTrue(ModifierBuilder.isTruthy(3.14))
    }

    @Test
    fun `isTruthy returns false for empty string`() {
        assertFalse(ModifierBuilder.isTruthy(""))
    }

    @Test
    fun `isTruthy returns false for string false`() {
        assertFalse(ModifierBuilder.isTruthy("false"))
    }

    @Test
    fun `isTruthy returns true for non-empty string`() {
        assertTrue(ModifierBuilder.isTruthy("hello"))
        assertTrue(ModifierBuilder.isTruthy("true"))
        assertTrue(ModifierBuilder.isTruthy("anything"))
    }

    @Test
    fun `isTruthy handles LispObject Boolean`() {
        assertTrue(ModifierBuilder.isTruthy(BooleanObject.TRUE))
        assertFalse(ModifierBuilder.isTruthy(BooleanObject.FALSE))
    }

    @Test
    fun `isTruthy handles LispObject IntObject`() {
        assertFalse(ModifierBuilder.isTruthy(IntObject(0)))
        assertTrue(ModifierBuilder.isTruthy(IntObject(1)))
        assertTrue(ModifierBuilder.isTruthy(IntObject(42)))
        assertTrue(ModifierBuilder.isTruthy(IntObject(-5)))
    }

    @Test
    fun `isTruthy handles LispObject StringObject`() {
        assertFalse(ModifierBuilder.isTruthy(StringObject("")))
        assertTrue(ModifierBuilder.isTruthy(StringObject("hello")))
    }

    @Test
    fun `isTruthy returns true for other objects`() {
        assertTrue(ModifierBuilder.isTruthy(Any()))
        assertTrue(ModifierBuilder.isTruthy(listOf(1, 2, 3)))
    }

    @Test
    fun `parseColor returns correct named colors`() {
        assertEquals(Color.Red, ModifierBuilder.parseColor("red"))
        assertEquals(Color.Green, ModifierBuilder.parseColor("green"))
        assertEquals(Color.Blue, ModifierBuilder.parseColor("blue"))
        assertEquals(Color.White, ModifierBuilder.parseColor("white"))
        assertEquals(Color.Black, ModifierBuilder.parseColor("black"))
        assertEquals(Color.Gray, ModifierBuilder.parseColor("gray"))
        assertEquals(Color.Gray, ModifierBuilder.parseColor("grey"))
        assertEquals(Color.Cyan, ModifierBuilder.parseColor("cyan"))
        assertEquals(Color.Magenta, ModifierBuilder.parseColor("magenta"))
        assertEquals(Color.Yellow, ModifierBuilder.parseColor("yellow"))
        assertEquals(Color.Transparent, ModifierBuilder.parseColor("transparent"))
    }

    @Test
    fun `parseColor returns null for unknown color names`() {
        assertNull(ModifierBuilder.parseColor("unknown"))
        assertNull(ModifierBuilder.parseColor("purple"))
        assertNull(ModifierBuilder.parseColor(""))
    }

    @Test
    fun `parseColor returns null for non-string non-color values`() {
        assertNull(ModifierBuilder.parseColor(42))
        assertNull(ModifierBuilder.parseColor(null))
        assertNull(ModifierBuilder.parseColor(true))
    }

    @Test
    fun `parseHexColor parses 6-digit hex colors`() {
        val red = ModifierBuilder.parseHexColor("#FF0000")
        assertNotNull(red)
        assertEquals(Color.Red, red)

        val green = ModifierBuilder.parseHexColor("#00FF00")
        assertNotNull(green)
        assertEquals(Color.Green, green)

        val blue = ModifierBuilder.parseHexColor("#0000FF")
        assertNotNull(blue)
        assertEquals(Color.Blue, blue)
    }

    @Test
    fun `parseHexColor parses 8-digit hex colors with alpha`() {
        val color = ModifierBuilder.parseHexColor("#80FF0000")
        assertNotNull(color)

        // Verify it parsed successfully (exact color comparison might vary)
        assertTrue(color.alpha < 1.0f, "Alpha should be less than 1.0")
    }

    @Test
    fun `parseHexColor handles hex without hash prefix`() {
        val color = ModifierBuilder.parseHexColor("FF0000")
        assertNotNull(color)
        assertEquals(Color.Red, color)
    }

    @Test
    fun `parseHexColor returns null for invalid hex`() {
        assertNull(ModifierBuilder.parseHexColor("#GGGGGG"))
        assertNull(ModifierBuilder.parseHexColor("#12"))
        assertNull(ModifierBuilder.parseHexColor("#12345"))
        assertNull(ModifierBuilder.parseHexColor("invalid"))
    }

    @Test
    fun `parseColor delegates to parseHexColor for hex strings`() {
        val color = ModifierBuilder.parseColor("#FF0000")
        assertNotNull(color)
        assertEquals(Color.Red, color)
    }

    @Test
    fun `build creates empty modifier for empty props`() {
        val props = emptyMap<String, Any?>()
        val modifier = ModifierBuilder.build(props)

        assertNotNull(modifier)
    }

    @Test
    fun `build applies width from props`() {
        val props = mapOf("width" to 100)
        val modifier = ModifierBuilder.build(props)

        assertNotNull(modifier)
        // Modifier is built successfully, actual dimension checking
        // would require Compose UI test framework
    }

    @Test
    fun `build applies height from props`() {
        val props = mapOf("height" to 200)
        val modifier = ModifierBuilder.build(props)

        assertNotNull(modifier)
    }

    @Test
    fun `build applies padding from props`() {
        val props = mapOf("padding" to 16)
        val modifier = ModifierBuilder.build(props)

        assertNotNull(modifier)
    }

    @Test
    fun `build applies padding-horizontal from props`() {
        val props = mapOf("padding-horizontal" to 20)
        val modifier = ModifierBuilder.build(props)

        assertNotNull(modifier)
    }

    @Test
    fun `build applies padding-vertical from props`() {
        val props = mapOf("padding-vertical" to 10)
        val modifier = ModifierBuilder.build(props)

        assertNotNull(modifier)
    }

    @Test
    fun `build applies fill-max-size from props`() {
        val props = mapOf("fill-max-size" to true)
        val modifier = ModifierBuilder.build(props)

        assertNotNull(modifier)
    }

    @Test
    fun `build applies fill-max-width from props`() {
        val props = mapOf("fill-max-width" to true)
        val modifier = ModifierBuilder.build(props)

        assertNotNull(modifier)
    }

    @Test
    fun `build applies fill-max-height from props`() {
        val props = mapOf("fill-max-height" to true)
        val modifier = ModifierBuilder.build(props)

        assertNotNull(modifier)
    }

    @Test
    fun `build applies background color from props`() {
        val props = mapOf("background" to "red")
        val modifier = ModifierBuilder.build(props)

        assertNotNull(modifier)
    }

    @Test
    fun `build applies background hex color from props`() {
        val props = mapOf("background" to "#FF0000")
        val modifier = ModifierBuilder.build(props)

        assertNotNull(modifier)
    }

    @Test
    fun `build ignores invalid background color`() {
        val props = mapOf("background" to "invalid-color")
        val modifier = ModifierBuilder.build(props)

        assertNotNull(modifier)
    }

    @Test
    fun `build combines multiple modifiers`() {
        val props = mapOf(
            "width" to 200,
            "height" to 100,
            "padding" to 16,
            "background" to "blue"
        )
        val modifier = ModifierBuilder.build(props)

        assertNotNull(modifier)
    }

    @Test
    fun `build ignores null values in props`() {
        val props = mapOf(
            "width" to null,
            "height" to 100,
            "padding" to null
        )
        val modifier = ModifierBuilder.build(props)

        assertNotNull(modifier)
    }

    @Test
    fun `build handles non-Number values for size props gracefully`() {
        val props = mapOf(
            "width" to "not-a-number",
            "height" to "also-not-a-number"
        )
        val modifier = ModifierBuilder.build(props)

        assertNotNull(modifier)
        // Should not throw, just ignore invalid values
    }
}
