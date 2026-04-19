package net.sourceforge.moonstone.persistence.db

/**
 * Registry that tracks all table definitions for an application.
 *
 * Tables are registered when db-table is called during script evaluation.
 * The registry is then used to create/migrate the database schema on first access.
 */
class SchemaRegistry {
    private val tables = mutableMapOf<String, TableDefinition>()

    /**
     * Register a table definition.
     * If a table with the same name already exists, it will be replaced.
     */
    fun registerTable(table: TableDefinition) {
        tables[table.name] = table
    }

    /**
     * Get a table definition by name.
     */
    fun getTable(name: String): TableDefinition? = tables[name]

    /**
     * Get all registered table definitions.
     */
    fun getAllTables(): List<TableDefinition> = tables.values.toList()

    /**
     * Get all table names.
     */
    fun getTableNames(): Set<String> = tables.keys.toSet()

    /**
     * Check if a table is registered.
     */
    fun hasTable(name: String): Boolean = tables.containsKey(name)

    /**
     * Get the number of registered tables.
     */
    fun tableCount(): Int = tables.size

    /**
     * Clear all registered tables.
     */
    fun clear() {
        tables.clear()
    }

    /**
     * Validate that all foreign key references point to registered tables.
     * @return List of error messages, empty if valid
     */
    fun validateReferences(): List<String> {
        val errors = mutableListOf<String>()
        for (table in tables.values) {
            for (column in table.foreignKeyColumns) {
                val refTable = column.references
                if (refTable != null && !tables.containsKey(refTable)) {
                    errors.add("Table '${table.name}' column '${column.name}' references unknown table '$refTable'")
                }
            }
        }
        return errors
    }

    /**
     * Get tables in dependency order (tables with no foreign keys first).
     * This is useful for creating tables in the correct order.
     */
    fun getTablesInDependencyOrder(): List<TableDefinition> {
        val result = mutableListOf<TableDefinition>()
        val remaining = tables.values.toMutableList()
        val added = mutableSetOf<String>()

        // Keep adding tables whose dependencies are satisfied
        while (remaining.isNotEmpty()) {
            val next =
                remaining.find { table ->
                    table.foreignKeyColumns.all { col ->
                        col.references == null || added.contains(col.references)
                    }
                }

            if (next != null) {
                result.add(next)
                added.add(next.name)
                remaining.remove(next)
            } else {
                // Circular dependency or unresolvable - add remaining in any order
                result.addAll(remaining)
                break
            }
        }

        return result
    }
}
