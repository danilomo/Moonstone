package net.sourceforge.moonstone.persistence.db

import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.objects.BooleanObject

/**
 * Definition of a database column.
 *
 * @property name Column name
 * @property type Column data type
 * @property isPrimaryKey Whether this is the primary key
 * @property isNotNull Whether NULL values are disallowed
 * @property isUnique Whether values must be unique
 * @property defaultValue Default value (LispObject or null)
 * @property defaultNow Whether default is current timestamp
 * @property size Max size hint (documentation only for SQLite)
 * @property references Foreign key reference to another table
 */
data class ColumnDefinition(
    val name: String,
    val type: ColumnType,
    val isPrimaryKey: Boolean = false,
    val isNotNull: Boolean = false,
    val isUnique: Boolean = false,
    val defaultValue: LispObject? = null,
    val defaultNow: Boolean = false,
    val size: Int? = null,
    val references: String? = null
) {
    /**
     * SQL-safe column name (hyphens converted to underscores).
     */
    val sqlName: String = name.replace('-', '_')

    /**
     * Generate SQL column definition for CREATE TABLE.
     */
    fun toSql(): String {
        val parts = mutableListOf<String>()

        // Column name and type (use SQL-safe name)
        parts.add(sqlName)
        parts.add(type.sqlType)

        // Primary key (unless SERIAL which includes it)
        if (isPrimaryKey && type != ColumnType.SERIAL) {
            parts.add("PRIMARY KEY")
        }

        // NOT NULL constraint
        if (isNotNull) {
            parts.add("NOT NULL")
        }

        // UNIQUE constraint
        if (isUnique) {
            parts.add("UNIQUE")
        }

        // DEFAULT value
        if (defaultNow) {
            // For timestamp columns, we'll handle 'now' default in application logic
            // SQLite doesn't have a direct NOW() equivalent for milliseconds
        } else if (defaultValue != null) {
            val sqlValue = lispObjectToSqlDefault(defaultValue)
            parts.add("DEFAULT $sqlValue")
        }

        // Foreign key reference (added as table constraint separately)

        return parts.joinToString(" ")
    }

    /**
     * Generate foreign key constraint SQL if this column references another table.
     */
    fun toForeignKeySql(): String? {
        if (references == null) return null
        val refTable = references.replace('-', '_')
        return "FOREIGN KEY ($sqlName) REFERENCES $refTable(id)"
    }

    private fun lispObjectToSqlDefault(obj: LispObject): String {
        return when {
            obj.asString() != null -> "'${obj.asString().value().replace("'", "''")}'"
            obj.asInt() != null -> obj.asInt().value().toString()
            obj.asDouble() != null -> obj.asDouble().value().toString()
            obj is BooleanObject -> if (obj.truthiness()) "1" else "0"
            else -> "NULL"
        }
    }
}
