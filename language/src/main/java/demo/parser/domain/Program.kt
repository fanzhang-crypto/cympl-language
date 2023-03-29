package demo.parser.domain

data class Program(val statements: List<Statement>) {
    override fun toString() = statements.joinToString("\n")

    fun <T : Statement> forEvery(clazz: Class<T>, action: (T) -> Unit) {
        statements.forEach { it.forEvery(clazz, action) }
    }

    private fun <T : Statement> Statement.forEvery(clazz: Class<T>, action: (T) -> Unit) {
        if (this.javaClass == clazz) {
            @Suppress("UNCHECKED_CAST")
            action(this as T)
        }

        when (this) {
            is Statement.Assignment -> {

            }

            is Statement.Block -> {
                statements.forEach { it.forEvery(clazz, action) }
            }

            is Statement.Break -> {}
            is Statement.Continue -> {}
            is Statement.ExpressionStatement -> {}
            is Statement.For -> {
                body.forEvery(clazz, action)
            }

            is Statement.FunctionDeclaration -> {
                body.forEvery(clazz, action)
            }

            is Statement.If -> {
                thenBranch.forEvery(clazz, action)
                elseBranch?.forEvery(clazz, action)
            }

            is Statement.IndexAssignment -> {}
            is Statement.Return -> {

            }

            is Statement.VariableDeclaration -> {}
            is Statement.While -> {
                body.forEvery(clazz, action)
            }

            is Statement.Switch -> {
                cases.forEach { it.forEvery(clazz, action) }
                defaultCase?.forEvery(clazz, action)
            }

            is Statement.Case -> {
                this.action?.forEvery(clazz, action)
            }
        }


    }
}
