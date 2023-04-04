package demo.parser.antlr

import demo.parser.domain.symbol.LambdaScope

interface ScopeResolver {

    fun resolveScope(ctx: CymplParser.LambdaContext): LambdaScope
}
