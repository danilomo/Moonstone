package net.sourceforge.moonstone.runtime

import net.sourceforge.moonstone.render.ModifierBuilder
import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.objects.BooleanObject

/**
 * Utility object for resolving values from state cells and derived state cells.
 * Provides consistent value extraction across all components.
 */
object ValueResolver {

    /**
     * Resolve any value, unwrapping StateCell and DerivedStateCell.
     * Returns the raw value or null.
     */
    fun resolveAny(value: Any?): Any? {
        return when (value) {
            is StateCell -> value.value.asObject()
            is DerivedStateCell -> value.value.asObject()
            else -> value
        }
    }

    /**
     * Resolve a value to a LispObject.
     * Useful when the raw LispObject is needed for further processing.
     */
    fun resolveLispObject(value: Any?): LispObject? {
        return when (value) {
            is StateCell -> value.value
            is DerivedStateCell -> value.value
            is LispObject -> value
            else -> null
        }
    }

    /**
     * Resolve a value to a string.
     * Handles StateCell, DerivedStateCell, and direct values.
     */
    fun resolveString(value: Any?): String {
        return when (value) {
            is StateCell -> lispObjectToString(value.value)
            is DerivedStateCell -> lispObjectToString(value.value)
            is String -> value
            null -> ""
            else -> value.toString()
        }
    }

    /**
     * Resolve a value to a boolean.
     * Handles StateCell, DerivedStateCell, BooleanObject, numbers, and other values.
     */
    fun resolveBoolean(value: Any?): Boolean {
        return when (value) {
            is StateCell -> lispObjectToBoolean(value.value)
            is DerivedStateCell -> lispObjectToBoolean(value.value)
            is Boolean -> value
            is Number -> value.toInt() != 0
            else -> ModifierBuilder.isTruthy(value)
        }
    }

    /**
     * Resolve a value to a number or null.
     */
    fun resolveNumber(value: Any?): Number? {
        return when (value) {
            is StateCell -> lispObjectToNumber(value.value)
            is DerivedStateCell -> lispObjectToNumber(value.value)
            is Number -> value
            else -> null
        }
    }

    /**
     * Convert a LispObject to a string representation.
     */
    private fun lispObjectToString(lispValue: LispObject): String {
        return when {
            lispValue.asString() != null -> lispValue.asString().value()
            lispValue.asInt() != null -> lispValue.asInt().value.toString()
            lispValue.asDouble() != null -> lispValue.asDouble().value.toString()
            else -> lispValue.asObject()?.toString() ?: ""
        }
    }

    /**
     * Convert a LispObject to a boolean.
     */
    private fun lispObjectToBoolean(lispValue: LispObject): Boolean {
        return when {
            lispValue.asInt() != null -> lispValue.asInt().value != 0
            lispValue.asObject(Boolean::class.java) != null ->
                lispValue.asObject(Boolean::class.java) == true
            lispValue == BooleanObject.TRUE -> true
            lispValue == BooleanObject.FALSE -> false
            else -> ModifierBuilder.isTruthy(lispValue.asObject())
        }
    }

    /**
     * Convert a LispObject to a number or null.
     */
    private fun lispObjectToNumber(lispValue: LispObject): Number? {
        return when {
            lispValue.asInt() != null -> lispValue.asInt().value
            lispValue.asDouble() != null -> lispValue.asDouble().value
            else -> null
        }
    }
}
