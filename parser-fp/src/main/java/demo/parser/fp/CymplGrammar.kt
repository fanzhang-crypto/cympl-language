package demo.parser.fp

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import demo.parser.domain.*

internal class CymplGrammar(
    private val semanticChecker: SemanticChecker
) : Grammar<Program>() {

    companion object Symbols {
        const val FUNC = "func"
        const val RETURN = "return"
        const val IF = "if"
        const val ELSE = "else"
        const val WHILE = "while"
        const val FOR = "for"
        const val BREAK = "break"
        const val CONTINUE = "continue"
        const val VOID_TYPE = "VOID"
        const val BOOL_TYPE = "BOOL"
        const val INT_TYPE = "INT"
        const val FLOAT_TYPE = "FLOAT"
        const val STRING_TYPE = "STRING"
        const val TRUE = "true"
        const val FALSE = "false"
        const val LPR = "("
        const val RPR = ")"
        const val LBR = "{"
        const val RBR = "}"
        const val COLON = ":"
        const val SEMICOLON = ";"
        const val COMMA = ","
        const val ASSIGN = "="
        const val PLUS = "+"
        const val MINUS = "-"
        const val MUL = "*"
        const val DIV = "/"
        const val MOD = "%"
        const val CARET = "^"
        const val AND = "&&"
        const val OR = "||"
        const val NOT = "!"
        const val EQ = "=="
        const val NEQ = "!="
        const val LT = "<"
        const val GT = ">"
        const val LEQ = "<="
        const val GEQ = ">="
    }

    private val FUNC by literalToken(Symbols.FUNC)
    private val RETURN by literalToken(Symbols.RETURN)
    private val IF by literalToken(Symbols.IF)
    private val ELSE by literalToken(Symbols.ELSE)
    private val WHILE by literalToken(Symbols.WHILE)
    private val FOR by literalToken(Symbols.FOR)
    private val BREAK by literalToken(Symbols.BREAK)
    private val CONTINUE by literalToken(Symbols.CONTINUE)

    private val VOID_TYPE by literalToken(Symbols.VOID_TYPE)
    private val BOOL_TYPE by literalToken(Symbols.BOOL_TYPE)
    private val INT_TYPE by literalToken(Symbols.INT_TYPE)
    private val FLOAT_TYPE by literalToken(Symbols.FLOAT_TYPE)
    private val STRING_TYPE by literalToken(Symbols.STRING_TYPE)

    private val TRUE by literalToken(Symbols.TRUE)
    private val FALSE by literalToken(Symbols.FALSE)
    private val BOOL by TRUE or FALSE

    private val ID by regexToken("[a-z][a-zA-Z0-9_]*")

    private val FLOAT by regexToken("0.0|[1-9][0-9]*\\.[0-9]+")
    private val INT by regexToken("0|[1-9][0-9]*")

    private val STRING by regexToken("\"[^\"]*\"")

    private val WS by regexToken("\\s+", ignore = true)
    private val COMMENT by regexToken("//[^\r\n]*", ignore = true)
    private val NEWLINE by regexToken("[\r\n]+", ignore = true)

    private val LPR by literalToken(Symbols.LPR)
    private val RPR by literalToken(Symbols.RPR)
    private val LBR by literalToken(Symbols.LBR)
    private val RBR by literalToken(Symbols.RBR)
    private val COLON by literalToken(Symbols.COLON)
    private val SEMICOLON by literalToken(Symbols.SEMICOLON)
    private val COMMA by literalToken(Symbols.COMMA)

    private val PLUS by literalToken(Symbols.PLUS)
    private val MINUS by literalToken(Symbols.MINUS)
    private val TIMES by literalToken(Symbols.MUL)
    private val DIV by literalToken(Symbols.DIV)
    private val REM by literalToken(Symbols.MOD)
    private val POW by literalToken(Symbols.CARET)

    private val EQ by literalToken(Symbols.EQ)
    private val NEQ by literalToken(Symbols.NEQ)
    private val GTE by literalToken(Symbols.GEQ)
    private val GT by literalToken(Symbols.GT)
    private val LTE by literalToken(Symbols.LEQ)
    private val LT by literalToken(Symbols.LT)
    private val ASSIGN by literalToken(Symbols.ASSIGN)

    private val NOT by literalToken(Symbols.NOT)
    private val AND by literalToken(Symbols.AND)
    private val OR by literalToken(Symbols.OR)

    private val expression by parser(this::logicalOrChain)

    private val logicalOrChain: Parser<Expression> by leftAssociative(parser(this::logicalAndChain), OR) { l, _, r ->
        Expression.Or(l, r)
    }

    private val logicalAndChain: Parser<Expression> by leftAssociative(parser(this::comparison), AND) { l, _, r ->
        Expression.And(l, r)
    }

    private val logicalNot: Parser<Expression> by (-NOT * parser(this::term))
        .map { e -> Expression.Not(e) }

    private val comparison by leftAssociative(
        parser(this::addOrSubChain),
        EQ or NEQ or GT or GTE or LT or LTE
    ) { l, op, r ->
        when (op.text) {
            Symbols.EQ -> Expression.Equality(l, r)
            Symbols.NEQ -> Expression.Inequality(l, r)
            Symbols.GT -> Expression.GreaterThan(l, r)
            Symbols.LT -> Expression.LessThan(l, r)
            Symbols.GEQ -> Expression.GreaterThanOrEqual(l, r)
            Symbols.LEQ -> Expression.LessThanOrEqual(l, r)
            else -> throw IllegalArgumentException("unknown operator $op")
        }
    }

    private val addOrSubChain: Parser<Expression> by leftAssociative(
        parser(this::mulOrDivChain),
        PLUS or MINUS
    ) { l, op, r ->
        when (op.text) {
            Symbols.PLUS -> Expression.Addition(l, r)
            Symbols.MINUS -> Expression.Subtraction(l, r)
            else -> throw IllegalArgumentException("unknown operator $op")
        }
    }

    private val mulOrDivChain: Parser<Expression> by leftAssociative(
        parser(this::powChain), TIMES or DIV or REM
    ) { l, op, r ->
        when (op.text) {
            Symbols.MUL -> Expression.Multiplication(l, r)
            Symbols.DIV -> Expression.Division(l, r)
            Symbols.MOD -> Expression.Remainder(l, r)
            else -> throw IllegalArgumentException("unknown operator $op")
        }
    }

    private val powChain: Parser<Expression> by rightAssociative(parser(this::term), POW) { l, _, r ->
        Expression.Power(l, r)
    }

    private val parenthesized: Parser<Expression> by (-LPR * expression * -RPR)
        .map { e -> Expression.Parenthesized(e) }

    private val negation: Parser<Expression> by (-MINUS * parser { term })
        .map { e -> Expression.Negation(e) }

    private val exprList: Parser<List<Expression>>
            by (expression * zeroOrMore(-COMMA * expression))
                .map { (first, rest) -> listOf(first) + rest }

    private val functionCall by (ID * -LPR * optional(exprList) * -RPR)
        .map { (idToken, e) ->
            semanticChecker.checkFunctionRef(idToken)
            Expression.FunctionCall(idToken.text, e ?: emptyList())
        }

    private val variable by ID.map { idToken ->
        semanticChecker.checkVariableRef(idToken)
        Expression.Variable(idToken.text)
    }

    // @formatter:off
    private val term: Parser<Expression> by
        BOOL.map { Expression.Bool(it.text.toBoolean()) } or
        INT.map { Expression.Int(it.text.toInt()) } or
        FLOAT.map { Expression.Float(it.text.toDouble()) } or
        STRING.map { Expression.String(it.text.substring(1, it.text.length - 1)) } or
        functionCall or
        variable or
        negation or
        logicalNot or
        parenthesized
    // @formatter:on

    private val assign: Parser<Statement> by (ID * -ASSIGN * expression).map { (idToken, e) ->
        semanticChecker.checkVariableRef(idToken)
        Statement.Assignment(idToken.text, e)
    }

    private val type = (BOOL_TYPE or INT_TYPE or FLOAT_TYPE or STRING_TYPE or VOID_TYPE)
        .map(TypeResolver::resolveType)

    private val variableDecl: Parser<Statement>
            by (ID * -COLON * parser(this::type) * -ASSIGN * expression)
                .map { (idToken, type, e) ->
                    val id = idToken.text
                    semanticChecker.defineVar(idToken, type)
                    Statement.VariableDeclaration(id, type, e)
                }

    private val exprStat: Parser<Statement> by (expression * -SEMICOLON).map {
        Statement.ExpressionStatement(it)
    }

    private val returnStat by (-RETURN * expression * -SEMICOLON).map { Statement.Return(it) }

    private val block: Parser<Statement.Block> by (
            -LBR.map { semanticChecker.enterBlock() }
                    * zeroOrMore(parser(this::statement))
                    * -RBR.map { semanticChecker.exitBlock() }
            ).map { Statement.Block(it) }

    private val parameter by (ID * -COLON * parser(this::type))

    private val parameterList by (parameter * zeroOrMore(-COMMA * parameter))
        .map { (first, rest) -> listOf(first) + rest }

    private val functionHeader: Parser<FunctionHeader> by (-FUNC * ID * -LPR * optional(parameterList) * -RPR * -COLON * type)
        .map { (idToken, paramIdAndTypes, type) ->
            semanticChecker.enterFuncDecl(idToken, type, paramIdAndTypes)

            val parameters = paramIdAndTypes?.map { (idToken, type) ->
                semanticChecker.defineVar(idToken, type)
                Statement.VariableDeclaration(idToken.text, type)
            } ?: emptyList()

            FunctionHeader(idToken.text, parameters, type)
        }

    private data class FunctionHeader(
        val name: String,
        val parameters: List<Statement.VariableDeclaration>,
        val returnType: BuiltinType
    )

    private val functionDecl: Parser<Statement> by (functionHeader * block)
        .map { (header, body) ->
            semanticChecker.exitFuncDecl()
            Statement.FunctionDeclaration(header.name, header.returnType, header.parameters, body)
        }

    private val ifStat: Parser<Statement>
            by (-IF * -LPR * expression * -RPR * parser(this::statement) * optional(-ELSE * parser(this::statement)))
                .map { (cond, thenBlock, elseBlock) -> Statement.If(cond, thenBlock, elseBlock) }

    private val whileStat: Parser<Statement>
            by (-WHILE * -LPR * expression * -RPR * parser(this::statement))
                .map { (cond, body) -> Statement.While(cond, body) }

    private val breakStat: Parser<Statement> by (BREAK * SEMICOLON).asJust(Statement.Break())

    private val continueStat: Parser<Statement> by (CONTINUE * SEMICOLON).asJust(Statement.Continue())

    private val forStat: Parser<Statement> by (
            -FOR * -LPR *
                    optional((variableDecl or assign)) *
                    -SEMICOLON *
                    optional(expression) *
                    -SEMICOLON *
                    optional(assign) *
                    -RPR *
                    parser(this::statement)
            ).map { (init, cond, update, body) -> Statement.For(init, cond, update, body) }

    private val statement: Parser<Statement> by functionDecl or
            (variableDecl * -SEMICOLON) or
            (assign * -SEMICOLON) or
            exprStat or
            returnStat or
            ifStat or
            whileStat or
            forStat or
            breakStat or
            continueStat or
            block

    private val prog: Parser<Program> by oneOrMore(statement).map { Program(it) }

    override val rootParser: Parser<Program> by prog
}

fun main() {
    val input = """
        func main(n:INT):INT { 
            a(); 
        }
    """.trimIndent()

    val semanticChecker = SemanticChecker()
    val program = CymplGrammar(semanticChecker).parseToEnd(input)
    program.statements.forEach(::println)
}
