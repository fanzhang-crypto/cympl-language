package cympl.language.symbol

class GlobalScope : BaseScope(null) {
    override val scopeName: String = "globals"

    init {
        define(IntrinsicSymbols.printLine)
        define(IntrinsicSymbols.readLine)
    }
}
