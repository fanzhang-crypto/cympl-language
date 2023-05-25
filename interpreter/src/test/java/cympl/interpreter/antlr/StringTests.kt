package cympl.interpreter.antlr

import org.junit.jupiter.api.Test
import cympl.interpreter.antlr.AntlrInterpretVerifier.verify

class StringTests {

    @Test
    fun `string concatenation test`() {
        val input = """
            String s1 = "a" + "b" + "c";
            String s2 = "d" + 1 + 2 + 3;
            String s3 = s1 + s2;
        """
        val output = """
            String s1 = "a" + "b" + "c"; => "abc"
            String s2 = "d" + 1 + 2 + 3; => "d123"
            String s3 = s1 + s2; => "abcd123"
            environment:
            s1:String = "abc", s2:String = "d123", s3:String = "abcd123"
        """
        verify(input, output)
    }

    @Test
    fun `can access length property of a string`() {
        val input = """
            String s = "abc";
            int len = s.length;
        """
        val output = """
            String s = "abc"; => "abc"
            int len = s.length; => 3
            environment:
            s:String = "abc", len:int = 3
        """
        verify(input, output)
    }
}
