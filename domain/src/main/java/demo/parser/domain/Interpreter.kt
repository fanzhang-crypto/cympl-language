package demo.parser.domain

import kotlin.math.pow

class Interpreter {

    private val localVariables = mutableMapOf<String, TValue>()

    fun interpret(program: Program): Sequence<String> = sequence {
        for (expr in program.expressions) {
            yield("$expr => ${formatTValue(evaluate(expr))}")
        }
        yield("environment:")
        yield(formatCurrentEnv())
    }

    private fun formatTValue(tvalue: TValue) = when(tvalue.type) {
        VariableType.STRING -> "\"${tvalue.value}\""
        else -> tvalue.value.toString()
    }

    private fun formatCurrentEnv() = localVariables
        .map { (k, tvalue) -> "$k:${tvalue.type} = ${formatTValue(tvalue)}" }
        .joinToString(", ")

    private fun evaluate(expr: Expression): TValue = when (expr) {
        is Expression.Declaration -> {
            val id = expr.id
            if (localVariables.contains(id)) {
                throw SemanticException("variable $id already declared")
            }
            val type = expr.type
            evaluate(expr.value)
                .also { v ->
                    type.checkValue(v.value)
                    localVariables[id] = TValue(type, v.value)
                }
        }
        is Expression.Assignment -> {
            val id = expr.id
            val variable = localVariables[id]
                ?: throw SemanticException("variable $id not defined")

            val value = evaluate(expr.value).value
            variable.withValue(value).also { localVariables[id] = it }
        }

        is Expression.Parenthesized -> evaluate(expr.expr)

        is Expression.Addition -> {
            val left = evaluate(expr.left)
            val right = evaluate(expr.right)
            when (left.type) {
                VariableType.INT -> when (right.type) {
                    VariableType.INT -> TValue(VariableType.INT, left.asInt() + right.asInt())
                    VariableType.FLOAT -> TValue(VariableType.FLOAT, left.asInt() + right.asDouble())
                    VariableType.STRING -> TValue(VariableType.STRING, left.asString() + right.asString())
                }
                VariableType.FLOAT -> when (right.type) {
                    VariableType.INT -> TValue(VariableType.FLOAT, left.asDouble() + right.asInt())
                    VariableType.FLOAT -> TValue(VariableType.FLOAT, left.asDouble() + right.asDouble())
                    VariableType.STRING -> TValue(VariableType.STRING, left.asString() + right.asString())
                }
                VariableType.STRING -> TValue(VariableType.STRING, left.asString() + right.asString())
            }
        }
        is Expression.Subtraction -> {
            val left = evaluate(expr.left)
            val right = evaluate(expr.right)
            when (left.type) {
                VariableType.INT -> when (right.type) {
                    VariableType.INT -> TValue(VariableType.INT, left.asInt() - right.asInt())
                    VariableType.FLOAT -> TValue(VariableType.FLOAT, left.asDouble() - right.asDouble())
                    VariableType.STRING -> throw SemanticException("cannot subtract string from int")
                }
                VariableType.FLOAT -> when (right.type) {
                    VariableType.INT -> TValue(VariableType.FLOAT, left.asDouble() - right.asDouble())
                    VariableType.FLOAT -> TValue(VariableType.FLOAT, left.asDouble() - right.asDouble())
                    VariableType.STRING -> throw SemanticException("cannot subtract string from float")
                }
                VariableType.STRING -> throw SemanticException("cannot subtract string from string")
            }
        }
        is Expression.Multiplication -> {
            val left = evaluate(expr.left)
            val right = evaluate(expr.right)
            when (left.type) {
                VariableType.INT -> when(right.type) {
                    VariableType.INT -> TValue(VariableType.INT, left.asInt() * right.asInt())
                    VariableType.FLOAT -> TValue(VariableType.FLOAT, left.asInt() * right.asDouble())
                    VariableType.STRING -> throw SemanticException("cannot multiply string by int")
                }
                VariableType.FLOAT -> when(right.type) {
                    VariableType.INT -> TValue(VariableType.FLOAT, left.asDouble() * right.asInt())
                    VariableType.FLOAT -> TValue(VariableType.FLOAT, left.asDouble() * right.asDouble())
                    VariableType.STRING -> throw SemanticException("cannot multiply string by float")
                }
                VariableType.STRING -> throw SemanticException("cannot multiply string")
            }
        }
        is Expression.Division -> {
            val left = evaluate(expr.left)
            val right = evaluate(expr.right)
            when (left.type) {
                VariableType.INT -> when(right.type) {
                    VariableType.INT -> TValue(VariableType.INT, left.asInt() / right.asInt())
                    VariableType.FLOAT -> TValue(VariableType.FLOAT, left.asInt() / right.asDouble())
                    VariableType.STRING -> throw SemanticException("cannot divide int by string")
                }
                VariableType.FLOAT -> when(right.type) {
                    VariableType.INT -> TValue(VariableType.FLOAT, left.asDouble() / right.asDouble())
                    VariableType.FLOAT -> TValue(VariableType.FLOAT, left.asDouble() / right.asDouble())
                    VariableType.STRING -> throw SemanticException("cannot divide float by string")
                }
                VariableType.STRING -> throw SemanticException("cannot divide string")
            }
        }

        is Expression.Power -> {
            val left = evaluate(expr.left).asDouble()
            val right = evaluate(expr.right).asDouble()
            TValue(VariableType.FLOAT, left.pow(right))
        }

        is Expression.Negation -> {
            val tvalue = evaluate(expr.expr)
            when (tvalue.type) {
                VariableType.INT -> TValue(VariableType.INT, -tvalue.asInt())
                VariableType.FLOAT -> TValue(VariableType.FLOAT, -tvalue.asDouble())
                else -> throw SemanticException("cannot negate $tvalue")
            }
        }

        is Expression.Variable -> {
            val id = expr.id
            localVariables[id] ?: throw SemanticException("variable $id not defined")
        }

        is Expression.Float -> TValue(VariableType.FLOAT, expr.value)
        is Expression.Int -> TValue(VariableType.INT, expr.value)
        is Expression.String -> TValue(VariableType.STRING, expr.value)
    }
}
