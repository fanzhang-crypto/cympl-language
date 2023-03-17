package demo.parser.interpret.antlr

import org.junit.jupiter.api.Test
import demo.parser.interpret.antlr.AntlrInterpretVerifier.verify

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
