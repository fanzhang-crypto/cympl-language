package demo.parser.domain

import demo.parser.domain.symbol.ArrayScope
import demo.parser.domain.symbol.Scope
import demo.parser.domain.symbol.StringScope

sealed class BuiltinType {

    val name: String
        get() = when (this) {
            is INT -> "INT"
            is FLOAT -> "FLOAT"
            is STRING -> "STRING"
            is BOOL -> "BOOL"
            is VOID -> "VOID"
            is ARRAY -> toString()
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

    override fun toString() = name
}

val EmptyArray = BuiltinType.ARRAY(BuiltinType.VOID)
