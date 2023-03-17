package demo.parser.app.command

import demo.parser.compile.jvm.JvmCompileOptions
import demo.parser.compile.jvm.JvmCompiler
import demo.parser.domain.ParseResult
import demo.parser.domain.Parser
import demo.parser.domain.Program
import org.apache.commons.text.CaseUtils
import org.jline.terminal.Terminal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.shell.standard.FileValueProvider
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import java.io.File
import java.io.FileInputStream

@ShellComponent
class CompileCommand(
    @Autowired private val parserFactory: () -> Parser<Program>,
    @Autowired private val terminal: Terminal,
) {

    private val compiler: JvmCompiler = JvmCompiler()

    @ShellMethod("Compile a program to JVM bytecode")
    fun compile(
        @ShellOption(
            value = ["-f"],
            defaultValue = ShellOption.NULL,
            help = "cympl script file path to compile",
            valueProvider = FileValueProvider::class
        ) sourceFile: File?,
        @ShellOption(
            value = ["-d"],
            defaultValue = ShellOption.NULL,
            help = "output file directory",
        ) outputDir: File?
    ) {
        if (sourceFile == null) {
            terminal.writer().println("source file is required")
            terminal.writer().flush()
            return
        }
        if (!sourceFile.exists()) {
            terminal.writer().println("source file does not exist")
            terminal.writer().flush()
            return
        }

        val mainClassName = CaseUtils.toCamelCase(sourceFile.nameWithoutExtension, true, '_', '-', ' ')
        val outputFile = (outputDir ?: sourceFile.parentFile).resolve("${mainClassName}.class")
        val options = JvmCompileOptions(mainClassName = mainClassName)

        FileInputStream(sourceFile).use {
            when (val r = parserFactory().parse(it)) {
                is ParseResult.Success<Program> -> {
                    val bytecode = compiler.compile(r.value, options)
                    outputFile.writeBytes(bytecode)
                }

                is ParseResult.Failure -> r.errors.forEach { e -> System.err.println(e.message) }
            }
        }
    }
}
