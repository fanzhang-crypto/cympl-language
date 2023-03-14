package demo.parser.antlr

import CymplBaseVisitor
import demo.parser.antlr.TypeResolver.resolveType
import demo.parser.domain.*

internal class AntlrToStatement : CymplBaseVisitor<Statement>() {

    private val antlrToExpression = AntlrToExpression()

    override fun visitExpression(ctx: CymplParser.ExpressionContext): Statement =
        antlrToExpression.visit(ctx.expr()).let { Statement.ExpressionStatement(it) }

    override fun visitVarDecl(ctx: CymplParser.VarDeclContext): Statement {
        val idToken = ctx.ID().symbol

        val id = idToken.text
        val type = resolveType(ctx.type())
        val value = antlrToExpression.visit(ctx.expr())
        return Statement.VariableDeclaration(id, type, value)
    }

    override fun visitAssign(ctx: CymplParser.AssignContext): Statement {
        val idToken = ctx.ID().symbol
        val id = idToken.text
        val value = antlrToExpression.visit(ctx.expr())
        return Statement.Assignment(id, value)
    }

    override fun visitIndexAssign(ctx: CymplParser.IndexAssignContext): Statement {
        val array = antlrToExpression.visit(ctx.arrayExpr)
        val index = antlrToExpression.visit(ctx.indexExpr)
        val value = antlrToExpression.visit(ctx.valueExpr)
        return Statement.IndexAssignment(array, index, value)
    }

    override fun visitFuncDecl(ctx: CymplParser.FuncDeclContext): Statement {
        val idToken = ctx.ID().symbol

        val id = idToken.text
        val returnType = resolveType(ctx.type())
        val parameters = ctx.paramDecls()?.paramDecl()?.map { visitParamDecl(it) } ?: emptyList()
        val body = ctx.block().statement().map { visit(it) }.let { Statement.Block(it) }

        return Statement.FunctionDeclaration(id, returnType, parameters, body)
    }

    override fun visitIfStat(ctx: CymplParser.IfStatContext): Statement {
        val condition = antlrToExpression.visit(ctx.expr())
        val thenBranch = visit(ctx.thenBranch)
        val elseBranch = ctx.elseBranch?.let { visit(it) }
        return Statement.If(condition, thenBranch, elseBranch)
    }

    override fun visitWhileStat(ctx: CymplParser.WhileStatContext): Statement {
        val condition = antlrToExpression.visit(ctx.expr())
        val body = visit(ctx.statement())
        return Statement.While(condition, body)
    }

    override fun visitForInit(ctx: CymplParser.ForInitContext): Statement =
        if (ctx.varDecl() != null) {
            visit(ctx.varDecl())
        } else {
            visit(ctx.assign())
        }

    override fun visitForStat(ctx: CymplParser.ForStatContext): Statement {
        val init = ctx.forInit()?.let { visit(it) }
        val condition = ctx.cond?.let { antlrToExpression.visit(it) }

        val updateExpr = ctx.updateExpr?.let { antlrToExpression.visit(it).toStatement() }
        val updateAssign = ctx.updateAssign?.let { visit(it) }
        val update = updateExpr ?: updateAssign

        val body = visit(ctx.statement())
        return Statement.For(init, condition, update, body)
    }

    override fun visitAssignment(ctx: CymplParser.AssignmentContext) = visitAssign(ctx.assign())

    override fun visitIndexAssignment(ctx: CymplParser.IndexAssignmentContext) = visitIndexAssign(ctx.indexAssign())

    override fun visitVariableDeclaration(ctx: CymplParser.VariableDeclarationContext) = visitVarDecl(ctx.varDecl())

    override fun visitFunctionDeclaration(ctx: CymplParser.FunctionDeclarationContext) = visitFuncDecl(ctx.funcDecl())

    override fun visitParamDecl(ctx: CymplParser.ParamDeclContext): Statement.VariableDeclaration {
        val paramIdToken = ctx.ID().symbol

        val paramId = paramIdToken.text
        val paramType = resolveType(ctx.type())

        return Statement.VariableDeclaration(paramId, paramType)
    }

    override fun visitBlock(ctx: CymplParser.BlockContext): Statement.Block {
        val statements = ctx.statement().map { visit(it) }
        return Statement.Block(statements)
    }

    override fun visitReturnStat(ctx: CymplParser.ReturnStatContext): Statement.Return {
        val value = ctx.expr()?.let { antlrToExpression.visit(it) }
        return Statement.Return(value)
    }

    override fun visitIfStatement(ctx: CymplParser.IfStatementContext) = visitIfStat(ctx.ifStat())

    override fun visitWhileStatement(ctx: CymplParser.WhileStatementContext) =
        visitWhileStat(ctx.whileStat())

    override fun visitForStatement(ctx: CymplParser.ForStatementContext) = visitForStat(ctx.forStat())

    override fun visitBreakStatement(ctx: CymplParser.BreakStatementContext) = Statement.Break()

    override fun visitContinueStatement(ctx: CymplParser.ContinueStatementContext) = Statement.Continue()


}
