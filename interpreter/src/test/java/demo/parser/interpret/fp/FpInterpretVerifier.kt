package demo.parser.interpret.fp

import demo.parser.interpret.InterpretVerifier
import demo.parser.fp.FpProgramParser

object FpInterpretVerifier : InterpretVerifier() {
    override val parser = ::FpProgramParser
}
