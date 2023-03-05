package demo.parser.antlr

import ExprBaseVisitor
import demo.parser.domain.*

internal class AntlrToExpression : ExprBaseVisitor<Expression>() {

    private val vars = mutableMapOf<String, VariableType>()

    private val semanticErrors: MutableList<SemanticException> = mutableListOf()

    fun getSemanticErrors(): List<SemanticException> = semanticErrors.toList()

    fun clearSemanticErrors() {
        semanticErrors.clear()
    }

    override fun visitDeclaration(ctx: ExprParser.DeclarationContext): Expression {
        val idToken = ctx.ID().symbol

        val type = when (ctx.type.type) {
            ExprLexer.INT_TYPE -> VariableType.INT
            ExprLexer.FLOAT_TYPE -> VariableType.FLOAT
            ExprLexer.STRING_TYPE -> VariableType.STRING
            else -> {
                val location = TokenLocation(ctx.type.line, ctx.type.charPositionInLine)
                throw SyntaxException("unknown variable type ${ctx.type.text}", location)
            }
        }

        val id = idToken.text

        if (vars.contains(id)) {
            val location = TokenLocation(idToken.line, idToken.charPositionInLine)
            semanticErrors += SemanticException("variable $id already declared", location)
        } else {
            vars[id] = type
        }
        val value = visit(ctx.expr())
        return Expression.Declaration(id, type, value)
    }

    override fun visitAssignment(ctx: ExprParser.AssignmentContext): Expression {
        val idToken = ctx.ID().symbol
        val id = idToken.text
        if (!vars.contains(id)) {
            val location = TokenLocation(idToken.line, idToken.charPositionInLine)
            semanticErrors += SemanticException("variable $id not defined", location)
        }
        val value = visit(ctx.expr())
        return Expression.Assignment(id, value)
    }

    override fun visitParenthesizedExpression(ctx: ExprParser.ParenthesizedExpressionContext): Expression {
        return Expression.Parenthesized(visit(ctx.expr()))
    }

    override fun visitPower(ctx: ExprParser.PowerContext): Expression {
        val left: Expression = visit(ctx.getChild(0))
        val right: Expression = visit(ctx.getChild(2))
        return Expression.Power(left, right)
    }

    override fun visitMulDiv(ctx: ExprParser.MulDivContext): Expression {
        val left: Expression = visit(ctx.getChild(0))
        val right: Expression = visit(ctx.getChild(2))

        return when (ctx.op.type) {
            ExprLexer.TIMES -> Expression.Multiplication(left, right)
            ExprLexer.DIV -> Expression.Division(left, right)
            else -> throw RuntimeException("unknown operator ${ctx.op}")
        }
    }

    override fun visitAddSub(ctx: ExprParser.AddSubContext): Expression {
        val left: Expression = visit(ctx.getChild(0))
        val right: Expression = visit(ctx.getChild(2))

        return when (ctx.op.type) {
            ExprLexer.PLUS -> Expression.Addition(left, right)
            ExprLexer.MINUS -> Expression.Subtraction(left, right)
            else -> throw RuntimeException("unknown operator ${ctx.op}")
        }
    }

    override fun visitVariable(ctx: ExprParser.VariableContext): Expression {
        val idToken = ctx.ID().symbol
        val id = idToken.text
        if (!vars.contains(id)) {
            val location = TokenLocation(idToken.line, idToken.charPositionInLine)
            semanticErrors += SemanticException("variable $id not defined", location)
        }
        return Expression.Variable(id)
    }

    override fun visitINT(ctx: ExprParser.INTContext): Expression {
        val value = ctx.INT().text.toInt()
        return Expression.Int(value)
    }

    override fun visitFLOAT(ctx: ExprParser.FLOATContext): Expression {
        val value = ctx.FLOAT().text.toDouble()
        return Expression.Float(value)
    }

    override fun visitSTRING(ctx: ExprParser.STRINGContext): Expression {
        val value = ctx.STRING().text.let { it.substring(1, it.length - 1) }
        return Expression.String(value)
    }
}
