package demo.parser.analyze

import demo.parser.domain.Expression
import demo.parser.domain.Program
import demo.parser.domain.Statement
import guru.nidi.graphviz.attribute.Font
import guru.nidi.graphviz.attribute.Rank
import guru.nidi.graphviz.attribute.Shape
import guru.nidi.graphviz.attribute.Size
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.graph
import guru.nidi.graphviz.toGraphviz
import java.io.File

class CallGraphAnalyzer {

    private val nodes: MutableSet<String> = LinkedHashSet()
    private val edges: MutableSet<Pair<String, String>> = LinkedHashSet()

    private var currentFunction: String? = null

    fun analyze(program: Program): Graphviz {
        for (stat in program.statements) {
            analyze(stat)
        }
        return toGraphviz().also { reset() }
    }

    private fun analyze(statement: Statement) {
        when (statement) {
            is Statement.FunctionDeclaration -> {
                nodes += statement.id
                currentFunction = statement.id
                analyze(statement.body)
            }

            is Statement.Return -> {
                statement.expr?.let { analyze(it) }
            }

            is Statement.VariableDeclaration -> {
                if (statement.expr != null)
                    analyze(statement.expr!!)
            }

            is Statement.Assignment -> {
                analyze(statement.expr)
            }

            is Statement.ExpressionStatement -> {
                analyze(statement.expr)
            }

            is Statement.Block -> {
                for (stat in statement.statements) {
                    analyze(stat)
                }
            }

            is Statement.If -> {
                analyze(statement.thenBranch)
                statement.elseBranch?.let { analyze(it) }
            }

            is Statement.While -> {
                analyze(statement.body)
            }

            is Statement.For -> {
                analyze(statement.body)
            }

            else -> {}
        }
    }

    private fun analyze(expression: Expression) {
        when (expression) {
            is Expression.FunctionCall -> {
                if (currentFunction != null) {
                    edges.add(currentFunction!! to expression.id)
                }
            }

            is Expression.Index -> {
                analyze(expression.arrayExpr)
                analyze(expression.indexExpr)
            }

            is Expression.Not -> {
                analyze(expression.expr)
            }

            is Expression.And -> {
                analyze(expression.left)
                analyze(expression.right)
            }

            is Expression.Or -> {
                analyze(expression.left)
                analyze(expression.right)
            }

            is Expression.Addition -> {
                analyze(expression.left)
                analyze(expression.right)
            }

            is Expression.Subtraction -> {
                analyze(expression.left)
                analyze(expression.right)
            }

            is Expression.Multiplication -> {
                analyze(expression.left)
                analyze(expression.right)
            }

            is Expression.Division -> {
                analyze(expression.left)
                analyze(expression.right)
            }

            is Expression.Remainder -> {
                analyze(expression.left)
                analyze(expression.right)
            }

            is Expression.Power -> {
                analyze(expression.left)
                analyze(expression.right)
            }

            is Expression.Equality -> {
                analyze(expression.left)
                analyze(expression.right)
            }

            is Expression.Inequality -> {
                analyze(expression.left)
                analyze(expression.right)
            }

            is Expression.LessThan -> {
                analyze(expression.left)
                analyze(expression.right)
            }

            is Expression.LessThanOrEqual -> {
                analyze(expression.left)
                analyze(expression.right)
            }

            is Expression.GreaterThan -> {
                analyze(expression.left)
                analyze(expression.right)
            }

            is Expression.GreaterThanOrEqual -> {
                analyze(expression.left)
                analyze(expression.right)
            }

            is Expression.Negation -> {
                analyze(expression.expr)
            }

            is Expression.Parenthesized -> {
                analyze(expression.expr)
            }

            is Expression.Array -> {
                for (element in expression.elements) {
                    analyze(element)
                }
            }

            is Expression.Variable,
            is Expression.String,
            is Expression.Bool,
            is Expression.Float,
            is Expression.Int -> {
            }

        }
    }

    private fun reset() {
        nodes.clear()
        edges.clear()
        currentFunction = null
    }

    private fun toGraphviz() = graph(directed = true) {
        graph[Rank.dir(Rank.RankDir.TOP_TO_BOTTOM)]
        node[Shape.CIRCLE, Font.name("Helvetica"), Font.size(15), Size.mode(Size.Mode.FIXED).size(1.0, 1.0)]

        nodes.forEach { -it }
        edges.forEach { (from, to) -> from - to }
    }.toGraphviz()
}

fun main() {
    graph(directed = true) {
        -"a" - "a"
        -"b"
        -"c"

    }.toGraphviz().render(Format.PNG).toFile(File("ex1.png"))
}
