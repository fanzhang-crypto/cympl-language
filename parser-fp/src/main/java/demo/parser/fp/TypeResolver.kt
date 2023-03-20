package demo.parser.fp

import com.github.h0tk3y.betterParse.lexer.TokenMatch
import demo.parser.domain.SyntaxException
import demo.parser.domain.TokenLocation
import demo.parser.domain.BuiltinType

object TypeResolver {

    fun resolveArrayType(elementType: BuiltinType, dimension: Int): BuiltinType =
        if (dimension == 1)
            BuiltinType.ARRAY(elementType)
        else
            BuiltinType.ARRAY(resolveArrayType(elementType, dimension - 1))

    fun resolveValueType(typeToken: TokenMatch): BuiltinType = when (typeToken.text) {
        CymplGrammar.VOID_TYPE -> BuiltinType.VOID
        CymplGrammar.BOOL_TYPE -> BuiltinType.BOOL
        CymplGrammar.INT_TYPE -> BuiltinType.INT
        CymplGrammar.FLOAT_TYPE -> BuiltinType.FLOAT
        CymplGrammar.STRING_TYPE -> BuiltinType.STRING
        else -> {
            val location = TokenLocation(typeToken.row, typeToken.column)
            throw SyntaxException("unknown type ${typeToken.text}", location)
        }
    }
}
