package demo.parser.analyze

import demo.parser.antlr.AntlrProgramParser
import demo.parser.domain.ParseResult
import demo.parser.domain.Program
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class CallGraphAnalyzerTest {

    @Test
    fun `support nested function declaration`() {
        val input = """
            func f1() {
                func f2() {
                    func f3() {
                        println("Hello, world!");
                    }
                    f3();
                }
                f2();
            }
            f1();
        """

        val program = parse(input)
        val analyzer = CallGraphAnalyzer()
        val callGraph = analyzer.analyze(program)

        with(callGraph) {
            nodes shouldContainExactly setOf("f1", "f2", "f3")
            edges shouldContainExactly setOf(
                "f1" to "f2",
                "f2" to "f3",
                "f3" to "println"
            )
        }
    }

    @Test
    fun `support function declaration with parameters`() {
        val input = """
            func main() { fact(0); a(); }

            func fact(n:INT):INT {
              if ( n==0 ) return 1;
              return n * fact(n-1);
            }

            func a() {  }
        """

        val program = parse(input)
        val analyzer = CallGraphAnalyzer()
        val callGraph = analyzer.analyze(program)

        with(callGraph) {
            nodes shouldContainExactly setOf("main", "fact", "a")
            edges shouldContainExactly setOf(
                "main" to "fact",
                "main" to "a",
                "fact" to "fact"
            )
        }
    }

    private fun parse(input: String): Program =
        when (val r = AntlrProgramParser().parse(input.byteInputStream())) {
            is ParseResult.Failure -> {
                r.errors.forEach { println(it) }
                fail(r.errors.first())
            }

            is ParseResult.Success -> r.value
        }
}
