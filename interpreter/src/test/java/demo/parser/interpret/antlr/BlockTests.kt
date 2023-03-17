package demo.parser.interpret.antlr

import demo.parser.interpret.antlr.AntlrInterpretVerifier.verify
import org.junit.jupiter.api.Test

class BlockTests  {

    @Test
    fun `variables in different blocks have no conflict`() {
        val input = """
            {
                x:INT = 1;
                {
                    x:INT = 2;
                }
                x;
            }
        """
        val output = """
            { x:INT = 1; { x:INT = 2; } x; } => void
            environment:
        """
        verify(input, output)
    }

    @Test
    fun `functions in different blocks have no conflict`() {
        val input = """
            {
                func f(x:INT):INT { return x + 1; }
                {
                    func f(x:INT):INT { return x + 2; }
                }
                f(1);
            }
        """
        val output = """
            { func f(x:INT):INT { return x + 1; } { func f(x:INT):INT { return x + 2; } } f(1); } => void
            environment:
        """
        verify(input, output)
    }

}
