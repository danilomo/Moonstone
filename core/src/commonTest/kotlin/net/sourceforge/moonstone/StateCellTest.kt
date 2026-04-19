package net.sourceforge.moonstone

import net.sourceforge.kleinlisp.objects.IntObject
import net.sourceforge.kleinlisp.objects.StringObject
import net.sourceforge.kleinlisp.objects.VoidObject
import net.sourceforge.moonstone.runtime.StateCell
import net.sourceforge.moonstone.runtime.StateManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StateCellTest {
    @Test
    fun `state creates cell with initial value`() {
        val stateManager = StateManager()
        val initialValue = IntObject(42)
        val cell = stateManager.createCell(initialValue)

        assertEquals(initialValue, cell.value)
    }

    @Test
    fun `state-ref returns current value`() {
        val stateManager = StateManager()
        val initialValue = StringObject("hello")
        val cell = StateCell(initialValue, stateManager)

        assertEquals(initialValue, cell.value)
        assertEquals("hello", cell.value.asString()?.value())
    }

    @Test
    fun `state-set! updates value`() {
        val stateManager = StateManager()
        val initialValue = IntObject(0)
        val cell = StateCell(initialValue, stateManager)

        assertEquals(IntObject(0), cell.value)

        val newValue = IntObject(10)
        cell.value = newValue

        assertEquals(newValue, cell.value)
        assertEquals(10, cell.value.asInt()?.value)
    }

    @Test
    fun `state-update! applies function to value`() {
        val stateManager = StateManager()
        val cell = StateCell(IntObject(5), stateManager)

        assertEquals(5, cell.value.asInt()?.value)

        // Simulate state-update! by reading, transforming, and writing
        val currentValue = cell.value.asInt()?.value ?: 0
        cell.value = IntObject(currentValue + 1)

        assertEquals(6, cell.value.asInt()?.value)
    }

    @Test
    fun `state notifies listeners on change`() {
        val stateManager = StateManager()
        var callbackInvoked = false
        var callbackCount = 0

        stateManager.setRecompositionCallback {
            callbackInvoked = true
            callbackCount++
        }

        val cell = stateManager.createCell(IntObject(0))

        // Change value - should trigger callback
        cell.value = IntObject(1)

        assertTrue(callbackInvoked, "Callback should be invoked on state change")
        assertEquals(1, callbackCount)

        // Change again
        cell.value = IntObject(2)
        assertEquals(2, callbackCount)
    }

    @Test
    fun `state handles null values`() {
        val stateManager = StateManager()
        val cell = StateCell(VoidObject.VOID, stateManager)

        assertEquals(VoidObject.VOID, cell.value)

        // Can set to actual value
        cell.value = IntObject(42)
        assertEquals(42, cell.value.asInt()?.value)

        // Can set back to void
        cell.value = VoidObject.VOID
        assertEquals(VoidObject.VOID, cell.value)
    }

    @Test
    fun `state change only notifies when value is different`() {
        val stateManager = StateManager()
        var callbackCount = 0

        stateManager.setRecompositionCallback {
            callbackCount++
        }

        val cell = stateManager.createCell(IntObject(0))
        assertEquals(0, callbackCount)

        // Set to same value - should NOT trigger callback
        cell.value = IntObject(0)
        assertEquals(0, callbackCount, "Setting same value should not trigger callback")

        // Set to different value - should trigger callback
        cell.value = IntObject(1)
        assertEquals(1, callbackCount)
    }

    @Test
    fun `state subscribe and unsubscribe works`() {
        val stateManager = StateManager()
        val cell = StateCell(IntObject(0), stateManager)
        var subscriberCallCount = 0

        val callback: () -> Unit = { subscriberCallCount++ }
        cell.subscribe(callback)

        // Change value - subscriber should be notified
        cell.value = IntObject(1)
        assertEquals(1, subscriberCallCount)

        // Unsubscribe
        cell.unsubscribe(callback)

        // Change value again - subscriber should NOT be notified
        cell.value = IntObject(2)
        assertEquals(1, subscriberCallCount, "Unsubscribed callback should not be called")
    }

    @Test
    fun `state toString returns readable format`() {
        val stateManager = StateManager()
        val cell = StateCell(IntObject(42), stateManager)

        val str = cell.toString()
        assertTrue(str.contains("StateCell"), "toString should contain StateCell")
        assertTrue(str.contains("42"), "toString should contain the value")
    }

    @Test
    fun `multiple state cells are independent`() {
        val stateManager = StateManager()
        val cell1 = stateManager.createCell(IntObject(1))
        val cell2 = stateManager.createCell(IntObject(2))

        assertEquals(1, cell1.value.asInt()?.value)
        assertEquals(2, cell2.value.asInt()?.value)

        cell1.value = IntObject(10)
        assertEquals(10, cell1.value.asInt()?.value)
        assertEquals(2, cell2.value.asInt()?.value, "Other cell should not be affected")
    }
}
