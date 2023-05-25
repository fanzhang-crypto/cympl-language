package cympl.interpreter.antlr

import cympl.parser.ParseResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.throwable.shouldHaveMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import cympl.interpreter.antlr.AntlrInterpretVerifier.parser
import cympl.interpreter.antlr.AntlrInterpretVerifier.verify
import cympl.language.BuiltinType
import cympl.language.Statement
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class FunctionTests {

    @Test
    fun `function can't be called without being defined`() {
        val input = """
            f(1);
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 1
                r.errors[0].shouldHaveMessage("semantic error at (2:12): function: f not defined")
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

    @Test
    fun `function can be called before defined`() {
        val input = """
            int f(int x) {
                return g(x + 1);
            }
            int g(int x) {
                return x * 2;
            }
            f(2);
        """
        val output = """
            func f(int x):int { return g(x + 1); } => Closure(#f)
            func g(int x):int { return x * 2; } => Closure(#g)
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
                r.errors.first().shouldHaveMessage("semantic error at (2:29): symbol x already defined")
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
                if (1 < 2) {
                    int x = 1;
                }
                return x;
            }
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 2
                r.errors[0].shouldHaveMessage("semantic error at (3:20): symbol x already defined")
                r.errors[1].shouldHaveMessage("semantic error at (5:24): symbol x already defined")
            }

            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

    @Test
    fun `allow global variable being shadowed inside function`() {
        val input = """
            int x = 1;
            int f() {
                int x = 2;
                return x + 3;
            }
            f();
        """.trimIndent()

        val output = """
            int x = 1; => 1
            func f():int { int x = 2; return x + 3; } => Closure(#f)
            f(); => 5
            environment:
            x:int = 1
            f: () -> int
        """.trimIndent()

        verify(input, output)
    }

    @Test
    fun `can't assign value to function`() {
        val input = """
            int f(int x) {
                return x + 1;
            }
            f = 1;
            f;
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 1
                r.errors[0].shouldHaveMessage("semantic error at (5:16): type mismatch: expected (int) -> int, but got int")
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
            void f() {
               1+2;
            }
            int i = f();
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 1
                r.errors[0].shouldHaveMessage("semantic error at (5:20): type mismatch: expected int, but got void")
            }

            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

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
            func insertionSort(int[] arr):int[] { for (int i = 1; i < arr.length; i++;) { int key = arr[i]; int j = i - 1; while (j >= 0 && arr[j] > key) { arr[j + 1] = arr[j]; j = j - 1; } arr[j + 1] = key; } return arr; } => Closure(#insertionSort)
            insertionSort([2, 3, 9, 1, 11, 32, 17, 23, 15, 21]); => [1, 2, 3, 9, 11, 15, 17, 21, 23, 32]
            environment:
            insertionSort: (int[]) -> int[]
        """.trimIndent()

        verify(input, output)
    }

    @Test
    fun `support closure`() {
        val input = """
            int f(int x) {
                int g(int y) {
                    return x + y;
                }
                return g(1);
            }
            f(2);
        """.trimIndent()

        val output = """
            func f(int x):int { func g(int y):int { return x + y; } return g(1); } => Closure(#f)
            f(2); => 3
            environment:
            f: (int) -> int
        """.trimIndent()

        verify(input, output)
    }

    @Test
    fun `closure can be returned from function`() {
        val input = """
            () -> int f() {
                int x = 0;
                int g() {
                    x = x + 1;
                    return x;
                }
                return g;
            }
            () -> int g = f();
            g();
            g();
            g();
        """.trimIndent()

        val output = """
            func f():() -> int { int x = 0; func g():int { x = x + 1; return x; } return g; } => Closure(#f)
            () -> int g = f(); => Closure(#g)
            g(); => 1
            g(); => 2
            g(); => 3
            environment:
            f: () -> () -> int, g: () -> int
        """.trimIndent()

        verify(input, output)
    }

    @Test
    fun `closure can be passed to a function`() {
            val input = """
            int f(int x, () -> int gg) {
                return gg() + x;
            }
            int g() {
                return 1;
            }
            f(2, g);
        """.trimIndent()

        val output = """
            func f(int x, () -> int gg):int { return gg() + x; } => Closure(#f)
            func g():int { return 1; } => Closure(#g)
            f(2, g); => 3
            environment:
            f: (int, () -> int) -> int, g: () -> int
        """.trimIndent()

        verify(input, output)
    }

    @Test
    fun `support function that has only varargs`() {
        val input = """
            int sum(int... args) {
                int s = 0;
                for(int i = 0; i < args.length; i++) {
                    s = s + args[i];
                }
                return s;
            }
            sum(1,2,3,4,5);
        """.trimIndent()

        val output = """
            func sum(int... args):int { int s = 0; for (int i = 0; i < args.length; i++;) { s = s + args[i]; } return s; } => Closure(#sum)
            sum([1, 2, 3, 4, 5]); => 15
            environment:
            sum: (int...) -> int
        """.trimIndent()

        verify(input, output)
    }

    @Test
    fun `support function that has both fix and variable args`() {
        val input = """
            String join(String sep, String... parts) {
                String s = "";
                for(int i = 0; i < parts.length; i++) {
                    s = s + parts[i];
                    if (i < parts.length - 1) {
                        s = s + sep;
                    }
                }
                return s;
            }
            join(",", "a", "b", "c");
        """.trimIndent()

        val output = """
            func join(String sep, String... parts):String { String s = ""; for (int i = 0; i < parts.length; i++;) { s = s + parts[i]; if (i < parts.length - 1) { s = s + sep; } } return s; } => Closure(#join)
            join(",", ["a", "b", "c"]); => "a,b,c"
            environment:
            join: (String, String...) -> String
        """.trimIndent()

        verify(input, output)
    }

}
