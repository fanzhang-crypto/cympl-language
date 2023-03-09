package demo.parser.interpret

import demo.parser.domain.*

sealed interface UnaryOperation {

    fun apply(value: TValue): TValue

    object Plus : UnaryOperation {
        override fun apply(value: TValue): TValue = when (value.type) {
            Type.INT -> value
            Type.FLOAT -> value
            else -> throw InterpretException("cannot apply unary + to ${value.type}")
        }
    }

    object Minus : UnaryOperation {
        override fun apply(value: TValue): TValue = when (value.type) {
            Type.INT -> TValue(Type.INT, -value.asInt())
            Type.FLOAT -> TValue(Type.FLOAT, -value.asDouble())
            else -> throw InterpretException("cannot apply unary - to ${value.type}")
        }
    }

    object Not : UnaryOperation {
        override fun apply(value: TValue): TValue = when (value.type) {
            Type.BOOL -> TValue(Type.BOOL, !value.asBoolean())
            else -> throw InterpretException("cannot apply logical not to ${value.type}")
        }
    }
}
