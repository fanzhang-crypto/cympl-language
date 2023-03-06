package demo.parser.domain

class SemanticChecker {

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


        fun addVariable(id: String, type: VariableType) {
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

    fun checkVariableDeclared(id: String, tokenLocation: TokenLocation) {
        if (!currentScope.containsVariable(id)) {
            semanticErrors += SemanticException("variable $id not defined", tokenLocation)
        }
    }

    fun checkVariableUndeclared(id: String, type: VariableType, tokenLocation: TokenLocation) {
        if (currentScope.containsVariable(id, true)) {
            semanticErrors += SemanticException("variable $id already declared", tokenLocation)
        } else {
            currentScope.addVariable(id, type)
        }
    }

    fun checkFunctionUndeclared(id: String, location: TokenLocation) {
        if (currentScope.containsFunction(id, true)) {
            semanticErrors += SemanticException("function $id already declared", location)
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
