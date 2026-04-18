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
    val version: String? = null,
    val author: String? = null
) {
    companion object {
        /**
         * Default empty config when no app.conf exists.
         */
        val EMPTY = AppConfig()
    }
}
