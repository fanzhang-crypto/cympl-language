package demo.parser.interpret

import demo.parser.domain.*

sealed interface UnaryOperation {

    fun apply(value: TValue): TValue

    object Plus : UnaryOperation {
        override fun apply(value: TValue): TValue = when (value.type) {
            BuiltinType.INT -> value
            BuiltinType.FLOAT -> value
            else -> throw InterpretException("cannot apply unary + to ${value.type}")
        }
    }

    object Minus : UnaryOperation {
        override fun apply(value: TValue): TValue = when (value.type) {
            BuiltinType.INT -> TValue(BuiltinType.INT, -value.asInt())
            BuiltinType.FLOAT -> TValue(BuiltinType.FLOAT, -value.asDouble())
            else -> throw InterpretException("cannot apply unary - to ${value.type}")
        }
    }

    object Increment : UnaryOperation {
        override fun apply(value: TValue): TValue = when (value.type) {
            BuiltinType.INT -> TValue(BuiltinType.INT, value.asInt() + 1)
            BuiltinType.FLOAT -> TValue(BuiltinType.FLOAT, value.asDouble() + 1.0)
            else -> throw InterpretException("cannot apply increment to ${value.type}")
        }
    }

    object Decrement : UnaryOperation {
        override fun apply(value: TValue): TValue = when (value.type) {
            BuiltinType.INT -> TValue(BuiltinType.INT, value.asInt() - 1)
            BuiltinType.FLOAT -> TValue(BuiltinType.FLOAT, value.asDouble() - 1.0)
            else -> throw InterpretException("cannot apply decrement to ${value.type}")
        }
    }

    object Not : UnaryOperation {
        override fun apply(value: TValue): TValue = when (value.type) {
            BuiltinType.BOOL -> TValue(BuiltinType.BOOL, !value.asBoolean())
            else -> throw InterpretException("cannot apply logical not to ${value.type}")
        }
    }
}
