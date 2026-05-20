package net.sourceforge.moonstone.components.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import net.sourceforge.kleinlisp.objects.FunctionObject
import net.sourceforge.moonstone.components.AbstractComponent
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.render.ModifierBuilder
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

    override val propTypes: Map<String, KClass<*>> =
        mapOf(
            "name" to String::class,
            "size" to Number::class,
            "tint" to String::class,
            "on-click" to Any::class,
            "content-description" to String::class,
        )

    override val requiredProps = listOf("name")

    @Composable
    override fun Render(
        element: UIElement,
        renderChild: @Composable (UIElement) -> Unit,
    ) {
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
            modifier =
                modifier.clickable {
                    onClickHandler.function().evaluate(emptyArray())
                }
        }

        if (tint != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = modifier,
                tint = tint,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = modifier,
            )
        }
    }

    private fun getIconByName(name: String): ImageVector {
        val normalized = name.lowercase()
        return getNavigationIcon(normalized)
            ?: getActionIcon(normalized)
            ?: getCommunicationIcon(normalized)
            ?: getContentIcon(normalized)
            ?: getMediaIcon(normalized)
            ?: getSocialIcon(normalized)
            ?: getPlacesIcon(normalized)
            ?: getMiscIcon(normalized)
            ?: Icons.Default.Info
    }

    private fun getNavigationIcon(name: String): ImageVector? =
        when (name) {
            "menu" -> Icons.Default.Menu
            "home" -> Icons.Default.Home
            "back", "arrow-back" -> Icons.AutoMirrored.Filled.ArrowBack
            "forward", "arrow-forward" -> Icons.AutoMirrored.Filled.ArrowForward
            "close" -> Icons.Default.Close
            "more", "more-vert" -> Icons.Default.MoreVert
            else -> null
        }

    @Suppress("CyclomaticComplexMethod")
    private fun getActionIcon(name: String): ImageVector? =
        when (name) {
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
            else -> null
        }

    private fun getCommunicationIcon(name: String): ImageVector? =
        when (name) {
            "email", "mail" -> Icons.Default.Email
            "phone", "call" -> Icons.Default.Phone
            "message", "chat" -> Icons.Default.Email
            "notifications" -> Icons.Default.Notifications
            else -> null
        }

    private fun getContentIcon(name: String): ImageVector? =
        when (name) {
            "favorite" -> Icons.Default.Favorite
            "star" -> Icons.Default.Star
            "star_border", "star-border", "star_outline", "star-outline" -> Icons.Outlined.Star
            "check" -> Icons.Default.Check
            "clear" -> Icons.Default.Clear
            "info" -> Icons.Default.Info
            "warning" -> Icons.Default.Warning
            else -> null
        }

    private fun getMediaIcon(name: String): ImageVector? =
        when (name) {
            "play", "play-arrow" -> Icons.Default.PlayArrow
            else -> null
        }

    private fun getSocialIcon(name: String): ImageVector? =
        when (name) {
            "person" -> Icons.Default.Person
            "people", "group" -> Icons.Default.Person
            "account", "account-circle" -> Icons.Default.AccountCircle
            else -> null
        }

    private fun getPlacesIcon(name: String): ImageVector? =
        when (name) {
            "location", "place" -> Icons.Default.LocationOn
            else -> null
        }

    private fun getMiscIcon(name: String): ImageVector? =
        when (name) {
            "lock" -> Icons.Default.Lock
            "thumb-up" -> Icons.Default.ThumbUp
            "thumb-down" -> Icons.Default.ThumbUp
            "shopping-cart", "cart" -> Icons.Default.ShoppingCart
            "done" -> Icons.Default.Done
            "keyboard-arrow-down" -> Icons.Default.KeyboardArrowDown
            "keyboard-arrow-up" -> Icons.Default.KeyboardArrowUp
            "keyboard-arrow-left" -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
            "keyboard-arrow-right" -> Icons.AutoMirrored.Filled.KeyboardArrowRight
            else -> null
        }
}
