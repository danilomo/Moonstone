package net.sourceforge.moonstone.desktop.config

import net.sourceforge.kleinlisp.LispEnvironment

/**
 * Window size configuration read from Scheme script.
 * Scripts can define *window-width* and *window-height* to customize the window size.
 * This only affects desktop; Android ignores these values.
 */
data class WindowConfig(
    val width: Int = 800,
    val height: Int = 900,
)

/**
 * Read window configuration from the Lisp environment.
 * Looks for optional *window-width* and *window-height* variables.
 */
fun readWindowConfig(env: LispEnvironment): WindowConfig {
    val defaultWidth = 800
    val defaultHeight = 900

    val width = readIntVariable(env, "*window-width*") ?: defaultWidth
    val height = readIntVariable(env, "*window-height*") ?: defaultHeight

    return WindowConfig(width, height)
}

/**
 * Read an optional integer variable from the Lisp environment.
 * Returns null if the variable is not defined or is not a number.
 */
private fun readIntVariable(
    env: LispEnvironment,
    name: String,
): Int? {
    return try {
        val atom = env.atomOf(name)
        val value = env.lookupValueOrNull(atom) ?: return null

        // Try to get as integer
        val intObj = value.asInt()
        if (intObj != null) {
            return intObj.value().toInt()
        }

        // Try to get as double and convert
        val doubleObj = value.asDouble()
        if (doubleObj != null) {
            return doubleObj.value().toInt()
        }

        null
    } catch (e: Exception) {
        null
    }
}
