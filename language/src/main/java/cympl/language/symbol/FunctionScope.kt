package cympl.language.symbol

interface FunctionScope : Scope {
    val parameters: List<VariableSymbol>
    val returnType: cympl.language.BuiltinType
}
