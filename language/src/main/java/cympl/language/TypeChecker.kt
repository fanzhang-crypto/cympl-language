package cympl.language

object TypeChecker {

    fun typeMatch(actualType: BuiltinType, expectType: BuiltinType): Boolean {
        if (actualType == expectType || expectType == BuiltinType.ANY) {
            return true
        }
        if (actualType is BuiltinType.ARRAY && expectType is BuiltinType.ARRAY) {
            return typeMatch(actualType.elementType, expectType.elementType)
        }
        return false
    }
}
