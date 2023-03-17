package demo.parser.interpret

import demo.parser.domain.ParseResult
import demo.parser.domain.Parser
import demo.parser.domain.Program
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.fail

abstract class InterpretVerifier {

    abstract val parser: () -> Parser<Program>

    fun verify(input: String, expected: String) = when (val r = parser().parse(input.byteInputStream())) {
        is ParseResult.Failure -> {
            r.errors.forEach { println(it) }
            fail(r.errors.first())
        }
        is ParseResult.Success -> {
            val program = r.value
            val outputs = Interpreter().interpret(program)
            outputs.joinToString("\n") shouldBe expected.trimIndent()
        }
    }
}
