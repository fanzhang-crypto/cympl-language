package cympl.interpreter.antlr

import cympl.interpreter.antlr.AntlrInterpretVerifier.verify
import org.junit.jupiter.api.Test

class BlockTests  {

    @Test
    fun `variables in different parallel blocks have no conflict`() {
        val input = """
            {
                {
                    int x = 2;
                }
                {
                    int x = 1;
                }
            }
        """
        val output = """
            { { int x = 2; } { int x = 1; } } => void
            environment:
        """
        verify(input, output)
    }

    @Test
    fun `functions in different blocks have no conflict`() {
        val input = """
            {
                int f(int x) { return x + 1; }
                {
                    int f(int x) { return x + 2; }
                }
                f(1);
            }
        """
        val output = """
            { func f(int x):int { return x + 1; } { func f(int x):int { return x + 2; } } f(1); } => void
            environment:
        """
        verify(input, output)
    }

}
