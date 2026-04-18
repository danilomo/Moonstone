package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
import net.sourceforge.kleinlisp.objects.FunctionObject
import kotlin.reflect.KClass

/**
 * Icon component for displaying Material Design icons.
 *
 * Scheme usage:
 * (icon #:name "home" #:size 24 #:tint "blue")
 * (icon #:name "menu" #:on-click (lambda () ...))
 */
class IconComponent : AbstractComponent() {
    override val name = "icon"
    override val acceptsChildren = false

    override val propTypes: Map<String, KClass<*>> = mapOf(
        "name" to String::class,
        "size" to Number::class,
        "tint" to String::class,
        "on-click" to Any::class,
        "content-description" to String::class
    )

    override val requiredProps = listOf("name")

    @Composable
    override fun Render(element: UIElement, renderChild: @Composable (UIElement) -> Unit) {
        val iconName = element.props["name"]?.toString() ?: "help"
        val size = (element.props["size"] as? Number)?.toInt()
        val tint = element.props["tint"]?.let { ModifierBuilder.parseColor(it) }
        val onClickHandler = element.props["on-click"] as? FunctionObject
        val contentDescription = element.props["content-description"]?.toString()

        val icon = getIconByName(iconName)

        var modifier: Modifier = Modifier
        if (size != null) {
            modifier = modifier.size(size.dp)
        }
        if (onClickHandler != null) {
            modifier = modifier.clickable {
                onClickHandler.function().evaluate(emptyArray())
            }
        }

        if (tint != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = modifier,
                tint = tint
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = modifier
            )
        }
    }

    private fun getIconByName(name: String): ImageVector {
        return when (name.lowercase()) {
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

            // Media
            "play", "play-arrow" -> Icons.Default.PlayArrow

            // Social
            "person" -> Icons.Default.Person
            "people", "group" -> Icons.Default.Person
            "account", "account-circle" -> Icons.Default.AccountCircle

            // Places
            "location", "place" -> Icons.Default.LocationOn

            // Misc
            "lock" -> Icons.Default.Lock
            "thumb-up" -> Icons.Default.ThumbUp
            "thumb-down" -> Icons.Default.ThumbUp
            "shopping-cart", "cart" -> Icons.Default.ShoppingCart
            "done" -> Icons.Default.Done
            "keyboard-arrow-down" -> Icons.Default.KeyboardArrowDown
            "keyboard-arrow-up" -> Icons.Default.KeyboardArrowUp
            "keyboard-arrow-left" -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
            "keyboard-arrow-right" -> Icons.AutoMirrored.Filled.KeyboardArrowRight

            else -> Icons.Default.Info
        }
    }
}
