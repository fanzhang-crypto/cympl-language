package cympl.interpreter.antlr

import org.junit.jupiter.api.Test
import cympl.interpreter.antlr.AntlrInterpretVerifier.verify

class TypealiasTests {

    @Test
    fun `support typealias for variable declaration`() {
        val input = """
            typealias MY_INT = int;
            MY_INT x = 1;
        """.trimIndent()

        val output = """
            int x = 1; => 1
            environment:
            x:int = 1
        """.trimIndent()

        verify(input, output)
    }

    @Test
    fun `support typealias for function declaration`() {
        val input = """
            typealias MY_INT = int;
            MY_INT foo(MY_INT x) { return x + 1; }
        """.trimIndent()

        val output = """
            func foo(int x):int { return x + 1; } => Closure(#foo)
            environment:
            foo: (int) -> int
        """.trimIndent()

        verify(input, output)
    }

    @Test
    fun `support high order type alias`() {
        val input = """
            typealias MY_INT = int;
            typealias MY_FUNC = (MY_INT) -> MY_INT;
            MY_FUNC foo = (x) -> x + 1;
            foo(1);
        """.trimIndent()

        val output = """
            (int) -> int foo = (x) -> x + 1; => Closure(#<lambda>)
            foo(1); => 2
            environment:
            foo: (int) -> int
        """.trimIndent()

        verify(input, output)
    }
}
