package demo.parser.compile.jvm

import demo.parser.antlr.AntlrProgramParser
import demo.parser.domain.ParseResult
import demo.parser.domain.Program
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File

class JvmCompilerTest {

    private val parser = AntlrProgramParser()
    private val compiler = JvmCompiler()

    @Test
    fun `support variable declaration`() {
        val input = """
            a:INT = 3;
            b:STRING = "Hello";
            c:BOOL = true;
            d:FLOAT = 3.14 * a;
            e:INT[] = [1,2,-a];
            f:INT[][] = [[1,2,3],[4,5,6]];
            
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
            a:INT = 3;
            b:INT = 4;
            c:INT = 5;
            d:INT = 6;
            e:INT = 7;
            f:INT = 8;
            g:INT = 9;
            h:INT = 10;
            i:INT = 11;
            j:INT = 12;
            
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
            a:INT[] = [1,2,3];
            b:INT[][] = [[1,2,3],[4,5,6]];
            c:STRING[] = ["Hello", "World", "!"];
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
            a:STRING = "Hello";
            b:STRING = "World";
            c:STRING = "!";
            
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
            a:INT = 3;
            b:INT = 4;
            c:FLOAT = 3.14;
            
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
            a:INT = 3;
            b:INT = 4;
            c:FLOAT = 3.14;
            d:FLOAT = 4.14;
            e:BOOL = true;
            f:BOOL = false;
            g:STRING = "Hello";
            h:STRING = "World";
            
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
            a:BOOL = true;
            b:BOOL = false;
            
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
            a:INT = 3;
            b:INT = 4;
            c:INT = 5;
            d:INT = 6;
            e:INT = 7;
            f:INT = 8;
            g:INT = 9;
            h:INT = 10;
            i:INT = 11;
            j:INT = 12;
            
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
            a:FLOAT = 0.0;
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
            a:INT = 0;
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
            a:INT = 0;
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
            a:INT = 0;
            while (a < 5) {
                a++;
                b:INT = 0;
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
            for (a:INT = 0; a < 10; a++) {
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
            for (a:INT = 0; a < 10; a++) {
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
            a:INT[] = [1, 2, 3, 4, 5];
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
            a:INT[][] = [[1, 2, 3], [4, 5, 6], [7, 8, 9]];
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
    fun `variables in different blocks have no conflict`() {
        val input = """
            a:INT = 0;
            {
                if (true) {
                    a = 3;
                }
                a:INT = 1;
                println(a);
                
                {
                    a:INT = 2;
                    println(a);
                }
            }
            println(a);
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
            1
            2
            3
        """.trimIndent()
    }

    @Test
    fun `support function declaration`() {
        val input = """
            func add(a:INT, b:INT):INT {
                return a + b;
            }
            func sub(a:INT, b:INT):INT {
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
            a:INT = 0;
            func add(b:INT):INT {
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
            x:INT = 1;
            func f(x:INT):INT {
                return x + 1;
            }
            func g(x:INT):INT {
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
            func f(x:INT):INT {
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
            func f(x:INT):INT {
                return g(x + 1);
            }
            func g(x:INT):INT {
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
            func f(x:STRING):STRING {
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
            func insertionSort(arr:INT[]):INT[] {
                for(i:INT = 1; i < arr.length; i++) {
                    key:INT = arr[i];
                    j:INT = i - 1;

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
            func f(x:INT):INT {
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
    fun `quick sort test`() {
        val input = """
            func quickSort(arr:INT[]):INT[] {
                func swap(arr:INT[], i:INT, j:INT)
                {
                    temp:INT = arr[i];
                    arr[i] = arr[j];
                    arr[j] = temp;
                }
    
                func partition(arr:INT[], low:INT, high:INT):INT
                {
                    pivot:INT = arr[high];
                    i:INT = low - 1;
                    for (j:INT = low; j <= high - 1; j++) {
                        if (arr[j] < pivot) {
                            i = i + 1;
                            swap(arr, i, j);
                        }
                    }
                    swap(arr, i + 1, high);
                    return i + 1;
                }
    
                func quickSortInternal(arr:INT[], low:INT, high:INT) {
                    if (low < high) {
                        pi:INT = partition(arr, low, high);
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

    private val compileOptions = JvmCompileOptions(
        mainClassName = DEFAULT_MAIN_CLASS_NAME,
    )

    private fun compileAndExecute(script: String): String {
        val program = parse(script)
        val bytecode = compiler.compile(program, compileOptions)

        File("${DEFAULT_MAIN_CLASS_NAME}.class").writeBytes(bytecode)

        return Runtime.getRuntime()
            .exec("java $DEFAULT_MAIN_CLASS_NAME").let {
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
        private const val DEFAULT_MAIN_CLASS_NAME = "MainTest"
    }
}
