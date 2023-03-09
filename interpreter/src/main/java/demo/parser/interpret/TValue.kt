package demo.parser.interpret

import demo.parser.domain.*

open class TValue(val type: Type, val value: Any) {

    object VOID : TValue(Type.VOID, "void") {
        override fun toString() = "void"
    }

    fun withValue(value: Any): TValue {
        type.checkValue(value)
        return TValue(type, value)
    }

    fun asInt() = when (value) {
        is Int -> value
        is Double -> value.toInt()
        else -> throw InterpretException("cannot convert $value to int")
    }

    fun asDouble() = when (value) {
        is Int -> value.toDouble()
        is Double -> value
        else -> throw InterpretException("cannot convert $value to double")
    }

    fun asString() = value.toString()

    fun asBoolean(): Boolean = when (value) {
        is Boolean -> value
        is Int -> value != 0
        is Double -> value != 0.0
        else -> throw InterpretException("cannot convert $value to boolean")
    }

    fun asComparable(): Comparable<*> = when (value) {
        is Int -> value
        is Double -> value
        is String -> value
        else -> throw InterpretException("cannot convert $value to comparable")
    }
}
