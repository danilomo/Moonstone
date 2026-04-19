package net.sourceforge.moonstone.android.widget

/**
 * Configuration for a widget instance.
 *
 * @param widgetId The unique widget ID assigned by the system
 * @param appFolder Absolute path to the app folder
 * @param appName Display name of the app
 * @param iconPath Optional path to the app's icon
 */
data class WidgetConfig(
    val widgetId: Int,
    val appFolder: String,
    val appName: String,
    val iconPath: String? = null,
)
