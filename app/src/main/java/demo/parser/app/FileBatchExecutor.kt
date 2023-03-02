package demo.parser.app

import demo.parser.domain.*
import java.io.FileInputStream

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Usage: file name")
        return
    }

    val fileName = args[0]
    val input = FileInputStream(fileName)

    val parser = ParserFactory.getParser()
    val interpreter = Interpreter()

    when (val r = parser.parse(input)) {
        is ParserResult.Success -> interpreter.interpret(r.program).forEach(::println)
        is ParserResult.Failure -> r.errors.map { it.message }.forEach( System.err::println)
    }
}
