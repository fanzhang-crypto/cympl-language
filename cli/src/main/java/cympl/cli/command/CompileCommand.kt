package cympl.cli.command

import cympl.compiler.jvm.JvmCompileOptions
import cympl.compiler.jvm.JvmCompiler
import cympl.parser.ParseResult
import cympl.parser.Parser
import cympl.language.Program
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
            help = "cympl script file path to compile",
            valueProvider = FileValueProvider::class
        ) sourceFile: File,
    ) {
        if (!sourceFile.exists()) {
            terminal.writer().println("source file does not exist")
            terminal.writer().flush()
            return
        }

        val mainClassName = CaseUtils.toCamelCase(sourceFile.nameWithoutExtension, true, '_', '-', ' ')
        val outputDir = sourceFile.parentFile
        val options = JvmCompileOptions(mainClassName = mainClassName)

        FileInputStream(sourceFile).use {
            when (val r = parserFactory().parse(it)) {
                is ParseResult.Success<Program> -> {
                    val program = r.value

                    compiler.compile(program, options).forEach { (name, bytecode) ->
                        if (name.contains(".")) {
                            // for classes with package name, create the directory structure accordingly
                            val classFilePath = name.replace(".", "/")
                            val classFileDir = classFilePath.substringBeforeLast("/")
                            File("$outputDir/$classFileDir").mkdirs()
                            File("$outputDir/$classFilePath.class").writeBytes(bytecode)
                        } else {
                            File("$outputDir/$name.class").writeBytes(bytecode)
                        }
                    }
                }

                is ParseResult.Failure -> r.errors.forEach { e -> System.err.println(e.message) }
            }
        }
    }
}
