package demo.parser.antlr

import demo.parser.domain.Interpreter
import demo.parser.domain.ParseResult
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.fail

abstract class InterpreterTest {

    protected val parser = AntlrProgramParser()
    private val interpreter = Interpreter()

    protected fun verify(input: String, expected: String) = when (val r = parser.parse(input.byteInputStream())) {
        is ParseResult.Failure -> {
            r.errors.forEach { println(it) }
            fail(r.errors.first())
        }
        is ParseResult.Success -> {
            val program = r.value
            val outputs = interpreter.interpret(program)
            outputs.joinToString("\n") shouldBe expected.trimIndent()
        }
    }
}
