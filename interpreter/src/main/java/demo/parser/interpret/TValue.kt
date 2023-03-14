package demo.parser.interpret

import demo.parser.domain.*
import demo.parser.interpret.TypeChecker.assertValueType

open class TValue(val type: BuiltinType, val value: Any) {

    object TEmptyArray : TValue(EmptyArray, emptyArray<TValue>())

    object VOID : TValue(BuiltinType.VOID, "void") {
        override fun toString() = "void"
    }

    fun withValue(value: Any): TValue = TValue(type, value).also { assertValueType(it, type) }

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

    @Suppress("UNCHECKED_CAST")
    fun asString() = when (type) {
        is BuiltinType.ARRAY -> "[" + (value as Array<TValue>).joinToString(", ") + "]"
        else -> value.toString()
    }

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

    override fun toString() = asString()
}
