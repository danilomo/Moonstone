package net.sourceforge.moonstone.persistence

import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.objects.*
import net.sourceforge.moonstone.persistence.db.ColumnDefinition

/**
 * Common implementation of ResultMapper shared across platforms.
 *
 * Handles the mapping logic from SQL column names to Scheme keyword keys,
 * delegating platform-specific value extraction to the caller.
 */
class CommonResultMapper : ResultMapper {
    override fun mapRow(
        columnDefs: List<ColumnDefinition>,
        getColumnName: (Int) -> String,
        getColumnValue: (Int, ColumnDefinition?) -> LispObject,
        columnCount: Int
    ): PMapObject {
        var pmap = PMapObject.EMPTY

        for (i in 0 until columnCount) {
            val sqlColumnName = getColumnName(i)

            // Find column definition by SQL name to get Scheme name and type
            val colDef = columnDefs.find { it.sqlName == sqlColumnName }

            // Use Scheme name (with hyphens) for keyword, fall back to SQL name
            val schemeName = colDef?.name ?: sqlColumnName.replace('_', '-')
            val key: LispObject = KeywordObject(schemeName)

            // Get the value from platform-specific implementation
            val value: LispObject = getColumnValue(i, colDef)

            pmap = pmap.assoc(key, value)
        }

        return pmap
    }
}
