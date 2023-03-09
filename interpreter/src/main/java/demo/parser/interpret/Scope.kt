package demo.parser.interpret

import demo.parser.domain.Statement

class Scope(private val parent: Scope? = null) {

    private val variables = mutableMapOf<String, TValue>()

    private val functions = mutableMapOf<String, Statement.FunctionDeclaration>()

    private var inLoop:Boolean = false

    fun isInLoop(): Boolean {
        return inLoop || parent?.isInLoop() ?: false
    }

    fun <T> withinLoop(block: () -> T):T {
        this.inLoop = true
        val result = block()
        this.inLoop = false
        return result
    }

    fun defineVariable(name: String, value: TValue) {
        variables[name] = value
    }

    fun setVariable(name: String, value: TValue) {
        if (variables.contains(name)) {
            variables[name] = value
        } else {
            parent?.setVariable(name, value)
        }
    }

    fun containsVariable(id: String, inCurrentScope:Boolean = false): Boolean =
        if (inCurrentScope)
            variables.contains(id)
        else
            variables.contains(id) || parent?.containsVariable(id, false) ?: false

    fun resolveVariable(id: String): TValue? {
        return variables[id] ?: parent?.resolveVariable(id)
    }

    fun defineFunction(name: String, function: Statement.FunctionDeclaration) {
        functions[name] = function
    }

    fun containsFunction(id: String, inCurrentScope:Boolean = false): Boolean =
        if (inCurrentScope)
            functions.contains(id)
        else
            functions.contains(id) || parent?.containsFunction(id, false) ?: false

    fun resolveFunction(id: String): Statement.FunctionDeclaration? {
        return functions[id] ?: parent?.resolveFunction(id)
    }

    fun getVariables(): Map<String, TValue> {
        return variables.toMap()
    }

    fun getFunctions(): Map<String, Statement.FunctionDeclaration> {
        return functions.toMap()
    }
}
