package demo.parser.app

import demo.parser.domain.Parser
import demo.parser.domain.Program
import demo.parser.fp.FpProgramParser

object ParserFactory {

    fun getParser(): Parser<Program> = FpProgramParser()
}
