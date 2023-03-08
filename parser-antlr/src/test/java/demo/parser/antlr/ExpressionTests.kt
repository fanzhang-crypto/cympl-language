package demo.parser.antlr

import demo.parser.domain.Expression
import demo.parser.domain.Interpreter
import demo.parser.domain.ParseResult
import demo.parser.domain.Program
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.throwable.shouldHaveMessage
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class ExpressionTests: InterpreterTest() {

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
            -i;
        """
        val output = """
            i:INT = 1; => 1
            j:INT = 2; => 2
            k:INT = 3; => 3
            k = i - j; => -1
            (i + j) * k; => -3
            i + j * 2 - k / 3; => 5
            (1 - (i + j)) / 2; => -1
            -i; => -1
            environment:
            i:INT = 1, j:INT = 2, k:INT = -1
        """
        verify(input, output)
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
        """
        val output = """
            i:INT = 1; => 1
            j:FLOAT = 2.0; => 2.0
            k:FLOAT = 3.0; => 3.0
            k = i - j; => -1.0
            (i + j) * k; => -3.0
            i + j * 2 - k / 3; => 5.333333333333333
            (1 - (i + j)) / 2; => -1.0
            environment:
            i:INT = 1, j:FLOAT = 2.0, k:FLOAT = -1.0
        """
        verify(input, output)
    }

    @Test
    fun `string test`() {
        val input = """
            s1:STRING = "a" + "b" + "c";
            s2:STRING = "d" + 1 + 2 + 3;
            s3:STRING = s1 + s2;
        """
        val output = """
            s1:STRING = "a" + "b" + "c"; => "abc"
            s2:STRING = "d" + 1 + 2 + 3; => "d123"
            s3:STRING = s1 + s2; => "abcd123"
            environment:
            s1:STRING = "abc", s2:STRING = "d123", s3:STRING = "abcd123"
        """
        verify(input, output)
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
        errors[0].message shouldContain "syntax error at (4:23): extraneous input ':'"
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

    @Test
    fun `support eq, neq, gt, get, lt, lte`() {
        val input = """
            i: INT = 5;
            j: INT = 7;
            i == j;
            i != j;
            i > j;
            i >= j;
            i < j;
            i <= j;
        """
        val output = """
            i:INT = 5; => 5
            j:INT = 7; => 7
            i == j; => false
            i != j; => true
            i > j; => false
            i >= j; => false
            i < j; => true
            i <= j; => true
            environment:
            i:INT = 5, j:INT = 7
        """
        verify(input, output)
    }

    @Test
    fun `support and or not operation`() {
        val input = """
            true && false;
            true || false;
            !true;
        """
        val output = """
            true && false; => false
            true || false; => true
            !true; => false
            environment:
        """
        verify(input, output)
    }

    @Test
    fun `logical and has a higher priority than or`() {
        val input = """
            true && false || true && true;
        """.byteInputStream()

        when(val r = parser.parse(input)) {
            is ParseResult.Failure -> {
                r.errors.forEach { println(it) }
                fail(r.errors.first())
            }
            is ParseResult.Success -> {
                val program = r.value
                program.statements shouldHaveSize 1
                program.statements.first() shouldBe Expression.Or(
                    Expression.And(
                        Expression.Bool(true),
                        Expression.Bool(false)
                    ),
                    Expression.And(
                        Expression.Bool(true),
                        Expression.Bool(true)
                    ),
                ).toStatement()
            }
        }
    }
}
