package cympl.interpreter

import cympl.language.*
import cympl.language.symbol.ArrayScope
import cympl.language.symbol.StringScope
import cympl.interpreter.RuntimeTypeChecker.assertValueType
import cympl.interpreter.RuntimeTypeChecker.checkArrayDimension

class Interpreter(private val runtime: Runtime) {

    private val globalScope = Environment()

    init {
        val printLine = Closure(IntrinsicFunction.PrintLine, globalScope)
        val readLine = Closure(IntrinsicFunction.ReadLine, globalScope)
        globalScope.defineVariable(IntrinsicFunction.PrintLine.id, printLine)
        globalScope.defineVariable(IntrinsicFunction.ReadLine.id, readLine)
    }

    val globalSymbols: Set<String> get() = globalScope.getVariables().keys

    sealed class Jump : Throwable() {
        data class Return(val value: TValue) : Jump()
        object Break : Jump()
        object Continue : Jump()
    }

    fun interpret(program: Program, verbose: Boolean = true): Sequence<String> = sequence {
        for (stat in program.statements) {
            try {
                val result = stat.evaluate(globalScope)
                if (verbose) {
                    yield("$stat => ${formatTValue(result)}")
                }
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

        if (verbose) {
            yieldAll(dumpEnvironment())
        }
    }

    private fun Statement.evaluate(scope: Environment): TValue = when (this) {
        is Statement.VariableDeclaration -> evaluate(scope)
        is Statement.Assignment -> evaluate(scope)
        is Statement.IndexAssignment -> evaluate(scope)
        is Statement.FunctionDeclaration -> evaluate(scope)
        is Statement.Block -> evaluate(scope)
        is Statement.ExpressionStatement -> expr.evaluate(scope)
        is Statement.If -> evaluate(scope)
        is Statement.While -> evaluate(scope)
        is Statement.For -> evaluate(scope)
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
            val value = expr?.evaluate(scope) ?: TValue.VOID
            throw Jump.Return(value)
        }

        is Statement.Switch -> evaluate(scope)

        else -> throw InterpretException("unknown statement $this")
    }

    private fun Statement.Block.evaluate(parent: Environment): TValue {
        val currentScope = Environment(parent)

        for (stat in statements) {
            stat.evaluate(currentScope)
        }

        return TValue.VOID
    }

    private fun Statement.If.evaluate(scope: Environment): TValue {
        return if (condition.evaluate(scope).asBoolean()) {
            thenBranch.evaluate(scope)
        } else {
            elseBranch?.evaluate(scope) ?: TValue.VOID
        }
    }

    private fun Statement.While.evaluate(scope: Environment): TValue = scope.withinLoop {
        while (condition.evaluate(scope).asBoolean()) {
            try {
                body.evaluate(scope)
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

    private fun Statement.For.evaluate(scope: Environment): TValue = scope.withinLoop {
        val forScope = Environment(scope)

        init?.evaluate(forScope)

        while (condition?.evaluate(forScope)?.asBoolean() != false) {
            try {
                body.evaluate(forScope)
            } catch (jump: Jump) {
                when (jump) {
                    is Jump.Return -> return@withinLoop jump.value
                    is Jump.Break -> break
                    is Jump.Continue -> {}
                }
            }
            update?.evaluate(forScope)
        }
        TValue.VOID
    }

    private fun Statement.Switch.evaluate(scope: Environment): TValue {
        val value = expr.evaluate(scope)
        var matched = false
        for (case in cases) {
            matched = matched || case.condition.evaluate(scope) == value
            if (matched) {
                case.action?.evaluate(scope)
            } else {
                continue
            }
            if (case.hasBreak) {
                break
            }
        }
        if (!matched) {
            defaultCase?.evaluate(scope)
        }
        return TValue.VOID
    }

    private fun Statement.FunctionDeclaration.evaluate(scope: Environment): TValue {
        if (scope.containsVariable(id, true)) {
            throw InterpretException("function $id already declared")
        }
        return Closure(this, scope)
            .also { scope.defineVariable(id, it) }
    }

    private fun Statement.Assignment.evaluate(scope: Environment): TValue {
        scope.resolveVariable(id)
            ?: throw InterpretException("variable $id not defined")

        val value = expr.evaluate(scope)
        scope.setVariable(id, value)
        return value
    }

    private fun Statement.IndexAssignment.evaluate(scope: Environment): TValue {
        val arrayIndexing = ArrayIndexing(arrayExpr, indexExpr, scope)

        val value = valueExpr.evaluate(scope)
        assertValueType(value, arrayIndexing.arrayType.elementType)

        arrayIndexing.setValue(value)

        return value
    }

    private fun Statement.VariableDeclaration.evaluate(scope: Environment): TValue {
        if (scope.containsVariable(id, true)) {
            throw InterpretException("variable $id already declared")
        }
        return expr!!.evaluate(scope)
            .also { result ->
                assertValueType(result, type)
                // TEmptyArray has no concrete type, so we need to cast it to the type of the variable
                val variable = if (result == TValue.TEmptyArray)
                    TValue.TEmptyArray.castTo(type as BuiltinType.ARRAY)
                else result

                scope.defineVariable(id, variable)
            }
    }

    private fun Expression.evaluate(scope: Environment): TValue = when (this) {
        is Expression.Parenthesized -> expr.evaluate(scope)

        is Expression.Addition -> {
            val left = left.evaluate(scope)
            val right = right.evaluate(scope)
            BinaryOperation.Arithmetic.Plus.apply(left, right)
        }

        is Expression.Subtraction -> {
            val left = left.evaluate(scope)
            val right = right.evaluate(scope)
            BinaryOperation.Arithmetic.Minus.apply(left, right)
        }

        is Expression.Multiplication -> {
            val left = left.evaluate(scope)
            val right = right.evaluate(scope)
            BinaryOperation.Arithmetic.Times.apply(left, right)
        }

        is Expression.Division -> {
            val left = left.evaluate(scope)
            val right = right.evaluate(scope)
            BinaryOperation.Arithmetic.Div.apply(left, right)
        }

        is Expression.Remainder -> {
            val left = left.evaluate(scope)
            val right = right.evaluate(scope)
            BinaryOperation.Arithmetic.Rem.apply(left, right)
        }

        is Expression.Power -> {
            val left = left.evaluate(scope)
            val right = right.evaluate(scope)
            BinaryOperation.Arithmetic.Pow.apply(left, right)
        }

        is Expression.Negation -> {
            val tvalue = expr.evaluate(scope)
            UnaryOperation.Minus.apply(tvalue)
        }

        is Expression.Increment -> {
            val target = expr
            val tvalue = target.evaluate(scope)
            val tvalueUpdated = UnaryOperation.Increment.apply(tvalue)

            when (target) {
                is Expression.Variable -> scope.setVariable(target.id, tvalueUpdated)
                is Expression.Index -> {
                    val arrayIndexing = ArrayIndexing(target.arrayExpr, target.indexExpr, scope)
                    arrayIndexing.setValue(tvalueUpdated)
                }

                else -> throw InterpretException("cannot increment $target")
            }

            if (postfix) tvalue else tvalueUpdated
        }

        is Expression.Decrement -> {
            val target = expr
            val tvalue = target.evaluate(scope)
            val tvalueUpdated = UnaryOperation.Decrement.apply(tvalue)

            when (target) {
                is Expression.Variable -> scope.setVariable(target.id, tvalueUpdated)
                is Expression.Index -> {
                    val arrayIndexing = ArrayIndexing(target.arrayExpr, target.indexExpr, scope)
                    arrayIndexing.setValue(tvalueUpdated)
                }

                else -> throw InterpretException("cannot increment $target")
            }

            if (postfix) tvalue else tvalueUpdated
        }

        is Expression.And -> {
            val left = left.evaluate(scope)
            val right = { right.evaluate(scope) }
            BinaryOperation.Logical.And.apply(left, right)
        }

        is Expression.Or -> {
            val left = left.evaluate(scope)
            val right = { right.evaluate(scope) }
            BinaryOperation.Logical.Or.apply(left, right)
        }

        is Expression.Not -> {
            val tvalue = expr.evaluate(scope)
            UnaryOperation.Not.apply(tvalue)
        }

        is Expression.Variable -> {
            scope.resolveVariable(id) ?: throw InterpretException("variable $id not defined")
        }

        is Expression.BoolLiteral -> TValue(BuiltinType.BOOL, value)
        is Expression.FloatLiteral -> TValue(BuiltinType.FLOAT, value)
        is Expression.IntLiteral -> TValue(BuiltinType.INT, value)
        is Expression.StringLiteral -> TValue(BuiltinType.STRING, value)

        is Expression.Equality -> {
            val left = left.evaluate(scope)
            val right = right.evaluate(scope)
            BinaryOperation.Comparison.Eq.apply(left, right)
        }

        is Expression.Inequality -> {
            val left = left.evaluate(scope)
            val right = right.evaluate(scope)
            BinaryOperation.Comparison.Neq.apply(left, right)
        }

        is Expression.GreaterThan -> {
            val left = left.evaluate(scope)
            val right = right.evaluate(scope)
            BinaryOperation.Comparison.Gt.apply(left, right)
        }

        is Expression.GreaterThanOrEqual -> {
            val left = left.evaluate(scope)
            val right = right.evaluate(scope)
            BinaryOperation.Comparison.Geq.apply(left, right)
        }

        is Expression.LessThan -> {
            val left = left.evaluate(scope)
            val right = right.evaluate(scope)
            BinaryOperation.Comparison.Lt.apply(left, right)
        }

        is Expression.LessThanOrEqual -> {
            val left = left.evaluate(scope)
            val right = right.evaluate(scope)
            BinaryOperation.Comparison.Leq.apply(left, right)
        }

        is Expression.ArrayLiteral -> {
            val elements = elements.map { it.evaluate(scope) }.toTypedArray()
            if (elements.isEmpty()) {
                TValue.TEmptyArray
            } else {
                val elementType = elements.firstOrNull()?.type ?: BuiltinType.VOID
                TValue(BuiltinType.ARRAY(elementType), elements)
            }
        }

        is Expression.NewArray -> when {
            dimensions.size == 1 -> {
                val dimension = dimensions.first().evaluate(scope).let { checkArrayDimension(it) }
                val elements = Array(dimension) { TValue.defaultValueOf(elementType) }
                TValue(resolvedType, elements)
            }

            dimensions.size > 1 -> {
                val dimension = dimensions.first().evaluate(scope).let { checkArrayDimension(it) }

                TValue(resolvedType, Array(dimension) {
                    Expression.NewArray(elementType, dimensions.drop(1)).evaluate(scope)
                })
            }

            else -> {
                throw InterpretException("cannot create array with zero or negative dimensions")
            }
        }

        is Expression.FunctionCall -> {
            val closure = when (val funcExpr = this.funcExpr) {
                is Expression.Variable -> {
                    scope.resolveVariable(funcExpr.id) as? Closure
                        ?: throw InterpretException("function not defined: $funcExpr")
                }

                else -> {
                    funcExpr.evaluate(scope) as? Closure
                        ?: throw InterpretException("not a function: $funcExpr")
                }
            }

            val function = closure.function
            val args = args.map { it.evaluate(scope) }
            if (args.size != function.parameters.size) {
                throw InterpretException("function $funcExpr expects ${function.parameters.size} arguments, got ${args.size}")
            }

            val functionScope = Environment(closure.env).apply {
                function.parameters.forEachIndexed { i, (name, type) ->
                    assertValueType(args[i], type)
                    defineVariable(name, args[i])
                }
            }

            if (function is IntrinsicFunction) {
                handleIntrinsicCall(function, args)
            } else {
                try {
                    function.body.evaluate(functionScope)
                } catch (ret: Jump.Return) {
                    ret.value
                }
            }
        }

        is Expression.Index -> {
            ArrayIndexing(arrayExpr, indexExpr, scope).getValue()
        }

        is Expression.Property -> {
            val owner = expr.evaluate(scope)
            when (owner.type) {
                is BuiltinType.ARRAY -> {
                    @Suppress("UNCHECKED_CAST")
                    val array = owner.value as Array<TValue>
                    when (propertyName) {
                        ArrayScope.LENGTH_PROPERTY.name -> TValue(BuiltinType.INT, array.size)
                        else -> throw InterpretException("array has no property $propertyName")
                    }
                }

                is BuiltinType.STRING -> {
                    val string = owner.value as String
                    when (propertyName) {
                        StringScope.LENGTH_PROPERTY.name -> TValue(BuiltinType.INT, string.length)
                        else -> throw InterpretException("string has no property $propertyName")
                    }
                }

                else -> throw InterpretException("cannot access property [$propertyName] of type ${owner.type}")
            }
        }

        is Expression.Lambda -> {
            val paramTypes = type.paramTypes
            val returnType = type.returnType

            val paramDecls = paramNames.zip(paramTypes).map { (name, type) ->
                Statement.VariableDeclaration(name, type)
            }

            val lambdaType = BuiltinType.FUNCTION(paramTypes, returnType)

            val functionDeclaration = Statement.FunctionDeclaration(
                id = "<lambda>",
                resolvedType = lambdaType,
                parameters = paramDecls,
                body = Statement.Block(listOf(body))
            )
            Closure(functionDeclaration, scope)
        }
    }

    private fun handleIntrinsicCall(function: IntrinsicFunction, args: List<TValue>): TValue = when (function) {
        IntrinsicFunction.PrintLine -> {
            val arg = args.firstOrNull()
            if (arg != null) {
                runtime.printLine(formatTValue(arg))
            } else {
                runtime.printLine("")
            }
            TValue.VOID
        }

        IntrinsicFunction.ReadLine -> {
            val arg = args.firstOrNull() ?: ""
            val line = runtime.readLine(arg.toString())
            TValue(BuiltinType.STRING, line)
        }
    }

    private fun formatTValue(tvalue: TValue) = when (tvalue.type) {
        BuiltinType.STRING -> "\"${tvalue.value}\""
        else -> tvalue.toString()
    }

    private fun dumpEnvironment(): Sequence<String> = sequence {
        yield("environment:")

        val (functions, variables) = globalScope.getVariables()
            .asSequence()
            .partition { it.value is Closure }

        val variableLine = variables.joinToString(", ") { (k, tvalue) -> "$k:${tvalue.type} = ${formatTValue(tvalue)}" }

        if (variableLine.isNotEmpty()) {
            yield(variableLine)
        }

        val functionsLine = functions
            .filter { (_, v) -> (v as Closure).function !is IntrinsicFunction }
            .joinToString(", ") { (k, tvalue) -> "$k: ${tvalue.type}" }

        if (functionsLine.isNotEmpty()) {
            yield(functionsLine)
        }
    }

    private inner class ArrayIndexing(arrayExpr: Expression, indexExpr: Expression, scope: Environment) {
        val array: Array<TValue>
        val index: Int
        val arrayType: BuiltinType.ARRAY

        init {
            val arrayTValue = arrayExpr.evaluate(scope)
            val indexTValue = indexExpr.evaluate(scope)
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
