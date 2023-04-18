package cympl.interpreter.fp

import cympl.interpreter.InterpretVerifier
import cympl.parser.fp.FpProgramParser

object FpInterpretVerifier : InterpretVerifier() {
    override val parser = ::FpProgramParser
}
