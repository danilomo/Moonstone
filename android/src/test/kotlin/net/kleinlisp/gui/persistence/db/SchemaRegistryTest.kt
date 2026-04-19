package net.sourceforge.moonstone.persistence.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SchemaRegistryTest {
    private lateinit var registry: SchemaRegistry

    @Before
    fun setUp() {
        registry = SchemaRegistry()
    }

    @Test
    fun `registerTable adds table to registry`() {
        val table =
            TableDefinition(
                name = "users",
                columns = listOf(ColumnDefinition("id", ColumnType.SERIAL)),
            )

        registry.registerTable(table)

        assertNotNull(registry.getTable("users"))
    }

    @Test
    fun `getTable returns null for unknown table`() {
        assertNull(registry.getTable("nonexistent"))
    }

    @Test
    fun `getAllTables returns all registered tables`() {
        val users = TableDefinition("users", listOf(ColumnDefinition("id", ColumnType.SERIAL)))
        val posts = TableDefinition("posts", listOf(ColumnDefinition("id", ColumnType.SERIAL)))

        registry.registerTable(users)
        registry.registerTable(posts)

        val allTables = registry.getAllTables()
        assertEquals(2, allTables.size)
    }

    @Test
    fun `validateReferences returns empty for valid references`() {
        val users =
            TableDefinition(
                name = "users",
                columns = listOf(ColumnDefinition("id", ColumnType.SERIAL)),
            )
        val posts =
            TableDefinition(
                name = "posts",
                columns =
                    listOf(
                        ColumnDefinition("id", ColumnType.SERIAL),
                        ColumnDefinition("user-id", ColumnType.LONG, references = "users"),
                    ),
            )

        registry.registerTable(users)
        registry.registerTable(posts)

        val errors = registry.validateReferences()
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validateReferences returns error for invalid reference`() {
        val posts =
            TableDefinition(
                name = "posts",
                columns =
                    listOf(
                        ColumnDefinition("id", ColumnType.SERIAL),
                        ColumnDefinition("user-id", ColumnType.LONG, references = "users"),
                    ),
            )

        registry.registerTable(posts) // users table not registered

        val errors = registry.validateReferences()
        assertFalse(errors.isEmpty())
        assertTrue(errors[0].contains("users"))
    }

    @Test
    fun `getTablesInDependencyOrder returns tables in correct order`() {
        // posts depends on users, comments depends on posts and users
        val users =
            TableDefinition(
                name = "users",
                columns = listOf(ColumnDefinition("id", ColumnType.SERIAL)),
            )
        val posts =
            TableDefinition(
                name = "posts",
                columns =
                    listOf(
                        ColumnDefinition("id", ColumnType.SERIAL),
                        ColumnDefinition("user-id", ColumnType.LONG, references = "users"),
                    ),
            )
        val comments =
            TableDefinition(
                name = "comments",
                columns =
                    listOf(
                        ColumnDefinition("id", ColumnType.SERIAL),
                        ColumnDefinition("post-id", ColumnType.LONG, references = "posts"),
                        ColumnDefinition("user-id", ColumnType.LONG, references = "users"),
                    ),
            )

        // Register in wrong order
        registry.registerTable(comments)
        registry.registerTable(users)
        registry.registerTable(posts)

        val ordered = registry.getTablesInDependencyOrder()

        // users should come before posts, posts should come before comments
        val usersIndex = ordered.indexOfFirst { it.name == "users" }
        val postsIndex = ordered.indexOfFirst { it.name == "posts" }
        val commentsIndex = ordered.indexOfFirst { it.name == "comments" }

        assertTrue(usersIndex < postsIndex)
        assertTrue(postsIndex < commentsIndex)
    }

    @Test
    fun `registering table with same name replaces existing`() {
        val users1 =
            TableDefinition(
                name = "users",
                columns = listOf(ColumnDefinition("id", ColumnType.SERIAL)),
            )
        val users2 =
            TableDefinition(
                name = "users",
                columns =
                    listOf(
                        ColumnDefinition("id", ColumnType.SERIAL),
                        ColumnDefinition("name", ColumnType.STRING),
                    ),
            )

        registry.registerTable(users1)
        registry.registerTable(users2)

        val table = registry.getTable("users")
        assertEquals(2, table?.columns?.size)
    }

    @Test
    fun `tables with no dependencies returned in registration order`() {
        val tableA = TableDefinition("a", listOf(ColumnDefinition("id", ColumnType.SERIAL)))
        val tableB = TableDefinition("b", listOf(ColumnDefinition("id", ColumnType.SERIAL)))
        val tableC = TableDefinition("c", listOf(ColumnDefinition("id", ColumnType.SERIAL)))

        registry.registerTable(tableA)
        registry.registerTable(tableB)
        registry.registerTable(tableC)

        val ordered = registry.getTablesInDependencyOrder()
        assertEquals(3, ordered.size)
    }

    @Test
    fun `getTable handles hyphenated names`() {
        val table =
            TableDefinition(
                name = "user-profiles",
                columns = listOf(ColumnDefinition("id", ColumnType.SERIAL)),
            )

        registry.registerTable(table)

        assertNotNull(registry.getTable("user-profiles"))
    }
}
