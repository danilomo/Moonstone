package net.sourceforge.moonstone.components

/**
 * Base interface for all UI elements in the tree.
 */
sealed interface UIElement {
    val type: String
    val props: Map<String, Any?>
    val children: List<UIElement>
}

/**
 * A generic component element representing any registered component.
 */
data class ComponentElement(
    override val type: String,
    override val props: Map<String, Any?>,
    override val children: List<UIElement>,
) : UIElement

/**
 * A text element for displaying text content.
 */
data class TextElement(
    val value: String,
    val style: String? = null,
    val color: String? = null,
) : UIElement {
    override val type = "text"
    override val props: Map<String, Any?> =
        buildMap {
            put("value", value)
            style?.let { put("style", it) }
            color?.let { put("color", it) }
        }
    override val children: List<UIElement> = emptyList()
}

/**
 * Marker class to wrap UIElement as a LispObject for use in the interpreter.
 */
class UIElementWrapper(
    val element: UIElement,
) {
    override fun toString(): String = "UIElement(${element.type})"
}
