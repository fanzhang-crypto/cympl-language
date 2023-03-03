package demo.parser.domain

class TokenLocation(private val line: Int, private val position: Int) {
    override fun toString() = "($line:$position)"
}

sealed class ParseException(message: String): RuntimeException(message)

class SemanticException : ParseException {

    constructor(message: String, location: TokenLocation)
            : super("semantic error at $location: $message")

    constructor(message: String)
            : super("semantic error: $message")
}

class SyntaxException(message: String, location: TokenLocation)
    : ParseException("syntax error at $location: $message")
