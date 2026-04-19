package net.sourceforge.moonstone.persistence.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for QueryBuilder.
 *
 * Note: Tests that require LispObject creation are handled in the integration
 * test suite (samples/orm-test-suite) since LispObjects require a LispEnvironment.
 */
class QueryBuilderTest {
    private lateinit var registry: SchemaRegistry
    private lateinit var builder: QueryBuilder

    @Before
    fun setUp() {
        registry = SchemaRegistry()

        // Register test tables
        val users =
            TableDefinition(
                name = "users",
                columns =
                    listOf(
                        ColumnDefinition("id", ColumnType.SERIAL),
                        ColumnDefinition("username", ColumnType.STRING, isNotNull = true),
                        ColumnDefinition("email", ColumnType.STRING),
                        ColumnDefinition("age", ColumnType.INT),
                        ColumnDefinition("is-active", ColumnType.BOOLEAN),
                        ColumnDefinition("balance", ColumnType.REAL),
                    ),
            )
        val posts =
            TableDefinition(
                name = "posts",
                columns =
                    listOf(
                        ColumnDefinition("id", ColumnType.SERIAL),
                        ColumnDefinition("user-id", ColumnType.LONG, references = "users"),
                        ColumnDefinition("title", ColumnType.STRING, isNotNull = true),
                        ColumnDefinition("content", ColumnType.TEXT),
                        ColumnDefinition("view-count", ColumnType.INT),
                    ),
            )

        registry.registerTable(users)
        registry.registerTable(posts)

        builder = QueryBuilder(registry)
    }

    @Test
    fun `buildSelect generates simple SELECT all`() {
        val result = builder.buildSelect("users")

        assertEquals("SELECT * FROM users", result.sql)
        assertTrue(result.parameterNames.isEmpty())
        assertEquals(6, result.columnDefs.size)
    }

    @Test
    fun `buildSelect with specific columns`() {
        val result =
            builder.buildSelect(
                tableName = "users",
                columns = listOf("id", "username", "email"),
            )

        assertEquals("SELECT id, username, email FROM users", result.sql)
        assertEquals(3, result.columnDefs.size)
    }

    @Test
    fun `buildSelect with order by ASC`() {
        val result =
            builder.buildSelect(
                tableName = "users",
                orderBy = listOf(Pair("username", "ASC")),
            )

        assertEquals("SELECT * FROM users ORDER BY username ASC", result.sql)
    }

    @Test
    fun `buildSelect with order by DESC`() {
        val result =
            builder.buildSelect(
                tableName = "users",
                orderBy = listOf(Pair("age", "DESC")),
            )

        assertEquals("SELECT * FROM users ORDER BY age DESC", result.sql)
    }

    @Test
    fun `buildSelect with multiple order by`() {
        val result =
            builder.buildSelect(
                tableName = "users",
                orderBy =
                    listOf(
                        Pair("is-active", "DESC"),
                        Pair("username", "ASC"),
                    ),
            )

        assertEquals("SELECT * FROM users ORDER BY is_active DESC, username ASC", result.sql)
    }

    @Test
    fun `buildSelect with limit`() {
        val result =
            builder.buildSelect(
                tableName = "users",
                limit = 10,
            )

        assertEquals("SELECT * FROM users LIMIT 10", result.sql)
    }

    @Test
    fun `buildSelect with columns and limit`() {
        val result =
            builder.buildSelect(
                tableName = "users",
                columns = listOf("id", "username"),
                limit = 5,
            )

        assertEquals("SELECT id, username FROM users LIMIT 5", result.sql)
    }

    @Test
    fun `buildSelect converts hyphenated column names to underscores`() {
        val result =
            builder.buildSelect(
                tableName = "users",
                columns = listOf("is-active"),
            )

        assertTrue(result.sql.contains("is_active"))
    }

    @Test
    fun `buildSelect with columns order by and limit`() {
        val result =
            builder.buildSelect(
                tableName = "users",
                columns = listOf("id", "username", "age"),
                orderBy = listOf(Pair("age", "DESC")),
                limit = 20,
            )

        assertEquals("SELECT id, username, age FROM users ORDER BY age DESC LIMIT 20", result.sql)
    }

    @Test
    fun `buildCount generates COUNT query`() {
        val (sql, params) = builder.buildCount("users")

        assertEquals("SELECT COUNT(*) FROM users", sql)
        assertTrue(params.isEmpty())
    }

    @Test
    fun `buildSelect throws for unknown table`() {
        try {
            builder.buildSelect("nonexistent")
            fail("Should throw exception")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Table not found") == true)
        }
    }

    @Test
    fun `buildCount throws for unknown table`() {
        try {
            builder.buildCount("nonexistent")
            fail("Should throw exception")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Table not found") == true)
        }
    }

    @Test
    fun `buildSelect returns correct column definitions`() {
        val result =
            builder.buildSelect(
                tableName = "users",
                columns = listOf("id", "username", "age"),
            )

        assertEquals(3, result.columnDefs.size)
        assertEquals("id", result.columnDefs[0].name)
        assertEquals("username", result.columnDefs[1].name)
        assertEquals("age", result.columnDefs[2].name)
    }

    @Test
    fun `buildSelect with all columns returns all column defs`() {
        val result = builder.buildSelect("users")

        assertEquals(6, result.columnDefs.size)
    }

    @Test
    fun `QueryResult sql is correct format`() {
        val result =
            builder.buildSelect(
                tableName = "posts",
                columns = listOf("id", "title"),
                orderBy = listOf(Pair("id", "DESC")),
                limit = 5,
            )

        assertTrue(result.sql.startsWith("SELECT"))
        assertTrue(result.sql.contains("FROM posts"))
        assertTrue(result.sql.contains("ORDER BY"))
        assertTrue(result.sql.contains("LIMIT"))
    }

    @Test
    fun `buildSelect handles table with hyphenated name`() {
        val table =
            TableDefinition(
                name = "user-profiles",
                columns =
                    listOf(
                        ColumnDefinition("id", ColumnType.SERIAL),
                        ColumnDefinition("bio", ColumnType.TEXT),
                    ),
            )
        registry.registerTable(table)

        val result = builder.buildSelect("user-profiles")

        assertEquals("SELECT * FROM user_profiles", result.sql)
    }

    @Test
    fun `buildCount handles table with hyphenated name`() {
        val table =
            TableDefinition(
                name = "user-settings",
                columns =
                    listOf(
                        ColumnDefinition("id", ColumnType.SERIAL),
                    ),
            )
        registry.registerTable(table)

        val (sql, _) = builder.buildCount("user-settings")

        assertEquals("SELECT COUNT(*) FROM user_settings", sql)
    }
}
