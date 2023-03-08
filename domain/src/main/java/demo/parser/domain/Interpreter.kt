package demo.parser.domain

class Interpreter {

    private val globalScope = Scope()

    private data class EvaluationResult(val value: TValue, val isReturning: Boolean = false, val isThrowing: Boolean = false)

    fun interpret(program: Program): Sequence<String> = sequence {
        for (stat in program.statements) {
            val result = evaluate(stat, globalScope)
            yield("$stat => ${formatTValue(result.value)}")
        }
        yield("environment:")
        formatCurrentEnvVariables().takeIf { it.isNotBlank() }?.let { yield(it) }
        formatCurrentEnvFunctions().takeIf { it.isNotBlank() }?.let { yield(it) }
    }

    private fun evaluate(statement: Statement, scope: Scope): EvaluationResult = when (statement) {
        is Statement.VariableDeclaration -> evaluate(statement, scope)
        is Statement.Assignment -> evaluate(statement, scope)
        is Statement.FunctionDeclaration -> evaluate(statement, scope)
        is Statement.Block -> evaluate(statement, scope)
        is Statement.Return -> evaluate(statement.expr, scope).copy(isReturning = true)
        is Statement.ExpressionStatement -> evaluate(statement.expr, scope)
        is Statement.If -> evaluate(statement, scope)
        is Statement.While -> evaluate(statement, scope)
        else -> throw SemanticException("unknown statement $statement")
    }

    private fun evaluate(block: Statement.Block, parent: Scope): EvaluationResult {
        val currentScope = Scope(parent)

        for (stat in block.statements) {
            val result = evaluate(stat, currentScope)
            if (result.isReturning || result.isThrowing) {
                return result
            }
        }

        return TValue.VOID.asEvaluationResult()
    }

    private fun evaluate(ifStatement: Statement.If, scope: Scope): EvaluationResult {
        val condition = evaluate(ifStatement.condition, scope).value
        return if (condition.asBoolean()) {
            evaluate(ifStatement.thenBranch, scope)
        } else {
            ifStatement.elseBranch?.let { evaluate(it, scope) }
                ?: TValue.VOID.asEvaluationResult()
        }
    }

    private fun evaluate(whileStat: Statement.While, scope: Scope): EvaluationResult {
        while (evaluate(whileStat.condition, scope).value.asBoolean()) {
            val result = evaluate(whileStat.body, scope)
            if (result.isReturning || result.isThrowing) {
                return result
            }
        }
        return TValue.VOID.asEvaluationResult()
    }

    private fun evaluate(functionDeclaration: Statement.FunctionDeclaration, scope: Scope): EvaluationResult {
        val id = functionDeclaration.id
        if (scope.containsFunction(id)) {
            throw SemanticException("function $id already declared")
        }
        scope.addFunction(id, functionDeclaration)
        return TValue.VOID.asEvaluationResult()
    }

    private fun evaluate(assignment: Statement.Assignment, scope: Scope): EvaluationResult {
        val id = assignment.id
        val variable = scope.resolveVariable(id)
            ?: throw SemanticException("variable $id not defined")

        val value = evaluate(assignment.expr, scope).value
        return variable.withValue(value.value).also { scope.setVariable(id, it) }.asEvaluationResult()
    }

    private fun evaluate(variableDeclaration: Statement.VariableDeclaration, scope: Scope): EvaluationResult {
        val id = variableDeclaration.id
        if (scope.containsVariable(id, true)) {
            throw SemanticException("variable $id already declared")
        }
        val type = variableDeclaration.type
        return evaluate(variableDeclaration.expr!!, scope)
            .also { result ->
                type.checkValue(result.value.value)
                scope.addVariable(id, TValue(type, result.value.value))
            }
    }

    private fun evaluate(expression: Expression, scope: Scope): EvaluationResult = when (expression) {
        is Expression.Parenthesized -> evaluate(expression.expr, scope)

        is Expression.Addition -> {
            val left = evaluate(expression.left, scope).value
            val right = evaluate(expression.right, scope).value
            BinaryOperation.Arithmetic.Plus.apply(left, right).asEvaluationResult()
        }

        is Expression.Subtraction -> {
            val left = evaluate(expression.left, scope).value
            val right = evaluate(expression.right, scope).value
            BinaryOperation.Arithmetic.Minus.apply(left, right).asEvaluationResult()
        }

        is Expression.Multiplication -> {
            val left = evaluate(expression.left, scope).value
            val right = evaluate(expression.right, scope).value
            BinaryOperation.Arithmetic.Times.apply(left, right).asEvaluationResult()
        }

        is Expression.Division -> {
            val left = evaluate(expression.left, scope).value
            val right = evaluate(expression.right, scope).value
            BinaryOperation.Arithmetic.Div.apply(left, right).asEvaluationResult()
        }

        is Expression.Remainder -> {
            val left = evaluate(expression.left, scope).value
            val right = evaluate(expression.right, scope).value
            BinaryOperation.Arithmetic.Rem.apply(left, right).asEvaluationResult()
        }

        is Expression.Power -> {
            val left = evaluate(expression.left, scope).value
            val right = evaluate(expression.right, scope).value
            BinaryOperation.Arithmetic.Pow.apply(left, right).asEvaluationResult()
        }

        is Expression.Negation -> {
            val tvalue = evaluate(expression.expr, scope).value
            UnaryOperation.Minus.apply(tvalue).asEvaluationResult()
        }

        is Expression.And -> {
            val left = evaluate(expression.left, scope).value
            val right = evaluate(expression.right, scope).value
            BinaryOperation.Logical.And.apply(left, right).asEvaluationResult()
        }

        is Expression.Or -> {
            val left = evaluate(expression.left, scope).value
            val right = evaluate(expression.right, scope).value
            BinaryOperation.Logical.Or.apply(left, right).asEvaluationResult()
        }

        is Expression.Not -> {
            val tvalue = evaluate(expression.expr, scope).value
            UnaryOperation.Not.apply(tvalue).asEvaluationResult()
        }

        is Expression.Variable -> {
            val id = expression.id
            scope.resolveVariable(id)?.asEvaluationResult()
                ?: throw SemanticException("variable $id not defined")
        }

        is Expression.Bool -> TValue(VariableType.BOOL, expression.value).asEvaluationResult()
        is Expression.Float -> TValue(VariableType.FLOAT, expression.value).asEvaluationResult()
        is Expression.Int -> TValue(VariableType.INT, expression.value).asEvaluationResult()
        is Expression.String -> TValue(VariableType.STRING, expression.value).asEvaluationResult()

        is Expression.Equality -> {
            val left = evaluate(expression.left, scope).value
            val right = evaluate(expression.right, scope).value
            BinaryOperation.Comparison.Eq.apply(left, right).asEvaluationResult()
        }
        is Expression.Inequality -> {
            val left = evaluate(expression.left, scope).value
            val right = evaluate(expression.right, scope).value
            BinaryOperation.Comparison.Neq.apply(left, right).asEvaluationResult()
        }
        is Expression.GreaterThan -> {
            val left = evaluate(expression.left, scope).value
            val right = evaluate(expression.right, scope).value
            BinaryOperation.Comparison.Gt.apply(left, right).asEvaluationResult()
        }
        is Expression.GreaterThanOrEqual -> {
            val left = evaluate(expression.left, scope).value
            val right = evaluate(expression.right, scope).value
            BinaryOperation.Comparison.Geq.apply(left, right).asEvaluationResult()
        }
        is Expression.LessThan -> {
            val left = evaluate(expression.left, scope).value
            val right = evaluate(expression.right, scope).value
            BinaryOperation.Comparison.Lt.apply(left, right).asEvaluationResult()
        }
        is Expression.LessThanOrEqual -> {
            val left = evaluate(expression.left, scope).value
            val right = evaluate(expression.right, scope).value
            BinaryOperation.Comparison.Leq.apply(left, right).asEvaluationResult()
        }

        is Expression.FunctionCall -> {
            val id = expression.id
            val function = scope.resolveFunction(id)
                ?: throw SemanticException("function $id not defined")

            val args = expression.args.map { evaluate(it, scope).value }
            if (args.size != function.args.size) {
                throw SemanticException("function $id expects ${function.args.size} arguments, got ${args.size}")
            }

            function.args.forEachIndexed { i, (name, type) ->
                type.checkValue(args[i].value)
            }

            val functionScope = Scope(scope).apply {
                function.args.forEachIndexed { i, (name, type) ->
                    addVariable(name, args[i])
                }
            }

            evaluate(function.body, functionScope)
        }
    }

    private fun TValue.asEvaluationResult() = EvaluationResult(this)

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

