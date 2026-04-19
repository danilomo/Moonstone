package net.sourceforge.moonstone.android.model

import java.io.File

/**
 * Represents a discovered KleinLisp application.
 *
 * @param id Unique identifier derived from folder name
 * @param name Display name (from app.conf or folder name)
 * @param folder The app folder containing app.scm
 * @param scriptFile The main script file (app.scm)
 * @param iconPath Optional path to custom icon (icon.png)
 * @param description Optional app description from app.conf
 * @param version Optional app version from app.conf
 * @param author Optional app author from app.conf
 */
data class AppInfo(
    val id: String,
    val name: String,
    val folder: File,
    val scriptFile: File,
    val iconPath: File? = null,
    val description: String? = null,
    val version: String? = null,
    val author: String? = null,
) {
    companion object {
        const val SCRIPT_FILE_NAME = "app.scm"
        const val CONFIG_FILE_NAME = "app.conf"
        const val ICON_FILE_NAME = "icon.png"
    }
}
