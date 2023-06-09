package cympl.cli.command

import cympl.cli.fg
import cympl.cli.highlight.SyntaxHighlighter
import cympl.parser.ParseResult
import cympl.parser.Parser
import cympl.interpreter.Interpreter
import cympl.language.BuiltinType
import cympl.language.Program
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.terminal.Terminal
import org.jline.utils.AttributedStyle
import org.jline.widget.AutosuggestionWidgets
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import java.lang.Exception

@ShellComponent
class ReplCommand(
    @Autowired private val parserProvider: () -> Parser<Program>,
    @Autowired private val terminal: Terminal,
    @Autowired private val jlineParser: org.jline.reader.Parser,
    @Lazy @Autowired private val runtime: cympl.interpreter.Runtime
) {

    @ShellMethod("Start a REPL")
    fun repl() {
        val evaluator = Evaluator()
        val lineReader = createLineReader { evaluator.globalSymbols }

        val autosuggestionWidgets = AutosuggestionWidgets(lineReader)
        autosuggestionWidgets.enable()

        try {
            lineReader.readUserInputLines().forEach(evaluator::eval)
        } catch (e: Exception) {
            when (e) {
                is UserInterruptException, is EndOfFileException -> {
                    // exit repl
                }

                else -> throw e
            }
        } finally {
            autosuggestionWidgets.disable()
        }
    }

    private fun createLineReader(symbolProvider: () -> Collection<String>): LineReader {
        val replCompleter = StringsCompleter { symbolProvider() + keywords }
        val replHighlighter = SyntaxHighlighter(symbolProvider)

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

    private fun LineReader.readUserInputLines(): Sequence<String> = sequence {
        while (true) {
            val line = readLine(prompt)
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

    private inner class Evaluator {

        val globalSymbols get() = interpreter.globalSymbols

        private val parser: Parser<Program> = parserProvider()
        private val interpreter = Interpreter(runtime)

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

    private fun printResult(result: String) {
        val message = result.fg(AttributedStyle.GREEN).toAnsi()
        terminal.writer().println(message)
    }

    private fun printError(e: Exception) {
        val message = (e.message ?: "Unknown error").fg(AttributedStyle.RED).toAnsi()
        terminal.writer().println(message)
    }

    companion object {
        private val prompt = ">>> ".fg(AttributedStyle.BLUE).toAnsi()

        private const val QUIT_COMMAND = "quit"

        private val keywords = setOf(
            "if", "else", "while", "for", "switch", "case", "break", "continue", "return", "true", "false",
            QUIT_COMMAND,
            BuiltinType.VOID.name,
            BuiltinType.INT.name,
            BuiltinType.FLOAT.name,
            BuiltinType.BOOL.name,
            BuiltinType.STRING.name
        )
    }
}
