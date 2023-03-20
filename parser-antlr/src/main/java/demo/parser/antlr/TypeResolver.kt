package demo.parser.antlr

import demo.parser.domain.SyntaxException
import demo.parser.domain.TokenLocation
import demo.parser.domain.BuiltinType
import org.antlr.v4.runtime.ParserRuleContext

interface TypeResolver {

    fun resolveType(node: CymplParser.ExprContext): BuiltinType

    fun resolveType(typeContext: CymplParser.TypeContext?): BuiltinType {
        if (typeContext == null) {
            return BuiltinType.VOID
        }

        if (typeContext.childCount == 1) {
            return resolveByText(typeContext)
        }

        if (typeContext.childCount == 2) {
            val elementTypeContext = typeContext.type()
            val elementType = resolveType(elementTypeContext)
            return BuiltinType.ARRAY(elementType)
        }

        val location = TokenLocation(typeContext.start.line, typeContext.start.charPositionInLine)
        throw SyntaxException("unknown type ${typeContext.text}", location)
    }

    private fun resolveByText(ctx: ParserRuleContext): BuiltinType {
        return when (ctx.text) {
            "void" -> BuiltinType.VOID
            "bool" -> BuiltinType.BOOL
            "int" -> BuiltinType.INT
            "float" -> BuiltinType.FLOAT
            "String" -> BuiltinType.STRING

            else -> {
                val location = TokenLocation(ctx.start.line, ctx.start.charPositionInLine)
                throw SyntaxException("unknown type ${ctx.text}", location)
            }
        }
    }
}
