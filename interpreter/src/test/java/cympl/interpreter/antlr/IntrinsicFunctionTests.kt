package cympl.interpreter.antlr

import org.junit.jupiter.api.Test
import cympl.interpreter.antlr.AntlrInterpretVerifier.verify

class IntrinsicFunctionTests {

    @Test
    fun `println works`() {
        val input = """
            println("Hello, world!");
        """

        val output = """
            println("Hello, world!"); => void
            environment:
        """

        verify(input, output)
    }
}
