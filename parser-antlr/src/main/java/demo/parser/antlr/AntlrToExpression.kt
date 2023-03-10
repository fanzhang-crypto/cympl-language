package demo.parser.antlr

import ExprBaseVisitor
import demo.parser.domain.*

internal class AntlrToExpression
    : ExprBaseVisitor<Expression>() {

    override fun visitFunctionCall(ctx: ExprParser.FunctionCallContext): Expression {
        val idToken = ctx.ID().symbol
        val id = idToken.text
        val arguments = ctx.exprlist()?.expr()?.map { visit(it) } ?: emptyList()
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
            ExprLexer.REM -> Expression.Remainder(left, right)
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

    override fun visitNegation(ctx: ExprParser.NegationContext): Expression {
        val expr: Expression = visit(ctx.getChild(1))
        return Expression.Negation(expr)
    }

    override fun visitComparison(ctx: ExprParser.ComparisonContext): Expression {
        val left: Expression = visit(ctx.getChild(0))
        val right: Expression = visit(ctx.getChild(2))

        return when (ctx.op.type) {
            ExprLexer.EQ -> Expression.Equality(left, right)
            ExprLexer.NEQ -> Expression.Inequality(left, right)
            ExprLexer.LT -> Expression.LessThan(left, right)
            ExprLexer.LTE -> Expression.LessThanOrEqual(left, right)
            ExprLexer.GT -> Expression.GreaterThan(left, right)
            ExprLexer.GTE -> Expression.GreaterThanOrEqual(left, right)
            else -> throw RuntimeException("unknown operator ${ctx.op}")
        }
    }

    override fun visitLogicalNot(ctx: ExprParser.LogicalNotContext): Expression {
        val expr: Expression = visit(ctx.getChild(1))
        return Expression.Not(expr)
    }

    override fun visitLogicalAnd(ctx: ExprParser.LogicalAndContext): Expression {
        val left: Expression = visit(ctx.getChild(0))
        val right: Expression = visit(ctx.getChild(2))
        return Expression.And(left, right)
    }

    override fun visitLogicalOr(ctx: ExprParser.LogicalOrContext?): Expression {
        val left: Expression = visit(ctx!!.getChild(0))
        val right: Expression = visit(ctx.getChild(2))
        return Expression.Or(left, right)
    }

    override fun visitVariable(ctx: ExprParser.VariableContext): Expression {
        val idToken = ctx.ID().symbol
        val id = idToken.text
        val location = TokenLocation(idToken.line, idToken.charPositionInLine)
        return Expression.Variable(id)
    }

    override fun visitBOOL(ctx: ExprParser.BOOLContext): Expression = when (ctx.bool.type) {
        ExprLexer.TRUE -> Expression.Bool(true)
        ExprLexer.FALSE -> Expression.Bool(false)
        else -> throw RuntimeException("unknown boolean value ${ctx.bool.text}")
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
