package demo.parser.domain

class TokenLocation(private val line: Int, private val position: Int) {
    override fun toString() = "($line:$position)"
}

class SemanticException : RuntimeException {

    constructor(message: String, location: TokenLocation)
            : super("semantic error at $location: $message")

    constructor(message: String)
            : super("semantic error: $message")
}

class SyntaxException(message: String, location: TokenLocation)
    : RuntimeException("syntax error at $location: $message")
