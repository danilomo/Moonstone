package net.sourceforge.moonstone.components.impl

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import net.sourceforge.kleinlisp.objects.FunctionObject
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import net.sourceforge.moonstone.runtime.StateCell
import kotlin.reflect.KClass

/**
 * Snackbar component for brief notifications.
 *
 * Scheme usage:
 * (snackbar #:visible snackbar-state
 *   #:message "Operation completed"
 *   #:action-label "Undo"
 *   #:on-action undo-handler
 *   #:on-dismiss dismiss-handler)
 */
class SnackbarComponent : AbstractComponent() {
    override val name = "snackbar"
    override val acceptsChildren = false

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "visible" to Any::class,
            "message" to String::class,
            "action-label" to String::class,
            "on-action" to Any::class,
            "on-dismiss" to Any::class,
            "duration" to String::class,
        )

    override val requiredProps = listOf("message")

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

        val message = element.props["message"]?.toString() ?: ""
        val actionLabel = element.props["action-label"]?.toString()
        val onActionHandler = element.props["on-action"] as? FunctionObject
        val onDismissHandler = element.props["on-dismiss"] as? FunctionObject

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(message, isVisible) {
            if (isVisible && message.isNotEmpty()) {
                val result =
                    snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = actionLabel,
                        duration = SnackbarDuration.Short,
                    )
                when (result) {
                    androidx.compose.material3.SnackbarResult.ActionPerformed -> {
                        onActionHandler?.function()?.evaluate(emptyArray())
                    }
                    androidx.compose.material3.SnackbarResult.Dismissed -> {
                        onDismissHandler?.function()?.evaluate(emptyArray())
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState) { data ->
            Snackbar(
                snackbarData = data,
            )
        }
    }
}
