package cympl.language.symbol

object ArrayScope : BaseScope(null) {
    override val scopeName: String = "array"

    val LENGTH_PROPERTY = VariableSymbol("length", cympl.language.BuiltinType.INT, this)

    init {
        define(LENGTH_PROPERTY)
    }
}
