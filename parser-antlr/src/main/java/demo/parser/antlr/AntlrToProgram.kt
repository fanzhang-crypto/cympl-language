package demo.parser.antlr

import ExprBaseVisitor
import demo.parser.domain.*

internal class AntlrToProgram(private val semanticChecker: SemanticChecker)
    : ExprBaseVisitor<Program>() {

    private val statVisitor = AntlrToStatement(semanticChecker)

    override fun visitProgram(ctx: ExprParser.ProgramContext): Program {
        if (ctx.childCount <= 1) {
            return Program(emptyList())
        }
        val statements = ctx.children.mapNotNull { statVisitor.visit(it) }
        return Program(statements)
    }
}
