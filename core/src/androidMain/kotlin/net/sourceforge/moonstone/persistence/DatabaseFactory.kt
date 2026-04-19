package net.sourceforge.moonstone.persistence

import net.sourceforge.kleinlisp.LispEnvironment
import net.sourceforge.moonstone.persistence.db.SchemaRegistry
import java.io.File

/**
 * Android-specific implementation of DatabaseFactory.
 *
 * Creates AndroidDatabaseConnection instances that wrap Android's SQLiteDatabase.
 */
actual class DatabaseFactory actual constructor() {
    /**
     * Create an Android-specific database connection.
     *
     * @param databasePath Path to the database file
     * @param schemaRegistry Registry containing table definitions
     * @param environment Lisp environment for atom resolution
     * @return AndroidDatabaseConnection implementation
     */
    actual fun createConnection(
        databasePath: File,
        schemaRegistry: SchemaRegistry,
        environment: LispEnvironment,
    ): DatabaseConnection = AndroidDatabaseConnection(databasePath, schemaRegistry, environment)
}
