package cympl.language.symbol

object StringScope : BaseScope(null) {
    override val scopeName: String = "string"

    val LENGTH_PROPERTY = VariableSymbol("length", cympl.language.BuiltinType.INT, this)

    init {
        define(LENGTH_PROPERTY)
    }
}
