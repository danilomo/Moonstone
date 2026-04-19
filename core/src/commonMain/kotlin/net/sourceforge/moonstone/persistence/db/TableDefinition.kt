package net.sourceforge.moonstone.persistence.db

import java.security.MessageDigest

/**
 * Definition of a database table.
 *
 * @property name Table name
 * @property columns List of column definitions
 */
data class TableDefinition(
    val name: String,
    val columns: List<ColumnDefinition>,
) {
    /**
     * SQL-safe table name (hyphens converted to underscores).
     */
    val sqlName: String = name.replace('-', '_')

    /**
     * Generate a hash of the schema for migration detection.
     */
    val schemaHash: String by lazy {
        val content =
            columns.joinToString("|") { col ->
                "${col.name}:${col.type}:${col.isPrimaryKey}:${col.isNotNull}:${col.isUnique}:${col.references}"
            }
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(content.toByteArray())
        bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Get the primary key column, if any.
     */
    val primaryKeyColumn: ColumnDefinition?
        get() = columns.find { it.isPrimaryKey || it.type == ColumnType.SERIAL }

    /**
     * Get all columns that have foreign key references.
     */
    val foreignKeyColumns: List<ColumnDefinition>
        get() = columns.filter { it.references != null }

    /**
     * Generate CREATE TABLE SQL statement.
     */
    fun toCreateTableSql(): String {
        val columnDefs = columns.joinToString(",\n    ") { it.toSql() }
        val foreignKeys = columns.mapNotNull { it.toForeignKeySql() }

        val allDefs =
            if (foreignKeys.isNotEmpty()) {
                "$columnDefs,\n    ${foreignKeys.joinToString(",\n    ")}"
            } else {
                columnDefs
            }

        return """
            CREATE TABLE IF NOT EXISTS $sqlName (
                $allDefs
            )
            """.trimIndent()
    }

    /**
     * Generate SQL to add a new column (for migrations).
     */
    fun toAddColumnSql(column: ColumnDefinition): String = "ALTER TABLE $sqlName ADD COLUMN ${column.toSql()}"

    /**
     * Get column names for SELECT queries (SQL names).
     */
    fun getColumnNames(): List<String> = columns.map { it.sqlName }

    /**
     * Find a column by Scheme name.
     */
    fun getColumn(name: String): ColumnDefinition? = columns.find { it.name == name }

    /**
     * Find a column by SQL name.
     */
    fun getColumnBySqlName(sqlName: String): ColumnDefinition? = columns.find { it.sqlName == sqlName }
}
