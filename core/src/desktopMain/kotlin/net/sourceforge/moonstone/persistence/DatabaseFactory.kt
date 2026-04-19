package net.sourceforge.moonstone.persistence

import net.sourceforge.kleinlisp.LispEnvironment
import net.sourceforge.moonstone.persistence.db.SchemaRegistry
import java.io.File

/**
 * Desktop implementation of DatabaseFactory.
 * Creates JdbcDatabaseConnection using SQLite JDBC driver.
 */
actual class DatabaseFactory actual constructor() {
    actual fun createConnection(
        databasePath: File,
        schemaRegistry: SchemaRegistry,
        environment: LispEnvironment,
    ): DatabaseConnection = JdbcDatabaseConnection(databasePath, schemaRegistry, environment)
}
