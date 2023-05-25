package cympl.interpreter.antlr

import org.junit.jupiter.api.Test
import cympl.interpreter.antlr.AntlrInterpretVerifier.verify

class IfStatementTests {

    @Test
    fun `support simple if else blocks`() {
        val input = """
            int x = 1;
            if (x == 1) {
                int x = 2;
                x;
            } else {
                int x = 3;
                x;
            }
            x;
        """
        val output = """
            int x = 1; => 1
            if (x == 1) { int x = 2; x; } else { int x = 3; x; } => void
            x; => 1
            environment:
            x:int = 1
        """
        verify(input, output)
    }

    @Test
    fun `support if else statement`() {
        val input = """
            int x = 1;
            if (x == 1)
                x = 2;
            else
                x = 3;
            x;
        """
        val output = """
                    int x = 1; => 1
                    if (x == 1) x = 2; else x = 3; => 2
                    x; => 2
                    environment:
                    x:int = 2
                """
        verify(input, output)
    }

    @Test
    fun `support nested if else blocks`() {
        val input = """
            int x = 1;
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
            int x = 1; => 1
            if (x == 1) { if (x < 0) { x = -x; } else { x = x + 1; } } else { x = 5; } => void
            x; => 2
            environment:
            x:int = 2
        """
        verify(input, output)
    }
}
