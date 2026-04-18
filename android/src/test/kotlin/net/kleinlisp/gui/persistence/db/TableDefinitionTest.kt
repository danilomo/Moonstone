package net.sourceforge.moonstone.persistence.db

import org.junit.Assert.*
import org.junit.Test

class TableDefinitionTest {

    @Test
    fun `simple table generates correct CREATE TABLE`() {
        val table = TableDefinition(
            name = "users",
            columns = listOf(
                ColumnDefinition("id", ColumnType.SERIAL),
                ColumnDefinition("name", ColumnType.STRING, isNotNull = true)
            )
        )

        val sql = table.toCreateTableSql()
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS users"))
        assertTrue(sql.contains("id INTEGER PRIMARY KEY AUTOINCREMENT"))
        assertTrue(sql.contains("name TEXT NOT NULL"))
    }

    @Test
    fun `table with foreign key generates FK constraint`() {
        val table = TableDefinition(
            name = "posts",
            columns = listOf(
                ColumnDefinition("id", ColumnType.SERIAL),
                ColumnDefinition("user-id", ColumnType.LONG, references = "users", isNotNull = true),
                ColumnDefinition("title", ColumnType.STRING, isNotNull = true)
            )
        )

        val sql = table.toCreateTableSql()
        assertTrue(sql.contains("FOREIGN KEY (user_id) REFERENCES users(id)"))
    }

    @Test
    fun `hyphenated table name converts to underscore`() {
        val table = TableDefinition(
            name = "user-profiles",
            columns = listOf(
                ColumnDefinition("id", ColumnType.SERIAL)
            )
        )

        assertEquals("user_profiles", table.sqlName)
        assertTrue(table.toCreateTableSql().contains("CREATE TABLE IF NOT EXISTS user_profiles"))
    }

    @Test
    fun `getColumn finds column by name`() {
        val table = TableDefinition(
            name = "users",
            columns = listOf(
                ColumnDefinition("id", ColumnType.SERIAL),
                ColumnDefinition("user-name", ColumnType.STRING)
            )
        )

        assertNotNull(table.getColumn("id"))
        assertNotNull(table.getColumn("user-name"))
        assertNull(table.getColumn("nonexistent"))
    }

    @Test
    fun `getColumnBySqlName finds column by SQL name`() {
        val table = TableDefinition(
            name = "users",
            columns = listOf(
                ColumnDefinition("id", ColumnType.SERIAL),
                ColumnDefinition("created-at", ColumnType.TIMESTAMP)
            )
        )

        assertNotNull(table.getColumnBySqlName("id"))
        assertNotNull(table.getColumnBySqlName("created_at"))
        assertNull(table.getColumnBySqlName("created-at")) // Hyphenated won't match SQL name
    }

    @Test
    fun `toAddColumnSql generates ALTER TABLE`() {
        val table = TableDefinition(
            name = "users",
            columns = listOf(
                ColumnDefinition("id", ColumnType.SERIAL)
            )
        )

        val newColumn = ColumnDefinition("email", ColumnType.STRING)
        val sql = table.toAddColumnSql(newColumn)

        assertEquals("ALTER TABLE users ADD COLUMN email TEXT", sql)
    }

    @Test
    fun `schema hash changes when columns change`() {
        val table1 = TableDefinition(
            name = "users",
            columns = listOf(
                ColumnDefinition("id", ColumnType.SERIAL),
                ColumnDefinition("name", ColumnType.STRING)
            )
        )

        val table2 = TableDefinition(
            name = "users",
            columns = listOf(
                ColumnDefinition("id", ColumnType.SERIAL),
                ColumnDefinition("name", ColumnType.STRING),
                ColumnDefinition("email", ColumnType.STRING)
            )
        )

        assertNotEquals(table1.schemaHash, table2.schemaHash)
    }

    @Test
    fun `same schema produces same hash`() {
        val table1 = TableDefinition(
            name = "users",
            columns = listOf(
                ColumnDefinition("id", ColumnType.SERIAL),
                ColumnDefinition("name", ColumnType.STRING)
            )
        )

        val table2 = TableDefinition(
            name = "users",
            columns = listOf(
                ColumnDefinition("id", ColumnType.SERIAL),
                ColumnDefinition("name", ColumnType.STRING)
            )
        )

        assertEquals(table1.schemaHash, table2.schemaHash)
    }

    @Test
    fun `multiple foreign keys handled correctly`() {
        val table = TableDefinition(
            name = "comments",
            columns = listOf(
                ColumnDefinition("id", ColumnType.SERIAL),
                ColumnDefinition("post-id", ColumnType.LONG, references = "posts", isNotNull = true),
                ColumnDefinition("user-id", ColumnType.LONG, references = "users", isNotNull = true),
                ColumnDefinition("content", ColumnType.TEXT)
            )
        )

        val sql = table.toCreateTableSql()
        assertTrue(sql.contains("FOREIGN KEY (post_id) REFERENCES posts(id)"))
        assertTrue(sql.contains("FOREIGN KEY (user_id) REFERENCES users(id)"))
    }
}
