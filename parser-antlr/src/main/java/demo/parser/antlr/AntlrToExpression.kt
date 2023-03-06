package demo.parser.antlr

import ExprBaseVisitor
import demo.parser.domain.*

internal class AntlrToExpression(private val semanticChecker: SemanticChecker)
    : ExprBaseVisitor<Expression>() {

    override fun visitFunctionCall(ctx: ExprParser.FunctionCallContext): Expression {
        val idToken = ctx.ID().symbol
        val id = idToken.text
        val tokenLocation = TokenLocation(idToken.line, idToken.charPositionInLine)
        semanticChecker.checkFunctionDeclared(id, tokenLocation)

        val arguments = ctx.exprlist().expr().map { visit(it) }
        return Expression.FunctionCall(id, arguments)
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

    override fun visitEquality(ctx: ExprParser.EqualityContext): Expression {
        val left: Expression = visit(ctx.getChild(0))
        val right: Expression = visit(ctx.getChild(2))

        return when (ctx.op.type) {
            ExprLexer.EQ -> Expression.Equality(left, right)
            ExprLexer.NEQ -> Expression.Inequality(left, right)
            else -> throw RuntimeException("unknown operator ${ctx.op}")
        }
    }

    override fun visitVariable(ctx: ExprParser.VariableContext): Expression {
        val idToken = ctx.ID().symbol
        val id = idToken.text
        val location = TokenLocation(idToken.line, idToken.charPositionInLine)
        semanticChecker.checkVariableDeclared(id, location)
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
