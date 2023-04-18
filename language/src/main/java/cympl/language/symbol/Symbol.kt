package cympl.language.symbol

sealed class Symbol(val name: String, val type: cympl.language.BuiltinType, var scope: Scope? = null) {
    override fun toString() = "<$name:$type>"
}


