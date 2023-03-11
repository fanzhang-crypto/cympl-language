package demo.parser.app.antlr

import demo.parser.antlr.AntlrProgramParser
import demo.parser.app.InterpretVerifier

object AntlrInterpretVerifier : InterpretVerifier() {
    override val parser = ::AntlrProgramParser
}
