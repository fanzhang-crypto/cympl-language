package demo.parser.antlr

import CymplBaseListener
import demo.parser.domain.SemanticException
import demo.parser.domain.TokenLocation
import demo.parser.domain.Type
import demo.parser.domain.symbol.*
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeProperty
import org.antlr.v4.runtime.tree.ParseTreeWalker

class SemanticChecker {

    val globals: GlobalScope = GlobalScope()
    val scopes: ParseTreeProperty<Scope> = ParseTreeProperty<Scope>()

    fun check(programAST: ParseTree): List<SemanticException> {
        val walker = ParseTreeWalker.DEFAULT

        val defPhase = DefPhase()
        walker.walk(defPhase, programAST)

        val refPhase = RefPhase()
        walker.walk(refPhase, programAST)

        return (defPhase.semanticErrors + refPhase.semanticErrors).sorted()
    }

    private inner class DefPhase: CymplBaseListener() {

        val semanticErrors = mutableListOf<SemanticException>()

        private var currentScope: Scope? = globals

        override fun enterFuncDecl(ctx: CymplParser.FuncDeclContext) {
            val function = defineFunc(ctx.ID().symbol, ctx.TYPE()?.symbol)
            saveScope(ctx, function)
            currentScope = function
        }

        override fun exitFuncDecl(ctx: CymplParser.FuncDeclContext) {
            currentScope = currentScope?.enclosingScope
            if (semanticErrors.isNotEmpty()) {
                // remove function from scope if there are errors
                currentScope?.remove(ctx.ID().text)
            }
        }

        override fun enterBlock(ctx: CymplParser.BlockContext) {
            currentScope = LocalScope(currentScope)
            saveScope(ctx, currentScope)
        }

        override fun exitBlock(ctx: CymplParser.BlockContext) {
            currentScope = currentScope?.enclosingScope
        }

        override fun exitParamDecl(ctx: CymplParser.ParamDeclContext) {
            defineVar(ctx.ID().symbol, ctx.TYPE().symbol)
        }

        override fun exitVarDecl(ctx: CymplParser.VarDeclContext) {
            defineVar(ctx.ID().symbol, ctx.TYPE().symbol)
        }

        override fun exitReturnStat(ctx: CymplParser.ReturnStatContext) {
            getFunctionDeclarationScope()?.let { function ->
                if (function.type == Type.VOID && ctx.expr() != null) {
                    val location = getLocation(ctx.expr().start)
                    semanticErrors += SemanticException("return value in a void function", location)
                }
            }
        }

        private fun getFunctionDeclarationScope(): FunctionSymbol? {
            var scope = currentScope
            while (scope != null) {
                if (scope is FunctionSymbol) {
                    return scope
                }
                scope = scope.enclosingScope
            }
            return null
        }

        private fun saveScope(ctx: ParserRuleContext, s: Scope?) {
            scopes.put(ctx, s)
        }

        private fun defineFunc(idToken: Token, typeToken: Token?): FunctionSymbol {
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

    private inner class RefPhase : CymplBaseListener() {

        val semanticErrors = mutableListOf<SemanticException>()

        private var currentScope: Scope? = globals

        override fun enterFuncDecl(ctx: CymplParser.FuncDeclContext?) {
            currentScope = scopes[ctx]
        }

        override fun exitFuncDecl(ctx: CymplParser.FuncDeclContext?) {
            currentScope = currentScope?.enclosingScope
        }

        override fun enterBlock(ctx: CymplParser.BlockContext?) {
            currentScope = scopes[ctx]
        }

        override fun exitBlock(ctx: CymplParser.BlockContext?) {
            currentScope = currentScope?.enclosingScope
        }

        override fun exitVariable(ctx: CymplParser.VariableContext) {
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

        override fun exitAssign(ctx: CymplParser.AssignContext) {
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

        override fun exitFunctionCall(ctx: CymplParser.FunctionCallContext) {
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
}



private fun getLocation(token: Token): TokenLocation = TokenLocation(token.line, token.charPositionInLine)
