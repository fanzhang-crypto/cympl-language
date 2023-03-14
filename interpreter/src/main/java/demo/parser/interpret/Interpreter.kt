package demo.parser.interpret

import demo.parser.domain.*
import demo.parser.interpret.TypeChecker.assertValueType

class Interpreter {

    private val globalScope = Scope()

    val globalSymbols: Set<String> get() = globalScope.getVariables().keys + globalScope.getFunctions().keys

    sealed class Jump : Throwable() {
        data class Return(val value: TValue) : Jump()
        object Break : Jump()
        object Continue : Jump()
    }

    fun interpret(program: Program): Sequence<String> = sequence {
        for (stat in program.statements) {
            try {
                val result = evaluate(stat, globalScope)
                yield("$stat => ${formatTValue(result)}")
            } catch (jump: Jump) {
                when (jump) {
                    is Jump.Return -> yield("$stat => ${formatTValue(jump.value)}")
                    is Jump.Break -> throw IllegalStateException("break outside of loop")
                    is Jump.Continue -> throw IllegalStateException("continue outside of loop")
                }
            } catch (e: InterpretException) {
                yield("$stat failed => ${e.message}")
            }
        }
        yield("environment:")
        formatCurrentEnvVariables().takeIf { it.isNotBlank() }?.let { yield(it) }
        formatCurrentEnvFunctions().takeIf { it.isNotBlank() }?.let { yield(it) }
    }

    private fun evaluate(statement: Statement, scope: Scope): TValue = when (statement) {
        is Statement.VariableDeclaration -> evaluate(statement, scope)
        is Statement.Assignment -> evaluate(statement, scope)
        is Statement.IndexAssignment -> evaluate(statement, scope)
        is Statement.FunctionDeclaration -> evaluate(statement, scope)
        is Statement.Block -> evaluate(statement, scope)
        is Statement.ExpressionStatement -> evaluate(statement.expr, scope)
        is Statement.If -> evaluate(statement, scope)
        is Statement.While -> evaluate(statement, scope)
        is Statement.For -> evaluate(statement, scope)
        is Statement.Break -> {
            if (scope.isInLoop())
                throw Jump.Break
            else
                throw InterpretException("break outside of loop")
        }

        is Statement.Continue -> {
            if (scope.isInLoop())
                throw Jump.Continue
            else
                throw InterpretException("continue outside of loop")
        }

        is Statement.Return -> {
            val value = statement.expr?.let { evaluate(it, scope) } ?: TValue.VOID
            throw Jump.Return(value)
        }

        else -> throw InterpretException("unknown statement $statement")
    }

    private fun evaluate(block: Statement.Block, parent: Scope): TValue {
        val currentScope = Scope(parent)

        for (stat in block.statements) {
            evaluate(stat, currentScope)
        }

        return TValue.VOID
    }

    private fun evaluate(ifStatement: Statement.If, scope: Scope): TValue {
        val condition = evaluate(ifStatement.condition, scope)
        return if (condition.asBoolean()) {
            evaluate(ifStatement.thenBranch, scope)
        } else {
            ifStatement.elseBranch?.let { evaluate(it, scope) }
                ?: TValue.VOID
        }
    }

    private fun evaluate(whileStat: Statement.While, scope: Scope): TValue = scope.withinLoop {
        while (evaluate(whileStat.condition, scope).asBoolean()) {
            try {
                evaluate(whileStat.body, scope)
            } catch (jump: Jump) {
                when (jump) {
                    is Jump.Return -> return@withinLoop jump.value
                    is Jump.Break -> break
                    is Jump.Continue -> {}
                }
            }
        }
        return@withinLoop TValue.VOID
    }

    private fun evaluate(forStat: Statement.For, scope: Scope): TValue = scope.withinLoop {
        forStat.init?.let { evaluate(it, scope) }
        while (forStat.condition?.let { evaluate(it, scope).asBoolean() } != false) {
            try {
                evaluate(forStat.body, scope)
            } catch (jump: Jump) {
                when (jump) {
                    is Jump.Return -> return@withinLoop jump.value
                    is Jump.Break -> break
                    is Jump.Continue -> {}
                }
            }
            forStat.update?.let { evaluate(it, scope) }
        }
        return@withinLoop TValue.VOID
    }

    private fun evaluate(functionDeclaration: Statement.FunctionDeclaration, scope: Scope): TValue {
        val id = functionDeclaration.id
        if (scope.containsFunction(id, true)) {
            throw InterpretException("function $id already declared")
        }
        scope.defineFunction(id, functionDeclaration)
        return TValue.VOID
    }

    private fun evaluate(assignment: Statement.Assignment, scope: Scope): TValue {
        val id = assignment.id
        val variable = scope.resolveVariable(id)
            ?: throw InterpretException("variable $id not defined")

        val value = evaluate(assignment.expr, scope)
        return variable.withValue(value.value).also { scope.setVariable(id, it) }
    }

    private fun evaluate(stat: Statement.IndexAssignment, scope: Scope): TValue {
        val arrayIndexing = ArrayIndexing(stat.arrayExpr, stat.indexExpr, scope)

        val value = evaluate(stat.valueExpr, scope)
        assertValueType(value, arrayIndexing.arrayType.elementType)

        arrayIndexing.setValue(value)

        return value
    }

    private fun evaluate(variableDeclaration: Statement.VariableDeclaration, scope: Scope): TValue {
        val id = variableDeclaration.id
        if (scope.containsVariable(id, true)) {
            throw InterpretException("variable $id already declared")
        }
        val type = variableDeclaration.type
        return evaluate(variableDeclaration.expr!!, scope)
            .also { result ->
                assertValueType(result, type)
                scope.defineVariable(id, TValue(type, result.value))
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

        is Expression.Increment -> {
            val target = expression.expr
            val tvalue = evaluate(target, scope)
            val tvalueUpdated = UnaryOperation.Increment.apply(tvalue)

            when (target) {
                is Expression.Variable -> scope.setVariable(target.id, tvalueUpdated)
                is Expression.Index -> {
                    val arrayIndexing = ArrayIndexing(target.arrayExpr, target.indexExpr, scope)
                    arrayIndexing.setValue(tvalueUpdated)
                }
                else -> throw InterpretException("cannot increment $target")
            }

            if (expression.postfix) tvalue else tvalueUpdated
        }

        is Expression.Decrement -> {
            val target = expression.expr
            val tvalue = evaluate(target, scope)
            val tvalueUpdated = UnaryOperation.Decrement.apply(tvalue)

            when (target) {
                is Expression.Variable -> scope.setVariable(target.id, tvalueUpdated)
                is Expression.Index -> {
                    val arrayIndexing = ArrayIndexing(target.arrayExpr, target.indexExpr, scope)
                    arrayIndexing.setValue(tvalueUpdated)
                }
                else -> throw InterpretException("cannot increment $target")
            }

            if (expression.postfix) tvalue else tvalueUpdated
        }

        is Expression.And -> {
            val left = evaluate(expression.left, scope)
            val right = { evaluate(expression.right, scope) }
            BinaryOperation.Logical.And.apply(left, right)
        }

        is Expression.Or -> {
            val left = evaluate(expression.left, scope)
            val right = { evaluate(expression.right, scope) }
            BinaryOperation.Logical.Or.apply(left, right)
        }

        is Expression.Not -> {
            val tvalue = evaluate(expression.expr, scope)
            UnaryOperation.Not.apply(tvalue)
        }

        is Expression.Variable -> {
            val id = expression.id
            scope.resolveVariable(id) ?: throw InterpretException("variable $id not defined")
        }

        is Expression.Bool -> TValue(BuiltinType.BOOL, expression.value)
        is Expression.Float -> TValue(BuiltinType.FLOAT, expression.value)
        is Expression.Int -> TValue(BuiltinType.INT, expression.value)
        is Expression.String -> TValue(BuiltinType.STRING, expression.value)

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

        is Expression.Array -> {
            val elements = expression.elements.map { evaluate(it, scope) }.toTypedArray()
            if (elements.isEmpty()) {
                TValue.TEmptyArray
            } else {
                val elementType = elements.firstOrNull()?.type ?: BuiltinType.VOID
                TValue(BuiltinType.ARRAY(elementType), elements)
            }
        }

        is Expression.FunctionCall -> {
            val id = expression.id
            val function = scope.resolveFunction(id)
                ?: throw InterpretException("function $id not defined")

            val args = expression.args.map { evaluate(it, scope) }
            if (args.size != function.args.size) {
                throw InterpretException("function $id expects ${function.args.size} arguments, got ${args.size}")
            }

            val functionScope = Scope(scope).apply {
                function.args.forEachIndexed { i, (name, type) ->
                    assertValueType(args[i], type)
                    defineVariable(name, args[i])
                }
            }

            try {
                evaluate(function.body, functionScope)
            } catch (ret: Jump.Return) {
                ret.value
            }
        }

        is Expression.Index -> {
            ArrayIndexing(expression.arrayExpr, expression.indexExpr, scope).getValue()
        }
    }

    private fun formatTValue(tvalue: TValue) = when (tvalue.type) {
        BuiltinType.STRING -> "\"${tvalue.value}\""
        else -> tvalue.toString()
    }

    private fun formatCurrentEnvVariables() = globalScope.getVariables()
        .map { (k, tvalue) -> "$k:${tvalue.type} = ${formatTValue(tvalue)}" }
        .joinToString(", ")

    private fun formatCurrentEnvFunctions() = globalScope.getFunctions()
        .map { (k, function) -> "$k(${function.args.joinToString(", ") { "${it.id}:${it.type}" }}):${function.returnType}" }
        .joinToString(", ")

    private inner class ArrayIndexing(arrayExpr: Expression, indexExpr: Expression, scope: Scope) {
        val array: Array<TValue>
        val index: Int
        val arrayType: BuiltinType.ARRAY

        init {
            val arrayTValue = evaluate(arrayExpr, scope)
            val indexTValue = evaluate(indexExpr, scope)
            if (arrayTValue.type !is BuiltinType.ARRAY) {
                throw InterpretException("indexing non-array type ${arrayTValue.type}")
            }
            if (indexTValue.type != BuiltinType.INT) {
                throw InterpretException("indexing array with non-int type ${indexTValue.type}")
            }

            @Suppress("UNCHECKED_CAST")
            this.array = arrayTValue.value as Array<TValue>
            this.index = indexTValue.value as Int
            if (index < 0 || index >= array.size) {
                throw InterpretException("index $index out of bounds for array of size ${array.size}")
            }

            arrayType = arrayTValue.type
        }

        fun getValue(): TValue = array[index]

        fun setValue(value: TValue) {
            array[index] = value
        }
    }
}
