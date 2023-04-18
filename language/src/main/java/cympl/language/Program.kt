package cympl.language

data class Program(val statements: List<Statement>) {
    override fun toString() = statements.joinToString("\n")
}
