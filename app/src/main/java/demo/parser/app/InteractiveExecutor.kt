package demo.parser.app

import demo.parser.domain.*
import demo.parser.interpret.Interpreter
import kotlin.system.exitProcess

fun main() {
    val parser = ParserFactory.getParser()
    val interpreter = Interpreter()

    System.`in`.reader().useLines { lines ->
        lines.forEach { line ->
            if (line.isBlank()) {
                return@forEach
            }
            if (line == "quit") {
                return@useLines
            }
            if (line == "help") {
                println("quit: quit the program")
                println("help: show this help")
                return@forEach
            }
            when (val r = parser.parse(line.byteInputStream())) {
                is ParseResult.Success -> {
                    try {
                        interpreter.interpret(r.value)
                            .forEach(::println)
                    } catch (e:RuntimeException) {
                        System.err.println(e.message)
                        exitProcess(1)
                    }
                }
                is ParseResult.Failure -> {
                    r.errors.map { it.message }.forEach(System.err::println)
                }
            }
        }
    }
}
