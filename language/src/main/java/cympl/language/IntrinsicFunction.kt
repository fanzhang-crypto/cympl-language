package cympl.language

import cympl.language.symbol.IntrinsicSymbols

/**
 * Intrinsic functions are functions that are built into the language and
 * need be implemented by the runtime
 */
sealed class IntrinsicFunction(
    id: String,
    type: BuiltinType.FUNCTION,
    parameters: List<Statement.VariableDeclaration>
) : Statement.FunctionDeclaration(id, type, parameters, native) {

    object PrintLine : IntrinsicFunction(
        IntrinsicSymbols.printLine.name,
        BuiltinType.FUNCTION(listOf(BuiltinType.ANY), BuiltinType.VOID),
        listOf(Statement.VariableDeclaration("obj", BuiltinType.ANY))
    )

    object ReadLine : IntrinsicFunction(
        IntrinsicSymbols.readLine.name,
        BuiltinType.FUNCTION(listOf(BuiltinType.STRING), BuiltinType.STRING),
        listOf(Statement.VariableDeclaration("prompt", BuiltinType.STRING))
    )

    override fun toString(): String {
        return "func $id(${parameters.joinToString(", ")}): $returnType"
    }

    companion object {
        /**
         * a place holding block for intrinsic functions
         */
        private val native = Statement.Block(emptyList())
    }
}
