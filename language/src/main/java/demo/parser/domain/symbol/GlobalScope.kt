package demo.parser.domain.symbol

class GlobalScope : BaseScope(null) {
    override val scopeName: String = "globals"

    init {
        define(IntrinsicSymbols.printLine)
        define(IntrinsicSymbols.readLine)
    }
}
