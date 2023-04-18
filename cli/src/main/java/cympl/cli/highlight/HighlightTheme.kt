package cympl.cli.highlight

import cympl.cli.highlight.AttributedStyles.BOLD_BLUE
import cympl.cli.highlight.AttributedStyles.CYAN
import cympl.cli.highlight.AttributedStyles.GREEN
import cympl.cli.highlight.AttributedStyles.ITALIC_BRIGHT
import cympl.cli.highlight.AttributedStyles.RED
import cympl.cli.highlight.AttributedStyles.WHITE
import cympl.cli.highlight.AttributedStyles.YELLOW
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
