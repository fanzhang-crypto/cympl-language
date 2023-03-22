package demo.parser.interpret

import demo.parser.domain.BuiltinType
import demo.parser.domain.BuiltinType.*
import demo.parser.domain.TypeChecker

internal object RuntimeTypeChecker {

    fun assertValueType(tvalue: TValue, expectedType: BuiltinType) {
        if (tvalue == TValue.TEmptyArray && expectedType is ARRAY) {
            // empty array is assignable to any array type
            return
        }

        if (!TypeChecker.typeMatch(tvalue.type, expectedType)) {
            throw InterpretException("type mismatch: expected $expectedType, got ${tvalue.type}")
        }
    }
}
