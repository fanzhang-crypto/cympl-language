package demo.parser.interpret.antlr

import org.junit.jupiter.api.Test
import demo.parser.interpret.antlr.AntlrInterpretVerifier.verify

class WhileStatementTests {

    @Test
    fun `support simple while statement`() {
        val input = """
            int x = 1;
            while (x < 10) {
                x = x + 1;
            }
            x;
        """

        val output ="""
            x:int = 1; => 1
            while (x < 10) { x = x + 1; } => void
            x; => 10
            environment:
            x:int = 10
        """

        verify(input, output)
    }

    @Test
    fun `support break in while loop`() {
        val input = """
            int x = 1;
            while (x < 10) {
                x = x + 1;
                if (x == 5) {
                    break;
                }
            }
            x;
        """
        val output = """
            x:int = 1; => 1
            while (x < 10) { x = x + 1; if (x == 5) { break; } } => void
            x; => 5
            environment:
            x:int = 5
        """
        verify(input, output)
    }

    @Test
    fun `support continue in while loop`() {
        val input = """
            int x = 0;
            int i = 0;
            while (i < 10) {
                if (i % 2 == 0) {
                    i = i + 1;
                    continue;
                }
                x = x + i;
                i = i + 1;
            }
            x;
        """

        val output ="""
            x:int = 0; => 0
            i:int = 0; => 0
            while (i < 10) { if (i % 2 == 0) { i = i + 1; continue; } x = x + i; i = i + 1; } => void
            x; => 25
            environment:
            x:int = 25, i:int = 10
        """
        verify(input, output)
    }

    @Test
    fun `support nest while loop`() {
        val input = """
            int x = 0;
            int i = 0;
            while (i < 10) {
                int j = 0;
                while (j < 10) {
                    x = x + 1;
                    j = j + 1;
                }
                i = i + 1;
            }
            x;
        """
        val output = """
            x:int = 0; => 0
            i:int = 0; => 0
            while (i < 10) { j:int = 0; while (j < 10) { x = x + 1; j = j + 1; } i = i + 1; } => void
            x; => 100
            environment:
            x:int = 100, i:int = 10
        """
        verify(input, output)
    }

    @Test
    fun `support nested break in nested while loop`() {
        val input = """
            int x = 0;
            int i = 0;
            while (i < 10) {
                int j = 0;
                while (j < 10) {
                    x = x + 1;
                    if (j > 5) {
                        break;
                    }
                    j = j + 1;
                }
                i = i + 1;
            }
            x;
        """

        val output = """
            x:int = 0; => 0
            i:int = 0; => 0
            while (i < 10) { j:int = 0; while (j < 10) { x = x + 1; if (j > 5) { break; } j = j + 1; } i = i + 1; } => void
            x; => 70
            environment:
            x:int = 70, i:int = 10
        """
        verify(input, output)
    }

    @Test
    fun `support nested continue and break in nested while loop`() {
        val input = """
            int x = 0;
            int i = 0;
            while (i < 10) {
                int j = 0;
                while (j < 10) {
                    if (j % 2 == 0) {
                        j = j + 1;
                        continue;
                    }
                    x = x + 1;
                    if (j > 5) {
                        break;
                    }
                    j = j + 1;
                }
                i = i + 1;
            }
            x;
        """

        val output ="""
            x:int = 0; => 0
            i:int = 0; => 0
            while (i < 10) { j:int = 0; while (j < 10) { if (j % 2 == 0) { j = j + 1; continue; } x = x + 1; if (j > 5) { break; } j = j + 1; } i = i + 1; } => void
            x; => 40
            environment:
            x:int = 40, i:int = 10
        """
        verify(input, output)
    }

    @Test
    fun `can return early from a while statement in function`() {
        val input = """
            int f(int x) {
                while (x < 10) {
                    x = x + 1;
                    if (x == 5) {
                        return x;
                    }
                }
                return x;
            }
            f(1);
        """
        val output ="""
            func f(x:int):int { while (x < 10) { x = x + 1; if (x == 5) { return x; } } return x; } => void
            f(1); => 5
            environment:
            f(x:int):int
        """
        verify(input, output)
    }
}
