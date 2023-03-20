package demo.parser.interpret.fp

import org.junit.jupiter.api.Test
import demo.parser.interpret.fp.FpInterpretVerifier.verify

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
