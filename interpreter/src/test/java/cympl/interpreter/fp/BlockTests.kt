package cympl.interpreter.fp

import cympl.interpreter.fp.FpInterpretVerifier.verify
import org.junit.jupiter.api.Test

class BlockTests  {

    @Test
    fun `variables in different blocks have no conflict`() {
        val input = """
            {
                int x = 1;
                {
                    int x = 2;
                }
                x;
            }
        """
        val output = """
            { x:int = 1; { x:int = 2; } x; } => void
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
            { func f(x:int):int { return x + 1; } { func f(x:int):int { return x + 2; } } f(1); } => void
            environment:
        """
        verify(input, output)
    }

}
