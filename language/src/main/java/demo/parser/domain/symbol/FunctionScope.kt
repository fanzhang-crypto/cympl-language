package demo.parser.domain.symbol

import demo.parser.domain.BuiltinType

interface FunctionScope : Scope {
    val parameters: List<VariableSymbol>
    val returnType: BuiltinType
}
