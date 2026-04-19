package net.sourceforge.moonstone.persistence

import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.moonstone.persistence.db.ColumnDefinition

/**
 * Platform-independent database connection interface.
 *
 * All operations are asynchronous and use callbacks with signature:
 * (result: LispObject, error: String?) -> Unit
 *
 * Callbacks are invoked on the main/UI thread.
 */
interface DatabaseConnection {
    /**
     * Open or create the database.
     * Initializes schema based on registered tables.
     */
    fun open()

    /**
     * Close the database and release resources.
     */
    fun close()

    /**
     * Check if the database is currently open.
     */
    fun isOpen(): Boolean

    /**
     * Execute a SELECT query asynchronously.
     *
     * @param sql SQL query string
     * @param args Query arguments
     * @param columnDefs Column definitions for result mapping
     * @param callback Callback receiving (results: ListObject, error: String?)
     */
    fun executeQuery(
        sql: String,
        args: Array<String>,
        columnDefs: List<ColumnDefinition>,
        callback: (LispObject, String?) -> Unit,
    )

    /**
     * Execute an INSERT asynchronously.
     *
     * @param table Table name
     * @param values Map of column names to values
     * @param callback Callback receiving (insertedId: IntObject, error: String?)
     */
    fun executeInsert(
        table: String,
        values: Map<String, Any?>,
        callback: (LispObject, String?) -> Unit,
    )

    /**
     * Execute a batch INSERT asynchronously.
     *
     * @param table Table name
     * @param valuesList List of maps (column names to values)
     * @param callback Callback receiving (insertedIds: ListObject, error: String?)
     */
    fun executeBatchInsert(
        table: String,
        valuesList: List<Map<String, Any?>>,
        callback: (LispObject, String?) -> Unit,
    )

    /**
     * Execute an UPDATE asynchronously.
     *
     * @param table Table name
     * @param values Map of column names to values
     * @param whereClause WHERE clause (without "WHERE" keyword)
     * @param whereArgs Arguments for WHERE clause
     * @param callback Callback receiving (affectedCount: IntObject, error: String?)
     */
    fun executeUpdate(
        table: String,
        values: Map<String, Any?>,
        whereClause: String?,
        whereArgs: Array<String>?,
        callback: (LispObject, String?) -> Unit,
    )

    /**
     * Execute a DELETE asynchronously.
     *
     * @param table Table name
     * @param whereClause WHERE clause (without "WHERE" keyword)
     * @param whereArgs Arguments for WHERE clause
     * @param callback Callback receiving (deletedCount: IntObject, error: String?)
     */
    fun executeDelete(
        table: String,
        whereClause: String?,
        whereArgs: Array<String>?,
        callback: (LispObject, String?) -> Unit,
    )

    /**
     * Execute a COUNT query asynchronously.
     *
     * @param table Table name
     * @param whereClause WHERE clause (without "WHERE" keyword)
     * @param whereArgs Arguments for WHERE clause
     * @param callback Callback receiving (count: IntObject, error: String?)
     */
    fun executeCount(
        table: String,
        whereClause: String?,
        whereArgs: Array<String>?,
        callback: (LispObject, String?) -> Unit,
    )

    /**
     * Execute raw SQL query asynchronously.
     *
     * @param sql SQL query string
     * @param args Query arguments
     * @param callback Callback receiving (results: ListObject, error: String?)
     */
    fun executeRawQuery(
        sql: String,
        args: Array<String>,
        callback: (LispObject, String?) -> Unit,
    )

    /**
     * Execute raw SQL update (INSERT/UPDATE/DELETE) asynchronously.
     *
     * @param sql SQL statement
     * @param args Statement arguments
     * @param callback Callback receiving (result: IntObject, error: String?)
     */
    fun executeRawUpdate(
        sql: String,
        args: Array<String>,
        callback: (LispObject, String?) -> Unit,
    )

    /**
     * Execute a transaction asynchronously.
     * The transaction block is called synchronously within the transaction.
     *
     * @param transactionBlock Function receiving TransactionContext, returns result
     * @param callback Callback receiving (success: BooleanObject, error: String?)
     */
    fun executeTransaction(
        transactionBlock: (TransactionContext) -> LispObject,
        callback: (LispObject, String?) -> Unit,
    )

    /**
     * Execute migration SQL statements.
     *
     * @param version Migration version number
     * @param statements List of SQL statements to execute
     * @param callback Callback receiving (success: BooleanObject, error: String?)
     */
    fun executeMigration(
        version: Int,
        statements: List<String>,
        callback: (LispObject, String?) -> Unit,
    )
}
