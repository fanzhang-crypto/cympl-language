package cympl.cli.highlight

import cympl.parser.antlr.LexicalAnalyzer
import org.jline.reader.LineReader
import org.jline.reader.impl.DefaultHighlighter
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle

class SyntaxHighlighter(
    private val symbols: () -> Collection<String>,
    private val theme: HighlightTheme = HighlightTheme() //use default theme
) : DefaultHighlighter() {

    private inner class TokenStyler(input: String) : LexicalAnalyzer.TokenListener {

        private val styleByRange = LinkedHashMap<IntRange, AttributedStyle>()

        init {
            LexicalAnalyzer.analyze(input, this)
        }

        override fun onKeyword(keywordText: String, start: Int, end: Int) {
            styleByRange[start..end] = theme.keywordStyle
        }

        override fun onQuote(quoteText: String, start: Int, end: Int) {
            styleByRange[start..end] = theme.quotedStyle
        }

        override fun onIdentifier(idText: String, start: Int, end: Int) {
            if (symbols().contains(idText)) {
                styleByRange[start..end] = theme.knownIdentifierStyle
            } else {
                styleByRange[start..end] = theme.unknownIdentifierStyle
            }
        }

        override fun onComment(commentText: String, start: Int, end: Int) {
            styleByRange[start..end] = theme.commentStyle
        }

        override fun onNumber(numberText: String, start: Int, end: Int) {
            styleByRange[start..end] = theme.numberStyle
        }

        override fun onDefault(defaultText: String, start: Int, end: Int) {
            styleByRange[start..end] = theme.defaultStyle
        }

        fun getStyleOfChar(pos: Int): AttributedStyle {
            return styleByRange.entries
                .firstOrNull { pos in it.key }
                ?.value
                ?: theme.defaultStyle
        }
    }

    override fun highlight(reader: LineReader, buffer: String): AttributedString {
        val tokenStyler = TokenStyler(buffer)

        var lastStyle = AttributedStyle.DEFAULT
        val sb = AttributedStringBuilder().style(lastStyle)
        for (i in buffer.indices) {
            val style = tokenStyler.getStyleOfChar(i)
            if (style != lastStyle) {
                sb.style(style)
                lastStyle = style
            }
            sb.append(buffer[i])
        }
        return sb.toAttributedString()
    }
}
