package cympl.analyzer

import cympl.language.Expression
import cympl.language.Program
import cympl.language.Statement
import cympl.language.symbol.FunctionSymbol
import cympl.language.symbol.GlobalScope
import cympl.language.symbol.Scope

class CallGraphAnalyzer {

    private var currentScope: Scope = GlobalScope()

    fun analyze(program: Program): Graph {
        val graphBuilder = Graph.Builder()
        for (stat in program.statements) {
            analyze(stat, graphBuilder)
        }
        return graphBuilder.build().also { currentScope = GlobalScope() }
    }

    private fun analyze(statement: Statement, builder: Graph.Builder) {
        when (statement) {
            is Statement.FunctionDeclaration -> {
                currentScope = FunctionSymbol(statement.id, statement.returnType, emptyList(), currentScope)
                builder.addNode(statement.id)
                analyze(statement.body, builder)
                currentScope = currentScope.enclosingScope!!
            }

            is Statement.Return -> {
                statement.expr?.let { analyze(it, builder) }
            }

            is Statement.VariableDeclaration -> {
                if (statement.expr != null)
                    analyze(statement.expr!!, builder)
            }

            is Statement.Assignment -> {
                analyze(statement.expr, builder)
            }

            is Statement.ExpressionStatement -> {
                analyze(statement.expr, builder)
            }

            is Statement.Block -> {
                for (stat in statement.statements) {
                    analyze(stat, builder)
                }
            }

            is Statement.If -> {
                analyze(statement.thenBranch, builder)
                statement.elseBranch?.let { analyze(it, builder) }
            }

            is Statement.While -> {
                analyze(statement.body, builder)
            }

            is Statement.For -> {
                analyze(statement.body, builder)
            }

            else -> {}
        }
    }

    private fun analyze(expression: Expression, builder: Graph.Builder) {
        when (expression) {
            is Expression.FunctionCall -> {
                if (currentScope is FunctionSymbol) {
                    val funcExpr = expression.funcExpr
                    if (funcExpr is Expression.Variable) {
                        builder.addEdge(currentScope.scopeName, funcExpr.id)
                    }
                }
            }

            is Expression.Index -> {
                analyze(expression.arrayExpr, builder)
                analyze(expression.indexExpr, builder)
            }

            is Expression.Not -> {
                analyze(expression.expr, builder)
            }

            is Expression.And -> {
                analyze(expression.left, builder)
                analyze(expression.right, builder)
            }

            is Expression.Or -> {
                analyze(expression.left, builder)
                analyze(expression.right, builder)
            }

            is Expression.Addition -> {
                analyze(expression.left, builder)
                analyze(expression.right, builder)
            }

            is Expression.Subtraction -> {
                analyze(expression.left, builder)
                analyze(expression.right, builder)
            }

            is Expression.Multiplication -> {
                analyze(expression.left, builder)
                analyze(expression.right, builder)
            }

            is Expression.Division -> {
                analyze(expression.left, builder)
                analyze(expression.right, builder)
            }

            is Expression.Remainder -> {
                analyze(expression.left, builder)
                analyze(expression.right, builder)
            }

            is Expression.Power -> {
                analyze(expression.left, builder)
                analyze(expression.right, builder)
            }

            is Expression.Equality -> {
                analyze(expression.left, builder)
                analyze(expression.right, builder)
            }

            is Expression.Inequality -> {
                analyze(expression.left, builder)
                analyze(expression.right, builder)
            }

            is Expression.LessThan -> {
                analyze(expression.left, builder)
                analyze(expression.right, builder)
            }

            is Expression.LessThanOrEqual -> {
                analyze(expression.left, builder)
                analyze(expression.right, builder)
            }

            is Expression.GreaterThan -> {
                analyze(expression.left, builder)
                analyze(expression.right, builder)
            }

            is Expression.GreaterThanOrEqual -> {
                analyze(expression.left, builder)
                analyze(expression.right, builder)
            }

            is Expression.Negation -> {
                analyze(expression.expr, builder)
            }

            is Expression.Parenthesized -> {
                analyze(expression.expr, builder)
            }

            is Expression.ArrayLiteral -> {
                for (element in expression.elements) {
                    analyze(element, builder)
                }
            }

            is Expression.Property -> {
                analyze(expression.expr, builder)
            }

            is Expression.NewArray -> {
                expression.dimensions.forEach { analyze(it, builder) }
            }

            is Expression.Variable,
            is Expression.StringLiteral,
            is Expression.BoolLiteral,
            is Expression.FloatLiteral,
            is Expression.IntLiteral,
            is Expression.Decrement,
            is Expression.Increment -> {
            }

            is Expression.Lambda -> {
//                currentScope = FunctionSymbol("lambda", expression.returnType, emptyList(), currentScope)
//                nodes += currentScope.scopeName
                analyze(expression.body, builder)
//                currentScope = currentScope.enclosingScope!!
            }
        }
    }
}

class Graph private constructor(
    val nodes: Set<String>,
    val edges: Set<Pair<String, String>>
) {
    internal class Builder {
        private val nodes: MutableSet<String> = LinkedHashSet()
        private val edges: MutableSet<Pair<String, String>> = LinkedHashSet()

        fun addNode(node: String) {
            nodes += node
        }

        fun addEdge(from: String, to: String) {
            edges += from to to
        }

        fun build() = Graph(nodes, edges)
    }
}
