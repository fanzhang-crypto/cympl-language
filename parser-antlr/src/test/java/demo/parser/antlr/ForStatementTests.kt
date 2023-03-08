package demo.parser.antlr

import org.junit.jupiter.api.Test

class ForStatementTests : InterpreterTest() {

    @Test
    fun `support single statement in for loop`() {
        val input = """
            x:INT = 0;
            for (i:INT = 0; i < 10; i = i + 1) 
                x = x + i;
            x;
        """

        val output = """
            x:INT = 0; => 0
            for (i:INT = 0; i < 10; i = i + 1;) x = x + i; => void
            x; => 45
            environment:
            x:INT = 45, i:INT = 10
        """

        verify(input, output)
    }

    @Test
    fun `support for loop statement`() {
        val input = """
            x:INT = 0;
            for (i:INT = 0; i < 10; i = i + 1) {
                x = x + i;
            }
            x;
        """

        val output = """
            x:INT = 0; => 0
            for (i:INT = 0; i < 10; i = i + 1;) { x = x + i; } => void
            x; => 45
            environment:
            x:INT = 45, i:INT = 10
        """

        verify(input, output)
    }

    @Test
    fun `support for loop statement with continue and break`() {
        val input = """
            x:INT = 0;
            for (i:INT = 0; i < 10; i = i + 1) {
                for (j:INT = 0; j < 10; j = j + 1) {
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
            x:INT = 0; => 0
            for (i:INT = 0; i < 10; i = i + 1;) { for (j:INT = 0; j < 10; j = j + 1;) { if (j % 2 == 0) { continue; } x = x + 1; if (j > 5) { break; } } } => void
            x; => 40
            environment:
            x:INT = 40, i:INT = 10
        """

        verify(input, output)
    }

    @Test
    fun `can return early from a for loop in function`() {
        val input = """
            func f(x:INT):INT {
                for (i:INT = 0; i < 10; i = i + 1) {
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
                    func f(x:INT):INT { for (i:INT = 0; i < 10; i = i + 1;) { x = x + 1; if (x == 5) { return x; } } return x; } => void
                    f(1); => 5
                    environment:
                    f(x:INT):INT
                """
        verify(input, output)
    }

    @Test
    fun `empty condition in for loop`() {
        val input = """
            x:INT = 0;
            for (i:INT = 0; ; i = i + 1) {
                x = x + i;
                if (i > 10) {
                    break;
                }
            }
            x;
        """
        val output= """
            x:INT = 0; => 0
            for (i:INT = 0; ; i = i + 1;) { x = x + i; if (i > 10) { break; } } => void
            x; => 66
            environment:
            x:INT = 66, i:INT = 11
        """.trimIndent()

        verify(input, output)
    }
}
