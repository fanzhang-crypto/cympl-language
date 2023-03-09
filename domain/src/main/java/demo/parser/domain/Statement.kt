package demo.parser.domain

sealed interface Statement {

    data class VariableDeclaration(val id: String, val type: Type, val expr: Expression? = null) : Statement {
        override fun toString() = if (expr != null) "$id:${type.name} = $expr;" else "$id:${type.name}"
    }

    data class Assignment(val id: String, val expr: Expression) : Statement {
        override fun toString() = "$id = $expr;"
    }

    data class ExpressionStatement(val expr: Expression) : Statement {
        override fun toString() = "$expr;"
    }

    data class Block(val statements: List<Statement>) : Statement {
        override fun toString() = "{ ${statements.joinToString(" ")} }"
    }

    data class If(val condition: Expression, val thenBranch: Statement, val elseBranch: Statement?) : Statement {
        override fun toString() =
            if (elseBranch != null)
                "if ($condition) $thenBranch else $elseBranch"
            else
                "if ($condition) $thenBranch"
    }

    data class While(val condition: Expression, val body: Statement) : Statement {
        override fun toString() = "while ($condition) $body"
    }

    data class For(
        val init: Statement?,
        val condition: Expression?,
        val update: Statement?,
        val body: Statement
    ) : Statement {
        override fun toString() = "for (${init ?: ""} ${condition ?: ""}; ${update ?: ""}) $body"
    }

    data class Return(val expr: Expression) : Statement {
        override fun toString() = "return $expr;"
    }

    data class Break(val label: String? = null) : Statement {
        override fun toString() = if (label != null) "break $label;" else "break;"
    }

    data class Continue(val label: String? = null) : Statement {
        override fun toString() = if (label != null) "continue $label;" else "continue;"
    }

    data class FunctionDeclaration(
        val id: String,
        val returnType: Type,
        val args: List<VariableDeclaration>,
        val body: Block
    ) : Statement {
        override fun toString() = "func $id(${args.joinToString(", ")}):$returnType $body"
    }
}


