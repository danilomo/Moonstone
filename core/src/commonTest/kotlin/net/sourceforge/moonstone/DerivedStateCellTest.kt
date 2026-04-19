package net.sourceforge.moonstone

import net.sourceforge.kleinlisp.Function
import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.objects.IntObject
import net.sourceforge.kleinlisp.objects.StringObject
import net.sourceforge.moonstone.runtime.DerivedStateCell
import net.sourceforge.moonstone.runtime.StateManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DerivedStateCellTest {
    @Test
    fun `derived computes initial value`() {
        val computation =
            object : Function {
                override fun evaluate(args: Array<LispObject>): LispObject = IntObject(42)
            }

        val derived = DerivedStateCell(computation)
        assertEquals(42, derived.value.asInt()?.value)
    }

    @Test
    fun `derived updates when dependency changes`() {
        val stateManager = StateManager()
        val sourceCell = stateManager.createCell(IntObject(5))

        // Create a derived cell that reads from sourceCell
        val computation =
            object : Function {
                override fun evaluate(args: Array<LispObject>): LispObject {
                    val current = sourceCell.value.asInt()?.value ?: 0
                    return IntObject(current * 2)
                }
            }

        val derived = DerivedStateCell(computation)

        // Initial value should be 5 * 2 = 10
        assertEquals(10, derived.value.asInt()?.value)

        // Update source cell
        sourceCell.value = IntObject(10)

        // Derived should recompute on next access
        assertEquals(20, derived.value.asInt()?.value)
    }

    @Test
    fun `derived handles multiple dependencies`() {
        val stateManager = StateManager()
        val cell1 = stateManager.createCell(IntObject(3))
        val cell2 = stateManager.createCell(IntObject(4))

        val computation =
            object : Function {
                override fun evaluate(args: Array<LispObject>): LispObject {
                    val v1 = cell1.value.asInt()?.value ?: 0
                    val v2 = cell2.value.asInt()?.value ?: 0
                    return IntObject(v1 + v2)
                }
            }

        val derived = DerivedStateCell(computation)
        assertEquals(7, derived.value.asInt()?.value)

        // Change first cell
        cell1.value = IntObject(10)
        assertEquals(14, derived.value.asInt()?.value)

        // Change second cell
        cell2.value = IntObject(5)
        assertEquals(15, derived.value.asInt()?.value)
    }

    @Test
    fun `derived recomputes on every access`() {
        var computationCount = 0

        val computation =
            object : Function {
                override fun evaluate(args: Array<LispObject>): LispObject {
                    computationCount++
                    return IntObject(42)
                }
            }

        val derived = DerivedStateCell(computation)

        // Each value access should trigger computation
        derived.value
        assertEquals(1, computationCount)

        derived.value
        assertEquals(2, computationCount)

        derived.value
        assertEquals(3, computationCount)
    }

    @Test
    fun `derived can compute string concatenation`() {
        val stateManager = StateManager()
        val firstName = stateManager.createCell(StringObject("John"))
        val lastName = stateManager.createCell(StringObject("Doe"))

        val computation =
            object : Function {
                override fun evaluate(args: Array<LispObject>): LispObject {
                    val first = firstName.value.asString()?.value() ?: ""
                    val last = lastName.value.asString()?.value() ?: ""
                    return StringObject("$first $last")
                }
            }

        val derived = DerivedStateCell(computation)
        assertEquals("John Doe", derived.value.asString()?.value())

        firstName.value = StringObject("Jane")
        assertEquals("Jane Doe", derived.value.asString()?.value())
    }

    @Test
    fun `derived can chain computations`() {
        val stateManager = StateManager()
        val base = stateManager.createCell(IntObject(2))

        // First derived: base * 2
        val derived1Computation =
            object : Function {
                override fun evaluate(args: Array<LispObject>): LispObject {
                    val v = base.value.asInt()?.value ?: 0
                    return IntObject(v * 2)
                }
            }
        val derived1 = DerivedStateCell(derived1Computation)

        // Second derived: derived1 + 10
        val derived2Computation =
            object : Function {
                override fun evaluate(args: Array<LispObject>): LispObject {
                    val v = derived1.value.asInt()?.value ?: 0
                    return IntObject(v + 10)
                }
            }
        val derived2 = DerivedStateCell(derived2Computation)

        // base=2, derived1=4, derived2=14
        assertEquals(14, derived2.value.asInt()?.value)

        // Update base
        base.value = IntObject(5)
        // base=5, derived1=10, derived2=20
        assertEquals(20, derived2.value.asInt()?.value)
    }

    @Test
    fun `derived toString returns readable format`() {
        val computation =
            object : Function {
                override fun evaluate(args: Array<LispObject>): LispObject = IntObject(0)
            }

        val derived = DerivedStateCell(computation)
        val str = derived.toString()

        assertTrue(str.contains("DerivedStateCell"), "toString should contain DerivedStateCell")
    }

    @Test
    fun `derived handles exceptions in computation gracefully`() {
        val computation =
            object : Function {
                override fun evaluate(args: Array<LispObject>): LispObject =
                    throw RuntimeException("Computation failed")
            }

        val derived = DerivedStateCell(computation)

        // Accessing value should propagate the exception
        var exceptionThrown = false
        try {
            derived.value
        } catch (e: RuntimeException) {
            exceptionThrown = true
            assertEquals("Computation failed", e.message)
        }

        assertTrue(exceptionThrown, "Exception should be propagated from computation")
    }

    @Test
    fun `derived with no dependencies returns constant`() {
        val computation =
            object : Function {
                override fun evaluate(args: Array<LispObject>): LispObject = IntObject(100)
            }

        val derived = DerivedStateCell(computation)

        assertEquals(100, derived.value.asInt()?.value)
        assertEquals(100, derived.value.asInt()?.value)
        assertEquals(100, derived.value.asInt()?.value)
    }
}
