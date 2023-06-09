package cympl.interpreter

import cympl.language.*

internal object RuntimeTypeChecker {

    fun assertValueType(tvalue: TValue, expectedType: BuiltinType) {
        if (tvalue == TValue.TEmptyArray && expectedType is BuiltinType.ARRAY) {
            // empty array is assignable to any array type
            return
        }

        if (!TypeChecker.typeMatch(tvalue.type, expectedType)) {
            throw InterpretException("type mismatch: expected $expectedType, got ${tvalue.type}")
        }
    }

    fun checkArrayDimension(dimension: TValue) : Int {
        if (dimension.type != BuiltinType.INT) {
            throw InterpretException("array dimension must be an integer: $dimension")
        }
        if (dimension.value as Int <= 0) {
            throw InterpretException("array dimension must be positive: $dimension")
        }
        return dimension.value
    }
}
