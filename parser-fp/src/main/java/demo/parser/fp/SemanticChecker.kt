package demo.parser.fp

import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.utils.Tuple2
import demo.parser.domain.SemanticException
import demo.parser.domain.TokenLocation
import demo.parser.domain.BuiltinType
import demo.parser.domain.symbol.*

internal class SemanticChecker {

    private var currentScope: Scope? = GlobalScope()

    private val semanticErrors: MutableList<SemanticException> = mutableListOf()

    fun getSemanticErrors(): List<SemanticException> = semanticErrors.toList()

    fun clearSemanticErrors() {
        semanticErrors.clear()
    }

    fun checkVariableRef(idToken: TokenMatch) {
        val varName: String = idToken.text
        val variableSymbol: Symbol? = currentScope?.resolve(varName)

        if (variableSymbol == null) {
            val location = getLocation(idToken)
            semanticErrors += SemanticException("variable $varName not defined", location)
        } else if (variableSymbol !is VariableSymbol) {
            val location = getLocation(idToken)
            semanticErrors += SemanticException("$varName is not a variable", location)
        }
    }

    fun checkFunctionRef(idToken: TokenMatch) {
        val functionName = idToken.text
        val functionSymbol: Symbol? = currentScope?.resolve(functionName)

        if (functionSymbol == null) {
            val location = getLocation(idToken)
            semanticErrors += SemanticException("function: $functionName not defined", location)
        } else if (functionSymbol !is FunctionSymbol) {
            val location = getLocation(idToken)
            semanticErrors += SemanticException("$functionName is not a function", location)
        }
    }

    fun enterFuncDecl(idToken: TokenMatch, type: BuiltinType, paramIdAndTypes: List<Tuple2<TokenMatch, BuiltinType>>?) {
        val function = defineFunc(idToken, type, paramIdAndTypes)
//        saveScope(ctx, function)
        currentScope = function
    }

    fun exitFuncDecl() {
        println(currentScope)
        currentScope = currentScope?.enclosingScope
    }

    fun enterBlock() {
        currentScope = LocalScope(currentScope)
//        saveScope(ctx, currentScope)
    }

    fun exitBlock() {
        currentScope = currentScope?.enclosingScope
    }

    private fun defineFunc(
        idToken: TokenMatch,
        type: BuiltinType,
        paramIdAndTypes: List<Tuple2<TokenMatch, BuiltinType>>?
    ): FunctionSymbol {
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

        val params = paramIdAndTypes?.map { (idToken, type) ->
            val id = idToken.text
            VariableSymbol(id, type, currentScope)
        } ?: emptyList()

        return FunctionSymbol(name, type, params, currentScope)
            .also { currentScope?.define(it) }
    }

    fun defineVar(idToken: TokenMatch, type: BuiltinType) {
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
        val symbol = VariableSymbol(id, type, currentScope)
        currentScope?.define(symbol)
    }

    private fun getLocation(token: TokenMatch) = TokenLocation(token.row, token.column - 1)
}
