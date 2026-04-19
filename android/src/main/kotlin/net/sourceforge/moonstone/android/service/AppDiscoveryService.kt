package net.sourceforge.moonstone.android.service

import net.sourceforge.moonstone.android.model.AppConfig
import net.sourceforge.moonstone.android.model.AppInfo
import java.io.File

/**
 * Service for discovering KleinLisp applications in a folder.
 *
 * An app is considered valid if it:
 * 1. Is a directory
 * 2. Contains an app.scm file
 *
 * Optional features:
 * - app.conf for metadata (name, description, icon, version, author)
 * - icon.png for custom app icon
 */
object AppDiscoveryService {
    /**
     * Discover all valid KleinLisp apps in the given root folder.
     *
     * @param rootFolder The folder to scan for apps
     * @return List of discovered apps, sorted alphabetically by name
     */
    fun discoverApps(rootFolder: File): List<AppInfo> {
        if (!rootFolder.exists() || !rootFolder.isDirectory) {
            return emptyList()
        }

        return rootFolder
            .listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { folder -> createAppInfo(folder) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    /**
     * Create an AppInfo from a folder if it contains a valid app.
     *
     * @param folder The folder to check
     * @return AppInfo if valid, null otherwise
     */
    private fun createAppInfo(folder: File): AppInfo? {
        val scriptFile = File(folder, AppInfo.SCRIPT_FILE_NAME)
        if (!scriptFile.exists() || !scriptFile.isFile) {
            return null
        }

        val configFile = File(folder, AppInfo.CONFIG_FILE_NAME)
        val config = IniConfigParser.parseAppConfig(configFile)

        val iconFile = resolveIconFile(folder, config)

        return AppInfo(
            id = folder.name,
            name = config.name ?: formatFolderName(folder.name),
            folder = folder,
            scriptFile = scriptFile,
            iconPath = iconFile,
            description = config.description,
            version = config.version,
            author = config.author,
        )
    }

    /**
     * Resolve the icon file for an app.
     * Priority:
     * 1. Icon specified in app.conf
     * 2. Default icon.png
     */
    private fun resolveIconFile(
        folder: File,
        config: AppConfig,
    ): File? {
        // Check config-specified icon
        if (config.icon != null) {
            val configIcon = File(folder, config.icon)
            if (configIcon.exists() && configIcon.isFile) {
                return configIcon
            }
        }

        // Check default icon.png
        val defaultIcon = File(folder, AppInfo.ICON_FILE_NAME)
        if (defaultIcon.exists() && defaultIcon.isFile) {
            return defaultIcon
        }

        return null
    }

    /**
     * Format a folder name into a display name.
     * Converts "my-app-name" or "my_app_name" to "My App Name"
     */
    private fun formatFolderName(name: String): String =
        name
            .replace("-", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }

    /**
     * Check if a folder contains a valid KleinLisp app.
     */
    fun isValidApp(folder: File): Boolean {
        if (!folder.exists() || !folder.isDirectory) {
            return false
        }
        val scriptFile = File(folder, AppInfo.SCRIPT_FILE_NAME)
        return scriptFile.exists() && scriptFile.isFile
    }
}
