package demo.parser.app.highlight

import demo.parser.app.highlight.AttributedStyles.BOLD_BLUE
import demo.parser.app.highlight.AttributedStyles.CYAN
import demo.parser.app.highlight.AttributedStyles.GREEN
import demo.parser.app.highlight.AttributedStyles.ITALIC_BRIGHT
import demo.parser.app.highlight.AttributedStyles.RED
import demo.parser.app.highlight.AttributedStyles.WHITE
import demo.parser.app.highlight.AttributedStyles.YELLOW
import org.jline.utils.AttributedStyle

data class HighlightTheme(
    val keywordStyle: AttributedStyle = BOLD_BLUE,
    val quotedStyle: AttributedStyle = GREEN,
    val knownIdentifierStyle: AttributedStyle = CYAN,
    val unknownIdentifierStyle: AttributedStyle = RED,
    val commentStyle: AttributedStyle = ITALIC_BRIGHT,
    val numberStyle: AttributedStyle = YELLOW,
    val defaultStyle: AttributedStyle = WHITE
)
