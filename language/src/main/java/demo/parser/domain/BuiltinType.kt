package demo.parser.domain

sealed class BuiltinType {

    val isPrimitive: Boolean
        get() = this is INT || this is FLOAT || this is STRING || this is BOOL

    val numericCompatible: Boolean
        get() = this is INT || this is FLOAT || this is BOOL

    val name: String
        get() = when (this) {
            is ANY -> "any"
            is INT -> "int"
            is FLOAT -> "float"
            is STRING -> "String"
            is BOOL -> "bool"
            is VOID -> "void"
            is ARRAY -> toString()
            is FUNCTION -> toString()
        }

    object ANY : BuiltinType()
    object VOID : BuiltinType()
    object BOOL : BuiltinType()
    object INT : BuiltinType()
    object FLOAT : BuiltinType()
    object STRING : BuiltinType()

    data class ARRAY(val elementType: BuiltinType) : BuiltinType() {
        override fun toString() = "$elementType[]"
    }

    data class FUNCTION(val paramTypes: List<BuiltinType>, val returnType: BuiltinType) : BuiltinType() {
        var isFirstClass: Boolean = false

        override fun toString() = "(${paramTypes.joinToString(", ")}) -> $returnType"
    }

    override fun toString() = name

    companion object {
        fun compatibleTypeOf(typeLeft: BuiltinType, typeRight: BuiltinType): BuiltinType =
            when {
                typeLeft == typeRight -> typeLeft
                typeLeft == FLOAT && typeRight == INT -> FLOAT
                typeLeft == INT && typeRight == FLOAT -> FLOAT
                typeLeft == INT && typeRight == BOOL -> INT
                typeLeft == BOOL && typeRight == INT -> INT
                typeLeft == FLOAT && typeRight == BOOL -> FLOAT
                typeLeft == BOOL && typeRight == FLOAT -> FLOAT

                else -> VOID
            }
    }
}
