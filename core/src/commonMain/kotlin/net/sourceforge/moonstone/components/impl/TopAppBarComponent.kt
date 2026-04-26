package net.sourceforge.moonstone.components.impl

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.ComponentElement
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.runtime.StateCell
import kotlin.reflect.KClass

/**
 * Top app bar component for navigation and actions.
 *
 * Scheme usage:
 * (top-app-bar #:title "App Title")
 * (top-app-bar #:title "App Title" #:style 'center-aligned
 *   #:navigation-icon (icon #:name "menu" #:on-click ...)
 *   #:actions (row (icon #:name "search") (icon #:name "more")))
 */
class TopAppBarComponent : AbstractComponent() {
    override val name = "top-app-bar"
    override val acceptsChildren = false

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "title" to String::class,
            "style" to String::class,
            "navigation-icon" to Any::class,
            "actions" to Any::class,
        )

    private data class TopAppBarConfig(
        val title: String,
        val style: String,
        val navigationIcon: UIElement?,
        val actions: UIElement?,
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        val config = parseTopAppBarConfig(element.props)

        val titleContent: @Composable () -> Unit = { Text(config.title) }
        val navigationIconContent: @Composable () -> Unit = {
            config.navigationIcon?.let { renderChild(it) }
        }
        val actionsContent: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
            renderActionsContent(config.actions, renderChild)
        }

        when (config.style) {
            "small" -> TopAppBar(title = titleContent, navigationIcon = navigationIconContent, actions = actionsContent)
            "center-aligned" ->
                CenterAlignedTopAppBar(
                    title = titleContent,
                    navigationIcon = navigationIconContent,
                    actions = actionsContent,
                )
            "medium" ->
                MediumTopAppBar(
                    title = titleContent,
                    navigationIcon = navigationIconContent,
                    actions = actionsContent,
                )
            "large" ->
                LargeTopAppBar(
                    title = titleContent,
                    navigationIcon = navigationIconContent,
                    actions = actionsContent,
                )
            else -> TopAppBar(title = titleContent, navigationIcon = navigationIconContent, actions = actionsContent)
        }
    }

    private fun parseTopAppBarConfig(props: Map<String, Any?>): TopAppBarConfig {
        val titleValue = props["title"]
        val title =
            when (titleValue) {
                is StateCell -> titleValue.value?.toString() ?: ""
                else -> titleValue?.toString() ?: ""
            }

        return TopAppBarConfig(
            title = title,
            style = props["style"]?.toString() ?: "small",
            navigationIcon = props["navigation-icon"] as? UIElement,
            actions = props["actions"] as? UIElement,
        )
    }

    @Composable
    private fun androidx.compose.foundation.layout.RowScope.renderActionsContent(
        actions: UIElement?,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        actions?.let {
            if (it is ComponentElement && it.type == "row") {
                it.children.forEach { child -> renderChild(child) }
            } else {
                renderChild(it)
            }
        }
    }
}
