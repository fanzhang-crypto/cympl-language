package demo.parser.domain

sealed interface UnaryOperation {

    fun apply(value: TValue): TValue

    object Plus : UnaryOperation {
        override fun apply(value: TValue): TValue = when (value.type) {
            VariableType.INT -> value
            VariableType.FLOAT -> value
            else -> throw SemanticException("cannot apply unary + to ${value.type}")
        }
    }

    object Minus : UnaryOperation {
        override fun apply(value: TValue): TValue = when (value.type) {
            VariableType.INT -> TValue(VariableType.INT, -value.asInt())
            VariableType.FLOAT -> TValue(VariableType.FLOAT, -value.asDouble())
            else -> throw SemanticException("cannot apply unary - to ${value.type}")
        }
    }

    object Not : UnaryOperation {
        override fun apply(value: TValue): TValue = when (value.type) {
            VariableType.BOOL -> TValue(VariableType.BOOL, !value.asBoolean())
            else -> throw SemanticException("cannot apply logical not to ${value.type}")
        }
    }
}
