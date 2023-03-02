package demo.parser.fp

import demo.parser.domain.Interpreter
import demo.parser.domain.ParserResult
import demo.parser.domain.SemanticException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class IntegrationTests {

    private val parser = FpParser
    private val interpreter = Interpreter()

    @Test
    fun testCase0() {
        val input = """
            i:INT = 1  // some comment 1
            //some comments 2
            j:INT = 2
            k:INT = 3
            k = i - j
            (i + j) * k
            i + j * 2 - k/3
            (1 - (i + j)) / 2
        """.byteInputStream()

        val program = parser.parse(input).shouldBeInstanceOf<ParserResult.Success>().program
        val outputs = interpreter.interpret(program)

        outputs.joinToString("\n") shouldBe """
            i:INT = 1 => 1
            j:INT = 2 => 2
            k:INT = 3 => 3
            k = i - j => -1
            (i + j) * k => -3
            i + j * 2 - k / 3 => 5
            (1 - (i + j)) / 2 => -1
            environment:
            i = 1, j = 2, k = -1
        """.trimIndent()
    }

    @Test
    fun testCase1() {
        val input = """
            i: INT = 5
            i: INT = 7
            j = i + 23:INT
            24 * k
            i: INT = 9
        """.byteInputStream()

        val errors = parser.parse(input).shouldBeInstanceOf<ParserResult.Failure>().errors
        errors shouldHaveSize 1
        errors[0].shouldHaveMessage("syntax error at (4:23): extraneous input ':'")
    }

    @Test
    fun testCase2() {
        val input = """
            i: INT = 5 // some comment here
            i: INT = 7
            i + 23
            24 * k
            i: INT = 9
        """.byteInputStream()

        val program = parser.parse(input)
            .shouldBeInstanceOf<ParserResult.Success>().program

        shouldThrow<SemanticException> { interpreter.interpret(program).forEach(::println) }
            .shouldHaveMessage("semantic error: variable i already declared")
    }
}
