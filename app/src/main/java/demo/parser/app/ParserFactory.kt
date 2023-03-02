package demo.parser.app

import demo.parser.antlr.AntlrParser
import demo.parser.domain.Parser
import demo.parser.fp.FpParser

object ParserFactory {

    fun getParser(): Parser = FpParser
}
