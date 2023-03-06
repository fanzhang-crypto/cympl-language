package demo.parser.fp

import demo.parser.domain.Interpreter
import demo.parser.domain.ParseResult
import demo.parser.domain.Program
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class ExpressionTests {

    private val parser = FpProgramParser()
    private val interpreter = Interpreter()

    @Test
    fun `integers test`() {
        val input = """
            i:INT = 1;  // some comment 1
            //some comment 2
            j:INT = 2;
            k:INT = 3;
            k = i - j;
            (i + j) * k;
            i + j * 2 - k/3;
            (1 - (i + j)) / 2;
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
                    i:INT = 1; => 1
                    j:INT = 2; => 2
                    k:INT = 3; => 3
                    k = i - j; => -1
                    (i + j) * k; => -3
                    i + j * 2 - k / 3; => 5
                    (1 - (i + j)) / 2; => -1
                    environment:
                    i:INT = 1, j:INT = 2, k:INT = -1
                """.trimIndent()
            }
        }
    }

    @Test
    fun `floats and integers test`() {
        val input = """
            i:INT = 1;  // some comment 1
            //some comment 2
            j:FLOAT = 2.0;
            k:FLOAT = 3.0;
            k = i - j; // i - j cast to FLOAT because j is FLOAT
            (i + j) * k;
            i + j * 2 - k/3;
            (1 - (i + j)) / 2;
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
                    i:INT = 1; => 1
                    j:FLOAT = 2.0; => 2.0
                    k:FLOAT = 3.0; => 3.0
                    k = i - j; => -1.0
                    (i + j) * k; => -3.0
                    i + j * 2 - k / 3; => 5.333333333333333
                    (1 - (i + j)) / 2; => -1.0
                    environment:
                    i:INT = 1, j:FLOAT = 2.0, k:FLOAT = -1.0
                """.trimIndent()
            }
        }

    }

    @Test
    fun `string test`() {
        val input = """
            s1:STRING = "a" + "b" + "c";
            s2:STRING = "d" + 1 + 2 + 3;
            s3:STRING = s1 + s2;
        """.byteInputStream()

        val program = parser.parse(input).shouldBeInstanceOf<ParseResult.Success<Program>>().value
        val outputs = interpreter.interpret(program)

        outputs.joinToString("\n") shouldBe """
            s1:STRING = "a" + "b" + "c"; => "abc"
            s2:STRING = "d" + 1 + 2 + 3; => "d123"
            s3:STRING = s1 + s2; => "abcd123"
            environment:
            s1:STRING = "abc", s2:STRING = "d123", s3:STRING = "abcd123"
        """.trimIndent()
    }

    @Test
    fun `should report syntax error`() {
        val input = """
            i: INT = 5;
            i: INT = 7;
            j = i + 23;:INT
            24 * k;
            i: INT = 9;
        """.byteInputStream()

        val errors = parser.parse(input).shouldBeInstanceOf<ParseResult.Failure<*>>().errors
        errors shouldHaveSize 1
        errors[0].shouldHaveMessage(
            "syntax error at (4:24): extraneous input ':'"
        )
    }

    @Test
    fun `should report semantic error`() {
        val input = """
            i: INT = 5; // some comment here
            i: INT = 7;
            i + 23;
            24 * k;
            i: INT = 9;
        """.byteInputStream()

        val errors = parser.parse(input).shouldBeInstanceOf<ParseResult.Failure<*>>().errors
        errors shouldHaveSize 3
        errors[0].shouldHaveMessage("semantic error at (3:12): variable i already declared")
        errors[1].shouldHaveMessage("semantic error at (5:17): variable k not defined")
        errors[2].shouldHaveMessage("semantic error at (6:12): variable i already declared")
    }
}
