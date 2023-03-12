package demo.parser.domain

import java.lang.Double
import java.lang.String
import java.lang.Boolean
import java.lang.RuntimeException

enum class Type {
    VOID, BOOL, INT, FLOAT, STRING, ARRAY;

    fun toJavaClass():Class<*> = when (this) {
        INT -> Integer::class.java
        FLOAT -> Double::class.java
        STRING -> String::class.java
        BOOL -> Boolean::class.java
        VOID -> Void::class.java
        ARRAY -> Array::class.java
    }

    fun checkValue(value: Any) {
        if (value.javaClass != toJavaClass()) {
            throw RuntimeException("type mismatch: expected $this, got ${value.javaClass.simpleName}")
        }
    }
}
