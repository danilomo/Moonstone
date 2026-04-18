package net.sourceforge.moonstone.android.model

import android.content.Context
import java.io.File

/**
 * User preferences for the launcher.
 *
 * @param appsRootPath Root folder path for discovering apps
 * @param gridColumns Number of columns in the app grid (2-6)
 * @param showAppNames Whether to show app names below icons
 * @param webIdeEnabled Whether the Web IDE server is enabled
 * @param webIdePort Port number for the Web IDE server
 * @param replServerEnabled Whether the REPL server is enabled for running apps
 * @param replServerPort Port number for the REPL server
 */
data class LauncherSettings(
    val appsRootPath: String,
    val gridColumns: Int = DEFAULT_GRID_COLUMNS,
    val showAppNames: Boolean = DEFAULT_SHOW_APP_NAMES,
    val webIdeEnabled: Boolean = DEFAULT_WEB_IDE_ENABLED,
    val webIdePort: Int = DEFAULT_WEB_IDE_PORT,
    val replServerEnabled: Boolean = DEFAULT_REPL_SERVER_ENABLED,
    val replServerPort: Int = DEFAULT_REPL_SERVER_PORT
) {
    companion object {
        const val APPS_FOLDER_NAME = "KleinLispApps"

        const val DEFAULT_GRID_COLUMNS = 4
        const val DEFAULT_SHOW_APP_NAMES = true

        const val DEFAULT_WEB_IDE_ENABLED = false
        const val DEFAULT_WEB_IDE_PORT = 8080

        const val DEFAULT_REPL_SERVER_ENABLED = false
        const val DEFAULT_REPL_SERVER_PORT = 37146  // Same as guile --listen default

        const val MIN_GRID_COLUMNS = 2
        const val MAX_GRID_COLUMNS = 6

        /**
         * Get the default apps path using app-specific external storage.
         * This path doesn't require any storage permissions.
         *
         * Path: /storage/emulated/0/Android/data/net.sourceforge.moonstone.android/files/KleinLispApps
         */
        fun getDefaultAppsPath(context: Context): String {
            val externalFilesDir = context.getExternalFilesDir(null)
            return File(externalFilesDir, APPS_FOLDER_NAME).absolutePath
        }
    }

    /**
     * Returns the apps root folder as a File.
     */
    fun getAppsRootFolder(): File = File(appsRootPath)
}
