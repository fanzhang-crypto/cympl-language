package demo.parser.domain

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

    object VOID : BuiltinType()
    object BOOL : BuiltinType()
    object INT : BuiltinType()
    object FLOAT : BuiltinType()
    object STRING : BuiltinType()

    data class ARRAY(val elementType: BuiltinType) : BuiltinType() {
        override fun toString() = "$elementType[]"
    }

    override fun toString() = name
}

val EmptyArray = BuiltinType.ARRAY(BuiltinType.VOID)
