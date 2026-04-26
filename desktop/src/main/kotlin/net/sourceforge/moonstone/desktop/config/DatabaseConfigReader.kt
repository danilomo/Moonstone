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
        val customPath = readDbLocationFromEnv(env, scriptPath)
        if (customPath != null) {
            return customPath
        }

        // Default to app.db in script's folder
        val appFolder = determineAppFolder(scriptPath)
        return File(appFolder, "app.db")
    }

    /**
     * Attempt to read database location from environment variable.
     */
    private fun readDbLocationFromEnv(
        env: LispEnvironment,
        scriptPath: String?,
    ): File? {
        try {
            val atom = env.atomOf("*db-location*")
            val value = env.lookupValueOrNull(atom) ?: return null
            val pathStr = value.asString()?.value()

            if (pathStr.isNullOrEmpty()) {
                return null
            }

            return resolveDatabasePath(pathStr, scriptPath)
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Resolve database path, handling both absolute and relative paths.
     */
    private fun resolveDatabasePath(
        pathStr: String,
        scriptPath: String?,
    ): File {
        val dbFile = File(pathStr)
        val resolved =
            if (dbFile.isAbsolute) {
                dbFile
            } else {
                val appFolder = determineAppFolder(scriptPath)
                File(appFolder, pathStr)
            }
        return resolved.canonicalFile
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
