package cympl.parser.antlr

import org.antlr.v4.runtime.CharStreams

object LexicalAnalyzer {

    interface TokenListener {
        fun onKeyword(keywordText: String, start: Int, end: Int)
        fun onQuote(quoteText: String, start: Int, end: Int)
        fun onIdentifier(idText: String, start: Int, end: Int)
        fun onComment(commentText: String, start: Int, end: Int)
        fun onNumber(numberText: String, start: Int, end: Int)
        fun onDefault(defaultText: String, start: Int, end: Int)
    }

    fun analyze(input: String, listener: TokenListener) {
        val cs = CharStreams.fromString(input)
        val lexer = CymplLexer(cs).also { it.removeErrorListeners() }
        try {
            lexer.allTokens.map { token ->
                val type = token.type
                val text = token.text
                when (type) {
                    CymplLexer.INT,
                    CymplLexer.FLOAT -> listener.onNumber(text, token.startIndex, token.stopIndex)

                    CymplLexer.STRING -> listener.onQuote(text, token.startIndex, token.stopIndex)

                    CymplLexer.INT_TYPE,
                    CymplLexer.FLOAT_TYPE,
                    CymplLexer.STRING_TYPE,
                    CymplLexer.BOOL_TYPE,
                    CymplLexer.VOID_TYPE,
                    CymplLexer.RETURN,
                    CymplLexer.BREAK,
                    CymplLexer.CONTINUE,
                    CymplLexer.IF,
                    CymplLexer.ELSE,
                    CymplLexer.WHILE,
                    CymplLexer.FOR,
                    CymplLexer.TRUE,
                    CymplLexer.FALSE -> listener.onKeyword(text, token.startIndex, token.stopIndex)

                    CymplLexer.ID -> listener.onIdentifier(text, token.startIndex, token.stopIndex)
                    CymplLexer.COMMENT -> listener.onComment(text, token.startIndex, token.stopIndex)

                    else -> listener.onDefault(text, token.startIndex, token.stopIndex)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }

}
