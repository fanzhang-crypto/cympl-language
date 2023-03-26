package demo.parser.domain.symbol

import demo.parser.domain.BuiltinType

sealed class Symbol(val name: String, val type: BuiltinType, var scope: Scope? = null) {
    override fun toString() = "<$name:$type>"
}


