package cympl.parser.antlr

import cympl.language.*
import cympl.parser.*
import cympl.parser.Parser
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNSimulator
import java.io.InputStream

class AntlrProgramParser : Parser<Program> {

    private val semanticChecker = SemanticChecker()
    private val programVisitor = AntlrToProgram(semanticChecker, semanticChecker)

    override fun parse(inputStream: InputStream): ParseResult<Program> {
        val errorListener = SyntaxErrorListener()

        val cs: CharStream = CharStreams.fromStream(inputStream)
        val lexer = CymplLexer(cs).apply { setErrorListener(errorListener) }

        val tokens = CommonTokenStream(lexer)
        val parser = CymplParser(tokens).apply { setErrorListener(errorListener) }

        val root = parser.prog()

        val syntaxErrors = errorListener.syntaxErrors
        if (syntaxErrors.isNotEmpty()) {
            return ParseResult.Failure(syntaxErrors)
        }

        val semanticErrors: List<SemanticException> = semanticChecker.check(root)
        if (semanticErrors.isNotEmpty()) {
            return ParseResult.Failure(semanticErrors)
        }

        val program = programVisitor.visit(root)
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
