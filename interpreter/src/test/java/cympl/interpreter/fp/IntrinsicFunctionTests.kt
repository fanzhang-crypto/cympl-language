package cympl.interpreter.fp

import org.junit.jupiter.api.Test
import cympl.interpreter.fp.FpInterpretVerifier.verify

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
