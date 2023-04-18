package cympl.interpreter.antlr

import cympl.interpreter.antlr.AntlrInterpretVerifier.parser
import cympl.parser.ParseException
import cympl.parser.ParseResult
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
            "3"++;
            --true;
            String[] a = ["1", "2"];
            a[0]++;
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 5
        errors[0] shouldHaveMessage "semantic error at (1:0): type mismatch: expected bool, int, float or String, but got int and String"
        errors[1] shouldHaveMessage "semantic error at (2:0): type mismatch: expected bool, int or float, but got String and int"
        errors[2] shouldHaveMessage "semantic error at (3:0): increment/decrement only works on int or float, but got String"
        errors[3] shouldHaveMessage "semantic error at (4:2): increment/decrement only works on int or float, but got bool"
        errors[4] shouldHaveMessage "semantic error at (6:0): increment/decrement only works on int or float, but got String"
    }

    @Test
    fun `should check type on variable declaration`() {
        val input = """
            int x = 1.1;
            float y = "1";
            String z = 2;
            bool b = 1;
            int xx = 1 / 2 * 3 + y;
            int[] arr = [1, 2.2];
            int[] arr2 = [1.2, 2.2];
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 7
        errors[0] shouldHaveMessage "semantic error at (1:8): type mismatch: expected int, but got float"
        errors[1] shouldHaveMessage "semantic error at (2:10): type mismatch: expected float, but got String"
        errors[2] shouldHaveMessage "semantic error at (3:11): type mismatch: expected String, but got int"
        errors[3] shouldHaveMessage "semantic error at (4:9): type mismatch: expected bool, but got int"
        errors[4] shouldHaveMessage "semantic error at (5:9): type mismatch: expected int, but got float"
        errors[5] shouldHaveMessage "semantic error at (6:12): array elements must be of the same type"
        errors[6] shouldHaveMessage "semantic error at (7:13): type mismatch: expected int[], but got float[]"
    }

    @Test
    fun `should check type on variable assign`() {
        val input = """
            int x = 1;
            x = 1.1;
            float y = 1.1;
            y = "1";
            String z = "1";
            z = 2;
            bool b = true;
            b = 1;
            int[] arr = [1, 2];
            arr = [1, 2.2];
            int[] arr2 = [1, 2];
            arr2 = [1.2, 2.2];
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 6
        errors[0] shouldHaveMessage "semantic error at (2:4): type mismatch: expected int, but got float"
        errors[1] shouldHaveMessage "semantic error at (4:4): type mismatch: expected float, but got String"
        errors[2] shouldHaveMessage "semantic error at (6:4): type mismatch: expected String, but got int"
        errors[3] shouldHaveMessage "semantic error at (8:4): type mismatch: expected bool, but got int"
        errors[4] shouldHaveMessage "semantic error at (10:6): array elements must be of the same type"
        errors[5] shouldHaveMessage "semantic error at (12:7): type mismatch: expected int[], but got float[]"
    }

    @Test
    fun `array index should be int`() {
        val input = """
            int[] arr = [1, 2];
            arr[1.1];
            arr[1.1] = 1;
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 2
        errors[0] shouldHaveMessage "semantic error at (2:4): array index must be of type int, but got float"
        errors[1] shouldHaveMessage "semantic error at (3:4): array index must be of type int, but got float"
    }

    @Test
    fun `only array can be indexed`() {
        val input = """
            int x = 1;
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
            int[] arr = [1, 2];
            arr[1] = 1.1;
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 1
        errors[0] shouldHaveMessage "semantic error at (2:9): type mismatch: expected int, but got float"
    }

    @Test
    fun `should check parameter type on function call`() {
        val input = """
            int foo(int x, int y) {
                return x + y;
            }
            foo(1.1, "1");
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 1
        errors[0] shouldHaveMessage "semantic error at (4:0): argument types mismatch: expected [int, int], but got [float, String]"
    }

    @Test
    fun `should check return type of function call in variable declaration`(){
        val input = """
            int[] f(int[] arr) {
                return arr;
            }
            String[] arr = f([1, 2, 3]);
        """

        val errors = check(input)
        errors shouldHaveSize 1
        errors[0] shouldHaveMessage "semantic error at (5:27): type mismatch: expected String[], but got int[]"
    }

    @Test
    fun `should check return expression type against the function's declaration type`() {
        val input = """
            void f(int x) {
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
        errors[0] shouldHaveMessage "semantic error at (3:15): return expression type mismatch: expected void, but got int"
        errors[1] shouldHaveMessage "semantic error at (7:11): return expression type mismatch: expected void, but got float"
    }

    @Test
    fun `should check array size type and element type on new array`() {
        val input = """
            int[] arr = new int[1.1];
            String[] arr2 = new int[1];
            arr2[0] = 1.1;
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 3
        errors[0] shouldHaveMessage "semantic error at (1:20): array dimensions must be of type int"
        errors[1] shouldHaveMessage "semantic error at (2:16): type mismatch: expected String[], but got int[]"
        errors[2] shouldHaveMessage "semantic error at (3:10): type mismatch: expected String, but got float"
    }

    @Test
    fun `should check condition type on if statement`() {
        val input = """
            if (1) {
                println("1");
            }
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 1
        errors[0] shouldHaveMessage "semantic error at (1:4): if condition must be of type bool, but got int"
    }

    @Test
    fun `should check condition type of while statement`() {
        val input = """
            while (1) {
                println("1");
            }
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 1
        errors[0] shouldHaveMessage "semantic error at (1:7): while condition must be of type bool, but got int"
    }

    @Test
    fun `should check condition type of for statement`() {
        val input = """
            for (int i = 0; "s"; i = i + 1) {
                println("1");
            }
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 1
        errors[0] shouldHaveMessage "semantic error at (1:16): for condition must be of type bool, but got String"
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
