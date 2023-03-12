package demo.parser.app.antlr

import demo.parser.app.antlr.AntlrInterpretVerifier.parser
import org.junit.jupiter.api.Test
import demo.parser.app.antlr.AntlrInterpretVerifier.verify
import demo.parser.domain.ParseResult
import demo.parser.interpret.InterpretException
import demo.parser.interpret.Interpreter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import org.junit.jupiter.api.fail

class ArrayTests {

    @Test
    fun `array can be declared`() {
        val input = """
            arr:INT[] = [1, 2, 3];
        """

        val output = """
            arr:INT[] = [1, 2, 3]; => [1, 2, 3]
            environment:
            arr:INT[] = [1, 2, 3]
        """

        verify(input, output)
    }

    @Test
    fun `array can be declared as empty`() {
        val input = """
            arr:INT[] = [ ];
        """

        val output = """
            arr:INT[] = []; => []
            environment:
            arr:INT[] = []
        """

        verify(input, output)
    }

    @Test
    fun `array can be returned from a function call`() {
        val input = """
            func f():INT[] {
                return [1, 2, 3];
            }
            arr:INT[] = f();
        """

        val output = """
            func f():INT[] { return [1, 2, 3]; } => void
            arr:INT[] = f(); => [1, 2, 3]
            environment:
            arr:INT[] = [1, 2, 3]
            f():INT[]
        """

        verify(input, output)
    }

    @Test
    fun `array can be passed through function call`(){
        val input = """
            func f(arr:INT[]):INT[] {
                return arr;
            }
            arr:INT[] = f([1, 2, 3]);
        """

        val output = """
            func f(arr:INT[]):INT[] { return arr; } => void
            arr:INT[] = f([1, 2, 3]); => [1, 2, 3]
            environment:
            arr:INT[] = [1, 2, 3]
            f(arr:INT[]):INT[]
        """

        verify(input, output)
    }

    @Test
    fun `should check element type when assigning array`(){
        val input = """
            arr:INT[] = [1, 2, 3.0];
        """

        when (val r = parser().parse(input.byteInputStream())) {
            is ParseResult.Failure -> {
                r.errors.forEach { println(it) }
                fail(r.errors.first())
            }
            is ParseResult.Success -> {
                val program = r.value
                val e = shouldThrow<InterpretException> { Interpreter().interpret(program).joinToString() }
                e shouldHaveMessage "type mismatch: expected INT, got FLOAT"
            }
        }
    }

    @Test
    fun `should check element type when assigning array 2`(){
        val input = """
            func f(arr:INT[]):INT[] {
                return arr;
            }
            arr:STRING[] = f([1, 2, 3]);
        """

        when (val r = parser().parse(input.byteInputStream())) {
            is ParseResult.Failure -> {
                r.errors.forEach { println(it) }
                fail(r.errors.first())
            }
            is ParseResult.Success -> {
                val program = r.value
                val e = shouldThrow<InterpretException> { Interpreter().interpret(program).joinToString() }
                e shouldHaveMessage "type mismatch: expected STRING[], got INT[]"
            }
        }
    }

    @Test
    fun `array can be indexed`() {
        val input = """
            arr:INT[] = [1, 2, 3];
            a:INT = arr[0];
            b:INT = arr[1];
            c:INT = arr[2];
        """

        val output = """
            arr:INT[] = [1, 2, 3]; => [1, 2, 3]
            a:INT = arr[0]; => 1
            b:INT = arr[1]; => 2
            c:INT = arr[2]; => 3
            environment:
            arr:INT[] = [1, 2, 3], a:INT = 1, b:INT = 2, c:INT = 3
        """

        verify(input, output)
    }

    @Test
    fun `array can be assigned with index`() {
        val input = """
            arr:INT[] = [1, 2, 3];
            arr[0] = 4;
            arr[1] = 5;
            arr[2] = 6;
        """

        val output = """
            arr:INT[] = [1, 2, 3]; => [1, 2, 3]
            arr[0] = 4 => 4
            arr[1] = 5 => 5
            arr[2] = 6 => 6
            environment:
            arr:INT[] = [4, 5, 6]
        """

        verify(input, output)
    }

    @Test
    fun `should check type when array be assigned with index`() {
        val input = """
            arr:INT[] = [1, 2, 3];
            arr[0] = 4.0;
        """

        when (val r = parser().parse(input.byteInputStream())) {
            is ParseResult.Failure -> {
                r.errors.forEach { println(it) }
                fail(r.errors.first())
            }
            is ParseResult.Success -> {
                val program = r.value
                val e = shouldThrow<InterpretException> { Interpreter().interpret(program).joinToString() }
                e shouldHaveMessage "type mismatch: expected INT, got FLOAT"
            }
        }
    }

    @Test
    fun `should check bounds when array be assigned with index`() {
        val input = """
            arr:INT[] = [1, 2, 3];
            arr[3] = 4;
        """

        when (val r = parser().parse(input.byteInputStream())) {
            is ParseResult.Failure -> {
                r.errors.forEach { println(it) }
                fail(r.errors.first())
            }
            is ParseResult.Success -> {
                val program = r.value
                val e = shouldThrow<InterpretException> { Interpreter().interpret(program).joinToString() }
                e shouldHaveMessage "index 3 out of bounds for array of size 3"
            }
        }
    }

    @Test
    fun `support 2D array`() {
        val input = """
            arr:INT[][] = [[1, 2, 3], [4, 5, 6]];
            arr[0][0] = 7;
            arr[0][1];
        """

        val output = """
            arr:INT[][] = [[1, 2, 3], [4, 5, 6]]; => [[1, 2, 3], [4, 5, 6]]
            arr[0][0] = 7 => 7
            arr[0][1]; => 2
            environment:
            arr:INT[][] = [[7, 2, 3], [4, 5, 6]]
        """

        verify(input, output)
    }

    @Test
    fun `array index from the return of function`() {
        val input = """
            func f():INT {
                return 1;
            }
            arr:INT[] = [1, 2, 3];
            a:INT = arr[f()];
        """

        val output = """
            func f():INT { return 1; } => void
            arr:INT[] = [1, 2, 3]; => [1, 2, 3]
            a:INT = arr[f()]; => 2
            environment:
            arr:INT[] = [1, 2, 3], a:INT = 2
            f():INT
        """

        verify(input, output)
    }

    @Test
    fun `index array which is a return of a function`() {
        val input = """
            func f():INT[] {
                return [1, 2, 3];
            }
            a:INT = f()[1];
        """

        val output = """
            func f():INT[] { return [1, 2, 3]; } => void
            a:INT = f()[1]; => 2
            environment:
            a:INT = 2
            f():INT[]
        """

        verify(input, output)
    }
}
