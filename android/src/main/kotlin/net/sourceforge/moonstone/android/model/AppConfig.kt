package net.sourceforge.moonstone.android.model

/**
 * Parsed configuration from app.conf file.
 *
 * The app.conf file uses INI format:
 * ```
 * [app]
 * name = My App Name
 * description = A description of the app
 * icon = icon.png
 * version = 1.0
 * author = Author Name
 * ```
 */
data class AppConfig(
    val name: String? = null,
    val description: String? = null,
    val icon: String? = null,
    val emoji: String? = null,
    val version: String? = null,
    val author: String? = null,
    val type: String? = null,
) {
    val isFolder: Boolean get() = type?.lowercase() == "folder"

    companion object {
        val EMPTY = AppConfig()
    }
}
