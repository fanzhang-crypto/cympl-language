package cympl.parser.antlr

import cympl.language.*

interface TypeResolver {

    fun resolveType(node: CymplParser.ExprContext): BuiltinType

    fun resolveType(typeContext: CymplParser.TypeContext?): BuiltinType
}
