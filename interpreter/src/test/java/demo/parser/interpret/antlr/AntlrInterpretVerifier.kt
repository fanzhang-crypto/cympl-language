package demo.parser.interpret.antlr

import demo.parser.antlr.AntlrProgramParser
import demo.parser.interpret.InterpretVerifier

object AntlrInterpretVerifier : InterpretVerifier() {
    override val parser = ::AntlrProgramParser
}
