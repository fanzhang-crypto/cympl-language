package cympl.language

sealed interface Statement {

    data class VariableDeclaration(val id: String, val type: BuiltinType, val expr: Expression? = null) : Statement {
        override fun toString() = if (expr != null) "${type.name} $id = $expr;" else "${type.name} $id"
    }

    data class Assignment(val leftExpr: Expression, val rightExpr: Expression) : Statement {
        override fun toString() = "$leftExpr = $rightExpr;"
    }

    data class ExpressionStatement(val expr: Expression) : Statement {
        override fun toString() = "$expr;"
    }

    class Block(val statements: List<Statement>) : Statement {
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

    data class Switch(val expr: Expression, val cases: List<Case>, val defaultCase: Statement?) : Statement {
        override fun toString(): String {
            val casesString = cases.joinToString("\n")
            val defaultCaseString = if (defaultCase != null) "default:$defaultCase\n" else ""
            return "switch ($expr) {\n$casesString\n$defaultCaseString}"
        }
    }

    data class Case(val condition: Expression, val action: Statement?, val hasBreak: Boolean) : Statement {
        override fun toString(): String {
            return "case $condition:${action ?: ""}${if (hasBreak) "break;" else ""}"
        }
    }

    data class Return(val expr: Expression?) : Statement {
        override fun toString() = "return $expr;"
    }

    data class Break(val label: String? = null) : Statement {
        override fun toString() = if (label != null) "break $label;" else "break;"
    }

    data class Continue(val label: String? = null) : Statement {
        override fun toString() = if (label != null) "continue $label;" else "continue;"
    }

    open class FunctionDeclaration(
        val id: String,
        final override val resolvedType: BuiltinType.FUNCTION,
        val parameters: List<VariableDeclaration>,
        val body: Block
    ) : Statement, Typed {

        val returnType = resolvedType.returnType

        override fun toString() : String {
            val paramTypesString = if (!resolvedType.supportVarargs)
                parameters.joinToString(", ")
            else
                parameters.withIndex().joinToString(", ") { (i, param) ->
                    if (i == parameters.size - 1) {
                        val varargType = (param.type as BuiltinType.ARRAY).elementType
                        "$varargType... ${param.id}"
                    } else
                        param.toString()
                }

            return "func $id($paramTypesString):$returnType $body"
        }
    }
}


