package demo.parser.fp

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import demo.parser.domain.*

internal class ProgramGrammar(
    private val semanticChecker: SemanticChecker
) : Grammar<Program>() {

    private val FUNC by literalToken("func")
    private val RETURN by literalToken("return")
    private val IF by literalToken("if")
    private val ELSE by literalToken("else")
    private val WHILE by literalToken("while")
    private val FOR by literalToken("for")
    private val BREAK by literalToken("break")
    private val CONTINUE by literalToken("continue")

    private val VOID_TYPE by literalToken("VOID")
    private val BOOL_TYPE by literalToken("BOOL")
    private val INT_TYPE by literalToken("INT")
    private val FLOAT_TYPE by literalToken("FLOAT")
    private val STRING_TYPE by literalToken("STRING")

    private val TRUE by literalToken("true")
    private val FALSE by literalToken("false")
    private val BOOL by TRUE or FALSE

    private val ID by regexToken("[a-z][a-zA-Z0-9_]*")

    private val FLOAT by regexToken("0.0|-?[1-9][0-9]*\\.[0-9]+")
    private val INT by regexToken("0|-?[1-9][0-9]*")

    private val STRING by regexToken("\"[^\"]*\"")


    private val WS by regexToken("\\s+", ignore = true)
    private val COMMENT by regexToken("//[^\r\n]*", ignore = true)
    private val NEWLINE by regexToken("[\r\n]+", ignore = true)

    private val LPR by literalToken("(")
    private val RPR by literalToken(")")
    private val LBR by literalToken("{")
    private val RBR by literalToken("}")
    private val COLON by literalToken(":")
    private val SEMICOLON by literalToken(";")
    private val COMMA by literalToken(",")

    private val PLUS by literalToken("+")
    private val MINUS by literalToken("-")
    private val TIMES by literalToken("*")
    private val DIV by literalToken("/")
    private val REM by literalToken("%")
    private val POW by literalToken("^")

    private val EQ by literalToken("==")
    private val NEQ by literalToken("!=")
    private val GTE by literalToken(">=")
    private val GT by literalToken(">")
    private val LTE by literalToken("<=")
    private val LT by literalToken("<")
    private val ASSIGN by literalToken("=")

    private val NOT by literalToken("!")
    private val AND by literalToken("&&")
    private val OR by literalToken("||")

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
            "==" -> Expression.Equality(l, r)
            "!=" -> Expression.Inequality(l, r)
            ">" -> Expression.GreaterThan(l, r)
            "<" -> Expression.LessThan(l, r)
            ">=" -> Expression.GreaterThanOrEqual(l, r)
            "<=" -> Expression.LessThanOrEqual(l, r)
            else -> throw IllegalArgumentException("unknown operator $op")
        }
    }

    private val addOrSubChain: Parser<Expression> by leftAssociative(
        parser(this::mulOrDivChain),
        PLUS or MINUS
    ) { l, op, r ->
        when (op.text) {
            "+" -> Expression.Addition(l, r)
            "-" -> Expression.Subtraction(l, r)
            else -> throw IllegalArgumentException("unknown operator $op")
        }
    }

    private val mulOrDivChain: Parser<Expression> by leftAssociative(parser(this::powChain), TIMES or DIV or REM) { l, op, r ->
        when (op.text) {
            "*" -> Expression.Multiplication(l, r)
            "/" -> Expression.Division(l, r)
            "%" -> Expression.Remainder(l, r)
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

    private val functionCall by (ID * -LPR * exprList * -RPR)
        .map { (idToken, e) -> Expression.FunctionCall(idToken.text, e) }

    private val variable by ID.map { idToken ->
        val id = idToken.text
        val location = TokenLocation(idToken.row, idToken.column - 1)
        semanticChecker.checkVariableDeclared(id, location)
        Expression.Variable(id)
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

    private val assign: Parser<Statement> by (ID * -ASSIGN * expression * -SEMICOLON).map { (idToken, e) ->
        val id = idToken.text
        val location = TokenLocation(idToken.row, idToken.column - 1)
        semanticChecker.checkVariableDeclared(id, location)

        Statement.Assignment(id, e)
    }

    private val type = (BOOL_TYPE or INT_TYPE or FLOAT_TYPE or STRING_TYPE or VOID_TYPE).map {
        when (it.text) {
            "VOID" -> VariableType.VOID
            "BOOL" -> VariableType.BOOL
            "INT" -> VariableType.INT
            "FLOAT" -> VariableType.FLOAT
            "STRING" -> VariableType.STRING
            else -> throw IllegalArgumentException("unknown type ${it.text}")
        }
    }

    private val variableDecl: Parser<Statement>
            by (ID * -COLON * parser(this::type) * -ASSIGN * expression * -SEMICOLON)
                .map { (idToken, type, e) ->
                    val id = idToken.text
                    val location = TokenLocation(idToken.row, idToken.column - 1)
                    semanticChecker.checkVariableUndeclared(id, type, location)

                    Statement.VariableDeclaration(idToken.text, type, e)
                }

    private val exprStat: Parser<Statement> by (expression * -SEMICOLON).map {
        Statement.ExpressionStatement(it)
    }

    private val returnStat by (-RETURN * expression * -SEMICOLON).map { Statement.Return(it) }

    private val blockInSameScope: Parser<Statement.Block>
            by (-LBR * zeroOrMore(parser(this::statement)) * -RBR)
                .map { Statement.Block(it) }

    private val block: Parser<Statement.Block>
            by (-LBR.map { semanticChecker.openScope() } * zeroOrMore(parser(this::statement)) * -RBR.map { semanticChecker.closeScope() })
                .map { Statement.Block(it) }

    private val parameter: Parser<Statement.VariableDeclaration> by (ID * -COLON * parser(this::type))
        .map { (idToken, type) ->
            val id = idToken.text
            val location = TokenLocation(idToken.row, idToken.column - 1)
            semanticChecker.checkVariableUndeclared(id, type, location)
            Statement.VariableDeclaration(id, type)
        }

    private val parameterList: Parser<List<Statement.VariableDeclaration>>
            by (parameter * zeroOrMore(-COMMA * parameter))
                .map { (first, rest) ->
                    listOf(first) + rest
                }

    private val functionID by parser(this::ID).map { idToken ->
        idToken.text.also {
            val location = TokenLocation(idToken.row, idToken.column - 1)
            semanticChecker.checkFunctionUndeclared(it, location)
            semanticChecker.openScope()
        }
    }

    private val functionDecl: Parser<Statement>
            by (-FUNC * functionID * -LPR * optional(parameterList) * -RPR * -COLON * type * blockInSameScope)
                .map { (id, params, type, body) ->
                    semanticChecker.closeScope()
                    Statement.FunctionDeclaration(id, type, params ?: emptyList(), body)
                }

    private val ifStat: Parser<Statement> by (-IF * -LPR * expression * -RPR * parser(this::statement) * optional(
        -ELSE * parser(
            this::statement
        )
    ))
        .map { (cond, thenBlock, elseBlock) -> Statement.If(cond, thenBlock, elseBlock) }

    private val whileStat: Parser<Statement> by (-WHILE * -LPR * expression * -RPR * parser(this::statement))
        .map { (cond, body) -> Statement.While(cond, body) }

    private val breakStat: Parser<Statement> by (BREAK * SEMICOLON).asJust(Statement.Break())

    private val continueStat: Parser<Statement> by (CONTINUE * SEMICOLON).asJust(Statement.Continue())

    private val statement: Parser<Statement> by functionDecl or
            variableDecl or
            assign or
            exprStat or
            returnStat or
            ifStat or
            whileStat or
            breakStat or
            continueStat or
            block

    private val prog: Parser<Program> by oneOrMore(statement).map { Program(it) }

    override val rootParser: Parser<Program> by prog

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val input = """
            while (i < 10) {
                if (i % 2 == 0) {
                    i = i + 1;
                    continue;
                }
                x = x + i;
                i = i + 1;
            }
            """.trimIndent()

            val semanticChecker = SemanticChecker()
            val program = ProgramGrammar(semanticChecker).parseToEnd(input)
            program.statements.forEach(::println)
        }
    }
}
