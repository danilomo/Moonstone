package net.sourceforge.moonstone.persistence

import net.sourceforge.kleinlisp.LispEnvironment
import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.objects.*
import net.sourceforge.moonstone.persistence.db.*
import java.io.File
import java.sql.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import javax.swing.SwingUtilities

/**
 * Desktop (JVM) implementation of DatabaseConnection using JDBC with SQLite.
 *
 * All async operations use CompletableFuture, executing on a thread pool
 * and invoking callbacks on the Swing EDT (main thread).
 */
class JdbcDatabaseConnection(
    private val databasePath: File,
    private val schemaRegistry: SchemaRegistry,
    private val environment: LispEnvironment
) : DatabaseConnection {

    companion object {
        private const val SCHEMA_TABLE = "_klein_schema"
    }

    private var connection: Connection? = null
    private val executor = Executors.newFixedThreadPool(4)
    private var isInitialized = false
    private val resultMapper = CommonResultMapper()

    // Cache the 'null atom for SQL NULL representation
    private val nullAtom: AtomObject by lazy { environment.atomOf("null") }

    @Synchronized
    override fun open() {
        if (connection?.isClosed == false) {
            return
        }

        // Ensure parent folder exists
        databasePath.parentFile?.mkdirs()

        // Open or create database
        val url = "jdbc:sqlite:${databasePath.absolutePath}"
        connection = DriverManager.getConnection(url)

        // Enable foreign keys
        connection!!.createStatement().execute("PRAGMA foreign_keys = ON")

        // Initialize schema if needed
        if (!isInitialized) {
            initializeSchema()
            isInitialized = true
        }
    }

    @Synchronized
    override fun close() {
        executor.shutdown()
        connection?.close()
        connection = null
        isInitialized = false
    }

    override fun isOpen(): Boolean = connection?.isClosed == false

    override fun executeQuery(
        sql: String,
        args: Array<String>,
        columnDefs: List<ColumnDefinition>,
        callback: (LispObject, String?) -> Unit
    ) {
        CompletableFuture.supplyAsync({
            try {
                val conn = getOpenConnection()
                val stmt = conn.prepareStatement(sql)
                setStatementValues(stmt, args.toList())

                val rs = stmt.executeQuery()
                val results = resultSetToListObject(rs, columnDefs)
                rs.close()
                stmt.close()

                Pair(results, null as String?)
            } catch (e: Exception) {
                println("[JdbcDatabaseConnection] Query error: ${e.message}")
                Pair(ListObject.NIL, categorizeError(e))
            }
        }, executor).thenAccept { (result, error) ->
            runOnMainThread { callback(result, error) }
        }
    }

    override fun executeInsert(
        table: String,
        values: Map<String, Any?>,
        callback: (LispObject, String?) -> Unit
    ) {
        CompletableFuture.supplyAsync({
            try {
                val conn = getOpenConnection()

                // Build INSERT statement
                val columns = values.keys.joinToString(", ")
                val placeholders = values.keys.joinToString(", ") { "?" }
                val sql = "INSERT INTO $table ($columns) VALUES ($placeholders)"

                val stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                setStatementValues(stmt, values.values.toList())

                stmt.executeUpdate()

                // Get generated ID
                val rs = stmt.generatedKeys
                val id = if (rs.next()) rs.getLong(1).toInt() else -1
                rs.close()
                stmt.close()

                Pair(IntObject(id), null as String?)
            } catch (e: Exception) {
                println("[JdbcDatabaseConnection] Insert error: ${e.message}")
                Pair(IntObject(-1), categorizeError(e))
            }
        }, executor).thenAccept { (result, error) ->
            runOnMainThread { callback(result, error) }
        }
    }

    override fun executeBatchInsert(
        table: String,
        valuesList: List<Map<String, Any?>>,
        callback: (LispObject, String?) -> Unit
    ) {
        CompletableFuture.supplyAsync({
            try {
                val conn = getOpenConnection()
                conn.autoCommit = false

                val ids = mutableListOf<LispObject>()

                try {
                    for (values in valuesList) {
                        val columns = values.keys.joinToString(", ")
                        val placeholders = values.keys.joinToString(", ") { "?" }
                        val sql = "INSERT INTO $table ($columns) VALUES ($placeholders)"

                        val stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                        setStatementValues(stmt, values.values.toList())
                        stmt.executeUpdate()

                        val rs = stmt.generatedKeys
                        val id = if (rs.next()) rs.getLong(1).toInt() else -1
                        rs.close()
                        stmt.close()

                        ids.add(IntObject(id))
                    }

                    conn.commit()
                    conn.autoCommit = true

                    Pair(ListObject.fromList(ids), null as String?)
                } catch (e: Exception) {
                    conn.rollback()
                    conn.autoCommit = true
                    throw e
                }
            } catch (e: Exception) {
                println("[JdbcDatabaseConnection] Batch insert error: ${e.message}")
                Pair(ListObject.NIL, categorizeError(e))
            }
        }, executor).thenAccept { (result, error) ->
            runOnMainThread { callback(result, error) }
        }
    }

    override fun executeUpdate(
        table: String,
        values: Map<String, Any?>,
        whereClause: String?,
        whereArgs: Array<String>?,
        callback: (LispObject, String?) -> Unit
    ) {
        CompletableFuture.supplyAsync({
            try {
                val conn = getOpenConnection()

                // Build UPDATE statement
                val setClauses = values.keys.joinToString(", ") { "$it = ?" }
                val sql = if (whereClause != null) {
                    "UPDATE $table SET $setClauses WHERE $whereClause"
                } else {
                    "UPDATE $table SET $setClauses"
                }

                val stmt = conn.prepareStatement(sql)
                val allArgs = values.values.toList() + (whereArgs?.toList() ?: emptyList())
                setStatementValues(stmt, allArgs)

                val count = stmt.executeUpdate()
                stmt.close()

                Pair(IntObject(count), null as String?)
            } catch (e: Exception) {
                println("[JdbcDatabaseConnection] Update error: ${e.message}")
                Pair(IntObject(0), categorizeError(e))
            }
        }, executor).thenAccept { (result, error) ->
            runOnMainThread { callback(result, error) }
        }
    }

    override fun executeDelete(
        table: String,
        whereClause: String?,
        whereArgs: Array<String>?,
        callback: (LispObject, String?) -> Unit
    ) {
        CompletableFuture.supplyAsync({
            try {
                val conn = getOpenConnection()

                val sql = if (whereClause != null) {
                    "DELETE FROM $table WHERE $whereClause"
                } else {
                    "DELETE FROM $table"
                }

                val stmt = conn.prepareStatement(sql)
                if (whereArgs != null) {
                    setStatementValues(stmt, whereArgs.toList())
                }

                val count = stmt.executeUpdate()
                stmt.close()

                Pair(IntObject(count), null as String?)
            } catch (e: Exception) {
                println("[JdbcDatabaseConnection] Delete error: ${e.message}")
                Pair(IntObject(0), categorizeError(e))
            }
        }, executor).thenAccept { (result, error) ->
            runOnMainThread { callback(result, error) }
        }
    }

    override fun executeCount(
        table: String,
        whereClause: String?,
        whereArgs: Array<String>?,
        callback: (LispObject, String?) -> Unit
    ) {
        println("[JdbcDatabaseConnection] executeCount called: table=$table, whereClause=$whereClause")
        CompletableFuture.supplyAsync({
            try {
                val conn = getOpenConnection()

                val sql = if (whereClause != null) {
                    "SELECT COUNT(*) FROM $table WHERE $whereClause"
                } else {
                    "SELECT COUNT(*) FROM $table"
                }

                println("[JdbcDatabaseConnection] executeCount SQL: $sql")
                val stmt = conn.prepareStatement(sql)
                if (whereArgs != null) {
                    setStatementValues(stmt, whereArgs.toList())
                }

                val rs = stmt.executeQuery()
                val count = if (rs.next()) rs.getLong(1).toInt() else 0
                rs.close()
                stmt.close()

                println("[JdbcDatabaseConnection] executeCount result: $count")
                Pair(IntObject(count), null as String?)
            } catch (e: Exception) {
                println("[JdbcDatabaseConnection] Count error: ${e.message}")
                Pair(IntObject(0), categorizeError(e))
            }
        }, executor).thenAccept { (result, error) ->
            runOnMainThread { callback(result, error) }
        }
    }

    override fun executeRawQuery(
        sql: String,
        args: Array<String>,
        callback: (LispObject, String?) -> Unit
    ) {
        CompletableFuture.supplyAsync({
            try {
                val conn = getOpenConnection()
                val stmt = conn.prepareStatement(sql)
                setStatementValues(stmt, args.toList())

                val rs = stmt.executeQuery()

                // Build column definitions from ResultSet metadata
                val meta = rs.metaData
                val columnDefs = (1..meta.columnCount).map { i ->
                    ColumnDefinition(
                        name = meta.getColumnName(i),
                        type = ColumnType.STRING // Default, actual type determined at runtime
                    )
                }

                val results = resultSetToListObject(rs, columnDefs)
                rs.close()
                stmt.close()

                Pair(results, null as String?)
            } catch (e: Exception) {
                println("[JdbcDatabaseConnection] Raw query error: ${e.message}")
                Pair(ListObject.NIL, "SQL error: ${e.message}")
            }
        }, executor).thenAccept { (result, error) ->
            runOnMainThread { callback(result, error) }
        }
    }

    override fun executeRawUpdate(
        sql: String,
        args: Array<String>,
        callback: (LispObject, String?) -> Unit
    ) {
        CompletableFuture.supplyAsync({
            try {
                val conn = getOpenConnection()
                val stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                setStatementValues(stmt, args.toList())

                val result = if (sql.trim().uppercase().startsWith("INSERT")) {
                    stmt.executeUpdate()
                    val rs = stmt.generatedKeys
                    val id = if (rs.next()) rs.getLong(1).toInt() else 0
                    rs.close()
                    id
                } else {
                    stmt.executeUpdate()
                }

                stmt.close()

                Pair(IntObject(result), null as String?)
            } catch (e: Exception) {
                println("[JdbcDatabaseConnection] Raw update error: ${e.message}")
                Pair(IntObject(0), "SQL error: ${e.message}")
            }
        }, executor).thenAccept { (result, error) ->
            runOnMainThread { callback(result, error) }
        }
    }

    override fun executeTransaction(
        transactionBlock: (TransactionContext) -> LispObject,
        callback: (LispObject, String?) -> Unit
    ) {
        CompletableFuture.supplyAsync({
            try {
                val conn = getOpenConnection()
                var result: LispObject = BooleanObject.TRUE
                var error: String? = null

                conn.autoCommit = false
                try {
                    val txContext = JdbcTransactionContext(conn)
                    result = transactionBlock(txContext)

                    if (result.truthiness() && !txContext.hasError()) {
                        conn.commit()
                    } else if (txContext.hasError()) {
                        error = txContext.getErrorMessage()
                        conn.rollback()
                    }
                } catch (e: Exception) {
                    error = categorizeError(e)
                    conn.rollback()
                } finally {
                    conn.autoCommit = true
                }

                Pair(if (error == null) BooleanObject.TRUE else BooleanObject.FALSE, error)
            } catch (e: Exception) {
                println("[JdbcDatabaseConnection] Transaction error: ${e.message}")
                Pair(BooleanObject.FALSE, categorizeError(e))
            }
        }, executor).thenAccept { (result, error) ->
            runOnMainThread { callback(result, error) }
        }
    }

    override fun executeMigration(
        version: Int,
        statements: List<String>,
        callback: (LispObject, String?) -> Unit
    ) {
        CompletableFuture.supplyAsync({
            try {
                val conn = getOpenConnection()

                conn.autoCommit = false
                try {
                    for (sql in statements) {
                        conn.createStatement().execute(sql)
                    }

                    // Update migration version in schema table
                    val sql = """
                        INSERT OR REPLACE INTO $SCHEMA_TABLE (table_name, schema_hash, version)
                        VALUES (?, ?, ?)
                    """.trimIndent()
                    val stmt = conn.prepareStatement(sql)
                    stmt.setString(1, "_migration_version")
                    stmt.setString(2, version.toString())
                    stmt.setInt(3, version)
                    stmt.executeUpdate()
                    stmt.close()

                    conn.commit()
                    conn.autoCommit = true

                    Pair(BooleanObject.TRUE, null as String?)
                } catch (e: Exception) {
                    conn.rollback()
                    conn.autoCommit = true
                    Pair(BooleanObject.FALSE, "Migration failed: ${e.message}")
                }
            } catch (e: Exception) {
                println("[JdbcDatabaseConnection] Migration error: ${e.message}")
                Pair(BooleanObject.FALSE, categorizeError(e))
            }
        }, executor).thenAccept { (result, error) ->
            runOnMainThread { callback(result, error) }
        }
    }

    // ========== Inner class for TransactionContext ==========

    private inner class JdbcTransactionContext(
        private val conn: Connection
    ) : TransactionContext {
        private var errorMessage: String? = null

        override fun insert(tableName: String, values: Map<String, Any?>): Long {
            val columns = values.keys.joinToString(", ")
            val placeholders = values.keys.joinToString(", ") { "?" }
            val sql = "INSERT INTO $tableName ($columns) VALUES ($placeholders)"

            val stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
            setStatementValues(stmt, values.values.toList())
            stmt.executeUpdate()

            val rs = stmt.generatedKeys
            val id = if (rs.next()) rs.getLong(1) else -1L
            rs.close()
            stmt.close()

            return id
        }

        override fun update(
            tableName: String,
            values: Map<String, Any?>,
            whereClause: String?,
            whereArgs: Array<String>?
        ): Int {
            val setClauses = values.keys.joinToString(", ") { "$it = ?" }
            val sql = if (whereClause != null) {
                "UPDATE $tableName SET $setClauses WHERE $whereClause"
            } else {
                "UPDATE $tableName SET $setClauses"
            }

            val stmt = conn.prepareStatement(sql)
            val allArgs = values.values.toList() + (whereArgs?.toList() ?: emptyList())
            setStatementValues(stmt, allArgs)

            val count = stmt.executeUpdate()
            stmt.close()

            return count
        }

        override fun delete(
            tableName: String,
            whereClause: String?,
            whereArgs: Array<String>?
        ): Int {
            val sql = if (whereClause != null) {
                "DELETE FROM $tableName WHERE $whereClause"
            } else {
                "DELETE FROM $tableName"
            }

            val stmt = conn.prepareStatement(sql)
            if (whereArgs != null) {
                setStatementValues(stmt, whereArgs.toList())
            }

            val count = stmt.executeUpdate()
            stmt.close()

            return count
        }

        override fun query(
            sql: String,
            args: Array<String>,
            columnDefs: List<ColumnDefinition>
        ): LispObject {
            val stmt = conn.prepareStatement(sql)
            setStatementValues(stmt, args.toList())

            val rs = stmt.executeQuery()
            val result = resultSetToListObject(rs, columnDefs)
            rs.close()
            stmt.close()

            return result
        }

        override fun querySingle(
            sql: String,
            args: Array<String>,
            columnDefs: List<ColumnDefinition>
        ): LispObject {
            val stmt = conn.prepareStatement(sql)
            setStatementValues(stmt, args.toList())

            val rs = stmt.executeQuery()
            val result = if (rs.next()) {
                resultSetRowToPMap(rs, columnDefs)
            } else {
                BooleanObject.FALSE
            }
            rs.close()
            stmt.close()

            return result
        }

        override fun setError(message: String) {
            errorMessage = message
        }

        override fun hasError(): Boolean = errorMessage != null

        fun getErrorMessage(): String? = errorMessage
    }

    // ========== Private Helper Methods ==========

    private fun getOpenConnection(): Connection {
        if (connection?.isClosed != false) {
            open()
        }
        return connection ?: throw IllegalStateException("Database not open")
    }

    private fun runOnMainThread(block: () -> Unit) {
        SwingUtilities.invokeLater(block)
    }

    private fun setStatementValues(stmt: PreparedStatement, values: List<Any?>) {
        for ((index, value) in values.withIndex()) {
            when (value) {
                null -> stmt.setNull(index + 1, Types.NULL)
                is String -> stmt.setString(index + 1, value)
                is Int -> stmt.setInt(index + 1, value)
                is Long -> stmt.setLong(index + 1, value)
                is Double -> stmt.setDouble(index + 1, value)
                is Float -> stmt.setFloat(index + 1, value)
                is Boolean -> stmt.setInt(index + 1, if (value) 1 else 0)
                is ByteArray -> stmt.setBytes(index + 1, value)
                else -> stmt.setString(index + 1, value.toString())
            }
        }
    }

    private fun resultSetToListObject(rs: ResultSet, columnDefs: List<ColumnDefinition>): LispObject {
        val rows = mutableListOf<LispObject>()
        while (rs.next()) {
            rows.add(resultSetRowToPMap(rs, columnDefs))
        }
        return if (rows.isEmpty()) ListObject.NIL else ListObject.fromList(rows)
    }

    private fun resultSetRowToPMap(rs: ResultSet, columnDefs: List<ColumnDefinition>): PMapObject {
        val meta = rs.metaData
        return resultMapper.mapRow(
            columnDefs = columnDefs,
            getColumnName = { meta.getColumnName(it + 1) },
            getColumnValue = { i, colDef -> resultSetValueToLisp(rs, i + 1, colDef) },
            columnCount = meta.columnCount
        )
    }

    private fun resultSetValueToLisp(rs: ResultSet, index: Int, colDef: ColumnDefinition?): LispObject {
        val value = rs.getObject(index)
        return when {
            value == null -> nullAtom
            value is Long || value is Int -> {
                if (colDef?.type == ColumnType.BOOLEAN) {
                    if (value.toString().toLong() == 0L) BooleanObject.FALSE else BooleanObject.TRUE
                } else {
                    IntObject(value.toString().toInt())
                }
            }
            value is Double || value is Float -> DoubleObject(value.toString().toDouble())
            value is String -> StringObject(value)
            value is ByteArray -> {
                StringObject(java.util.Base64.getEncoder().encodeToString(value))
            }
            else -> StringObject(value.toString())
        }
    }

    /**
     * Initialize the database schema.
     */
    private fun initializeSchema() {
        val conn = connection ?: return

        // Create schema tracking table
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS $SCHEMA_TABLE (
                table_name TEXT PRIMARY KEY,
                schema_hash TEXT NOT NULL,
                version INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())

        // Validate foreign key references
        val errors = schemaRegistry.validateReferences()
        if (errors.isNotEmpty()) {
            println("[JdbcDatabaseConnection] Schema validation warnings: ${errors.joinToString("; ")}")
        }

        // Create/migrate tables in dependency order
        val tables = schemaRegistry.getTablesInDependencyOrder()
        for (table in tables) {
            createOrMigrateTable(conn, table)
        }
    }

    /**
     * Create a new table or migrate an existing one.
     */
    private fun createOrMigrateTable(conn: Connection, table: TableDefinition) {
        // Check if table exists
        val stmt = conn.prepareStatement(
            "SELECT schema_hash FROM $SCHEMA_TABLE WHERE table_name = ?"
        )
        stmt.setString(1, table.name)
        val rs = stmt.executeQuery()

        val existingHash = if (rs.next()) rs.getString(1) else null
        rs.close()
        stmt.close()

        if (existingHash == null) {
            // Table doesn't exist - create it
            println("[JdbcDatabaseConnection] Creating table: ${table.name}")
            conn.createStatement().execute(table.toCreateTableSql())

            // Record schema
            val insertStmt = conn.prepareStatement(
                "INSERT INTO $SCHEMA_TABLE (table_name, schema_hash, version) VALUES (?, ?, ?)"
            )
            insertStmt.setString(1, table.name)
            insertStmt.setString(2, table.schemaHash)
            insertStmt.setInt(3, 1)
            insertStmt.executeUpdate()
            insertStmt.close()

        } else if (existingHash != table.schemaHash) {
            // Schema changed - attempt migration
            println("[JdbcDatabaseConnection] Migrating table: ${table.name}")
            migrateTable(conn, table)

            // Update schema hash
            val updateStmt = conn.prepareStatement(
                "UPDATE $SCHEMA_TABLE SET schema_hash = ? WHERE table_name = ?"
            )
            updateStmt.setString(1, table.schemaHash)
            updateStmt.setString(2, table.name)
            updateStmt.executeUpdate()
            updateStmt.close()
        }
    }

    /**
     * Migrate a table by adding new columns.
     */
    private fun migrateTable(conn: Connection, table: TableDefinition) {
        // Get existing columns (SQL names)
        val rs = conn.createStatement().executeQuery("PRAGMA table_info(${table.sqlName})")
        val existingColumns = mutableSetOf<String>()
        while (rs.next()) {
            existingColumns.add(rs.getString(2)) // Column name is at index 2 (1-based: cid, name, type, ...)
        }
        rs.close()

        // Add missing columns (compare by SQL name)
        for (column in table.columns) {
            if (!existingColumns.contains(column.sqlName)) {
                println("[JdbcDatabaseConnection] Adding column ${column.sqlName} to ${table.sqlName}")
                try {
                    conn.createStatement().execute(table.toAddColumnSql(column))
                } catch (e: Exception) {
                    println("[JdbcDatabaseConnection] Failed to add column ${column.sqlName}: ${e.message}")
                }
            }
        }
    }

    /**
     * Categorize database errors for user-friendly messages.
     */
    private fun categorizeError(e: Exception): String {
        val message = e.message ?: return "Unknown database error"

        return when {
            message.contains("UNIQUE constraint failed", ignoreCase = true) ->
                "Constraint violation: UNIQUE"
            message.contains("NOT NULL constraint failed", ignoreCase = true) ->
                "Constraint violation: NOT NULL"
            message.contains("FOREIGN KEY constraint failed", ignoreCase = true) ->
                "Constraint violation: FOREIGN KEY"
            message.contains("no such table", ignoreCase = true) -> {
                val tableName = Regex("no such table: (\\w+)").find(message)?.groupValues?.get(1)
                "Table not found: ${tableName ?: "unknown"}"
            }
            message.contains("no such column", ignoreCase = true) -> {
                val colName = Regex("no such column: (\\w+)").find(message)?.groupValues?.get(1)
                "Column not found: ${colName ?: "unknown"}"
            }
            else -> "SQL error: $message"
        }
    }
}
