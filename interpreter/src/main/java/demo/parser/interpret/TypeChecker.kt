package demo.parser.interpret

import java.lang.Boolean
import java.lang.Integer
import java.lang.Double
import demo.parser.domain.BuiltinType
import demo.parser.domain.BuiltinType.*

internal object TypeChecker {

    fun assertValueType(tvalue: TValue, expectedType: BuiltinType) {
        if (tvalue == TValue.TEmptyArray && expectedType is ARRAY) {
            // empty array is assignable to any array type
            return
        }
        if (tvalue.type != expectedType) {
            throw InterpretException("type mismatch: expected $expectedType, got ${tvalue.type}")
        }

        val value = tvalue.value
        when (expectedType) {
            is ARRAY -> {
                if (value !is Array<*>) {
                    throw InterpretException("type mismatch: expected $expectedType, got ${value.javaClass.simpleName}")
                }
                for (element in value) {
                    assertValueType(element as TValue, expectedType.elementType)
                }
            }

            else -> checkType(value, expectedType)
        }
    }

    private fun checkType(value: Any, expectedType: BuiltinType) {
        if (value.javaClass != expectedType.toJavaClass()) {
            throw InterpretException("type mismatch: expected $expectedType, got ${value.javaClass.simpleName}")
        }
    }

    private fun BuiltinType.toJavaClass(): Class<*> = when (this) {
        is INT -> Integer::class.java
        is FLOAT -> Double::class.java
        is STRING -> String::class.java
        is BOOL -> Boolean::class.java
        is VOID -> Void::class.java
        is ARRAY -> Array::class.java
    }
}
