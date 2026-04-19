package net.sourceforge.moonstone.android.service

import net.sourceforge.moonstone.android.model.AppConfig
import java.io.File

/**
 * Simple INI file parser for app.conf files.
 *
 * Supports basic INI format with sections and key-value pairs:
 * ```
 * [section]
 * key = value
 * another_key = another value
 * ```
 */
object IniConfigParser {
    /**
     * Parse an app.conf file into an AppConfig.
     *
     * @param file The app.conf file to parse
     * @return Parsed AppConfig, or AppConfig.EMPTY if file doesn't exist or is invalid
     */
    fun parseAppConfig(file: File): AppConfig {
        if (!file.exists() || !file.isFile) {
            return AppConfig.EMPTY
        }

        return try {
            val properties = parseIniFile(file)
            val appSection = properties["app"] ?: return AppConfig.EMPTY

            AppConfig(
                name = appSection["name"],
                description = appSection["description"],
                icon = appSection["icon"],
                version = appSection["version"],
                author = appSection["author"],
            )
        } catch (e: Exception) {
            AppConfig.EMPTY
        }
    }

    /**
     * Parse an INI file into a map of sections to key-value pairs.
     *
     * @param file The INI file to parse
     * @return Map of section names to their key-value pairs
     */
    fun parseIniFile(file: File): Map<String, Map<String, String>> {
        val result = mutableMapOf<String, MutableMap<String, String>>()
        var currentSection: String? = null

        file.readLines().forEach { line ->
            val trimmed = line.trim()

            when {
                // Skip empty lines and comments
                trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";") -> {
                    // Do nothing
                }

                // Section header: [section]
                trimmed.startsWith("[") && trimmed.endsWith("]") -> {
                    currentSection = trimmed.substring(1, trimmed.length - 1).trim().lowercase()
                    result.getOrPut(currentSection!!) { mutableMapOf() }
                }

                // Key-value pair: key = value
                trimmed.contains("=") && currentSection != null -> {
                    val separatorIndex = trimmed.indexOf("=")
                    val key = trimmed.substring(0, separatorIndex).trim().lowercase()
                    val value = trimmed.substring(separatorIndex + 1).trim()
                    result[currentSection]?.put(key, value)
                }
            }
        }

        return result
    }
}
