package demo.parser.domain

sealed class Type {

    val name: String
        get() = when (this) {
            is INT -> "INT"
            is FLOAT -> "FLOAT"
            is STRING -> "STRING"
            is BOOL -> "BOOL"
            is VOID -> "VOID"
            is ARRAY -> toString()
        }

    object VOID : Type()
    object BOOL : Type()
    object INT : Type()
    object FLOAT : Type()
    object STRING : Type()

    data class ARRAY(val elementType: Type) : Type() {
        override fun toString() = "$elementType[]"
    }

    override fun toString() = name
}
