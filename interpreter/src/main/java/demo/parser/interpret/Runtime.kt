package demo.parser.interpret

interface Runtime {
    fun printLine(value: Any)
    fun readLine(prompt: String): String
}
