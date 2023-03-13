package demo.parser.interpret

import demo.parser.domain.*
import kotlin.math.pow

sealed interface BinaryOperation {

    fun apply(left: TValue, right: TValue): TValue

    sealed interface Arithmetic : BinaryOperation {

        object Plus : Arithmetic {
            override fun apply(left: TValue, right: TValue): TValue = when (left.type) {
                BuiltinType.INT -> when (right.type) {
                    BuiltinType.INT -> TValue(BuiltinType.INT, left.asInt() + right.asInt())
                    BuiltinType.FLOAT -> TValue(BuiltinType.FLOAT, left.asInt() + right.asDouble())
                    BuiltinType.STRING -> TValue(BuiltinType.STRING, left.asString() + right.asString())
                    else -> throw InterpretException("cannot add ${left.type} to ${right.type}")
                }

                BuiltinType.FLOAT -> when (right.type) {
                    BuiltinType.INT -> TValue(BuiltinType.FLOAT, left.asDouble() + right.asInt())
                    BuiltinType.FLOAT -> TValue(BuiltinType.FLOAT, left.asDouble() + right.asDouble())
                    BuiltinType.STRING -> TValue(BuiltinType.STRING, left.asString() + right.asString())
                    else -> throw InterpretException("cannot add ${left.type} to ${right.type}")
                }

                BuiltinType.STRING -> TValue(BuiltinType.STRING, left.asString() + right.asString())
                else -> {
                    throw InterpretException("cannot add ${left.type} to ${right.type}")
                }
            }
        }

        object Minus : Arithmetic {
            override fun apply(left: TValue, right: TValue): TValue = when (left.type) {
                BuiltinType.INT -> when (right.type) {
                    BuiltinType.INT -> TValue(BuiltinType.INT, left.asInt() - right.asInt())
                    BuiltinType.FLOAT -> TValue(BuiltinType.FLOAT, left.asDouble() - right.asDouble())
                    else -> throw InterpretException("cannot subtract ${right.type} from ${left.type}")
                }

                BuiltinType.FLOAT -> when (right.type) {
                    BuiltinType.INT -> TValue(BuiltinType.FLOAT, left.asDouble() - right.asDouble())
                    BuiltinType.FLOAT -> TValue(BuiltinType.FLOAT, left.asDouble() - right.asDouble())
                    else -> throw InterpretException("cannot subtract ${right.type} from ${left.type}")
                }

                else -> throw InterpretException("cannot subtract ${right.type} from ${left.type}")
            }
        }

        object Times : Arithmetic {
            override fun apply(left: TValue, right: TValue): TValue = when (left.type) {
                BuiltinType.INT -> when (right.type) {
                    BuiltinType.INT -> TValue(BuiltinType.INT, left.asInt() * right.asInt())
                    BuiltinType.FLOAT -> TValue(BuiltinType.FLOAT, left.asInt() * right.asDouble())
                    else -> throw InterpretException("cannot multiply ${left.type} by ${right.type}")
                }

                BuiltinType.FLOAT -> when (right.type) {
                    BuiltinType.INT -> TValue(BuiltinType.FLOAT, left.asDouble() * right.asInt())
                    BuiltinType.FLOAT -> TValue(BuiltinType.FLOAT, left.asDouble() * right.asDouble())
                    else -> throw InterpretException("cannot multiply ${left.type} by ${right.type}")
                }

                else -> throw InterpretException("cannot multiply ${left.type} by ${right.type}")
            }
        }

        object Div : Arithmetic {
            override fun apply(left: TValue, right: TValue): TValue = when (left.type) {
                BuiltinType.INT -> when (right.type) {
                    BuiltinType.INT -> TValue(BuiltinType.INT, left.asInt() / right.asInt())
                    BuiltinType.FLOAT -> TValue(BuiltinType.FLOAT, left.asInt() / right.asDouble())
                    else -> throw InterpretException("cannot divide ${left.type} by ${right.type}")
                }

                BuiltinType.FLOAT -> when (right.type) {
                    BuiltinType.INT -> TValue(BuiltinType.FLOAT, left.asDouble() / right.asDouble())
                    BuiltinType.FLOAT -> TValue(BuiltinType.FLOAT, left.asDouble() / right.asDouble())
                    else -> throw InterpretException("cannot divide ${left.type} by ${right.type}")
                }

                else -> throw InterpretException("cannot divide ${left.type} by ${right.type}")
            }
        }

        object Rem : Arithmetic {
            override fun apply(left: TValue, right: TValue): TValue = when (left.type) {
                BuiltinType.INT -> when (right.type) {
                    BuiltinType.INT -> TValue(BuiltinType.INT, left.asInt() % right.asInt())
                    BuiltinType.FLOAT -> TValue(BuiltinType.FLOAT, left.asInt() % right.asDouble())
                    else -> throw InterpretException("cannot  ${left.type} by ${right.type}")
                }

                BuiltinType.FLOAT -> when (right.type) {
                    BuiltinType.INT -> TValue(BuiltinType.FLOAT, left.asDouble() % right.asDouble())
                    BuiltinType.FLOAT -> TValue(BuiltinType.FLOAT, left.asDouble() % right.asDouble())
                    else -> throw InterpretException("cannot divide ${left.type} by ${right.type}")
                }

                else -> throw InterpretException("cannot divide ${left.type} by ${right.type}")
            }
        }

        object Pow : Arithmetic {
            override fun apply(left: TValue, right: TValue): TValue {
                val leftValue = left.asDouble()
                val rightValue = right.asDouble()
                return left.withValue(leftValue.pow(rightValue))
            }
        }
    }

    sealed class Logical : BinaryOperation {
        override fun apply(left: TValue, right: TValue): TValue {
            val leftValue = left.asBoolean()
            val rightValue = right.asBoolean()
            return when (this) {
                is And -> left.withValue(leftValue && rightValue)
                is Or -> left.withValue(leftValue || rightValue)
            }
        }

        object And : Logical()
        object Or : Logical()
    }

    sealed interface Comparison : BinaryOperation {
        object Eq : Comparison {
            override fun apply(left: TValue, right: TValue): TValue {
                if (left.type != right.type) {
                    throw InterpretException("cannot compare ${left.type} to ${right.type}")
                }
                return TValue(BuiltinType.BOOL, left.value == right.value)
            }
        }

        object Neq : Comparison {
            override fun apply(left: TValue, right: TValue): TValue {
                if (left.type != right.type) {
                    throw InterpretException("cannot compare ${left.type} to ${right.type}")
                }
                return TValue(BuiltinType.BOOL, left.value != right.value)
            }
        }

        object Gt : Comparison {
            override fun apply(left: TValue, right: TValue): TValue {
                if (left.type != right.type) {
                    throw InterpretException("cannot compare ${left.type} to ${right.type}")
                }
                return when (left.type) {
                    BuiltinType.INT -> TValue(BuiltinType.BOOL, left.asInt() > right.asInt())
                    BuiltinType.FLOAT -> TValue(BuiltinType.BOOL, left.asDouble() > right.asDouble())
                    BuiltinType.STRING -> TValue(BuiltinType.BOOL, left.asString() > right.asString())
                    else -> throw InterpretException("cannot compare ${left.type} to ${right.type}")
                }
            }
        }
        object Lt : Comparison {
            override fun apply(left: TValue, right: TValue): TValue {
                if (left.type != right.type) {
                    throw InterpretException("cannot compare ${left.type} to ${right.type}")
                }
                return when (left.type) {
                    BuiltinType.INT -> TValue(BuiltinType.BOOL, left.asInt() < right.asInt())
                    BuiltinType.FLOAT -> TValue(BuiltinType.BOOL, left.asDouble() < right.asDouble())
                    BuiltinType.STRING -> TValue(BuiltinType.BOOL, left.asString() < right.asString())
                    else -> throw InterpretException("cannot compare ${left.type} to ${right.type}")
                }
            }
        }
        object Leq : Comparison {
            override fun apply(left: TValue, right: TValue): TValue {
                if (left.type != right.type) {
                    throw InterpretException("cannot compare ${left.type} to ${right.type}")
                }
                return when (left.type) {
                    BuiltinType.INT -> TValue(BuiltinType.BOOL, left.asInt() <= right.asInt())
                    BuiltinType.FLOAT -> TValue(BuiltinType.BOOL, left.asDouble() <= right.asDouble())
                    BuiltinType.STRING -> TValue(BuiltinType.BOOL, left.asString() <= right.asString())
                    else -> throw InterpretException("cannot compare ${left.type} to ${right.type}")
                }
            }
        }
        object Geq : Comparison {
            override fun apply(left: TValue, right: TValue): TValue {
                if (left.type != right.type) {
                    throw InterpretException("cannot compare ${left.type} to ${right.type}")
                }
                return when (left.type) {
                    BuiltinType.INT -> TValue(BuiltinType.BOOL, left.asInt() >= right.asInt())
                    BuiltinType.FLOAT -> TValue(BuiltinType.BOOL, left.asDouble() >= right.asDouble())
                    BuiltinType.STRING -> TValue(BuiltinType.BOOL, left.asString() >= right.asString())
                    else -> throw InterpretException("cannot compare ${left.type} to ${right.type}")
                }
            }
        }
    }
}
