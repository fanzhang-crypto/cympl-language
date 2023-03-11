package demo.parser.app.fp

import demo.parser.app.fp.FpInterpretVerifier.verify
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
    fun `functions in different blocks have no conflict`() {
        val input = """
            {
                func f(x:INT):INT { return x + 1; }
                {
                    func f(x:INT):INT { return x + 2; }
                }
                return f(1);
            }
        """
        val output = """
            { func f(x:INT):INT { return x + 1; } { func f(x:INT):INT { return x + 2; } } return f(1); } => 2
            environment:
        """
        verify(input, output)
    }

}
