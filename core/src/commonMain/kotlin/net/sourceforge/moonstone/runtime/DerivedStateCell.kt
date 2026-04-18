package net.sourceforge.moonstone.runtime

import net.sourceforge.kleinlisp.Function
import net.sourceforge.kleinlisp.LispObject

/**
 * A derived state cell that computes its value from a function.
 * The computation is evaluated during Compose render, so any state cell
 * reads are tracked by Compose for automatic recomposition.
 *
 * Scheme usage:
 * (define my-derived (derived (lambda () <computation>)))
 *
 * Example:
 * (define count (state 0))
 * (define display-text (derived (lambda ()
 *   (string-append "Count: " (number->string (state-ref count))))))
 * (text #:value display-text)  ; Updates automatically when count changes
 */
class DerivedStateCell(
    private val computation: Function
) {
    /**
     * Get the current computed value.
     * This should be called during Compose render to enable dependency tracking.
     * Each call re-evaluates the computation, so any state-ref calls inside
     * are tracked by Compose's snapshot system.
     */
    val value: LispObject
        get() = computation.evaluate(emptyArray())

    override fun toString(): String = "DerivedStateCell(<computation>)"
}
