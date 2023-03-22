package demo.parser.interpret

import demo.parser.domain.*
import demo.parser.interpret.RuntimeTypeChecker.assertValueType

open class TValue(val type: BuiltinType, val value: Any) {

    object TEmptyArray : TValue(EmptyArray, emptyArray<TValue>())

    object NULL: TValue(BuiltinType.ANY, "null") {
        override fun toString() = "null"
    }

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

    override fun toString() = asString()

    companion object {
        fun defaultValueOf(type: BuiltinType) = when (type) {
            BuiltinType.ANY -> NULL
            BuiltinType.VOID -> VOID
            BuiltinType.BOOL -> TValue(type, false)
            BuiltinType.INT -> TValue(type, 0)
            BuiltinType.FLOAT -> TValue(type, 0.0)
            BuiltinType.STRING -> TValue(type, "")
            is BuiltinType.ARRAY -> TEmptyArray
            else -> throw InterpretException("unknown type $type")
        }
    }
}
