package demo.parser.app

import demo.parser.domain.*
import demo.parser.interpret.Interpreter
import org.jline.reader.EndOfFileException
import org.jline.reader.Highlighter
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.terminal.Terminal
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle
import org.jline.widget.AutosuggestionWidgets
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.shell.standard.FileValueProvider
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import java.io.File
import java.io.FileInputStream
import java.lang.Exception
import java.util.regex.Pattern

@ShellComponent
class InterpretCommand(
    @Autowired private val parserProvider: () -> Parser<Program>,
    @Autowired private val terminal: Terminal,
    @Autowired private val jlineParser: org.jline.reader.Parser
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
            repl()
        } else if (file != null) {
            interpretInBatch(file)
        }
    }

    private val keywords = setOf(
        "if", "else", "while", "for", "break", "continue", "return", "func", "true", "false", "quit",
        BuiltinType.VOID.name, BuiltinType.INT.name, BuiltinType.FLOAT.name, BuiltinType.BOOL.name,
        BuiltinType.STRING.name
    )

    private fun repl() {
        val evaluator = Evaluator()
        val lineReader = getReplLineReader { evaluator.interpreter.globalSymbols }

        val autosuggestionWidgets = AutosuggestionWidgets(lineReader)
        autosuggestionWidgets.enable()

        try {
            readUserInputLines(lineReader, prompt).forEach(evaluator::eval)
        } catch (e: Exception) {
            when (e) {
                is UserInterruptException, is EndOfFileException -> {
                    // ignore
                }

                else -> throw e
            }
        } finally {
            autosuggestionWidgets.disable()
        }
    }

    private inner class Evaluator {
        val interpreter = Interpreter()
        private val parser = parserProvider()

        fun eval(inputLine: String) {
            when (val r = parser.parse(inputLine.byteInputStream())) {
                is ParseResult.Success -> {
                    try {
                        interpreter.interpret(r.value).forEach(::printResult)
                    } catch (e: RuntimeException) {
                        printError(e)
                    }
                }

                is ParseResult.Failure -> {
                    r.errors.forEach(::printError)
                }
            }
        }
    }


    private fun readUserInputLines(lineReader: LineReader, prompt: String): Sequence<String> = sequence {
        while (true) {
            val line = lineReader.readLine(prompt)
            if (line.isBlank()) {
                continue
            }
            if (line.trim() == "quit") {
                break
            }

            val inputLine = if (line.trimEnd().endsWith(";") || line.endsWith("}"))
                line
            else
                "$line;"

            yield(inputLine)
        }
    }

    private fun getReplLineReader(symbolProvider: () -> Collection<String>): LineReader {
        val replCompleter = StringsCompleter {
            symbolProvider() + keywords
        }
        val replHighlighter = object : Highlighter {
            override fun highlight(reader: LineReader, buffer: String): AttributedString {
                val color = if (symbolProvider().contains(buffer)) {
                    AttributedStyle.CYAN
                } else if (keywords.contains(buffer)) {
                    AttributedStyle.YELLOW
                } else {
                    AttributedStyle.WHITE
                }
                return buffer.foreground(color)
            }

            override fun setErrorPattern(errorPattern: Pattern?) {
            }

            override fun setErrorIndex(errorIndex: Int) {
            }
        }

        return LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(replCompleter)
            .highlighter(replHighlighter)
            .parser(jlineParser)
            .build()
            .also {
                it.unsetOpt(LineReader.Option.INSERT_TAB)
            }
    }

    private fun interpretInBatch(file: File) {
        val parser = parserProvider()
        val interpreter = Interpreter()

        FileInputStream(file).use {
            when (val r = parser.parse(it)) {
                is ParseResult.Success<Program> -> {
                    interpreter.interpret(r.value).forEach(::printResult)
                }

                is ParseResult.Failure<*> -> {
                    r.errors.forEach(::printError)
                }
            }
            terminal.writer().flush()
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

    companion object {
        private val prompt = ">>> ".foreground(AttributedStyle.BLUE).toAnsi()

        private fun String.foreground(color: Int): AttributedString =
            AttributedString(this, AttributedStyle.DEFAULT.foreground(color))
    }
}
