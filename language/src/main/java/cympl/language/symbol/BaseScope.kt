package cympl.language.symbol

abstract class BaseScope(override val enclosingScope: Scope?) : Scope {

    private val symbolTable: MutableMap<String, Symbol> = LinkedHashMap()

    override fun define(symbol: Symbol) {
        symbolTable[symbol.name] = symbol
        symbol.scope = this // track the scope in each symbol
    }

    override fun resolve(name: String): Symbol? =
        symbolTable[name] ?: enclosingScope?.resolve(name)

    override fun remove(text: String) {
        symbolTable.remove(text)
    }

    override val symbols: List<Symbol>
        get() = symbolTable.values.toList()
}
