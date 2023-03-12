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
        return when (typeToken.text) {
            "VOID" -> Type.VOID
            "BOOL" -> Type.BOOL
            "INT" -> Type.INT
            "FLOAT" -> Type.FLOAT
            "STRING" -> Type.STRING

            else -> {
                val location = TokenLocation(typeToken.line, typeToken.charPositionInLine)
                throw SyntaxException("unknown type ${typeToken.text}", location)
            }
        }
    }
}
