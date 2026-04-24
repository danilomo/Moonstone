package net.sourceforge.moonstone.desktop.config

import net.sourceforge.kleinlisp.LispEnvironment
import java.io.File

/**
 * Utilities for reading database configuration from the Lisp environment.
 */
object DatabaseConfigReader {
    /**
     * Determine the database path.
     * Checks for *db-location* variable in the environment first,
     * otherwise defaults to app.db in the script's folder.
     *
     * Relative paths (e.g., "../shared.db") are resolved relative to the script's directory.
     */
    fun readDbLocation(
        env: LispEnvironment,
        scriptPath: String?,
    ): File {
        // Check for *db-location* override
        try {
            val atom = env.atomOf("*db-location*")
            val value = env.lookupValueOrNull(atom)
            if (value != null) {
                val pathStr = value.asString()?.value()
                if (pathStr != null && pathStr.isNotEmpty()) {
                    val dbFile = File(pathStr)
                    // If relative path, resolve from script directory
                    val resolved =
                        if (dbFile.isAbsolute) {
                            dbFile
                        } else {
                            val appFolder = determineAppFolder(scriptPath)
                            File(appFolder, pathStr)
                        }
                    // Canonicalize to resolve ".." and "." segments
                    return resolved.canonicalFile
                }
            }
        } catch (_: Exception) {
            // Variable not defined or invalid, use default
        }

        // Default to app.db in script's folder
        val appFolder = determineAppFolder(scriptPath)
        return File(appFolder, "app.db")
    }

    /**
     * Determine the app folder based on the script path.
     * Uses the script's directory, or ~/.kleinlisp if no script is provided.
     */
    fun determineAppFolder(scriptPath: String?): File =
        if (scriptPath != null) {
            File(scriptPath).parentFile ?: File(System.getProperty("user.home"), ".kleinlisp")
        } else {
            File(System.getProperty("user.home"), ".kleinlisp")
        }
}
