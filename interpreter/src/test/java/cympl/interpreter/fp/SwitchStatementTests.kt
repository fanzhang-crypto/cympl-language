package cympl.interpreter.fp

import cympl.interpreter.antlr.AntlrInterpretVerifier.verify
import org.junit.jupiter.api.Test

class SwitchStatementTests {

    @Test
    fun `support normal switch with multiple cases`() {
        val input = """
            int x = 1;
            String result = "";
            switch (x) {
                case 1:
                    result = "one";
                    break;
                case 2:
                    result = "two";
                    break;
                default:
                    result = "other";
            }
            result;
        """

        val output = """
            int x = 1; => 1
            String result = ""; => ""
            switch (x) {
            case 1:result = "one";break;
            case 2:result = "two";break;
            default:result = "other";
            } => void
            result; => "one"
            environment:
            x:int = 1, result:String = "one"
        """

        verify(input, output)
    }

    @Test
    fun `support switch with fall-through cases`() {
        val input = """
            int x = 1;
            String result = "";
            switch (x) {
                case 1:
                case 2:
                    result = "one or two";
                    break;
                default:
                    result = "other";
            }
            result;
        """

        val output = """
            int x = 1; => 1
            String result = ""; => ""
            switch (x) {
            case 1:
            case 2:result = "one or two";break;
            default:result = "other";
            } => void
            result; => "one or two"
            environment:
            x:int = 1, result:String = "one or two"
        """

        verify(input, output)
    }

    @Test
    fun `switch fall through from 2nd case`() {
        val input = """
            int x = 2;
            String result = "";
            switch (x) {
                case 1:
                    result = result + "one";
                case 2:
                    result = result + "two";
                case 3:
                    result = result + "three";
                    break;
                case 4:
                    result = result + "four";
                default:
                    result = "other";
            }
            result;
        """

        val output = """
            int x = 2; => 2
            String result = ""; => ""
            switch (x) {
            case 1:result = result + "one";
            case 2:result = result + "two";
            case 3:result = result + "three";break;
            case 4:result = result + "four";
            default:result = "other";
            } => void
            result; => "twothree"
            environment:
            x:int = 2, result:String = "twothree"
        """

        verify(input, output)
    }

    @Test
    fun `default case should be matched if no other case is matched`() {
        val input = """
            int x = 4;
            String result = "";
            switch (x) {
                case 1:
                    result = "one";
                    break;
                case 2:
                    result = "two";
                    break;
                default:
                    result = "other";
            }
            result;
        """

        val output = """
            int x = 4; => 4
            String result = ""; => ""
            switch (x) {
            case 1:result = "one";break;
            case 2:result = "two";break;
            default:result = "other";
            } => void
            result; => "other"
            environment:
            x:int = 4, result:String = "other"
        """

        verify(input, output)
    }

    @Test
    fun `there can be no default case`() {
        val input = """
            int x = 4;
            String result = "";
            switch (x) {
                case 1:
                    result = "one";
                    break;
                case 2:
                    result = "two";
                    break;
            }
            result;
        """

        val output = """
            int x = 4; => 4
            String result = ""; => ""
            switch (x) {
            case 1:result = "one";break;
            case 2:result = "two";break;
            } => void
            result; => ""
            environment:
            x:int = 4, result:String = ""
        """

        verify(input, output)
    }

    @Test
    fun `can return from a switch case statement in a function`() {
        val input = """
            String foo(int x) {
                switch (x) {
                    case 1:
                        return "one";
                    case 2:
                        return "two";
                    default:
                        return "other";
                }
            }
            foo(1);
        """

        val output = """
            func foo(int x):String { switch (x) {
            case 1:return "one";
            case 2:return "two";
            default:return "other";
            } } => Closure(#foo)
            foo(1); => "one"
            environment:
            foo: (int) -> String
            """

        verify(input, output)
    }
}
