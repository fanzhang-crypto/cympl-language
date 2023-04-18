package cympl.interpreter

interface Runtime {
    fun printLine(value: Any)
    fun readLine(prompt: String): String
}
