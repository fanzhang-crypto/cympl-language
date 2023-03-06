package demo.parser.antlr

import ExprLexer
import ExprParser
import demo.parser.domain.*
import demo.parser.domain.Parser
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNSimulator
import org.antlr.v4.runtime.tree.ParseTree
import java.io.InputStream

class AntlrProgramParser: Parser<Program> {

    private val semanticChecker = SemanticChecker()
    private val programVisitor = AntlrToProgram(semanticChecker)

    override fun parse(inputStream: InputStream): ParseResult<Program> {
        val errorListener = SyntaxErrorListener()

        val cs: CharStream = CharStreams.fromStream(inputStream)
        val lexer = ExprLexer(cs).setErrorListener(errorListener) as ExprLexer

        val tokens = CommonTokenStream(lexer)
        val parser = ExprParser(tokens).setErrorListener(errorListener) as ExprParser

        val programAST: ParseTree = parser.prog()

        val syntaxErrors = errorListener.syntaxErrors
        if (syntaxErrors.isNotEmpty()) {
            return ParseResult.Failure(syntaxErrors)
        }

        val program = programVisitor.visit(programAST)

        val semanticErrors = semanticChecker.getSemanticErrors()
        if (semanticErrors.isNotEmpty()) {
            semanticChecker.clearSemanticErrors()
            return ParseResult.Failure(semanticErrors)
        }

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
