package net.sourceforge.moonstone.persistence.db

import net.sourceforge.kleinlisp.objects.ListObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryDefinitionTest {
    @Test
    fun `QueryDefinition has correct defaults`() {
        val query =
            QueryDefinition(
                name = "test-query",
                tableName = "users",
            )

        assertEquals("test-query", query.name)
        assertEquals("users", query.tableName)
        assertNull(query.columns)
        assertNull(query.joins)
        assertNull(query.whereCondition)
        assertNull(query.orderBy)
        assertNull(query.limit)
        assertTrue(query.parameterNames.isEmpty())
        assertFalse(query.isSingle)
    }

    @Test
    fun `QueryDefinition with all properties`() {
        val whereCondition = ListObject.NIL
        val joins =
            listOf(
                JoinDefinition("posts", "INNER", ListObject.NIL),
            )

        val query =
            QueryDefinition(
                name = "complex-query",
                tableName = "users",
                columns = listOf("id", "name", "email"),
                joins = joins,
                whereCondition = whereCondition,
                orderBy = listOf(Pair("name", "ASC"), Pair("id", "DESC")),
                limit = 10,
                parameterNames = listOf("user-id", "status"),
                isSingle = true,
            )

        assertEquals("complex-query", query.name)
        assertEquals(3, query.columns?.size)
        assertEquals(1, query.joins?.size)
        assertEquals(10, query.limit)
        assertEquals(2, query.parameterNames.size)
        assertTrue(query.isSingle)
    }

    @Test
    fun `QueryDefinition is a data class with copy`() {
        val original =
            QueryDefinition(
                name = "original",
                tableName = "users",
            )

        val copy = original.copy(name = "copy", limit = 5)

        assertEquals("original", original.name)
        assertEquals("copy", copy.name)
        assertNull(original.limit)
        assertEquals(5, copy.limit)
    }

    @Test
    fun `JoinDefinition has correct defaults`() {
        val join =
            JoinDefinition(
                tableName = "posts",
                onCondition = ListObject.NIL,
            )

        assertEquals("posts", join.tableName)
        assertEquals("INNER", join.joinType)
    }

    @Test
    fun `JoinDefinition with custom join type`() {
        val join =
            JoinDefinition(
                tableName = "posts",
                joinType = "LEFT",
                onCondition = ListObject.NIL,
            )

        assertEquals("LEFT", join.joinType)
    }

    @Test
    fun `multiple joins supported`() {
        val joins =
            listOf(
                JoinDefinition("users", "INNER", ListObject.NIL),
                JoinDefinition("categories", "LEFT", ListObject.NIL),
                JoinDefinition("tags", "LEFT", ListObject.NIL),
            )

        val query =
            QueryDefinition(
                name = "multi-join",
                tableName = "posts",
                joins = joins,
            )

        assertEquals(3, query.joins?.size)
    }

    @Test
    fun `isSingle defaults to false`() {
        val query =
            QueryDefinition(
                name = "test",
                tableName = "users",
            )

        assertFalse(query.isSingle)
    }

    @Test
    fun `parameterNames defaults to empty list`() {
        val query =
            QueryDefinition(
                name = "test",
                tableName = "users",
            )

        assertTrue(query.parameterNames.isEmpty())
        assertEquals(0, query.parameterNames.size)
    }
}
