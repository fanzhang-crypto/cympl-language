package cympl.language.symbol

import cympl.language.BuiltinType

class VariableSymbol(name: String, type: BuiltinType, scope: Scope?) : Symbol(name, type, scope)
