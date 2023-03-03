package demo.parser.fp

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import demo.parser.domain.*

internal class ExpressionGrammar : Grammar<Program>() {

    private val vars = mutableSetOf<String>()
    private val semanticErrors = mutableListOf<SemanticException>()

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
    private val DIV by literalToken("/")
    private val POW by literalToken("^")

    private val EQ by literalToken("=")
    private val COLON by literalToken(":")

    private val addOrSubChain: Parser<Expression> by leftAssociative(
        parser(this::mulOrDivChain),
        PLUS or MINUS
    ) { l, op, r ->
        when (op.text) {
            "+" -> Addition(l, r)
            "-" -> Subtraction(l, r)
            else -> throw IllegalArgumentException("unknown operator $op")
        }
    }

    private val mulOrDivChain: Parser<Expression> by leftAssociative(parser(this::powChain), TIMES or DIV) { l, op, r ->
        when (op.text) {
            "*" -> Multiplication(l, r)
            "/" -> Division(l, r)
            else -> throw IllegalArgumentException("unknown operator $op")
        }
    }

    private val powChain: Parser<Expression> by rightAssociative(parser(this::term), POW) { l, _, r ->
        Power(l, r)
    }

    private val parenthesized: Parser<Expression> by (-LPR * addOrSubChain * -RPR)
        .map { e -> Parenthesized(e) }

    private val negation: Parser<Expression> by (-MINUS * parser { term })
        .map { e -> Negation(e) }

    // @formatter:off
    private val term: Parser<Expression> by
        NUM.map { Number(it.text.toInt()) } or
        ID.map {idToken -> Variable(checkVarDeclared(idToken)) } or
        negation or
        parenthesized
    // @formatter:on

    private val assign: Parser<Expression> by (ID * EQ * addOrSubChain).map { (idToken, _, e) ->
        val id = checkVarDeclared(idToken)
        Assignment(id, e)
    }

    private val decl: Parser<Expression> by (ID * COLON * INT_TYPE * EQ * addOrSubChain).map { (idToken, _, type, _, e) ->
        val id = checkVarNotDeclared(idToken)
        vars += id
        Declaration(idToken.text, type.text, e)
    }

    private val prog: Parser<Program> by oneOrMore(
        decl or assign or addOrSubChain
    ).map { Program(it) }

    override val rootParser: Parser<Program> by prog

    fun getSemanticErrors(): List<SemanticException> = semanticErrors.toList()

    fun clearSemanticErrors() {
        semanticErrors.clear()
    }

    private fun checkVarDeclared(idToken: TokenMatch): String = idToken.text.also {
        if (!vars.contains(it)) {
            val location = TokenLocation(idToken.row, idToken.column - 1)
            semanticErrors += SemanticException("variable $it not defined", location)
        }
    }

    private fun checkVarNotDeclared(idToken: TokenMatch): String = idToken.text.also {
        if (vars.contains(it)) {
            val location = TokenLocation(idToken.row, idToken.column - 1)
            semanticErrors += SemanticException("variable $it already declared", location)
        }
    }
}

fun main() {
    val input = """
       ---2
    """.trimIndent()

    val program = ExpressionGrammar().parseToEnd(input)
    program.expressions.forEach(::println)

}
