package demo.parser.domain

data class Program(val statements: List<Statement>) {
    override fun toString() = statements.joinToString("\n")

    fun <T : Statement> specificProcess(clazz: Class<T>, action: (T) -> Unit) {
        statements.forEach { it.specificProcess(clazz, action) }
    }

    private fun <T : Statement> Statement.specificProcess(clazz: Class<T>, action: (T) -> Unit) {
        if (this.javaClass == clazz) {
            @Suppress("UNCHECKED_CAST")
            action(this as T)
        }

        when (this) {
            is Statement.Assignment -> {

            }

            is Statement.Block -> {
                statements.forEach { it.specificProcess(clazz, action) }
            }

            is Statement.Break -> {}
            is Statement.Continue -> {}
            is Statement.ExpressionStatement -> {}
            is Statement.For -> {
                body.specificProcess(clazz, action)
            }

            is Statement.FunctionDeclaration -> {
                body.specificProcess(clazz, action)
            }

            is Statement.If -> {
                thenBranch.specificProcess(clazz, action)
                elseBranch?.specificProcess(clazz, action)
            }

            is Statement.IndexAssignment -> {}
            is Statement.Return -> {

            }

            is Statement.VariableDeclaration -> {}
            is Statement.While -> {
                body.specificProcess(clazz, action)
            }

            is Statement.Switch -> {
                cases.forEach { it.specificProcess(clazz, action) }
                defaultCase?.specificProcess(clazz, action)
            }

            is Statement.Case -> {
                this.action?.specificProcess(clazz, action)
            }
        }


    }
}
