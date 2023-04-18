package cympl.parser.antlr

import cympl.language.symbol.LambdaScope

interface ScopeResolver {

    fun resolveScope(ctx: CymplParser.LambdaContext): LambdaScope
}
