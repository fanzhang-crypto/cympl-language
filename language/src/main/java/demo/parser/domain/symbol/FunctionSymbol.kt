package demo.parser.domain.symbol

import demo.parser.domain.BuiltinType

class FunctionSymbol(
    name: String,
    override val returnType: BuiltinType,
    override val parameters: List<VariableSymbol>,
    override val enclosingScope: Scope?
) : Symbol(name, BuiltinType.FUNCTION(parameters.map { it.type }, returnType)), FunctionScope {

    private val locals: MutableMap<String, Symbol> = LinkedHashMap<String, Symbol>().also {
        parameters.forEach { arg -> it[arg.name] = arg }
    }

    override fun define(symbol: Symbol) {
        locals[symbol.name] = symbol
        symbol.scope = this
    }

    override fun resolve(name: String): Symbol? =
        locals[name] ?: enclosingScope?.resolve(name)

    override fun remove(text: String) {
        throw UnsupportedOperationException("cannot remove argument from function")
    }

    override val scopeName: String = name

    override fun toString(): String = "function${super.toString()}:${locals.values}"
}
