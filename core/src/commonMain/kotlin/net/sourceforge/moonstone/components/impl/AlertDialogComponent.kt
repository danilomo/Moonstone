package net.sourceforge.moonstone.components.impl

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import net.sourceforge.kleinlisp.objects.FunctionObject
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import net.sourceforge.moonstone.runtime.StateCell
import kotlin.reflect.KClass

/**
 * Alert dialog component for modal confirmations.
 *
 * Scheme usage:
 * (alert-dialog #:visible show-dialog
 *   #:title "Confirm"
 *   #:text "Are you sure?"
 *   #:confirm-button (button #:on-click confirm-handler (text "Yes"))
 *   #:dismiss-button (button #:on-click dismiss-handler (text "No"))
 *   #:on-dismiss (lambda () (state-set! show-dialog 0)))
 */
class AlertDialogComponent : AbstractComponent() {
    override val name = "alert-dialog"
    override val acceptsChildren = false

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "visible" to Any::class,
            "title" to String::class,
            "text" to String::class,
            "confirm-button" to Any::class,
            "dismiss-button" to Any::class,
            "on-dismiss" to Any::class,
            "icon" to Any::class,
        )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        val visibleValue = element.props["visible"]
        val isVisible =
            when (visibleValue) {
                is StateCell -> ModifierBuilder.isTruthy(visibleValue.value)
                else -> ModifierBuilder.isTruthy(visibleValue)
            }

        if (!isVisible) return

        val title = element.props["title"]?.toString()
        val text = element.props["text"]?.toString()
        val confirmButton = element.props["confirm-button"] as? UIElement
        val dismissButton = element.props["dismiss-button"] as? UIElement
        val onDismissHandler = element.props["on-dismiss"] as? FunctionObject
        val icon = element.props["icon"] as? UIElement

        val dismissRequest: () -> Unit = {
            onDismissHandler?.function()?.evaluate(emptyArray())
        }

        AlertDialog(
            onDismissRequest = dismissRequest,
            title = title?.let { { Text(it) } },
            text = text?.let { { Text(it) } },
            icon = icon?.let { { renderChild(it) } },
            confirmButton = {
                confirmButton?.let { renderChild(it) }
            },
            dismissButton = dismissButton?.let { { renderChild(it) } },
        )
    }
}
