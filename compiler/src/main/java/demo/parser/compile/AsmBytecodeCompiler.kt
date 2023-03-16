package demo.parser.compile

import demo.parser.domain.BuiltinType
import demo.parser.domain.Expression
import demo.parser.domain.Program
import demo.parser.domain.Statement
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ClassWriter.COMPUTE_FRAMES
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method
import org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintStream
import java.io.PrintWriter
import java.util.*
import kotlin.math.exp

class AsmBytecodeCompiler : ByteCodeCompiler {

    private class LocalVarSlot(val index: Int, val type: BuiltinType)

    private class CompilingContext {
        var localVarTable = mutableMapOf<String, LocalVarSlot>()
        var returnGenerated: Boolean = false
        lateinit var cv: ClassVisitor
        lateinit var mv: GeneratorAdapter
    }

    override fun compile(program: Program): ByteArray {
        val cw = ClassWriter(COMPUTE_FRAMES)
        val printWriter = PrintWriter(System.out)
        val cv = TraceClassVisitor(cw, printWriter)

        val ctx = CompilingContext().apply { this.cv = cv }
        compile(program, ctx)

        cv.visitEnd()
        return cw.toByteArray()
    }

    private fun compile(program: Program, ctx: CompilingContext) = ctx.apply {
        cv.visit(V1_8, ACC_PUBLIC + ACC_SUPER, DEFAULT_MAIN_CLASS_NAME, null, "java/lang/Object", null)

        val mainMethod = Method.getMethod("void main (String[])")
        mv = GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, mainMethod, null, null, cv)

        val methodStart = Label()
        val methodEnd = Label()

        mv.visitLabel(methodStart)

        program.statements.forEach { compile(it, ctx) }

        mv.returnValue()
        mv.visitLabel(methodEnd)

        localVarTable.forEach { (name, index) ->
            mv.visitLocalVariable(name, index.type.jvmDescription, null, methodStart, methodEnd, index.index)
        }

        mv.endMethod()
    }

    private val BuiltinType.asmType
        get():Type = when (this) {
            BuiltinType.VOID -> Type.VOID_TYPE
            BuiltinType.INT -> Type.INT_TYPE
            BuiltinType.BOOL -> Type.BOOLEAN_TYPE
            BuiltinType.FLOAT -> Type.DOUBLE_TYPE
            BuiltinType.STRING -> Type.getType(String::class.java)
            is BuiltinType.ARRAY -> Type.getType("[${elementType.asmType.descriptor}")
            else -> throw CompilationException("unsupported type: $this")
        }

    private val BuiltinType.jvmDescription get() = asmType.descriptor

    private fun compile(stat: Statement, ctx: CompilingContext): Unit = with(ctx) {
        when (stat) {
            is Statement.VariableDeclaration -> {
                val type = stat.type
                val slot = mv.newLocal(type.asmType)
                localVarTable[stat.id] = LocalVarSlot(slot, type)

                stat.expr!!.compile(this)
                mv.storeLocal(slot)
            }

            is Statement.Assignment -> {
                stat.expr.compile(this)
                val slot = localVarTable[stat.id]!!.index
                mv.storeLocal(slot)
            }

            is Statement.ExpressionStatement -> stat.expr.compile(this)
            is Statement.Return -> {
                if (stat.expr != null) {
                    stat.expr!!.compile(this)
                    mv.visitInsn(IRETURN) //返回整数
                } else {
                    mv.visitInsn(RETURN)
                }

                returnGenerated = true
            }

            else -> throw CompilationException("Unknown statement type: $stat")
        }
    }

    private fun Expression.compile(ctx: CompilingContext) {
        when (this) {
            is Expression.Variable -> compile(ctx)
            is Expression.Negation -> compile(ctx)

            is Expression.Addition -> compile(ctx)
            is Expression.Subtraction -> compile(ctx)
            is Expression.Multiplication -> compile(ctx)
            is Expression.Division -> compile(ctx)
            is Expression.Remainder -> compile(ctx)

            is Expression.Increment -> compile(ctx)
            is Expression.Decrement -> compile(ctx)
            is Expression.ComparisonExpression -> compile(ctx)
            is Expression.Not -> compile(ctx)

            is Expression.IntLiteral -> ctx.mv.push(value)
            is Expression.FloatLiteral -> ctx.mv.push(value)
            is Expression.StringLiteral -> ctx.mv.push(value)
            is Expression.BoolLiteral -> ctx.mv.push(value)
            is Expression.ArrayLiteral -> compile(ctx)

            is Expression.FunctionCall -> {
                if (id == "println") {
                    compileIntrinsicCalls(this, ctx)
                }
            }

            else -> throw CompilationException("Unknown expression type: $this")
        }
    }

    private fun Expression.Not.compile(ctx: CompilingContext) {
        expr.compile(ctx)
        ctx.mv.visitInsn(ICONST_1)
        ctx.mv.visitInsn(IXOR)
    }

    private fun Expression.Variable.compile(ctx: CompilingContext) {
        val index = ctx.localVarTable[id]!!.index
        ctx.mv.loadLocal(index)
    }

    private fun Expression.Negation.compile(ctx: CompilingContext) {
        expr.compile(ctx)
        ctx.mv.visitInsn(INEG)
    }

    private fun Expression.Addition.compile(ctx: CompilingContext) {
        val exprType = resolvedType
        left.compileAndCast(ctx, exprType)
        right.compileAndCast(ctx, exprType)
        when (exprType) {
            BuiltinType.INT -> ctx.mv.visitInsn(IADD)
            BuiltinType.FLOAT -> ctx.mv.visitInsn(DADD)
            BuiltinType.STRING -> ctx.mv.invokeVirtual(
                Type.getType(String::class.java),
                Method.getMethod("String concat(String)")
            )

            else -> throw CompilationException("unsupported type for add: $this")
        }
    }

    private fun Expression.Subtraction.compile(ctx: CompilingContext) {
        val exprType = resolvedType
        left.compileAndCast(ctx, exprType)
        right.compileAndCast(ctx, exprType)
        when (exprType) {
            BuiltinType.INT -> ctx.mv.visitInsn(ISUB)
            BuiltinType.FLOAT -> ctx.mv.visitInsn(DSUB)
            else -> throw CompilationException("unsupported type for subtract: $this")
        }
    }

    private fun Expression.Multiplication.compile(ctx: CompilingContext) {
        val exprType = resolvedType
        left.compileAndCast(ctx, exprType)
        right.compileAndCast(ctx, exprType)
        when (exprType) {
            BuiltinType.INT -> ctx.mv.visitInsn(IMUL)
            BuiltinType.FLOAT -> ctx.mv.visitInsn(DMUL)
            else -> throw CompilationException("unsupported type for add: $this")
        }
    }

    private fun Expression.Division.compile(ctx: CompilingContext) {
        val exprType = resolvedType
        left.compileAndCast(ctx, exprType)
        right.compileAndCast(ctx, exprType)
        when (exprType) {
            BuiltinType.INT -> ctx.mv.visitInsn(IDIV)
            BuiltinType.FLOAT -> ctx.mv.visitInsn(DDIV)
            else -> throw CompilationException("unsupported type for divide: $this")
        }
    }

    private fun Expression.Remainder.compile(ctx: CompilingContext) {
        val exprType = resolvedType
        left.compileAndCast(ctx, exprType)
        right.compileAndCast(ctx, exprType)
        when (exprType) {
            BuiltinType.INT -> ctx.mv.visitInsn(IREM)
            BuiltinType.FLOAT -> ctx.mv.visitInsn(DREM)
            else -> throw CompilationException("unsupported type for remainder: $this")
        }
    }

    private fun Expression.Increment.compile(ctx: CompilingContext) {
        val target = expr
        if (target !is Expression.Variable) {
            throw CompilationException("unsupported increment expression: $expr")
        }

        val slot = ctx.localVarTable[target.id]!!.index
        when (val exprType = expr.resolvedType) {
            BuiltinType.INT -> {
                if (postfix) {
                    ctx.mv.loadLocal(slot)
                    ctx.mv.iinc(slot, 1)
                } else {
                    ctx.mv.iinc(slot, 1)
                    ctx.mv.loadLocal(slot)
                }
            }

            BuiltinType.FLOAT -> {
                if (postfix) {
                    ctx.mv.loadLocal(slot)
                    ctx.mv.dup2()
                    ctx.mv.push(1.0)
                    ctx.mv.visitInsn(DADD)
                    ctx.mv.storeLocal(slot)
                } else {
                    ctx.mv.loadLocal(slot)
                    ctx.mv.push(1.0)
                    ctx.mv.visitInsn(DADD)
                    ctx.mv.dup2()
                    ctx.mv.storeLocal(slot)
                }
            }

            else -> throw CompilationException("unsupported type for increment: $exprType")
        }
    }

    private fun Expression.Decrement.compile(ctx: CompilingContext) {
        val target = expr
        if (target !is Expression.Variable) {
            throw CompilationException("unsupported decrement expression: $expr")
        }

        val slot = ctx.localVarTable[target.id]!!.index
        when (val exprType = expr.resolvedType) {
            BuiltinType.INT -> {
                if (postfix) {
                    ctx.mv.loadLocal(slot)
                    ctx.mv.iinc(slot, -1)
                } else {
                    ctx.mv.iinc(slot, -1)
                    ctx.mv.loadLocal(slot)
                }
            }

            BuiltinType.FLOAT -> {
                if (postfix) {
                    ctx.mv.loadLocal(slot)
                    ctx.mv.dup2()
                    ctx.mv.push(1.0)
                    ctx.mv.visitInsn(DSUB)
                    ctx.mv.storeLocal(slot)
                } else {
                    ctx.mv.loadLocal(slot)
                    ctx.mv.push(1.0)
                    ctx.mv.visitInsn(DSUB)
                    ctx.mv.dup2()
                    ctx.mv.storeLocal(slot)
                }
            }

            else -> throw CompilationException("unsupported type for decrement: $exprType")
        }
    }

    private fun Expression.ComparisonExpression.compile(ctx: CompilingContext) {
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
        val falseLabel = Label()

        when (compareType) {
            BuiltinType.BOOL, BuiltinType.INT, BuiltinType.FLOAT -> {
                ctx.mv.ifCmp(compareType.asmType, mode, trueLabel)
                ctx.mv.push(false)
                ctx.mv.goTo(falseLabel)
                ctx.mv.mark(trueLabel)
                ctx.mv.push(true)
                ctx.mv.mark(falseLabel)
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
                        ctx.mv.visitInsn(ICONST_1)
                        ctx.mv.visitInsn(IXOR)
                    }

                    GeneratorAdapter.GT, GeneratorAdapter.GE, GeneratorAdapter.LT, GeneratorAdapter.LE -> {
                        ctx.mv.invokeVirtual(
                            Type.getType(String::class.java),
                            Method.getMethod("int compareTo(String)")
                        )
                        ctx.mv.push(0)
                        ctx.mv.ifCmp(Type.BYTE_TYPE, mode, trueLabel)
                        ctx.mv.push(false)
                        ctx.mv.goTo(falseLabel)
                        ctx.mv.mark(trueLabel)
                        ctx.mv.push(true)
                        ctx.mv.mark(falseLabel)
                    }

                    else -> throw CompilationException("unsupported comparison mode for string: $mode")
                }
            }

            else -> throw CompilationException("unsupported type for comparison: $compareType")
        }
    }

    private fun Expression.ArrayLiteral.compile(ctx: CompilingContext) {
        val elementType = resolvedType.elementType.asmType

        ctx.mv.push(elements.size)
        ctx.mv.newArray(elementType)
        elements.forEachIndexed { index, element ->
            ctx.mv.dup()
            ctx.mv.push(index)
            element.compile(ctx)
            ctx.mv.arrayStore(elementType)
        }
    }

    private fun Expression.compileAndCast(ctx: CompilingContext, expectedType: BuiltinType) {
        compile(ctx)
        when {
            resolvedType == BuiltinType.INT && expectedType == BuiltinType.FLOAT -> ctx.mv.visitInsn(I2D)
            resolvedType == BuiltinType.FLOAT && expectedType == BuiltinType.INT -> ctx.mv.visitInsn(D2I)
            else -> Unit
        }
    }

    private fun compileIntrinsicCalls(functionCall: Expression.FunctionCall, ctx: CompilingContext): Unit = with(ctx) {
        mv.getStatic(Type.getType(System::class.java), "out", Type.getType(PrintStream::class.java))
        val arg = functionCall.args[0]
        arg.compile(this)

        val argType = arg.resolvedType

        if (argType is BuiltinType.ARRAY) {
            if (argType.elementType is BuiltinType.ARRAY) {
                // for arrays with dimension > 1
                mv.invokeStatic(
                    Type.getType(Arrays::class.java),
                    Method.getMethod("String deepToString (Object[])")
                )
            } else {
                mv.invokeStatic(
                    Type.getType(Arrays::class.java),
                    Method.getMethod("String toString (${arg.resolvedType.asmType.className})")
                )
            }
        }

        val typeSignature = if (arg.resolvedType.isPrimitive) arg.resolvedType.asmType.className else "Object"

        mv.invokeVirtual(
            Type.getType(PrintStream::class.java),
            Method.getMethod("void println ($typeSignature)")
        )
    }

    companion object {
        const val DEFAULT_MAIN_CLASS_NAME = "AsmGeneratedMain"
    }
}


