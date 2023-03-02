package demo.parser.fp

import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.UnparsedRemainder
import demo.parser.domain.*
import java.io.InputStream

object FpParser: Parser {

    override fun parse(inputStream: InputStream): ParserResult {
        val input = String(inputStream.readAllBytes())

        return when(val r = ExpressionGrammar.tryParseToEnd(input)) {
            is Parsed<Program> -> ParserResult.Success(r.value)
            is UnparsedRemainder -> {
                val tokenLocation = TokenLocation(r.startsWith.row, r.startsWith.column)
                val msg = "extraneous input '${r.startsWith.text}'"
                val e = SyntaxException(msg, tokenLocation)
                ParserResult.Failure(listOf(e))
            }
            is ErrorResult -> ParserResult.Failure(listOf(RuntimeException(r.toString())))
        }
    }
}
