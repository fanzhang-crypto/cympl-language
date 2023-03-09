package demo.parser.domain.symbol

import demo.parser.domain.Type

sealed class Symbol(val name: String, val type: Type, var scope: Scope? = null) {
    override fun toString() = "<$name:$type>"
}

class VariableSymbol(name: String, type: Type, scope: Scope?) : Symbol(name, type, scope)

class FunctionSymbol(name: String, type: Type, override val enclosingScope: Scope?) : Symbol(name, type), Scope {

    private val arguments: MutableMap<String, Symbol> = LinkedHashMap()

    override fun define(symbol: Symbol) {
        arguments[symbol.name] = symbol
        symbol.scope = this
    }

    override fun resolve(name: String): Symbol? =
        arguments[name] ?: enclosingScope?.resolve(name)

    override val scopeName: String = name

    override fun toString(): String = "function${super.toString()}:${arguments.values}"

}
