package demo.parser.domain.symbol

import demo.parser.domain.BuiltinType

object StringScope : BaseScope(null) {
    override val scopeName: String = "string"

    val LENGTH_PROPERTY = VariableSymbol("length", BuiltinType.INT, this)

    init {
        define(LENGTH_PROPERTY)
    }
}
