package demo.parser.domain

data class TValue(val type: VariableType, val value: Any) {

    fun withValue(value: Any): TValue {
        type.checkValue(value)
        return copy(value = value)
    }

    fun asInt() = when(value) {
        is kotlin.Int -> value
        is Double -> value.toInt()
        else -> throw SemanticException("cannot convert $value to int")
    }

    fun asDouble() = when(value) {
        is kotlin.Int -> value.toDouble()
        is Double -> value
        else -> throw SemanticException("cannot convert $value to double")
    }

    fun asString() = value.toString()
}
