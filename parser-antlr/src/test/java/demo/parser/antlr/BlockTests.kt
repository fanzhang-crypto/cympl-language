package demo.parser.antlr

import org.junit.jupiter.api.Test

class BlockTests : InterpreterTest() {

    @Test
    fun `variables in different blocks have no conflict`() {
        val input = """
            {
                x:INT = 1;
                {
                    x:INT = 2;
                }
                return x;
            }
        """
        val output = """
            { x:INT = 1; { x:INT = 2; } return x; } => 1
            environment:
        """
        verify(input, output)
    }

    @Test
    fun `support simple if else blocks`() {
        val input = """
            x:INT = 1;
            if (x == 1) {
                x:INT = 2;
                return x;
            } else {
                x:INT = 3;
                return x;
            }
            x;
        """
        val output = """
            x:INT = 1; => 1
            if (x == 1) { x:INT = 2; return x; } else { x:INT = 3; return x; } => 2
            x; => 1
            environment:
            x:INT = 1
        """
        verify(input, output)
    }

    @Test
    fun `support if else statement`() {
        val input = """
            x:INT = 1;
            if (x == 1)
                x = 2;
            else
                x = 3;
            x;
        """
        val output = """
                    x:INT = 1; => 1
                    if (x == 1) x = 2; else x = 3; => 2
                    x; => 2
                    environment:
                    x:INT = 2
                """
        verify(input, output)
    }

    @Test
    fun `support nested if else blocks`() {
        val input = """
            x:INT = 1;
            if (x == 1) {
                if (x < 0) {
                    x = -x;
                } else {
                    x = x + 1;
                }
            } else {
                x = 5;
            }
            x;
        """
        val output = """
            x:INT = 1; => 1
            if (x == 1) { if (x < 0) { x = -x; } else { x = x + 1; } } else { x = 5; } => void
            x; => 2
            environment:
            x:INT = 2
        """
        verify(input, output)
    }
}
