package demo.parser.domain

import demo.parser.domain.symbol.IntrinsicSymbols

private val native = Statement.Block(emptyList())

sealed class Intrinsic(
    id: String,
    returnType: BuiltinType,
    args: List<Statement.VariableDeclaration>
) : Statement.FunctionDeclaration(id, returnType, args, native) {

    object PrintLine : Intrinsic(
        IntrinsicSymbols.printLine.name,
        BuiltinType.VOID,
        listOf(Statement.VariableDeclaration("str", BuiltinType.STRING))
    )

    object ReadLine : Intrinsic(
        IntrinsicSymbols.readLine.name,
        BuiltinType.STRING,
        emptyList()
    )

    override fun toString(): String {
        return "func $id(${args.joinToString(", ")}): $returnType"
    }
}
