package cympl.parser.antlr

import cympl.language.*
import cympl.parser.SyntaxException
import cympl.parser.TokenLocation
import cympl.parser.antlr.CymplParser.ArrayTypeContext
import cympl.parser.antlr.CymplParser.BoolTypeContext
import cympl.parser.antlr.CymplParser.FloatTypeContext
import cympl.parser.antlr.CymplParser.FunctionArrayTypeContext
import cympl.parser.antlr.CymplParser.FunctionTypeContext
import cympl.parser.antlr.CymplParser.IntTypeContext
import cympl.parser.antlr.CymplParser.StringTypeContext
import cympl.parser.antlr.CymplParser.VoidTypeContext

interface TypeResolver {

    fun resolveType(node: CymplParser.ExprContext): BuiltinType

    fun resolveType(typeContext: CymplParser.TypeContext?): BuiltinType {
        return when (typeContext) {
            null, is VoidTypeContext -> BuiltinType.VOID
            is BoolTypeContext -> BuiltinType.BOOL
            is IntTypeContext -> BuiltinType.INT
            is FloatTypeContext -> BuiltinType.FLOAT
            is StringTypeContext -> BuiltinType.STRING
            is FunctionTypeContext -> {
                val funcTypeContext = typeContext.funcType()
                val returnType = resolveType(funcTypeContext.retType)
                val parameterTypes =
                    funcTypeContext.paramTypes?.type()?.map { resolveType(it as CymplParser.TypeContext) }
                        ?: emptyList()
                BuiltinType.FUNCTION(parameterTypes, returnType)
            }

            is FunctionArrayTypeContext -> {
                val funcTypeContext = typeContext.funcType()
                val returnType = resolveType(funcTypeContext.retType)
                val parameterTypes =
                    funcTypeContext.paramTypes?.type()?.map { resolveType(it as CymplParser.TypeContext) }
                        ?: emptyList()
                BuiltinType.ARRAY(BuiltinType.FUNCTION(parameterTypes, returnType))
            }

            is ArrayTypeContext -> {
                val elementTypeContext = typeContext.type()
                val elementType = resolveType(elementTypeContext)
                BuiltinType.ARRAY(elementType)
            }

            else -> {
                val location = TokenLocation(typeContext.start.line, typeContext.start.charPositionInLine)
                throw SyntaxException("unknown type ${typeContext.text}", location)
            }
        }
    }
}
