package net.sourceforge.moonstone.config

import net.sourceforge.kleinlisp.LispEnvironment
import net.sourceforge.kleinlisp.objects.BooleanObject
import net.sourceforge.kleinlisp.objects.DoubleObject
import net.sourceforge.kleinlisp.objects.IntObject
import net.sourceforge.kleinlisp.objects.StringObject
import java.io.File

/**
 * Loads configuration from app.conf files into the Lisp environment.
 *
 * Configuration values are loaded as Lisp variables with the naming convention `*key-name*`.
 * For example, `window-height = 1000` becomes `(define *window-height* 1000)`.
 *
 * Supported value types:
 * - Integers: `123`, `-456`
 * - Floating point: `1.5`, `-3.14`
 * - Booleans: `true`, `false`, `#t`, `#f`
 * - Strings: everything else (no quotes needed)
 *
 * Example app.conf:
 * ```
 * [app]
 * name = My App
 * description = A sample application
 * window-width = 800
 * window-height = 600
 * db-location = /shared/mydb.db
 * ```
 */
object ConfigLoader {

    /**
     * Load configuration from an app.conf file into the Lisp environment.
     *
     * All key-value pairs from the [app] section are loaded as Lisp variables
     * with the `*name*` naming convention.
     *
     * @param configFile The app.conf file to load
     * @param env The Lisp environment to load variables into
     * @return ParsedConfig containing the raw parsed values for metadata use
     */
    fun loadConfig(configFile: File, env: LispEnvironment): ParsedConfig {
        if (!configFile.exists() || !configFile.isFile) {
            return ParsedConfig.EMPTY
        }

        return try {
            val properties = parseIniFile(configFile)
            val appSection = properties["app"] ?: return ParsedConfig.EMPTY

            // Load all key-value pairs into the environment as *key*
            appSection.forEach { (key, value) ->
                val varName = "*${key}*"
                val atom = env.atomOf(varName)
                val lispValue = parseValue(value)
                env.set(atom, lispValue)
            }

            ParsedConfig(appSection)
        } catch (e: Exception) {
            System.err.println("Warning: Failed to parse app.conf: ${e.message}")
            ParsedConfig.EMPTY
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
                    val key = trimmed.substring(0, separatorIndex).trim()
                    val value = trimmed.substring(separatorIndex + 1).trim()
                    result[currentSection]?.put(key, value)
                }
            }
        }

        return result
    }

    /**
     * Parse a string value into the appropriate Lisp object type.
     * Attempts to parse as: integer, double, boolean, then falls back to string.
     */
    private fun parseValue(value: String): net.sourceforge.kleinlisp.LispObject {
        // Try integer
        value.toIntOrNull()?.let {
            return IntObject(it)
        }

        // Try double
        value.toDoubleOrNull()?.let {
            return DoubleObject(it)
        }

        // Try boolean
        when (value.lowercase()) {
            "true", "#t" -> return BooleanObject.TRUE
            "false", "#f" -> return BooleanObject.FALSE
        }

        // Default to string
        return StringObject(value)
    }
}

/**
 * Parsed configuration data.
 * Contains raw key-value pairs for metadata extraction.
 */
data class ParsedConfig(
    val values: Map<String, String>
) {
    companion object {
        val EMPTY = ParsedConfig(emptyMap())
    }

    /**
     * Get a string value from the config.
     */
    fun getString(key: String): String? = values[key]

    /**
     * Get an integer value from the config.
     */
    fun getInt(key: String): Int? = values[key]?.toIntOrNull()

    /**
     * Check if the config has a value for a key.
     */
    fun hasKey(key: String): Boolean = values.containsKey(key)
}
