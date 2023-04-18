package cympl.cli.command

import cympl.parser.ParseResult
import cympl.parser.Parser
import cympl.language.Program
import cympl.interpreter.Interpreter
import org.jline.terminal.Terminal
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.shell.standard.FileValueProvider
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import java.io.File
import java.io.FileInputStream
import java.lang.Exception

@ShellComponent
class InterpretCommand(
    @Autowired private val parserProvider: () -> Parser<Program>,
    @Autowired private val terminal: Terminal,
    @Lazy @Autowired private val runtime: cympl.interpreter.Runtime
) {

    @ShellMethod("Interpret a program")
    fun interpret(
        @ShellOption(
            value = ["-f"],
            defaultValue = ShellOption.NULL,
            valueProvider = FileValueProvider::class,
            help = "program file path to interpret"
        ) file: File?,
        @ShellOption(
            value = ["-v"],
            defaultValue = "false",
            help = "show verbose output when interpreting"
        ) verbose: Boolean,
    ) {
        if (file == null) {
            printResult("File is required")
            terminal.writer().flush()
            return
        }

        val parser = parserProvider()
        val interpreter = Interpreter(runtime)

        FileInputStream(file).use {
            when (val r = parser.parse(it)) {
                is ParseResult.Success<Program> -> {
                    interpreter.interpret(r.value, verbose).forEach(::printResult)
                }

                is ParseResult.Failure<*> -> {
                    r.errors.forEach(::printError)
                }
            }
        }
        terminal.writer().flush()
    }

    private fun printResult(result: String) {
        val message = result.fg(AttributedStyle.GREEN).toAnsi()
        terminal.writer().println(message)
    }

    private fun printError(e: Exception) {
        val message = (e.message ?: "Unknown error").fg(AttributedStyle.RED).toAnsi()
        terminal.writer().println(message)
    }

    companion object {
        private fun String.fg(color: Int): AttributedString =
            AttributedString(this, AttributedStyle.DEFAULT.foreground(color))
    }
}
