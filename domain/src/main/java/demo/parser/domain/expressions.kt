package demo.parser.domain

data class Program(val expressions: List<Expression>)

sealed interface Expression

data class Declaration(val id: String, val type: String, val value: Expression) : Expression {
    override fun toString() = "$id:$type = $value"
}

data class Assignment(val id: String, val value: Expression) : Expression {
    override fun toString() = "$id = $value"
}

data class Parenthesized(val expr: Expression) : Expression {
    override fun toString() = "($expr)"
}

data class Number(val int: Int) : Expression {
    override fun toString() = int.toString()
}

data class Variable(val id: String) : Expression {
    override fun toString() = id
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

