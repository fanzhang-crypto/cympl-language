package demo.parser.domain

sealed interface Statement {

    data class VariableDeclaration(val id: String, val type: VariableType, val expr: Expression) : Statement {
        override fun toString() = "$id:${type.name} = $expr"
    }

    data class Assignment(val id: String, val expr: Expression) : Statement {
        override fun toString() = "$id = $expr"
    }
}


