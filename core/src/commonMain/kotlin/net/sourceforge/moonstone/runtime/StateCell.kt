package net.sourceforge.moonstone.runtime

import androidx.compose.runtime.mutableStateOf
import net.sourceforge.kleinlisp.LispObject

/**
 * A reactive container that triggers UI updates when its value changes.
 * Wraps a Compose MutableState to integrate with Compose's recomposition system.
 */
class StateCell(
    initialValue: LispObject,
    private val stateManager: StateManager,
) {
    private val state = mutableStateOf(initialValue)
    private val subscribers = mutableListOf<() -> Unit>()

    var value: LispObject
        get() = state.value
        set(newValue) {
            if (state.value != newValue) {
                state.value = newValue
                notifySubscribers()
            }
        }

    fun subscribe(callback: () -> Unit) {
        subscribers.add(callback)
    }

    fun unsubscribe(callback: () -> Unit) {
        subscribers.remove(callback)
    }

    private fun notifySubscribers() {
        stateManager.scheduleRecomposition()
        subscribers.forEach { it() }
    }

    override fun toString(): String = "StateCell(${state.value})"
}
