package net.sourceforge.moonstone.persistence

import net.sourceforge.moonstone.persistence.db.*
import net.sourceforge.kleinlisp.LispEnvironment
import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.objects.*
import java.io.File

/**
 * Database extension functions for KleinLisp scripts.
 *
 * Registers db-* functions that allow Scheme scripts to interact with
 * a SQLite database. The database handler is stored in the *db* Scheme variable.
 *
 * Usage:
 * ```scheme
 * ;; Create a database handler
 * (define my-db (make-db "path/to/app.db"))
 *
 * ;; Set as the active database
 * (set! *db* my-db)
 *
 * ;; Now all db-* functions use *db* implicitly
 * (db-table users (id #:serial) (name #:string))
 * (db-insert users #:values (p-map #:name "Alice") callback)
 *
 * ;; Close when done
 * (db-close my-db)
 * ```
 *
 * For GUI applications, the entry point automatically creates a handler
 * and sets *db* before loading the script, so scripts can use db-* functions
 * directly without manual setup.
 */
class DatabaseExtensions(private val env: LispEnvironment) {

    /**
     * Register all database functions with the environment.
     */
    fun register() {
        // Initialize *db* variable to #f (no database set)
        env.set(env.atomOf("*db*"), BooleanObject.FALSE)

        // Register make-db function - creates a new database handler
        env.registerFunction("make-db") { params ->
            if (params.isEmpty()) {
                throw IllegalArgumentException("make-db requires a path argument")
            }
            val path = params[0].asString()?.value()
                ?: throw IllegalArgumentException("make-db: path must be a string")
            createHandler(File(path))
        }

        // Register db-close function - closes a database handler
        env.registerFunction("db-close") { params ->
            if (params.isEmpty()) {
                throw IllegalArgumentException("db-close requires a database handler argument")
            }
            val handler = params[0] as? DatabaseHandler
                ?: throw IllegalArgumentException("db-close: argument must be a database handler")
            handler.close()
            VoidObject.VOID
        }

        // db-null? - Check if a value is SQL NULL
        env.registerFunction("db-null?") { params ->
            if (params.isEmpty()) {
                throw IllegalArgumentException("db-null? requires 1 argument")
            }
            val value = params[0]
            val isNull = value.asAtom()?.toString() == "null"
            if (isNull) BooleanObject.TRUE else BooleanObject.FALSE
        }

        // db-table - Define a table schema (MACRO - receives unevaluated args)
        // Format: (db-table name (col1 #:type ...) (col2 #:type ...) ...)
        env.registerMacro("db-table") { list ->
            try {
                // list = (db-table name (col1 ...) (col2 ...) ...)
                val args = list.cdr() // skip "db-table"
                val tableNameAtom = args.car().asAtom()
                    ?: throw IllegalArgumentException("db-table: first argument must be a table name symbol")
                parseAndRegisterTableFromList(args)

                // Define the table name as a variable that evaluates to a string
                // This allows (db-count tablename ...) to work when tablename is evaluated
                // We use StringObject so extractTableName can handle it
                env.set(tableNameAtom, StringObject(tableNameAtom.toString()))
            } catch (e: Exception) {
                println("[DatabaseExtensions] ERROR: db-table macro error: ${e.message}")
                throw e
            }
            // Return (quote #f) which evaluates to #f
            // Note: We can't use (begin) because KleinLisp's BeginForm has a bug
            // that causes NPE when processing empty begin
            ListObject(env.atomOf("quote"), ListObject(BooleanObject.FALSE, ListObject.NIL))
        }

        // Register query functions (as macros since they also receive unevaluated args)
        registerQueryMacros()
        registerManipulationFunctions()
    }

    /**
     * Create a new database handler for the given path.
     *
     * @param dbPath Path to the database file
     * @return A new DatabaseHandler instance
     */
    fun createHandler(dbPath: File): DatabaseHandler {
        val schemaRegistry = SchemaRegistry()
        val queryBuilder = QueryBuilder(schemaRegistry)
        val connection = DatabaseFactory().createConnection(dbPath, schemaRegistry, env)
        return DatabaseHandler(connection, schemaRegistry, queryBuilder, env, dbPath)
    }

    /**
     * Get the current database handler from the *db* variable.
     *
     * @throws IllegalStateException if no database is set
     */
    private fun getCurrentHandler(): DatabaseHandler {
        val dbVar = env.lookupValueOrNull(env.atomOf("*db*"))
        return dbVar as? DatabaseHandler
            ?: throw IllegalStateException(
                "No database set. Call (set! *db* (make-db \"path/to/db\")) first, " +
                "or ensure the GUI entry point has set up the database."
            )
    }

    /**
     * Get the schema registry from the current handler.
     */
    private fun getCurrentSchemaRegistry(): SchemaRegistry = getCurrentHandler().schemaRegistry

    /**
     * Get the query builder from the current handler.
     */
    private fun getCurrentQueryBuilder(): QueryBuilder = getCurrentHandler().queryBuilder

    /**
     * Get the database connection from the current handler.
     */
    private fun getCurrentConnection(): DatabaseConnection = getCurrentHandler().connection

    // ========== Query Function Registration ==========

    private fun registerQueryMacros() {
        // db-query - Define a query function that returns multiple rows (MACRO)
        // Format: (db-query name #:from table #:where condition ...)
        env.registerMacro("db-query") { list ->
            try {
                val args = list.cdr() // skip "db-query"
                val queryDef = parseQueryDefinitionFromList(args, isSingle = false)
                registerGeneratedQueryFunction(queryDef)
            } catch (e: Exception) {
                println("[DatabaseExtensions] ERROR: db-query macro error: ${e.message}")
                throw e
            }
            // Return (quote #f) which evaluates to #f
            ListObject(env.atomOf("quote"), ListObject(BooleanObject.FALSE, ListObject.NIL))
        }

        // db-query-single - Define a query function that returns single row or #f (MACRO)
        env.registerMacro("db-query-single") { list ->
            try {
                val args = list.cdr() // skip "db-query-single"
                val queryDef = parseQueryDefinitionFromList(args, isSingle = true)
                registerGeneratedQueryFunction(queryDef)
            } catch (e: Exception) {
                println("[DatabaseExtensions] ERROR: db-query-single macro error: ${e.message}")
                throw e
            }
            // Return (quote #f) which evaluates to #f
            ListObject(env.atomOf("quote"), ListObject(BooleanObject.FALSE, ListObject.NIL))
        }

        // db-count - Count rows (immediate execution, not definition)
        // This is a MACRO because the WHERE condition should NOT be evaluated
        // (column names like is-active would be looked up as variables otherwise)
        // The macro transforms the call to use __db-count-impl at runtime
        env.registerMacro("db-count") { list ->
            transformCountMacro(list)
        }

        // __db-count-impl - Internal function called at runtime by db-count macro
        env.registerFunction("__db-count-impl") { params ->
            executeCountImpl(params)
        }

        // db-execute - Raw SQL query (Phase 6)
        env.registerFunction("db-execute") { params ->
            executeRawQuery(params)
        }

        // db-execute-update - Raw SQL update (Phase 6)
        env.registerFunction("db-execute-update") { params ->
            executeRawUpdate(params)
        }
    }

    /**
     * Execute raw SQL query.
     *
     * Format: (db-execute "SELECT ..." #:params (val1 val2) callback)
     */
    private fun executeRawQuery(params: Array<out LispObject>): LispObject {
        println("[DatabaseExtensions] executeRawQuery called with ${params.size} params")
        if (params.isEmpty()) {
            throw IllegalArgumentException("db-execute requires a SQL string")
        }

        val sql = params[0].asString()?.value()
            ?: throw IllegalArgumentException("db-execute: first argument must be a SQL string")
        println("[DatabaseExtensions] executeRawQuery SQL: $sql")

        var sqlParams: List<String> = emptyList()
        var callback: net.sourceforge.kleinlisp.Function? = null

        var i = 1
        while (i < params.size) {
            val keyword = params[i].asKeyword()?.name()
            if (keyword != null && i + 1 < params.size) {
                when (keyword) {
                    "params" -> {
                        val paramList = params[i + 1].asList()
                            ?: throw IllegalArgumentException("db-execute: #:params requires a list")
                        sqlParams = listToArray(paramList).map { lispObjectToSqlString(it) }
                    }
                }
                i += 2
            } else if (params[i].asFunction() != null) {
                callback = params[i].asFunction().function()
                i++
            } else {
                i++
            }
        }

        if (callback == null) {
            throw IllegalArgumentException("db-execute: callback function is required")
        }

        println("[DatabaseExtensions] executeRawQuery executing with params: $sqlParams")
        val finalCallback = callback
        val connection = getCurrentConnection()
        connection.executeRawQuery(sql, sqlParams.toTypedArray()) { result, error ->
            println("[DatabaseExtensions] executeRawQuery callback: result=$result, error=$error")
            val errorObj = if (error != null) StringObject(error) else BooleanObject.FALSE
            finalCallback.evaluate(arrayOf(result, errorObj))
        }

        return VoidObject.VOID
    }

    /**
     * Execute raw SQL update (INSERT/UPDATE/DELETE).
     *
     * Format: (db-execute-update "UPDATE ..." #:params (val1 val2) callback)
     */
    private fun executeRawUpdate(params: Array<out LispObject>): LispObject {
        if (params.isEmpty()) {
            throw IllegalArgumentException("db-execute-update requires a SQL string")
        }

        val sql = params[0].asString()?.value()
            ?: throw IllegalArgumentException("db-execute-update: first argument must be a SQL string")

        var sqlParams: List<String> = emptyList()
        var callback: net.sourceforge.kleinlisp.Function? = null

        var i = 1
        while (i < params.size) {
            val keyword = params[i].asKeyword()?.name()
            if (keyword != null && i + 1 < params.size) {
                when (keyword) {
                    "params" -> {
                        val paramList = params[i + 1].asList()
                            ?: throw IllegalArgumentException("db-execute-update: #:params requires a list")
                        sqlParams = listToArray(paramList).map { lispObjectToSqlString(it) }
                    }
                }
                i += 2
            } else if (params[i].asFunction() != null) {
                callback = params[i].asFunction().function()
                i++
            } else {
                i++
            }
        }

        if (callback == null) {
            throw IllegalArgumentException("db-execute-update: callback function is required")
        }

        val finalCallback = callback
        val connection = getCurrentConnection()
        connection.executeRawUpdate(sql, sqlParams.toTypedArray()) { result, error ->
            val errorObj = if (error != null) StringObject(error) else BooleanObject.FALSE
            finalCallback.evaluate(arrayOf(result, errorObj))
        }

        return VoidObject.VOID
    }

    /**
     * Parse a query definition from a ListObject (macro version).
     * Converts the list to an array and delegates to parseQueryDefinition.
     */
    private fun parseQueryDefinitionFromList(list: ListObject, isSingle: Boolean): QueryDefinition {
        val params = listToArray(list)
        return parseQueryDefinition(params.toTypedArray(), isSingle)
    }

    /**
     * Parse a query definition from db-query or db-query-single arguments.
     *
     * Expected format:
     * (db-query function-name
     *   #:from table-name
     *   #:columns (col1 col2 ...)
     *   #:join (table #:on condition)
     *   #:where condition
     *   #:order-by (col1 #:asc col2 #:desc)
     *   #:limit n
     *   #:params (param1 param2 ...))
     */
    private fun parseQueryDefinition(params: Array<out LispObject>, isSingle: Boolean): QueryDefinition {
        if (params.isEmpty()) {
            throw IllegalArgumentException("db-query requires a function name")
        }

        // First param is function name
        val functionName = params[0].asAtom()?.toString()
            ?: throw IllegalArgumentException("db-query: first argument must be a function name symbol")

        // Parse keyword arguments
        var tableName: String? = null
        var columns: List<String>? = null
        val joins = mutableListOf<JoinDefinition>()
        var whereCondition: LispObject? = null
        var orderBy: MutableList<Pair<String, String>>? = null
        var limit: Int? = null
        var limitParam: String? = null
        var parameterNames: List<String> = emptyList()

        var i = 1
        while (i < params.size) {
            val keyword = params[i].asKeyword()?.name()
            if (keyword != null && i + 1 < params.size) {
                val value = params[i + 1]
                when (keyword) {
                    "from" -> {
                        tableName = value.asAtom()?.toString()
                            ?: throw IllegalArgumentException("#:from requires a table name symbol")
                    }
                    "columns" -> {
                        val colList = value.asList()
                            ?: throw IllegalArgumentException("#:columns requires a list of column names")
                        columns = listToArray(colList).map { it.asAtom()?.toString() ?: it.toString() }
                    }
                    "join" -> {
                        val joinDef = parseJoin(value)
                        if (joinDef != null) joins.add(joinDef)
                    }
                    "left-join" -> {
                        val joinDef = parseJoin(value, "LEFT")
                        if (joinDef != null) joins.add(joinDef)
                    }
                    "where" -> {
                        whereCondition = value
                    }
                    "order-by" -> {
                        orderBy = parseOrderBy(value)
                    }
                    "limit" -> {
                        // Check if it's a parameter reference (e.g., ?limit)
                        val atomStr = value.asAtom()?.toString()
                        if (atomStr != null && atomStr.startsWith("?")) {
                            limitParam = atomStr.substring(1) // Remove the '?'
                        } else {
                            limit = value.asInt()?.value()
                                ?: throw IllegalArgumentException("#:limit requires an integer or parameter (?name)")
                        }
                    }
                    "params" -> {
                        val paramList = value.asList()
                            ?: throw IllegalArgumentException("#:params requires a list of parameter names")
                        parameterNames = listToArray(paramList).map { it.asAtom()?.toString() ?: it.toString() }
                    }
                }
                i += 2
            } else {
                i++
            }
        }

        if (tableName == null) {
            throw IllegalArgumentException("db-query '$functionName': #:from is required")
        }

        // For single queries, default limit to 1 (unless using a parameter)
        val effectiveLimit = if (isSingle && limit == null && limitParam == null) 1 else limit

        return QueryDefinition(
            name = functionName,
            tableName = tableName,
            columns = columns,
            joins = if (joins.isEmpty()) null else joins,
            whereCondition = whereCondition,
            orderBy = orderBy,
            limit = effectiveLimit,
            limitParam = limitParam,
            parameterNames = parameterNames,
            isSingle = isSingle
        )
    }

    /**
     * Parse a #:join value.
     * Format: (table-name #:on condition)
     */
    private fun parseJoin(value: LispObject, joinType: String = "INNER"): JoinDefinition? {
        val list = value.asList()
            ?: throw IllegalArgumentException("#:join requires a list (table #:on condition)")

        val elements = listToArray(list)
        if (elements.isEmpty()) return null

        val joinTableName = elements[0].asAtom()?.toString()
            ?: throw IllegalArgumentException("#:join: first element must be a table name")

        var onCondition: LispObject? = null

        var i = 1
        while (i < elements.size) {
            val keyword = elements[i].asKeyword()?.name()
            if (keyword == "on" && i + 1 < elements.size) {
                onCondition = elements[i + 1]
                i += 2
            } else {
                i++
            }
        }

        if (onCondition == null) {
            throw IllegalArgumentException("#:join: #:on condition is required")
        }

        return JoinDefinition(
            tableName = joinTableName,
            joinType = joinType,
            onCondition = onCondition
        )
    }

    /**
     * Parse #:order-by value.
     * Format: (col1 #:asc col2 #:desc) or (col1 col2) defaults to ASC
     */
    private fun parseOrderBy(value: LispObject): MutableList<Pair<String, String>> {
        val list = value.asList()
            ?: throw IllegalArgumentException("#:order-by requires a list")

        val elements = listToArray(list)
        val result = mutableListOf<Pair<String, String>>()

        var i = 0
        while (i < elements.size) {
            val colName = elements[i].asAtom()?.toString()
            if (colName != null) {
                // Check if next element is direction
                var direction = "ASC"
                if (i + 1 < elements.size) {
                    val nextKeyword = elements[i + 1].asKeyword()?.name()
                    if (nextKeyword == "asc" || nextKeyword == "desc") {
                        direction = nextKeyword.uppercase()
                        i++
                    }
                }
                result.add(Pair(colName, direction))
            }
            i++
        }

        return result
    }

    /**
     * Register a generated query function in the environment.
     */
    private fun registerGeneratedQueryFunction(queryDef: QueryDefinition) {
        env.registerFunction(queryDef.name) { params ->
            executeGeneratedQuery(queryDef, params)
        }
    }

    /**
     * Execute a generated query function.
     *
     * The function receives:
     * - Named parameters (#:param-name value ...)
     * - A callback as the last argument
     */
    private fun executeGeneratedQuery(queryDef: QueryDefinition, params: Array<out LispObject>): LispObject {
        println("[DatabaseExtensions] executeGeneratedQuery: ${queryDef.name}, params=${params.toList()}")

        val handler = getCurrentHandler()
        val queryBuilder = handler.queryBuilder

        // Parse parameters and callback
        val paramValues = mutableMapOf<String, LispObject>()
        var callback: net.sourceforge.kleinlisp.Function? = null

        var i = 0
        while (i < params.size) {
            val keyword = params[i].asKeyword()?.name()
            if (keyword != null && i + 1 < params.size) {
                paramValues[keyword] = params[i + 1]
                i += 2
            } else if (params[i].asFunction() != null) {
                callback = params[i].asFunction().function()
                i++
            } else {
                println("[DatabaseExtensions] executeGeneratedQuery: skipping param[$i]=${params[i]}")
                i++
            }
        }

        if (callback == null) {
            println("[DatabaseExtensions] ERROR: executeGeneratedQuery: no callback found!")
            throw IllegalArgumentException("${queryDef.name}: callback function is required")
        }

        // Resolve dynamic limit if using a parameter
        val effectiveLimit = if (queryDef.limitParam != null) {
            val limitValue = paramValues[queryDef.limitParam]
                ?: throw IllegalArgumentException("${queryDef.name}: missing parameter #:${queryDef.limitParam}")
            limitValue.asInt()?.value()
                ?: throw IllegalArgumentException("${queryDef.name}: #:${queryDef.limitParam} must be an integer")
        } else {
            queryDef.limit
        }

        // Build the query
        val queryResult = queryBuilder.buildSelect(
            tableName = queryDef.tableName,
            columns = queryDef.columns,
            joins = queryDef.joins,
            whereCondition = queryDef.whereCondition,
            orderBy = queryDef.orderBy,
            limit = effectiveLimit
        )

        // Bind parameters
        val boundParams = queryResult.parameterNames.map { paramName ->
            val value = paramValues[paramName]
                ?: throw IllegalArgumentException("${queryDef.name}: missing parameter #:$paramName")
            lispObjectToSqlString(value)
        }

        // Execute query asynchronously
        val finalCallback = callback
        val isSingle = queryDef.isSingle

        println("[DatabaseExtensions] executeGeneratedQuery: SQL=${queryResult.sql}, args=${boundParams}")

        handler.connection.executeQuery(
            sql = queryResult.sql,
            args = boundParams.toTypedArray(),
            columnDefs = queryResult.columnDefs
        ) { result, error ->
            println("[DatabaseExtensions] executeGeneratedQuery callback: queryName=${queryDef.name}, result=$result, error=$error")
            if (isSingle) {
                // For single queries, return first row or #f
                val singleResult = if (result.asList()?.let { it != ListObject.NIL } == true) {
                    result.asList()!!.car()
                } else {
                    BooleanObject.FALSE
                }
                val errorObj = if (error != null) StringObject(error) else BooleanObject.FALSE
                println("[DatabaseExtensions] executeGeneratedQuery: about to call user callback for ${queryDef.name}")
                try {
                    finalCallback.evaluate(arrayOf(singleResult, errorObj))
                    println("[DatabaseExtensions] executeGeneratedQuery: user callback completed for ${queryDef.name}")
                } catch (e: Exception) {
                    println("[DatabaseExtensions] ERROR: executeGeneratedQuery: callback threw exception for ${queryDef.name} - ${e.message}")
                    throw e
                }
            } else {
                val errorObj = if (error != null) StringObject(error) else BooleanObject.FALSE
                println("[DatabaseExtensions] executeGeneratedQuery: about to call user callback for ${queryDef.name}")
                try {
                    finalCallback.evaluate(arrayOf(result, errorObj))
                    println("[DatabaseExtensions] executeGeneratedQuery: user callback completed for ${queryDef.name}")
                } catch (e: Exception) {
                    println("[DatabaseExtensions] ERROR: executeGeneratedQuery: callback threw exception for ${queryDef.name} - ${e.message}")
                    throw e
                }
            }
        }

        return VoidObject.VOID
    }

    // Counter for generating unique IDs for db-count calls
    private var countCallId = 0
    private val countCallData = mutableMapOf<Int, CountCallData>()

    private data class CountCallData(
        val tableName: String,
        val whereCondition: LispObject?,
        val paramValues: Map<String, String>
    )

    /**
     * Transform db-count macro into a runtime call.
     *
     * Transforms: (db-count table #:where condition callback)
     * Into: (__db-count-impl <id> callback)
     *
     * The WHERE condition is stored during macro expansion and retrieved at runtime.
     */
    private fun transformCountMacro(list: ListObject): ListObject {
        println("[DatabaseExtensions] transformCountMacro called with: $list")

        val args = list.cdr() // Skip 'db-count symbol
        if (args == ListObject.NIL) {
            throw IllegalArgumentException("db-count requires a table name")
        }

        // First arg is table name (must be a symbol at this point)
        val tableNameAtom = args.car().asAtom()
            ?: throw IllegalArgumentException("db-count: first argument must be a table name symbol")
        val tableName = tableNameAtom.toString()
        println("[DatabaseExtensions] transformCountMacro table: $tableName")

        var whereCondition: LispObject? = null
        var callbackExpr: LispObject? = null

        // Parse remaining args (keep as AST, don't evaluate)
        var current: ListObject = args.cdr()
        while (current != ListObject.NIL) {
            val item = current.car()
            val keyword = item.asKeyword()?.name()

            if (keyword != null) {
                current = current.cdr()
                if (current == ListObject.NIL) break
                val value = current.car()

                when (keyword) {
                    "where" -> whereCondition = value // Keep as AST
                }
                current = current.cdr()
            } else {
                // This should be the callback expression
                callbackExpr = item
                current = current.cdr()
            }
        }

        if (callbackExpr == null) {
            throw IllegalArgumentException("db-count: callback function is required")
        }

        // Store the call data for runtime retrieval
        val id = countCallId++
        countCallData[id] = CountCallData(tableName, whereCondition, emptyMap())

        println("[DatabaseExtensions] transformCountMacro stored id=$id, tableName=$tableName, whereCondition=$whereCondition")

        // Return: (__db-count-impl <id> <callback-expr>)
        // This will be evaluated at runtime, which will evaluate callback-expr
        return ListObject(
            env.atomOf("__db-count-impl"),
            ListObject(
                IntObject(id),
                ListObject(callbackExpr, ListObject.NIL)
            )
        )
    }

    /**
     * Execute db-count at runtime.
     *
     * Called as: (__db-count-impl <id> <callback>)
     */
    private fun executeCountImpl(params: Array<out LispObject>): LispObject {
        println("[DatabaseExtensions] executeCountImpl called with params: ${params.toList()}")

        if (params.size < 2) {
            throw IllegalArgumentException("__db-count-impl requires id and callback")
        }

        val id = params[0].asInt()?.value()
            ?: throw IllegalArgumentException("__db-count-impl: first argument must be an integer id")

        val callData = countCallData[id]
            ?: throw IllegalArgumentException("__db-count-impl: invalid call id $id")

        val callback = params[1].asFunction()?.function()
            ?: throw IllegalArgumentException("__db-count-impl: second argument must be a callback function")

        println("[DatabaseExtensions] executeCountImpl: id=$id, tableName=${callData.tableName}, whereCondition=${callData.whereCondition}")

        val handler = getCurrentHandler()
        val queryBuilder = handler.queryBuilder
        val schemaRegistry = handler.schemaRegistry

        // Build count query
        val (sql, paramNames) = queryBuilder.buildCount(callData.tableName, callData.whereCondition)
        println("[DatabaseExtensions] executeCountImpl SQL: $sql, params: $paramNames")

        // Note: For simplicity, we don't support parameterized WHERE in db-count for now
        // The WHERE condition should use literal values

        // Execute count asynchronously
        handler.connection.executeCount(
            table = schemaRegistry.getTable(callData.tableName)?.sqlName ?: callData.tableName.replace('-', '_'),
            whereClause = if (sql.contains("WHERE")) sql.substringAfter("WHERE ") else null,
            whereArgs = null
        ) { result, error ->
            println("[DatabaseExtensions] executeCountImpl callback: result=$result, error=$error")
            val errorObj = if (error != null) StringObject(error) else BooleanObject.FALSE
            callback.evaluate(arrayOf(result, errorObj))
        }

        return VoidObject.VOID
    }


    /**
     * Extract a table name from a LispObject.
     * Handles atoms (symbols), strings, and identifiers.
     */
    private fun extractTableName(obj: LispObject, functionName: String): String {
        // Try string first (our db-table macro sets table names as StringObject)
        obj.asString()?.let { return it.value() }

        // Try atom (includes identifiers which have asAtom)
        obj.asAtom()?.let { return it.toString() }

        throw IllegalArgumentException("$functionName: first argument must be a table name (symbol or string), got ${obj::class.simpleName}")
    }

    /**
     * Convert a LispObject to a SQL string value.
     */
    private fun lispObjectToSqlString(obj: LispObject): String {
        return when {
            obj.asString() != null -> obj.asString().value()
            obj.asInt() != null -> obj.asInt().value().toString()
            obj.asDouble() != null -> obj.asDouble().value().toString()
            obj is BooleanObject -> if (obj.truthiness()) "1" else "0"
            obj.asAtom()?.toString() == "null" -> "NULL"
            else -> obj.toString()
        }
    }

    // ========== Table Definition Parsing ==========

    /**
     * Parse db-table arguments from a ListObject (macro version) and register the table.
     * Format: (name (col1 #:type ...) (col2 #:type ...) ...)
     */
    private fun parseAndRegisterTableFromList(list: ListObject) {
        if (list == ListObject.NIL) {
            throw IllegalArgumentException("db-table requires a table name")
        }

        val tableName = list.car().asAtom()?.toString()
            ?: throw IllegalArgumentException("db-table: first argument must be a table name symbol")

        val columns = mutableListOf<ColumnDefinition>()
        var current = list.cdr()
        while (current != ListObject.NIL) {
            val columnDef = parseColumnDefinition(current.car())
            if (columnDef != null) {
                columns.add(columnDef)
            }
            current = current.cdr()
        }

        if (columns.isEmpty()) {
            throw IllegalArgumentException("db-table '$tableName': at least one column is required")
        }

        val table = TableDefinition(tableName, columns)
        getCurrentSchemaRegistry().registerTable(table)
    }

    /**
     * Parse db-table arguments and register the table.
     */
    private fun parseAndRegisterTable(params: Array<out LispObject>) {
        if (params.isEmpty()) {
            throw IllegalArgumentException("db-table requires a table name")
        }

        val tableName = params[0].asAtom()?.toString()
            ?: throw IllegalArgumentException("db-table: first argument must be a table name symbol")

        val columns = mutableListOf<ColumnDefinition>()
        for (i in 1 until params.size) {
            val columnDef = parseColumnDefinition(params[i])
            if (columnDef != null) {
                columns.add(columnDef)
            }
        }

        if (columns.isEmpty()) {
            throw IllegalArgumentException("db-table '$tableName': at least one column is required")
        }

        val table = TableDefinition(tableName, columns)
        getCurrentSchemaRegistry().registerTable(table)
    }

    private fun parseColumnDefinition(obj: LispObject): ColumnDefinition? {
        val list = obj.asList() ?: return null
        if (list == ListObject.NIL) return null

        val elements = listToArray(list)
        if (elements.isEmpty()) return null

        val columnName = elements[0].asAtom()?.toString()
            ?: throw IllegalArgumentException("Column definition must start with a column name")

        var type: ColumnType? = null
        var isPrimaryKey = false
        var isNotNull = false
        var isUnique = false
        var defaultValue: LispObject? = null
        var defaultNow = false
        var size: Int? = null
        var references: String? = null

        var i = 1
        while (i < elements.size) {
            val elem = elements[i]
            val keyword = elem.asKeyword()?.name()

            when (keyword) {
                "serial" -> { type = ColumnType.SERIAL; isPrimaryKey = true }
                "int" -> type = ColumnType.INT
                "long" -> type = ColumnType.LONG
                "string" -> type = ColumnType.STRING
                "text" -> type = ColumnType.TEXT
                "real" -> type = ColumnType.REAL
                "boolean" -> type = ColumnType.BOOLEAN
                "timestamp" -> type = ColumnType.TIMESTAMP
                "blob" -> type = ColumnType.BLOB
                "pk" -> isPrimaryKey = true
                "not-null" -> isNotNull = true
                "unique" -> isUnique = true
                "default" -> {
                    i++
                    if (i < elements.size) {
                        val defVal = elements[i]
                        // Check for 'now (quoted symbol) or plain now
                        val symbolValue = extractSymbolValue(defVal)
                        if (symbolValue == "now") {
                            defaultNow = true
                        } else {
                            defaultValue = defVal
                        }
                    }
                }
                "size" -> {
                    i++
                    if (i < elements.size) {
                        size = elements[i].asInt()?.value()?.toInt()
                    }
                }
                "references" -> {
                    i++
                    if (i < elements.size) {
                        references = elements[i].asAtom()?.toString()
                    }
                }
            }
            i++
        }

        if (type == null) {
            throw IllegalArgumentException("Column '$columnName': no type specified")
        }

        return ColumnDefinition(
            name = columnName,
            type = type,
            isPrimaryKey = isPrimaryKey,
            isNotNull = isNotNull,
            isUnique = isUnique,
            defaultValue = defaultValue,
            defaultNow = defaultNow,
            size = size,
            references = references
        )
    }

    private fun listToArray(list: ListObject): List<LispObject> {
        val result = mutableListOf<LispObject>()
        var current: ListObject? = list
        while (current != null && current != ListObject.NIL) {
            result.add(current.car())
            val cdr = current.cdr()
            current = if (cdr is ListObject) cdr else null
        }
        return result
    }

    /**
     * Extract a symbol value from an expression.
     * Handles both plain atoms (now) and quoted symbols ('now -> (quote now)).
     */
    private fun extractSymbolValue(obj: LispObject): String? {
        // Check for plain atom
        obj.asAtom()?.let { return it.toString() }

        // Check for quoted expression: (quote symbol)
        obj.asList()?.let { list ->
            if (list != ListObject.NIL && list.length() == 2) {
                val car = list.car()
                if (car.asAtom()?.toString() == "quote") {
                    val quoted = list.cdr().car()
                    return quoted.asAtom()?.toString()
                }
            }
        }

        return null
    }

    // ========== Data Manipulation (Phase 5) ==========

    // Storage for update/delete call data (like countCallData)
    private var updateCallId = 0
    private val updateCallData = mutableMapOf<Int, UpdateCallData>()
    private var deleteCallId = 0
    private val deleteCallData = mutableMapOf<Int, DeleteCallData>()

    private data class UpdateCallData(
        val tableName: String,
        val setExpr: LispObject,
        val whereCondition: LispObject?
    )

    private data class DeleteCallData(
        val tableName: String,
        val whereCondition: LispObject?,
        val deleteAll: Boolean
    )

    private fun registerManipulationFunctions() {
        // db-insert - Insert row(s) into a table
        env.registerFunction("db-insert") { params ->
            executeInsert(params)
        }

        // db-update - Update rows in a table (MACRO to prevent WHERE evaluation)
        env.registerMacro("db-update") { list ->
            transformUpdateMacro(list)
        }

        // __db-update-impl - Internal runtime function
        env.registerFunction("__db-update-impl") { params ->
            executeUpdateImpl(params)
        }

        // db-delete - Delete rows from a table (MACRO to prevent WHERE evaluation)
        env.registerMacro("db-delete") { list ->
            transformDeleteMacro(list)
        }

        // __db-delete-impl - Internal runtime function
        env.registerFunction("__db-delete-impl") { params ->
            executeDeleteImpl(params)
        }

        // db-transaction - Execute operations atomically (Phase 6)
        env.registerFunction("db-transaction") { params ->
            executeTransaction(params)
        }

        // db-migrate - Manual schema migration (Phase 6)
        env.registerFunction("db-migrate") { params ->
            executeMigration(params)
        }
    }

    /**
     * Transform db-update macro into a runtime call.
     */
    private fun transformUpdateMacro(list: ListObject): ListObject {
        println("[DatabaseExtensions] transformUpdateMacro called with: $list")

        val args = list.cdr() // Skip 'db-update symbol
        if (args == ListObject.NIL) {
            throw IllegalArgumentException("db-update requires a table name")
        }

        val tableNameAtom = args.car().asAtom()
            ?: throw IllegalArgumentException("db-update: first argument must be a table name symbol")
        val tableName = tableNameAtom.toString()

        var setExpr: LispObject? = null
        var whereCondition: LispObject? = null
        var callbackExpr: LispObject? = null

        var current: ListObject = args.cdr()
        while (current != ListObject.NIL) {
            val item = current.car()
            val keyword = item.asKeyword()?.name()

            if (keyword != null) {
                current = current.cdr()
                if (current == ListObject.NIL) break
                val value = current.car()

                when (keyword) {
                    "set" -> setExpr = value
                    "where" -> whereCondition = value
                }
                current = current.cdr()
            } else {
                callbackExpr = item
                current = current.cdr()
            }
        }

        if (setExpr == null) {
            throw IllegalArgumentException("db-update: #:set is required")
        }
        if (callbackExpr == null) {
            throw IllegalArgumentException("db-update: callback function is required")
        }

        val id = updateCallId++
        updateCallData[id] = UpdateCallData(tableName, setExpr, whereCondition)

        // Return: (__db-update-impl <id> <set-expr> <callback-expr>)
        return ListObject(
            env.atomOf("__db-update-impl"),
            ListObject(
                IntObject(id),
                ListObject(
                    setExpr,  // set expression needs to be evaluated at runtime
                    ListObject(callbackExpr, ListObject.NIL)
                )
            )
        )
    }

    /**
     * Execute db-update at runtime.
     */
    private fun executeUpdateImpl(params: Array<out LispObject>): LispObject {
        println("[DatabaseExtensions] executeUpdateImpl called with params: ${params.toList()}")

        if (params.size < 3) {
            throw IllegalArgumentException("__db-update-impl requires id, set-values, and callback")
        }

        val id = params[0].asInt()?.value()
            ?: throw IllegalArgumentException("__db-update-impl: first argument must be an integer id")

        val callData = updateCallData[id]
            ?: throw IllegalArgumentException("__db-update-impl: invalid call id $id")

        val setValues = params[1] as? PMapObject
            ?: throw IllegalArgumentException("__db-update-impl: #:set must be a p-map")

        val callback = params[2].asFunction()?.function()
            ?: throw IllegalArgumentException("__db-update-impl: third argument must be a callback function")

        val handler = getCurrentHandler()
        val table = handler.schemaRegistry.getTable(callData.tableName)
            ?: throw IllegalArgumentException("db-update: table not found: ${callData.tableName}")

        val values = pMapToMap(setValues, table)

        // Build WHERE clause
        var whereClause: String? = null
        if (callData.whereCondition != null) {
            val (sql, _) = handler.queryBuilder.buildCount(callData.tableName, callData.whereCondition)
            whereClause = if (sql.contains("WHERE")) sql.substringAfter("WHERE ") else null
        }

        println("[DatabaseExtensions] executeUpdateImpl: table=${table.sqlName}, whereClause=$whereClause")

        handler.connection.executeUpdate(table.sqlName, values, whereClause, null) { result, error ->
            println("[DatabaseExtensions] executeUpdateImpl callback: result=$result, error=$error")
            val errorObj = if (error != null) StringObject(error) else BooleanObject.FALSE
            callback.evaluate(arrayOf(result, errorObj))
        }

        return VoidObject.VOID
    }

    /**
     * Transform db-delete macro into a runtime call.
     */
    private fun transformDeleteMacro(list: ListObject): ListObject {
        println("[DatabaseExtensions] transformDeleteMacro called with: $list")

        val args = list.cdr() // Skip 'db-delete symbol
        if (args == ListObject.NIL) {
            throw IllegalArgumentException("db-delete requires a table name")
        }

        val tableNameAtom = args.car().asAtom()
            ?: throw IllegalArgumentException("db-delete: first argument must be a table name symbol")
        val tableName = tableNameAtom.toString()

        var whereCondition: LispObject? = null
        var deleteAll = false
        var callbackExpr: LispObject? = null

        var current: ListObject = args.cdr()
        while (current != ListObject.NIL) {
            val item = current.car()
            val keyword = item.asKeyword()?.name()

            if (keyword != null) {
                current = current.cdr()
                if (current == ListObject.NIL) break
                val value = current.car()

                when (keyword) {
                    "where" -> whereCondition = value
                    "all" -> {
                        // Check if value is #t
                        deleteAll = value is BooleanObject && value.truthiness()
                    }
                }
                current = current.cdr()
            } else {
                callbackExpr = item
                current = current.cdr()
            }
        }

        if (whereCondition == null && !deleteAll) {
            throw IllegalArgumentException("db-delete: #:where is required (use #:all #t to delete all rows)")
        }
        if (callbackExpr == null) {
            throw IllegalArgumentException("db-delete: callback function is required")
        }

        val id = deleteCallId++
        deleteCallData[id] = DeleteCallData(tableName, whereCondition, deleteAll)

        // Return: (__db-delete-impl <id> <callback-expr>)
        return ListObject(
            env.atomOf("__db-delete-impl"),
            ListObject(
                IntObject(id),
                ListObject(callbackExpr, ListObject.NIL)
            )
        )
    }

    /**
     * Execute db-delete at runtime.
     */
    private fun executeDeleteImpl(params: Array<out LispObject>): LispObject {
        println("[DatabaseExtensions] executeDeleteImpl called with params: ${params.toList()}")

        if (params.size < 2) {
            throw IllegalArgumentException("__db-delete-impl requires id and callback")
        }

        val id = params[0].asInt()?.value()
            ?: throw IllegalArgumentException("__db-delete-impl: first argument must be an integer id")

        val callData = deleteCallData[id]
            ?: throw IllegalArgumentException("__db-delete-impl: invalid call id $id")

        val callback = params[1].asFunction()?.function()
            ?: throw IllegalArgumentException("__db-delete-impl: second argument must be a callback function")

        val handler = getCurrentHandler()
        val table = handler.schemaRegistry.getTable(callData.tableName)
            ?: throw IllegalArgumentException("db-delete: table not found: ${callData.tableName}")

        // Build WHERE clause
        var whereClause: String? = null
        if (callData.whereCondition != null) {
            val (sql, _) = handler.queryBuilder.buildCount(callData.tableName, callData.whereCondition)
            whereClause = if (sql.contains("WHERE")) sql.substringAfter("WHERE ") else null
        }

        println("[DatabaseExtensions] executeDeleteImpl: table=${table.sqlName}, whereClause=$whereClause, deleteAll=${callData.deleteAll}")

        handler.connection.executeDelete(table.sqlName, whereClause, null) { result, error ->
            println("[DatabaseExtensions] executeDeleteImpl callback: result=$result, error=$error")
            val errorObj = if (error != null) StringObject(error) else BooleanObject.FALSE
            callback.evaluate(arrayOf(result, errorObj))
        }

        return VoidObject.VOID
    }

    /**
     * Execute a transaction.
     *
     * Format: (db-transaction (lambda (tx) ...) callback)
     *
     * Inside the transaction lambda, use tx-* functions:
     * - (tx-insert tx table #:values p-map) -> returns ID
     * - (tx-update tx table #:set p-map #:where condition) -> returns count
     * - (tx-delete tx table #:where condition) -> returns count
     * - (tx-query tx table #:where condition ...) -> returns rows
     * - (tx-query-single tx table #:where condition ...) -> returns row or #f
     */
    private fun executeTransaction(params: Array<out LispObject>): LispObject {
        if (params.size < 2) {
            throw IllegalArgumentException("db-transaction requires a transaction function and callback")
        }

        val txFunction = params[0].asFunction()?.function()
            ?: throw IllegalArgumentException("db-transaction: first argument must be a function")

        val callback = params[1].asFunction()?.function()
            ?: throw IllegalArgumentException("db-transaction: second argument must be a callback function")

        val handler = getCurrentHandler()

        // Create a transaction context object that will be passed to the transaction function
        val txContextObj = TransactionContextObject(handler)

        // Register tx-* functions temporarily
        registerTransactionFunctions(txContextObj)

        handler.connection.executeTransaction(
            transactionBlock = { txContext ->
                txContextObj.context = txContext
                try {
                    println("[DatabaseExtensions] executeTransaction: calling txFunction")
                    val txResult = txFunction.evaluate(arrayOf(txContextObj))
                    println("[DatabaseExtensions] executeTransaction: txFunction returned $txResult")
                    txResult
                } catch (e: Exception) {
                    println("[DatabaseExtensions] ERROR: executeTransaction: txFunction threw exception - ${e.message}")
                    txContext.setError(e.message ?: "Transaction error")
                    BooleanObject.FALSE
                }
            },
            callback = { result, error ->
                println("[DatabaseExtensions] executeTransaction callback: result=$result, error=$error")
                val errorObj = if (error != null) StringObject(error) else BooleanObject.FALSE
                println("[DatabaseExtensions] executeTransaction: about to call user callback")
                callback.evaluate(arrayOf(result, errorObj))
                println("[DatabaseExtensions] executeTransaction: user callback completed")
            }
        )

        return VoidObject.VOID
    }

    /**
     * Register tx-* functions for use within transactions.
     */
    private fun registerTransactionFunctions(txContextObj: TransactionContextObject) {
        val handler = txContextObj.handler

        // tx-insert
        env.registerFunction("tx-insert") { params ->
            val ctx = txContextObj.context
                ?: throw IllegalStateException("tx-insert can only be used inside db-transaction")

            if (params.size < 2) {
                throw IllegalArgumentException("tx-insert requires (tx table #:values p-map)")
            }

            // First param is tx context (ignored, we use our reference)
            val tableName = extractTableName(params[1], "tx-insert")

            val table = handler.schemaRegistry.getTable(tableName)
                ?: throw IllegalArgumentException("tx-insert: table not found: $tableName")

            var values: PMapObject? = null
            var i = 2
            while (i < params.size) {
                val keyword = params[i].asKeyword()?.name()
                if (keyword == "values" && i + 1 < params.size) {
                    val v = params[i + 1]
                    if (v is PMapObject) {
                        values = v
                    }
                    i += 2
                } else {
                    i++
                }
            }

            if (values == null) {
                throw IllegalArgumentException("tx-insert: #:values p-map required")
            }

            val valuesMap = pMapToMap(values, table)
            val id = ctx.insert(table.sqlName, valuesMap)
            IntObject(id.toInt())
        }

        // tx-update
        env.registerFunction("tx-update") { params ->
            val ctx = txContextObj.context
                ?: throw IllegalStateException("tx-update can only be used inside db-transaction")

            if (params.size < 2) {
                throw IllegalArgumentException("tx-update requires (tx table #:set p-map #:where condition)")
            }

            val tableName = extractTableName(params[1], "tx-update")

            val table = handler.schemaRegistry.getTable(tableName)
                ?: throw IllegalArgumentException("tx-update: table not found: $tableName")

            var setValues: PMapObject? = null
            var whereCondition: LispObject? = null
            var i = 2
            while (i < params.size) {
                val keyword = params[i].asKeyword()?.name()
                if (keyword != null && i + 1 < params.size) {
                    when (keyword) {
                        "set" -> {
                            val v = params[i + 1]
                            if (v is PMapObject) setValues = v
                        }
                        "where" -> whereCondition = params[i + 1]
                    }
                    i += 2
                } else {
                    i++
                }
            }

            if (setValues == null) {
                throw IllegalArgumentException("tx-update: #:set p-map required")
            }

            val valuesMap = pMapToMap(setValues, table)
            var whereClause: String? = null
            var whereArgs: Array<String>? = null

            if (whereCondition != null) {
                val (sql, paramNames) = handler.queryBuilder.buildCount(tableName, whereCondition)
                whereClause = if (sql.contains("WHERE")) sql.substringAfter("WHERE ") else null
                whereArgs = null // No parameter support in tx-* for simplicity
            }

            val count = ctx.update(table.sqlName, valuesMap, whereClause, whereArgs)
            IntObject(count)
        }

        // tx-delete
        env.registerFunction("tx-delete") { params ->
            val ctx = txContextObj.context
                ?: throw IllegalStateException("tx-delete can only be used inside db-transaction")

            if (params.size < 2) {
                throw IllegalArgumentException("tx-delete requires (tx table #:where condition)")
            }

            val tableName = extractTableName(params[1], "tx-delete")

            val table = handler.schemaRegistry.getTable(tableName)
                ?: throw IllegalArgumentException("tx-delete: table not found: $tableName")

            var whereCondition: LispObject? = null
            var deleteAll = false
            var i = 2
            while (i < params.size) {
                val keyword = params[i].asKeyword()?.name()
                if (keyword != null && i + 1 < params.size) {
                    when (keyword) {
                        "where" -> whereCondition = params[i + 1]
                        "all" -> {
                            val v = params[i + 1]
                            deleteAll = v is BooleanObject && v.truthiness()
                        }
                    }
                    i += 2
                } else {
                    i++
                }
            }

            if (whereCondition == null && !deleteAll) {
                throw IllegalArgumentException("tx-delete: #:where required (use #:all #t to delete all)")
            }

            var whereClause: String? = null
            var whereArgs: Array<String>? = null

            if (whereCondition != null) {
                val (sql, _) = handler.queryBuilder.buildCount(tableName, whereCondition)
                whereClause = if (sql.contains("WHERE")) sql.substringAfter("WHERE ") else null
            }

            val count = ctx.delete(table.sqlName, whereClause, whereArgs)
            IntObject(count)
        }

        // tx-query
        env.registerFunction("tx-query") { params ->
            val ctx = txContextObj.context
                ?: throw IllegalStateException("tx-query can only be used inside db-transaction")

            if (params.size < 2) {
                throw IllegalArgumentException("tx-query requires (tx table ...)")
            }

            val tableName = extractTableName(params[1], "tx-query")

            val table = handler.schemaRegistry.getTable(tableName)
                ?: throw IllegalArgumentException("tx-query: table not found: $tableName")

            var columns: List<String>? = null
            var whereCondition: LispObject? = null
            var orderBy: MutableList<Pair<String, String>>? = null
            var limit: Int? = null

            var i = 2
            while (i < params.size) {
                val keyword = params[i].asKeyword()?.name()
                if (keyword != null && i + 1 < params.size) {
                    when (keyword) {
                        "columns" -> {
                            val colList = params[i + 1].asList()
                            columns = colList?.let { listToArray(it).map { c -> c.asAtom()?.toString() ?: c.toString() } }
                        }
                        "where" -> whereCondition = params[i + 1]
                        "order-by" -> orderBy = parseOrderBy(params[i + 1])
                        "limit" -> limit = params[i + 1].asInt()?.value()
                    }
                    i += 2
                } else {
                    i++
                }
            }

            val queryResult = handler.queryBuilder.buildSelect(tableName, columns, whereCondition, orderBy, limit)
            ctx.query(queryResult.sql, emptyArray(), queryResult.columnDefs)
        }

        // tx-query-single
        env.registerFunction("tx-query-single") { params ->
            val ctx = txContextObj.context
                ?: throw IllegalStateException("tx-query-single can only be used inside db-transaction")

            if (params.size < 2) {
                throw IllegalArgumentException("tx-query-single requires (tx table ...)")
            }

            val tableName = extractTableName(params[1], "tx-query-single")

            val table = handler.schemaRegistry.getTable(tableName)
                ?: throw IllegalArgumentException("tx-query-single: table not found: $tableName")

            var whereCondition: LispObject? = null

            var i = 2
            while (i < params.size) {
                val keyword = params[i].asKeyword()?.name()
                if (keyword != null && i + 1 < params.size) {
                    when (keyword) {
                        "where" -> whereCondition = params[i + 1]
                    }
                    i += 2
                } else {
                    i++
                }
            }

            val queryResult = handler.queryBuilder.buildSelect(tableName, null, whereCondition, null, 1)
            ctx.querySingle(queryResult.sql, emptyArray(), queryResult.columnDefs)
        }
    }

    /**
     * Execute manual migration.
     *
     * Format: (db-migrate version "SQL1" "SQL2" ... callback)
     */
    private fun executeMigration(params: Array<out LispObject>): LispObject {
        if (params.size < 2) {
            throw IllegalArgumentException("db-migrate requires a version number and at least one SQL statement")
        }

        val version = params[0].asInt()?.value()
            ?: throw IllegalArgumentException("db-migrate: first argument must be a version number")

        val statements = mutableListOf<String>()
        var callback: net.sourceforge.kleinlisp.Function? = null

        for (i in 1 until params.size) {
            val param = params[i]
            when {
                param.asString() != null -> statements.add(param.asString().value())
                param.asFunction() != null -> callback = param.asFunction().function()
            }
        }

        if (statements.isEmpty()) {
            throw IllegalArgumentException("db-migrate: at least one SQL statement required")
        }
        if (callback == null) {
            throw IllegalArgumentException("db-migrate: callback function required")
        }

        val finalCallback = callback
        val connection = getCurrentConnection()
        connection.executeMigration(version, statements) { result, error ->
            val errorObj = if (error != null) StringObject(error) else BooleanObject.FALSE
            finalCallback.evaluate(arrayOf(result, errorObj))
        }

        return VoidObject.VOID
    }

    /**
     * Transaction context wrapper object for passing to Scheme functions.
     */
    private class TransactionContextObject(
        val handler: DatabaseHandler
    ) : LispObject {
        var context: TransactionContext? = null

        override fun asObject(): Any = this
        override fun truthiness(): Boolean = true
        override fun <T> accept(visitor: net.sourceforge.kleinlisp.LispVisitor<T>): T? = null
        override fun error(): Boolean = false
        override fun toString(): String = "#<transaction-context>"
    }

    /**
     * Execute db-insert.
     *
     * Format: (db-insert table #:values p-map-or-list callback)
     *
     * - Single row: #:values is a p-map, callback receives (id error)
     * - Batch: #:values is a list of p-maps, callback receives (ids error)
     */
    private fun executeInsert(params: Array<out LispObject>): LispObject {
        if (params.isEmpty()) {
            throw IllegalArgumentException("db-insert requires a table name")
        }

        val tableName = extractTableName(params[0], "db-insert")

        val handler = getCurrentHandler()
        val table = handler.schemaRegistry.getTable(tableName)
            ?: throw IllegalArgumentException("db-insert: table not found: $tableName")

        var values: LispObject? = null
        var callback: net.sourceforge.kleinlisp.Function? = null

        var i = 1
        while (i < params.size) {
            val keyword = params[i].asKeyword()?.name()
            if (keyword != null && i + 1 < params.size) {
                when (keyword) {
                    "values" -> values = params[i + 1]
                }
                i += 2
            } else if (params[i].asFunction() != null) {
                callback = params[i].asFunction().function()
                i++
            } else {
                i++
            }
        }

        if (values == null) {
            throw IllegalArgumentException("db-insert: #:values is required")
        }
        if (callback == null) {
            throw IllegalArgumentException("db-insert: callback function is required")
        }

        val finalCallback = callback

        // Check if it's a batch insert (list of p-maps) or single insert (p-map)
        if (values is PMapObject) {
            // Single insert
            val valuesMap = pMapToMap(values, table)
            handler.connection.executeInsert(table.sqlName, valuesMap) { result, error ->
                val errorObj = if (error != null) StringObject(error) else BooleanObject.FALSE
                finalCallback.evaluate(arrayOf(result, errorObj))
            }
        } else if (values.asList() != null && values.asList() != ListObject.NIL) {
            // Batch insert
            val valuesList = mutableListOf<Map<String, Any?>>()
            var current = values.asList()
            while (current != null && current != ListObject.NIL) {
                val item = current.car()
                if (item is PMapObject) {
                    valuesList.add(pMapToMap(item, table))
                } else {
                    throw IllegalArgumentException("db-insert: batch values must be a list of p-maps")
                }
                val cdr = current.cdr()
                current = if (cdr is ListObject) cdr else null
            }

            handler.connection.executeBatchInsert(table.sqlName, valuesList) { result, error ->
                val errorObj = if (error != null) StringObject(error) else BooleanObject.FALSE
                finalCallback.evaluate(arrayOf(result, errorObj))
            }
        } else {
            throw IllegalArgumentException("db-insert: #:values must be a p-map or list of p-maps")
        }

        return VoidObject.VOID
    }

    /**
     * Convert a PMapObject to Map<String, Any?> for database operations.
     */
    private fun pMapToMap(pmap: PMapObject, table: TableDefinition): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        // Get the underlying PersistentMap and iterate over entries
        val map = pmap.map

        for (entry in map.entries) {
            val key = entry.key
            val value = entry.value

            // Get column name from keyword
            val columnName = when {
                key is KeywordObject -> key.name()
                key.asKeyword() != null -> key.asKeyword().name()
                else -> continue
            }

            // Find the column definition
            val column = table.getColumn(columnName)
            val sqlColumnName = column?.sqlName ?: columnName.replace('-', '_')

            // Convert value to appropriate type
            val convertedValue: Any? = when {
                value.asAtom()?.toString() == "null" -> null
                value.asString() != null -> value.asString().value()
                value.asInt() != null -> value.asInt().value()
                value.asDouble() != null -> value.asDouble().value()
                value is BooleanObject -> if (value.truthiness()) 1 else 0
                else -> value.toString()
            }

            result[sqlColumnName] = convertedValue
        }

        return result
    }
}
