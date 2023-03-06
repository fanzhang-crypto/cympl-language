package demo.parser.antlr

import demo.parser.domain.Interpreter
import demo.parser.domain.ParseResult
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class BlockTests {

    private val parser = AntlrProgramParser()
    private val interpreter = Interpreter()

    @Test
    fun `variables in different blocks have no conflict`() {
        val input = """
            {
                x:INT = 1;
                {
                    x:INT = 2;
                    x;
                }
                x;
            }
        """.byteInputStream()

        when (val r = parser.parse(input)) {
            is ParseResult.Failure -> {
                r.errors.forEach { println(it) }
                fail(r.errors.first())
            }
            is ParseResult.Success -> {
                val program = r.value
                val outputs = interpreter.interpret(program)
                outputs.joinToString("\n") shouldBe """
                    { x:INT = 1; { x:INT = 2; x; } x; } => 1
                    environment:
                """.trimIndent()
            }
        }
    }
}
