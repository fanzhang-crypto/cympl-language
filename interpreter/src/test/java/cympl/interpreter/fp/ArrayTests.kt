package cympl.interpreter.fp

import org.junit.jupiter.api.Test
import cympl.interpreter.fp.FpInterpretVerifier.verify
import org.junit.jupiter.api.Disabled

class ArrayTests {

    @Test
    fun `array can be declared`() {
        val input = """
            int[] arr = [1, 2, 3];
        """

        val output = """
            int[] arr = [1, 2, 3]; => [1, 2, 3]
            environment:
            arr:int[] = [1, 2, 3]
        """

        verify(input, output)
    }

    @Test
    fun `array can be declared as empty`() {
        val input = """
            int[] arr = [];
        """

        val output = """
            int[] arr = []; => []
            environment:
            arr:int[] = []
        """

        verify(input, output)
    }

    @Test
    fun `array can be returned from a function call`() {
        val input = """
            int[] f() {
                return [1, 2, 3];
            }
            int[] arr = f();
        """

        val output = """
            func f():int[] { return [1, 2, 3]; } => Closure(#f)
            int[] arr = f(); => [1, 2, 3]
            environment:
            arr:int[] = [1, 2, 3]
            f: () -> int[]
        """

        verify(input, output)
    }

    @Test
    fun `array can be passed through function call`(){
        val input = """
            int[] f(int[] arr) {
                return arr;
            }
            int[] arr = f([1, 2, 3]);
        """

        val output = """
            func f(int[] arr):int[] { return arr; } => Closure(#f)
            int[] arr = f([1, 2, 3]); => [1, 2, 3]
            environment:
            arr:int[] = [1, 2, 3]
            f: (int[]) -> int[]
        """

        verify(input, output)
    }

    @Test
    fun `array can be indexed`() {
        val input = """
            int[] arr = [1, 2, 3];
            int a = arr[0];
            int b = arr[1];
            int c = arr[2];
        """

        val output = """
            int[] arr = [1, 2, 3]; => [1, 2, 3]
            int a = arr[0]; => 1
            int b = arr[1]; => 2
            int c = arr[2]; => 3
            environment:
            arr:int[] = [1, 2, 3], a:int = 1, b:int = 2, c:int = 3
        """

        verify(input, output)
    }

    @Test
    fun `array can be assigned with index`() {
        val input = """
            int[] arr = [1, 2, 3];
            arr[0] = 4;
            arr[1] = 5;
            arr[2] = 6;
        """

        val output = """
            int[] arr = [1, 2, 3]; => [1, 2, 3]
            arr[0] = 4; => 4
            arr[1] = 5; => 5
            arr[2] = 6; => 6
            environment:
            arr:int[] = [4, 5, 6]
        """

        verify(input, output)
    }

    @Test
    fun `should check bounds when array be assigned with index`() {
        val input = """
            int[] arr = [1, 2, 3];
            arr[3] = 4;
        """.trimIndent()

        val output = """
            int[] arr = [1, 2, 3]; => [1, 2, 3]
            arr[3] = 4; failed => index 3 out of bounds for array of size 3
            environment:
            arr:int[] = [1, 2, 3]
        """.trimIndent()

        verify(input, output)
    }

    @Test
    fun `support 2D array`() {
        val input = """
            int[][] arr = [[1, 2, 3], [4, 5, 6]];
            arr[0][0] = 7;
            arr[0][1];
        """

        val output = """
            int[][] arr = [[1, 2, 3], [4, 5, 6]]; => [[1, 2, 3], [4, 5, 6]]
            arr[0][0] = 7; => 7
            arr[0][1]; => 2
            environment:
            arr:int[][] = [[7, 2, 3], [4, 5, 6]]
        """

        verify(input, output)
    }

    @Test
    fun `array index from the return of function`() {
        val input = """
            int f() {
                return 1;
            }
            int[] arr = [1, 2, 3];
            int a = arr[f()];
        """

        val output = """
            func f():int { return 1; } => Closure(#f)
            int[] arr = [1, 2, 3]; => [1, 2, 3]
            int a = arr[f()]; => 2
            environment:
            arr:int[] = [1, 2, 3], a:int = 2
            f: () -> int
        """

        verify(input, output)
    }

    @Disabled("Not implemented yet")
    @Test
    fun `index array which is a return of a function`() {
        val input = """
            int[] f() {
                return [1, 2, 3];
            }
            int a = f()[1];
        """

        val output = """
            func f():int[] { return [1, 2, 3]; } => Closure(#f)
            int a = f()[1]; => 2
            environment:
            int a = 2
            f():int[]
        """

        verify(input, output)
    }

    @Test
    fun `can access length property of an array`() {
        val input = """
            int[] arr = [1, 2, 3];
            int a = arr.length;
        """

        val output = """
            int[] arr = [1, 2, 3]; => [1, 2, 3]
            int a = arr.length; => 3
            environment:
            arr:int[] = [1, 2, 3], a:int = 3
        """

        verify(input, output)
    }

    @Test
    fun `can access length properties in an 2D array`() {
        val input = """
            int[][] arr = [[1, 2, 3], [4, 5]];
            arr.length;
            arr[0].length;
            arr[1].length;
        """

        val output = """
            int[][] arr = [[1, 2, 3], [4, 5]]; => [[1, 2, 3], [4, 5]]
            arr.length; => 2
            arr[0].length; => 3
            arr[1].length; => 2
            environment:
            arr:int[][] = [[1, 2, 3], [4, 5]]
        """

        verify(input, output)

    }
}
