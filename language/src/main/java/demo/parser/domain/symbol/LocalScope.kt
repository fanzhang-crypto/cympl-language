package demo.parser.domain.symbol

class LocalScope(parent: Scope?) : BaseScope(parent) {
    override val scopeName: String = "locals"
}
