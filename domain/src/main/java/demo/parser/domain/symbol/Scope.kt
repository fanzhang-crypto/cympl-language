package demo.parser.domain.symbol

sealed interface Scope {

    val scopeName: String

    /** Where to look next for symbols  */
    val enclosingScope: Scope?

    /** Define a symbol in the current scope  */
    fun define(symbol: Symbol)

    /** Look up name in this scope or in enclosing scope if not here  */
    fun resolve(name: String): Symbol?

    fun remove(text: String)
}

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

class GlobalScope : BaseScope(null) {
    override val scopeName: String = "globals"
}

class LocalScope(parent: Scope?) : BaseScope(parent) {
    override val scopeName: String = "locals"
}
