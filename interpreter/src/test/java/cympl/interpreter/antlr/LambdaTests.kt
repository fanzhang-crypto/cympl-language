package cympl.interpreter.antlr

import cympl.interpreter.antlr.AntlrInterpretVerifier.verify
import org.junit.jupiter.api.Test

class LambdaTests {

    @Test
    fun `support lambda expression in variable declaration`() {
        val input = """
            (int, int) -> int g = (x, y) -> x + y;
            g(1, 2);
        """.trimIndent()

        val output = """
            g:(int, int) -> int = (x, y) -> x + y; => Closure(#<lambda>)
            g(1, 2); => 3
            environment:
            g: (int, int) -> int
        """.trimIndent()

        verify(input, output)
    }

    @Test
    fun `support lambda expression in variable assignment`() {
            val input = """
            (int) -> int g = (x) -> x + 1;
            g = (x) -> x + 2;
            g(1);
        """.trimIndent()

        val output = """
            g:(int) -> int = (x) -> x + 1; => Closure(#<lambda>)
            g = (x) -> x + 2; => Closure(#<lambda>)
            g(1); => 3
            environment:
            g: (int) -> int
        """.trimIndent()

        verify(input, output)
    }

    @Test
    fun `support lambda expression as return value in function body`() {
        val input = """
            (int) -> (int) -> int f = (x) -> (y) -> x + y;
            f(1)(2);
        """.trimIndent()

        val output = """
            f:(int) -> (int) -> int = (x) -> (y) -> x + y; => Closure(#<lambda>)
            f(1)(2); => 3
            environment:
            f: (int) -> (int) -> int
        """.trimIndent()

        verify(input, output)
    }

    @Test
    fun `support accept lambda expression as function parameter`() {
        val input = """
            (int, (int) -> int) -> int f = (x, g) -> g(x);
            f(1, (x) -> x + 1);
        """.trimIndent()

        val output = """
            f:(int, (int) -> int) -> int = (x, g) -> g(x); => Closure(#<lambda>)
            f(1, (x) -> x + 1); => 2
            environment:
            f: (int, (int) -> int) -> int
        """.trimIndent()

        verify(input, output)
    }

    @Test
    fun `fibonacci with lambda`() {
        val input = """
            () -> int fibonacci() {
                int a = 0;
                int b = 1;
                return () -> {
                    int c = a;
                    a = b;
                    b = a + c;
                    return c;
                };
            }
            () -> int fibo = fibonacci();
            fibo();
            fibo();
            fibo();
            fibo();
            fibo();
        """.trimIndent()

        val output = """
            func fibonacci():() -> int { a:int = 0; b:int = 1; return () -> { c:int = a; a = b; b = a + c; return c; }; } => Closure(#fibonacci)
            fibo:() -> int = fibonacci(); => Closure(#<lambda>)
            fibo(); => 0
            fibo(); => 1
            fibo(); => 1
            fibo(); => 2
            fibo(); => 3
            environment:
            fibonacci: () -> () -> int, fibo: () -> int
        """.trimIndent()

        verify(input, output)
    }
}
