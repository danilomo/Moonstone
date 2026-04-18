package net.sourceforge.moonstone.persistence

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.*
import net.sourceforge.kleinlisp.LispEnvironment
import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.objects.*
import net.sourceforge.moonstone.persistence.db.*
import java.io.File

/**
 * Android-specific implementation of DatabaseConnection.
 *
 * Wraps Android's SQLiteDatabase with the DatabaseConnection interface.
 * All async operations use Kotlin Coroutines, executing on Dispatchers.IO
 * and invoking callbacks on Dispatchers.Main.
 */
class AndroidDatabaseConnection(
    private val databasePath: File,
    private val schemaRegistry: SchemaRegistry,
    private val environment: LispEnvironment
) : DatabaseConnection {

    companion object {
        private const val TAG = "AndroidDatabaseConnection"
        private const val SCHEMA_TABLE = "_klein_schema"
    }

    private var database: SQLiteDatabase? = null
    private val dbScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isInitialized = false
    private val resultMapper = CommonResultMapper()

    // Cache the 'null atom for SQL NULL representation
    private val nullAtom: AtomObject by lazy { environment.atomOf("null") }

    @Synchronized
    override fun open() {
        if (database?.isOpen == true) {
            return
        }

        // Ensure parent folder exists
        val parentDir = databasePath.parentFile
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs()
        }

        // Open or create database
        database = SQLiteDatabase.openOrCreateDatabase(databasePath, null)

        // Enable foreign keys
        database!!.execSQL("PRAGMA foreign_keys = ON")

        // Initialize schema if needed
        if (!isInitialized) {
            initializeSchema()
            isInitialized = true
        }
    }

    @Synchronized
    override fun close() {
        dbScope.cancel()
        database?.close()
        database = null
        isInitialized = false
    }

    override fun isOpen(): Boolean = database?.isOpen == true

    override fun executeQuery(
        sql: String,
        args: Array<String>,
        columnDefs: List<ColumnDefinition>,
        callback: (LispObject, String?) -> Unit
    ) {
        dbScope.launch {
            try {
                val db = getOpenDatabase()
                val cursor = db.rawQuery(sql, args)
                val results = cursorToListObject(cursor, columnDefs)
                cursor.close()

                withContext(Dispatchers.Main) {
                    callback(results, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Query error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(ListObject.NIL, categorizeError(e))
                }
            }
        }
    }

    override fun executeInsert(
        table: String,
        values: Map<String, Any?>,
        callback: (LispObject, String?) -> Unit
    ) {
        dbScope.launch {
            try {
                val db = getOpenDatabase()
                val contentValues = mapToContentValues(values)
                val id = db.insertOrThrow(table, null, contentValues)

                withContext(Dispatchers.Main) {
                    callback(IntObject(id.toInt()), null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Insert error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(IntObject(-1), categorizeError(e))
                }
            }
        }
    }

    override fun executeBatchInsert(
        table: String,
        valuesList: List<Map<String, Any?>>,
        callback: (LispObject, String?) -> Unit
    ) {
        dbScope.launch {
            try {
                val db = getOpenDatabase()
                val ids = mutableListOf<LispObject>()

                db.beginTransaction()
                try {
                    for (values in valuesList) {
                        val contentValues = mapToContentValues(values)
                        val id = db.insertOrThrow(table, null, contentValues)
                        ids.add(IntObject(id.toInt()))
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }

                withContext(Dispatchers.Main) {
                    callback(ListObject.fromList(ids), null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Batch insert error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(ListObject.NIL, categorizeError(e))
                }
            }
        }
    }

    override fun executeUpdate(
        table: String,
        values: Map<String, Any?>,
        whereClause: String?,
        whereArgs: Array<String>?,
        callback: (LispObject, String?) -> Unit
    ) {
        dbScope.launch {
            try {
                val db = getOpenDatabase()
                val contentValues = mapToContentValues(values)
                val count = db.update(table, contentValues, whereClause, whereArgs)

                withContext(Dispatchers.Main) {
                    callback(IntObject(count), null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(IntObject(0), categorizeError(e))
                }
            }
        }
    }

    override fun executeDelete(
        table: String,
        whereClause: String?,
        whereArgs: Array<String>?,
        callback: (LispObject, String?) -> Unit
    ) {
        dbScope.launch {
            try {
                val db = getOpenDatabase()
                val count = db.delete(table, whereClause, whereArgs)

                withContext(Dispatchers.Main) {
                    callback(IntObject(count), null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Delete error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(IntObject(0), categorizeError(e))
                }
            }
        }
    }

    override fun executeCount(
        table: String,
        whereClause: String?,
        whereArgs: Array<String>?,
        callback: (LispObject, String?) -> Unit
    ) {
        Log.d(TAG, "executeCount called: table=$table, whereClause=$whereClause")
        dbScope.launch {
            Log.d(TAG, "executeCount: coroutine started")
            try {
                Log.d(TAG, "executeCount: opening database")
                val db = getOpenDatabase()
                Log.d(TAG, "executeCount: database opened")
                val sql = if (whereClause != null) {
                    "SELECT COUNT(*) FROM $table WHERE $whereClause"
                } else {
                    "SELECT COUNT(*) FROM $table"
                }
                Log.d(TAG, "executeCount: executing SQL: $sql")
                val cursor = db.rawQuery(sql, whereArgs ?: emptyArray())
                val count = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
                cursor.close()
                Log.d(TAG, "executeCount: count result = $count")

                withContext(Dispatchers.Main) {
                    Log.d(TAG, "executeCount: calling callback with count=$count")
                    callback(IntObject(count.toInt()), null)
                    Log.d(TAG, "executeCount: callback returned")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Count error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(IntObject(0), categorizeError(e))
                }
            }
        }
        Log.d(TAG, "executeCount: launch completed (async)")
    }

    override fun executeRawQuery(
        sql: String,
        args: Array<String>,
        callback: (LispObject, String?) -> Unit
    ) {
        dbScope.launch {
            try {
                val db = getOpenDatabase()
                val cursor = db.rawQuery(sql, args)

                // Build column definitions from cursor metadata
                val columnDefs = (0 until cursor.columnCount).map { i ->
                    ColumnDefinition(
                        name = cursor.getColumnName(i),
                        type = ColumnType.STRING // Default, actual type determined at runtime
                    )
                }

                val results = cursorToListObject(cursor, columnDefs)
                cursor.close()

                withContext(Dispatchers.Main) {
                    callback(results, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Raw query error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(ListObject.NIL, "SQL error: ${e.message}")
                }
            }
        }
    }

    override fun executeRawUpdate(
        sql: String,
        args: Array<String>,
        callback: (LispObject, String?) -> Unit
    ) {
        dbScope.launch {
            try {
                val db = getOpenDatabase()

                val statement = db.compileStatement(sql)
                for (i in args.indices) {
                    statement.bindString(i + 1, args[i])
                }

                val result = try {
                    // For INSERT, returns row ID; for UPDATE/DELETE, returns affected rows
                    if (sql.trim().uppercase().startsWith("INSERT")) {
                        statement.executeInsert().toInt()
                    } else {
                        statement.executeUpdateDelete()
                    }
                } finally {
                    statement.close()
                }

                withContext(Dispatchers.Main) {
                    callback(IntObject(result), null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Raw update error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(IntObject(0), "SQL error: ${e.message}")
                }
            }
        }
    }

    override fun executeTransaction(
        transactionBlock: (TransactionContext) -> LispObject,
        callback: (LispObject, String?) -> Unit
    ) {
        dbScope.launch {
            try {
                val db = getOpenDatabase()
                var result: LispObject = BooleanObject.TRUE
                var error: String? = null

                db.beginTransaction()
                try {
                    val txContext = AndroidTransactionContext(db)
                    result = transactionBlock(txContext)

                    // Check if transaction should be committed
                    if (result.truthiness() && !txContext.hasError()) {
                        db.setTransactionSuccessful()
                    } else if (txContext.hasError()) {
                        error = txContext.getErrorMessage()
                    }
                } catch (e: Exception) {
                    error = categorizeError(e)
                } finally {
                    db.endTransaction()
                }

                withContext(Dispatchers.Main) {
                    callback(if (error == null) BooleanObject.TRUE else BooleanObject.FALSE, error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Transaction error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(BooleanObject.FALSE, categorizeError(e))
                }
            }
        }
    }

    override fun executeMigration(
        version: Int,
        statements: List<String>,
        callback: (LispObject, String?) -> Unit
    ) {
        dbScope.launch {
            try {
                val db = getOpenDatabase()

                db.beginTransaction()
                try {
                    for (sql in statements) {
                        db.execSQL(sql)
                    }

                    // Update migration version in schema table
                    val values = ContentValues().apply {
                        put("table_name", "_migration_version")
                        put("schema_hash", version.toString())
                        put("version", version)
                    }
                    db.insertWithOnConflict(
                        SCHEMA_TABLE, null, values,
                        SQLiteDatabase.CONFLICT_REPLACE
                    )

                    db.setTransactionSuccessful()

                    withContext(Dispatchers.Main) {
                        callback(BooleanObject.TRUE, null)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        callback(BooleanObject.FALSE, "Migration failed: ${e.message}")
                    }
                } finally {
                    db.endTransaction()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Migration error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(BooleanObject.FALSE, categorizeError(e))
                }
            }
        }
    }

    // ========== Inner class for TransactionContext ==========

    private inner class AndroidTransactionContext(
        private val db: SQLiteDatabase
    ) : TransactionContext {
        private var errorMessage: String? = null

        override fun insert(tableName: String, values: Map<String, Any?>): Long {
            val contentValues = mapToContentValues(values)
            return db.insertOrThrow(tableName, null, contentValues)
        }

        override fun update(
            tableName: String,
            values: Map<String, Any?>,
            whereClause: String?,
            whereArgs: Array<String>?
        ): Int {
            val contentValues = mapToContentValues(values)
            return db.update(tableName, contentValues, whereClause, whereArgs)
        }

        override fun delete(
            tableName: String,
            whereClause: String?,
            whereArgs: Array<String>?
        ): Int {
            return db.delete(tableName, whereClause, whereArgs)
        }

        override fun query(
            sql: String,
            args: Array<String>,
            columnDefs: List<ColumnDefinition>
        ): LispObject {
            val cursor = db.rawQuery(sql, args)
            val rows = mutableListOf<LispObject>()

            while (cursor.moveToNext()) {
                rows.add(cursorRowToPMap(cursor, columnDefs))
            }
            cursor.close()

            return if (rows.isEmpty()) ListObject.NIL else ListObject.fromList(rows)
        }

        override fun querySingle(
            sql: String,
            args: Array<String>,
            columnDefs: List<ColumnDefinition>
        ): LispObject {
            val cursor = db.rawQuery(sql, args)
            val result = if (cursor.moveToFirst()) {
                cursorRowToPMap(cursor, columnDefs)
            } else {
                BooleanObject.FALSE
            }
            cursor.close()
            return result
        }

        override fun setError(message: String) {
            errorMessage = message
        }

        override fun hasError(): Boolean = errorMessage != null

        fun getErrorMessage(): String? = errorMessage
    }

    // ========== Private Helper Methods ==========

    private fun getOpenDatabase(): SQLiteDatabase {
        if (database?.isOpen != true) {
            open()
        }
        return database ?: throw IllegalStateException("Database not open")
    }

    private fun mapToContentValues(values: Map<String, Any?>): ContentValues {
        val cv = ContentValues()
        for ((key, value) in values) {
            when (value) {
                null -> cv.putNull(key)
                is String -> cv.put(key, value)
                is Int -> cv.put(key, value)
                is Long -> cv.put(key, value)
                is Double -> cv.put(key, value)
                is Float -> cv.put(key, value)
                is Boolean -> cv.put(key, if (value) 1 else 0)
                is ByteArray -> cv.put(key, value)
                else -> cv.put(key, value.toString())
            }
        }
        return cv
    }

    private fun cursorToListObject(cursor: Cursor, columnDefs: List<ColumnDefinition>): LispObject {
        val rows = mutableListOf<LispObject>()
        while (cursor.moveToNext()) {
            rows.add(cursorRowToPMap(cursor, columnDefs))
        }
        return if (rows.isEmpty()) ListObject.NIL else ListObject.fromList(rows)
    }

    private fun cursorRowToPMap(cursor: Cursor, columnDefs: List<ColumnDefinition>): PMapObject {
        return resultMapper.mapRow(
            columnDefs = columnDefs,
            getColumnName = { cursor.getColumnName(it) },
            getColumnValue = { i, colDef -> cursorValueToLisp(cursor, i, colDef) },
            columnCount = cursor.columnCount
        )
    }

    private fun cursorValueToLisp(cursor: Cursor, index: Int, colDef: ColumnDefinition?): LispObject {
        return when (cursor.getType(index)) {
            Cursor.FIELD_TYPE_NULL -> nullAtom
            Cursor.FIELD_TYPE_INTEGER -> {
                if (colDef?.type == ColumnType.BOOLEAN) {
                    if (cursor.getLong(index) == 0L) BooleanObject.FALSE else BooleanObject.TRUE
                } else {
                    IntObject(cursor.getLong(index).toInt())
                }
            }
            Cursor.FIELD_TYPE_FLOAT -> DoubleObject(cursor.getDouble(index))
            Cursor.FIELD_TYPE_STRING -> StringObject(cursor.getString(index))
            Cursor.FIELD_TYPE_BLOB -> {
                val bytes = cursor.getBlob(index)
                StringObject(android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
            }
            else -> nullAtom
        }
    }

    /**
     * Initialize the database schema.
     */
    private fun initializeSchema() {
        val db = database ?: return

        // Create schema tracking table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $SCHEMA_TABLE (
                table_name TEXT PRIMARY KEY,
                schema_hash TEXT NOT NULL,
                version INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent()
        )

        // Validate foreign key references
        val errors = schemaRegistry.validateReferences()
        if (errors.isNotEmpty()) {
            Log.w(TAG, "Schema validation warnings: ${errors.joinToString("; ")}")
        }

        // Create/migrate tables in dependency order
        val tables = schemaRegistry.getTablesInDependencyOrder()
        for (table in tables) {
            createOrMigrateTable(db, table)
        }
    }

    /**
     * Create a new table or migrate an existing one.
     */
    private fun createOrMigrateTable(db: SQLiteDatabase, table: TableDefinition) {
        // Check if table exists
        val cursor = db.rawQuery(
            "SELECT schema_hash FROM $SCHEMA_TABLE WHERE table_name = ?",
            arrayOf(table.name)
        )

        val existingHash = if (cursor.moveToFirst()) cursor.getString(0) else null
        cursor.close()

        if (existingHash == null) {
            // Table doesn't exist - create it
            Log.d(TAG, "Creating table: ${table.name}")
            db.execSQL(table.toCreateTableSql())

            // Record schema
            val values = ContentValues().apply {
                put("table_name", table.name)
                put("schema_hash", table.schemaHash)
                put("version", 1)
            }
            db.insert(SCHEMA_TABLE, null, values)

        } else if (existingHash != table.schemaHash) {
            // Schema changed - attempt migration
            Log.d(TAG, "Migrating table: ${table.name}")
            migrateTable(db, table)

            // Update schema hash
            val values = ContentValues().apply {
                put("schema_hash", table.schemaHash)
            }
            db.update(SCHEMA_TABLE, values, "table_name = ?", arrayOf(table.name))
        }
    }

    /**
     * Migrate a table by adding new columns.
     */
    private fun migrateTable(db: SQLiteDatabase, table: TableDefinition) {
        // Get existing columns (SQL names)
        val cursor = db.rawQuery("PRAGMA table_info(${table.sqlName})", null)
        val existingColumns = mutableSetOf<String>()
        while (cursor.moveToNext()) {
            existingColumns.add(cursor.getString(1)) // Column name is at index 1
        }
        cursor.close()

        // Add missing columns (compare by SQL name)
        for (column in table.columns) {
            if (!existingColumns.contains(column.sqlName)) {
                Log.d(TAG, "Adding column ${column.sqlName} to ${table.sqlName}")
                try {
                    db.execSQL(table.toAddColumnSql(column))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to add column ${column.sqlName}: ${e.message}")
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
