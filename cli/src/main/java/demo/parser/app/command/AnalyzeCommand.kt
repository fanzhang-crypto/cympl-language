package demo.parser.app.command

import demo.parser.analyze.CallGraphAnalyzer
import demo.parser.domain.ParseResult
import demo.parser.domain.Parser
import demo.parser.domain.Program
import guru.nidi.graphviz.engine.Format
import org.jline.terminal.Terminal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.shell.standard.FileValueProvider
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import java.io.File
import java.io.FileInputStream

@ShellComponent
class AnalyzeCommand(
    @Autowired private val parserFactory: () -> Parser<Program>,
    @Autowired private val terminal: Terminal,
) {

    private val analyzer = CallGraphAnalyzer()

    @ShellMethod("Analyze a program's call graph")
    fun analyze(
        @ShellOption(
            value = ["-f"],
            defaultValue = ShellOption.NULL,
            help = "program file path to analyze",
            valueProvider = FileValueProvider::class
        ) file: File?
    ) {
        if (file == null) {
            terminal.writer().println("File is required")
            terminal.writer().flush()
            return
        }

        FileInputStream(file).use {
            when (val r = parserFactory().parse(it)) {
                is ParseResult.Success<Program> -> {
                    val outputFilename = "${file.nameWithoutExtension}-call-graph.png"
                    val outputPath = file.path.replace(file.name, outputFilename)
                    analyzer.analyze(r.value)
                        .toGraphviz()
                        .render(Format.PNG)
                        .toFile(File(outputPath))

                    Runtime.getRuntime().exec("open $outputPath")
                }

                is ParseResult.Failure -> r.errors.forEach { e -> System.err.println(e.message) }
            }
        }
    }
}
