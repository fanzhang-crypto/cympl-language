package cympl.cli.command

import cympl.analyzer.CallGraphAnalyzer
import cympl.analyzer.Graph
import cympl.parser.ParseResult
import cympl.parser.Parser
import cympl.language.Program
import guru.nidi.graphviz.attribute.Font
import guru.nidi.graphviz.attribute.Rank
import guru.nidi.graphviz.attribute.Shape
import guru.nidi.graphviz.attribute.Size
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.graph
import guru.nidi.graphviz.toGraphviz
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.shell.standard.FileValueProvider
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files

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
    ) {
        FileInputStream(file).use {
            when (val r = parserFactory().parse(it)) {
                is ParseResult.Success<Program> -> {
                    val outputPath = Files.createTempFile("call-graph-", "-${file.nameWithoutExtension}.png")

                    analyzer.analyze(r.value)
                        .toGraphviz()
                        .render(Format.PNG)
                        .toFile(outputPath.toFile())

                    Runtime.getRuntime().exec("open $outputPath")
                }

                is ParseResult.Failure -> r.errors.forEach { e -> System.err.println(e.message) }
            }
        }
    }

    private fun Graph.toGraphviz() = graph(directed = true) {
        graph[Rank.dir(Rank.RankDir.TOP_TO_BOTTOM)]
        node[Shape.CIRCLE, Font.name("Helvetica"), Font.size(15), Size.mode(Size.Mode.FIXED).size(1.0, 1.0)]

        nodes.forEach { -it }
        edges.forEach { (from, to) -> from - to }
    }.toGraphviz()
}
