package cympl.interpreter.fp

import cympl.parser.ParseResult
import cympl.language.Statement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.throwable.shouldHaveMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import cympl.interpreter.fp.FpInterpretVerifier.parser
import cympl.interpreter.fp.FpInterpretVerifier.verify
import cympl.language.BuiltinType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Disabled

class FunctionTests {

    @Test
    fun `function can't be called without being defined`() {
        val input = """
            f(1);
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 1
                r.errors[0].shouldHaveMessage("semantic error at (2:12): symbol f not defined")
            }
            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

    @Test
    fun `function call test`() {
        val input = """
            int f(int x) {
                return x + 1;
            }
            int g(int x) {
                return x * 2;
            }
            f(g(2));
        """

        val output = """
            func f(int x):int { return x + 1; } => Closure(#f)
            func g(int x):int { return x * 2; } => Closure(#g)
            f(g(2)); => 5
            environment:
            f: (int) -> int, g: (int) -> int
        """
        verify(input, output)
    }

    @Test
    fun `function can be called with multiple arguments`() {
        val input = """
            int f(int x, int y) {
                return x + y;
            }
            f(1, 2);
        """
        val output = """
            func f(int x, int y):int { return x + y; } => Closure(#f)
            f(1, 2); => 3
            environment:
            f: (int, int) -> int
        """.trimIndent()
        verify(input, output)
    }

    @Test
    fun `variable can be shadowed in function decl`() {
        val input = """
            int x = 1;
            int f(int x) {
                return x + 1;
            }
            int g(int x) {
                return x * 2;
            }
            f(g(2));
        """
        val output = """
            int x = 1; => 1
            func f(int x):int { return x + 1; } => Closure(#f)
            func g(int x):int { return x * 2; } => Closure(#g)
            f(g(2)); => 5
            environment:
            x:int = 1
            f: (int) -> int, g: (int) -> int
        """.trimIndent()
        verify(input, output)
    }

    @Test
    fun `function can call itself`() {
        val input = """
            int f(int x) {
                if (x == 0) {
                    return 1;
                } else {
                    return x * f(x - 1);
                }
            }
            f(5);
        """
        val output = """
            func f(int x):int { if (x == 0) { return 1; } else { return x * f(x - 1); } } => Closure(#f)
            f(5); => 120
            environment:
            f: (int) -> int
        """
        verify(input, output)
    }

    @Disabled("fp parser doesn't support function call before definition")
    @Test
    fun `function can be called before defined`() {
        val input = """
            int g(int x) {
                return x * 2;
            }
            int f(int x) {
                return g(x + 1);
            }
            f(2);
        """
        val output = """
            func f(int x):int { return g(x + 1); } => void
            func g(int x):int { return x * 2; } => void
            f(2); => 6
            environment:
            f: (int) -> int, g: (int) -> int
        """
        verify(input, output)
    }

    @Test
    fun `function can operate string`() {
        val input = """
            String f(String x) {
                return x + " world";
            }
            f("hello");
        """
        val output = """
            func f(String x):String { return x + " world"; } => Closure(#f)
            f("hello"); => "hello world"
            environment:
            f: (String) -> String
        """
        verify(input, output)
    }

    @Test
    fun `can't declare functions with same name`() {
        val input = """
            int f(int x) {
                return x + 1;
            }
            int f(int x) {
                return x + 1;
            }
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 1
                r.errors.first().shouldHaveMessage("semantic error at (5:16): function f already defined")
            }
            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

    @Test
    fun `can't have more than 2 parameter with the same name in function declaration`() {
        val input = """
            int f(int x, int x) {
                return x + 1;
            }
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 1
                r.errors.first().shouldHaveMessage("semantic error at (2:29): variable x already defined")
            }
            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

    @Test
    fun `can't define variables with same name in function`() {
        val input = """
            int f(int x) {
                int x = 1;
                int x = 2;
                return x + 1;
            }
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 1
                r.errors.first().shouldHaveMessage("semantic error at (4:20): variable x already defined")
            }

            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

    @Disabled("type check unavailable in fp parser for now")
    @Test
    fun `can't use function as variable`() {
        val input = """
            int f(int x) {
                return x + 1;
            }
            f = 1;
            f;
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 2
                r.errors[0].shouldHaveMessage("semantic error at (5:12): f is not a variable")
                r.errors[1].shouldHaveMessage("semantic error at (6:12): f is not a variable")
            }

            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

    @Test
    fun `treat empty return type as VOID`() {
        val input = """
            void f(int x) {
                x + 1;
            }
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                fail(r.errors[0].message)
            }

            is ParseResult.Success -> {
                val program = r.value
                program.statements shouldHaveSize 1
                val funcDecl = program.statements[0].shouldBeInstanceOf<Statement.FunctionDeclaration>()
                funcDecl.returnType shouldBe BuiltinType.VOID
            }
        }
    }

    @Test
    fun `check type of function's return value`() {
        val input = """
            int f() {
                1+2;
            }
            int i = f();
        """.trimIndent()

        val output = """
            func f():int { 1 + 2; } => Closure(#f)
            int i = f(); failed => type mismatch: expected int, got void
            environment:
            f: () -> int
        """.trimIndent()

        verify(input, output)
    }

    @Disabled("fp parser doesn't support array type yet")
    @Test
    fun `insert sort test`() {
        val input = """
            int[] insertionSort(int[] arr) {
                for(int i = 1; i < arr.length; i++) {
                    int key = arr[i];
                    int j = i - 1;

                    while (j >= 0 && arr[j] > key) {
                        arr[j+1] = arr[j];
                        j = j - 1;
                    }
                    arr[j+1] = key;
                }
                return arr;
            }

            insertionSort([2,3,9,1,11,32,17,23,15,21]);
        """.trimIndent()

        val output = """
            func insertionSort(int[] arr):int[] { for (int i = 1; i < arr.length; i++;) { int key = arr[i]; int j = i - 1; while (j >= 0 && arr[j] > key) { arr[j + 1] = arr[j] j = j - 1; } arr[j + 1] = key } return arr; } => void
            insertionSort([2, 3, 9, 1, 11, 32, 17, 23, 15, 21]); => [1, 2, 3, 9, 11, 15, 17, 21, 23, 32]
            environment:
            insertionSort: (int[]) -> int[]
        """.trimIndent()

        verify(input, output)
    }
}
