package demo.parser.antlr

import ExprBaseVisitor
import demo.parser.domain.*

internal class AntlrToProgram : ExprBaseVisitor<Program>() {

    private val exprVisitor = AntlrToExpression()

    fun getSemanticErrors() = exprVisitor.getSemanticErrors()

    fun clearSemanticErrors() {
        exprVisitor.clearSemanticErrors()
    }

    override fun visitProgram(ctx: ExprParser.ProgramContext): Program {
        if (ctx.childCount <= 1) {
            return Program(emptyList())
        }

        val expressions = mutableListOf<Expression>()

        for (i in 0 until ctx.childCount - 1) {
            val child = ctx.getChild(i)
            val expr = exprVisitor.visit(child)
            expressions += expr
        }
        return Program(expressions)
    }
}
