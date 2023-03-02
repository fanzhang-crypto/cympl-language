package demo.parser.domain

import kotlin.math.pow

class Interpreter {

    private val localVariables = mutableMapOf<String, Int>()

    fun interpret(program: Program): Sequence<String> = sequence {
        for (expr in program.expressions) {
            yield("$expr => ${evaluate(expr)}")
        }
        yield("environment:")
        yield(formatCurrentEnv())
    }

    private fun formatCurrentEnv() = localVariables
        .map { (k, v) -> "$k = $v" }
        .joinToString(", ")

    private fun evaluate(expr: Expression): Int {
        return when (expr) {
            is Declaration -> {
                val id = expr.id
                if (localVariables.contains(id)) {
                    throw SemanticException("variable $id already declared")
                }
                evaluate(expr.value).also { localVariables[id] = it }
            }

            is Assignment -> {
                val id = expr.id
                if (!localVariables.contains(id)) {
                    throw SemanticException("variable $id not defined")
                }
                evaluate(expr.value).also { localVariables[id] = it }
            }

            is Parenthesized -> evaluate(expr.expr)

            is Addition -> evaluate(expr.left) + evaluate(expr.right)
            is Multiplication -> evaluate(expr.left) * evaluate(expr.right)
            is Division -> evaluate(expr.left) / evaluate(expr.right)
            is Subtraction -> evaluate(expr.left) - evaluate(expr.right)
            is Power -> {
                val l = evaluate(expr.left).toDouble()
                val r = evaluate(expr.right).toDouble()
                l.pow(r).toInt()
            }
            is Negation -> -evaluate(expr.expr)

            is Number -> expr.int
            is Variable -> {
                val id = expr.id
                if (!localVariables.contains(id)) {
                    throw SemanticException("variable $id not defined")
                }
                localVariables[id]!!
            }
        }
    }

}
