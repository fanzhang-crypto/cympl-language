package demo.parser.antlr

import ExprBaseListener
import demo.parser.domain.SemanticException
import demo.parser.domain.TokenLocation
import demo.parser.domain.Type
import demo.parser.domain.symbol.*
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeProperty
import org.antlr.v4.runtime.tree.ParseTreeWalker

object SemanticChecker {

    fun check(programAST: ParseTree): List<SemanticException> {
        val walker = ParseTreeWalker.DEFAULT

        val defPhase = DefPhase()
        walker.walk(defPhase, programAST)

        val refPhase = RefPhase(defPhase)
        walker.walk(refPhase, programAST)

        return (defPhase.semanticErrors + refPhase.semanticErrors).sorted()
    }
}

private class DefPhase: ExprBaseListener() {

    val semanticErrors = mutableListOf<SemanticException>()

    val scopes: ParseTreeProperty<Scope> = ParseTreeProperty<Scope>()

    val globals: GlobalScope = GlobalScope()

    private var currentScope: Scope? = globals

    override fun exitProgram(ctx: ExprParser.ProgramContext) {
        println(globals)
    }

    override fun enterFuncDecl(ctx: ExprParser.FuncDeclContext) {
        val function = defineFunc(ctx.ID().symbol, ctx.type)
        saveScope(ctx, function)
        currentScope = function
    }

    override fun exitFuncDecl(ctx: ExprParser.FuncDeclContext) {
        println(currentScope)
        currentScope = currentScope?.enclosingScope
    }

    override fun enterBlock(ctx: ExprParser.BlockContext) {
        currentScope = LocalScope(currentScope)
        saveScope(ctx, currentScope)
    }

    override fun exitBlock(ctx: ExprParser.BlockContext) {
        println(currentScope)
        currentScope = currentScope?.enclosingScope
    }

    override fun exitParamDecl(ctx: ExprParser.ParamDeclContext) {
        defineVar(ctx.ID().symbol, ctx.type)
    }

    override fun exitVarDecl(ctx: ExprParser.VarDeclContext) {
        defineVar(ctx.ID().symbol, ctx.type)
    }

    private fun saveScope(ctx: ParserRuleContext, s: Scope?) {
        scopes.put(ctx, s)
    }

    private fun defineFunc(idToken: Token, typeToken: Token): FunctionSymbol {
        val name: String = idToken.text
        val functionSymbol: Symbol? = currentScope?.resolve(name)

        if (functionSymbol != null) {
            val location = getLocation(idToken)
            if (functionSymbol.scope == currentScope) {
                semanticErrors += SemanticException("function $name already defined", location)
            } else {
                println("function shadowed at $location: $name")
            }
        }

        val type: Type = TypeResolver.resolveType(typeToken)

        return FunctionSymbol(name, type, currentScope)
            .also { currentScope?.define(it) }
    }

    private fun defineVar(idToken: Token, typeToken: Token) {
        val name: String = idToken.text
        val variableSymbol: Symbol? = currentScope?.resolve(name)

        if (variableSymbol != null) {
            val location = getLocation(idToken)
            if (variableSymbol.scope == currentScope) {
                semanticErrors += SemanticException("variable $name already defined", location)
            } else {
                println("variable shadowed at $location: $name")
            }
        }

        val id = idToken.text
        val type = TypeResolver.resolveType(typeToken)
        val symbol = VariableSymbol(id, type, currentScope)
        currentScope?.define(symbol)
    }
}

private class RefPhase(predecessor: DefPhase) : ExprBaseListener() {

    val semanticErrors = mutableListOf<SemanticException>()

    private val scopes: ParseTreeProperty<Scope> = predecessor.scopes
    private var currentScope: Scope? = predecessor.globals

    override fun enterFuncDecl(ctx: ExprParser.FuncDeclContext?) {
        currentScope = scopes[ctx]
    }

    override fun exitFuncDecl(ctx: ExprParser.FuncDeclContext?) {
        currentScope = currentScope?.enclosingScope
    }

    override fun enterBlock(ctx: ExprParser.BlockContext?) {
        currentScope = scopes[ctx]
    }

    override fun exitBlock(ctx: ExprParser.BlockContext?) {
        currentScope = currentScope?.enclosingScope
    }

    override fun exitVariable(ctx: ExprParser.VariableContext) {
        val idToken = ctx.ID()
        val varName: String = idToken.text
        val variableSymbol: Symbol? = currentScope?.resolve(varName)

        if (variableSymbol == null) {
            val location = getLocation(ctx.ID().symbol)
            semanticErrors += SemanticException("variable $varName not defined", location)
        } else if (variableSymbol !is VariableSymbol) {
            val location = getLocation(ctx.ID().symbol)
            semanticErrors += SemanticException("$varName is not a variable", location)
        }
    }

    override fun exitAssign(ctx: ExprParser.AssignContext) {
        val idToken = ctx.ID()
        val varName: String = idToken.text
        val variableSymbol: Symbol? = currentScope?.resolve(varName)

        if (variableSymbol == null) {
            val location = getLocation(ctx.ID().symbol)
            semanticErrors += SemanticException("variable $varName not defined", location)
        } else if (variableSymbol !is VariableSymbol) {
            val location = getLocation(ctx.ID().symbol)
            semanticErrors += SemanticException("$varName is not a variable", location)
        }
    }

    override fun exitFunctionCall(ctx: ExprParser.FunctionCallContext) {
        val idToken = ctx.ID()
        val functionName = idToken.text
        val functionSymbol: Symbol? = currentScope?.resolve(functionName)

        if (functionSymbol == null) {
            val location = getLocation(idToken.symbol)
            semanticErrors += SemanticException("function: $functionName not defined", location)
        } else if (functionSymbol !is FunctionSymbol) {
            val location = getLocation(idToken.symbol)
            semanticErrors += SemanticException("$functionName is not a function", location)
        }
    }
}

private fun getLocation(token: Token): TokenLocation = TokenLocation(token.line, token.charPositionInLine)
