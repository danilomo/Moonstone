package net.sourceforge.moonstone.persistence

import net.sourceforge.kleinlisp.LispEnvironment
import net.sourceforge.moonstone.persistence.db.SchemaRegistry
import java.io.File

/**
 * Platform-specific factory for creating database connections.
 *
 * This uses Kotlin's expect/actual mechanism to provide platform-specific
 * implementations while maintaining a common API.
 */
expect class DatabaseFactory() {
    /**
     * Create a platform-specific database connection.
     *
     * @param databasePath Path to the database file
     * @param schemaRegistry Registry containing table definitions
     * @param environment Lisp environment for atom resolution
     * @return Platform-specific DatabaseConnection implementation
     */
    fun createConnection(
        databasePath: File,
        schemaRegistry: SchemaRegistry,
        environment: LispEnvironment,
    ): DatabaseConnection
}
