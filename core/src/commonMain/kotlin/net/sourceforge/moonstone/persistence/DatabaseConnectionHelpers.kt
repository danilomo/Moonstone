package net.sourceforge.moonstone.persistence

/**
 * Common helpers for database connection implementations.
 */
object DatabaseConnectionHelpers {
    /**
     * Categorize database errors for user-friendly messages.
     */
    fun categorizeError(e: Exception): String {
        val message = e.message ?: return "Unknown database error"

        return when {
            message.contains("UNIQUE constraint failed", ignoreCase = true) ->
                "Constraint violation: UNIQUE"
            message.contains("NOT NULL constraint failed", ignoreCase = true) ->
                "Constraint violation: NOT NULL"
            message.contains("FOREIGN KEY constraint failed", ignoreCase = true) ->
                "Constraint violation: FOREIGN KEY"
            message.contains("no such table", ignoreCase = true) ->
                extractTableName(message)
            message.contains("no such column", ignoreCase = true) ->
                extractColumnName(message)
            else -> "SQL error: $message"
        }
    }

    private fun extractTableName(message: String): String {
        val tableName = Regex("no such table: (\\w+)").find(message)?.groupValues?.get(1)
        return "Table not found: ${tableName ?: "unknown"}"
    }

    private fun extractColumnName(message: String): String {
        val colName = Regex("no such column: (\\w+)").find(message)?.groupValues?.get(1)
        return "Column not found: ${colName ?: "unknown"}"
    }
}
