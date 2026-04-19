package net.sourceforge.moonstone.components.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import net.sourceforge.kleinlisp.objects.FunctionObject
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import net.sourceforge.moonstone.runtime.StateCell
import kotlin.reflect.KClass

/**
 * Bottom sheet component for modal content from the bottom.
 *
 * Scheme usage:
 * (bottom-sheet #:visible sheet-visible #:on-dismiss dismiss-handler
 *   (column #:padding 16
 *     (text #:value "Sheet content")))
 */
class BottomSheetComponent : AbstractComponent() {
    override val name = "bottom-sheet"
    override val acceptsChildren = true

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "visible" to Any::class,
            "on-dismiss" to Any::class,
            "skip-partially-expanded" to Boolean::class,
        )

    @OptIn(ExperimentalMaterial3Api::class)
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

        val onDismissHandler = element.props["on-dismiss"] as? FunctionObject
        val skipPartiallyExpanded =
            element.props["skip-partially-expanded"]?.let {
                ModifierBuilder.isTruthy(it)
            } ?: false

        val sheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = skipPartiallyExpanded,
            )

        // Show/hide based on visibility
        LaunchedEffect(isVisible) {
            if (isVisible) {
                sheetState.show()
            } else {
                sheetState.hide()
            }
        }

        if (isVisible) {
            ModalBottomSheet(
                onDismissRequest = {
                    onDismissHandler?.function()?.evaluate(emptyArray())
                },
                sheetState = sheetState,
            ) {
                element.children.forEach { child ->
                    renderChild(child)
                }
            }
        }
    }
}
