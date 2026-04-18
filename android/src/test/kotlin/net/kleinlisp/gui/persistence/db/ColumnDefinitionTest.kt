package net.sourceforge.moonstone.persistence.db

import org.junit.Assert.*
import org.junit.Test

class ColumnDefinitionTest {

    @Test
    fun `serial column generates correct SQL`() {
        val column = ColumnDefinition(
            name = "id",
            type = ColumnType.SERIAL
        )
        assertEquals("id INTEGER PRIMARY KEY AUTOINCREMENT", column.toSql())
    }

    @Test
    fun `string column with not null generates correct SQL`() {
        val column = ColumnDefinition(
            name = "name",
            type = ColumnType.STRING,
            isNotNull = true
        )
        assertEquals("name TEXT NOT NULL", column.toSql())
    }

    @Test
    fun `column with unique constraint`() {
        val column = ColumnDefinition(
            name = "email",
            type = ColumnType.STRING,
            isUnique = true
        )
        assertEquals("email TEXT UNIQUE", column.toSql())
    }

    @Test
    fun `boolean column with default true`() {
        val column = ColumnDefinition(
            name = "active",
            type = ColumnType.BOOLEAN,
            defaultValue = net.sourceforge.kleinlisp.objects.BooleanObject.TRUE
        )
        assertTrue(column.toSql().contains("DEFAULT 1"))
    }

    @Test
    fun `boolean column with default false`() {
        val column = ColumnDefinition(
            name = "deleted",
            type = ColumnType.BOOLEAN,
            defaultValue = net.sourceforge.kleinlisp.objects.BooleanObject.FALSE
        )
        assertTrue(column.toSql().contains("DEFAULT 0"))
    }

    @Test
    fun `timestamp column with default now does not generate SQL default`() {
        // defaultNow is handled at application level, not in SQL
        val column = ColumnDefinition(
            name = "created-at",
            type = ColumnType.TIMESTAMP,
            defaultNow = true
        )
        // Should not contain DEFAULT since it's handled at insert time
        assertFalse(column.toSql().contains("DEFAULT"))
        assertTrue(column.defaultNow)
    }

    @Test
    fun `foreign key generates correct SQL`() {
        val column = ColumnDefinition(
            name = "user-id",
            type = ColumnType.LONG,
            references = "users"
        )
        val fkSql = column.toForeignKeySql()
        assertNotNull(fkSql)
        assertEquals("FOREIGN KEY (user_id) REFERENCES users(id)", fkSql)
    }

    @Test
    fun `hyphenated name converts to underscore for SQL`() {
        val column = ColumnDefinition(
            name = "created-at",
            type = ColumnType.TIMESTAMP
        )
        assertEquals("created_at", column.sqlName)
    }

    @Test
    fun `integer column with default value`() {
        val column = ColumnDefinition(
            name = "count",
            type = ColumnType.INT,
            defaultValue = net.sourceforge.kleinlisp.objects.IntObject(0)
        )
        assertTrue(column.toSql().contains("DEFAULT 0"))
    }

    @Test
    fun `real column type`() {
        val column = ColumnDefinition(
            name = "price",
            type = ColumnType.REAL
        )
        assertEquals("price REAL", column.toSql())
    }

    @Test
    fun `text column type`() {
        val column = ColumnDefinition(
            name = "content",
            type = ColumnType.TEXT
        )
        assertEquals("content TEXT", column.toSql())
    }

    @Test
    fun `blob column type`() {
        val column = ColumnDefinition(
            name = "data",
            type = ColumnType.BLOB
        )
        assertEquals("data BLOB", column.toSql())
    }

    @Test
    fun `multiple constraints combined`() {
        val column = ColumnDefinition(
            name = "email",
            type = ColumnType.STRING,
            isNotNull = true,
            isUnique = true
        )
        val sql = column.toSql()
        assertTrue(sql.contains("NOT NULL"))
        assertTrue(sql.contains("UNIQUE"))
    }
}
