package net.sourceforge.moonstone.runtime

import androidx.compose.runtime.mutableStateOf
import net.sourceforge.kleinlisp.LispObject

/**
 * A reactive container that triggers UI updates when its value changes.
 * Wraps a Compose MutableState to integrate with Compose's recomposition system.
 */
class StateCell(
    initialValue: LispObject,
    private val stateManager: StateManager
) {
    private val _state = mutableStateOf(initialValue)
    private val subscribers = mutableListOf<() -> Unit>()

    var value: LispObject
        get() = _state.value
        set(newValue) {
            if (_state.value != newValue) {
                _state.value = newValue
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

    override fun toString(): String = "StateCell(${_state.value})"
}
