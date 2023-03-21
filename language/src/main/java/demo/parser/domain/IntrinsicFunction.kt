package demo.parser.domain

import demo.parser.domain.symbol.IntrinsicSymbols

/**
 * Intrinsic functions are functions that are built into the language and
 * need be implemented by the runtime
 */
sealed class IntrinsicFunction(
    id: String,
    returnType: BuiltinType,
    parameters: List<Statement.VariableDeclaration>
) : Statement.FunctionDeclaration(id, returnType, parameters, native) {

    object PrintLine : IntrinsicFunction(
        IntrinsicSymbols.printLine.name,
        BuiltinType.VOID,
        listOf(Statement.VariableDeclaration("obj", BuiltinType.ANY))
    )

    object ReadLine : IntrinsicFunction(
        IntrinsicSymbols.readLine.name,
        BuiltinType.STRING,
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
