package demo.parser.antlr

import ExprBaseVisitor
import demo.parser.domain.*


internal class AntlrToExpression: ExprBaseVisitor<Expression>() {

    private val vars = mutableSetOf<String>()

    private val semanticErrors: MutableList<SemanticException> = mutableListOf()

    fun getSemanticErrors(): List<SemanticException> = semanticErrors.toList()

    fun clearSemanticErrors() {
        semanticErrors.clear()
    }

    override fun visitDeclaration(ctx: ExprParser.DeclarationContext): Expression {
        val idToken = ctx.ID().symbol

        val id = idToken.text
        if (vars.contains(id)) {
            val location = TokenLocation(idToken.line, idToken.charPositionInLine)
            semanticErrors += SemanticException("variable $id already declared", location)
        } else {
            vars += id
        }

        val type = ctx.INT_TYPE().text
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

    override fun visitMultiplication(ctx: ExprParser.MultiplicationContext): Expression {
        val left: Expression = visit(ctx.getChild(0))
        val right: Expression = visit(ctx.getChild(2))
        return Multiplication(left, right)
    }

    override fun visitDivision(ctx: ExprParser.DivisionContext): Expression {
        val left: Expression = visit(ctx.getChild(0))
        val right: Expression = visit(ctx.getChild(2))
        return Division(left, right)
    }

    override fun visitAddition(ctx: ExprParser.AdditionContext): Expression {
        val left: Expression = visit(ctx.getChild(0))
        val right: Expression = visit(ctx.getChild(2))
        return Addition(left, right)
    }

    override fun visitSubstraction(ctx: ExprParser.SubstractionContext): Expression {
        val left: Expression = visit(ctx.getChild(0))
        val right: Expression = visit(ctx.getChild(2))
        return Subtraction(left, right)
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

    override fun visitNumber(ctx: ExprParser.NumberContext): Expression {
        val num = ctx.NUM().text.toInt()
        return Number(num)
    }
}
