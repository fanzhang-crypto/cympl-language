package demo.parser.domain

data class Program(val statements: List<Statement>) {
    override fun toString() = statements.joinToString("\n")

    fun <T : Statement> specificProcess(clazz: Class<T>, action: (T) -> Unit) {
        statements.forEach {
            if (it.javaClass == clazz) {
                @Suppress("UNCHECKED_CAST")
                action(it as T)
            }
        }
    }
}
