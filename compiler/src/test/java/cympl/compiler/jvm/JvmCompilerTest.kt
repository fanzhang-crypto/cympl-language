package cympl.compiler.jvm

import cympl.parser.antlr.AntlrProgramParser
import cympl.parser.ParseResult
import cympl.language.Program
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File
import java.io.PrintWriter

class JvmCompilerTest {

    private val parser = AntlrProgramParser()
    private val compiler = JvmCompiler()

    @Test
    fun `support variable declaration`() {
        val input = """
            int a = 3;
            String b = "Hello";
            bool c = true;
            float d = 3.14 * a;
            int[] e = [1,2,-a];
            int[][] f = [[1,2,3],[4,5,6]];
            
            println(a);
            println(b);
            println(c);
            println(d);
            println(e);
            println(f);
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            3
            Hello
            true
            9.42
            [1, 2, -3]
            [[1, 2, 3], [4, 5, 6]]
        """.trimIndent()
    }

    @Test
    fun `support arithmetic expression`() {
        val input = """
            int a = 3;
            int b = 4;
            int c = 5;
            int d = 6;
            int e = 7;
            int f = 8;
            int g = 9;
            int h = 10;
            int i = 11;
            int j = 12;
            
            println(a ^ b);
            println(a + b);
            println(c - d);
            println(e * f);
            println(g / h);
            println(i % j);
            println((a + b) * c);
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            81.0
            7
            -1
            56
            0
            11
            35
        """.trimIndent()
    }

    @Test
    fun `support array indexing`() {
        val input = """
            int[] a = [1,2,3];
            int[][] b = [[1,2,3],[4,5,6]];
            String[] c = ["Hello", "World", "!"];
            println(a[0]);
            println(a[1]);
            println(a[2]);
            println(b[0][0]);
            println(b[0][1]);
            println(b[0][2]);
            println(b[1][0]);
            println(b[1][1]);
            println(b[1][2]);
            println(c[0]);
            println(c[1]);
            println(c[2]);
            println(c[0] + c[1] + c[2]);
            println(c.length);
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            1
            2
            3
            1
            2
            3
            4
            5
            6
            Hello
            World
            !
            HelloWorld!
            3
        """.trimIndent()
    }

    @Test
    fun `support string concatenation`() {
        val input = """
            String a = "Hello";
            String b = "World";
            String c = "!";
            
            println(a + b + c);
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            HelloWorld!
        """.trimIndent()
    }

    @Test
    fun `support self increment and decrement`() {
        val input = """
            int a = 3;
            int b = 4;
            float c = 3.14;
            
            println(a++);
            println(++a);
            println(b--);
            println(--b);
            println(c--);
            println(--c);
            println(c++);
            println(++c);
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            3
            5
            4
            2
            3.14
            1.1400000000000001
            1.1400000000000001
            3.14
        """.trimIndent()
    }

    @Test
    fun `support comparison of int float bool and string`() {
        val input = """
            int a = 3;
            int b = 4;
            float c = 3.14;
            float d = 4.14;
            bool e = true;
            bool f = false;
            String g = "Hello";
            String h = "World";
            
            println(a == b);
            println(a != b);
            println(a < b);
            println(a <= b);
            println(a > b);
            println(a >= b);
            println(c == d);
            println(c != d);
            println(c < d);
            println(c <= d);
            println(c > d);
            println(c >= d);
            println(e == f);
            println(e != f);
            println(e < f);
            println(e <= f);
            println(e > f);
            println(e >= f);
            println(g == h);
            println(g != h);
            println(g < h);
            println(g <= h);
            println(g > h);
            println(g >= h);
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            false
            true
            true
            true
            false
            false
            false
            true
            true
            true
            false
            false
            false
            true
            false
            false
            true
            true
            false
            true
            true
            true
            false
            false
        """.trimIndent()
    }

    @Test
    fun `support logical expression`() {
        val input = """
            bool a = true;
            bool b = false;
            
            println(a && b);
            println(a || b);
            println(!a && !b);
            println(!a || !b);
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            false
            true
            false
            true
        """.trimIndent()
    }

    @Test
    fun `support if else statement`() {
        val input = """
            int a = 3;
            int b = 4;
            int c = 5;
            int d = 6;
            int e = 7;
            int f = 8;
            int g = 9;
            int h = 10;
            int i = 11;
            int j = 12;
            
            if (a == b) {
                println("a == b");
            } else if (a == c) {
                println("a == c");
            } else if (a == d) {
                println("a == d");
            } else if (a == e) {
                println("a == e");
            } else if (a == f) {
                println("a == f");
            } else if (a == g) {
                println("a == g");
            } else if (a == h) {
                println("a == h");
            } else if (a == i) {
                println("a == i");
            } else if (a == j) {
                println("a == j");
            } else {
                println("a != j");
            }
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            a != j
        """.trimIndent()
    }

    @Test
    fun `support while loop`() {
        val input = """
            float a = 0.0;
            while (a < 10) {
                println(a);
                a++;
            }
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            0.0
            1.0
            2.0
            3.0
            4.0
            5.0
            6.0
            7.0
            8.0
            9.0
        """.trimIndent()
    }

    @Test
    fun `support while loop with break`() {
        val input = """
            int a = 0;
            while (a < 10) {
                println(a);
                a++;
                if (a == 7) {
                    break;
                }
            }
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            0
            1
            2
            3
            4
            5
            6
        """.trimIndent()
    }

    @Test
    fun `support while loop with continue`() {
        val input = """
            int a = 0;
            while (a < 10) {
                a++;
                if (a == 10) {
                    continue;
                }
                println(a);
            }
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            1
            2
            3
            4
            5
            6
            7
            8
            9
        """.trimIndent()
    }

    @Test
    fun `support break in a nest loop`() {
        val input = """
            int a = 0;
            while (a < 5) {
                a++;
                int b = 0;
                while (b < 10) {
                    b++;
                    if (b == 7) {
                        break;
                    }
                    println(b);
                }
                if (a == 3) {
                    break;
                }
            }
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            1
            2
            3
            4
            5
            6
            1
            2
            3
            4
            5
            6
            1
            2
            3
            4
            5
            6
        """.trimIndent()
    }

    @Test
    fun `support for loop`() {
        val input = """
            for (int a = 0; a < 10; a++) {
                println(a);
            }
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            0
            1
            2
            3
            4
            5
            6
            7
            8
            9
        """.trimIndent()
    }

    @Test
    fun `support break and continue in a for loop`() {
        val input = """
            for (int a = 0; a < 10; a++) {
                if (a == 7) {
                    continue;
                }
                if (a == 9) {
                    break;
                }
                println(a);
            }
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            0
            1
            2
            3
            4
            5
            6
            8
        """.trimIndent()
    }

    @Test
    fun `support 1D array element assignment`() {
        val input = """
            int[] a = [1, 2, 3, 4, 5];
            a[0] = 10;
            a[1] = 20;
            a[2] = 30;
            a[3] = 40;
            a[4] = 50;
            println(a[0]);
            println(a[1]);
            println(a[2]);
            println(a[3]);
            println(a[4]);
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            10
            20
            30
            40
            50
        """.trimIndent()
    }

    @Test
    fun `support 2D array element assignment`() {
        val input = """
            int[][] a = [[1, 2, 3], [4, 5, 6], [7, 8, 9]];
            a[0][0] = 10;
            a[0][1] = 20;
            a[0][2] = 30;
            a[1][0] = 40;
            a[1][1] = 50;
            a[1][2] = 60;
            a[2][0] = 70;
            a[2][1] = 80;
            a[2][2] = 90;
            println(a[0][0]);
            println(a[0][1]);
            println(a[0][2]);
            println(a[1][0]);
            println(a[1][1]);
            println(a[1][2]);
            println(a[2][0]);
            println(a[2][1]);
            println(a[2][2]);
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            10
            20
            30
            40
            50
            60
            70
            80
            90
        """.trimIndent()
    }

    @Test
    fun `support new array`() {
        val input = """
            int[] a = new int[5];
            a[0] = 10;
            a[1] = 20;
            a[2] = 30;
            a[3] = 40;
            a[4] = 50;
            println(a[0]);
            println(a[1]);
            println(a[2]);
            println(a[3]);
            println(a[4]);
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            10
            20
            30
            40
            50
        """.trimIndent()
    }

    @Test
    fun `support new 2d array`() {
        val input = """
            int[][] a = new int[3][3];
            a[0][0] = 10;
            a[0][1] = 20;
            a[0][2] = 30;
            a[1][0] = 40;
            a[1][1] = 50;
            a[1][2] = 60;
            a[2][0] = 70;
            a[2][1] = 80;
            a[2][2] = 90;
            println(a[0][0]);
            println(a[0][1]);
            println(a[0][2]);
            println(a[1][0]);
            println(a[1][1]);
            println(a[1][2]);
            println(a[2][0]);
            println(a[2][1]);
            println(a[2][2]);
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            10
            20
            30
            40
            50
            60
            70
            80
            90
        """.trimIndent()
    }

    @Test
    fun `support function declaration`() {
        val input = """
            int add(int a, int b) {
                return a + b;
            }
            int sub(int a, int b) {
                return a - b;
            }
            println(add(1, 2));
            println(sub(1, 2));
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            3
            -1
        """.trimIndent()
    }

    @Test
    fun `functions can manipulate global variables`() {
        val input = """
            int a = 0;
            int add(int b) {
                a = a + b;
                return a;
            }
            println(add(1));
            println(add(2));
            println(add(3));
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            1
            3
            6
        """.trimIndent()
    }

    @Test
    fun `variable can be shadowed in function decl`() {
        val input = """
            int x = 1;
            int f(int x) {
                return x + 1;
            }
            int g(int x) {
                return x * 2;
            }
            println(f(g(2)));
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            5
        """.trimIndent()
    }

    @Test
    fun `function can call itself`() {
        val input = """
            int f(int x) {
                if (x == 0) {
                    return 1;
                } else {
                    return x * f(x - 1);
                }
            }
            println(f(5));
        """.trimIndent()
        val output = compileAndExecute(input)

        output shouldBe """
            120
        """.trimIndent()
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
            println(f(2));
        """
        val output = compileAndExecute(input)

        output shouldBe """
            6
        """.trimIndent()
    }

    @Test
    fun `function can operate string`() {
        val input = """
            String f(String x) {
                return x + " world";
            }
            println(f("hello"));
        """
        val output = compileAndExecute(input)

        output shouldBe """
            hello world
        """.trimIndent()
    }

    @Test
    fun `insert sort test`() {
        val input = """
            int[] insertionSort(int[] arr) {
                for(int i = 1; i < arr.length; i++) {
                    int key = arr[i];
                    int j = i - 1;

                    while (j >= 0 && arr[j] > key) {
                        arr[j+1] = arr[j];
                        j = j - 1;
                    }
                    arr[j+1] = key;
                }
                return arr;
            }

            println(insertionSort([2,3,9,1,11,32,17,23,15,21]));
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            [1, 2, 3, 9, 11, 15, 17, 21, 23, 32]
        """.trimIndent()
    }

    @Test
    fun `can return early in a loop in function`() {
        val input = """
            int f(int x) {
                while (x < 10) {
                    x++;
                    if (x == 5) {
                        return x;
                    }
                }
                return x;
            }
            println(f(1));
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            5
        """.trimIndent()
    }

    @Test
    fun `support switch case`() {
        val input = """
            String f(int x) {
                String s = "";
                switch (x) {
                    case 1:
                        s = "1";
                        break;
                    case 2:
                        s = "2";
                        break;
                    case 3:
                        s = "3";
                        break;
                    default:
                        s = "0";
                }
                return s;
            }
            println(f(1));
            println(f(2));
            println(f(3));
            println(f(4));
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            1
            2
            3
            0
        """.trimIndent()
    }

    @Test
    fun `support switch case with fall through`() {
        val input = """
            String f(int x) {
                String s = "";
                switch (x) {
                    case 1:
                        s = "1";
                    case 2:
                        s = "2";
                    case 3:
                        s = "3";
                        break;
                    default:
                        s = "0";
                }
                return s;
            }
            println(f(1));
            println(f(2));
            println(f(3));
            println(f(4));
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            3
            3
            3
            0
        """.trimIndent()
    }

    @Test
    fun `support return from switch case statement`() {
        val input = """
            String f(int x) {
                switch (x) {
                    case 1: return "1";
                    case 2: return "2";
                    case 3: return "3";
                    default: return "0";
                }
            }
            println(f(1));
            println(f(2));
            println(f(3));
            println(f(4));
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            1
            2
            3
            0
        """.trimIndent()
    }

    @Test
    fun `readln test`() {
        val input = """
            String s = readln("Enter a string: ");
            println(s);
        """.trimIndent()

        val output = compileAndExecute(input, "hello world")

        output shouldBe """
            Enter a string: hello world
        """.trimIndent()
    }

    @Test
    fun `support declaring variable as lambda`() {
        val input = """
            () -> String hello = () -> "hello";
            (int) -> int add1 = (x) -> x + 1;
            (int) -> int sub1 = (x) -> x - 1;
            (int, int) -> int add = (x, y) -> x + y;
            (String) -> void greeting = (name) -> println("hello " + name);
            
            println(hello());
            println(add1(2));
            println(sub1(2));
            println(add(2, 3));
            greeting("world");
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            hello
            3
            1
            5
            hello world
        """.trimIndent()
    }

    @Test
    fun `lambda can be return from a function call`() {
        val input = """
            int z = 3;
            (int) -> int f(int x) {
                return (y) -> x + y + z;
            }
            (int) -> int add1 = f(1);
            println(add1(2));
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            6
        """.trimIndent()
    }

    @Test
    fun `support high order function chaining`() {
        val input = """
            (int) -> (int) -> (int) -> int f = (x) -> (y) -> (z) -> x + y + z;
            println(f(1)(2)(3));
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            6
        """.trimIndent()
    }

    @Test
    fun `lambda can be passed as argument`() {
        val input = """
            int z = 3;
            void f((int) -> int g) {
                println(g(2));
            }
            f((y) -> 1 + y + z);
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            6
        """.trimIndent()
    }

    @Test
    fun `lambda that return an array of integers`() {
        val input = """
            (int) -> int[] f() {
                return (x) -> [x, x + 1, x + 2];
            }
            (int) -> int[] ff = f();
            int[] arr = ff(1);
            println(arr[0]);
            println(arr[1]);
            println(arr[2]);
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            1
            2
            3
        """.trimIndent()
    }

    @Test
    fun `lambda array test`() {
        val input = """
            (() -> int)[] a = new () -> int[12];
        
            for (int i = 0; i < 10; i++) {
                a[i] = () -> i;
            }
            
            println(a[6]());
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            6
        """.trimIndent()
    }

    @Test
    fun `fibonacci test`() {
        val input = """
            () -> int fibonacci() {
                int a = 0;
                int b = 1;
                return () -> {
                    int c = a;
                    a = b;
                    b = a + c;
                    return c;
                };
            }

            () -> int fibo = fibonacci();

            for (int i = 0; i < 10; i++) {
                println(fibo());
            }
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            0
            1
            1
            2
            3
            5
            8
            13
            21
            34
        """.trimIndent()
    }

    @Test
    fun `quick sort test`() {
        val input = """
            int[] quickSort(int[] arr) {
                void swap(int[] arr, int i, int j)
                {
                    int temp = arr[i];
                    arr[i] = arr[j];
                    arr[j] = temp;
                }
            
                int partition(int[] arr, int low, int high)
                {
                    int pivot = arr[high]; // pivot
                    int i = low - 1;
                    for (int j = low; j <= high - 1; j++) {
                        if (arr[j] < pivot) {
                            i = i + 1;
                            swap(arr, i, j);
                        }
                    }
                    swap(arr, i + 1, high);
                    return i + 1;
                }
            
                void quickSortInternal(int[] arr, int low, int high) {
                    if (low < high) {
                        int pi = partition(arr, low, high);
                        quickSortInternal(arr, low, pi - 1);
                        quickSortInternal(arr, pi + 1, high);
                    }
                }
            
                quickSortInternal(arr, 0, arr.length - 1);
                return arr;
            }

            println(quickSort([2,3,9,1,11,32,17,23,15,21]));

        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            [1, 2, 3, 9, 11, 15, 17, 21, 23, 32]
        """.trimIndent()
    }

    @Test
    fun `support function that has only varargs`() {
        val input = """
            void f(int... args) {
                for (int i = 0; i < args.length; i++) {
                    println(args[i]);
                }
            }
            f(1, 2, 3, 4, 5);
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            1
            2
            3
            4
            5
        """.trimIndent()
    }

    @Test
    fun `support function having both fix and variable arguments`() {
        val input = """
            String join(String sep, String... parts) {
                String s = "";
                for(int i = 0; i < parts.length; i++) {
                    s = s + parts[i];
                    if (i < parts.length - 1) {
                        s = s + sep;
                    }
                }
                return s;
            }
            println(join(",", "a", "b", "c"));
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            a,b,c
        """.trimIndent()
    }

    @Test
    fun `lambda calculus test`() {
        val input = """
            ((int) -> int) -> (int) -> int one = (f) -> (x) -> f(x);
            one((x) -> {
                println("1");
                return 0;
            })(0);
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            1
        """.trimIndent()
    }

    @Disabled("Not supported yet")
    @Test
    fun `support lambda that has variable arguments`() {
        val input = """
            (String, String...) -> String join = (sep, parts) -> {
                String s = "";
                for(int i = 0; i < parts.length; i++) {
                    s = s + parts[i];
                    if (i < parts.length - 1) {
                        s = s + sep;
                    }
                }
                return s;
            }
            println(join(",", "a", "b", "c"));
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            a,b,c
        """.trimIndent()
    }

    private val compileOptions = JvmCompileOptions(
        mainClassName = DEFAULT_MAIN_CLASS_NAME,
    )

    private fun compileAndExecute(script: String, input: String? = null): String {
        val program = parse(script)

        compiler.compile(program, compileOptions).forEach { (name, bytecode) ->
            if (name.contains(".")) {
                val classFilePath = name.replace(".", "/")
                val classFileDir = classFilePath.substringBeforeLast("/")
                File("build/classes/$classFileDir").mkdirs()
                File("build/classes/$classFilePath.class").writeBytes(bytecode)
            } else {
                File("build/classes/$name.class").writeBytes(bytecode)
            }
        }

        return Runtime.getRuntime()
            .exec("java -cp build/classes/cympl/runtime -cp build/classes  $DEFAULT_MAIN_CLASS_NAME").let {
                if (input != null) {
                    PrintWriter(it.outputStream).use { writer ->
                        writer.println(input)
                        writer.flush()
                    }
                }

                it.waitFor()

                val errorLines = it.errorLines()
                if (errorLines.isNotEmpty()) {
                    errorLines.forEach { System.err.println(it) }
                }
                errorLines.shouldBeEmpty()
                it.outputLines().joinToString("\n")
            }
    }

    private fun parse(input: String): Program {
        return when (val r = parser.parse(input.byteInputStream())) {
            is ParseResult.Failure -> {
                r.errors.forEach { println(it) }
                fail(r.errors.first())
            }

            is ParseResult.Success -> {
                r.value
            }
        }
    }

    private fun Process.errorLines(): List<String> = errorStream.bufferedReader().lines().toList()
    private fun Process.outputLines(): List<String> = inputStream.bufferedReader().lines().toList()

    companion object {
        private const val DEFAULT_MAIN_CLASS_NAME = "JvmTest"
    }
}
