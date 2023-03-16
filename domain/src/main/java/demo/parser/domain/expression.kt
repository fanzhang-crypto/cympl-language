package demo.parser.domain

sealed interface Expression: Typed {

    fun toStatement(): Statement = Statement.ExpressionStatement(this)

    sealed interface BinaryExpression : Expression {
        val left: Expression
        val right: Expression
    }

    sealed class ArithmeticExpression(
        final override val left: Expression,
        final override val right: Expression
    ) : BinaryExpression {
        override val resolvedType = compatibleNumberType(left.resolvedType, right.resolvedType)
    }

    sealed class ComparisonExpression(
        final override val left: Expression,
        final override val right: Expression
    ) : BinaryExpression {
        override val resolvedType = BuiltinType.BOOL
    }

    data class Parenthesized(val expr: Expression) : Expression {
        override val resolvedType = expr.resolvedType
        override fun toString() = "($expr)"
    }

    data class BoolLiteral(val value: Boolean) : Expression {
        override val resolvedType = BuiltinType.BOOL
        override fun toString() = value.toString()
    }

    data class IntLiteral(val value: Int) : Expression {
        override val resolvedType = BuiltinType.INT
        override fun toString() = value.toString()
    }

    data class FloatLiteral(val value: Double) : Expression {
        override val resolvedType = BuiltinType.FLOAT
        override fun toString() = value.toString()
    }

    data class StringLiteral(val value: String) : Expression {
        override val resolvedType = BuiltinType.STRING
        override fun toString() = "\"$value\""
    }

    data class ArrayLiteral(val elements: List<Expression>) : Expression {
        override val resolvedType = BuiltinType.ARRAY(
            elements.firstOrNull()?.resolvedType ?: BuiltinType.VOID
        )

        override fun toString() = "[${elements.joinToString(", ")}]"
    }

    data class Variable(val id: String, val type: BuiltinType) : Expression {
        override val resolvedType = type
        override fun toString() = id
    }

    class Power(left: Expression, right: Expression) : ArithmeticExpression(left, right) {
        override fun toString() = "$left ^ $right"
    }

    class Multiplication(left: Expression, right: Expression) : ArithmeticExpression(left, right) {
        override fun toString() = "$left * $right"
    }

    class Division(left: Expression, right: Expression) : ArithmeticExpression(left, right) {
        override fun toString() = "$left / $right"
    }

    class Remainder(left: Expression, right: Expression) : ArithmeticExpression(left, right) {
        override fun toString() = "$left % $right"
    }

    class Addition(left: Expression, right: Expression) : ArithmeticExpression(left, right) {
        override val resolvedType: BuiltinType
            get() =
                if (left.resolvedType == BuiltinType.STRING || right.resolvedType == BuiltinType.STRING)
                    BuiltinType.STRING
                else super.resolvedType

        override fun toString() = "$left + $right"
    }

    class Subtraction(left: Expression, right: Expression) : ArithmeticExpression(left, right) {
        override fun toString() = "$left - $right"
    }

    class Negation(val expr: Expression) : Expression {
        override val resolvedType = expr.resolvedType
        override fun toString() = "-$expr"
    }

    class Increment(val expr: Expression, val postfix: Boolean) : Expression {
        override val resolvedType = expr.resolvedType
        override fun toString() = if (postfix) "$expr++" else "++$expr"
    }

    class Decrement(val expr: Expression, val postfix: Boolean) : Expression {
        override val resolvedType = expr.resolvedType
        override fun toString() = if (postfix) "$expr--" else "--$expr"
    }

    class Equality(left: Expression, right: Expression) : ComparisonExpression(left, right) {
        override fun toString() = "$left == $right"
    }

    class GreaterThan(left: Expression, right: Expression) : ComparisonExpression(left, right) {
        override fun toString() = "$left > $right"
    }

    class LessThan(left: Expression, right: Expression) : ComparisonExpression(left, right) {
        override val resolvedType = BuiltinType.BOOL
        override fun toString() = "$left < $right"
    }

    class GreaterThanOrEqual(left: Expression, right: Expression) : ComparisonExpression(left, right) {
        override val resolvedType = BuiltinType.BOOL
        override fun toString() = "$left >= $right"
    }

    class LessThanOrEqual(left: Expression, right: Expression) : ComparisonExpression(left, right) {
        override fun toString() = "$left <= $right"
    }

    class Inequality(left: Expression, right: Expression) : ComparisonExpression(left, right) {
        override fun toString() = "$left != $right"
    }

    data class And(val left: Expression, val right: Expression) : Expression {
        override val resolvedType = BuiltinType.BOOL
        override fun toString() = "$left && $right"
    }

    data class Or(val left: Expression, val right: Expression) : Expression {
        override val resolvedType = BuiltinType.BOOL
        override fun toString() = "$left || $right"
    }

    data class Not(val expr: Expression) : Expression {
        override val resolvedType = BuiltinType.BOOL
        override fun toString() = "!$expr"
    }

    data class FunctionCall(val id: String, val args: List<Expression>, val type: BuiltinType) : Expression {
        override val resolvedType = type
        override fun toString() = "$id(${args.joinToString(", ")})"
    }

    data class Index(val arrayExpr: Expression, val indexExpr: Expression) : Expression {
        override val resolvedType = (arrayExpr.resolvedType as BuiltinType.ARRAY).elementType
        override fun toString() = "$arrayExpr[$indexExpr]"
    }

    data class Property(val expr: Expression, val propertyName: String, val type: BuiltinType) : Expression {
        override val resolvedType = type
        override fun toString() = "$expr.$propertyName"
    }

    companion object {
        private fun compatibleNumberType(left: BuiltinType, right: BuiltinType): BuiltinType {
            return when {
                left == BuiltinType.FLOAT || right == BuiltinType.FLOAT -> BuiltinType.FLOAT
                left == BuiltinType.INT && right == BuiltinType.INT -> BuiltinType.INT
                else -> BuiltinType.VOID
            }
        }
    }
}



