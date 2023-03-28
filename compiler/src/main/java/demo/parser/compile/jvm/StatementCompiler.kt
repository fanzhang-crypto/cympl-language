package demo.parser.compile.jvm

import demo.parser.compile.CompilationException
import demo.parser.domain.BuiltinType
import demo.parser.domain.Expression
import demo.parser.domain.Statement
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.TableSwitchGenerator

internal object StatementCompiler {

    fun compile(statement: Statement, ctx: CompilationContext) {
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
            is Statement.IndexAssignment -> statement.compile(ctx)

            is Statement.Return -> statement.compile(ctx)
            is Statement.FunctionDeclaration -> {
                // leave it to ProgramCompiler
            }
        }
    }

    private fun Statement.VariableDeclaration.compile(ctx: CompilationContext) {
        ctx.declareVar(id, type)

        expr?.let {
            ExpressionCompiler.compile(it, ctx)
            ctx.setVar(id, type)
        }
    }

    private fun Statement.Assignment.compile(ctx: CompilationContext) {
        ExpressionCompiler.compile(expr, ctx)
        ctx.setVar(id, expr.resolvedType)
    }

    private fun Statement.ExpressionStatement.compile(ctx: CompilationContext) {
        when (val expr = this.expr) {
            is Expression.Increment -> ExpressionCompiler.compile(expr, ctx, true)
            is Expression.Decrement -> ExpressionCompiler.compile(expr, ctx, true)
            else -> ExpressionCompiler.compile(expr, ctx)
        }
    }

    private fun Statement.Block.compile(ctx: CompilationContext) {
        ctx.enterScope()
        statements.forEach { compile(it, ctx) }
        ctx.exitScope()
    }

    private fun Statement.If.compile(ctx: CompilationContext) {
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

    private fun Statement.While.compile(ctx: CompilationContext) {
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

    private fun Statement.For.compile(ctx: CompilationContext) {
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

    private fun Statement.Break.compile(ctx: CompilationContext) {
        ctx.loopContext.nearestBreakLabel?.let {
            ctx.mv.goTo(it)
        } ?: run { throw CompilationException("Break statement outside of loop") }
    }

    private fun Statement.Continue.compile(ctx: CompilationContext) {
        ctx.loopContext.nearestContinueLabel?.let {
            ctx.mv.goTo(it)
        } ?: run { throw CompilationException("Continue statement outside of loop") }
    }

    private fun Statement.IndexAssignment.compile(ctx: CompilationContext) {
        ExpressionCompiler.compile(arrayExpr, ctx)
        ExpressionCompiler.compile(indexExpr, ctx)
        ExpressionCompiler.compile(valueExpr, ctx)

        val arrayType = arrayExpr.resolvedType as BuiltinType.ARRAY
        ctx.mv.arrayStore(arrayType.elementType.asmType)
    }

    private fun Statement.Return.compile(ctx: CompilationContext) {
        if (expr != null) {
            ExpressionCompiler.compile(expr!!, ctx)
            ctx.mv.returnValue()
        } else {
            ctx.mv.returnValue()
        }
        ctx.returnGenerated = true
    }

    private fun Statement.Switch.compile(ctx: CompilationContext) {
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

    private fun Statement.Case.compile(ctx: CompilationContext) {
        action?.let { compile(it, ctx) }
    }
}
