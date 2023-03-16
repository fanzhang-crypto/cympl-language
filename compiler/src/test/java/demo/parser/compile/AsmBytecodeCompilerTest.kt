package demo.parser.compile

import demo.parser.antlr.AntlrProgramParser
import demo.parser.compile.AsmBytecodeCompiler.Companion.DEFAULT_MAIN_CLASS_NAME
import demo.parser.domain.ParseResult
import demo.parser.domain.Program
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method
import org.objectweb.asm.util.TraceClassVisitor
import java.io.File
import java.io.PrintWriter

class AsmBytecodeCompilerTest {

    private val parser = AntlrProgramParser()
    private val compiler = AsmBytecodeCompiler()

    @Test
    fun `support variable declaration`() {
        val input = """
            a:INT = 3;
            b:STRING = "Hello";
            c:BOOL = true;
            d:FLOAT = 3.14 * a;
            e:INT[] = [1,2,-a];
            f:INT[][] = [[1,2,3],[4,5,6]];
            
            println(a++);
            println(++a);
            println(a--);
            println(--a);
            println(b);
            println(c);
            println(d--);
            println(--d);
            println(d++);
            println(++d);
            println(e);
            println(f);
            println(b + " World");
            println(a == d);
            println(a == 3.0);
            println(b + " World" == "Hello World");
            println(b != "World");
            println(b > "Hello World");
            println(b >= "Hello World");
            println(b < "Hello World");
            println(b <= "Hello World");
        """.trimIndent()

        val output = compileAndExecute(input)

        output shouldBe """
                3
                5
                5
                3
                Hello
                true
                9.42
                7.42
                7.42
                9.42
                [1, 2, -3]
                [[1, 2, 3], [4, 5, 6]]
                Hello World
                false
                true
                true
                true
                false
                false
                true
                true
            """.trimIndent()
    }

    @Test
    fun `GeneratorAdapter test`() {
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        val printWriter = PrintWriter(System.out)
        val cv = TraceClassVisitor(cw, printWriter)

        cv.visit(V1_8, ACC_PUBLIC + ACC_SUPER, "Example", null, "java/lang/Object", null)

        val m2 = Method.getMethod("void main (String[])")
        GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, m2, null, null, cv).apply {
            val `var`: Int = newLocal(Type.INT_TYPE)
            push(42.42)
            storeLocal(`var`)
            val varLabel: Label = mark()
            returnValue()
            val endLabel: Label = mark()
            visitLocalVariable("x", "D", null, varLabel, endLabel, `var`)
            endMethod()
        }

        cv.visitEnd()

        File("Example.class").writeBytes(cw.toByteArray())
    }

    @Test
    fun `javassit test`() {
        val input = """
            a:INT = 3;
        """.trimIndent()

        val program = parse(input)
        val bytecode = JavassitBytecodeCompiler().compile(program)

        File("${JavassitBytecodeCompiler.DEFAULT_MAIN_CLASS_NAME}.class").writeBytes(bytecode)
    }

    private fun compileAndExecute(script: String): String {
        val program = parse(script)
        val bytecode = compiler.compile(program)

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

}
