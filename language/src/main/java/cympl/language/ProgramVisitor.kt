package cympl.language

object ProgramVisitor {

    fun <T : Statement> Program.forStatementType(clazz: Class<T>, action: (T) -> Unit) {
        statements.forEach { it.forType(clazz, action) }
    }

    private fun <T : Statement> Statement.forType(clazz: Class<T>, action: (T) -> Unit) {
        if (this.javaClass == clazz) {
            @Suppress("UNCHECKED_CAST")
            action(this as T)
        }

        when (this) {
            is Statement.Assignment -> {
            }

            is Statement.Block -> {
                statements.forEach { it.forType(clazz, action) }
            }

            is Statement.Break -> {}
            is Statement.Continue -> {}
            is Statement.ExpressionStatement -> {}
            is Statement.For -> {
                body.forType(clazz, action)
            }

            is Statement.FunctionDeclaration -> {
                body.forType(clazz, action)
            }

            is Statement.If -> {
                thenBranch.forType(clazz, action)
                elseBranch?.forType(clazz, action)
            }

            is Statement.IndexAssignment -> {}
            is Statement.Return -> {

            }

            is Statement.VariableDeclaration -> {}
            is Statement.While -> {
                body.forType(clazz, action)
            }

            is Statement.Switch -> {
                cases.forEach { it.forType(clazz, action) }
                defaultCase?.forType(clazz, action)
            }

            is Statement.Case -> {
                this.action?.forType(clazz, action)
            }
        }
    }
}
