package demo.parser.domain

import java.lang.Double
import java.lang.String

enum class VariableType {
    VOID, BOOL, INT, FLOAT, STRING;

    fun toJavaClass():Class<*> = when (this) {
        INT -> Integer::class.java
        FLOAT -> Double::class.java
        STRING -> String::class.java
        BOOL -> Boolean::class.java
        VOID -> Void::class.java
    }

    fun checkValue(value: Any) {
        if (value.javaClass != toJavaClass()) {
            throw SemanticException("type mismatch: expected $this, got ${value.javaClass.kotlin}")
        }
    }
}
