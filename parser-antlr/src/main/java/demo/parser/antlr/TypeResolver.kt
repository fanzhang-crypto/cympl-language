package demo.parser.antlr

import demo.parser.domain.SyntaxException
import demo.parser.domain.TokenLocation
import demo.parser.domain.Type
import org.antlr.v4.runtime.Token

object TypeResolver {
    fun resolveType(typeToken: Token?): Type {
        if (typeToken == null) {
            return Type.VOID
        }
        return when (typeToken.type) {
            ExprLexer.VOID_TYPE -> Type.VOID
            ExprLexer.BOOL_TYPE -> Type.BOOL
            ExprLexer.INT_TYPE -> Type.INT
            ExprLexer.FLOAT_TYPE -> Type.FLOAT
            ExprLexer.STRING_TYPE -> Type.STRING

            else -> {
                val location = TokenLocation(typeToken.line, typeToken.charPositionInLine)
                throw SyntaxException("unknown type ${typeToken.text}", location)
            }
        }
    }
}
