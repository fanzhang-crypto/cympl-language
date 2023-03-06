package demo.parser.domain

import kotlin.math.pow

class Interpreter {

    private val globalScope = Scope()

    fun interpret(program: Program): Sequence<String> = sequence {
        for (stat in program.statements) {
            val result = evaluate(stat, globalScope)
            yield("$stat => ${formatTValue(result)}")
        }
        yield("environment:")
        formatCurrentEnvVariables().takeIf { it.isNotBlank() }?.let { yield(it) }
        formatCurrentEnvFunctions().takeIf { it.isNotBlank() }?.let { yield(it) }
    }

    private fun evaluate(statement: Statement, scope: Scope): TValue = when (statement) {
        is Statement.VariableDeclaration -> evaluate(statement, scope)
        is Statement.Assignment -> evaluate(statement, scope)
        is Statement.FunctionDeclaration -> evaluate(statement, scope)
        is Statement.Block -> evaluate(statement, scope)
        is Statement.If -> evaluate(statement, scope)
        is Statement.Return -> evaluate(statement.expr, scope)
        is Statement.ExpressionStatement -> evaluate(statement.expr, scope)
        else -> throw SemanticException("unknown statement $statement")
    }

    private fun evaluate(block: Statement.Block, parent: Scope): TValue {
        val currentScope = Scope(parent)

        for (stat in block.statements) {
            when (stat) {
                is Statement.VariableDeclaration -> evaluate(stat, currentScope)
                is Statement.Assignment -> evaluate(stat, currentScope)
                is Statement.FunctionDeclaration -> evaluate(stat, currentScope)
                is Statement.Block -> evaluate(stat, Scope(currentScope))
                is Statement.ExpressionStatement -> return evaluate(stat, currentScope)
                is Statement.If -> return evaluate(stat, currentScope)
                is Statement.Return -> return evaluate(stat.expr, currentScope)
                else -> throw SemanticException("unknown statement $stat")
            }
        }
        return  TValue.VOID
    }

    private fun evaluate(ifStatement: Statement.If, scope: Scope): TValue {
        val condition = evaluate(ifStatement.condition, scope)
        return if (condition.asBoolean()) {
            evaluate(ifStatement.thenBranch, scope)
        } else {
            ifStatement.elseBranch?.let { evaluate(it, scope) } ?: TValue.VOID
        }
    }

    private fun evaluate(functionDeclaration: Statement.FunctionDeclaration, scope: Scope): TValue {
        val id = functionDeclaration.id
        if (scope.containsFunction(id)) {
            throw SemanticException("function $id already declared")
        }
        scope.addFunction(id, functionDeclaration)
        return TValue.VOID
    }

    private fun evaluate(assignment: Statement.Assignment, scope: Scope): TValue {
        val id = assignment.id
        val variable = scope.resolveVariable(id)
            ?: throw SemanticException("variable $id not defined")

        val value = evaluate(assignment.expr, scope).value
        return variable.withValue(value).also { scope.addVariable(id, it) }
    }

    private fun evaluate(variableDeclaration: Statement.VariableDeclaration, scope: Scope): TValue {
        val id = variableDeclaration.id
        if (scope.containsVariable(id, true)) {
            throw SemanticException("variable $id already declared")
        }
        val type = variableDeclaration.type
        return evaluate(variableDeclaration.expr!!, scope)
            .also { v ->
                type.checkValue(v.value)
                scope.addVariable(id, TValue(type, v.value))
            }
    }

    private fun evaluate(expression: Expression, scope: Scope): TValue = when (expression) {
        is Expression.Parenthesized -> evaluate(expression.expr, scope)

        is Expression.Addition -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            when (left.type) {
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

        is Expression.Subtraction -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            when (left.type) {
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

        is Expression.Multiplication -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            when (left.type) {
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

        is Expression.Division -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            when (left.type) {
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

        is Expression.Power -> {
            val left = evaluate(expression.left, scope).asDouble()
            val right = evaluate(expression.right, scope).asDouble()
            TValue(VariableType.FLOAT, left.pow(right))
        }

        is Expression.Negation -> {
            val tvalue = evaluate(expression.expr, scope)
            when (tvalue.type) {
                VariableType.INT -> TValue(VariableType.INT, -tvalue.asInt())
                VariableType.FLOAT -> TValue(VariableType.FLOAT, -tvalue.asDouble())
                else -> throw SemanticException("cannot negate $tvalue")
            }
        }

        is Expression.Variable -> {
            val id = expression.id
            scope.resolveVariable(id) ?: throw SemanticException("variable $id not defined")
        }

        is Expression.Float -> TValue(VariableType.FLOAT, expression.value)
        is Expression.Int -> TValue(VariableType.INT, expression.value)
        is Expression.String -> TValue(VariableType.STRING, expression.value)
        is Expression.FunctionCall -> {
            val id = expression.id
            val function = scope.resolveFunction(id)
                ?: throw SemanticException("function $id not defined")

            val args = expression.args.map { evaluate(it, scope) }
            if (args.size != function.args.size) {
                throw SemanticException("function $id expects ${function.args.size} arguments, got ${args.size}")
            }

            function.args.forEachIndexed { i, (name, type) ->
                type.checkValue(args[i].value)
            }

            val functionScope = Scope(scope).apply {
                function.args.forEachIndexed { i, (name, type) ->
                    addVariable(name,  args[i])
                }
            }

            evaluate(function.body, functionScope)
        }

        is Expression.Equality -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            if (left.type != right.type) {
                throw SemanticException("cannot compare ${left.type} to ${right.type}")
            }
            TValue(VariableType.BOOL, left.value == right.value)
        }
        is Expression.Inequality -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            if (left.type != right.type) {
                throw SemanticException("cannot compare ${left.type} to ${right.type}")
            }
            TValue(VariableType.BOOL, left.value != right.value)
        }

        is Expression.GreaterThan -> TODO()
        is Expression.GreaterThanOrEqual -> TODO()
        is Expression.LessThan -> TODO()
        is Expression.LessThanOrEqual -> TODO()
    }

    private fun formatTValue(tvalue: TValue) = when (tvalue.type) {
        VariableType.STRING -> "\"${tvalue.value}\""
        else -> tvalue.value.toString()
    }

    private fun formatCurrentEnvVariables() = globalScope.getVariables()
        .map { (k, tvalue) -> "$k:${tvalue.type} = ${formatTValue(tvalue)}" }
        .joinToString(", ")

    private fun formatCurrentEnvFunctions() = globalScope.getFunctions()
        .map { (k, function) -> "$k(${function.args.joinToString(", ") { "${it.id}:${it.type}" }}):${function.returnType}" }
        .joinToString(", ")
}

