package demo.parser.domain

sealed interface Expression {

    fun toStatement(): Statement = Statement.ExpressionStatement(this)

    data class Parenthesized(val expr: Expression) : Expression {
        override fun toString() = "($expr)"
    }

    data class BoolLiteral(val value: Boolean) : Expression {
        override fun toString() = value.toString()
    }

    data class IntLiteral(val value: Int) : Expression {
        override fun toString() = value.toString()
    }

    data class FloatLiteral(val value: Double) : Expression {
        override fun toString() = value.toString()
    }

    data class StringLiteral(val value: String) : Expression {
        override fun toString() = "\"$value\""
    }

    data class ArrayLiteral(val elements: List<Expression>) : Expression {
        override fun toString() = "[${elements.joinToString(", ")}]"
    }

    data class Variable(val id: String) : Expression {
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

    data class Remainder(val left: Expression, val right: Expression) : Expression {
        override fun toString() = "$left % $right"
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

    data class Increment(val expr: Expression, val postfix: Boolean) : Expression {
        override fun toString() = if (postfix) "$expr++" else "++$expr"
    }

    data class Decrement(val expr: Expression, val postfix: Boolean) : Expression {
        override fun toString() = if (postfix) "$expr--" else "--$expr"
    }

    data class Equality(val left: Expression, val right: Expression) : Expression {
        override fun toString() = "$left == $right"
    }

    data class GreaterThan(val left: Expression, val right: Expression) : Expression {
        override fun toString() = "$left > $right"
    }

    data class LessThan(val left: Expression, val right: Expression) : Expression {
        override fun toString() = "$left < $right"
    }

    data class GreaterThanOrEqual(val left: Expression, val right: Expression) : Expression {
        override fun toString() = "$left >= $right"
    }

    data class LessThanOrEqual(val left: Expression, val right: Expression) : Expression {
        override fun toString() = "$left <= $right"
    }

    data class Inequality(val left: Expression, val right: Expression) : Expression {
        override fun toString() = "$left != $right"
    }

    data class And(val left: Expression, val right: Expression) : Expression {
        override fun toString() = "$left && $right"
    }

    data class Or(val left: Expression, val right: Expression) : Expression {
        override fun toString() = "$left || $right"
    }

    data class Not(val expr: Expression) : Expression {
        override fun toString() = "!$expr"
    }

    data class FunctionCall(val id: String, val args: List<Expression>) : Expression {
        override fun toString() = "$id(${args.joinToString(", ")})"
    }

    data class Index(val arrayExpr: Expression, val indexExpr: Expression) : Expression {
        override fun toString() = "$arrayExpr[$indexExpr]"
    }

    data class Property(val expr: Expression, val propertyName: String) : Expression {
        override fun toString() = "$expr.$propertyName"
    }
}



