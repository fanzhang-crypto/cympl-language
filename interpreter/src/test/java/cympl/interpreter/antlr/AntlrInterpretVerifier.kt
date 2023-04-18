package cympl.interpreter.antlr

import cympl.parser.antlr.AntlrProgramParser
import cympl.interpreter.InterpretVerifier

object AntlrInterpretVerifier : InterpretVerifier() {
    override val parser = ::AntlrProgramParser
}
