package cympl.language.symbol

sealed interface Scope {

    val scopeName: String

    /** Where to look next for symbols  */
    val enclosingScope: Scope?

    val symbols: List<Symbol>

    /** Define a symbol in the current scope  */
    fun define(symbol: Symbol)

    /** Look up name in this scope or in enclosing scope if not here  */
    fun resolve(name: String): Symbol?

    fun remove(text: String)
}
