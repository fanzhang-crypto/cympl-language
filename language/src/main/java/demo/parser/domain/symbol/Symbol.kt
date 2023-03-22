package demo.parser.domain.symbol

import demo.parser.domain.BuiltinType

sealed class Symbol(val name: String, val type: BuiltinType, var scope: Scope? = null) {
    override fun toString() = "<$name:$type>"
}

class VariableSymbol(name: String, type: BuiltinType, scope: Scope?) : Symbol(name, type, scope)

class FunctionSymbol(
    name: String,
    returnType: BuiltinType,
    val parameters: List<VariableSymbol>,
    override val enclosingScope: Scope?
) : Symbol(name, returnType), Scope {

    private val locals: MutableMap<String, Symbol> = LinkedHashMap<String, Symbol>().also {
        parameters.forEach { arg -> it[arg.name] = arg }
    }

    override fun define(symbol: Symbol) {
        locals[symbol.name] = symbol
        symbol.scope = this
    }

    override fun resolve(name: String): Symbol? =
        locals[name] ?: enclosingScope?.resolve(name)

    override fun remove(text: String) {
        throw UnsupportedOperationException("cannot remove argument from function")
    }

    override val scopeName: String = name

    override fun toString(): String = "function${super.toString()}:${locals.values}"
}

object IntrinsicSymbols {
    val printLine = FunctionSymbol(
        "println",
        BuiltinType.VOID,
        listOf(VariableSymbol("str", BuiltinType.ANY, null)),
        null
    )

    val readLine = FunctionSymbol(
        "readln",
        BuiltinType.STRING,
        listOf(VariableSymbol("prompt", BuiltinType.STRING, null)),
        null
    )
}


