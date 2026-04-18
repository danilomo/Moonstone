package net.sourceforge.moonstone.components

import androidx.compose.runtime.Composable
import net.sourceforge.kleinlisp.LispObject
import kotlin.reflect.KClass

/**
 * Factory interface for creating UI components from Scheme code.
 */
interface ComponentFactory {
    /**
     * Component identifier (e.g., "button", "text-field")
     */
    val name: String

    /**
     * List of required property names
     */
    val requiredProps: List<String>
        get() = emptyList()

    /**
     * Map of property names to their expected types
     */
    val propTypes: Map<String, KClass<*>>
        get() = emptyMap()

    /**
     * Whether this component accepts children
     */
    val acceptsChildren: Boolean
        get() = true

    /**
     * Create a UI element from Scheme parameters.
     * Returns a JavaObject wrapping a UIElementWrapper.
     */
    fun create(params: Array<LispObject>): LispObject

    /**
     * Render this component to Compose.
     * This is called during composition to produce the actual UI.
     */
    @Composable
    fun Render(element: UIElement, renderChild: @Composable (UIElement) -> Unit)
}
