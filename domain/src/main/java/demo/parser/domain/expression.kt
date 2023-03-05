package demo.parser.domain

data class Program(val expressions: List<Expression>)

sealed interface Expression {

    data class Declaration(val id: kotlin.String, val type: VariableType, val value: Expression) : Expression {
        override fun toString() = "$id:${type.name} = $value"
    }

    data class Assignment(val id: kotlin.String, val value: Expression) : Expression {
        override fun toString() = "$id = $value"
    }

    data class Parenthesized(val expr: Expression) : Expression {
        override fun toString() = "($expr)"
    }

    data class Int(val value: kotlin.Int) : Expression {
        override fun toString() = value.toString()
    }

    data class Float(val value: Double) : Expression {
        override fun toString() = value.toString()
    }

    data class String(val value: kotlin.String) : Expression {
        override fun toString() = "\"$value\""
    }

    data class Variable(val id: kotlin.String) : Expression {
        override fun toString() = id
    }

    data class Power(val left: Expression, val right: Expression) : Expression {
        override fun toString() = "$left ^ $right"
    }

    data class Multiplication(val left: Expression, val right: Expression) : Expression {
        override fun toString() = "$left * $right"
    }

    data class Division(val left: Expression, val right: Expression) : Expression {
        override fun toString() = "$left / $right"
    }

    data class Addition(val left: Expression, val right: Expression) : Expression {
        override fun toString() = "$left + $right"
    }

    data class Subtraction(val left: Expression, val right: Expression) : Expression {
        override fun toString() = "$left - $right"
    }

    data class Negation(val expr: Expression) : Expression {
        override fun toString() = "-$expr"
    }
}


