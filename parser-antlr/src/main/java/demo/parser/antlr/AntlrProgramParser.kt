package demo.parser.antlr

import CymplLexer
import CymplParser
import demo.parser.domain.*
import demo.parser.domain.Parser
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNSimulator
import org.antlr.v4.runtime.tree.ParseTree
import java.io.InputStream

class AntlrProgramParser: Parser<Program> {

    private val programVisitor = AntlrToProgram()
    private val semanticChecker = SemanticChecker()

    override fun parse(inputStream: InputStream): ParseResult<Program> {
        val errorListener = SyntaxErrorListener()

        val cs: CharStream = CharStreams.fromStream(inputStream)
        val lexer = CymplLexer(cs).setErrorListener(errorListener) as CymplLexer

        val tokens = CommonTokenStream(lexer)
        val parser = CymplParser(tokens).setErrorListener(errorListener) as CymplParser

        val programAST: ParseTree = parser.prog()

        val syntaxErrors = errorListener.syntaxErrors
        if (syntaxErrors.isNotEmpty()) {
            return ParseResult.Failure(syntaxErrors)
        }

        val semanticErrors:List<SemanticException> = semanticChecker.check(programAST)
        if (semanticErrors.isNotEmpty()) {
            return ParseResult.Failure(semanticErrors)
        }

        val program = programVisitor.visit(programAST)
        return ParseResult.Success(program)
    }

    private fun <S, A : ATNSimulator> Recognizer<S, A>.setErrorListener(listener: ANTLRErrorListener) = apply {
        removeErrorListeners()
        addErrorListener(listener)
    }

    private class SyntaxErrorListener : BaseErrorListener() {

        val syntaxErrors = mutableListOf<SyntaxException>()

        override fun syntaxError(
            recognizer: Recognizer<*, *>,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?
        ) {
            syntaxErrors += SyntaxException(msg, TokenLocation(line, charPositionInLine))
        }
    }
}
