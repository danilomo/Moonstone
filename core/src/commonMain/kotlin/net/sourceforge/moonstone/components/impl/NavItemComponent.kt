package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import net.sourceforge.kleinlisp.objects.FunctionObject
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import kotlin.reflect.KClass

/**
 * Navigation item component for use within BottomNavigation.
 * When used standalone, renders as a simple icon with label column.
 *
 * Scheme usage:
 * (nav-item #:icon "home" #:label "Home" #:value 0 #:on-select handler)
 */
class NavItemComponent : AbstractComponent() {
    override val name = "nav-item"
    override val acceptsChildren = false

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "icon" to String::class,
            "label" to String::class,
            "value" to Number::class,
            "on-select" to Any::class,
            "enabled" to Boolean::class,
        )

    override val requiredProps = listOf("icon", "label")

    // This component is typically rendered by BottomNavigationComponent
    // When used standalone, renders as a simple clickable column
    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
        val iconName = element.props["icon"]?.toString() ?: "info"
        val label = element.props["label"]?.toString() ?: ""
        val onSelectHandler = element.props["on-select"] as? FunctionObject
        val enabled = element.props["enabled"]?.let { ModifierBuilder.isTruthy(it) } ?: true

        val icon = getIconByName(iconName)

        Column(
            modifier =
                Modifier
                    .padding(8.dp)
                    .then(
                        if (enabled && onSelectHandler != null) {
                            Modifier.clickable {
                                onSelectHandler.function().evaluate(emptyArray())
                            }
                        } else {
                            Modifier
                        },
                    ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = label)
            Text(label)
        }
    }

    companion object {
        fun getIconByName(name: String): ImageVector =
            when (name.lowercase()) {
                // Navigation
                "menu" -> Icons.Default.Menu
                "home" -> Icons.Default.Home
                "back", "arrow-back" -> Icons.AutoMirrored.Filled.ArrowBack
                "forward", "arrow-forward" -> Icons.AutoMirrored.Filled.ArrowForward
                "close" -> Icons.Default.Close
                "more", "more-vert" -> Icons.Default.MoreVert

                // Actions
                "add" -> Icons.Default.Add
                "remove" -> Icons.Default.Clear
                "delete" -> Icons.Default.Delete
                "edit" -> Icons.Default.Edit
                "search" -> Icons.Default.Search
                "settings" -> Icons.Default.Settings
                "refresh" -> Icons.Default.Refresh
                "share" -> Icons.Default.Share
                "send" -> Icons.AutoMirrored.Filled.Send
                "save" -> Icons.Default.Done

                // Communication
                "email", "mail" -> Icons.Default.Email
                "phone", "call" -> Icons.Default.Phone
                "message", "chat" -> Icons.Default.Email
                "notifications" -> Icons.Default.Notifications

                // Content
                "favorite" -> Icons.Default.Favorite
                "star" -> Icons.Default.Star
                "check" -> Icons.Default.Check
                "clear" -> Icons.Default.Clear
                "info" -> Icons.Default.Info
                "warning" -> Icons.Default.Warning

                // Social
                "person" -> Icons.Default.Person
                "people", "group" -> Icons.Default.Person
                "account", "account-circle" -> Icons.Default.AccountCircle

                // Places
                "location", "place" -> Icons.Default.LocationOn

                // Misc
                "lock" -> Icons.Default.Lock
                "shopping-cart", "cart" -> Icons.Default.ShoppingCart
                "done" -> Icons.Default.Done

                else -> Icons.Default.Info
            }
    }
}
