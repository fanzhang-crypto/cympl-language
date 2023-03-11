package demo.parser.app.fp

import demo.parser.app.InterpretVerifier
import demo.parser.fp.FpProgramParser

object FpInterpretVerifier : InterpretVerifier() {
    override val parser = ::FpProgramParser
}
