package demo.parser.app

import demo.parser.domain.*
import demo.parser.interpret.Interpreter
import org.jline.reader.LineReader
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
    @Autowired private val parserFactory: () -> Parser<Program>,
    @Autowired @Lazy private val lineReader: LineReader,
    @Autowired private val terminal: Terminal
) {

    @ShellMethod("Interpret a program")
    fun interpret(
        @ShellOption(
            value = ["-f"],
            defaultValue = ShellOption.NULL,
            valueProvider = FileValueProvider::class,
            help = "program file path to interpret"
        ) file: File?,
        @ShellOption(value = ["-i"], help = "interpret interactively") interactively: Boolean
    ) {
        if (interactively) {
            interpretInteractively()
        } else if (file != null) {
            interpretInBatch(file)
        }
    }

    private fun interpretInteractively() {
        val parser = parserFactory()
        val interpreter = Interpreter()

        val prompt = AttributedString("interpreter:>", AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
        while (true) {
            lineReader.readLine(prompt.toAnsi())?.let { line ->
                if (line.isBlank()) {
                    return@let
                }
                if (line == "quit") {
                    return
                }

                val inputLine = if (line.trimEnd().endsWith(";") || line.endsWith("}"))
                    line
                else
                    "$line;"

                when (val r = parser.parse(inputLine.byteInputStream())) {
                    is ParseResult.Success -> {
                        try {
                            interpreter.interpret(r.value).forEach(::printResult)
                        } catch (e: RuntimeException) {
                            printError(e)
                            return
                        }
                    }

                    is ParseResult.Failure -> {
                        r.errors.forEach(::printError)
                    }
                }
            }
        }
    }

    private fun interpretInBatch(file: File) {
        val parser = parserFactory()
        val interpreter = Interpreter()

        FileInputStream(file).use {
            when (val r = parser.parse(it)) {
                is ParseResult.Success<Program> -> interpreter.interpret(r.value).forEach(::printResult)
                is ParseResult.Failure<*> -> r.errors.forEach(::printError)
            }
        }
    }

    private fun printResult(result: String) {
        val message = AttributedString(result, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
        terminal.writer().println(message.toAnsi())
    }

    private fun printError(e: Exception) {
        val message = AttributedString(e.message, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
        terminal.writer().println(message.toAnsi())
    }
}
