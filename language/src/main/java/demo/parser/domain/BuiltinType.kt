package demo.parser.domain

import demo.parser.domain.symbol.ArrayScope
import demo.parser.domain.symbol.Scope
import demo.parser.domain.symbol.StringScope

sealed class BuiltinType {

    val isPrimitive: Boolean
        get() = this is INT || this is FLOAT || this is STRING || this is BOOL

    val numericCompatible: Boolean
        get() = this is INT || this is FLOAT || this is BOOL

    val name: String
        get() = when (this) {
            is INT -> "int"
            is FLOAT -> "float"
            is STRING -> "String"
            is BOOL -> "bool"
            is VOID -> "void"
            is ARRAY -> toString()
            is FUNCTION -> "(${paramTypes.joinToString(", ")}) -> $returnType"
        }

    open val scope: Scope? = null

    object VOID : BuiltinType()
    object BOOL : BuiltinType()
    object INT : BuiltinType()
    object FLOAT : BuiltinType()

    object STRING : BuiltinType() {
        override val scope: Scope = StringScope
    }

    data class ARRAY(val elementType: BuiltinType) : BuiltinType() {
        override val scope: Scope = ArrayScope
        override fun toString() = "$elementType[]"
    }

    data class FUNCTION(val returnType: BuiltinType, val paramTypes: List<BuiltinType>) : BuiltinType()

    override fun toString() = name

    companion object {
        fun compatibleTypeOf(typeLeft: BuiltinType, typeRight: BuiltinType): BuiltinType {
            return when {
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
}

val EmptyArray = BuiltinType.ARRAY(BuiltinType.VOID)
