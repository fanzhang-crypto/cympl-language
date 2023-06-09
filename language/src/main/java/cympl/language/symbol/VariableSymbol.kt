package cympl.language.symbol

import cympl.language.BuiltinType

class VariableSymbol(name: String, type: BuiltinType, scope: Scope?) : Symbol(name, type, scope) {
    init {
        if (type is BuiltinType.VOID)
            throw IllegalArgumentException("Variable cannot be of type void")
        if (type is BuiltinType.FUNCTION) {
            type.isFirstClass = true
        }
    }
}
