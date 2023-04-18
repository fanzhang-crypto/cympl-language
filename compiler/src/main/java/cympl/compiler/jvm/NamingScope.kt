package cympl.compiler.jvm

internal class NamingScope(val parent: NamingScope?) {

    private val nameMapping = mutableMapOf<String, String>()

    val isRoot: Boolean get() = parent == null

    fun add(name: String):String {
        var index = 0
        var alias = name
        while(alias in nameMapping) {
            // name already exists, add a suffix to avoid conflict
            alias += index++
        }
        nameMapping[name] = alias
        return alias
    }

    fun get(name: String):String? {
        return nameMapping[name] ?: parent?.get(name)
    }
}
