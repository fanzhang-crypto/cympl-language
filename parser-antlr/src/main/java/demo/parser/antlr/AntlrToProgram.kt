package demo.parser.antlr

import CymplBaseVisitor
import demo.parser.domain.*

internal class AntlrToProgram : CymplBaseVisitor<Program>() {

    private val statVisitor = AntlrToStatement()

    override fun visitProgram(ctx: CymplParser.ProgramContext): Program {
        if (ctx.childCount <= 1) {
            return Program(emptyList())
        }
        val statements = ctx.children.mapNotNull { statVisitor.visit(it) }
        return Program(statements)
    }
}
