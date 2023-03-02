package demo.parser.fp

import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.*
import demo.parser.domain.*
import demo.parser.domain.Parser
import java.io.InputStream

object FpParser : Parser {

    override fun parse(inputStream: InputStream): ParserResult {
        val input = String(inputStream.readAllBytes())

        return when (val r = ExpressionGrammar.tryParseToEnd(input)) {
            is Parsed<Program> -> ParserResult.Success(r.value)
            else -> toParserResult(r as ErrorResult)
        }
    }

    private fun toParserResult(r: ErrorResult): ParserResult.Failure = when(r) {
        is UnparsedRemainder -> {
            val tokenLocation = TokenLocation(r.startsWith.row, r.startsWith.column)
            val msg = "extraneous input '${r.startsWith.text}'"
            val e = SyntaxException(msg, tokenLocation)
            ParserResult.Failure(listOf(e))
        }

        is MismatchedToken -> {
            val tokenLocation = TokenLocation(r.found.row, r.found.column)
            val msg = "expected '${r.expected.name}' but got ${r.found.text}"
            val e = SyntaxException(msg, tokenLocation)
            ParserResult.Failure(listOf(e))
        }

        is NoMatchingToken -> {
            val tokenLocation = TokenLocation(r.tokenMismatch.row, r.tokenMismatch.column)
            val msg = "token mismatch: ${r.tokenMismatch}"
            val e = SyntaxException(msg, tokenLocation)
            ParserResult.Failure(listOf(e))
        }

        is UnexpectedEof -> {
            val tokenLocation = TokenLocation(-1, -1)
            val msg = "expected ${r.expected.name} got EOF"
            val e = SyntaxException(msg, tokenLocation)
            ParserResult.Failure(listOf(e))
        }

        is AlternativesFailure -> toParserResult(r.errors[0])

        else -> ParserResult.Failure(listOf(RuntimeException(r.toString())))
    }
}
