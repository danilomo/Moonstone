package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import kotlin.reflect.KClass

/**
 * Error boundary component that wraps children.
 *
 * Note: Due to Compose limitations, try-catch around composable functions
 * is not supported. This component provides a structural marker for error
 * handling zones. The fallback and on-error props are reserved for future
 * use when error handling can be implemented at a higher level.
 *
 * Scheme usage:
 * (error-boundary
 *   #:fallback (text #:value "Something went wrong")
 *   #:on-error (lambda (e) (println e))
 *   (child-components))
 */
class ErrorBoundaryComponent : AbstractComponent() {
    override val name = "error-boundary"
    override val acceptsChildren = true

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "fallback" to Any::class,
            "on-error" to Any::class,
        )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        // Render children normally - error handling props reserved for future use
        Box(
            modifier =
                Modifier.border(
                    width = 0.dp,
                    color = Color.Transparent,
                ),
        ) {
            element.children.forEach { child ->
                renderChild(child)
            }
        }
    }
}
