package demo.parser.interpret.antlr

import demo.parser.domain.Expression
import demo.parser.domain.ParseResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.throwable.shouldHaveMessage
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import demo.parser.interpret.antlr.AntlrInterpretVerifier.parser
import demo.parser.interpret.antlr.AntlrInterpretVerifier.verify

class ExpressionTests {

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
    fun `should report syntax error`() {
        val input = """
            i: INT = 5;
            i: INT = 7;
            j = i + 23;:INT
            24 * k;
            i: INT = 9;
        """.byteInputStream()

        val errors = parser().parse(input).shouldBeInstanceOf<ParseResult.Failure<*>>().errors
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
            j = i + 23;
        """.byteInputStream()

        val errors = parser().parse(input).shouldBeInstanceOf<ParseResult.Failure<*>>().errors
        errors shouldHaveSize 4
        errors[0].shouldHaveMessage("semantic error at (3:12): variable i already defined")
        errors[1].shouldHaveMessage("semantic error at (5:17): variable k not defined")
        errors[2].shouldHaveMessage("semantic error at (6:12): variable i already defined")
        errors[3].shouldHaveMessage("semantic error at (7:12): variable j not defined")
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

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors.forEach { println(it) }
                fail(r.errors.first())
            }

            is ParseResult.Success -> {
                val program = r.value
                program.statements shouldHaveSize 1
                program.statements.first() shouldBe Expression.Or(
                    Expression.And(
                        Expression.BoolLiteral(true),
                        Expression.BoolLiteral(false)
                    ),
                    Expression.And(
                        Expression.BoolLiteral(true),
                        Expression.BoolLiteral(true)
                    ),
                ).toStatement()
            }
        }
    }

    @Test
    fun `logical and should be short circuited`() {
        val input = """
            false && 1/0 == 0;
        """

        val output = """
            false && 1 / 0 == 0; => false
            environment:
        """
        verify(input, output)
    }

    @Test
    fun `logical or should be short circuited`() {
        val input = """
            true || 1/0 == 0;
        """

        val output = """
            true || 1 / 0 == 0; => true
            environment:
        """
        verify(input, output)
    }

    @Test
    fun `support ++ and -- on variable`() {
        val input = """
            i: INT = 5;
            i++;
            i--;
            ++i;
            --i;
        """
        val output = """
            i:INT = 5; => 5
            i++; => 5
            i--; => 6
            ++i; => 6
            --i; => 5
            environment:
            i:INT = 5
        """
        verify(input, output)
    }

    @Test
    fun `support ++ and -- on array elements`() {
        val input = """
            a: INT[] = [1, 2, 3];
            a[0]++;
            a[1]--;
            ++a[2];
            --a[0];
        """
        val output = """
            a:INT[] = [1, 2, 3]; => [1, 2, 3]
            a[0]++; => 1
            a[1]--; => 2
            ++a[2]; => 4
            --a[0]; => 1
            environment:
            a:INT[] = [1, 1, 4]
        """
        verify(input, output)
    }

    @Test
    fun `++ and -- only works on variables and array elements`() {
        val input = """
            5++;
            5--;
            ++5;
            --5;
        """

        val output = """
            5++; failed => cannot increment 5
            5--; failed => cannot increment 5
            ++5; failed => cannot increment 5
            --5; failed => cannot increment 5
            environment:
        """

        verify(input, output)
    }
}
