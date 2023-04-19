package cympl.language.symbol

import cympl.language.BuiltinType

object IntrinsicSymbols {
    val printLine = FunctionSymbol(
        "println",
        BuiltinType.VOID,
        listOf(VariableSymbol("str", BuiltinType.ANY, null)),
        null
    )

    val readLine = FunctionSymbol(
        "readln",
        BuiltinType.STRING,
        listOf(VariableSymbol("prompt", BuiltinType.STRING, null)),
        null
    )
}
