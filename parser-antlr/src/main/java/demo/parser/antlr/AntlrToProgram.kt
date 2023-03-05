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
        val expressions = ctx.children.mapNotNull { exprVisitor.visit(it) }
        return Program(expressions)
    }
}
