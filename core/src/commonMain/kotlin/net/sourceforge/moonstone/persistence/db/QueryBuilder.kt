package net.sourceforge.moonstone.persistence.db

import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.objects.BooleanObject
import net.sourceforge.kleinlisp.objects.ListObject

/**
 * Builds SQL queries from Scheme query definitions.
 *
 * Handles:
 * - SELECT with columns
 * - WHERE conditions with various operators
 * - ORDER BY with ASC/DESC
 * - LIMIT
 * - Parameter placeholders (?param)
 */
class QueryBuilder(
    private val schemaRegistry: SchemaRegistry,
) {
    /**
     * Result of building a query.
     *
     * @property sql The generated SQL string with ? placeholders
     * @property parameterNames Ordered list of parameter names (without ? prefix)
     * @property columnDefs Column definitions for result mapping
     */
    data class QueryResult(
        val sql: String,
        val parameterNames: List<String>,
        val columnDefs: List<ColumnDefinition>,
    )

    /**
     * Build a SELECT query.
     *
     * @param tableName Main table name (Scheme name)
     * @param columns List of column names to select (null for all)
     * @param joins List of JOIN definitions (or null)
     * @param whereCondition WHERE condition AST (or null)
     * @param orderBy List of (column, direction) pairs
     * @param limit Maximum rows to return (or null)
     */
    @Suppress("LongParameterList", "LongMethod")
    fun buildSelect(
        tableName: String,
        columns: List<String>? = null,
        joins: List<JoinDefinition>? = null,
        whereCondition: LispObject? = null,
        orderBy: List<Pair<String, String>>? = null,
        limit: Int? = null,
    ): QueryResult {
        val table = getTableOrThrow(tableName)
        val parameterNames = mutableListOf<String>()
        val allTables = collectAllTables(table, joins)

        val (columnsSql, columnDefs) = buildColumnsClause(columns, table, allTables, joins)
        val fromSql = buildFromClause(table, joins, allTables, parameterNames)
        val whereSql = buildWhereClause(whereCondition, table, allTables, parameterNames)
        val orderBySql = buildOrderByClause(orderBy)
        val limitSql = buildLimitClause(limit)

        val sql = "SELECT $columnsSql FROM $fromSql$whereSql$orderBySql$limitSql"
        return QueryResult(sql, parameterNames, columnDefs)
    }

    private fun getTableOrThrow(tableName: String): TableDefinition =
        schemaRegistry.getTable(tableName)
            ?: throw IllegalArgumentException("Table not found: $tableName")

    private fun collectAllTables(
        mainTable: TableDefinition,
        joins: List<JoinDefinition>?,
    ): List<TableDefinition> {
        val allTables = mutableListOf(mainTable)
        joins?.forEach { join ->
            schemaRegistry.getTable(join.tableName)?.let { allTables.add(it) }
        }
        return allTables
    }

    private fun buildColumnsClause(
        columns: List<String>?,
        table: TableDefinition,
        allTables: List<TableDefinition>,
        joins: List<JoinDefinition>?,
    ): Pair<String, List<ColumnDefinition>> =
        if (columns.isNullOrEmpty()) {
            val sql = if (joins.isNullOrEmpty()) "*" else "${table.sqlName}.*"
            Pair(sql, table.columns)
        } else {
            val columnDefs = resolveColumnDefinitions(columns, table, allTables)
            val sql = columns.joinToString(", ") { toSqlColumnName(it) }
            Pair(sql, columnDefs)
        }

    private fun resolveColumnDefinitions(
        columns: List<String>,
        table: TableDefinition,
        allTables: List<TableDefinition>,
    ): List<ColumnDefinition> =
        columns.mapNotNull { colName ->
            if (colName.contains('.')) {
                resolveQualifiedColumn(colName, allTables)
            } else {
                table.getColumn(colName) ?: table.getColumn(colName.replace('_', '-'))
            }
        }

    private fun resolveQualifiedColumn(
        qualifiedName: String,
        allTables: List<TableDefinition>,
    ): ColumnDefinition? {
        val parts = qualifiedName.split('.')
        val tblName = parts[0]
        val col = parts[1]
        val tbl = allTables.find { it.name == tblName || it.sqlName == tblName.replace('-', '_') }
        return tbl?.getColumn(col) ?: tbl?.getColumn(col.replace('_', '-'))
    }

    private fun buildFromClause(
        table: TableDefinition,
        joins: List<JoinDefinition>?,
        allTables: List<TableDefinition>,
        parameterNames: MutableList<String>,
    ): String {
        var fromSql = table.sqlName
        joins?.forEach { join ->
            fromSql = appendJoinClause(fromSql, join, table, allTables, parameterNames)
        }
        return fromSql
    }

    private fun appendJoinClause(
        fromSql: String,
        join: JoinDefinition,
        table: TableDefinition,
        allTables: List<TableDefinition>,
        parameterNames: MutableList<String>,
    ): String {
        val joinTable =
            schemaRegistry.getTable(join.tableName)
                ?: throw IllegalArgumentException("Join table not found: ${join.tableName}")
        val joinType = join.joinType.uppercase()
        val (onSql, onParams) = buildCondition(join.onCondition, table, allTables)
        parameterNames.addAll(onParams)
        return "$fromSql $joinType JOIN ${joinTable.sqlName} ON $onSql"
    }

    private fun buildWhereClause(
        whereCondition: LispObject?,
        table: TableDefinition,
        allTables: List<TableDefinition>,
        parameterNames: MutableList<String>,
    ): String =
        if (whereCondition != null) {
            val (sql, params) = buildCondition(whereCondition, table, allTables)
            parameterNames.addAll(params)
            " WHERE $sql"
        } else {
            ""
        }

    private fun buildOrderByClause(orderBy: List<Pair<String, String>>?): String =
        if (orderBy != null && orderBy.isNotEmpty()) {
            val parts = orderBy.map { (col, dir) -> "${toSqlColumnName(col)} ${dir.uppercase()}" }
            " ORDER BY ${parts.joinToString(", ")}"
        } else {
            ""
        }

    private fun buildLimitClause(limit: Int?): String = if (limit != null) " LIMIT $limit" else ""

    /**
     * Overload for backward compatibility.
     */
    fun buildSelect(
        tableName: String,
        columns: List<String>? = null,
        whereCondition: LispObject? = null,
        orderBy: List<Pair<String, String>>? = null,
        limit: Int? = null,
    ): QueryResult = buildSelect(tableName, columns, null, whereCondition, orderBy, limit)

    /**
     * Build a COUNT query.
     */
    fun buildCount(
        tableName: String,
        whereCondition: LispObject? = null,
    ): Pair<String, List<String>> {
        val table =
            schemaRegistry.getTable(tableName)
                ?: throw IllegalArgumentException("Table not found: $tableName")

        val parameterNames = mutableListOf<String>()

        val whereSql =
            if (whereCondition != null) {
                val (sql, params) = buildCondition(whereCondition, table)
                parameterNames.addAll(params)
                " WHERE $sql"
            } else {
                ""
            }

        val sql = "SELECT COUNT(*) FROM ${table.sqlName}$whereSql"
        return Pair(sql, parameterNames)
    }

    /**
     * Build a WHERE condition from a Scheme AST.
     *
     * Returns (sql, parameterNames)
     */
    @Suppress("CyclomaticComplexMethod")
    private fun buildCondition(
        condition: LispObject,
        table: TableDefinition,
        allTables: List<TableDefinition>? = null,
    ): Pair<String, List<String>> {
        val list =
            requireNotNull(condition.asList()) {
                "Condition must be a list, got: $condition"
            }

        require(list != ListObject.NIL) { "Condition cannot be empty" }

        val elements = listToArray(list)
        require(elements.isNotEmpty()) { "Condition cannot be empty" }

        val operator =
            requireNotNull(elements[0].asAtom()?.toString()) {
                "Condition must start with an operator"
            }

        val tables = allTables ?: listOf(table)

        return when (operator) {
            // Comparison operators
            "=" -> buildComparison(elements, "=", table, tables)
            "<>" -> buildComparison(elements, "<>", table, tables)
            "!=" -> buildComparison(elements, "<>", table, tables)
            "<" -> buildComparison(elements, "<", table, tables)
            ">" -> buildComparison(elements, ">", table, tables)
            "<=" -> buildComparison(elements, "<=", table, tables)
            ">=" -> buildComparison(elements, ">=", table, tables)

            // Logical operators
            "and" -> buildLogical(elements.drop(1), "AND", table, tables)
            "or" -> buildLogical(elements.drop(1), "OR", table, tables)
            "not" -> buildNot(elements, table, tables)

            // Special operators
            "like" -> buildLike(elements, table, tables)
            "in" -> buildIn(elements, table, tables)
            "is-null" -> buildIsNull(elements, false)
            "is-not-null" -> buildIsNull(elements, true)
            "between" -> buildBetween(elements, table, tables)

            else -> throw IllegalArgumentException("Unknown operator: $operator")
        }
    }

    private fun buildComparison(
        elements: List<LispObject>,
        sqlOp: String,
        table: TableDefinition,
        allTables: List<TableDefinition>,
    ): Pair<String, List<String>> {
        require(elements.size == 3) { "$sqlOp requires exactly 2 arguments" }

        val column = toSqlColumnName(getColumnName(elements[1]))
        val (valueSql, params) = buildValue(elements[2], table, allTables)

        return Pair("$column $sqlOp $valueSql", params)
    }

    private fun buildLogical(
        conditions: List<LispObject>,
        sqlOp: String,
        table: TableDefinition,
        allTables: List<TableDefinition>,
    ): Pair<String, List<String>> {
        require(conditions.isNotEmpty()) { "$sqlOp requires at least one condition" }

        val parts = mutableListOf<String>()
        val params = mutableListOf<String>()

        for (cond in conditions) {
            val (sql, condParams) = buildCondition(cond, table, allTables)
            parts.add("($sql)")
            params.addAll(condParams)
        }

        return Pair(parts.joinToString(" $sqlOp "), params)
    }

    private fun buildNot(
        elements: List<LispObject>,
        table: TableDefinition,
        allTables: List<TableDefinition>,
    ): Pair<String, List<String>> {
        require(elements.size == 2) { "not requires exactly 1 argument" }

        val (sql, params) = buildCondition(elements[1], table, allTables)
        return Pair("NOT ($sql)", params)
    }

    private fun buildLike(
        elements: List<LispObject>,
        table: TableDefinition,
        allTables: List<TableDefinition>,
    ): Pair<String, List<String>> {
        require(elements.size == 3) { "like requires exactly 2 arguments" }

        val column = toSqlColumnName(getColumnName(elements[1]))
        val (valueSql, params) = buildValue(elements[2], table, allTables)

        return Pair("$column LIKE $valueSql", params)
    }

    private fun buildIn(
        elements: List<LispObject>,
        table: TableDefinition,
        allTables: List<TableDefinition>,
    ): Pair<String, List<String>> {
        require(elements.size == 3) { "in requires exactly 2 arguments" }

        val column = toSqlColumnName(getColumnName(elements[1]))
        val valuesArg = elements[2]

        // Check if it's a parameter reference
        val paramName = getParameterName(valuesArg)
        if (paramName != null) {
            // Parameter will be expanded at runtime
            return Pair("$column IN (?)", listOf(paramName))
        }

        // It's a literal list
        val valuesList =
            requireNotNull(valuesArg.asList()) {
                "in values must be a list or parameter"
            }

        val values = listToArray(valuesList)
        val params = mutableListOf<String>()
        val placeholders =
            values.map { value ->
                val (sql, valueParams) = buildValue(value, table, allTables)
                params.addAll(valueParams)
                sql
            }

        return Pair("$column IN (${placeholders.joinToString(", ")})", params)
    }

    private fun buildIsNull(
        elements: List<LispObject>,
        isNotNull: Boolean,
    ): Pair<String, List<String>> {
        require(elements.size == 2) { "is-null/is-not-null requires exactly 1 argument" }

        val column = toSqlColumnName(getColumnName(elements[1]))
        val nullCheck = if (isNotNull) "IS NOT NULL" else "IS NULL"

        return Pair("$column $nullCheck", emptyList())
    }

    private fun buildBetween(
        elements: List<LispObject>,
        table: TableDefinition,
        allTables: List<TableDefinition>,
    ): Pair<String, List<String>> {
        require(elements.size == 4) { "between requires exactly 3 arguments" }

        val column = toSqlColumnName(getColumnName(elements[1]))
        val (lowSql, lowParams) = buildValue(elements[2], table, allTables)
        val (highSql, highParams) = buildValue(elements[3], table, allTables)

        return Pair("$column BETWEEN $lowSql AND $highSql", lowParams + highParams)
    }

    /**
     * Build a value expression (literal or parameter reference).
     */
    @Suppress("UNUSED_PARAMETER", "CyclomaticComplexMethod")
    private fun buildValue(
        value: LispObject,
        table: TableDefinition,
        allTables: List<TableDefinition>? = null,
    ): Pair<String, List<String>> {
        // Check for parameter reference (?param)
        val paramName = getParameterName(value)
        if (paramName != null) {
            return Pair("?", listOf(paramName))
        }

        // Literal values - embed directly in SQL (these come from Scheme scripts, not user input)
        return when {
            value.asString() != null -> {
                // Escape single quotes in the string for SQL safety
                val escaped = value.asString().value().replace("'", "''")
                Pair("'$escaped'", emptyList())
            }
            value.asInt() != null -> Pair(value.asInt().value().toString(), emptyList())
            value.asDouble() != null -> Pair(value.asDouble().value().toString(), emptyList())
            value is BooleanObject -> Pair(if (value.truthiness()) "1" else "0", emptyList())
            value.asAtom() != null -> {
                val atomValue = value.asAtom().toString()
                when (atomValue) {
                    "null" -> Pair("NULL", emptyList())
                    "true" -> Pair("1", emptyList())
                    "false" -> Pair("0", emptyList())
                    else -> {
                        // Treat as column reference
                        Pair(toSqlColumnName(atomValue), emptyList())
                    }
                }
            }
            else -> throw IllegalArgumentException("Unsupported value type: $value")
        }
    }

    /**
     * Get parameter name from a ?param reference.
     */
    private fun getParameterName(value: LispObject): String? {
        val atom = value.asAtom()?.toString() ?: return null
        if (atom.startsWith("?")) {
            return atom.substring(1)
        }
        return null
    }

    /**
     * Get column name from an identifier.
     */
    private fun getColumnName(value: LispObject): String =
        value.asAtom()?.toString()
            ?: throw IllegalArgumentException("Expected column name, got: $value")

    /**
     * Convert Scheme column name to SQL column name.
     */
    private fun toSqlColumnName(name: String): String {
        // Handle qualified names (table.column)
        if (name.contains('.')) {
            val parts = name.split('.')
            return parts.joinToString(".") { it.replace('-', '_') }
        }
        return name.replace('-', '_')
    }

    /**
     * Convert a ListObject to array.
     */
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
}
