package cympl.cli

import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle

internal fun String.fg(color: Int): AttributedString =
    AttributedString(this, AttributedStyle.DEFAULT.foreground(color))
