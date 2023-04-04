package demo.parser.compile.jvm

import demo.parser.compile.CompilationException
import demo.parser.domain.BuiltinType
import demo.parser.domain.Expression
import demo.parser.domain.IntrinsicFunction
import demo.parser.domain.symbol.ArrayScope
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method

internal object ExpressionCompiler {

    fun compile(source: Expression, ctx: MethodContext) {
        when (source) {
            is Expression.Parenthesized -> compile(source.expr, ctx)
            is Expression.Variable -> source.compile(ctx)
            is Expression.Negation -> source.compile(ctx)

            is Expression.Addition -> source.compile(ctx)
            is Expression.Subtraction -> source.compile(ctx)
            is Expression.Multiplication -> source.compile(ctx)
            is Expression.Division -> source.compile(ctx)
            is Expression.Remainder -> source.compile(ctx)
            is Expression.Power -> source.compile(ctx)

            is Expression.Increment -> compile(source, ctx)
            is Expression.Decrement -> compile(source, ctx)
            is Expression.ComparisonExpression -> source.compile(ctx)

            is Expression.Not -> source.compile(ctx)
            is Expression.And -> source.compile(ctx)
            is Expression.Or -> source.compile(ctx)

            is Expression.IntLiteral -> ctx.mv.push(source.value)
            is Expression.FloatLiteral -> ctx.mv.push(source.value)
            is Expression.StringLiteral -> ctx.mv.push(source.value)
            is Expression.BoolLiteral -> ctx.mv.push(source.value)
            is Expression.ArrayLiteral -> source.compile(ctx)
            is Expression.Index -> source.compile(ctx)
            is Expression.Property -> source.compile(ctx)
            is Expression.FunctionCall -> source.compile(ctx)
            is Expression.NewArray -> source.compile(ctx)
            is Expression.Lambda -> source.compile(ctx)
        }
    }

    private fun Expression.Lambda.compile(ctx: MethodContext) {
        val lambdaClassType = LambdaCompiler.compile(this, ctx.classContext.rootContext)
        ctx.mv.newInstance(lambdaClassType)
        ctx.mv.dup()

        val argumentTypes = captures.map {
            ctx.getVar(it.id, it.type)
            it.type.asmType
        }.toTypedArray()

        val constructor = Method("<init>", Type.VOID_TYPE, argumentTypes)
        ctx.mv.invokeConstructor(lambdaClassType, constructor)
    }

    private fun Expression.NewArray.compile(ctx: MethodContext) {
        for (dimension in dimensions) {
            compile(dimension, ctx)
        }
        ctx.mv.visitMultiANewArrayInsn(resolvedType.jvmDescriptor, dimensions.size)
    }

    private fun Expression.Property.compile(ctx: MethodContext) {
        if (expr.resolvedType is BuiltinType.ARRAY && propertyName == ArrayScope.LENGTH_PROPERTY.name) {
            compile(expr, ctx)
            ctx.mv.arrayLength()
        }
    }

    private fun Expression.Index.compile(ctx: MethodContext) {
        compile(arrayExpr, ctx)
        compile(indexExpr, ctx)

        val arrayType = (arrayExpr.resolvedType as BuiltinType.ARRAY).elementType
        ctx.mv.arrayLoad(arrayType.asmType)
    }

    private fun Expression.Not.compile(ctx: MethodContext) {
        compile(expr, ctx)
        ctx.mv.not()
    }

    private fun Expression.And.compile(ctx: MethodContext) {
        val trueLabel = Label()
        val falseLabel = Label()
        val endLabel = Label()

        compile(left, ctx)
        ctx.mv.ifZCmp(Opcodes.IFEQ, falseLabel)

        compile(right, ctx)
        ctx.mv.ifZCmp(Opcodes.IFNE, trueLabel)

        ctx.mv.mark(falseLabel)
        ctx.mv.push(false)
        ctx.mv.goTo(endLabel)

        ctx.mv.mark(trueLabel)
        ctx.mv.push(true)

        ctx.mv.mark(endLabel)
    }

    private fun Expression.Or.compile(ctx: MethodContext) {
        val trueLabel = Label()
        val falseLabel = Label()
        val trueEndLabel = Label()
        val falseEndLabel = Label()

        compile(left, ctx)
        ctx.mv.ifZCmp(Opcodes.IFNE, trueLabel)

        compile(right, ctx)
        ctx.mv.ifZCmp(Opcodes.IFEQ, falseLabel)

        ctx.mv.mark(trueLabel)
        ctx.mv.goTo(trueEndLabel)

        ctx.mv.mark(falseLabel)
        ctx.mv.push(false)
        ctx.mv.goTo(falseEndLabel)

        ctx.mv.mark(trueEndLabel)
        ctx.mv.push(true)

        ctx.mv.mark(falseEndLabel)
    }

    private fun Expression.Variable.compile(ctx: MethodContext) {
        ctx.getVar(id, resolvedType)
    }

    private fun Expression.Negation.compile(ctx: MethodContext) {
        val exprType = resolvedType
        expr.compileAndCast(ctx, exprType)
        when (exprType) {
            BuiltinType.INT, BuiltinType.FLOAT ->
                ctx.mv.math(GeneratorAdapter.NEG, exprType.asmType)

            else -> throw CompilationException("unsupported type for negation: $exprType")
        }
    }

    private fun Expression.Addition.compile(ctx: MethodContext) {
        val exprType = resolvedType
        left.compileAndCast(ctx, exprType)
        right.compileAndCast(ctx, exprType)

        when (exprType) {
            BuiltinType.INT, BuiltinType.FLOAT ->
                ctx.mv.math(GeneratorAdapter.ADD, exprType.asmType)

            BuiltinType.STRING -> ctx.mv.invokeVirtual(
                Type.getType(String::class.java),
                Method.getMethod("String concat(String)")
            )

            else -> throw CompilationException("unsupported type for add: $this")
        }
    }

    private fun Expression.Subtraction.compile(ctx: MethodContext) {
        val exprType = resolvedType
        left.compileAndCast(ctx, exprType)
        right.compileAndCast(ctx, exprType)
        when (exprType) {
            BuiltinType.INT, BuiltinType.FLOAT ->
                ctx.mv.math(GeneratorAdapter.SUB, exprType.asmType)

            else -> throw CompilationException("unsupported type for subtract: $this")
        }
    }

    private fun Expression.Multiplication.compile(ctx: MethodContext) {
        val exprType = resolvedType
        left.compileAndCast(ctx, exprType)
        right.compileAndCast(ctx, exprType)
        when (exprType) {
            BuiltinType.INT, BuiltinType.FLOAT ->
                ctx.mv.math(GeneratorAdapter.MUL, exprType.asmType)

            else -> throw CompilationException("unsupported type for add: $this")
        }
    }

    private fun Expression.Division.compile(ctx: MethodContext) {
        val exprType = resolvedType
        left.compileAndCast(ctx, exprType)
        right.compileAndCast(ctx, exprType)
        when (exprType) {
            BuiltinType.INT, BuiltinType.FLOAT ->
                ctx.mv.math(GeneratorAdapter.DIV, exprType.asmType)

            else -> throw CompilationException("unsupported type for divide: $this")
        }
    }

    private fun Expression.Remainder.compile(ctx: MethodContext) {
        val exprType = resolvedType
        left.compileAndCast(ctx, exprType)
        right.compileAndCast(ctx, exprType)
        when (exprType) {
            BuiltinType.INT, BuiltinType.FLOAT ->
                ctx.mv.math(GeneratorAdapter.REM, exprType.asmType)

            else -> throw CompilationException("unsupported type for remainder: $this")
        }
    }

    private fun Expression.Power.compile(ctx: MethodContext) {
        left.compileAndCast(ctx, BuiltinType.FLOAT)
        right.compileAndCast(ctx, BuiltinType.FLOAT)
        ctx.mv.invokeStatic(
            Type.getType(Math::class.java),
            Method.getMethod("double pow(double, double)")
        )
    }

    fun compile(increment: Expression.Increment, ctx: MethodContext, asStatement: Boolean = false) {
        val target = increment.expr
        if (target !is Expression.Variable) {
            throw CompilationException("increment unsupported for expression: $target")
        }
        ctx.increment(target.id, target.resolvedType, increment.postfix, asStatement)
    }

    fun compile(decrement: Expression.Decrement, ctx: MethodContext, asStatement: Boolean = false) {
        val target = decrement.expr
        if (target !is Expression.Variable) {
            throw CompilationException("decrement unsupported for expression: $target")
        }
        ctx.decrement(target.id, target.resolvedType, decrement.postfix, asStatement)
    }

    private fun Expression.ComparisonExpression.compile(ctx: MethodContext) {
        val compareType = BuiltinType.compatibleTypeOf(left.resolvedType, right.resolvedType)

        left.compileAndCast(ctx, compareType)
        right.compileAndCast(ctx, compareType)

        val mode = when (this) {
            is Expression.Equality -> GeneratorAdapter.EQ
            is Expression.GreaterThan -> GeneratorAdapter.GT
            is Expression.GreaterThanOrEqual -> GeneratorAdapter.GE
            is Expression.Inequality -> GeneratorAdapter.NE
            is Expression.LessThan -> GeneratorAdapter.LT
            is Expression.LessThanOrEqual -> GeneratorAdapter.LE
        }
        val trueLabel = Label()
        val endLabel = Label()

        when (compareType) {
            BuiltinType.BOOL, BuiltinType.INT, BuiltinType.FLOAT -> {
                ctx.mv.ifCmp(compareType.asmType, mode, trueLabel)
                ctx.mv.push(false)
                ctx.mv.goTo(endLabel)

                ctx.mv.mark(trueLabel)
                ctx.mv.push(true)

                ctx.mv.mark(endLabel)
            }

            BuiltinType.STRING -> {
                when (mode) {
                    GeneratorAdapter.EQ -> ctx.mv.invokeVirtual(
                        Type.getType(String::class.java),
                        Method.getMethod("boolean equals(Object)")
                    )

                    GeneratorAdapter.NE -> {
                        ctx.mv.invokeVirtual(
                            Type.getType(String::class.java),
                            Method.getMethod("boolean equals(Object)")
                        )
                        ctx.mv.visitInsn(Opcodes.ICONST_1)
                        ctx.mv.visitInsn(Opcodes.IXOR)
                    }

                    GeneratorAdapter.GT, GeneratorAdapter.GE, GeneratorAdapter.LT, GeneratorAdapter.LE -> {
                        ctx.mv.invokeVirtual(
                            Type.getType(String::class.java),
                            Method.getMethod("int compareTo(String)")
                        )
                        ctx.mv.push(0)
                        ctx.mv.ifCmp(Type.BYTE_TYPE, mode, trueLabel)

                        ctx.mv.push(false)
                        ctx.mv.goTo(endLabel)

                        ctx.mv.mark(trueLabel)
                        ctx.mv.push(true)

                        ctx.mv.mark(endLabel)
                    }

                    else -> throw CompilationException("unsupported comparison mode for string: $mode")
                }
            }

            else -> throw CompilationException("unsupported type for comparison: $compareType")
        }
    }

    private fun Expression.ArrayLiteral.compile(ctx: MethodContext) {
        val elementType = resolvedType.elementType.asmType

        ctx.mv.push(elements.size)
        ctx.mv.newArray(elementType)
        elements.forEachIndexed { index, element ->
            ctx.mv.dup()
            ctx.mv.push(index)
            compile(element, ctx)
            ctx.mv.arrayStore(elementType)
        }
    }

    private fun Expression.compileAndCast(ctx: MethodContext, expectedType: BuiltinType) {
        compile(this, ctx)
        when {
            resolvedType == BuiltinType.INT && expectedType == BuiltinType.FLOAT -> ctx.mv.visitInsn(Opcodes.I2D)
            resolvedType == BuiltinType.FLOAT && expectedType == BuiltinType.INT -> ctx.mv.visitInsn(Opcodes.D2I)
            else -> Unit
        }
    }

    private fun Expression.FunctionCall.compile(ctx: MethodContext) {
        val funcExpr = this.funcExpr
        if (funcExpr !is Expression.Variable) {
            //high order function call expression like `f()()` are not supported yet
            throw CompilationException("function call unsupported for expression: $funcExpr")
        }
        val funcType = funcExpr.type
        if (funcType !is BuiltinType.FUNCTION) {
            throw CompilationException("not a function: $funcExpr")
        }

        when (funcExpr.id) {
            IntrinsicFunction.PrintLine.id ->
                IntrinsicCallCompiler.compilePrintLineCall(this, ctx)

            IntrinsicFunction.ReadLine.id ->
                IntrinsicCallCompiler.compileReadLineCall(this, ctx)

            else -> {
                if (funcType.isFirstClass) {
                    ctx.getVar(funcExpr.id, funcType)
                    args.forEach { arg ->
                        compile(arg, ctx)
                        ctx.mv.box(arg.resolvedType.asmType)
                    }
                    val interfaceType = Type.getType(funcType.asJavaFunctionInterface)
                    val interfaceMethod = LambdaCompiler.getApplyBridgeMethod(interfaceType)
                    ctx.mv.invokeInterface(interfaceType, interfaceMethod)
                    ctx.mv.unbox(resolvedType.asmType)
                } else {
                    // normal function call
                    args.forEach { compile(it, ctx) }
                    ctx.invokeMethod(
                        funcExpr.id,
                        args.map { it.resolvedType.asmType }.toTypedArray(),
                        resolvedType.asmType
                    )
                }
            }
        }
    }
}
