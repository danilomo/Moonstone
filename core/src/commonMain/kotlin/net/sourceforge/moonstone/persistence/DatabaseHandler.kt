package net.sourceforge.moonstone.persistence

import net.sourceforge.kleinlisp.LispEnvironment
import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.LispVisitor
import net.sourceforge.moonstone.persistence.db.SchemaRegistry
import net.sourceforge.moonstone.persistence.db.QueryBuilder
import java.io.File

/**
 * Database handler wrapper containing all components needed for ORM operations.
 * This is the object stored in the *db* Scheme variable.
 *
 * Each handler has its own:
 * - DatabaseConnection - the actual database connection
 * - SchemaRegistry - table schema definitions (isolated per handler)
 * - QueryBuilder - SQL query construction
 *
 * This design allows:
 * - Graphical apps to automatically use app.db in the script folder
 * - Non-graphical apps to set their own database instances
 * - Integration tests to parameterize isolated database handlers
 */
class DatabaseHandler(
    val connection: DatabaseConnection,
    val schemaRegistry: SchemaRegistry,
    val queryBuilder: QueryBuilder,
    val environment: LispEnvironment,
    private val dbPath: File
) : LispObject {

    /**
     * Close the database connection and release resources.
     */
    fun close() {
        connection.close()
    }

    /**
     * Check if the database connection is open.
     */
    fun isOpen(): Boolean = connection.isOpen()

    /**
     * Get the absolute path to the database file.
     */
    fun getPath(): String = dbPath.absolutePath

    /**
     * Get the database filename.
     */
    fun getFilename(): String = dbPath.name

    // ========== LispObject Implementation ==========

    override fun asObject(): Any = this

    override fun truthiness(): Boolean = isOpen()

    override fun <T> accept(visitor: LispVisitor<T>): T? = null

    override fun error(): Boolean = false

    override fun toString(): String = "#<database-handler ${dbPath.name}>"

    // ========== Convenience Methods ==========

    /**
     * Create an atom in the associated Lisp environment.
     */
    fun atomOf(name: String) = environment.atomOf(name)
}
