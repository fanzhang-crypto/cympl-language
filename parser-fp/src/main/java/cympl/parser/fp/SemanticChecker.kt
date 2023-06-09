package cympl.parser.fp

import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.utils.Tuple2
import cympl.language.BuiltinType
import cympl.parser.SemanticException
import cympl.parser.TokenLocation
import cympl.language.symbol.*
import java.util.TreeSet

internal class SemanticChecker {

    private var currentScope: Scope? = GlobalScope()

    private val semanticErrors: MutableSet<SemanticException> = TreeSet()

    fun getSemanticErrors(): List<SemanticException> = semanticErrors.toList()

    fun clearSemanticErrors() {
        semanticErrors.clear()
    }

    fun checkIDRef(idToken: TokenMatch) {
        val id = idToken.text
        val symbol: Symbol? = currentScope?.resolve(id)

        if (symbol == null) {
            val location = getLocation(idToken)
            semanticErrors += SemanticException("symbol $id not defined", location)
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

    fun enterFuncDecl(idToken: TokenMatch, type: BuiltinType, paramTypeAndIds: List<Tuple2<BuiltinType, TokenMatch>>?) {
        val function = defineFunc(idToken, type, paramTypeAndIds)
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
        paramTypeAndIds: List<Tuple2<BuiltinType, TokenMatch>>?
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

        val params = paramTypeAndIds?.map { (type, idToken) ->
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
//                println("variable shadowed at $location: $name")
            }
        }

        val id = idToken.text
        val symbol = VariableSymbol(id, type, currentScope)
        currentScope?.define(symbol)
    }

    private fun getLocation(token: TokenMatch) = TokenLocation(token.row, token.column - 1)
}
