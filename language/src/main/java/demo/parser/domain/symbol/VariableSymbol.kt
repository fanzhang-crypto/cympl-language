package demo.parser.domain.symbol

import demo.parser.domain.BuiltinType

class VariableSymbol(name: String, type: BuiltinType, scope: Scope?) : Symbol(name, type, scope)
