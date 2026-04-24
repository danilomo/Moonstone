package net.sourceforge.moonstone.persistence.db

/**
 * Supported column types for db-table definitions.
 */
@Suppress("StringLiteralDuplication") // SQL type strings are intentionally duplicated across enum values
enum class ColumnType(
    val sqlType: String,
) {
    /** Auto-incrementing integer primary key */
    SERIAL("INTEGER PRIMARY KEY AUTOINCREMENT"),

    /** Integer */
    INT("INTEGER"),

    /** Long integer (same as INT in SQLite) */
    LONG("INTEGER"),

    /** Text/string */
    STRING("TEXT"),

    /** Text (alias for STRING) */
    TEXT("TEXT"),

    /** Floating point number */
    REAL("REAL"),

    /** Boolean (stored as INTEGER 0/1) */
    BOOLEAN("INTEGER"),

    /** Unix timestamp in milliseconds */
    TIMESTAMP("INTEGER"),

    /** Binary data (Base64 encoded) */
    BLOB("BLOB"),
    ;

    companion object {
        /**
         * Parse a column type from a keyword symbol (e.g., "serial", "string").
         */
        fun fromKeyword(keyword: String): ColumnType? =
            when (keyword.lowercase()) {
                "serial" -> SERIAL
                "int" -> INT
                "long" -> LONG
                "string" -> STRING
                "text" -> TEXT
                "real" -> REAL
                "boolean" -> BOOLEAN
                "timestamp" -> TIMESTAMP
                "blob" -> BLOB
                else -> null
            }
    }
}
