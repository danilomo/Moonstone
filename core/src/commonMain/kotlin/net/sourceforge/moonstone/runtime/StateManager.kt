package net.sourceforge.moonstone.runtime

import net.sourceforge.kleinlisp.LispObject

/**
 * Coordinates state updates and UI recomposition.
 */
class StateManager {
    private val cells = mutableListOf<StateCell>()
    private var recompositionPending = false
    private var recompositionCallback: (() -> Unit)? = null

    fun createCell(initialValue: LispObject): StateCell {
        val cell = StateCell(initialValue, this)
        cells.add(cell)
        return cell
    }

    fun setRecompositionCallback(callback: () -> Unit) {
        recompositionCallback = callback
    }

    fun scheduleRecomposition() {
        if (!recompositionPending) {
            recompositionPending = true
            recompositionCallback?.invoke()
            recompositionPending = false
        }
    }

    fun clear() {
        cells.clear()
    }
}
