package demo.parser.app

import demo.parser.domain.*

fun main() {
    val parser = ParserFactory.getParser()
    val interpreter = Interpreter()

    System.`in`.reader().useLines { lines ->
        lines.forEach { line ->
            if (line == "quit") {
                return@useLines
            }

            when (val r = parser.parse(line.byteInputStream())) {
                is ParseResult.Success -> {
                    try {
                        interpreter.interpret(r.value)
                            .forEach(::println)
                    } catch (e:RuntimeException) {
                        System.err.println(e.message)
                    }
                }
                is ParseResult.Failure -> {
                    r.errors.map { it.message }.forEach(System.err::println)
                }
            }
        }
    }
}
