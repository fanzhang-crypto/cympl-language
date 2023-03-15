package demo.parser.app.antlr

import org.junit.jupiter.api.Test
import demo.parser.app.antlr.AntlrInterpretVerifier.verify

class StringTests {

    @Test
    fun `string concatenation test`() {
        val input = """
            s1:STRING = "a" + "b" + "c";
            s2:STRING = "d" + 1 + 2 + 3;
            s3:STRING = s1 + s2;
        """
        val output = """
            s1:STRING = "a" + "b" + "c"; => "abc"
            s2:STRING = "d" + 1 + 2 + 3; => "d123"
            s3:STRING = s1 + s2; => "abcd123"
            environment:
            s1:STRING = "abc", s2:STRING = "d123", s3:STRING = "abcd123"
        """
        verify(input, output)
    }

    @Test
    fun `can access length property of a string`() {
        val input = """
            s:STRING = "abc";
            len:INT = s.length;
        """
        val output = """
            s:STRING = "abc"; => "abc"
            len:INT = s.length; => 3
            environment:
            s:STRING = "abc", len:INT = 3
        """
        verify(input, output)
    }
}
