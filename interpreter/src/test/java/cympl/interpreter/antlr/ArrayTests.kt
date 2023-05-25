package cympl.interpreter.antlr

import org.junit.jupiter.api.Test
import cympl.interpreter.antlr.AntlrInterpretVerifier.verify

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
    fun `array can be passed through function call`() {
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
            a:int = 2
            f: () -> int[]
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

    @Test
    fun `support new 1D array`() {
        val input = """
            int[] arr = new int[3];
            arr[0] = 1;
            arr[1] = 2;
            arr[2] = 3;
        """

        val output = """
            int[] arr = new int[3]; => [0, 0, 0]
            arr[0] = 1; => 1
            arr[1] = 2; => 2
            arr[2] = 3; => 3
            environment:
            arr:int[] = [1, 2, 3]
        """

        verify(input, output)
    }

    @Test
    fun `support new 2D array`() {
        val input = """
            int[][] arr = new int[2][3];
            arr[0][0] = 1;
            arr[0][1] = 2;
            arr[0][2] = 3;
            arr[1][0] = 4;
            arr[1][1] = 5;
            arr[1][2] = 6;
        """

        val output = """
            int[][] arr = new int[2][3]; => [[0, 0, 0], [0, 0, 0]]
            arr[0][0] = 1; => 1
            arr[0][1] = 2; => 2
            arr[0][2] = 3; => 3
            arr[1][0] = 4; => 4
            arr[1][1] = 5; => 5
            arr[1][2] = 6; => 6
            environment:
            arr:int[][] = [[1, 2, 3], [4, 5, 6]]
        """

        verify(input, output)
    }

    @Test
    fun `support new 3D array`() {
        val input = """
            int[][][] arr = new int[2][3][4];
            arr[0][0][0] = 1;
            arr[0][0][1] = 2;
            arr[0][0][2] = 3;
            arr[0][0][3] = 4;
            arr[0][1][0] = 5;
            arr[0][1][1] = 6;
            arr[0][1][2] = 7;
            arr[0][1][3] = 8;
            arr[0][2][0] = 9;
            arr[0][2][1] = 10;
            arr[0][2][2] = 11;
            arr[0][2][3] = 12;
            arr[1][0][0] = 13;
            arr[1][0][1] = 14;
            arr[1][0][2] = 15;
            arr[1][0][3] = 16;
            arr[1][1][0] = 17;
            arr[1][1][1] = 18;
            arr[1][1][2] = 19;
            arr[1][1][3] = 20;
            arr[1][2][0] = 21;
            arr[1][2][1] = 22;
            arr[1][2][2] = 23;
            arr[1][2][3] = 24;
        """

        val output = """
            int[][][] arr = new int[2][3][4]; => [[[0, 0, 0, 0], [0, 0, 0, 0], [0, 0, 0, 0]], [[0, 0, 0, 0], [0, 0, 0, 0], [0, 0, 0, 0]]]
            arr[0][0][0] = 1; => 1
            arr[0][0][1] = 2; => 2
            arr[0][0][2] = 3; => 3
            arr[0][0][3] = 4; => 4
            arr[0][1][0] = 5; => 5
            arr[0][1][1] = 6; => 6
            arr[0][1][2] = 7; => 7
            arr[0][1][3] = 8; => 8
            arr[0][2][0] = 9; => 9
            arr[0][2][1] = 10; => 10
            arr[0][2][2] = 11; => 11
            arr[0][2][3] = 12; => 12
            arr[1][0][0] = 13; => 13
            arr[1][0][1] = 14; => 14
            arr[1][0][2] = 15; => 15
            arr[1][0][3] = 16; => 16
            arr[1][1][0] = 17; => 17
            arr[1][1][1] = 18; => 18
            arr[1][1][2] = 19; => 19
            arr[1][1][3] = 20; => 20
            arr[1][2][0] = 21; => 21
            arr[1][2][1] = 22; => 22
            arr[1][2][2] = 23; => 23
            arr[1][2][3] = 24; => 24
            environment:
            arr:int[][][] = [[[1, 2, 3, 4], [5, 6, 7, 8], [9, 10, 11, 12]], [[13, 14, 15, 16], [17, 18, 19, 20], [21, 22, 23, 24]]]
        """

        verify(input, output)

    }
}
