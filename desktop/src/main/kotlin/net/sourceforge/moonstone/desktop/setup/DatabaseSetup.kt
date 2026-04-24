package net.sourceforge.moonstone.desktop.setup

import net.sourceforge.moonstone.debug.ReloadableRuntime
import net.sourceforge.moonstone.desktop.config.DatabaseConfigReader
import net.sourceforge.moonstone.persistence.DatabaseExtensions
import net.sourceforge.moonstone.runtime.MoonstoneRuntime

/**
 * Utilities for setting up database extensions.
 */
object DatabaseSetup {
    /**
     * Register database extensions with the runtime.
     * Creates a database handler and sets it as *db*.
     *
     * If `*db-location*` is defined in the environment (e.g., from app.conf),
     * that path is used for the database. Otherwise, defaults to `app.db`
     * in the script's directory.
     */
    fun register(
        runtime: MoonstoneRuntime,
        scriptPath: String,
    ): DatabaseExtensions {
        val env = runtime.environment()
        val databaseExtensions = DatabaseExtensions(env)
        databaseExtensions.register()

        // Check for *db-location* override from config
        val dbPath = DatabaseConfigReader.readDbLocation(env, scriptPath)
        val handler = databaseExtensions.createHandler(dbPath)
        env.set(env.atomOf("*db*"), handler)

        return databaseExtensions
    }

    /**
     * Register database extensions with a reloadable runtime.
     * Creates a database handler and sets it as *db*.
     *
     * If `*db-location*` is defined in the environment (e.g., from app.conf),
     * that path is used for the database. Otherwise, defaults to `app.db`
     * in the script's directory.
     */
    fun register(
        runtime: ReloadableRuntime,
        scriptPath: String,
    ): DatabaseExtensions {
        val env = runtime.baseRuntime.environment()
        val databaseExtensions = DatabaseExtensions(env)
        databaseExtensions.register()

        // Check for *db-location* override from config
        val dbPath = DatabaseConfigReader.readDbLocation(env, scriptPath)
        val handler = databaseExtensions.createHandler(dbPath)
        env.set(env.atomOf("*db*"), handler)

        return databaseExtensions
    }
}
