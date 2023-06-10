package cympl.interpreter.antlr

import cympl.interpreter.antlr.AntlrInterpretVerifier.parser
import cympl.interpreter.antlr.AntlrInterpretVerifier.verify
import cympl.parser.ParseResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.throwable.shouldHaveMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class ScopeCheckTests  {

    @Test
    fun `variable shadowing is not allowed in if else block`() {
        val input = """
            int x = 10;

            if (x > 5) {
                int x = 20;
                println("x is greater than 5");
            } else {
                int x = 30;
                println("x is less than or equal to 5");
            }
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 2
                r.errors[0].shouldHaveMessage("semantic error at (5:21): symbol x already defined")
                r.errors[1].shouldHaveMessage("semantic error at (8:21): symbol x already defined")
            }

            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

    @Test
    fun `variable shadowing is not allowed in while block`() {
        val input = """
            int x = 10;

            while (x > 5) {
                int x = 20;
                println("x is greater than 5");
            }
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 1
                r.errors[0].shouldHaveMessage("semantic error at (5:21): symbol x already defined")
            }

            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

    @Test
    fun `variable shadowing is not allowed in for block`() {
        val input = """
            int x = 10;

            for (int x = 0; x < 10; x = x + 1) {
                println("x is greater than 5");
            }
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 1
                r.errors[0].shouldHaveMessage("semantic error at (4:22): symbol x already defined")
            }

            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

    @Test
    fun `for loop should have a separate scope even without brackets`() {
        val input = """
            for (int x = 0; x < 3; x++)
                println(x);
                
            println(x);
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 1
                r.errors[0].shouldHaveMessage("semantic error at (5:21): variable x not defined")
            }

            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

    @Test
    fun `external variable can be shadowed in function`() {
        val input = """
            int x = 10;

            void foo() {
                int x = 20;
                println("x is" + x);
            }
            foo();
        """.trimIndent()

        val output = """
            int x = 10; => 10
            func foo():void { int x = 20; println("x is" + x); } => Closure(#foo)
            foo(); => void
            environment:
            x:int = 10
            foo: () -> void
        """.trimIndent()

        verify(input, output)
    }

    @Test
    fun `variable shadowing not allowed inside function`() {
        val input = """
            int f(int x) {
                int x = 1;
                if (1 < 2) {
                    int x = 1;
                }
                return x;
            }
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 2
                r.errors[0].shouldHaveMessage("semantic error at (3:21): symbol x already defined")
                r.errors[1].shouldHaveMessage("semantic error at (5:25): symbol x already defined")
            }

            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

    @Test
    fun `function name shadowing not allowed inside function`() {
        val input = """
            {
                int f(int x) { return x + 1; }
                {
                    int f(int x) { return x + 2; }
                }
                f(1);
            }
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 1
                r.errors[0].shouldHaveMessage("semantic error at (5:25): symbol f already defined")
            }

            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

    @Test
    fun `variables in different parallel blocks have no conflict`() {
        val input = """
            {
                {
                    int x = 2;
                }
                {
                    int x = 1;
                }
            }
        """
        val output = """
            { { int x = 2; } { int x = 1; } } => void
            environment:
        """
        verify(input, output)
    }

    @Test
    fun `variable's declaration must precede its reference`() {
        val input = """
            {
                int x = y;
                int y = 1;
            }
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 1
                r.errors[0].shouldHaveMessage("semantic error at (3:25): variable y not defined")
            }

            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

    @Test
    fun `function can be called before defined`() {
        val input = """
            int f(int x) {
                return g(x + 1);
            }
            int g(int x) {
                return x * 2;
            }
            f(2);
        """
        val output = """
            func f(int x):int { return g(x + 1); } => Closure(#f)
            func g(int x):int { return x * 2; } => Closure(#g)
            f(2); => 6
            environment:
            f: (int) -> int, g: (int) -> int
        """
        verify(input, output)
    }
}
