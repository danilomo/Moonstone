package net.sourceforge.moonstone.android.widget

import android.content.Context
import android.content.SharedPreferences

/**
 * Repository for persisting widget configurations.
 *
 * Each widget instance has a unique ID and maps to a specific KleinLisp app.
 * Uses SharedPreferences for storage.
 */
class WidgetRepository(
    context: Context,
) {
    companion object {
        private const val PREFS_NAME = "kleinlisp_widget_prefs"

        private fun keyAppFolder(widgetId: Int) = "widget_${widgetId}_app_folder"

        private fun keyAppName(widgetId: Int) = "widget_${widgetId}_app_name"

        private fun keyIconPath(widgetId: Int) = "widget_${widgetId}_icon_path"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Save widget configuration.
     *
     * @param config The widget configuration to save
     */
    fun saveWidgetConfig(config: WidgetConfig) {
        prefs.edit().apply {
            putString(keyAppFolder(config.widgetId), config.appFolder)
            putString(keyAppName(config.widgetId), config.appName)
            if (config.iconPath != null) {
                putString(keyIconPath(config.widgetId), config.iconPath)
            } else {
                remove(keyIconPath(config.widgetId))
            }
            apply()
        }
    }

    /**
     * Load widget configuration by widget ID.
     *
     * @param widgetId The widget ID to look up
     * @return WidgetConfig if found, null otherwise
     */
    fun loadWidgetConfig(widgetId: Int): WidgetConfig? {
        val appFolder = prefs.getString(keyAppFolder(widgetId), null) ?: return null
        val appName = prefs.getString(keyAppName(widgetId), null) ?: return null
        val iconPath = prefs.getString(keyIconPath(widgetId), null)

        return WidgetConfig(
            widgetId = widgetId,
            appFolder = appFolder,
            appName = appName,
            iconPath = iconPath,
        )
    }

    /**
     * Delete widget configuration.
     *
     * @param widgetId The widget ID to delete
     */
    fun deleteWidgetConfig(widgetId: Int) {
        prefs.edit().apply {
            remove(keyAppFolder(widgetId))
            remove(keyAppName(widgetId))
            remove(keyIconPath(widgetId))
            apply()
        }
    }

    /**
     * Check if a widget configuration exists.
     *
     * @param widgetId The widget ID to check
     * @return true if configuration exists
     */
    fun hasWidgetConfig(widgetId: Int): Boolean = prefs.contains(keyAppFolder(widgetId))
}
