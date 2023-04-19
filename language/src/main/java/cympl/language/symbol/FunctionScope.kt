package cympl.language.symbol

import cympl.language.BuiltinType

interface FunctionScope : Scope {
    val parameters: List<VariableSymbol>
    val returnType: BuiltinType
}
