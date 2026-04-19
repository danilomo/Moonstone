package net.sourceforge.moonstone

import androidx.compose.runtime.Composable
import net.sourceforge.moonstone.components.impl.ColumnComponent
import net.sourceforge.moonstone.components.impl.TextComponent
import net.sourceforge.moonstone.error.ScriptLoadException
import net.sourceforge.moonstone.runtime.MoonstoneRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MoonstoneRuntimeTest {
    private fun createRuntimeWithComponents(): MoonstoneRuntime {
        val runtime = MoonstoneRuntime()
        runtime.registerComponent(TextComponent())
        runtime.registerComponent(ColumnComponent())
        return runtime
    }
    @Test
    fun `finds app entry point`() {
        val runtime = createRuntimeWithComponents()
        val code = """
            (define (app)
              (text #:value "Hello"))
        """.trimIndent()

        val element = runtime.loadCode(code)
        assertNotNull(element, "Should find and execute app entry point")
    }

    @Test
    fun `finds main entry point`() {
        val runtime = createRuntimeWithComponents()
        val code = """
            (define (main)
              (text #:value "Hello"))
        """.trimIndent()

        val element = runtime.loadCode(code)
        assertNotNull(element, "Should find and execute main entry point")
    }

    @Test
    fun `finds my-app entry point`() {
        val runtime = createRuntimeWithComponents()
        val code = """
            (define (my-app)
              (text #:value "Hello"))
        """.trimIndent()

        val element = runtime.loadCode(code)
        assertNotNull(element, "Should find and execute my-app entry point")
    }

    @Test
    fun `finds root entry point`() {
        val runtime = createRuntimeWithComponents()
        val code = """
            (define (root)
              (text #:value "Hello"))
        """.trimIndent()

        val element = runtime.loadCode(code)
        assertNotNull(element, "Should find and execute root entry point")
    }

    @Test
    fun `throws when no entry point found`() {
        val runtime = createRuntimeWithComponents()
        val code = """
            (define (some-other-function)
              (text #:value "Hello"))
        """.trimIndent()

        val exception = assertFailsWith<ScriptLoadException> {
            runtime.loadCode(code)
        }

        assertTrue(
            exception.cause is IllegalStateException,
            "Underlying cause should be IllegalStateException"
        )
        val message = exception.cause?.message ?: exception.message ?: ""
        assertTrue(
            message.contains("No entry point found"),
            "Exception should mention missing entry point"
        )
        assertTrue(
            message.contains("app"),
            "Exception should list available entry points"
        )
    }

    @Test
    fun `loads script from string`() {
        val runtime = createRuntimeWithComponents()
        val code = """
            (define (app)
              (column
                (text #:value "Line 1")
                (text #:value "Line 2")))
        """.trimIndent()

        val element = runtime.loadCode(code)
        assertNotNull(element, "Should load and parse script from string")
        assertEquals("column", element.type)
    }

    @Test
    fun `handles syntax errors gracefully`() {
        val runtime = MoonstoneRuntime()
        val code = """
            (define (app)
              (text #:value "Missing closing paren"
        """.trimIndent()

        assertFailsWith<ScriptLoadException> {
            runtime.loadCode(code)
        }
    }

    @Test
    fun `evaluates simple expressions`() {
        val runtime = MoonstoneRuntime()
        val result = runtime.evaluate("(+ 2 3)")

        assertEquals(5, result.asInt()?.value)
    }

    @Test
    fun `can register custom components`() {
        val runtime = createRuntimeWithComponents()

        val customComponentCalled = mutableListOf<Boolean>()
        val customFactory = object : net.sourceforge.moonstone.components.ComponentFactory {
            override val name = "custom-text"

            override fun create(params: Array<net.sourceforge.kleinlisp.LispObject>): net.sourceforge.kleinlisp.LispObject {
                customComponentCalled.add(true)
                return runtime.componentRegistry.get("text")?.create(params)!!
            }

            @Composable
            override fun Render(element: net.sourceforge.moonstone.components.UIElement, renderChild: @Composable (net.sourceforge.moonstone.components.UIElement) -> Unit) {
                // No-op for test
            }
        }
        runtime.registerComponent(customFactory)

        assertTrue(runtime.componentRegistry.contains("custom-text"))

        val code = """
            (define (app)
              (custom-text #:value "Hello"))
        """.trimIndent()

        runtime.loadCode(code)
        assertTrue(customComponentCalled.isNotEmpty(), "Custom component should be called")
    }

    @Test
    fun `state functions are available`() {
        val runtime = MoonstoneRuntime()

        val result = runtime.evaluate("(state 42)")
        assertNotNull(result, "state function should be available")

        val stateRefCode = """
            (define my-state (state 10))
            (state-ref my-state)
        """.trimIndent()
        val value = runtime.evaluate(stateRefCode)
        assertEquals(10, value.asInt()?.value)
    }

    @Test
    fun `state-set! modifies state`() {
        val runtime = MoonstoneRuntime()

        val code = """
            (define my-state (state 5))
            (state-set! my-state 15)
            (state-ref my-state)
        """.trimIndent()

        val result = runtime.evaluate(code)
        assertEquals(15, result.asInt()?.value)
    }

    @Test
    fun `state-update! applies function`() {
        val runtime = MoonstoneRuntime()

        val code = """
            (define counter (state 0))
            (state-update! counter (lambda (x) (+ x 1)))
            (state-ref counter)
        """.trimIndent()

        val result = runtime.evaluate(code)
        assertEquals(1, result.asInt()?.value)
    }

    @Test
    fun `derived state computes value`() {
        val runtime = MoonstoneRuntime()

        val code = """
            (define base (state 10))
            (define doubled (derived (lambda () (* (state-ref base) 2))))
            (state-ref doubled)
        """.trimIndent()

        val result = runtime.evaluate(code)
        assertEquals(20, result.asInt()?.value)
    }

    @Test
    fun `platform function returns platform`() {
        val runtime = MoonstoneRuntime()
        val result = runtime.evaluate("(platform)")

        assertNotNull(result, "platform function should return a value")
    }

    @Test
    fun `reactive mode stores entry point function`() {
        val runtime = createRuntimeWithComponents()
        val code = """
            (define (app)
              (text #:value "Hello"))
        """.trimIndent()

        runtime.loadCodeForReactiveMode(code)
        assertTrue(runtime.isReactiveMode(), "Should be in reactive mode after loading")
        assertNotNull(runtime.rootElement.value, "Root element should be set")
    }

    @Test
    fun `reactive mode re-evaluates on state change`() {
        val runtime = createRuntimeWithComponents()
        val code = """
            (define counter (state 0))
            (define (app)
              (text #:value (number->string (state-ref counter))))
        """.trimIndent()

        runtime.loadCodeForReactiveMode(code)

        val initialElement = runtime.rootElement.value
        assertNotNull(initialElement)

        // Modify state - should trigger re-evaluation
        runtime.evaluate("(state-set! counter 1)")

        // Root element should be updated
        val updatedElement = runtime.rootElement.value
        assertNotNull(updatedElement)
    }

    @Test
    fun `update-app changes the running app`() {
        val runtime = createRuntimeWithComponents()
        val initialCode = """
            (define (app)
              (text #:value "Initial"))
        """.trimIndent()

        runtime.loadCodeForReactiveMode(initialCode)

        // Define a new app function and update
        val updateCode = """
            (define (new-app)
              (text #:value "Updated"))
            (update-app new-app)
        """.trimIndent()

        val result = runtime.evaluate(updateCode)
        assertTrue(result.truthiness(), "update-app should return true on success")
    }

    @Test
    fun `handles invalid state-ref gracefully`() {
        val runtime = MoonstoneRuntime()

        val exception = assertFailsWith<Exception> {
            runtime.evaluate("(state-ref)")
        }

        assertTrue(
            exception.message?.contains("No argument") == true ||
                exception.message?.contains("state-ref") == true,
            "Should provide helpful error for state-ref with no arguments"
        )
    }

    @Test
    fun `handles invalid state-set! gracefully`() {
        val runtime = MoonstoneRuntime()

        val exception = assertFailsWith<Exception> {
            runtime.evaluate("(state-set! 42 100)")
        }

        assertTrue(
            exception.message?.contains("state cell") == true ||
                exception.message?.contains("state-set!") == true,
            "Should provide helpful error for invalid state-set!"
        )
    }
}
