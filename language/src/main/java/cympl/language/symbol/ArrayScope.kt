package cympl.language.symbol

import cympl.language.BuiltinType

object ArrayScope : BaseScope(null) {
    override val scopeName: String = "array"

    val LENGTH_PROPERTY = VariableSymbol("length", BuiltinType.INT, this)

    init {
        define(LENGTH_PROPERTY)
    }
}
