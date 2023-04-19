package cympl.interpreter

import cympl.language.Statement

class Closure(
    val function: Statement.FunctionDeclaration,
    val env: Environment
) : TValue(function.resolvedType, function) {

    override fun toString(): String {
        return "Closure(#${function.id})"
    }
}
