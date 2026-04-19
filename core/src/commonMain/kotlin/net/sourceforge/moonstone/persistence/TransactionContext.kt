package net.sourceforge.moonstone.persistence

import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.moonstone.persistence.db.ColumnDefinition

/**
 * Context for synchronous operations within a transaction.
 *
 * All operations are synchronous and return results directly.
 * Use setError() to mark the transaction for rollback.
 */
interface TransactionContext {
    /**
     * Insert a row synchronously.
     *
     * @param tableName Table name
     * @param values Map of column names to values
     * @return Inserted row ID
     */
    fun insert(
        tableName: String,
        values: Map<String, Any?>,
    ): Long

    /**
     * Update rows synchronously.
     *
     * @param tableName Table name
     * @param values Map of column names to values
     * @param whereClause WHERE clause (without "WHERE" keyword)
     * @param whereArgs Arguments for WHERE clause
     * @return Number of affected rows
     */
    fun update(
        tableName: String,
        values: Map<String, Any?>,
        whereClause: String?,
        whereArgs: Array<String>?,
    ): Int

    /**
     * Delete rows synchronously.
     *
     * @param tableName Table name
     * @param whereClause WHERE clause (without "WHERE" keyword)
     * @param whereArgs Arguments for WHERE clause
     * @return Number of deleted rows
     */
    fun delete(
        tableName: String,
        whereClause: String?,
        whereArgs: Array<String>?,
    ): Int

    /**
     * Query rows synchronously.
     *
     * @param sql SQL query string
     * @param args Query arguments
     * @param columnDefs Column definitions for result mapping
     * @return ListObject of PMapObject rows
     */
    fun query(
        sql: String,
        args: Array<String>,
        columnDefs: List<ColumnDefinition>,
    ): LispObject

    /**
     * Query a single row synchronously.
     *
     * @param sql SQL query string
     * @param args Query arguments
     * @param columnDefs Column definitions for result mapping
     * @return PMapObject or BooleanObject.FALSE if not found
     */
    fun querySingle(
        sql: String,
        args: Array<String>,
        columnDefs: List<ColumnDefinition>,
    ): LispObject

    /**
     * Mark the transaction as failed.
     * The transaction will be rolled back.
     *
     * @param message Error message
     */
    fun setError(message: String)

    /**
     * Check if the transaction has an error.
     */
    fun hasError(): Boolean
}
