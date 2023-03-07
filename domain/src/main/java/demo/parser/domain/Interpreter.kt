package demo.parser.domain

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
        return TValue.VOID
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
        return variable.withValue(value).also { scope.setVariable(id, it) }
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
            BinaryOperation.Arithmetic.Plus.apply(left, right)
        }

        is Expression.Subtraction -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            BinaryOperation.Arithmetic.Minus.apply(left, right)
        }

        is Expression.Multiplication -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            BinaryOperation.Arithmetic.Times.apply(left, right)
        }

        is Expression.Division -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            BinaryOperation.Arithmetic.Div.apply(left, right)
        }

        is Expression.Remainder -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            BinaryOperation.Arithmetic.Rem.apply(left, right)
        }

        is Expression.Power -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            BinaryOperation.Arithmetic.Pow.apply(left, right)
        }

        is Expression.Negation -> {
            val tvalue = evaluate(expression.expr, scope)
            UnaryOperation.Minus.apply(tvalue)
        }

        is Expression.And -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            BinaryOperation.Logical.And.apply(left, right)
        }

        is Expression.Or -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            BinaryOperation.Logical.Or.apply(left, right)
        }

        is Expression.Not -> {
            val tvalue = evaluate(expression.expr, scope)
            UnaryOperation.Not.apply(tvalue)
        }

        is Expression.Variable -> {
            val id = expression.id
            scope.resolveVariable(id) ?: throw SemanticException("variable $id not defined")
        }

        is Expression.Bool -> TValue(VariableType.BOOL, expression.value)
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
                    addVariable(name, args[i])
                }
            }

            evaluate(function.body, functionScope)
        }

        is Expression.Equality -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            BinaryOperation.Comparison.Eq.apply(left, right)
        }

        is Expression.Inequality -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            BinaryOperation.Comparison.Neq.apply(left, right)
        }

        is Expression.GreaterThan -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            BinaryOperation.Comparison.Gt.apply(left, right)
        }

        is Expression.GreaterThanOrEqual -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            BinaryOperation.Comparison.Geq.apply(left, right)
        }

        is Expression.LessThan -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            BinaryOperation.Comparison.Lt.apply(left, right)
        }

        is Expression.LessThanOrEqual -> {
            val left = evaluate(expression.left, scope)
            val right = evaluate(expression.right, scope)
            BinaryOperation.Comparison.Leq.apply(left, right)
        }
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

