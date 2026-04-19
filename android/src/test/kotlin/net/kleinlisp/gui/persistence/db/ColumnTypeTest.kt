package net.sourceforge.moonstone.persistence.db

import org.junit.Assert.assertEquals
import org.junit.Test

class ColumnTypeTest {
    @Test
    fun `SERIAL maps to INTEGER PRIMARY KEY AUTOINCREMENT`() {
        assertEquals("INTEGER PRIMARY KEY AUTOINCREMENT", ColumnType.SERIAL.sqlType)
    }

    @Test
    fun `INT maps to INTEGER`() {
        assertEquals("INTEGER", ColumnType.INT.sqlType)
    }

    @Test
    fun `LONG maps to INTEGER`() {
        assertEquals("INTEGER", ColumnType.LONG.sqlType)
    }

    @Test
    fun `STRING maps to TEXT`() {
        assertEquals("TEXT", ColumnType.STRING.sqlType)
    }

    @Test
    fun `TEXT maps to TEXT`() {
        assertEquals("TEXT", ColumnType.TEXT.sqlType)
    }

    @Test
    fun `REAL maps to REAL`() {
        assertEquals("REAL", ColumnType.REAL.sqlType)
    }

    @Test
    fun `BOOLEAN maps to INTEGER`() {
        assertEquals("INTEGER", ColumnType.BOOLEAN.sqlType)
    }

    @Test
    fun `TIMESTAMP maps to INTEGER`() {
        assertEquals("INTEGER", ColumnType.TIMESTAMP.sqlType)
    }

    @Test
    fun `BLOB maps to BLOB`() {
        assertEquals("BLOB", ColumnType.BLOB.sqlType)
    }

    @Test
    fun `all column types are covered`() {
        val types = ColumnType.values()
        assertEquals(9, types.size)
    }
}
