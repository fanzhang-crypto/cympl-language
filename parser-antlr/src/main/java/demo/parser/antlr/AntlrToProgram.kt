package demo.parser.antlr

import ExprBaseVisitor
import demo.parser.domain.*

internal class AntlrToProgram : ExprBaseVisitor<Program>() {

    private val statVisitor = AntlrToStatement()

    override fun visitProgram(ctx: ExprParser.ProgramContext): Program {
        if (ctx.childCount <= 1) {
            return Program(emptyList())
        }
        val statements = ctx.children.mapNotNull { statVisitor.visit(it) }
        return Program(statements)
    }
}
