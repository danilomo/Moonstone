package net.sourceforge.moonstone.persistence.db

import net.sourceforge.kleinlisp.LispObject

/**
 * Definition of a JOIN clause.
 *
 * @property tableName Table to join (Scheme name)
 * @property joinType Type of join (INNER, LEFT, RIGHT)
 * @property onCondition ON condition AST
 */
data class JoinDefinition(
    val tableName: String,
    val joinType: String = "INNER",
    val onCondition: LispObject
)

/**
 * Definition of a database query created by db-query or db-query-single.
 *
 * @property name Function name for this query
 * @property tableName Main table name (Scheme name)
 * @property columns List of column names to select (null for all)
 * @property joins List of JOIN definitions
 * @property whereCondition WHERE condition AST (or null)
 * @property orderBy List of (column, direction) pairs
 * @property limit Maximum rows to return (or null for unlimited)
 * @property limitParam Parameter name for dynamic limit (e.g., "limit" for ?limit)
 * @property parameterNames Declared parameter names
 * @property isSingle Whether this returns a single row (db-query-single)
 */
data class QueryDefinition(
    val name: String,
    val tableName: String,
    val columns: List<String>? = null,
    val joins: List<JoinDefinition>? = null,
    val whereCondition: LispObject? = null,
    val orderBy: List<Pair<String, String>>? = null,
    val limit: Int? = null,
    val limitParam: String? = null,
    val parameterNames: List<String> = emptyList(),
    val isSingle: Boolean = false
)
