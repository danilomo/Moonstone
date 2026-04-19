package net.sourceforge.moonstone.components

import net.sourceforge.kleinlisp.LispEnvironment

/**
 * Registry for UI components.
 * Maps component names to their factories.
 */
class ComponentRegistry {
    private val components = mutableMapOf<String, ComponentFactory>()

    fun register(
        name: String,
        factory: ComponentFactory,
    ) {
        components[name] = factory
    }

    fun register(factory: ComponentFactory) {
        components[factory.name] = factory
    }

    fun get(name: String): ComponentFactory? = components[name]

    fun contains(name: String): Boolean = components.containsKey(name)

    fun forEach(action: (String, ComponentFactory) -> Unit) {
        components.forEach(action)
    }

    /**
     * Register all components as Scheme functions in the given environment.
     */
    fun bindToEnvironment(env: LispEnvironment) {
        components.forEach { (name, factory) ->
            env.registerFunction(name) { params -> factory.create(params) }
        }
    }

    /**
     * Get all registered component names.
     */
    fun getNames(): Set<String> = components.keys.toSet()
}
