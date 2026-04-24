package net.sourceforge.moonstone.render

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.sourceforge.moonstone.components.ComponentRegistry
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.error.ErrorContext
import net.sourceforge.moonstone.runtime.StateManager

/**
 * Renders UIElement trees to Compose UI.
 */
class UIRenderer(
    private val componentRegistry: ComponentRegistry,
    @Suppress("UNUSED_PARAMETER") // TODO: May be needed for future state management features
    private val stateManager: StateManager,
) {
    /**
     * Render the root element tree.
     */
    @Composable
    fun RenderRoot(element: UIElement) {
        MaterialTheme {
            Render(element)
        }
    }

    /**
     * Render a single UIElement.
     */
    @Composable
    fun Render(element: UIElement) {
        val component = componentRegistry.get(element.type)
        if (component != null) {
            // Track component in context for error messages
            ErrorContext.pushComponent(element.type)
            component.Render(element) { child ->
                Render(child)
            }
            ErrorContext.popComponent()
        } else {
            // Display inline error for unknown components
            RenderUnknownComponent(
                componentName = element.type,
                availableComponents = componentRegistry.getNames().toList(),
            )
        }
    }

    /**
     * Display an inline error for unknown components.
     */
    @Composable
    private fun RenderUnknownComponent(
        componentName: String,
        availableComponents: List<String>,
    ) {
        Box(
            modifier =
                Modifier
                    .padding(4.dp)
                    .background(
                        Color(0xFFFFF8E1),
                        RoundedCornerShape(4.dp),
                    ).border(
                        1.dp,
                        Color(0xFFFF8F00),
                        RoundedCornerShape(4.dp),
                    ).padding(8.dp),
        ) {
            Column {
                Text(
                    text = "Unknown component: $componentName",
                    color = Color(0xFFE65100),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                Text(
                    text = "Available: ${availableComponents.sorted().joinToString(", ")}",
                    color = Color(0xFF666666),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
