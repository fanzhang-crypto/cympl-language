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
        errors[0] shouldHaveMessage "semantic error at (1:1): type mismatch: expected bool, int, float or String, but got int and String"
        errors[1] shouldHaveMessage "semantic error at (2:1): type mismatch: expected bool, int or float, but got String and int"
        errors[2] shouldHaveMessage "semantic error at (3:1): increment/decrement only works on int or float, but got String"
        errors[3] shouldHaveMessage "semantic error at (4:3): increment/decrement only works on int or float, but got bool"
        errors[4] shouldHaveMessage "semantic error at (6:1): increment/decrement only works on int or float, but got String"
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
        errors[0] shouldHaveMessage "semantic error at (1:9): type mismatch: expected int, but got float"
        errors[1] shouldHaveMessage "semantic error at (2:11): type mismatch: expected float, but got String"
        errors[2] shouldHaveMessage "semantic error at (3:12): type mismatch: expected String, but got int"
        errors[3] shouldHaveMessage "semantic error at (4:10): type mismatch: expected bool, but got int"
        errors[4] shouldHaveMessage "semantic error at (5:10): type mismatch: expected int, but got float"
        errors[5] shouldHaveMessage "semantic error at (6:13): array elements must be of the same type"
        errors[6] shouldHaveMessage "semantic error at (7:14): type mismatch: expected int[], but got float[]"
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
        errors[0] shouldHaveMessage "semantic error at (2:5): type mismatch: expected int, but got float"
        errors[1] shouldHaveMessage "semantic error at (4:5): type mismatch: expected float, but got String"
        errors[2] shouldHaveMessage "semantic error at (6:5): type mismatch: expected String, but got int"
        errors[3] shouldHaveMessage "semantic error at (8:5): type mismatch: expected bool, but got int"
        errors[4] shouldHaveMessage "semantic error at (10:7): array elements must be of the same type"
        errors[5] shouldHaveMessage "semantic error at (12:8): type mismatch: expected int[], but got float[]"
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
        errors[0] shouldHaveMessage "semantic error at (2:5): array index must be of type int, but got float"
        errors[1] shouldHaveMessage "semantic error at (3:5): array index must be of type int, but got float"
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
        errors[0] shouldHaveMessage "semantic error at (2:1): indexing only works on arrays"
        errors[1] shouldHaveMessage "semantic error at (3:1): indexing only works on arrays"
    }

    @Test
    fun `should check value type for array index assignment`() {
        val input = """
            int[] arr = [1, 2];
            arr[1] = 1.1;
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 1
        errors[0] shouldHaveMessage "semantic error at (2:10): type mismatch: expected int, but got float"
    }

    @Test
    fun `should check simple parameter type on function call`() {
        val input = """
            int foo(int x, int y) {
                return x + y;
            }
            foo(1.1, "1");
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 2
        errors[0] shouldHaveMessage "semantic error at (4:5): argument type mismatch at index 0: expected int, but got float"
        errors[1] shouldHaveMessage "semantic error at (4:10): argument type mismatch at index 1: expected int, but got String"
    }

    @Test
    fun `for function that takes lambda as parameter, the arity of lambda passed on function call should be checked`() {
        val input = """
            int foo(int x, int y, (int, int) -> int op) {
                return op(x, y);
            }
            foo(1, 1, (x) -> x);
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 1
        errors[0] shouldHaveMessage "semantic error at (4:11): lambda expression expected to have 2 parameters, but got 1"
    }

    @Test
    fun `for function that takes lambda as parameter, the return type of lambda passed on function call should be checked`() {
        val input = """
            int foo(int x, int y, (int, int) -> int op) {
                return op(x, y);
            }
            foo(1, 1, (x, y) -> 1.1);
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 1
        errors[0] shouldHaveMessage "semantic error at (4:21): lambda expression expected to return int, but got float"
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
        errors[0] shouldHaveMessage "semantic error at (5:28): type mismatch: expected String[], but got int[]"
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
        errors[0] shouldHaveMessage "semantic error at (3:16): return expression type mismatch: expected void, but got int"
        errors[1] shouldHaveMessage "semantic error at (7:12): return expression type mismatch: expected void, but got float"
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
        errors[0] shouldHaveMessage "semantic error at (1:21): array dimensions must be of type int"
        errors[1] shouldHaveMessage "semantic error at (2:17): type mismatch: expected String[], but got int[]"
        errors[2] shouldHaveMessage "semantic error at (3:11): type mismatch: expected String, but got float"
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
        errors[0] shouldHaveMessage "semantic error at (1:5): if condition must be of type bool, but got int"
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
        errors[0] shouldHaveMessage "semantic error at (1:8): while condition must be of type bool, but got int"
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
        errors[0] shouldHaveMessage "semantic error at (1:17): for condition must be of type bool, but got String"
    }


    @Test
    fun `there must be return statement for function that returns non-void`() {
        val input = """
            int f() {
                int a=1;
            }
        """.trimIndent()

        val errors = check(input)
        errors shouldHaveSize 1
        errors[0] shouldHaveMessage "semantic error at (3:1): missing return statement in function f"
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
