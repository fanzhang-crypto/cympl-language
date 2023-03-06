package demo.parser.antlr

import demo.parser.domain.Interpreter
import demo.parser.domain.ParseResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class FunctionTests {

    private val parser = AntlrProgramParser()
    private val interpreter = Interpreter()

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
        """.byteInputStream()

        when (val r = parser.parse(input)) {
            is ParseResult.Failure -> {
                r.errors.forEach { println(it) }
                fail(r.errors.first())
            }

            is ParseResult.Success -> {
                val program = r.value
                val outputs = interpreter.interpret(program)
                outputs.joinToString("\n") shouldBe """
                    func f(x:INT):INT { return x + 1; } => void
                    func g(x:INT):INT { return x * 2; } => void
                    f(g(2)); => 5
                    environment:
                    f(x:INT):INT, g(x:INT):INT
                """.trimIndent()
            }
        }
    }

    @Test
    fun `function call test with multiple arguments`() {
        val input = """
            func f(x:INT, y:INT):INT {
                return x + y;
            }
            f(1, 2);
        """.byteInputStream()

        when (val r = parser.parse(input)) {
            is ParseResult.Failure -> {
                r.errors.forEach { println(it) }
                fail(r.errors.first())
            }

            is ParseResult.Success -> {
                val program = r.value
                val outputs = interpreter.interpret(program)
                outputs.joinToString("\n") shouldBe """
                    func f(x:INT, y:INT):INT { return x + y; } => void
                    f(1, 2); => 3
                    environment:
                    f(x:INT, y:INT):INT
                """.trimIndent()
            }
        }
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
        """.byteInputStream()

        when (val r = parser.parse(input)) {
            is ParseResult.Failure -> {
                r.errors.forEach { println(it) }
                fail(r.errors.first())
            }
            is ParseResult.Success -> {
                val program = r.value
                val outputs = interpreter.interpret(program)
                outputs.joinToString("\n") shouldBe """
                    x:INT = 1; => 1
                    func f(x:INT):INT { return x + 1; } => void
                    func g(x:INT):INT { return x * 2; } => void
                    f(g(2)); => 5
                    environment:
                    x:INT = 1
                    f(x:INT):INT, g(x:INT):INT
                """.trimIndent()
            }
        }
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
        """.byteInputStream()

        when (val r = parser.parse(input)) {
            is ParseResult.Failure -> {
                r.errors.forEach { println(it) }
                fail(r.errors.first())
            }
            is ParseResult.Success -> {
                val program = r.value
                val outputs = interpreter.interpret(program)
                outputs.joinToString("\n") shouldBe """
                    func f(x:INT):INT { if (x == 0) { return 1; } else { return x * f(x - 1); } } => void
                    f(5); => 120
                    environment:
                    f(x:INT):INT
                """.trimIndent()
            }
        }
    }

    @Test
    fun `functions can call each other`() {
        val input = """
            func g(x:INT):INT {
                return x * 2;
            }
            func f(x:INT):INT {
                return g(x + 1);
            }
            f(2);
        """.byteInputStream()

        when (val r = parser.parse(input)) {
            is ParseResult.Failure -> {
                r.errors.forEach { println(it) }
                fail(r.errors.first())
            }
            is ParseResult.Success -> {
                val program = r.value
                val outputs = interpreter.interpret(program)
                outputs.joinToString("\n") shouldBe """
                    func g(x:INT):INT { return x * 2; } => void
                    func f(x:INT):INT { return g(x + 1); } => void
                    f(2); => 6
                    environment:
                    g(x:INT):INT, f(x:INT):INT
                """.trimIndent()
            }
        }
    }

    @Test
    fun `function can operate string`() {
        val input = """
            func f(x:STRING):STRING {
                return x + " world";
            }
            f("hello");
        """.byteInputStream()

        when (val r = parser.parse(input)) {
            is ParseResult.Failure -> {
                r.errors.forEach { println(it) }
                fail(r.errors.first())
            }
            is ParseResult.Success -> {
                val program = r.value
                val outputs = interpreter.interpret(program)
                outputs.joinToString("\n") shouldBe """
                    func f(x:STRING):STRING { return x + " world"; } => void
                    f("hello"); => "hello world"
                    environment:
                    f(x:STRING):STRING
                """.trimIndent()
            }
        }
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

        when (val r = parser.parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 1
                r.errors.first().shouldHaveMessage("semantic error at (5:17): function f already declared")
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

        when (val r = parser.parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 1
                r.errors.first().shouldHaveMessage("semantic error at (2:26): variable x already declared")
            }
            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

    @Test
    fun `can't declare variable with same name as param in function`() {
        val input = """
            func f(x:INT):INT {
                x:INT = 1;
                return x + 1;
            }
        """.byteInputStream()

        when (val r = parser.parse(input)) {
            is ParseResult.Failure -> {
                r.errors shouldHaveSize 1
                r.errors.first().shouldHaveMessage("semantic error at (3:16): variable x already declared")
            }
            is ParseResult.Success -> {
                fail("should throw semantic error, but not")
            }
        }
    }

}
