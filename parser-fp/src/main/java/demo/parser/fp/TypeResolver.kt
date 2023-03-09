package demo.parser.fp

import com.github.h0tk3y.betterParse.lexer.TokenMatch
import demo.parser.domain.SyntaxException
import demo.parser.domain.TokenLocation
import demo.parser.domain.Type

object TypeResolver {
    fun resolveType(typeToken: TokenMatch): Type = when (typeToken.text) {
        ProgramGrammar.VOID_TYPE -> Type.VOID
        ProgramGrammar.BOOL_TYPE -> Type.BOOL
        ProgramGrammar.INT_TYPE -> Type.INT
        ProgramGrammar.FLOAT_TYPE -> Type.FLOAT
        ProgramGrammar.STRING_TYPE -> Type.STRING
        else -> {
            val location = TokenLocation(typeToken.row, typeToken.column)
            throw SyntaxException("unknown type ${typeToken.text}", location)
        }
    }
}
