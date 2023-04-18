package cympl.language.symbol

class LocalScope(parent: Scope?) : BaseScope(parent) {
    override val scopeName: String = "locals"
}
