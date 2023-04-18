package cympl.interpreter

import cympl.language.*
import kotlin.math.pow

sealed interface BinaryOperation {

    sealed interface Arithmetic : cympl.interpreter.BinaryOperation {
        fun apply(left: cympl.interpreter.TValue, right: cympl.interpreter.TValue): cympl.interpreter.TValue

        object Plus : cympl.interpreter.BinaryOperation.Arithmetic {
            override fun apply(left: cympl.interpreter.TValue, right: cympl.interpreter.TValue): cympl.interpreter.TValue = when (left.type) {
                BuiltinType.INT -> when (right.type) {
                    BuiltinType.INT -> cympl.interpreter.TValue(BuiltinType.INT, left.asInt() + right.asInt())
                    BuiltinType.FLOAT -> cympl.interpreter.TValue(BuiltinType.FLOAT, left.asInt() + right.asDouble())
                    BuiltinType.STRING -> cympl.interpreter.TValue(
                        BuiltinType.STRING,
                        left.asString() + right.asString()
                    )
                    else -> throw cympl.interpreter.InterpretException("cannot add ${left.type} to ${right.type}")
                }

                BuiltinType.FLOAT -> when (right.type) {
                    BuiltinType.INT -> cympl.interpreter.TValue(BuiltinType.FLOAT, left.asDouble() + right.asInt())
                    BuiltinType.FLOAT -> cympl.interpreter.TValue(BuiltinType.FLOAT, left.asDouble() + right.asDouble())
                    BuiltinType.STRING -> cympl.interpreter.TValue(
                        BuiltinType.STRING,
                        left.asString() + right.asString()
                    )
                    else -> throw cympl.interpreter.InterpretException("cannot add ${left.type} to ${right.type}")
                }

                BuiltinType.STRING -> cympl.interpreter.TValue(BuiltinType.STRING, left.asString() + right.asString())
                else -> {
                    throw cympl.interpreter.InterpretException("cannot add ${left.type} to ${right.type}")
                }
            }
        }

        object Minus : cympl.interpreter.BinaryOperation.Arithmetic {
            override fun apply(left: cympl.interpreter.TValue, right: cympl.interpreter.TValue): cympl.interpreter.TValue = when (left.type) {
                BuiltinType.INT -> when (right.type) {
                    BuiltinType.INT -> cympl.interpreter.TValue(BuiltinType.INT, left.asInt() - right.asInt())
                    BuiltinType.FLOAT -> cympl.interpreter.TValue(BuiltinType.FLOAT, left.asDouble() - right.asDouble())
                    else -> throw cympl.interpreter.InterpretException("cannot subtract ${right.type} from ${left.type}")
                }

                BuiltinType.FLOAT -> when (right.type) {
                    BuiltinType.INT -> cympl.interpreter.TValue(BuiltinType.FLOAT, left.asDouble() - right.asDouble())
                    BuiltinType.FLOAT -> cympl.interpreter.TValue(BuiltinType.FLOAT, left.asDouble() - right.asDouble())
                    else -> throw cympl.interpreter.InterpretException("cannot subtract ${right.type} from ${left.type}")
                }

                else -> throw cympl.interpreter.InterpretException("cannot subtract ${right.type} from ${left.type}")
            }
        }

        object Times : cympl.interpreter.BinaryOperation.Arithmetic {
            override fun apply(left: cympl.interpreter.TValue, right: cympl.interpreter.TValue): cympl.interpreter.TValue = when (left.type) {
                BuiltinType.INT -> when (right.type) {
                    BuiltinType.INT -> cympl.interpreter.TValue(BuiltinType.INT, left.asInt() * right.asInt())
                    BuiltinType.FLOAT -> cympl.interpreter.TValue(BuiltinType.FLOAT, left.asInt() * right.asDouble())
                    else -> throw cympl.interpreter.InterpretException("cannot multiply ${left.type} by ${right.type}")
                }

                BuiltinType.FLOAT -> when (right.type) {
                    BuiltinType.INT -> cympl.interpreter.TValue(BuiltinType.FLOAT, left.asDouble() * right.asInt())
                    BuiltinType.FLOAT -> cympl.interpreter.TValue(BuiltinType.FLOAT, left.asDouble() * right.asDouble())
                    else -> throw cympl.interpreter.InterpretException("cannot multiply ${left.type} by ${right.type}")
                }

                else -> throw cympl.interpreter.InterpretException("cannot multiply ${left.type} by ${right.type}")
            }
        }

        object Div : cympl.interpreter.BinaryOperation.Arithmetic {
            override fun apply(left: cympl.interpreter.TValue, right: cympl.interpreter.TValue): cympl.interpreter.TValue = when (left.type) {
                BuiltinType.INT -> when (right.type) {
                    BuiltinType.INT -> cympl.interpreter.TValue(BuiltinType.INT, left.asInt() / right.asInt())
                    BuiltinType.FLOAT -> cympl.interpreter.TValue(BuiltinType.FLOAT, left.asInt() / right.asDouble())
                    else -> throw cympl.interpreter.InterpretException("cannot divide ${left.type} by ${right.type}")
                }

                BuiltinType.FLOAT -> when (right.type) {
                    BuiltinType.INT -> cympl.interpreter.TValue(BuiltinType.FLOAT, left.asDouble() / right.asDouble())
                    BuiltinType.FLOAT -> cympl.interpreter.TValue(BuiltinType.FLOAT, left.asDouble() / right.asDouble())
                    else -> throw cympl.interpreter.InterpretException("cannot divide ${left.type} by ${right.type}")
                }

                else -> throw cympl.interpreter.InterpretException("cannot divide ${left.type} by ${right.type}")
            }
        }

        object Rem : cympl.interpreter.BinaryOperation.Arithmetic {
            override fun apply(left: cympl.interpreter.TValue, right: cympl.interpreter.TValue): cympl.interpreter.TValue = when (left.type) {
                BuiltinType.INT -> when (right.type) {
                    BuiltinType.INT -> cympl.interpreter.TValue(BuiltinType.INT, left.asInt() % right.asInt())
                    BuiltinType.FLOAT -> cympl.interpreter.TValue(BuiltinType.FLOAT, left.asInt() % right.asDouble())
                    else -> throw cympl.interpreter.InterpretException("cannot  ${left.type} by ${right.type}")
                }

                BuiltinType.FLOAT -> when (right.type) {
                    BuiltinType.INT -> cympl.interpreter.TValue(BuiltinType.FLOAT, left.asDouble() % right.asDouble())
                    BuiltinType.FLOAT -> cympl.interpreter.TValue(BuiltinType.FLOAT, left.asDouble() % right.asDouble())
                    else -> throw cympl.interpreter.InterpretException("cannot divide ${left.type} by ${right.type}")
                }

                else -> throw cympl.interpreter.InterpretException("cannot divide ${left.type} by ${right.type}")
            }
        }

        object Pow : cympl.interpreter.BinaryOperation.Arithmetic {
            override fun apply(left: cympl.interpreter.TValue, right: cympl.interpreter.TValue): cympl.interpreter.TValue {
                val leftValue = left.asDouble()
                val rightValue = right.asDouble()
                return left.withValue(leftValue.pow(rightValue))
            }
        }
    }

    sealed interface Logical : cympl.interpreter.BinaryOperation {

        fun apply(left: cympl.interpreter.TValue, right: () -> cympl.interpreter.TValue): cympl.interpreter.TValue {
            return when (this) {
                is cympl.interpreter.BinaryOperation.Logical.And -> left.withValue(left.asBoolean() && right().asBoolean())
                is cympl.interpreter.BinaryOperation.Logical.Or -> left.withValue(left.asBoolean() || right().asBoolean())
            }
        }

        object And : cympl.interpreter.BinaryOperation.Logical
        object Or : cympl.interpreter.BinaryOperation.Logical
    }

    sealed interface Comparison : cympl.interpreter.BinaryOperation {

        fun apply(left: cympl.interpreter.TValue, right: cympl.interpreter.TValue): cympl.interpreter.TValue

        object Eq : cympl.interpreter.BinaryOperation.Comparison {
            override fun apply(left: cympl.interpreter.TValue, right: cympl.interpreter.TValue): cympl.interpreter.TValue {
                if (left.type != right.type) {
                    throw cympl.interpreter.InterpretException("cannot compare ${left.type} to ${right.type}")
                }
                return cympl.interpreter.TValue(BuiltinType.BOOL, left.value == right.value)
            }
        }

        object Neq : cympl.interpreter.BinaryOperation.Comparison {
            override fun apply(left: cympl.interpreter.TValue, right: cympl.interpreter.TValue): cympl.interpreter.TValue {
                if (left.type != right.type) {
                    throw cympl.interpreter.InterpretException("cannot compare ${left.type} to ${right.type}")
                }
                return cympl.interpreter.TValue(BuiltinType.BOOL, left.value != right.value)
            }
        }

        object Gt : cympl.interpreter.BinaryOperation.Comparison {
            override fun apply(left: cympl.interpreter.TValue, right: cympl.interpreter.TValue): cympl.interpreter.TValue {
                if (left.type != right.type) {
                    throw cympl.interpreter.InterpretException("cannot compare ${left.type} to ${right.type}")
                }
                return when (left.type) {
                    BuiltinType.INT -> cympl.interpreter.TValue(BuiltinType.BOOL, left.asInt() > right.asInt())
                    BuiltinType.FLOAT -> cympl.interpreter.TValue(BuiltinType.BOOL, left.asDouble() > right.asDouble())
                    BuiltinType.STRING -> cympl.interpreter.TValue(BuiltinType.BOOL, left.asString() > right.asString())
                    else -> throw cympl.interpreter.InterpretException("cannot compare ${left.type} to ${right.type}")
                }
            }
        }

        object Lt : cympl.interpreter.BinaryOperation.Comparison {
            override fun apply(left: cympl.interpreter.TValue, right: cympl.interpreter.TValue): cympl.interpreter.TValue {
                if (left.type != right.type) {
                    throw cympl.interpreter.InterpretException("cannot compare ${left.type} to ${right.type}")
                }
                return when (left.type) {
                    BuiltinType.INT -> cympl.interpreter.TValue(BuiltinType.BOOL, left.asInt() < right.asInt())
                    BuiltinType.FLOAT -> cympl.interpreter.TValue(BuiltinType.BOOL, left.asDouble() < right.asDouble())
                    BuiltinType.STRING -> cympl.interpreter.TValue(BuiltinType.BOOL, left.asString() < right.asString())
                    else -> throw cympl.interpreter.InterpretException("cannot compare ${left.type} to ${right.type}")
                }
            }
        }

        object Leq : cympl.interpreter.BinaryOperation.Comparison {
            override fun apply(left: cympl.interpreter.TValue, right: cympl.interpreter.TValue): cympl.interpreter.TValue {
                if (left.type != right.type) {
                    throw cympl.interpreter.InterpretException("cannot compare ${left.type} to ${right.type}")
                }
                return when (left.type) {
                    BuiltinType.INT -> cympl.interpreter.TValue(BuiltinType.BOOL, left.asInt() <= right.asInt())
                    BuiltinType.FLOAT -> cympl.interpreter.TValue(BuiltinType.BOOL, left.asDouble() <= right.asDouble())
                    BuiltinType.STRING -> cympl.interpreter.TValue(
                        BuiltinType.BOOL,
                        left.asString() <= right.asString()
                    )
                    else -> throw cympl.interpreter.InterpretException("cannot compare ${left.type} to ${right.type}")
                }
            }
        }

        object Geq : cympl.interpreter.BinaryOperation.Comparison {
            override fun apply(left: cympl.interpreter.TValue, right: cympl.interpreter.TValue): cympl.interpreter.TValue {
                if (left.type != right.type) {
                    throw cympl.interpreter.InterpretException("cannot compare ${left.type} to ${right.type}")
                }
                return when (left.type) {
                    BuiltinType.INT -> cympl.interpreter.TValue(BuiltinType.BOOL, left.asInt() >= right.asInt())
                    BuiltinType.FLOAT -> cympl.interpreter.TValue(BuiltinType.BOOL, left.asDouble() >= right.asDouble())
                    BuiltinType.STRING -> cympl.interpreter.TValue(
                        BuiltinType.BOOL,
                        left.asString() >= right.asString()
                    )
                    else -> throw cympl.interpreter.InterpretException("cannot compare ${left.type} to ${right.type}")
                }
            }
        }
    }
}
