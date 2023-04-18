package cympl.language.symbol

object IntrinsicSymbols {
    val printLine = FunctionSymbol(
        "println",
        cympl.language.BuiltinType.VOID,
        listOf(VariableSymbol("str", cympl.language.BuiltinType.ANY, null)),
        null
    )

    val readLine = FunctionSymbol(
        "readln",
        cympl.language.BuiltinType.STRING,
        listOf(VariableSymbol("prompt", cympl.language.BuiltinType.STRING, null)),
        null
    )
}
