package demo.parser.domain.symbol

import demo.parser.domain.BuiltinType

object ArrayScope : BaseScope(null) {
    override val scopeName: String = "array"

    val LENGTH_PROPERTY = VariableSymbol("length", BuiltinType.INT, this)

    init {
        define(LENGTH_PROPERTY)
    }
}
