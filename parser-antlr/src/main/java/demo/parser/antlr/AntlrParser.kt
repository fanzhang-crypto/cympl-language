package demo.parser.antlr

import ExprLexer
import ExprParser
import demo.parser.domain.Parser
import demo.parser.domain.ParserResult
import demo.parser.domain.SyntaxException
import demo.parser.domain.TokenLocation
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNSimulator
import org.antlr.v4.runtime.tree.ParseTree
import java.io.InputStream

class AntlrParser: Parser {

    private val programVisitor = AntlrToProgram()

    override fun parse(inputStream: InputStream): ParserResult {
        val errorListener = SyntaxErrorListener()

        val cs: CharStream = CharStreams.fromStream(inputStream)
        val lexer = ExprLexer(cs).setErrorListener(errorListener) as ExprLexer

        val tokens = CommonTokenStream(lexer)
        val parser = ExprParser(tokens).setErrorListener(errorListener) as ExprParser

        val programAST: ParseTree = parser.prog()

        val syntaxErrors = errorListener.syntaxErrors
        if (syntaxErrors.isNotEmpty()) {
            return ParserResult.Failure(syntaxErrors)
        }

        val program = programVisitor.visit(programAST)

        val semanticErrors = programVisitor.getSemanticErrors()
        if (semanticErrors.isNotEmpty()) {
            programVisitor.clearSemanticErrors()
            return ParserResult.Failure(semanticErrors)
        }

        return ParserResult.Success(program)
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
