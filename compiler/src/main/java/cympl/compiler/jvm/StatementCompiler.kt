package cympl.compiler.jvm

import cympl.compiler.CompilationException
import cympl.language.BuiltinType
import cympl.language.Expression
import cympl.language.Statement
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.TableSwitchGenerator

internal object StatementCompiler {

    fun compile(statement: Statement, ctx: MethodContext) {
        when (statement) {
            is Statement.VariableDeclaration -> statement.compile(ctx)
            is Statement.Assignment -> statement.compile(ctx)
            is Statement.ExpressionStatement -> statement.compile(ctx)
            is Statement.Block -> statement.compile(ctx)
            is Statement.If -> statement.compile(ctx)
            is Statement.While -> statement.compile(ctx)
            is Statement.For -> statement.compile(ctx)
            is Statement.Break -> statement.compile(ctx)
            is Statement.Continue -> statement.compile(ctx)
            is Statement.Switch -> statement.compile(ctx)
            is Statement.Case -> statement.compile(ctx)
            is Statement.Return -> statement.compile(ctx)
            is Statement.FunctionDeclaration -> {
                // leave it to ProgramCompiler
            }
        }
    }

    private fun Statement.VariableDeclaration.compile(ctx: MethodContext) {
        ctx.declareVar(id, type)

        expr?.let {
            ctx.setVar(id, type) {
                ExpressionCompiler.compile(it, ctx)
            }
        }
    }

    private fun Statement.Assignment.compile(ctx: MethodContext) {
        when(val leftExpr = this.leftExpr) {
            is Expression.Variable -> {
                ctx.setVar(leftExpr.id, leftExpr.resolvedType) {
                    ExpressionCompiler.compile(rightExpr, ctx)
                }
            }
            is Expression.ArrayAccess -> {
                ExpressionCompiler.compile(leftExpr.arrayExpr, ctx)
                ExpressionCompiler.compile(leftExpr.indexExpr, ctx)
                ExpressionCompiler.compile(rightExpr, ctx)

                val arrayType = leftExpr.arrayExpr.resolvedType as BuiltinType.ARRAY
                ctx.mv.arrayStore(arrayType.elementType.asmType)
            }
            else -> throw CompilationException("Invalid left expression for assignment: $leftExpr")
        }
    }

    private fun Statement.ExpressionStatement.compile(ctx: MethodContext) {
        when (val expr = this.expr) {
            is Expression.Increment -> ExpressionCompiler.compile(expr, ctx, true)
            is Expression.Decrement -> ExpressionCompiler.compile(expr, ctx, true)
            else -> ExpressionCompiler.compile(expr, ctx)
        }
    }

    private fun Statement.Block.compile(ctx: MethodContext) {
        ctx.enterScope()
        statements.forEach { compile(it, ctx) }
        ctx.exitScope()
    }

    private fun Statement.If.compile(ctx: MethodContext) {
        val endLabel = ctx.mv.newLabel()
        val elseLabel = if (elseBranch == null) endLabel else ctx.mv.newLabel()

        ExpressionCompiler.compile(condition, ctx)

        ctx.mv.ifZCmp(Opcodes.IFEQ, elseLabel)

        compile(thenBranch, ctx)

        if (elseBranch != null) {
            ctx.mv.goTo(endLabel)
            ctx.mv.mark(elseLabel)
            elseBranch?.let { compile(it, ctx) }
        }

        ctx.mv.mark(endLabel)
    }

    private fun Statement.While.compile(ctx: MethodContext) {
        val loopStartLabel = ctx.mv.newLabel()
        val loopEndLabel = ctx.mv.newLabel()

        ctx.loopContext.mark(loopStartLabel, loopEndLabel)

        ctx.mv.mark(loopStartLabel)
        ExpressionCompiler.compile(condition, ctx)
        ctx.mv.ifZCmp(Opcodes.IFEQ, loopEndLabel)
        compile(body, ctx)
        ctx.mv.goTo(loopStartLabel)

        ctx.mv.mark(loopEndLabel)

        ctx.loopContext.unmark()
    }

    private fun Statement.For.compile(ctx: MethodContext) {
        val loopStartLabel = ctx.mv.newLabel()
        val loopEndLabel = ctx.mv.newLabel()
        val loopContinueLabel = ctx.mv.newLabel()

        ctx.loopContext.mark(loopContinueLabel, loopEndLabel)

        this.init?.let { compile(it, ctx) }

        ctx.mv.mark(loopStartLabel)
        this.condition?.let { ExpressionCompiler.compile(it, ctx) }
        ctx.mv.ifZCmp(Opcodes.IFEQ, loopEndLabel)

        compile(this.body, ctx)

        ctx.mv.mark(loopContinueLabel)
        this.update?.let { compile(it, ctx) }
        ctx.mv.goTo(loopStartLabel)

        ctx.mv.mark(loopEndLabel)

        ctx.loopContext.unmark()
    }

    private fun Statement.Break.compile(ctx: MethodContext) {
        ctx.loopContext.nearestBreakLabel?.let {
            ctx.mv.goTo(it)
        } ?: run { throw CompilationException("Break statement outside of loop") }
    }

    private fun Statement.Continue.compile(ctx: MethodContext) {
        ctx.loopContext.nearestContinueLabel?.let {
            ctx.mv.goTo(it)
        } ?: run { throw CompilationException("Continue statement outside of loop") }
    }

    private fun Statement.Return.compile(ctx: MethodContext) {
        if (expr != null) {
            ExpressionCompiler.compile(expr!!, ctx)
            if (ctx.inLambda) {
                ctx.mv.box(expr!!.resolvedType.asmType)
            }
            ctx.mv.returnValue()
        } else {
            ctx.mv.returnValue()
        }
    }

    private fun Statement.Switch.compile(ctx: MethodContext) {
        ExpressionCompiler.compile(expr, ctx)

        val caseByKey = cases.associateBy { (it.condition as Expression.IntLiteral).value }

        val tableSwitchGenerator = object : TableSwitchGenerator {
            override fun generateCase(key: Int, end: Label) {
                val case = caseByKey[key] ?: throw CompilationException("No case for key $key")
                compile(case, ctx)
                if (case.hasBreak) {
                    ctx.mv.goTo(end)
                }
            }

            override fun generateDefault() {
                defaultCase?.let { compile(it, ctx) }
            }
        }

        ctx.mv.tableSwitch(caseByKey.keys.toIntArray(), tableSwitchGenerator)
    }

    private fun Statement.Case.compile(ctx: MethodContext) {
        action?.let { compile(it, ctx) }
    }
}
