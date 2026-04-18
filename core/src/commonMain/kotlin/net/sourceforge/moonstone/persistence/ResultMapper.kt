package net.sourceforge.moonstone.persistence

import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.objects.PMapObject
import net.sourceforge.moonstone.persistence.db.ColumnDefinition

/**
 * Interface for mapping database result rows to PMapObjects.
 *
 * This abstraction allows platform-specific implementations to handle
 * their native result types (Cursor on Android, ResultSet on JDBC).
 */
interface ResultMapper {
    /**
     * Map a single result row to a PMapObject.
     *
     * @param columnDefs Column definitions with type information
     * @param getColumnName Function to get column name by index
     * @param getColumnValue Function to get column value by index as LispObject
     * @param columnCount Total number of columns
     * @return PMapObject with keyword keys and LispObject values
     */
    fun mapRow(
        columnDefs: List<ColumnDefinition>,
        getColumnName: (Int) -> String,
        getColumnValue: (Int, ColumnDefinition?) -> LispObject,
        columnCount: Int
    ): PMapObject
}
