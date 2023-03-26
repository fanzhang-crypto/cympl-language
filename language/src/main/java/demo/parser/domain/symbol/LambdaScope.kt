package demo.parser.domain.symbol

import demo.parser.domain.BuiltinType

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
}
