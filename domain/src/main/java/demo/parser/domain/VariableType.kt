package demo.parser.domain

enum class VariableType {
    INT, FLOAT, STRING;

    private fun toJavaClass():Class<*> = when (this) {
        INT -> Integer::class.java
        FLOAT -> java.lang.Double::class.java
        STRING -> java.lang.String::class.java
    }

    fun checkValue(value: Any) {
        if (value.javaClass != toJavaClass()) {
            throw SemanticException("type mismatch: expected $this, got ${value.javaClass.kotlin}")
        }
    }
}
