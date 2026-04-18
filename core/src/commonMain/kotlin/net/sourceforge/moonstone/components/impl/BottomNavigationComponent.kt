package net.sourceforge.moonstone.components.impl

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import net.sourceforge.moonstone.runtime.StateCell
import net.sourceforge.kleinlisp.objects.FunctionObject
import kotlin.reflect.KClass

/**
 * Bottom navigation component for app navigation.
 *
 * Scheme usage:
 * (bottom-navigation #:selected tab-state
 *   (nav-item #:icon "home" #:label "Home" #:value 0 #:on-select handler)
 *   (nav-item #:icon "search" #:label "Search" #:value 1 #:on-select handler)
 *   (nav-item #:icon "profile" #:label "Profile" #:value 2 #:on-select handler))
 */
class BottomNavigationComponent : AbstractComponent() {
    override val name = "bottom-navigation"
    override val acceptsChildren = true

    override val propTypes: Map<String, KClass<*>> = mapOf(
        "selected" to Any::class
    )

    @Composable
    override fun Render(element: UIElement, renderChild: @Composable (UIElement) -> Unit) {
        val selectedValue = element.props["selected"]
        val currentSelection = when (selectedValue) {
            is StateCell -> {
                val v = selectedValue.value
                when (v) {
                    is Number -> v.toInt()
                    is net.sourceforge.kleinlisp.objects.AtomObject -> v.value().toString().toIntOrNull() ?: 0
                    else -> 0
                }
            }
            is Number -> selectedValue.toInt()
            else -> 0
        }

        NavigationBar {
            element.children.forEach { child ->
                val iconName = child.props["icon"]?.toString() ?: "help"
                val label = child.props["label"]?.toString() ?: ""
                val value = (child.props["value"] as? Number)?.toInt() ?: 0
                val onSelectHandler = child.props["on-select"] as? FunctionObject
                val enabled = child.props["enabled"]?.let { ModifierBuilder.isTruthy(it) } ?: true

                val icon = NavItemComponent.getIconByName(iconName)
                val isSelected = currentSelection == value

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        onSelectHandler?.function()?.evaluate(emptyArray())
                    },
                    icon = { Icon(icon, contentDescription = label) },
                    label = { Text(label) },
                    enabled = enabled
                )
            }
        }
    }
}
