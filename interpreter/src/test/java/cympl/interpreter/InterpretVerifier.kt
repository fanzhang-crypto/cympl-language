package cympl.interpreter

import cympl.parser.ParseResult
import cympl.parser.Parser
import cympl.language.Program
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.fail

abstract class InterpretVerifier {

    abstract val parser: () -> Parser<Program>

    private object TestRuntime : Runtime {
        override fun printLine(value: Any) {
            println(value)
        }

        override fun readLine(prompt: String): String {
            return readln()
        }
    }

    fun verify(input: String, expected: String) = when (val r = parser().parse(input.byteInputStream())) {
        is ParseResult.Failure -> {
            r.errors.forEach { println(it) }
            fail(r.errors.first())
        }
        is ParseResult.Success -> {
            val program = r.value
            val outputs = Interpreter(TestRuntime).interpret(program)
            outputs.joinToString("\n") shouldBe expected.trimIndent()
        }
    }
}
