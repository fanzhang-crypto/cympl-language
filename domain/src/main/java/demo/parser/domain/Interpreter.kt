package demo.parser.domain

import kotlin.math.pow

class Interpreter {

    private val localVariables = mutableMapOf<String, TValue>()

    fun interpret(program: Program): Sequence<String> = sequence {
        for (expr in program.statements) {
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

    private fun evaluate(stat: Statement): TValue = when (stat) {
        is Statement.VariableDeclaration -> {
            val id = stat.id
            if (localVariables.contains(id)) {
                throw SemanticException("variable $id already declared")
            }
            val type = stat.type
            evaluate(stat.expr)
                .also { v ->
                    type.checkValue(v.value)
                    localVariables[id] = TValue(type, v.value)
                }
        }
        is Statement.Assignment -> {
            val id = stat.id
            val variable = localVariables[id]
                ?: throw SemanticException("variable $id not defined")

            val value = evaluate(stat.expr).value
            variable.withValue(value).also { localVariables[id] = it }
        }

        is Expression.Parenthesized -> evaluate(stat.expr)

        is Expression.Addition -> {
            val left = evaluate(stat.left)
            val right = evaluate(stat.right)
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
            val left = evaluate(stat.left)
            val right = evaluate(stat.right)
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
            val left = evaluate(stat.left)
            val right = evaluate(stat.right)
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
            val left = evaluate(stat.left)
            val right = evaluate(stat.right)
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
            val left = evaluate(stat.left).asDouble()
            val right = evaluate(stat.right).asDouble()
            TValue(VariableType.FLOAT, left.pow(right))
        }

        is Expression.Negation -> {
            val tvalue = evaluate(stat.expr)
            when (tvalue.type) {
                VariableType.INT -> TValue(VariableType.INT, -tvalue.asInt())
                VariableType.FLOAT -> TValue(VariableType.FLOAT, -tvalue.asDouble())
                else -> throw SemanticException("cannot negate $tvalue")
            }
        }

        is Expression.Variable -> {
            val id = stat.id
            localVariables[id] ?: throw SemanticException("variable $id not defined")
        }

        is Expression.Float -> TValue(VariableType.FLOAT, stat.value)
        is Expression.Int -> TValue(VariableType.INT, stat.value)
        is Expression.String -> TValue(VariableType.STRING, stat.value)
    }
}
