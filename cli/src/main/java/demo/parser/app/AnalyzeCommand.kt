package demo.parser.app

import demo.parser.analyze.CallGraphAnalyzer
import demo.parser.domain.*
import guru.nidi.graphviz.engine.Format
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.shell.standard.FileValueProvider
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import java.io.File
import java.io.FileInputStream

@ShellComponent
class AnalyzeCommand(
    @Autowired private val parserFactory: () -> Parser<Program>
) {

    private val analyzer = CallGraphAnalyzer()

    @ShellMethod("Analyze a program's call graph")
    fun analyze(
        @ShellOption(
            value = ["-f"],
            help = "program file path to analyze",
            valueProvider = FileValueProvider::class
        ) file: File
    ): Unit =
        FileInputStream(file).use {
            when (val r = parserFactory().parse(it)) {
                is ParseResult.Success<Program> -> {
                    analyzer.analyze(r.value)
                        .render(Format.PNG)
                        .toFile(File("call-graph.png"))

                    Runtime.getRuntime().exec("open call-graph.png")
                }

                is ParseResult.Failure -> r.errors.forEach { e -> System.err.println(e.message) }
            }
        }
}
