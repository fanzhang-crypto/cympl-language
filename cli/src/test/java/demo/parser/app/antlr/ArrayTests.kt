package demo.parser.app.antlr

import org.junit.jupiter.api.Test
import demo.parser.app.antlr.AntlrInterpretVerifier.verify

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
    fun `should check bounds when array be assigned with index`() {
        val input = """
            arr:INT[] = [1, 2, 3];
            arr[3] = 4;
        """.trimIndent()

        val output = """
            arr:INT[] = [1, 2, 3]; => [1, 2, 3]
            arr[3] = 4 failed => index 3 out of bounds for array of size 3
            environment:
            arr:INT[] = [1, 2, 3]
        """.trimIndent()

        verify(input, output)
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

    @Test
    fun `can access length property of an array`() {
        val input = """
            arr:INT[] = [1, 2, 3];
            a:INT = arr.length;
        """

        val output = """
            arr:INT[] = [1, 2, 3]; => [1, 2, 3]
            a:INT = arr.length; => 3
            environment:
            arr:INT[] = [1, 2, 3], a:INT = 3
        """

        verify(input, output)
    }

    @Test
    fun `can access length properties in an 2D array`() {
        val input = """
            arr:INT[][] = [[1, 2, 3], [4, 5]];
            arr.length;
            arr[0].length;
            arr[1].length;
        """

        val output = """
            arr:INT[][] = [[1, 2, 3], [4, 5]]; => [[1, 2, 3], [4, 5]]
            arr.length; => 2
            arr[0].length; => 3
            arr[1].length; => 2
            environment:
            arr:INT[][] = [[1, 2, 3], [4, 5]]
        """

        verify(input, output)

    }
}
