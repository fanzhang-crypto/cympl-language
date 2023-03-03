package demo.parser.antlr

import demo.parser.domain.Interpreter
import demo.parser.domain.ParseResult
import demo.parser.domain.Program
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class IntegrationTests {

    private val parser = AntlrProgramParser()
    private val interpreter = Interpreter()

    @Test
    fun testCase0() {
        val input = """
            i:INT = 1  // some comment 1
            //some comment 2
            j:INT = 2
            k:INT = 3
            k = i - j
            (i + j) * k
            i + j * 2 - k/3
            (1 - (i + j)) / 2
        """.byteInputStream()

        val program = parser.parse(input).shouldBeInstanceOf<ParseResult.Success<Program>>().value
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

        val errors = parser.parse(input).shouldBeInstanceOf<ParseResult.Failure<*>>().errors
        errors shouldHaveSize 1
        errors[0].shouldHaveMessage("syntax error at (4:22): extraneous input ':' expecting {<EOF>, '(', ID, NUM}")
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

        val errors = parser.parse(input).shouldBeInstanceOf<ParseResult.Failure<*>>().errors
        errors shouldHaveSize 3
        errors[0].shouldHaveMessage("semantic error at (3:12): variable i already declared")
        errors[1].shouldHaveMessage("semantic error at (5:17): variable k not defined")
        errors[2].shouldHaveMessage("semantic error at (6:12): variable i already declared")
    }
}
