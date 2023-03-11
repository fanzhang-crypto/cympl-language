package demo.parser.app.fp

import org.junit.jupiter.api.Test
import demo.parser.app.fp.FpInterpretVerifier.verify

class WhileStatementTests {

    @Test
    fun `support simple while statement`() {
        val input = """
            x:INT = 1;
            while (x < 10) {
                x = x + 1;
            }
            x;
        """

        val output ="""
            x:INT = 1; => 1
            while (x < 10) { x = x + 1; } => void
            x; => 10
            environment:
            x:INT = 10
        """

        verify(input, output)
    }

    @Test
    fun `support break in while loop`() {
        val input = """
            x:INT = 1;
            while (x < 10) {
                x = x + 1;
                if (x == 5) {
                    break;
                }
            }
            x;
        """
        val output = """
            x:INT = 1; => 1
            while (x < 10) { x = x + 1; if (x == 5) { break; } } => void
            x; => 5
            environment:
            x:INT = 5
        """
        verify(input, output)
    }

    @Test
    fun `support continue in while loop`() {
        val input = """
            x:INT = 0;
            i:INT = 0;
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
            x:INT = 0; => 0
            i:INT = 0; => 0
            while (i < 10) { if (i % 2 == 0) { i = i + 1; continue; } x = x + i; i = i + 1; } => void
            x; => 25
            environment:
            x:INT = 25, i:INT = 10
        """
        verify(input, output)
    }

    @Test
    fun `support nest while loop`() {
        val input = """
            x:INT = 0;
            i:INT = 0;
            while (i < 10) {
                j:INT = 0;
                while (j < 10) {
                    x = x + 1;
                    j = j + 1;
                }
                i = i + 1;
            }
            x;
        """
        val output = """
            x:INT = 0; => 0
            i:INT = 0; => 0
            while (i < 10) { j:INT = 0; while (j < 10) { x = x + 1; j = j + 1; } i = i + 1; } => void
            x; => 100
            environment:
            x:INT = 100, i:INT = 10
        """
        verify(input, output)
    }

    @Test
    fun `support nested break in nested while loop`() {
        val input = """
            x:INT = 0;
            i:INT = 0;
            while (i < 10) {
                j:INT = 0;
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
            x:INT = 0; => 0
            i:INT = 0; => 0
            while (i < 10) { j:INT = 0; while (j < 10) { x = x + 1; if (j > 5) { break; } j = j + 1; } i = i + 1; } => void
            x; => 70
            environment:
            x:INT = 70, i:INT = 10
        """
        verify(input, output)
    }

    @Test
    fun `support nested continue and break in nested while loop`() {
        val input = """
            x:INT = 0;
            i:INT = 0;
            while (i < 10) {
                j:INT = 0;
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
            x:INT = 0; => 0
            i:INT = 0; => 0
            while (i < 10) { j:INT = 0; while (j < 10) { if (j % 2 == 0) { j = j + 1; continue; } x = x + 1; if (j > 5) { break; } j = j + 1; } i = i + 1; } => void
            x; => 40
            environment:
            x:INT = 40, i:INT = 10
        """
        verify(input, output)
    }

    @Test
    fun `can return early from a while statement in function`() {
        val input = """
            func f(x:INT):INT {
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
            func f(x:INT):INT { while (x < 10) { x = x + 1; if (x == 5) { return x; } } return x; } => void
            f(1); => 5
            environment:
            f(x:INT):INT
        """
        verify(input, output)
    }
}
