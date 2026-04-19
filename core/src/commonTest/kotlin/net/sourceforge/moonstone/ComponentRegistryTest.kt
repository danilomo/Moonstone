package net.sourceforge.moonstone

import androidx.compose.runtime.Composable
import net.sourceforge.kleinlisp.Lisp
import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.objects.JavaObject
import net.sourceforge.moonstone.components.ComponentElement
import net.sourceforge.moonstone.components.ComponentFactory
import net.sourceforge.moonstone.components.ComponentRegistry
import net.sourceforge.moonstone.components.UIElement
import net.sourceforge.moonstone.components.UIElementWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComponentRegistryTest {
    private class TestComponentFactory(
        override val name: String,
        val onCreateCallback: ((Array<LispObject>) -> Unit)? = null,
    ) : ComponentFactory {
        override fun create(params: Array<LispObject>): LispObject {
            onCreateCallback?.invoke(params)
            val element = ComponentElement(name, emptyMap(), emptyList())
            return JavaObject(UIElementWrapper(element))
        }

        @Composable
        override fun Render(element: UIElement, renderChild: @Composable (UIElement) -> Unit) {
            // No-op for tests
        }
    }

    @Test
    fun `register adds component to registry`() {
        val registry = ComponentRegistry()
        val factory = TestComponentFactory("test-component")

        registry.register("test-component", factory)

        assertTrue(registry.contains("test-component"))
        assertEquals(factory, registry.get("test-component"))
    }

    @Test
    fun `register using factory name`() {
        val registry = ComponentRegistry()
        val factory = TestComponentFactory("my-component")

        registry.register(factory)

        assertTrue(registry.contains("my-component"))
        assertEquals(factory, registry.get("my-component"))
    }

    @Test
    fun `get returns null for non-existent component`() {
        val registry = ComponentRegistry()

        assertNull(registry.get("non-existent"))
        assertFalse(registry.contains("non-existent"))
    }

    @Test
    fun `contains returns true only for registered components`() {
        val registry = ComponentRegistry()
        val factory = TestComponentFactory("button")

        assertFalse(registry.contains("button"))

        registry.register(factory)

        assertTrue(registry.contains("button"))
        assertFalse(registry.contains("text"))
    }

    @Test
    fun `forEach iterates over all registered components`() {
        val registry = ComponentRegistry()
        val factory1 = TestComponentFactory("component-1")
        val factory2 = TestComponentFactory("component-2")
        val factory3 = TestComponentFactory("component-3")

        registry.register(factory1)
        registry.register(factory2)
        registry.register(factory3)

        val visited = mutableListOf<String>()
        registry.forEach { name, factory ->
            visited.add(name)
        }

        assertEquals(3, visited.size)
        assertTrue(visited.contains("component-1"))
        assertTrue(visited.contains("component-2"))
        assertTrue(visited.contains("component-3"))
    }

    @Test
    fun `getNames returns all registered component names`() {
        val registry = ComponentRegistry()
        registry.register(TestComponentFactory("comp-a"))
        registry.register(TestComponentFactory("comp-b"))
        registry.register(TestComponentFactory("comp-c"))

        val names = registry.getNames()

        assertEquals(3, names.size)
        assertTrue(names.contains("comp-a"))
        assertTrue(names.contains("comp-b"))
        assertTrue(names.contains("comp-c"))
    }

    @Test
    fun `bindToEnvironment registers components as Scheme functions`() {
        val registry = ComponentRegistry()
        var createCalled = false

        val factory = TestComponentFactory("my-widget") { params ->
            createCalled = true
        }
        registry.register(factory)

        val lisp = Lisp()
        val env = lisp.environment()
        registry.bindToEnvironment(env)

        // Verify the function is registered in the environment
        val atom = env.atomOf("my-widget")
        val value = env.lookupValueOrNull(atom)
        assertNotNull(value, "Component should be registered as a function")

        // Call the function
        lisp.evaluate("(my-widget)")

        assertTrue(createCalled, "Component factory create should be called")
    }

    @Test
    fun `registry can replace existing component`() {
        val registry = ComponentRegistry()
        val factory1 = TestComponentFactory("button")
        val factory2 = TestComponentFactory("button")

        registry.register(factory1)
        assertEquals(factory1, registry.get("button"))

        registry.register(factory2)
        assertEquals(factory2, registry.get("button"), "Should replace with new factory")
    }

    @Test
    fun `empty registry has no components`() {
        val registry = ComponentRegistry()

        assertEquals(0, registry.getNames().size)
        assertFalse(registry.contains("anything"))
        assertNull(registry.get("anything"))
    }

    @Test
    fun `multiple registries are independent`() {
        val registry1 = ComponentRegistry()
        val registry2 = ComponentRegistry()

        registry1.register(TestComponentFactory("component-1"))
        registry2.register(TestComponentFactory("component-2"))

        assertTrue(registry1.contains("component-1"))
        assertFalse(registry1.contains("component-2"))

        assertTrue(registry2.contains("component-2"))
        assertFalse(registry2.contains("component-1"))
    }

    @Test
    fun `factory create is called with correct parameters`() {
        val registry = ComponentRegistry()
        var receivedParams: Array<LispObject>? = null

        val factory = TestComponentFactory("test") { params ->
            receivedParams = params
        }
        registry.register(factory)

        val lisp = Lisp()
        val env = lisp.environment()
        registry.bindToEnvironment(env)

        lisp.evaluate("(test 1 2 3)")

        assertNotNull(receivedParams, "Parameters should be passed to factory")
        assertEquals(3, receivedParams!!.size)
    }

    @Test
    fun `getNames returns empty set for empty registry`() {
        val registry = ComponentRegistry()
        val names = registry.getNames()

        assertNotNull(names)
        assertEquals(0, names.size)
    }
}
