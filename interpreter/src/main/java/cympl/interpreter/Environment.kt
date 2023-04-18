package cympl.interpreter

class Environment(private val parent: Environment? = null) {

    private val variables = mutableMapOf<String, TValue>()

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

    fun getVariables(): Map<String, TValue> {
        return variables.toMap()
    }
}
