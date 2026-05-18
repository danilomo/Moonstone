package net.sourceforge.moonstone.persistence

import net.sourceforge.kleinlisp.LispEnvironment
import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.objects.BooleanObject
import net.sourceforge.kleinlisp.objects.IntObject
import net.sourceforge.kleinlisp.objects.KeywordObject
import net.sourceforge.kleinlisp.objects.ListObject
import net.sourceforge.kleinlisp.objects.PMapObject
import net.sourceforge.kleinlisp.objects.StringObject
import net.sourceforge.kleinlisp.objects.VoidObject
import net.sourceforge.moonstone.persistence.DatabaseFactory
import net.sourceforge.moonstone.persistence.TransactionContext
import net.sourceforge.moonstone.persistence.db.ColumnDefinition
import net.sourceforge.moonstone.persistence.db.ColumnType
import net.sourceforge.moonstone.persistence.db.JoinDefinition
import net.sourceforge.moonstone.persistence.db.QueryBuilder
import net.sourceforge.moonstone.persistence.db.QueryDefinition
import net.sourceforge.moonstone.persistence.db.SchemaRegistry
import net.sourceforge.moonstone.persistence.db.TableDefinition
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
@Suppress("StringLiteralDuplication")
class DatabaseExtensions(
    private val env: LispEnvironment,
) {
    /**
     * Centralized error messages for consistent error reporting across database operations.
     */
    private object ErrorMessages {
        // Group A: Function validation (7 occurrences)
        fun callbackRequired(fnName: String) = "$fnName: callback function is required"

        // Group B: Table validation (8 occurrences)
        fun tableNotFound(
            operation: String,
            tableName: String,
        ) = "$operation: table not found: $tableName"

        // Group C: Table name requirements (7 occurrences)
        fun requiresTableName(fnName: String) = "$fnName requires a table name"

        // Group D: First argument validation (6 occurrences)
        fun firstArgTableNameSymbol(fnName: String) = "$fnName: first argument must be a table name symbol"

        // Group E: List requirements (6 occurrences)
        fun requiresList(context: String) = "$context requires a list"

        // Group F: Macro implementation (3 occurrences)
        fun invalidCallId(
            implName: String,
            id: Int,
        ) = "$implName: invalid call id $id"

        // Transaction function names
        const val TX_INSERT = "tx-insert"
        const val TX_UPDATE = "tx-update"
        const val TX_DELETE = "tx-delete"
        const val TX_QUERY = "tx-query"
        const val TX_QUERY_SINGLE = "tx-query-single"

        // Transaction error messages
        const val TX_INSERT_INSIDE_TRANSACTION = "tx-insert can only be used inside db-transaction"
        const val TX_UPDATE_INSIDE_TRANSACTION = "tx-update can only be used inside db-transaction"
        const val TX_DELETE_INSIDE_TRANSACTION = "tx-delete can only be used inside db-transaction"
        const val TX_QUERY_INSIDE_TRANSACTION = "tx-query can only be used inside db-transaction"
        const val TX_QUERY_SINGLE_INSIDE_TRANSACTION = "tx-query-single can only be used inside db-transaction"

        // Additional constants
        const val WHERE_CONDITION_REQUIRED = "#:where requires a condition expression"
        const val SET_CLAUSE_REQUIRED = "#:set is required for update operations"
        const val COLUMNS_LIST_REQUIRED = "#:columns requires a list of column names"
        const val PARAMS_LIST_REQUIRED = "#:params requires a list of parameter names"
        const val ORDER_BY_LIST_REQUIRED = "#:order-by requires a list"
    }

    /**
     * SQL-related constants.
     */
    private object SqlConstants {
        const val WHERE_KEYWORD = "WHERE"
        const val WHERE_SPACE = "WHERE "
    }

    /**
     * Extract WHERE clause from SQL string if present.
     */
    private fun extractWhereClause(sql: String): String? =
        if (sql.contains(SqlConstants.WHERE_KEYWORD)) {
            sql.substringAfter(SqlConstants.WHERE_SPACE)
        } else {
            null
        }

    /**
     * Register all database functions with the environment.
     */
    fun register() {
        env.set(env.atomOf("*db*"), BooleanObject.FALSE)
        registerBaseFunctions()
        registerTypeConversionFunctions()
        registerDbTableMacro()
        registerQueryMacros()
        registerManipulationFunctions()
    }

    private fun registerBaseFunctions() {
        env.registerFunction("make-db") { params ->
            require(params.isNotEmpty()) { "make-db requires a path argument" }
            val path =
                params[0].asString()?.value()
                    ?: throw IllegalArgumentException("make-db: path must be a string")
            createHandler(File(path))
        }

        env.registerFunction("db-close") { params ->
            require(params.isNotEmpty()) { "db-close requires a database handler argument" }
            val handler =
                params[0] as? DatabaseHandler
                    ?: throw IllegalArgumentException("db-close: argument must be a database handler")
            handler.close()
            VoidObject.VOID
        }

        env.registerFunction("db-null?") { params ->
            require(params.isNotEmpty()) { "db-null? requires 1 argument" }
            val value = params[0]
            val isNull = value.asAtom()?.toString() == "null"
            if (isNull) BooleanObject.TRUE else BooleanObject.FALSE
        }
    }

    private fun registerTypeConversionFunctions() {
        // (keyword->string #:weight) → "weight"
        env.registerFunction("keyword->string") { params ->
            require(params.isNotEmpty()) { "keyword->string requires 1 argument" }
            val kw =
                params[0].asKeyword()
                    ?: throw IllegalArgumentException(
                        "keyword->string: argument must be a keyword, got ${params[0]}",
                    )
            StringObject(kw.name())
        }

        // (string->keyword "weight") → #:weight
        env.registerFunction("string->keyword") { params ->
            require(params.isNotEmpty()) { "string->keyword requires 1 argument" }
            val str =
                params[0].asString()?.value()
                    ?: throw IllegalArgumentException(
                        "string->keyword: argument must be a string, got ${params[0]}",
                    )
            KeywordObject(str)
        }
    }

    private fun registerDbTableMacro() {
        // (db-table name (col1 #:type ...) (col2 #:type ...) ...)
        env.registerMacro("db-table") { list ->
            try {
                val args = list.cdr() // skip "db-table"
                val tableNameAtom =
                    args.car().asAtom()
                        ?: throw IllegalArgumentException(ErrorMessages.firstArgTableNameSymbol("db-table"))
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
        return checkNotNull(dbVar as? DatabaseHandler) {
            "No database set. Call (set! *db* (make-db \"path/to/db\")) first, " +
                "or ensure the GUI entry point has set up the database."
        }
    }

    /**
     * Get the schema registry from the current handler.
     */
    private fun getCurrentSchemaRegistry(): SchemaRegistry = getCurrentHandler().schemaRegistry

    /**
     * Get the database connection from the current handler.
     */
    private fun getCurrentConnection(): DatabaseConnection = getCurrentHandler().connection

    // ========== Query Function Registration ==========

    private fun registerQueryMacros() {
        // define-table - Define a table schema from a quoted list (plain function, not macro)
        // Format: (define-table '(name (col1 #:type ...) (col2 #:type ...) ...))
        env.registerFunction("define-table") { params ->
            require(params.isNotEmpty()) { "define-table requires a table definition list" }
            val tableList =
                params[0].asList()
                    ?: throw IllegalArgumentException(
                        "define-table: argument must be a quoted list like '(table-name (col #:type) ...)",
                    )
            require(tableList != ListObject.NIL) { "define-table: table definition list cannot be empty" }
            val tableNameAtom =
                tableList.car().asAtom()
                    ?: throw IllegalArgumentException(
                        "define-table: first element of the list must be the table name symbol",
                    )
            parseAndRegisterTableFromList(tableList)
            env.set(tableNameAtom, StringObject(tableNameAtom.toString()))
            VoidObject.VOID
        }

        // query-table - Execute a query immediately from a quoted options list (plain function, not macro)
        // Format: (query-table '(#:from table #:where condition ...) #:param val ... callback)
        env.registerFunction("query-table") { params ->
            executeAnonymousQuery(params, isSingle = false)
        }

        // query-table-single - Same as query-table but returns one row or #f
        env.registerFunction("query-table-single") { params ->
            executeAnonymousQuery(params, isSingle = true)
        }

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
        require(params.isNotEmpty()) { "db-execute requires a SQL string" }

        val sql =
            params[0].asString()?.value()
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
                        val paramList =
                            params[i + 1].asList()
                                ?: throw IllegalArgumentException(ErrorMessages.requiresList("db-execute: #:params"))
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

        require(callback != null) { ErrorMessages.callbackRequired("db-execute") }

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
        require(params.isNotEmpty()) { "db-execute-update requires a SQL string" }

        val sql =
            params[0].asString()?.value()
                ?: throw IllegalArgumentException("db-execute-update: first argument must be a SQL string")

        var sqlParams: List<String> = emptyList()
        var callback: net.sourceforge.kleinlisp.Function? = null

        var i = 1
        while (i < params.size) {
            val keyword = params[i].asKeyword()?.name()
            if (keyword != null && i + 1 < params.size) {
                when (keyword) {
                    "params" -> {
                        val paramList =
                            params[i + 1].asList()
                                ?: throw IllegalArgumentException(
                                    ErrorMessages.requiresList("db-execute-update: #:params"),
                                )
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

        require(callback != null) { ErrorMessages.callbackRequired("db-execute-update") }

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
    private fun parseQueryDefinitionFromList(
        list: ListObject,
        isSingle: Boolean,
    ): QueryDefinition {
        val params = listToArray(list)
        return parseQueryDefinition(params.toTypedArray(), isSingle)
    }

    /**
     * Holds parsed query definition parameters.
     */
    private data class ParsedQueryParams(
        var tableName: String? = null,
        var columns: List<String>? = null,
        val joins: MutableList<JoinDefinition> = mutableListOf(),
        var whereCondition: LispObject? = null,
        var orderBy: MutableList<Pair<String, String>>? = null,
        var limit: Int? = null,
        var limitParam: String? = null,
        var parameterNames: List<String> = emptyList(),
    )

    /**
     * Parse a query definition from db-query or db-query-single arguments.
     */
    private fun parseQueryDefinition(
        params: Array<out LispObject>,
        isSingle: Boolean,
    ): QueryDefinition {
        require(params.isNotEmpty()) { "db-query requires a function name" }

        val functionName =
            params[0].asAtom()?.toString()
                ?: throw IllegalArgumentException("db-query: first argument must be a function name symbol")

        val parsed = parseQueryKeywordArgs(params)

        require(parsed.tableName != null) { "db-query '$functionName': #:from is required" }

        val effectiveLimit = if (isSingle && parsed.limit == null && parsed.limitParam == null) 1 else parsed.limit

        return QueryDefinition(
            name = functionName,
            tableName = parsed.tableName!!,
            columns = parsed.columns,
            joins = if (parsed.joins.isEmpty()) null else parsed.joins,
            whereCondition = parsed.whereCondition,
            orderBy = parsed.orderBy,
            limit = effectiveLimit,
            limitParam = parsed.limitParam,
            parameterNames = parsed.parameterNames,
            isSingle = isSingle,
        )
    }

    /**
     * Parse keyword arguments for query definition, starting at the given index.
     */
    private fun parseQueryKeywordArgsFromIndex(
        params: Array<out LispObject>,
        startIndex: Int,
    ): ParsedQueryParams {
        val result = ParsedQueryParams()

        var i = startIndex
        while (i < params.size) {
            val keyword = params[i].asKeyword()?.name()
            if (keyword != null && i + 1 < params.size) {
                val value = params[i + 1]
                parseQueryKeyword(keyword, value, result)
                i += 2
            } else {
                i++
            }
        }

        return result
    }

    /**
     * Parse keyword arguments for query definition (skips index 0, which is the function name).
     */
    private fun parseQueryKeywordArgs(params: Array<out LispObject>): ParsedQueryParams =
        parseQueryKeywordArgsFromIndex(params, 1)

    /**
     * Parse a single query keyword argument.
     */
    @Suppress("CyclomaticComplexMethod")
    private fun parseQueryKeyword(
        keyword: String,
        value: LispObject,
        result: ParsedQueryParams,
    ) {
        when (keyword) {
            "from" -> {
                result.tableName = value.asAtom()?.toString()
                    ?: throw IllegalArgumentException("#:from requires a table name symbol")
            }
            "columns" -> {
                val colList = value.asList() ?: throw IllegalArgumentException(ErrorMessages.COLUMNS_LIST_REQUIRED)
                result.columns = listToArray(colList).map { it.asAtom()?.toString() ?: it.toString() }
            }
            "join" -> {
                val joinDef = parseJoin(value)
                if (joinDef != null) result.joins.add(joinDef)
            }
            "left-join" -> {
                val joinDef = parseJoin(value, "LEFT")
                if (joinDef != null) result.joins.add(joinDef)
            }
            "where" -> {
                result.whereCondition = value
            }
            "order-by" -> {
                result.orderBy = parseOrderBy(value)
            }
            "limit" -> {
                val atomStr = value.asAtom()?.toString()
                if (atomStr != null && atomStr.startsWith("?")) {
                    result.limitParam = atomStr.substring(1)
                } else {
                    result.limit = value.asInt()?.value()
                        ?: throw IllegalArgumentException("#:limit requires an integer or parameter (?name)")
                }
            }
            "params" -> {
                val paramList = value.asList() ?: throw IllegalArgumentException(ErrorMessages.PARAMS_LIST_REQUIRED)
                result.parameterNames = listToArray(paramList).map { it.asAtom()?.toString() ?: it.toString() }
            }
        }
    }

    /**
     * Parse a #:join value.
     * Format: (table-name #:on condition)
     */
    private fun parseJoin(
        value: LispObject,
        joinType: String = "INNER",
    ): JoinDefinition? {
        val list =
            value.asList()
                ?: throw IllegalArgumentException("#:join requires a list (table #:on condition)")

        val elements = listToArray(list)
        if (elements.isEmpty()) return null

        val joinTableName =
            elements[0].asAtom()?.toString()
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

        require(onCondition != null) { "#:join: #:on condition is required" }

        return JoinDefinition(
            tableName = joinTableName,
            joinType = joinType,
            onCondition = onCondition,
        )
    }

    /**
     * Parse #:order-by value.
     * Format: (col1 #:asc col2 #:desc) or (col1 col2) defaults to ASC
     */
    @Suppress("NestedBlockDepth")
    private fun parseOrderBy(value: LispObject): MutableList<Pair<String, String>> {
        val list =
            value.asList()
                ?: throw IllegalArgumentException(ErrorMessages.ORDER_BY_LIST_REQUIRED)

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
     * Holds parsed query execution parameters.
     */
    private data class ParsedQueryExecParams(
        val paramValues: Map<String, LispObject>,
        val callback: net.sourceforge.kleinlisp.Function,
    )

    /**
     * Execute a generated query function.
     */
    private fun executeGeneratedQuery(
        queryDef: QueryDefinition,
        params: Array<out LispObject>,
    ): LispObject {
        println("[DatabaseExtensions] executeGeneratedQuery: ${queryDef.name}, params=${params.toList()}")

        val handler = getCurrentHandler()
        val parsed = parseQueryExecutionParams(params, queryDef.name)
        val effectiveLimit = resolveQueryLimit(queryDef, parsed.paramValues)
        val queryResult = buildQueryFromDefinition(handler.queryBuilder, queryDef, effectiveLimit)
        val boundParams = bindQueryParameters(queryResult.parameterNames, parsed.paramValues, queryDef.name)

        executeQueryWithCallback(handler, queryDef, queryResult, boundParams, parsed.callback)

        return VoidObject.VOID
    }

    /**
     * Parse query execution parameters and callback.
     */
    private fun parseQueryExecutionParams(
        params: Array<out LispObject>,
        queryName: String,
    ): ParsedQueryExecParams {
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

        require(callback != null) {
            println("[DatabaseExtensions] ERROR: executeGeneratedQuery: no callback found!")
            ErrorMessages.callbackRequired(queryName)
        }

        return ParsedQueryExecParams(paramValues, callback)
    }

    /**
     * Resolve the effective query limit from definition or parameters.
     */
    private fun resolveQueryLimit(
        queryDef: QueryDefinition,
        paramValues: Map<String, LispObject>,
    ): Int? {
        if (queryDef.limitParam != null) {
            val limitValue =
                paramValues[queryDef.limitParam]
                    ?: throw IllegalArgumentException(
                        "${queryDef.name}: missing parameter #:${queryDef.limitParam}",
                    )
            return limitValue.asInt()?.value()
                ?: throw IllegalArgumentException("${queryDef.name}: #:${queryDef.limitParam} must be an integer")
        }
        return queryDef.limit
    }

    /**
     * Build query from definition with resolved limit.
     */
    private fun buildQueryFromDefinition(
        queryBuilder: QueryBuilder,
        queryDef: QueryDefinition,
        effectiveLimit: Int?,
    ) = queryBuilder.buildSelect(
        tableName = queryDef.tableName,
        columns = queryDef.columns,
        joins = queryDef.joins,
        whereCondition = queryDef.whereCondition,
        orderBy = queryDef.orderBy,
        limit = effectiveLimit,
    )

    /**
     * Bind query parameters to SQL string values.
     */
    private fun bindQueryParameters(
        parameterNames: List<String>,
        paramValues: Map<String, LispObject>,
        queryName: String,
    ): List<String> =
        parameterNames.map { paramName ->
            val value =
                paramValues[paramName]
                    ?: throw IllegalArgumentException("$queryName: missing parameter #:$paramName")
            lispObjectToSqlString(value)
        }

    /**
     * Execute query and invoke callback with result.
     */
    private fun executeQueryWithCallback(
        handler: DatabaseHandler,
        queryDef: QueryDefinition,
        queryResult: QueryBuilder.QueryResult,
        boundParams: List<String>,
        callback: net.sourceforge.kleinlisp.Function,
    ) {
        println("[DatabaseExtensions] executeGeneratedQuery: SQL=${queryResult.sql}, args=$boundParams")

        handler.connection.executeQuery(
            sql = queryResult.sql,
            args = boundParams.toTypedArray(),
            columnDefs = queryResult.columnDefs,
        ) { result, error ->
            println(
                "[DatabaseExtensions] executeGeneratedQuery callback: " +
                    "queryName=${queryDef.name}, result=$result, error=$error",
            )

            val resultToPass =
                if (queryDef.isSingle) {
                    if (result.asList()?.let { it != ListObject.NIL } == true) {
                        result.asList()!!.car()
                    } else {
                        BooleanObject.FALSE
                    }
                } else {
                    result
                }

            invokeQueryCallback(callback, queryDef.name, resultToPass, error)
        }
    }

    /**
     * Execute a query immediately from a quoted options list.
     *
     * Called by query-table and query-table-single.
     *
     * Format: (query-table '(#:from table #:where cond #:params (p1 p2)) #:p1 val1 callback)
     */
    private fun executeAnonymousQuery(
        params: Array<out LispObject>,
        isSingle: Boolean,
    ): LispObject {
        val fnName = if (isSingle) "query-table-single" else "query-table"
        require(params.isNotEmpty()) { "$fnName requires a query options list as first argument" }

        val queryListObj =
            params[0].asList()
                ?: throw IllegalArgumentException(
                    "$fnName: first argument must be a quoted list like '(#:from table ...)",
                )

        val queryListArr = listToArray(queryListObj).toTypedArray()
        val parsedDef = parseQueryKeywordArgsFromIndex(queryListArr, 0)
        require(parsedDef.tableName != null) { "$fnName: #:from is required in the options list" }

        val queryDef =
            QueryDefinition(
                name = fnName,
                tableName = parsedDef.tableName!!,
                columns = parsedDef.columns,
                joins = if (parsedDef.joins.isEmpty()) null else parsedDef.joins,
                whereCondition = parsedDef.whereCondition,
                orderBy = parsedDef.orderBy,
                limit = if (isSingle && parsedDef.limit == null && parsedDef.limitParam == null) 1 else parsedDef.limit,
                limitParam = parsedDef.limitParam,
                parameterNames = parsedDef.parameterNames,
                isSingle = isSingle,
            )

        val execParams = params.sliceArray(1 until params.size)
        val parsed = parseQueryExecutionParams(execParams, fnName)

        val handler = getCurrentHandler()
        val effectiveLimit = resolveQueryLimit(queryDef, parsed.paramValues)
        val queryResult = buildQueryFromDefinition(handler.queryBuilder, queryDef, effectiveLimit)
        val boundParams = bindQueryParameters(queryResult.parameterNames, parsed.paramValues, fnName)

        executeQueryWithCallback(handler, queryDef, queryResult, boundParams, parsed.callback)

        return VoidObject.VOID
    }

    // Counter for generating unique IDs for db-count calls
    private var countCallId = 0
    private val countCallData = mutableMapOf<Int, CountCallData>()

    private data class CountCallData(
        val tableName: String,
        val whereCondition: LispObject?,
        val paramValues: Map<String, String>,
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
            throw IllegalArgumentException(ErrorMessages.requiresTableName("db-count"))
        }

        // First arg is table name (must be a symbol at this point)
        val tableNameAtom =
            args.car().asAtom()
                ?: throw IllegalArgumentException(ErrorMessages.firstArgTableNameSymbol("db-count"))
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

        require(callbackExpr != null) { ErrorMessages.callbackRequired("db-count") }

        // Store the call data for runtime retrieval
        val id = countCallId++
        countCallData[id] = CountCallData(tableName, whereCondition, emptyMap())

        println(
            "[DatabaseExtensions] transformCountMacro stored " +
                "id=$id, tableName=$tableName, whereCondition=$whereCondition",
        )

        // Return: (__db-count-impl <id> <callback-expr>)
        // This will be evaluated at runtime, which will evaluate callback-expr
        return ListObject(
            env.atomOf("__db-count-impl"),
            ListObject(
                IntObject(id),
                ListObject(callbackExpr, ListObject.NIL),
            ),
        )
    }

    /**
     * Execute db-count at runtime.
     *
     * Called as: (__db-count-impl <id> <callback>)
     */
    private fun executeCountImpl(params: Array<out LispObject>): LispObject {
        println("[DatabaseExtensions] executeCountImpl called with params: ${params.toList()}")

        require(params.size >= 2) { "__db-count-impl requires id and callback" }

        val id =
            params[0].asInt()?.value()
                ?: throw IllegalArgumentException("__db-count-impl: first argument must be an integer id")

        val callData =
            countCallData[id]
                ?: throw IllegalArgumentException(ErrorMessages.invalidCallId("__db-count-impl", id))

        val callback =
            params[1].asFunction()?.function()
                ?: throw IllegalArgumentException("__db-count-impl: second argument must be a callback function")

        println(
            "[DatabaseExtensions] executeCountImpl: " +
                "id=$id, tableName=${callData.tableName}, whereCondition=${callData.whereCondition}",
        )

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
            whereClause = extractWhereClause(sql),
            whereArgs = null,
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
    private fun extractTableName(
        obj: LispObject,
        functionName: String,
    ): String {
        // Try string first (our db-table macro sets table names as StringObject)
        obj.asString()?.let { return it.value() }

        // Try atom (includes identifiers which have asAtom)
        obj.asAtom()?.let { return it.toString() }

        throw IllegalArgumentException(
            "$functionName: first argument must be a table name (symbol or string), got ${obj::class.simpleName}",
        )
    }

    /**
     * Convert a LispObject to a SQL string value.
     */
    private fun lispObjectToSqlString(obj: LispObject): String =
        when {
            obj.asString() != null -> obj.asString().value()
            obj.asInt() != null -> obj.asInt().value().toString()
            obj.asDouble() != null -> obj.asDouble().value().toString()
            obj is BooleanObject -> if (obj.truthiness()) "1" else "0"
            obj.asAtom()?.toString() == "null" -> "NULL"
            else -> obj.toString()
        }

    // ========== Table Definition Parsing ==========

    /**
     * Parse db-table arguments from a ListObject (macro version) and register the table.
     * Format: (name (col1 #:type ...) (col2 #:type ...) ...)
     */
    private fun parseAndRegisterTableFromList(list: ListObject) {
        require(list != ListObject.NIL) { ErrorMessages.requiresTableName("db-table") }

        val tableName =
            list.car().asAtom()?.toString()
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

        require(columns.isNotEmpty()) { "db-table '$tableName': at least one column is required" }

        val table = TableDefinition(tableName, columns)
        getCurrentSchemaRegistry().registerTable(table)
    }

    /**
     * Holds parsed column definition attributes.
     */
    private data class ParsedColumnAttrs(
        var type: ColumnType? = null,
        var isPrimaryKey: Boolean = false,
        var isNotNull: Boolean = false,
        var isUnique: Boolean = false,
        var defaultValue: LispObject? = null,
        var defaultNow: Boolean = false,
        var size: Int? = null,
        var references: String? = null,
    )

    private fun parseColumnDefinition(obj: LispObject): ColumnDefinition? {
        val list = obj.asList() ?: return null
        if (list == ListObject.NIL) return null

        val elements = listToArray(list)
        if (elements.isEmpty()) return null

        val columnName =
            elements[0].asAtom()?.toString()
                ?: throw IllegalArgumentException("Column definition must start with a column name")

        val attrs = parseColumnAttributes(elements)

        require(attrs.type != null) { "Column '$columnName': no type specified" }

        return ColumnDefinition(
            name = columnName,
            type = attrs.type!!,
            isPrimaryKey = attrs.isPrimaryKey,
            isNotNull = attrs.isNotNull,
            isUnique = attrs.isUnique,
            defaultValue = attrs.defaultValue,
            defaultNow = attrs.defaultNow,
            size = attrs.size,
            references = attrs.references,
        )
    }

    /**
     * Parse column attributes from keyword arguments.
     */
    private fun parseColumnAttributes(elements: List<LispObject>): ParsedColumnAttrs {
        val attrs = ParsedColumnAttrs()

        var i = 1
        while (i < elements.size) {
            val keyword = elements[i].asKeyword()?.name()
            i = parseColumnKeyword(keyword, elements, i, attrs)
        }

        return attrs
    }

    /**
     * Parse a single column keyword and return the next index.
     */
    @Suppress("CyclomaticComplexMethod")
    private fun parseColumnKeyword(
        keyword: String?,
        elements: List<LispObject>,
        currentIndex: Int,
        attrs: ParsedColumnAttrs,
    ): Int {
        var i = currentIndex
        when (keyword) {
            "serial" -> {
                attrs.type = ColumnType.SERIAL
                attrs.isPrimaryKey = true
            }
            "int" -> attrs.type = ColumnType.INT
            "long" -> attrs.type = ColumnType.LONG
            "string" -> attrs.type = ColumnType.STRING
            "text" -> attrs.type = ColumnType.TEXT
            "real" -> attrs.type = ColumnType.REAL
            "double" -> attrs.type = ColumnType.REAL
            "float" -> attrs.type = ColumnType.REAL
            "boolean" -> attrs.type = ColumnType.BOOLEAN
            "timestamp" -> attrs.type = ColumnType.TIMESTAMP
            "blob" -> attrs.type = ColumnType.BLOB
            "pk" -> attrs.isPrimaryKey = true
            "not-null" -> attrs.isNotNull = true
            "unique" -> attrs.isUnique = true
            "default" -> {
                i++
                if (i < elements.size) {
                    val defVal = elements[i]
                    val symbolValue = extractSymbolValue(defVal)
                    if (symbolValue == "now") {
                        attrs.defaultNow = true
                    } else {
                        attrs.defaultValue = defVal
                    }
                }
            }
            "size" -> {
                i++
                if (i < elements.size) {
                    attrs.size = elements[i].asInt()?.value()?.toInt()
                }
            }
            "references" -> {
                i++
                if (i < elements.size) {
                    attrs.references = elements[i].asAtom()?.toString()
                }
            }
        }
        return i + 1
    }

    private fun listToArray(list: ListObject): List<LispObject> {
        val result = mutableListOf<LispObject>()
        var current: ListObject? = list
        while (current != null && current != ListObject.NIL) {
            result.add(current.car())
            val cdr = current.cdr()
            current = cdr as? ListObject
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
        val whereCondition: LispObject?,
    )

    private data class DeleteCallData(
        val tableName: String,
        val whereCondition: LispObject?,
        val deleteAll: Boolean,
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
        require(args != ListObject.NIL) { ErrorMessages.requiresTableName("db-update") }

        val tableNameAtom =
            args.car().asAtom()
                ?: throw IllegalArgumentException(ErrorMessages.firstArgTableNameSymbol("db-update"))
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

        require(setExpr != null) { "db-update: #:set is required" }
        require(callbackExpr != null) { ErrorMessages.callbackRequired("db-update") }

        val id = updateCallId++
        updateCallData[id] = UpdateCallData(tableName, setExpr, whereCondition)

        // Return: (__db-update-impl <id> <set-expr> <callback-expr>)
        return ListObject(
            env.atomOf("__db-update-impl"),
            ListObject(
                IntObject(id),
                ListObject(
                    setExpr, // set expression needs to be evaluated at runtime
                    ListObject(callbackExpr, ListObject.NIL),
                ),
            ),
        )
    }

    /**
     * Execute db-update at runtime.
     */
    private fun executeUpdateImpl(params: Array<out LispObject>): LispObject {
        println("[DatabaseExtensions] executeUpdateImpl called with params: ${params.toList()}")

        require(params.size >= 3) { "__db-update-impl requires id, set-values, and callback" }

        val id =
            params[0].asInt()?.value()
                ?: throw IllegalArgumentException("__db-update-impl: first argument must be an integer id")

        val callData =
            updateCallData[id]
                ?: throw IllegalArgumentException(ErrorMessages.invalidCallId("__db-update-impl", id))

        val setValues =
            params[1] as? PMapObject
                ?: throw IllegalArgumentException("__db-update-impl: #:set must be a p-map")

        val callback =
            params[2].asFunction()?.function()
                ?: throw IllegalArgumentException("__db-update-impl: third argument must be a callback function")

        val handler = getCurrentHandler()
        val table = handler.getTableOrThrow(callData.tableName, "db-update")

        val values = pMapToMap(setValues, table)

        // Build WHERE clause
        var whereClause: String? = null
        if (callData.whereCondition != null) {
            val (sql, _) = handler.queryBuilder.buildCount(callData.tableName, callData.whereCondition)
            whereClause = extractWhereClause(sql)
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
        require(args != ListObject.NIL) { ErrorMessages.requiresTableName("db-delete") }

        val tableNameAtom =
            args.car().asAtom()
                ?: throw IllegalArgumentException(ErrorMessages.firstArgTableNameSymbol("db-delete"))
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

        require(whereCondition != null || deleteAll) {
            "db-delete: #:where is required (use #:all #t to delete all rows)"
        }
        require(callbackExpr != null) { ErrorMessages.callbackRequired("db-delete") }

        val id = deleteCallId++
        deleteCallData[id] = DeleteCallData(tableName, whereCondition, deleteAll)

        // Return: (__db-delete-impl <id> <callback-expr>)
        return ListObject(
            env.atomOf("__db-delete-impl"),
            ListObject(
                IntObject(id),
                ListObject(callbackExpr, ListObject.NIL),
            ),
        )
    }

    /**
     * Execute db-delete at runtime.
     */
    private fun executeDeleteImpl(params: Array<out LispObject>): LispObject {
        println("[DatabaseExtensions] executeDeleteImpl called with params: ${params.toList()}")

        require(params.size >= 2) { "__db-delete-impl requires id and callback" }

        val id =
            params[0].asInt()?.value()
                ?: throw IllegalArgumentException("__db-delete-impl: first argument must be an integer id")

        val callData =
            deleteCallData[id]
                ?: throw IllegalArgumentException(ErrorMessages.invalidCallId("__db-delete-impl", id))

        val callback =
            params[1].asFunction()?.function()
                ?: throw IllegalArgumentException("__db-delete-impl: second argument must be a callback function")

        val handler = getCurrentHandler()
        val table = handler.getTableOrThrow(callData.tableName, "db-delete")

        // Build WHERE clause
        var whereClause: String? = null
        if (callData.whereCondition != null) {
            val (sql, _) = handler.queryBuilder.buildCount(callData.tableName, callData.whereCondition)
            whereClause = extractWhereClause(sql)
        }

        println(
            "[DatabaseExtensions] executeDeleteImpl: " +
                "table=${table.sqlName}, whereClause=$whereClause, deleteAll=${callData.deleteAll}",
        )

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
        require(params.size >= 2) { "db-transaction requires a transaction function and callback" }

        val txFunction =
            params[0].asFunction()?.function()
                ?: throw IllegalArgumentException("db-transaction: first argument must be a function")

        val callback =
            params[1].asFunction()?.function()
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
            },
        )

        return VoidObject.VOID
    }

    /**
     * Register tx-* functions for use within transactions.
     */
    private fun registerTransactionFunctions(txContextObj: TransactionContextObject) {
        registerTxInsert(txContextObj)
        registerTxUpdate(txContextObj)
        registerTxDelete(txContextObj)
        registerTxQuery(txContextObj)
        registerTxQuerySingle(txContextObj)
    }

    private fun registerTxInsert(txContextObj: TransactionContextObject) {
        val handler = txContextObj.handler
        env.registerFunction(ErrorMessages.TX_INSERT) { params ->
            val ctx =
                checkNotNull(txContextObj.context) {
                    ErrorMessages.TX_INSERT_INSIDE_TRANSACTION
                }

            require(params.size >= 2) { "tx-insert requires (tx table #:values p-map)" }

            val tableName = extractTableName(params[1], ErrorMessages.TX_INSERT)
            val table = handler.getTableOrThrow(tableName, ErrorMessages.TX_INSERT)

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

            require(values != null) { "tx-insert: #:values p-map required" }

            val valuesMap = pMapToMap(values, table)
            val id = ctx.insert(table.sqlName, valuesMap)
            IntObject(id.toInt())
        }
    }

    @Suppress("CognitiveComplexMethod")
    private fun registerTxUpdate(txContextObj: TransactionContextObject) {
        val handler = txContextObj.handler
        env.registerFunction(ErrorMessages.TX_UPDATE) { params ->
            val ctx =
                checkNotNull(txContextObj.context) {
                    ErrorMessages.TX_UPDATE_INSIDE_TRANSACTION
                }

            require(params.size >= 2) { "tx-update requires (tx table #:set p-map #:where condition)" }

            val tableName = extractTableName(params[1], ErrorMessages.TX_UPDATE)
            val table = handler.getTableOrThrow(tableName, ErrorMessages.TX_UPDATE)

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

            require(setValues != null) { "tx-update: #:set p-map required" }

            val valuesMap = pMapToMap(setValues, table)
            var whereClause: String? = null
            var whereArgs: Array<String>? = null

            if (whereCondition != null) {
                val (sql, _) = handler.queryBuilder.buildCount(tableName, whereCondition)
                whereClause = extractWhereClause(sql)
                whereArgs = null
            }

            val count = ctx.update(table.sqlName, valuesMap, whereClause, whereArgs)
            IntObject(count)
        }
    }

    @Suppress("CognitiveComplexMethod")
    private fun registerTxDelete(txContextObj: TransactionContextObject) {
        val handler = txContextObj.handler
        env.registerFunction(ErrorMessages.TX_DELETE) { params ->
            val ctx =
                checkNotNull(txContextObj.context) {
                    ErrorMessages.TX_DELETE_INSIDE_TRANSACTION
                }

            require(params.size >= 2) { "tx-delete requires (tx table #:where condition)" }

            val tableName = extractTableName(params[1], ErrorMessages.TX_DELETE)
            val table = handler.getTableOrThrow(tableName, ErrorMessages.TX_DELETE)

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

            require(whereCondition != null || deleteAll) {
                "tx-delete: #:where required (use #:all #t to delete all)"
            }

            var whereClause: String? = null
            var whereArgs: Array<String>? = null

            if (whereCondition != null) {
                val (sql, _) = handler.queryBuilder.buildCount(tableName, whereCondition)
                whereClause = extractWhereClause(sql)
            }

            val count = ctx.delete(table.sqlName, whereClause, whereArgs)
            IntObject(count)
        }
    }

    private fun registerTxQuery(txContextObj: TransactionContextObject) {
        val handler = txContextObj.handler
        env.registerFunction(ErrorMessages.TX_QUERY) { params ->
            val ctx =
                checkNotNull(txContextObj.context) {
                    ErrorMessages.TX_QUERY_INSIDE_TRANSACTION
                }

            require(params.size >= 2) { "tx-query requires (tx table ...)" }

            val tableName = extractTableName(params[1], ErrorMessages.TX_QUERY)
            val table = handler.getTableOrThrow(tableName, ErrorMessages.TX_QUERY)

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
                            columns =
                                colList?.let { listToArray(it).map { c -> c.asAtom()?.toString() ?: c.toString() } }
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
    }

    private fun registerTxQuerySingle(txContextObj: TransactionContextObject) {
        val handler = txContextObj.handler
        env.registerFunction(ErrorMessages.TX_QUERY_SINGLE) { params ->
            val ctx =
                checkNotNull(txContextObj.context) {
                    ErrorMessages.TX_QUERY_SINGLE_INSIDE_TRANSACTION
                }

            require(params.size >= 2) { "tx-query-single requires (tx table ...)" }

            val tableName = extractTableName(params[1], ErrorMessages.TX_QUERY_SINGLE)
            val table = handler.getTableOrThrow(tableName, ErrorMessages.TX_QUERY_SINGLE)

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
        require(params.size >= 2) { "db-migrate requires a version number and at least one SQL statement" }

        val version =
            params[0].asInt()?.value()
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

        require(statements.isNotEmpty()) { "db-migrate: at least one SQL statement required" }
        require(callback != null) { "db-migrate: callback function required" }

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
        val handler: DatabaseHandler,
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
    @Suppress("CyclomaticComplexMethod", "CognitiveComplexMethod")
    private fun executeInsert(params: Array<out LispObject>): LispObject {
        require(params.isNotEmpty()) { ErrorMessages.requiresTableName("db-insert") }

        val tableName = extractTableName(params[0], "db-insert")

        val handler = getCurrentHandler()
        val table = handler.getTableOrThrow(tableName, "db-insert")

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

        require(values != null) { "db-insert: #:values is required" }
        require(callback != null) { ErrorMessages.callbackRequired("db-insert") }

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
                require(item is PMapObject) { "db-insert: batch values must be a list of p-maps" }
                valuesList.add(pMapToMap(item, table))
                val cdr = current.cdr()
                current = cdr as? ListObject
            }

            handler.connection.executeBatchInsert(table.sqlName, valuesList) { result, error ->
                val errorObj = if (error != null) StringObject(error) else BooleanObject.FALSE
                finalCallback.evaluate(arrayOf(result, errorObj))
            }
        } else {
            require(false) { "db-insert: #:values must be a p-map or list of p-maps" }
        }

        return VoidObject.VOID
    }

    /**
     * Convert a PMapObject to Map<String, Any?> for database operations.
     */
    @Suppress("CyclomaticComplexMethod")
    private fun pMapToMap(
        pmap: PMapObject,
        table: TableDefinition,
    ): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        // Get the underlying PersistentMap and iterate over entries
        val map = pmap.map

        for (entry in map.entries) {
            val key = entry.key
            val value = entry.value

            // Get column name from keyword
            val columnName =
                when {
                    key is KeywordObject -> key.name()
                    key.asKeyword() != null -> key.asKeyword().name()
                    else -> continue
                }

            // Find the column definition
            val column = table.getColumn(columnName)
            val sqlColumnName = column?.sqlName ?: columnName.replace('-', '_')

            // Convert value to appropriate type
            val convertedValue: Any? =
                when {
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

    /**
     * Invoke a query callback with result and error handling.
     * Centralizes duplicate callback invocation logic.
     */
    private fun invokeQueryCallback(
        callback: net.sourceforge.kleinlisp.Function,
        queryName: String,
        result: LispObject,
        error: String?,
    ) {
        val errorObj = if (error != null) StringObject(error) else BooleanObject.FALSE
        println("[DatabaseExtensions] DEBUG: about to call user callback for $queryName")
        try {
            callback.evaluate(arrayOf(result, errorObj))
            println("[DatabaseExtensions] DEBUG: user callback completed for $queryName")
        } catch (e: Exception) {
            println("[DatabaseExtensions] ERROR: callback failed for $queryName: ${e.message}")
            throw e
        }
    }

    /**
     * Get a table from the schema registry or throw an exception if not found.
     * This helper eliminates duplicate table lookup code throughout the file.
     */
    private fun DatabaseHandler.getTableOrThrow(
        tableName: String,
        operation: String,
    ): TableDefinition =
        schemaRegistry.getTable(tableName)
            ?: throw IllegalArgumentException(ErrorMessages.tableNotFound(operation, tableName))
}
