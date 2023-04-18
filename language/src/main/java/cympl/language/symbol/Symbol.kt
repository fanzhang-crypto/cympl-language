package cympl.language.symbol

import cympl.language.BuiltinType

sealed class Symbol(val name: String, val type: BuiltinType, var scope: Scope? = null) {
    override fun toString() = "<$name:$type>"
}
