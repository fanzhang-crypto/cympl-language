package demo.parser.fp

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import demo.parser.domain.*

internal object ExpressionGrammar : Grammar<Program>() {
    private val INT_TYPE by literalToken("INT")
    private val ID by regexToken("[a-z][a-zA-Z0-9_]*")
    private val NUM by regexToken("0|-?[1-9][0-9]*")

    private val WS by regexToken("\\s+", ignore = true)
    private val COMMENT by regexToken("//[^\r\n]*", ignore = true)
    private val NEWLINE by regexToken("[\r\n]+", ignore = true)

    private val LPR by literalToken("(")
    private val RPR by literalToken(")")

    private val PLUS by literalToken("+")
    private val MINUS by literalToken("-")
    private val TIMES by literalToken("*")
    private val DIV by regexToken("/")

    private val EQ by literalToken("=")
    private val COLON by literalToken(":")

    private val addOrSub: Parser<Expression> by leftAssociative(parser(this::mulOrDiv), PLUS or MINUS) { l, op, r ->
        when (op.text) {
            "+" -> Addition(l, r)
            "-" -> Subtraction(l, r)
            else -> throw IllegalArgumentException("unknown operator $op")
        }
    }

    private val mulOrDiv: Parser<Expression> by leftAssociative(parser(this::term), TIMES or DIV) { l, op, r ->
        when (op.text) {
            "*" -> Multiplication(l, r)
            "/" -> Division(l, r)
            else -> throw IllegalArgumentException("unknown operator $op")
        }
    }

    private val parenthesized: Parser<Expression> by (-LPR * addOrSub * -RPR)
        .map { e -> Parenthesized(e) }

    private val negation: Parser<Expression> by (MINUS * parser { term })
        .map { (_, e) -> Negation(e) }

    // @formatter:off
    private val term: Parser<Expression> by
        NUM.map { Number(it.text.toInt()) } or
        ID.map { Variable(it.text) } or
        negation or
        parenthesized
    // @formatter:on

    private val assign: Parser<Expression> by (ID * EQ * addOrSub).map { (id, _, e) ->
        Assignment(id.text, e)
    }

    private val decl: Parser<Expression> by (ID * COLON * INT_TYPE * EQ * addOrSub).map { (id, _, type, _, e) ->
        Declaration(id.text, type.text, e)
    }

    private val prog: Parser<Program> by oneOrMore(
        decl or assign or addOrSub
    ).map { Program(it) }

    override val rootParser: Parser<Program> by prog
}

fun main() {
    val input = """
        k = i * j
        k:INT = i / j
    """.trimIndent()

    val program = ExpressionGrammar.parseToEnd(input)
    program.expressions.forEach(::println)

}
