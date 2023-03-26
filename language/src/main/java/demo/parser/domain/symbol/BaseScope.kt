package demo.parser.domain.symbol

abstract class BaseScope(override val enclosingScope: Scope?) : Scope {

    private val symbols: MutableMap<String, Symbol> = LinkedHashMap()

    override fun define(symbol: Symbol) {
        symbols[symbol.name] = symbol
        symbol.scope = this // track the scope in each symbol
    }

    override fun resolve(name: String): Symbol? =
        symbols[name] ?: enclosingScope?.resolve(name)

    override fun remove(text: String) {
        symbols.remove(text)
    }
}
