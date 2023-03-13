package demo.parser.app.antlr

import demo.parser.app.antlr.AntlrInterpretVerifier.parser
import demo.parser.domain.ParseException
import demo.parser.domain.ParseResult
import demo.parser.interpret.InterpretException
import demo.parser.interpret.Interpreter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.throwable.shouldHaveMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class TypeCheckTests {

    @Test
    fun `should check type for expressions`() {
        val input = """
            1 > "2";
            "2" * 3;
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 2
        errors[0] shouldHaveMessage "semantic error at (1:0): comparison only works between INT and FLOAT, INT and INT, FLOAT and FLOAT, STRING and STRING, BOOL and BOOL, but got INT and STRING"
        errors[1] shouldHaveMessage "semantic error at (2:0): type mismatch: expected INT or FLOAT, but got STRING and INT"
    }

    @Test
    fun `should check type on variable declaration`() {
        val input = """
            x:INT = 1.1;
            y:FLOAT = "1";
            z:STRING = 2;
            b:BOOL = 1;
            xx:INT = 1 / 2 * 3 + y;
            arr:INT[] = [1, 2.2];
            arr2:INT[] = [1.2, 2.2];
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 7
        errors[0] shouldHaveMessage "semantic error at (1:8): type mismatch: expected INT, but got FLOAT"
        errors[1] shouldHaveMessage "semantic error at (2:10): type mismatch: expected FLOAT, but got STRING"
        errors[2] shouldHaveMessage "semantic error at (3:11): type mismatch: expected STRING, but got INT"
        errors[3] shouldHaveMessage "semantic error at (4:9): type mismatch: expected BOOL, but got INT"
        errors[4] shouldHaveMessage "semantic error at (5:9): type mismatch: expected INT, but got FLOAT"
        errors[5] shouldHaveMessage "semantic error at (6:12): array elements must be of the same type"
        errors[6] shouldHaveMessage "semantic error at (7:13): type mismatch: expected INT[], but got FLOAT[]"
    }

    @Test
    fun `should check type on variable assign`() {
        val input = """
            x:INT = 1;
            x = 1.1;
            y:FLOAT = 1.1;
            y = "1";
            z:STRING = "1";
            z = 2;
            b:BOOL = true;
            b = 1;
            arr:INT[] = [1, 2];
            arr = [1, 2.2];
            arr2:INT[] = [1, 2];
            arr2 = [1.2, 2.2];
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 6
        errors[0] shouldHaveMessage "semantic error at (2:4): type mismatch: expected INT, but got FLOAT"
        errors[1] shouldHaveMessage "semantic error at (4:4): type mismatch: expected FLOAT, but got STRING"
        errors[2] shouldHaveMessage "semantic error at (6:4): type mismatch: expected STRING, but got INT"
        errors[3] shouldHaveMessage "semantic error at (8:4): type mismatch: expected BOOL, but got INT"
        errors[4] shouldHaveMessage "semantic error at (10:6): array elements must be of the same type"
        errors[5] shouldHaveMessage "semantic error at (12:7): type mismatch: expected INT[], but got FLOAT[]"
    }

    @Test
    fun `array index should be INT`() {
        val input = """
            arr:INT[] = [1, 2];
            arr[1.1];
            arr[1.1] = 1;
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 2
        errors[0] shouldHaveMessage "semantic error at (2:4): array index must be of type INT, but got FLOAT"
        errors[1] shouldHaveMessage "semantic error at (3:4): array index must be of type INT, but got FLOAT"
    }

    @Test
    fun `only array can be indexed`() {
        val input = """
            x:INT = 1;
            x[1];
            x[1] = 1;
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 2
        errors[0] shouldHaveMessage "semantic error at (2:0): indexing only works on arrays"
        errors[1] shouldHaveMessage "semantic error at (3:0): indexing only works on arrays"
    }

    @Test
    fun `should check value type for array index assignment`() {
        val input = """
            arr:INT[] = [1, 2];
            arr[1] = 1.1;
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 1
        errors[0] shouldHaveMessage "semantic error at (2:9): type mismatch: expected INT, but got FLOAT"
    }

    @Test
    fun `should check parameter type on function call`() {
        val input = """
            func foo(x:INT, y:INT):INT {
                return x + y;
            }
            foo(1.1, "1");
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 1
        errors[0] shouldHaveMessage "semantic error at (4:0): argument types mismatch: expected [INT, INT], but got [FLOAT, STRING]"
    }

    @Test
    fun `should check return type of function call in variable declaration`(){
        val input = """
            func f(arr:INT[]):INT[] {
                return arr;
            }
            arr:STRING[] = f([1, 2, 3]);
        """

        val errors = check(input)
        errors shouldHaveSize 1
        errors[0] shouldHaveMessage "semantic error at (5:27): type mismatch: expected STRING[], but got INT[]"
    }

    @Test
    fun `should check return expression type against the function's declaration type`() {
        val input = """
            func f(x:INT) {
                if (true) {
                    return 1;
                } else {
                    return;
                }
                return x + 1.0;
            }
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 2
        errors[0] shouldHaveMessage "semantic error at (3:15): return expression type mismatch: expected VOID, but got INT"
        errors[1] shouldHaveMessage "semantic error at (7:11): return expression type mismatch: expected VOID, but got FLOAT"
    }

    private fun check(input: String): List<ParseException> =
        when (val r = parser().parse(input.byteInputStream())) {
            is ParseResult.Failure ->
                r.errors

            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
}
