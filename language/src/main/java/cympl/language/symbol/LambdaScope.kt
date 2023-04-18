package cympl.language.symbol

import cympl.language.BuiltinType

class LambdaScope(
    override var returnType: BuiltinType,
    override val parameters: List<VariableSymbol>,
    enclosingScope: Scope?
) : BaseScope(enclosingScope), FunctionScope {

    override val scopeName: String = "lambda"

    init {
        parameters.forEach(this::define)
    }

    override fun remove(text: String) {
        throw UnsupportedOperationException("cannot remove argument or local from function")
    }

    override fun toString(): String = scopeName

    val captures get(): List<VariableSymbol> {
        val captured = mutableListOf<VariableSymbol>()
        var scope: Scope? = enclosingScope
        while (scope != null) {
            when (scope) {
                is FunctionScope -> captured.addAll(scope.symbols.filterIsInstance<VariableSymbol>())
                is BaseScope -> captured.addAll(scope.symbols.filterIsInstance<VariableSymbol>().filter { it.type !is BuiltinType.FUNCTION })
            }
            scope = scope.enclosingScope
        }
        return captured
    }
}
