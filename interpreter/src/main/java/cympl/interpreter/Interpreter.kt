package cympl.interpreter

import cympl.language.*
import cympl.language.symbol.ArrayScope
import cympl.language.symbol.StringScope
import cympl.interpreter.RuntimeTypeChecker.assertValueType
import cympl.interpreter.RuntimeTypeChecker.checkArrayDimension

class Interpreter(private val runtime: Runtime) {

    private val env = Environment()

    init {
        val printLine = Closure(IntrinsicFunction.PrintLine, env)
        val readLine = Closure(IntrinsicFunction.ReadLine, env)
        env.defineVariable(IntrinsicFunction.PrintLine.id, printLine)
        env.defineVariable(IntrinsicFunction.ReadLine.id, readLine)
    }

    val globalSymbols: Set<String> get() = env.getVariables().keys

    sealed class Jump : Throwable() {
        data class Return(val value: TValue) : Jump()
        object Break : Jump()
        object Continue : Jump()
    }

    fun interpret(program: Program, verbose: Boolean = true): Sequence<String> = sequence {
        for (stat in program.statements) {
            try {
                val result = stat.evaluate(env)
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

    private fun Statement.evaluate(env: Environment): TValue = when (this) {
        is Statement.VariableDeclaration -> evaluate(env)
        is Statement.Assignment -> evaluate(env)
        is Statement.FunctionDeclaration -> evaluate(env)
        is Statement.Block -> evaluate(env)
        is Statement.ExpressionStatement -> expr.evaluate(env)
        is Statement.If -> evaluate(env)
        is Statement.While -> evaluate(env)
        is Statement.For -> evaluate(env)
        is Statement.Break -> {
            if (env.isInLoop())
                throw Jump.Break
            else
                throw InterpretException("break outside of loop")
        }

        is Statement.Continue -> {
            if (env.isInLoop())
                throw Jump.Continue
            else
                throw InterpretException("continue outside of loop")
        }

        is Statement.Return -> {
            val value = expr?.evaluate(env) ?: TValue.VOID
            throw Jump.Return(value)
        }

        is Statement.Switch -> evaluate(env)

        else -> throw InterpretException("unknown statement $this")
    }

    private fun Statement.Block.evaluate(parent: Environment): TValue {
        val currentScope = Environment(parent)

        for (stat in statements) {
            stat.evaluate(currentScope)
        }

        return TValue.VOID
    }

    private fun Statement.If.evaluate(env: Environment): TValue {
        return if (condition.evaluate(env).asBoolean()) {
            thenBranch.evaluate(env)
        } else {
            elseBranch?.evaluate(env) ?: TValue.VOID
        }
    }

    private fun Statement.While.evaluate(env: Environment): TValue = env.withinLoop {
        while (condition.evaluate(env).asBoolean()) {
            try {
                body.evaluate(env)
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

    private fun Statement.For.evaluate(env: Environment): TValue = env.withinLoop {
        val forScope = Environment(env)

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

    private fun Statement.Switch.evaluate(env: Environment): TValue {
        val value = expr.evaluate(env)
        var matched = false
        for (case in cases) {
            matched = matched || case.condition.evaluate(env) == value
            if (matched) {
                case.action?.evaluate(env)
            } else {
                continue
            }
            if (case.hasBreak) {
                break
            }
        }
        if (!matched) {
            defaultCase?.evaluate(env)
        }
        return TValue.VOID
    }

    private fun Statement.FunctionDeclaration.evaluate(env: Environment): TValue {
        if (env.containsVariable(id, true)) {
            throw InterpretException("function $id already declared")
        }
        return Closure(this, env)
            .also { env.defineVariable(id, it) }
    }

    private fun Statement.Assignment.evaluate(env: Environment): TValue {
        when (val leftExpr = this.leftExpr) {
            is Expression.Variable -> {
                env.resolveVariable(leftExpr.id)
                    ?: throw InterpretException("variable ${leftExpr.id} not defined")

                val rightValue = rightExpr.evaluate(env)
                env.setVariable(leftExpr.id, rightValue)
                return rightValue
            }
            is Expression.ArrayAccess -> {
                val arrayAccess = ArrayAccess(leftExpr.arrayExpr, leftExpr.indexExpr, env)

                val value = rightExpr.evaluate(env)
                assertValueType(value, arrayAccess.arrayType.elementType)

                arrayAccess.setValue(value)

                return value
            }
            else -> throw InterpretException("invalid assignment $this")
        }
    }

    private fun Statement.VariableDeclaration.evaluate(env: Environment): TValue {
        if (env.containsVariable(id, true)) {
            throw InterpretException("variable $id already declared")
        }
        return expr!!.evaluate(env)
            .also { result ->
                assertValueType(result, type)
                // TEmptyArray has no concrete type, so we need to cast it to the type of the variable
                val variable = if (result == TValue.TEmptyArray)
                    TValue.TEmptyArray.castTo(type as BuiltinType.ARRAY)
                else result

                env.defineVariable(id, variable)
            }
    }

    private fun Expression.evaluate(env: Environment): TValue = when (this) {
        is Expression.Parenthesized -> expr.evaluate(env)

        is Expression.Addition -> {
            val left = left.evaluate(env)
            val right = right.evaluate(env)
            BinaryOperation.Arithmetic.Plus.apply(left, right)
        }

        is Expression.Subtraction -> {
            val left = left.evaluate(env)
            val right = right.evaluate(env)
            BinaryOperation.Arithmetic.Minus.apply(left, right)
        }

        is Expression.Multiplication -> {
            val left = left.evaluate(env)
            val right = right.evaluate(env)
            BinaryOperation.Arithmetic.Times.apply(left, right)
        }

        is Expression.Division -> {
            val left = left.evaluate(env)
            val right = right.evaluate(env)
            BinaryOperation.Arithmetic.Div.apply(left, right)
        }

        is Expression.Remainder -> {
            val left = left.evaluate(env)
            val right = right.evaluate(env)
            BinaryOperation.Arithmetic.Rem.apply(left, right)
        }

        is Expression.Power -> {
            val left = left.evaluate(env)
            val right = right.evaluate(env)
            BinaryOperation.Arithmetic.Pow.apply(left, right)
        }

        is Expression.Negation -> {
            val tvalue = expr.evaluate(env)
            UnaryOperation.Minus.apply(tvalue)
        }

        is Expression.Increment -> {
            val target = expr
            val tvalue = target.evaluate(env)
            val tvalueUpdated = UnaryOperation.Increment.apply(tvalue)

            when (target) {
                is Expression.Variable -> env.setVariable(target.id, tvalueUpdated)
                is Expression.ArrayAccess -> {
                    val arrayIndexing = ArrayAccess(target.arrayExpr, target.indexExpr, env)
                    arrayIndexing.setValue(tvalueUpdated)
                }

                else -> throw InterpretException("cannot increment $target")
            }

            if (postfix) tvalue else tvalueUpdated
        }

        is Expression.Decrement -> {
            val target = expr
            val tvalue = target.evaluate(env)
            val tvalueUpdated = UnaryOperation.Decrement.apply(tvalue)

            when (target) {
                is Expression.Variable -> env.setVariable(target.id, tvalueUpdated)
                is Expression.ArrayAccess -> {
                    val arrayIndexing = ArrayAccess(target.arrayExpr, target.indexExpr, env)
                    arrayIndexing.setValue(tvalueUpdated)
                }

                else -> throw InterpretException("cannot increment $target")
            }

            if (postfix) tvalue else tvalueUpdated
        }

        is Expression.And -> {
            val left = left.evaluate(env)
            val right = { right.evaluate(env) }
            BinaryOperation.Logical.And.apply(left, right)
        }

        is Expression.Or -> {
            val left = left.evaluate(env)
            val right = { right.evaluate(env) }
            BinaryOperation.Logical.Or.apply(left, right)
        }

        is Expression.Not -> {
            val tvalue = expr.evaluate(env)
            UnaryOperation.Not.apply(tvalue)
        }

        is Expression.Variable -> {
            env.resolveVariable(id) ?: throw InterpretException("variable $id not defined")
        }

        is Expression.BoolLiteral -> TValue(BuiltinType.BOOL, value)
        is Expression.FloatLiteral -> TValue(BuiltinType.FLOAT, value)
        is Expression.IntLiteral -> TValue(BuiltinType.INT, value)
        is Expression.StringLiteral -> TValue(BuiltinType.STRING, value)

        is Expression.Equality -> {
            val left = left.evaluate(env)
            val right = right.evaluate(env)
            BinaryOperation.Comparison.Eq.apply(left, right)
        }

        is Expression.Inequality -> {
            val left = left.evaluate(env)
            val right = right.evaluate(env)
            BinaryOperation.Comparison.Neq.apply(left, right)
        }

        is Expression.GreaterThan -> {
            val left = left.evaluate(env)
            val right = right.evaluate(env)
            BinaryOperation.Comparison.Gt.apply(left, right)
        }

        is Expression.GreaterThanOrEqual -> {
            val left = left.evaluate(env)
            val right = right.evaluate(env)
            BinaryOperation.Comparison.Geq.apply(left, right)
        }

        is Expression.LessThan -> {
            val left = left.evaluate(env)
            val right = right.evaluate(env)
            BinaryOperation.Comparison.Lt.apply(left, right)
        }

        is Expression.LessThanOrEqual -> {
            val left = left.evaluate(env)
            val right = right.evaluate(env)
            BinaryOperation.Comparison.Leq.apply(left, right)
        }

        is Expression.ArrayLiteral -> {
            val elements = elements.map { it.evaluate(env) }.toTypedArray()
            if (elements.isEmpty()) {
                TValue.TEmptyArray
            } else {
                val elementType = elements.firstOrNull()?.type ?: BuiltinType.VOID
                TValue(BuiltinType.ARRAY(elementType), elements)
            }
        }

        is Expression.NewArray -> when {
            dimensions.size == 1 -> {
                val dimension = dimensions.first().evaluate(env).let { checkArrayDimension(it) }
                val elements = Array(dimension) { TValue.defaultValueOf(elementType) }
                TValue(resolvedType, elements)
            }

            dimensions.size > 1 -> {
                val dimension = dimensions.first().evaluate(env).let { checkArrayDimension(it) }

                TValue(resolvedType, Array(dimension) {
                    Expression.NewArray(elementType, dimensions.drop(1)).evaluate(env)
                })
            }

            else -> {
                throw InterpretException("cannot create array with zero or negative dimensions")
            }
        }

        is Expression.FunctionCall -> {
            val closure = when (val funcExpr = this.funcExpr) {
                is Expression.Variable -> {
                    env.resolveVariable(funcExpr.id) as? Closure
                        ?: throw InterpretException("function not defined: $funcExpr")
                }

                else -> {
                    funcExpr.evaluate(env) as? Closure
                        ?: throw InterpretException("not a function: $funcExpr")
                }
            }

            val function = closure.function
            val args = args.map { it.evaluate(env) }
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

        is Expression.ArrayAccess -> {
            ArrayAccess(arrayExpr, indexExpr, env).getValue()
        }

        is Expression.Property -> {
            val owner = expr.evaluate(env)
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
            Closure(functionDeclaration, env)
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

        val (functions, variables) = env.getVariables()
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

    private inner class ArrayAccess(arrayExpr: Expression, indexExpr: Expression, env: Environment) {
        val arrayValue: Array<TValue>
        val arrayType: BuiltinType.ARRAY
        val index: Int

        init {
            arrayExpr.evaluate(env).also {
                if (it.type !is BuiltinType.ARRAY) {
                    throw InterpretException("indexing non-array type ${it.type}")
                }
                this.arrayType = it.type
                @Suppress("UNCHECKED_CAST")
                this.arrayValue = it.value as Array<TValue>
            }

            indexExpr.evaluate(env).also {
                if (it.type != BuiltinType.INT) {
                    throw InterpretException("indexing array with non-int type ${it.type}")
                }
                this.index = it.value as Int
            }

            if (index < 0 || index >= arrayValue.size) {
                throw InterpretException("index $index out of bounds for array of size ${arrayValue.size}")
            }
        }

        fun getValue(): TValue = arrayValue[index]

        fun setValue(value: TValue) {
            arrayValue[index] = value
        }
    }
}
