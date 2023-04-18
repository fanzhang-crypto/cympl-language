package cympl.language

object TypeChecker {

    fun typesMatch(actualTypes: List<BuiltinType>, expectTypes: List<BuiltinType>) : Boolean {
        if (actualTypes.size != expectTypes.size) {
            return false
        }
        for (i in actualTypes.indices) {
            if (!typeMatch(actualTypes[i], expectTypes[i])) {
                return false
            }
        }
        return true
    }

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
