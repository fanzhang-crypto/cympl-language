package demo.parser.domain

import kotlin.math.pow

sealed interface BinaryOperation {

    fun apply(left: TValue, right: TValue): TValue

    sealed interface Arithmetic : BinaryOperation {

        object Plus : Arithmetic {
            override fun apply(left: TValue, right: TValue): TValue = when (left.type) {
                VariableType.INT -> when (right.type) {
                    VariableType.INT -> TValue(VariableType.INT, left.asInt() + right.asInt())
                    VariableType.FLOAT -> TValue(VariableType.FLOAT, left.asInt() + right.asDouble())
                    VariableType.STRING -> TValue(VariableType.STRING, left.asString() + right.asString())
                    else -> throw SemanticException("cannot add ${left.type} to ${right.type}")
                }

                VariableType.FLOAT -> when (right.type) {
                    VariableType.INT -> TValue(VariableType.FLOAT, left.asDouble() + right.asInt())
                    VariableType.FLOAT -> TValue(VariableType.FLOAT, left.asDouble() + right.asDouble())
                    VariableType.STRING -> TValue(VariableType.STRING, left.asString() + right.asString())
                    else -> throw SemanticException("cannot add ${left.type} to ${right.type}")
                }

                VariableType.STRING -> TValue(VariableType.STRING, left.asString() + right.asString())
                else -> {
                    throw SemanticException("cannot add ${left.type} to ${right.type}")
                }
            }
        }

        object Minus : Arithmetic {
            override fun apply(left: TValue, right: TValue): TValue = when (left.type) {
                VariableType.INT -> when (right.type) {
                    VariableType.INT -> TValue(VariableType.INT, left.asInt() - right.asInt())
                    VariableType.FLOAT -> TValue(VariableType.FLOAT, left.asDouble() - right.asDouble())
                    else -> throw SemanticException("cannot subtract ${right.type} from ${left.type}")
                }

                VariableType.FLOAT -> when (right.type) {
                    VariableType.INT -> TValue(VariableType.FLOAT, left.asDouble() - right.asDouble())
                    VariableType.FLOAT -> TValue(VariableType.FLOAT, left.asDouble() - right.asDouble())
                    else -> throw SemanticException("cannot subtract ${right.type} from ${left.type}")
                }

                else -> throw SemanticException("cannot subtract ${right.type} from ${left.type}")
            }
        }

        object Times : Arithmetic {
            override fun apply(left: TValue, right: TValue): TValue = when (left.type) {
                VariableType.INT -> when (right.type) {
                    VariableType.INT -> TValue(VariableType.INT, left.asInt() * right.asInt())
                    VariableType.FLOAT -> TValue(VariableType.FLOAT, left.asInt() * right.asDouble())
                    else -> throw SemanticException("cannot multiply ${left.type} by ${right.type}")
                }

                VariableType.FLOAT -> when (right.type) {
                    VariableType.INT -> TValue(VariableType.FLOAT, left.asDouble() * right.asInt())
                    VariableType.FLOAT -> TValue(VariableType.FLOAT, left.asDouble() * right.asDouble())
                    else -> throw SemanticException("cannot multiply ${left.type} by ${right.type}")
                }

                else -> throw SemanticException("cannot multiply ${left.type} by ${right.type}")
            }
        }

        object Div : Arithmetic {
            override fun apply(left: TValue, right: TValue): TValue = when (left.type) {
                VariableType.INT -> when (right.type) {
                    VariableType.INT -> TValue(VariableType.INT, left.asInt() / right.asInt())
                    VariableType.FLOAT -> TValue(VariableType.FLOAT, left.asInt() / right.asDouble())
                    else -> throw SemanticException("cannot divide ${left.type} by ${right.type}")
                }

                VariableType.FLOAT -> when (right.type) {
                    VariableType.INT -> TValue(VariableType.FLOAT, left.asDouble() / right.asDouble())
                    VariableType.FLOAT -> TValue(VariableType.FLOAT, left.asDouble() / right.asDouble())
                    else -> throw SemanticException("cannot divide ${left.type} by ${right.type}")
                }

                else -> throw SemanticException("cannot divide ${left.type} by ${right.type}")
            }
        }

        object Rem : Arithmetic {
            override fun apply(left: TValue, right: TValue): TValue = when (left.type) {
                VariableType.INT -> when (right.type) {
                    VariableType.INT -> TValue(VariableType.INT, left.asInt() % right.asInt())
                    VariableType.FLOAT -> TValue(VariableType.FLOAT, left.asInt() % right.asDouble())
                    else -> throw SemanticException("cannot  ${left.type} by ${right.type}")
                }

                VariableType.FLOAT -> when (right.type) {
                    VariableType.INT -> TValue(VariableType.FLOAT, left.asDouble() % right.asDouble())
                    VariableType.FLOAT -> TValue(VariableType.FLOAT, left.asDouble() % right.asDouble())
                    else -> throw SemanticException("cannot divide ${left.type} by ${right.type}")
                }

                else -> throw SemanticException("cannot divide ${left.type} by ${right.type}")
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
                    throw SemanticException("cannot compare ${left.type} to ${right.type}")
                }
                return TValue(VariableType.BOOL, left.value == right.value)
            }
        }

        object Neq : Comparison {
            override fun apply(left: TValue, right: TValue): TValue {
                if (left.type != right.type) {
                    throw SemanticException("cannot compare ${left.type} to ${right.type}")
                }
                return TValue(VariableType.BOOL, left.value != right.value)
            }
        }

        object Gt : Comparison {
            override fun apply(left: TValue, right: TValue): TValue {
                if (left.type != right.type) {
                    throw SemanticException("cannot compare ${left.type} to ${right.type}")
                }
                return when (left.type) {
                    VariableType.INT -> TValue(VariableType.BOOL, left.asInt() > right.asInt())
                    VariableType.FLOAT -> TValue(VariableType.BOOL, left.asDouble() > right.asDouble())
                    VariableType.STRING -> TValue(VariableType.BOOL, left.asString() > right.asString())
                    else -> throw SemanticException("cannot compare ${left.type} to ${right.type}")
                }
            }
        }
        object Lt : Comparison {
            override fun apply(left: TValue, right: TValue): TValue {
                if (left.type != right.type) {
                    throw SemanticException("cannot compare ${left.type} to ${right.type}")
                }
                return when (left.type) {
                    VariableType.INT -> TValue(VariableType.BOOL, left.asInt() < right.asInt())
                    VariableType.FLOAT -> TValue(VariableType.BOOL, left.asDouble() < right.asDouble())
                    VariableType.STRING -> TValue(VariableType.BOOL, left.asString() < right.asString())
                    else -> throw SemanticException("cannot compare ${left.type} to ${right.type}")
                }
            }
        }
        object Leq : Comparison {
            override fun apply(left: TValue, right: TValue): TValue {
                if (left.type != right.type) {
                    throw SemanticException("cannot compare ${left.type} to ${right.type}")
                }
                return when (left.type) {
                    VariableType.INT -> TValue(VariableType.BOOL, left.asInt() <= right.asInt())
                    VariableType.FLOAT -> TValue(VariableType.BOOL, left.asDouble() <= right.asDouble())
                    VariableType.STRING -> TValue(VariableType.BOOL, left.asString() <= right.asString())
                    else -> throw SemanticException("cannot compare ${left.type} to ${right.type}")
                }
            }
        }
        object Geq : Comparison {
            override fun apply(left: TValue, right: TValue): TValue {
                if (left.type != right.type) {
                    throw SemanticException("cannot compare ${left.type} to ${right.type}")
                }
                return when (left.type) {
                    VariableType.INT -> TValue(VariableType.BOOL, left.asInt() >= right.asInt())
                    VariableType.FLOAT -> TValue(VariableType.BOOL, left.asDouble() >= right.asDouble())
                    VariableType.STRING -> TValue(VariableType.BOOL, left.asString() >= right.asString())
                    else -> throw SemanticException("cannot compare ${left.type} to ${right.type}")
                }
            }
        }
    }
}
