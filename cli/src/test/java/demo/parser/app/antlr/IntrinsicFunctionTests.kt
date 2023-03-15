package demo.parser.app.antlr

import org.junit.jupiter.api.Test
import demo.parser.app.antlr.AntlrInterpretVerifier.verify

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
