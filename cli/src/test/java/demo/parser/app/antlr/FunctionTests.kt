package demo.parser.app.antlr

import demo.parser.domain.ParseResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.throwable.shouldHaveMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import demo.parser.app.antlr.AntlrInterpretVerifier.parser
import demo.parser.app.antlr.AntlrInterpretVerifier.verify
import demo.parser.domain.Statement
import demo.parser.domain.BuiltinType
import demo.parser.interpret.InterpretException
import demo.parser.interpret.Interpreter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class FunctionTests {

    @Test
    fun `function call test`() {
        val input = """
            func f(x:INT):INT {
                return x + 1;
            }
            func g(x:INT):INT {
                return x * 2;
            }
            f(g(2));
        """

        val output = """
            func f(x:INT):INT { return x + 1; } => void
            func g(x:INT):INT { return x * 2; } => void
            f(g(2)); => 5
            environment:
            f(x:INT):INT, g(x:INT):INT
        """
        verify(input, output)
    }

    @Test
    fun `function call test with multiple arguments`() {
        val input = """
            func f(x:INT, y:INT):INT {
                return x + y;
            }
            f(1, 2);
        """
        val output = """
                    func f(x:INT, y:INT):INT { return x + y; } => void
                    f(1, 2); => 3
                    environment:
                    f(x:INT, y:INT):INT
                """.trimIndent()
        verify(input, output)
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
            f(g(2));
        """
        val output = """
            x:INT = 1; => 1
            func f(x:INT):INT { return x + 1; } => void
            func g(x:INT):INT { return x * 2; } => void
            f(g(2)); => 5
            environment:
            x:INT = 1
            f(x:INT):INT, g(x:INT):INT
        """.trimIndent()
        verify(input, output)
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
            f(5);
        """
        val output = """
            func f(x:INT):INT { if (x == 0) { return 1; } else { return x * f(x - 1); } } => void
            f(5); => 120
            environment:
            f(x:INT):INT
        """
        verify(input, output)
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
            f(2);
        """
        val output = """
            func f(x:INT):INT { return g(x + 1); } => void
            func g(x:INT):INT { return x * 2; } => void
            f(2); => 6
            environment:
            f(x:INT):INT, g(x:INT):INT
        """
        verify(input, output)
    }

    @Test
    fun `function can operate string`() {
        val input = """
            func f(x:STRING):STRING {
                return x + " world";
            }
            f("hello");
        """
        val output = """
            func f(x:STRING):STRING { return x + " world"; } => void
            f("hello"); => "hello world"
            environment:
            f(x:STRING):STRING
        """
        verify(input, output)
    }

    @Test
    fun `can't declare functions with same name`() {
        val input = """
            func f(x:INT):INT {
                return x + 1;
            }
            func f(x:INT):INT {
                return x + 1;
            }
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 1
                r.errors.first().shouldHaveMessage("semantic error at (5:17): function f already defined")
            }
            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

    @Test
    fun `can't have more than 2 parameter with the same name in function declaration`() {
        val input = """
            func f(x:INT, x:INT):INT {
                return x + 1;
            }
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 1
                r.errors.first().shouldHaveMessage("semantic error at (2:26): variable x already defined")
            }
            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

    @Test
    fun `can't define variables with same name in function`() {
        val input = """
            func f(x:INT):INT {
                x:INT = 1;
                x:INT = 2;
                return x + 1;
            }
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 1
                r.errors.first().shouldHaveMessage("semantic error at (4:16): variable x already defined")
            }

            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

    @Test
    fun `can't use function as variable`() {
        val input = """
            func f(x:INT):INT {
                return x + 1;
            }
            f = 1;
            f;
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 2
                r.errors[0].shouldHaveMessage("semantic error at (5:12): f is not a variable")
                r.errors[1].shouldHaveMessage("semantic error at (6:12): f is not a variable")
            }

            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

    @Test
    fun `treat empty return type as VOID`() {
        val input = """
            func f(x:INT) {
                x + 1;
            }
        """.byteInputStream()

        when (val r = parser().parse(input)) {
            is ParseResult.Failure -> {
                fail(r.errors[0].message)
            }

            is ParseResult.Success -> {
                val program = r.value
                program.statements shouldHaveSize 1
                val funcDecl = program.statements[0].shouldBeInstanceOf<Statement.FunctionDeclaration>()
                funcDecl.returnType shouldBe BuiltinType.VOID
            }
        }
    }

    @Test
    fun `check type of function's return value`() {
        val input = """
            func f():INT {
                1+2;
            }
            i:INT = f();
        """.trimIndent()

        val output = """
            func f():INT { 1 + 2; } => void
            i:INT = f(); failed => type mismatch: expected INT, got VOID
            environment:
            f():INT
        """.trimIndent()

        verify(input, output)
    }

    @Test
    fun `insert sort test`() {
        val input = """
            func insertionSort(arr:INT[], n:INT):INT[] {
                for(i:INT = 1; i < n; i++) {
                    key:INT = arr[i];
                    j:INT = i - 1;

                    /* Move elements of arr[0..i-1], that are
                       greater than key, to one position ahead
                       of their current position */
                    while (j >= 0 && arr[j] > key) {
                        arr[j+1] = arr[j];
                        j = j - 1;
                    }
                    arr[j+1] = key;
                }
                return arr;
            }

            insertionSort([2,3,9,1,11,32,17,23,15,21], 10);
        """.trimIndent()

        val output = """
            func insertionSort(arr:INT[], n:INT):INT[] { for (i:INT = 1; i < n; i++;) { key:INT = arr[i]; j:INT = i - 1; while (j >= 0 && arr[j] > key) { arr[j + 1] = arr[j] j = j - 1; } arr[j + 1] = key } return arr; } => void
            insertionSort([2, 3, 9, 1, 11, 32, 17, 23, 15, 21], 10); => [1, 2, 3, 9, 11, 15, 17, 21, 23, 32]
            environment:
            insertionSort(arr:INT[], n:INT):INT[]
        """.trimIndent()

        verify(input, output)
    }
}
