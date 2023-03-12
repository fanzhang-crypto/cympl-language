package demo.parser.antlr

import demo.parser.domain.SyntaxException
import demo.parser.domain.TokenLocation
import demo.parser.domain.Type
import org.antlr.v4.runtime.ParserRuleContext

object TypeResolver {
    fun resolveType(typeContext: CymplParser.TypeContext?): Type {
        if (typeContext == null) {
            return Type.VOID
        }

        if (typeContext.childCount == 1) {
            return resolveByText(typeContext)
        }

        if (typeContext.childCount == 2) {
            val elementTypeContext = typeContext.type()
            val elementType = resolveType(elementTypeContext)
            return Type.ARRAY(elementType)
        }

        val location = TokenLocation(typeContext.start.line, typeContext.start.charPositionInLine)
        throw SyntaxException("unknown type ${typeContext.text}", location)
    }

    private fun resolveByText(ctx: ParserRuleContext): Type {
        return when (ctx.text) {
            "VOID" -> Type.VOID
            "BOOL" -> Type.BOOL
            "INT" -> Type.INT
            "FLOAT" -> Type.FLOAT
            "STRING" -> Type.STRING

            else -> {
                val location = TokenLocation(ctx.start.line, ctx.start.charPositionInLine)
                throw SyntaxException("unknown type ${ctx.text}", location)
            }
        }
    }
}
