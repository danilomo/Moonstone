package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import net.sourceforge.kleinlisp.objects.FunctionObject
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import kotlin.reflect.KClass

/**
 * Scaffold component providing Material Design layout structure.
 *
 * Scheme usage:
 * (scaffold
 *   #:top-bar (top-app-bar #:title "My App")
 *   #:bottom-bar (bottom-navigation ...)
 *   #:floating-action-button (button ...)
 *   #:on-start (lambda () ...)  ; called once when app starts
 *   #:on-resume (lambda () ...) ; called when app resumes/recomposes
 *   #:on-close (lambda () ...)  ; called when app closes
 *   (column ...))  ; main content
 */
class ScaffoldComponent : AbstractComponent() {
    override val name = "scaffold"
    override val acceptsChildren = true

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "top-bar" to Any::class,
            "bottom-bar" to Any::class,
            "floating-action-button" to Any::class,
            "floating-action-button-position" to String::class,
            "on-start" to Any::class,
            "on-resume" to Any::class,
            "on-close" to Any::class,
        )

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        val topBar = element.props["top-bar"] as? UIElement
        val bottomBar = element.props["bottom-bar"] as? UIElement
        val fab = element.props["floating-action-button"] as? UIElement
        val fabPosition = parseFabPosition(element.props["floating-action-button-position"]?.toString())

        val onStart = element.props["on-start"] as? FunctionObject
        val onResume = element.props["on-resume"] as? FunctionObject
        val onClose = element.props["on-close"] as? FunctionObject

        // on-start: called once when the scaffold is first composed
        onStart?.let { callback ->
            LaunchedEffect(Unit) {
                callback.function().evaluate(emptyArray())
            }
        }

        // on-resume: called every time the scaffold recomposes
        onResume?.let { callback ->
            LaunchedEffect(true) {
                callback.function().evaluate(emptyArray())
            }
        }

        // on-close: called when the scaffold is disposed
        onClose?.let { callback ->
            DisposableEffect(Unit) {
                onDispose {
                    callback.function().evaluate(emptyArray())
                }
            }
        }

        Scaffold(
            topBar = {
                topBar?.let { renderChild(it) }
            },
            bottomBar = {
                bottomBar?.let { renderChild(it) }
            },
            floatingActionButton = {
                fab?.let { renderChild(it) }
            },
            floatingActionButtonPosition = fabPosition,
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                element.children.forEach { child ->
                    renderChild(child)
                }
            }
        }
    }

    private fun parseFabPosition(value: String?): FabPosition =
        when (value) {
            "center" -> FabPosition.Center
            "end" -> FabPosition.End
            "end-overlay" -> FabPosition.EndOverlay
            else -> FabPosition.End
        }
}
