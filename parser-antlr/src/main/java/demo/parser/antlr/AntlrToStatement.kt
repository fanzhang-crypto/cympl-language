package demo.parser.antlr

import ExprBaseVisitor
import demo.parser.antlr.TypeResolver.resolveType
import demo.parser.domain.*

internal class AntlrToStatement : ExprBaseVisitor<Statement>() {

    private val antlrToExpression = AntlrToExpression()

    override fun visitExpression(ctx: ExprParser.ExpressionContext): Statement =
        antlrToExpression.visit(ctx.expr()).let { Statement.ExpressionStatement(it) }

    override fun visitVarDecl(ctx: ExprParser.VarDeclContext): Statement {
        val idToken = ctx.ID().symbol

        val id = idToken.text
        val type = resolveType(ctx.type)
        val value = antlrToExpression.visit(ctx.expr())
        return Statement.VariableDeclaration(id, type, value)
    }

    override fun visitAssign(ctx: ExprParser.AssignContext): Statement {
        val idToken = ctx.ID().symbol
        val id = idToken.text
        val value = antlrToExpression.visit(ctx.expr())
        return Statement.Assignment(id, value)
    }

    override fun visitFuncDecl(ctx: ExprParser.FuncDeclContext): Statement {
        val idToken = ctx.ID().symbol

        val id = idToken.text
        val returnType = resolveType(ctx.type)
        val parameters = ctx.paramDecls()?.paramDecl()?.map { visitParamDecl(it) } ?: emptyList()
        val body = ctx.block().statement().map { visit(it) }.let { Statement.Block(it) }

        return Statement.FunctionDeclaration(id, returnType, parameters, body)
    }

    override fun visitIfStat(ctx: ExprParser.IfStatContext): Statement {
        val condition = antlrToExpression.visit(ctx.expr())
        val thenBranch = visit(ctx.thenBranch)
        val elseBranch = ctx.elseBranch?.let { visit(it) }
        return Statement.If(condition, thenBranch, elseBranch)
    }

    override fun visitWhileStat(ctx: ExprParser.WhileStatContext): Statement {
        val condition = antlrToExpression.visit(ctx.expr())
        val body = visit(ctx.statement())
        return Statement.While(condition, body)
    }

    override fun visitForInit(ctx: ExprParser.ForInitContext): Statement =
        if (ctx.varDecl() != null) {
            visit(ctx.varDecl())
        } else {
            visit(ctx.assign())
        }

    override fun visitForStat(ctx: ExprParser.ForStatContext): Statement {
        val init = ctx.forInit()?.let { visit(it) }
        val condition = ctx.cond?.let { antlrToExpression.visit(it) }
        val update = ctx.update?.let { visit(it) }
        val body = visit(ctx.statement())
        return Statement.For(init, condition, update, body)
    }

    override fun visitAssignment(ctx: ExprParser.AssignmentContext) = visitAssign(ctx.assign())

    override fun visitVariableDeclaration(ctx: ExprParser.VariableDeclarationContext) = visitVarDecl(ctx.varDecl())

    override fun visitFunctionDeclaration(ctx: ExprParser.FunctionDeclarationContext) = visitFuncDecl(ctx.funcDecl())

    override fun visitParamDecl(ctx: ExprParser.ParamDeclContext): Statement.VariableDeclaration {
        val paramIdToken = ctx.ID().symbol

        val paramId = paramIdToken.text
        val paramType = resolveType(ctx.type)

        return Statement.VariableDeclaration(paramId, paramType)
    }

    override fun visitBlock(ctx: ExprParser.BlockContext): Statement.Block {
        val statements = ctx.statement().map { visit(it) }
        return Statement.Block(statements)
    }

    override fun visitReturnStat(ctx: ExprParser.ReturnStatContext) =
        Statement.Return(antlrToExpression.visit(ctx.expr()))

    override fun visitIfStatement(ctx: ExprParser.IfStatementContext) = visitIfStat(ctx.ifStat())

    override fun visitWhileStatement(ctx: ExprParser.WhileStatementContext) =
        visitWhileStat(ctx.whileStat())

    override fun visitForStatement(ctx: ExprParser.ForStatementContext) = visitForStat(ctx.forStat())

    override fun visitBreakStatement(ctx: ExprParser.BreakStatementContext) = Statement.Break()

    override fun visitContinueStatement(ctx: ExprParser.ContinueStatementContext) = Statement.Continue()


}
