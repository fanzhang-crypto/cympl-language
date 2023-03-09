package demo.parser.fp

import demo.parser.domain.SemanticException
import demo.parser.domain.Statement
import demo.parser.domain.TokenLocation
import demo.parser.domain.Type

internal class SemanticChecker {

    class Scope(val parent: Scope? = null, vararg initialVariables: Statement.VariableDeclaration) {

        val vars = initialVariables.associate { it.id to it.type }.toMutableMap()
        val functionNames = mutableSetOf<String>()

        fun containsVariable(id: String, inSameScope: Boolean = false): Boolean =
            if (inSameScope)
                vars.contains(id)
            else
                vars.contains(id) || parent?.containsVariable(id, false) ?: false

        fun containsFunction(id: String, inSameScope: Boolean = false): Boolean =
            if (inSameScope)
                functionNames.contains(id)
            else
                functionNames.contains(id) || parent?.containsFunction(id, false) ?: false


        fun addVariable(id: String, type: Type) {
            vars[id] = type
        }

        fun addFunction(id: String) {
            functionNames += id
        }
    }

    private var currentScope = Scope()

    private val semanticErrors: MutableList<SemanticException> = mutableListOf()

    fun getSemanticErrors(): List<SemanticException> = semanticErrors.toList()

    fun clearSemanticErrors() {
        semanticErrors.clear()
    }

    fun checkVariableDefined(id: String, tokenLocation: TokenLocation) {
        if (!currentScope.containsVariable(id)) {
            semanticErrors += SemanticException("variable $id not defined", tokenLocation)
        }
    }

    fun checkVariableUndeclared(id: String, type: Type, tokenLocation: TokenLocation) {
        if (currentScope.containsVariable(id, true)) {
            semanticErrors += SemanticException("variable $id already defined", tokenLocation)
        } else {
            currentScope.addVariable(id, type)
        }
    }

    fun checkFunctionUndeclared(id: String, location: TokenLocation) {
        if (currentScope.containsFunction(id, true)) {
            semanticErrors += SemanticException("function $id already defined", location)
        } else {
            currentScope.addFunction(id)
        }
    }

    fun checkFunctionDeclared(id: String, location: TokenLocation) {
        if (!currentScope.containsFunction(id)) {
            semanticErrors += SemanticException("function $id not defined", location)
        }
    }

    fun <T> inNewScope(vararg initialVariables: Statement.VariableDeclaration, block: () -> T): T {
        openScope(*initialVariables)
        return block().also { closeScope() }
    }

    fun openScope(vararg initialVariables: Statement.VariableDeclaration) {
        currentScope = Scope(currentScope, *initialVariables)
    }

    fun closeScope() {
        currentScope = currentScope.parent!!
    }

}
