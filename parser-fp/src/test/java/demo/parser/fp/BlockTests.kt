package demo.parser.fp

import demo.parser.domain.Interpreter
import demo.parser.domain.ParseResult
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class BlockTests {

    private val parser = FpProgramParser()
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
    fun `support break in while loop`() {
        val input = """
            x:INT = 1;
            while (x < 10) {
                x = x + 1;
                if (x == 5) {
                    break;
                }
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
                    while (x < 10) { x = x + 1; if (x == 5) { break; } } => void
                    x; => 5
                    environment:
                    x:INT = 5
                """.trimIndent()
            }
        }
    }

    @Test
    fun `support continue in while loop`() {
        val input = """
            x:INT = 0;
            i:INT = 0;
            while (i < 10) {
                if (i % 2 == 0) {
                    i = i + 1;
                    continue;
                }
                x = x + i;
                i = i + 1;
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
                    x:INT = 0; => 0
                    i:INT = 0; => 0
                    while (i < 10) { if (i % 2 == 0) { i = i + 1; continue; } x = x + i; i = i + 1; } => void
                    x; => 25
                    environment:
                    x:INT = 25, i:INT = 10
                """.trimIndent()
            }
        }
    }

    @Test
    fun `support nest while loop`() {
        val input = """
            x:INT = 0;
            i:INT = 0;
            while (i < 10) {
                j:INT = 0;
                while (j < 10) {
                    x = x + 1;
                    j = j + 1;
                }
                i = i + 1;
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
                    x:INT = 0; => 0
                    i:INT = 0; => 0
                    while (i < 10) { j:INT = 0; while (j < 10) { x = x + 1; j = j + 1; } i = i + 1; } => void
                    x; => 100
                    environment:
                    x:INT = 100, i:INT = 10
                """.trimIndent()
            }
        }
    }

    @Test
    fun `support nested break in nested while loop`() {
        val input = """
            x:INT = 0;
            i:INT = 0;
            while (i < 10) {
                j:INT = 0;
                while (j < 10) {
                    x = x + 1;
                    if (j > 5) {
                        break;
                    }
                    j = j + 1;
                }
                i = i + 1;
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
                    x:INT = 0; => 0
                    i:INT = 0; => 0
                    while (i < 10) { j:INT = 0; while (j < 10) { x = x + 1; if (j > 5) { break; } j = j + 1; } i = i + 1; } => void
                    x; => 70
                    environment:
                    x:INT = 70, i:INT = 10
                """.trimIndent()
            }
        }
    }

    @Test
    fun `support nested continue and break in nested while loop`() {
        val input = """
            x:INT = 0;
            i:INT = 0;
            while (i < 10) {
                j:INT = 0;
                while (j < 10) {
                    if (j % 2 == 0) {
                        j = j + 1;
                        continue;
                    }
                    x = x + 1;
                    if (j > 5) {
                        break;
                    }
                    j = j + 1;
                }
                i = i + 1;
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
                    x:INT = 0; => 0
                    i:INT = 0; => 0
                    while (i < 10) { j:INT = 0; while (j < 10) { if (j % 2 == 0) { j = j + 1; continue; } x = x + 1; if (j > 5) { break; } j = j + 1; } i = i + 1; } => void
                    x; => 40
                    environment:
                    x:INT = 40, i:INT = 10
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
