package demo.parser.analyze

import demo.parser.domain.Expression
import demo.parser.domain.Program
import demo.parser.domain.Statement
import demo.parser.domain.symbol.FunctionSymbol
import demo.parser.domain.symbol.GlobalScope
import demo.parser.domain.symbol.Scope
import guru.nidi.graphviz.attribute.Font
import guru.nidi.graphviz.attribute.Rank
import guru.nidi.graphviz.attribute.Shape
import guru.nidi.graphviz.attribute.Size
import guru.nidi.graphviz.graph
import guru.nidi.graphviz.toGraphviz

class CallGraphAnalyzer {

    private val nodes: MutableSet<String> = LinkedHashSet()
    private val edges: MutableSet<Pair<String, String>> = LinkedHashSet()

    private var currentScope: Scope = GlobalScope()

    class Graph(
        val nodes: Set<String>,
        val edges: Set<Pair<String, String>>
    ) {
        fun toGraphviz() = graph(directed = true) {
            graph[Rank.dir(Rank.RankDir.TOP_TO_BOTTOM)]
            node[Shape.CIRCLE, Font.name("Helvetica"), Font.size(15), Size.mode(Size.Mode.FIXED).size(1.0, 1.0)]

            nodes.forEach { -it }
            edges.forEach { (from, to) -> from - to }
        }.toGraphviz()
    }

    fun analyze(program: Program): Graph {
        for (stat in program.statements) {
            analyze(stat)
        }
        return Graph(nodes.toSet(), edges.toSet()).also { reset() }
    }

    private fun analyze(statement: Statement) {
        when (statement) {
            is Statement.FunctionDeclaration -> {
                currentScope = FunctionSymbol(statement.id, statement.returnType, emptyList(), currentScope)
                nodes += currentScope.scopeName
                analyze(statement.body)
                currentScope = currentScope.enclosingScope!!
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
                if (currentScope is FunctionSymbol) {
                    edges.add(currentScope.scopeName to expression.id)
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

            is Expression.ArrayLiteral -> {
                for (element in expression.elements) {
                    analyze(element)
                }
            }

            is Expression.Property -> {
                analyze(expression.expr)
            }

            is Expression.Variable,
            is Expression.StringLiteral,
            is Expression.BoolLiteral,
            is Expression.FloatLiteral,
            is Expression.IntLiteral,
            is Expression.Decrement,
            is Expression.Increment -> {
            }
        }
    }

    private fun reset() {
        nodes.clear()
        edges.clear()
        currentScope = GlobalScope()
    }
}
