package demo.parser.antlr

import ExprBaseVisitor
import demo.parser.domain.*

internal class AntlrToStatement(private val semanticChecker: SemanticChecker)
    : ExprBaseVisitor<Statement>() {

    private val antlrToExpression = AntlrToExpression(semanticChecker)

    override fun visitExpression(ctx: ExprParser.ExpressionContext): Statement =
        antlrToExpression.visit(ctx.expr())

    override fun visitVariableDeclaration(ctx: ExprParser.VariableDeclarationContext): Statement {
        val idToken = ctx.decl().ID().symbol

        val type = when (ctx.decl().type.type) {
            ExprLexer.INT_TYPE -> VariableType.INT
            ExprLexer.FLOAT_TYPE -> VariableType.FLOAT
            ExprLexer.STRING_TYPE -> VariableType.STRING
            else -> {
                val location = TokenLocation(ctx.decl().type.line, ctx.decl().type.charPositionInLine)
                throw SyntaxException("unknown variable type ${ctx.decl().type.text}", location)
            }
        }

        semanticChecker.checkVariableUndeclared(idToken, type)

        val id = idToken.text
        val value = antlrToExpression.visit(ctx.decl().expr())
        return Statement.VariableDeclaration(id, type, value)
    }

    override fun visitAssignment(ctx: ExprParser.AssignmentContext): Statement {
        val idToken = ctx.assign().ID().symbol
        semanticChecker.checkVariableDeclared(idToken)

        val id = idToken.text
        val value = antlrToExpression.visit(ctx.assign().expr())
        return Statement.Assignment(id, value)
    }
}
