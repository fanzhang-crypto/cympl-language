package demo.parser.antlr

import ExprBaseVisitor
import demo.parser.domain.*
import demo.parser.domain.Float
import demo.parser.domain.Int


internal class AntlrToExpression : ExprBaseVisitor<Expression>() {

    private val vars = mutableMapOf<String, VariableType>()

    private val semanticErrors: MutableList<SemanticException> = mutableListOf()

    fun getSemanticErrors(): List<SemanticException> = semanticErrors.toList()

    fun clearSemanticErrors() {
        semanticErrors.clear()
    }

    override fun visitDeclaration(ctx: ExprParser.DeclarationContext): Expression {
        val idToken = ctx.ID().symbol

        val type = when (ctx.TYPE().text) {
            "INT" -> VariableType.INT
            "FLOAT" -> VariableType.FLOAT
            else -> {
                val location = TokenLocation(ctx.TYPE().symbol.line, ctx.TYPE().symbol.charPositionInLine)
                throw SyntaxException("unknown variable type ${ctx.TYPE().text}", location)
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
        return Declaration(id, type, value)
    }

    override fun visitAssignment(ctx: ExprParser.AssignmentContext): Expression {
        val idToken = ctx.ID().symbol
        val id = idToken.text
        if (!vars.contains(id)) {
            val location = TokenLocation(idToken.line, idToken.charPositionInLine)
            semanticErrors += SemanticException("variable $id not defined", location)
        }
        val value = visit(ctx.expr())
        return Assignment(id, value)
    }

    override fun visitParenthesizedExpression(ctx: ExprParser.ParenthesizedExpressionContext): Expression {
        return Parenthesized(visit(ctx.expr()))
    }

    override fun visitPower(ctx: ExprParser.PowerContext): Expression {
        val left: Expression = visit(ctx.getChild(0))
        val right: Expression = visit(ctx.getChild(2))
        return Power(left, right)
    }

    override fun visitMulDiv(ctx: ExprParser.MulDivContext): Expression {
        val left: Expression = visit(ctx.getChild(0))
        val right: Expression = visit(ctx.getChild(2))

        return when (ctx.op.type) {
            ExprLexer.TIMES -> Multiplication(left, right)
            ExprLexer.DIV -> Division(left, right)
            else -> throw RuntimeException("unknown operator ${ctx.op}")
        }
    }

    override fun visitAddSub(ctx: ExprParser.AddSubContext): Expression {
        val left: Expression = visit(ctx.getChild(0))
        val right: Expression = visit(ctx.getChild(2))

        return when (ctx.op.type) {
            ExprLexer.PLUS -> Addition(left, right)
            ExprLexer.MINUS -> Subtraction(left, right)
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
        return Variable(id)
    }

    override fun visitINT(ctx: ExprParser.INTContext): Expression {
        val value = ctx.INT().text.toInt()
        return Int(value)
    }

    override fun visitFLOAT(ctx: ExprParser.FLOATContext): Expression {
        val value = ctx.FLOAT().text.toDouble()
        return Float(value)
    }

}
