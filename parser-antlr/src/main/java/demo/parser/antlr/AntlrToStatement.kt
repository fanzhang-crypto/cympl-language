package demo.parser.antlr

import ExprBaseVisitor
import demo.parser.domain.*
import org.antlr.v4.runtime.Token

internal class AntlrToStatement(private val semanticChecker: SemanticChecker) : ExprBaseVisitor<Statement>() {

    private val antlrToExpression = AntlrToExpression(semanticChecker)

    override fun visitExpression(ctx: ExprParser.ExpressionContext): Statement =
        antlrToExpression.visit(ctx.expr()).let { Statement.ExpressionStatement(it) }

    override fun visitVariableDeclaration(ctx: ExprParser.VariableDeclarationContext): Statement {
        val idToken = ctx.varDecl().ID().symbol

        val id = idToken.text
        val type = resolveType(ctx.varDecl().type)
        val location = TokenLocation(idToken.line, idToken.charPositionInLine)

        semanticChecker.checkVariableUndeclared(id, type, location)

        val value = antlrToExpression.visit(ctx.varDecl().expr())
        return Statement.VariableDeclaration(id, type, value)
    }

    override fun visitAssignment(ctx: ExprParser.AssignmentContext): Statement {
        val idToken = ctx.assign().ID().symbol
        val id = idToken.text
        val location = TokenLocation(idToken.line, idToken.charPositionInLine)
        semanticChecker.checkVariableDeclared(id, location)

        val value = antlrToExpression.visit(ctx.assign().expr())
        return Statement.Assignment(id, value)
    }

    override fun visitFunctionDeclaration(ctx: ExprParser.FunctionDeclarationContext): Statement {
        val idToken = ctx.funcDecl().ID().symbol

        val id = idToken.text
        val location = TokenLocation(idToken.line, idToken.charPositionInLine)
        semanticChecker.checkFunctionUndeclared(id, location)

        val returnType = resolveType(ctx.funcDecl().type)

        return semanticChecker.inNewScope {
            val parameters = ctx.funcDecl().paramDecls().paramDecl().map { visitParamDecl(it) }
            val body = ctx.funcDecl().block().statement().map { visit(it) }.let { Statement.Block(it) }

            Statement.FunctionDeclaration(id, returnType, parameters, body)
        }
    }

    override fun visitParamDecl(ctx: ExprParser.ParamDeclContext): Statement.VariableDeclaration {
        val paramIdToken = ctx.ID().symbol

        val paramId = paramIdToken.text
        val paramType = resolveType(ctx.type)
        val location = TokenLocation(paramIdToken.line, paramIdToken.charPositionInLine)

        semanticChecker.checkVariableUndeclared(paramId, paramType, location)

        return Statement.VariableDeclaration(paramId, paramType)
    }

    override fun visitBlock(ctx: ExprParser.BlockContext): Statement.Block = semanticChecker.inNewScope {
        val statements = ctx.statement().map { visit(it) }
        Statement.Block(statements)
    }

    override fun visitReturnStat(ctx: ExprParser.ReturnStatContext): Statement {
        return Statement.Return(antlrToExpression.visit(ctx.expr()))
    }

    override fun visitIfStatement(ctx: ExprParser.IfStatementContext): Statement {
        val condition = antlrToExpression.visit(ctx.ifStat().expr())
        val thenBranch = visit(ctx.ifStat().thenBranch)
        val elseBranch = ctx.ifStat().elseBranch?.let { visit(it) }
        return Statement.If(condition, thenBranch, elseBranch)
    }

    private fun resolveType(typeToken: Token): VariableType = when (typeToken.type) {
        ExprLexer.INT_TYPE -> VariableType.INT
        ExprLexer.FLOAT_TYPE -> VariableType.FLOAT
        ExprLexer.STRING_TYPE -> VariableType.STRING
        else -> {
            val location = TokenLocation(typeToken.line, typeToken.charPositionInLine)
            throw SyntaxException("unknown type ${typeToken.text}", location)
        }
    }
}
