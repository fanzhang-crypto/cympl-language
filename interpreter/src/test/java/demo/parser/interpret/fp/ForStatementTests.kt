package demo.parser.interpret.fp

import org.junit.jupiter.api.Test
import demo.parser.interpret.fp.FpInterpretVerifier.verify

class ForStatementTests {

    @Test
    fun `support single statement in for loop`() {
        val input = """
            int x = 0;
            for (int i = 0; i < 10; i = i + 1) 
                x = x + i;
            x;
        """

        val output = """
            x:int = 0; => 0
            for (i:int = 0; i < 10; i = i + 1;) x = x + i; => void
            x; => 45
            environment:
            x:int = 45, i:int = 10
        """

        verify(input, output)
    }

    @Test
    fun `support for loop statement`() {
        val input = """
            int x = 0;
            for (int i = 0; i < 10; i = i + 1) {
                x = x + i;
            }
            x;
        """

        val output = """
            x:int = 0; => 0
            for (i:int = 0; i < 10; i = i + 1;) { x = x + i; } => void
            x; => 45
            environment:
            x:int = 45, i:int = 10
        """

        verify(input, output)
    }

    @Test
    fun `support for loop statement with continue and break`() {
        val input = """
            int x = 0;
            for (int i = 0; i < 10; i = i + 1) {
                for (int j = 0; j < 10; j = j + 1) {
                    if (j % 2 == 0) {
                        continue;
                    }
                    x = x + 1;
                    if (j > 5) {
                        break;
                    }
                }
            }
            x;
        """
        val output = """
            x:int = 0; => 0
            for (i:int = 0; i < 10; i = i + 1;) { for (j:int = 0; j < 10; j = j + 1;) { if (j % 2 == 0) { continue; } x = x + 1; if (j > 5) { break; } } } => void
            x; => 40
            environment:
            x:int = 40, i:int = 10
        """

        verify(input, output)
    }

    @Test
    fun `can return early from a for loop in function`() {
        val input = """
            int f(int x) {
                for (int i = 0; i < 10; i = i + 1) {
                    x = x + 1;
                    if (x == 5) {
                        return x;
                    }
                }
                return x;
            }
            f(1);
        """
        val output = """
            func f(x:int):int { for (i:int = 0; i < 10; i = i + 1;) { x = x + 1; if (x == 5) { return x; } } return x; } => void
            f(1); => 5
            environment:
            f(x:int):int
        """
        verify(input, output)
    }

    @Test
    fun `empty condition in for loop`() {
        val input = """
            int x = 0;
            for (int i = 0; ; i = i + 1) {
                x = x + i;
                if (i > 10) {
                    break;
                }
            }
            x;
        """
        val output= """
            x:int = 0; => 0
            for (i:int = 0; ; i = i + 1;) { x = x + i; if (i > 10) { break; } } => void
            x; => 66
            environment:
            x:int = 66, i:int = 11
        """.trimIndent()

        verify(input, output)
    }
}
