package demo.parser.fp

import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.*
import demo.parser.domain.*
import demo.parser.domain.ParseResult
import demo.parser.domain.Parser
import java.io.InputStream

typealias BPParseResult<T> = com.github.h0tk3y.betterParse.parser.ParseResult<T>

class FpProgramParser : Parser<Program> {

    private val grammar = ExpressionGrammar()

    override fun parse(inputStream: InputStream): ParseResult<Program> {
        val input = String(inputStream.readAllBytes())
        return grammar.tryParseToEnd(input).toParseResult()
    }

    private fun <T> BPParseResult<T>.toParseResult(): ParseResult<T> = when (this) {
        is Parsed<*> -> {
            val semanticErrors = grammar.getSemanticErrors()

            if (semanticErrors.isNotEmpty()) {
                grammar.clearSemanticErrors()
                ParseResult.Failure(semanticErrors)
            } else
                @Suppress("UNCHECKED_CAST")
                ParseResult.Success(value as T)
        }

        is UnparsedRemainder -> {
            val tokenLocation = TokenLocation(startsWith.row, startsWith.column)
            val msg = "extraneous input '${startsWith.text}'"
            val e = SyntaxException(msg, tokenLocation)
            ParseResult.Failure(listOf(e))
        }

        is MismatchedToken -> {
            val tokenLocation = TokenLocation(found.row, found.column)
            val msg = "expected '${expected.name}' but got ${found.text}"
            val e = SyntaxException(msg, tokenLocation)
            ParseResult.Failure(listOf(e))
        }

        is NoMatchingToken -> {
            val tokenLocation = TokenLocation(tokenMismatch.row, tokenMismatch.column)
            val msg = "token mismatch: ${tokenMismatch.text}"
            val e = SyntaxException(msg, tokenLocation)
            ParseResult.Failure(listOf(e))
        }

        is UnexpectedEof -> {
            val tokenLocation = TokenLocation(-1, -1)
            val msg = "expected ${expected.name} got EOF"
            val e = SyntaxException(msg, tokenLocation)
            ParseResult.Failure(listOf(e))
        }

        is AlternativesFailure -> this.errors[0].toParseResult()

        is ErrorResult -> {
            val tokenLocation = TokenLocation(-1, -1)
            val msg = "unexpected error: $this"
            val e = SyntaxException(msg, tokenLocation)
            ParseResult.Failure(listOf(e))
        }
    }
}
