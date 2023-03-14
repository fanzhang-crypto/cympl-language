package demo.parser.antlr

import CymplBaseVisitor
import demo.parser.domain.*

internal class AntlrToExpression
    : CymplBaseVisitor<Expression>() {

    override fun visitFunctionCall(ctx: CymplParser.FunctionCallContext): Expression {
        val idToken = ctx.ID().symbol
        val id = idToken.text
        val arguments = ctx.exprlist()?.expr()?.map { visit(it) } ?: emptyList()
        return Expression.FunctionCall(id, arguments)
    }

    override fun visitIndex(ctx: CymplParser.IndexContext): Expression {
        val array = visit(ctx.arrayExpr)
        val index = visit(ctx.indexExpr)
        return Expression.Index(array, index)
    }

    override fun visitParenthesizedExpression(ctx: CymplParser.ParenthesizedExpressionContext): Expression {
        return Expression.Parenthesized(visit(ctx.expr()))
    }

    override fun visitPower(ctx: CymplParser.PowerContext): Expression {
        val left: Expression = visit(ctx.getChild(0))
        val right: Expression = visit(ctx.getChild(2))
        return Expression.Power(left, right)
    }

    override fun visitMulDiv(ctx: CymplParser.MulDivContext): Expression {
        val left: Expression = visit(ctx.getChild(0))
        val right: Expression = visit(ctx.getChild(2))

        return when (ctx.op.type) {
            CymplLexer.TIMES -> Expression.Multiplication(left, right)
            CymplLexer.DIV -> Expression.Division(left, right)
            CymplLexer.REM -> Expression.Remainder(left, right)
            else -> throw RuntimeException("unknown operator ${ctx.op}")
        }
    }

    override fun visitAddSub(ctx: CymplParser.AddSubContext): Expression {
        val left: Expression = visit(ctx.getChild(0))
        val right: Expression = visit(ctx.getChild(2))

        return when (ctx.op.type) {
            CymplLexer.PLUS -> Expression.Addition(left, right)
            CymplLexer.MINUS -> Expression.Subtraction(left, right)
            else -> throw RuntimeException("unknown operator ${ctx.op}")
        }
    }

    override fun visitNegation(ctx: CymplParser.NegationContext): Expression {
        val expr: Expression = visit(ctx.getChild(1))
        return Expression.Negation(expr)
    }

    override fun visitPreIncDec(ctx: CymplParser.PreIncDecContext): Expression {
        val expr = visit(ctx.getChild(1))
        return when (ctx.op.type) {
            CymplLexer.INC -> Expression.Increment(expr, false)
            CymplLexer.DEC -> Expression.Decrement(expr, false)
            else -> throw RuntimeException("unknown operator ${ctx.op}")
        }
    }

    override fun visitPostIncDec(ctx: CymplParser.PostIncDecContext): Expression {
        val expr = visit(ctx.getChild(0))
        return when (ctx.op.type) {
            CymplLexer.INC -> Expression.Increment(expr, true)
            CymplLexer.DEC -> Expression.Decrement(expr, true)
            else -> throw RuntimeException("unknown operator ${ctx.op}")
        }
    }

    override fun visitComparison(ctx: CymplParser.ComparisonContext): Expression {
        val left: Expression = visit(ctx.getChild(0))
        val right: Expression = visit(ctx.getChild(2))

        return when (ctx.op.type) {
            CymplLexer.EQ -> Expression.Equality(left, right)
            CymplLexer.NEQ -> Expression.Inequality(left, right)
            CymplLexer.LT -> Expression.LessThan(left, right)
            CymplLexer.LTE -> Expression.LessThanOrEqual(left, right)
            CymplLexer.GT -> Expression.GreaterThan(left, right)
            CymplLexer.GTE -> Expression.GreaterThanOrEqual(left, right)
            else -> throw RuntimeException("unknown operator ${ctx.op}")
        }
    }

    override fun visitLogicalNot(ctx: CymplParser.LogicalNotContext): Expression {
        val expr: Expression = visit(ctx.getChild(1))
        return Expression.Not(expr)
    }

    override fun visitLogicalAnd(ctx: CymplParser.LogicalAndContext): Expression {
        val left: Expression = visit(ctx.getChild(0))
        val right: Expression = visit(ctx.getChild(2))
        return Expression.And(left, right)
    }

    override fun visitLogicalOr(ctx: CymplParser.LogicalOrContext?): Expression {
        val left: Expression = visit(ctx!!.getChild(0))
        val right: Expression = visit(ctx.getChild(2))
        return Expression.Or(left, right)
    }

    override fun visitVariable(ctx: CymplParser.VariableContext): Expression {
        val idToken = ctx.ID().symbol
        val id = idToken.text
        return Expression.Variable(id)
    }

    override fun visitBOOL(ctx: CymplParser.BOOLContext): Expression = when (ctx.bool.type) {
        CymplLexer.TRUE -> Expression.Bool(true)
        CymplLexer.FALSE -> Expression.Bool(false)
        else -> throw RuntimeException("unknown boolean value ${ctx.bool.text}")
    }

    override fun visitINT(ctx: CymplParser.INTContext): Expression {
        val value = ctx.INT().text.toInt()
        return Expression.Int(value)
    }

    override fun visitFLOAT(ctx: CymplParser.FLOATContext): Expression {
        val value = ctx.FLOAT().text.toDouble()
        return Expression.Float(value)
    }

    override fun visitSTRING(ctx: CymplParser.STRINGContext): Expression {
        val value = ctx.STRING().text.let { it.substring(1, it.length - 1) }
        return Expression.String(value)
    }

    override fun visitArrayExpression(ctx: CymplParser.ArrayExpressionContext): Expression {
        val elements = ctx.exprlist()?.expr()?.map { visit(it) } ?: emptyList()
        return Expression.Array(elements)
    }
}
