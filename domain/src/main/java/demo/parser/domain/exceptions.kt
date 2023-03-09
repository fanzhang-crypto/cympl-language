package demo.parser.domain

class TokenLocation(private val line: Int, private val position: Int) : Comparable<TokenLocation> {

    companion object {
        private val COMPARATOR = compareBy<TokenLocation> { it.line }.thenBy { it.position }
    }

    override fun compareTo(other: TokenLocation): Int {
        return COMPARATOR.compare(this, other)
    }

    override fun toString() = "($line:$position)"
}

interface Located : Comparable<Located> {
    val location: TokenLocation
    override fun compareTo(other: Located): Int = location.compareTo(other.location)
}

sealed class ParseException(message: String) : RuntimeException(message), Located

class SemanticException(message: String, override val location: TokenLocation) :
    ParseException("semantic error at $location: $message")

class SyntaxException(message: String, override val location: TokenLocation) :
    ParseException("syntax error at $location: $message")
