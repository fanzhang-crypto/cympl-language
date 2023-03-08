package demo.parser.antlr

import demo.parser.domain.Interpreter
import demo.parser.domain.ParseResult
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class BlockTests {

    private val parser = AntlrProgramParser()
    private val interpreter = Interpreter()

    @Test
    fun `variables in different blocks have no conflict`() {
        val input = """
            {
                x:INT = 1;
                {
                    x:INT = 2;
                }
                return x;
            }
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
                    { x:INT = 1; { x:INT = 2; } return x; } => 1
                    environment:
                """.trimIndent()
            }
        }
    }

    @Test
    fun `support simple if else blocks`() {
        val input = """
            x:INT = 1;
            if (x == 1) {
                x:INT = 2;
                return x;
            } else {
                x:INT = 3;
                return x;
            }
            x;
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
                    if (x == 1) { x:INT = 2; return x; } else { x:INT = 3; return x; } => 2
                    x; => 1
                    environment:
                    x:INT = 1
                """.trimIndent()
            }
        }
    }

    @Test
    fun `support if else statement`() {
        val input = """
            x:INT = 1;
            if (x == 1)
                x = 2;
            else
                x = 3;
            x;
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
                    if (x == 1) x = 2; else x = 3; => 2
                    x; => 2
                    environment:
                    x:INT = 2
                """.trimIndent()
            }
        }
    }

    @Test
    fun `support nested if else blocks`() {
        val input = """
            x:INT = 1;
            if (x == 1) {
                if (x < 0) {
                    x = -x;
                } else {
                    x = x + 1;
                }
            } else {
                x = 5;
            }
            x;
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
                    if (x == 1) { if (x < 0) { x = -x; } else { x = x + 1; } } else { x = 5; } => void
                    x; => 2
                    environment:
                    x:INT = 2
                """.trimIndent()
            }
        }
    }

    @Test
    fun `support simple while statement`() {
        val input = """
            x:INT = 1;
            while (x < 10) {
                x = x + 1;
            }
            x;
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
                    while (x < 10) { x = x + 1; } => void
                    x; => 10
                    environment:
                    x:INT = 10
                """.trimIndent()
            }
        }
    }

    @Test
    fun `can return early from a while statement in function`() {
        val input = """
            func f(x:INT):INT {
                while (x < 10) {
                    x = x + 1;
                    if (x == 5) {
                        return x;
                    }
                }
                return x;
            }
            f(1);
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
                    func f(x:INT):INT { while (x < 10) { x = x + 1; if (x == 5) { return x; } } return x; } => void
                    f(1); => 5
                    environment:
                    f(x:INT):INT
                """.trimIndent()
            }
        }
    }
}
