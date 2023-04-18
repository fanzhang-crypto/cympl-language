package cympl.language.symbol

class FunctionSymbol(
    name: String,
    override val returnType: cympl.language.BuiltinType,
    override val parameters: List<VariableSymbol>,
    override val enclosingScope: Scope?,
) : Symbol(name, cympl.language.BuiltinType.FUNCTION(parameters.map { it.type }, returnType), enclosingScope), FunctionScope {

    private val locals: MutableMap<String, Symbol> = LinkedHashMap()

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

    override val symbols: List<Symbol>
        get() = locals.values.toList()
}
