package net.sourceforge.moonstone.android.service

import android.content.Context
import android.content.SharedPreferences
import net.sourceforge.moonstone.android.model.LauncherSettings

/**
 * Repository for persisting and retrieving launcher settings.
 *
 * Uses SharedPreferences for storage.
 */
class SettingsRepository(
    private val context: Context,
) {
    companion object {
        private const val PREFS_NAME = "kleinlisp_launcher_settings"

        private const val KEY_APPS_ROOT_PATH = "apps_root_path"
        private const val KEY_GRID_COLUMNS = "grid_columns"
        private const val KEY_SHOW_APP_NAMES = "show_app_names"
        private const val KEY_WEB_IDE_ENABLED = "web_ide_enabled"
        private const val KEY_WEB_IDE_PORT = "web_ide_port"
        private const val KEY_REPL_SERVER_ENABLED = "repl_server_enabled"
        private const val KEY_REPL_SERVER_PORT = "repl_server_port"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Get the default apps path for this device.
     */
    fun getDefaultAppsPath(): String = LauncherSettings.getDefaultAppsPath(context)

    /**
     * Load settings from SharedPreferences.
     *
     * @return Current settings, or defaults if not set
     */
    fun loadSettings(): LauncherSettings =
        LauncherSettings(
            appsRootPath =
                prefs.getString(KEY_APPS_ROOT_PATH, null)
                    ?: getDefaultAppsPath(),
            gridColumns = prefs.getInt(KEY_GRID_COLUMNS, LauncherSettings.DEFAULT_GRID_COLUMNS),
            showAppNames = prefs.getBoolean(KEY_SHOW_APP_NAMES, LauncherSettings.DEFAULT_SHOW_APP_NAMES),
            webIdeEnabled = prefs.getBoolean(KEY_WEB_IDE_ENABLED, LauncherSettings.DEFAULT_WEB_IDE_ENABLED),
            webIdePort = prefs.getInt(KEY_WEB_IDE_PORT, LauncherSettings.DEFAULT_WEB_IDE_PORT),
            replServerEnabled = prefs.getBoolean(KEY_REPL_SERVER_ENABLED, LauncherSettings.DEFAULT_REPL_SERVER_ENABLED),
            replServerPort = prefs.getInt(KEY_REPL_SERVER_PORT, LauncherSettings.DEFAULT_REPL_SERVER_PORT),
        )

    /**
     * Save settings to SharedPreferences.
     *
     * @param settings The settings to save
     */
    fun saveSettings(settings: LauncherSettings) {
        prefs.edit().apply {
            putString(KEY_APPS_ROOT_PATH, settings.appsRootPath)
            putInt(KEY_GRID_COLUMNS, settings.gridColumns)
            putBoolean(KEY_SHOW_APP_NAMES, settings.showAppNames)
            putBoolean(KEY_WEB_IDE_ENABLED, settings.webIdeEnabled)
            putInt(KEY_WEB_IDE_PORT, settings.webIdePort)
            putBoolean(KEY_REPL_SERVER_ENABLED, settings.replServerEnabled)
            putInt(KEY_REPL_SERVER_PORT, settings.replServerPort)
            apply()
        }
    }

    /**
     * Update just the apps root path.
     */
    fun setAppsRootPath(path: String) {
        prefs.edit().putString(KEY_APPS_ROOT_PATH, path).apply()
    }

    /**
     * Update just the grid columns.
     */
    fun setGridColumns(columns: Int) {
        val clamped =
            columns.coerceIn(
                LauncherSettings.MIN_GRID_COLUMNS,
                LauncherSettings.MAX_GRID_COLUMNS,
            )
        prefs.edit().putInt(KEY_GRID_COLUMNS, clamped).apply()
    }

    /**
     * Update just the show app names setting.
     */
    fun setShowAppNames(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_APP_NAMES, show).apply()
    }

    /**
     * Update just the Web IDE enabled setting.
     */
    fun setWebIdeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WEB_IDE_ENABLED, enabled).apply()
    }

    /**
     * Update just the Web IDE port setting.
     */
    fun setWebIdePort(port: Int) {
        prefs.edit().putInt(KEY_WEB_IDE_PORT, port).apply()
    }

    /**
     * Update just the REPL server enabled setting.
     */
    fun setReplServerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REPL_SERVER_ENABLED, enabled).apply()
    }

    /**
     * Update just the REPL server port setting.
     */
    fun setReplServerPort(port: Int) {
        prefs.edit().putInt(KEY_REPL_SERVER_PORT, port).apply()
    }
}
