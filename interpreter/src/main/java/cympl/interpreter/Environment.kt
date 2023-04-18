package cympl.interpreter

class Environment(private val parent: cympl.interpreter.Environment? = null) {

    private val variables = mutableMapOf<String, cympl.interpreter.TValue>()

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

    fun defineVariable(name: String, value: cympl.interpreter.TValue) {
        variables[name] = value
    }

    fun setVariable(name: String, value: cympl.interpreter.TValue) {
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

    fun resolveVariable(id: String): cympl.interpreter.TValue? {
        return variables[id] ?: parent?.resolveVariable(id)
    }

    fun getVariables(): Map<String, cympl.interpreter.TValue> {
        return variables.toMap()
    }
}
