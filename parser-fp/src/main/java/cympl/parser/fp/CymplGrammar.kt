package cympl.parser.fp

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import cympl.language.*

internal class CymplGrammar(
    private val semanticChecker: SemanticChecker
) : Grammar<Program>() {

    private val RETURN by literalToken(Symbols.RETURN)
    private val IF by literalToken(Symbols.IF)
    private val ELSE by literalToken(Symbols.ELSE)
    private val WHILE by literalToken(Symbols.WHILE)
    private val FOR by literalToken(Symbols.FOR)
    private val BREAK by literalToken(Symbols.BREAK)
    private val CONTINUE by literalToken(Symbols.CONTINUE)
    private val SWITCH by literalToken(Symbols.SWITCH)
    private val CASE by literalToken(Symbols.CASE)
    private val DEFAULT by literalToken(Symbols.DEFAULT)

    private val VOID_TYPE by literalToken(Symbols.VOID_TYPE)
    private val BOOL_TYPE by literalToken(Symbols.BOOL_TYPE)
    private val INT_TYPE by literalToken(Symbols.INT_TYPE)
    private val FLOAT_TYPE by literalToken(Symbols.FLOAT_TYPE)
    private val STRING_TYPE by literalToken(Symbols.STRING_TYPE)
    private val TRUE by literalToken(Symbols.TRUE)
    private val FALSE by literalToken(Symbols.FALSE)
    private val BOOL_LITERAL by TRUE or FALSE
    private val FLOAT_LITERAL by regexToken("0.0|[1-9][0-9]*\\.[0-9]+")
    private val INT_LITERAL by regexToken("0|[1-9][0-9]*")
    private val STRING_LITERAL by regexToken("\"[^\"]*\"")

    private val ID by regexToken("[a-z][a-zA-Z0-9_]*")

    private val WS by regexToken("\\s+", ignore = true)
    private val COMMENT by regexToken("//[^\r\n]*", ignore = true)
    private val NEWLINE by regexToken("[\r\n]+", ignore = true)

    private val LPR by literalToken(Symbols.LPR)
    private val RPR by literalToken(Symbols.RPR)
    private val LBRACE by literalToken(Symbols.LBRACE)
    private val RBRACE by literalToken(Symbols.RBRACE)
    private val LBRACKET by literalToken(Symbols.LBRACKET)
    private val RBRACKET by literalToken(Symbols.RBRACKET)
    private val COLON by literalToken(Symbols.COLON)
    private val SEMICOLON by literalToken(Symbols.SEMICOLON)
    private val COMMA by literalToken(Symbols.COMMA)
    private val DOT by literalToken(Symbols.DOT)

    private val INC by literalToken(Symbols.INC)
    private val DEC by literalToken(Symbols.DEC)

    private val PLUS by literalToken(Symbols.PLUS)
    private val MINUS by literalToken(Symbols.MINUS)
    private val TIMES by literalToken(MUL)
    private val DIV by literalToken(Symbols.DIV)
    private val REM by literalToken(MOD)
    private val POW by literalToken(CARET)

    private val EQ by literalToken(Symbols.EQ)
    private val NEQ by literalToken(Symbols.NEQ)
    private val GTE by literalToken(GEQ)
    private val GT by literalToken(Symbols.GT)
    private val LTE by literalToken(LEQ)
    private val LT by literalToken(Symbols.LT)
    private val ASSIGN by literalToken(Symbols.ASSIGN)

    private val NOT by literalToken(Symbols.NOT)
    private val AND by literalToken(Symbols.AND)
    private val OR by literalToken(Symbols.OR)

    private val expression by parser(this::logicalOrChain)

    private val arrayLiteral by (-LBRACKET * separatedTerms(expression, COMMA, acceptZero = true) * -RBRACKET)
        .map { Expression.ArrayLiteral(it) }

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
            GEQ -> Expression.GreaterThanOrEqual(l, r)
            LEQ -> Expression.LessThanOrEqual(l, r)
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
            MUL -> Expression.Multiplication(l, r)
            Symbols.DIV -> Expression.Division(l, r)
            MOD -> Expression.Remainder(l, r)
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
            val funcExpr = Expression.Variable(idToken.text, BuiltinType.VOID)
            Expression.FunctionCall(funcExpr, e ?: emptyList(), BuiltinType.VOID)
        }

    private val variable by ID.map { idToken ->
        semanticChecker.checkIDRef(idToken)
        Expression.Variable(idToken.text, BuiltinType.VOID)
    }

    private val arrayAccess: Parser<Expression.ArrayAccess> by (ID * oneOrMore(-LBRACKET * expression * -RBRACKET))
        .map { (idToken, indices) ->
            val array = Expression.Variable(idToken.text, BuiltinType.VOID)
            val initialArrayIndex = Expression.ArrayAccess(array, indices[0])
            indices.subList(1, indices.size).fold(initialArrayIndex) { acc, index ->
                Expression.ArrayAccess(acc, index)
            }
        }

    private val preIncrement: Parser<Expression> by (-INC * parser { term })
        .map { expr -> Expression.Increment(expr, false) }

    private val preDecrement: Parser<Expression> by (-DEC * parser { term })
        .map { expr -> Expression.Decrement(expr, false) }

    private val postIncrement: Parser<Expression> by ((arrayAccess or variable) * -INC)
        .map { expr -> Expression.Increment(expr, true) }

    private val postDecrement: Parser<Expression> by ((arrayAccess or variable) * -DEC)
        .map { expr -> Expression.Decrement(expr, true) }

    private val property: Parser<Expression.Property> by ((arrayAccess or variable) * oneOrMore(-DOT * ID))
        .map { (obj, properties) ->
            val initialProperty = Expression.Property(obj, properties[0].text, BuiltinType.INT)
            properties.subList(1, properties.size).fold(initialProperty) { acc, property ->
                Expression.Property(acc, property.text, BuiltinType.INT)
            }
        }

    // @formatter:off
    private val term: Parser<Expression> by
        BOOL_LITERAL.map { Expression.BoolLiteral(it.text.toBoolean()) } or
        INT_LITERAL.map { Expression.IntLiteral(it.text.toInt()) } or
        FLOAT_LITERAL.map { Expression.FloatLiteral(it.text.toDouble()) } or
        STRING_LITERAL.map { Expression.StringLiteral(it.text.substring(1, it.text.length - 1)) } or
        arrayLiteral or
        functionCall or
        property or

        preIncrement or
        preDecrement or
        postIncrement or
        postDecrement or
        arrayAccess or
        variable or
        negation or
        logicalNot or
        parenthesized
    // @formatter:on

    private val lvalue by (arrayAccess or variable)

    private val assign: Parser<Statement> by (lvalue * -ASSIGN * expression).map { (leftExpr, rightExpr) ->
        Statement.Assignment(leftExpr, rightExpr)
    }

    private val valueType by (BOOL_TYPE or INT_TYPE or FLOAT_TYPE or STRING_TYPE or VOID_TYPE)
        .map(TypeResolver::resolveValueType)

    private val arrayType by (valueType * oneOrMore(LBRACKET * RBRACKET))
        .map { (type, brackets) -> TypeResolver.resolveArrayType(type, brackets.size) }

    private val type by arrayType or valueType

    private val variableDecl: Parser<Statement>
            by (type * ID * -ASSIGN * expression)
                .map { (type, idToken, e) ->
                    val id = idToken.text
                    semanticChecker.defineVar(idToken, type)
                    Statement.VariableDeclaration(id, type, e)
                }

    private val exprStat: Parser<Statement> by (expression * -SEMICOLON).map {
        Statement.ExpressionStatement(it)
    }

    private val returnStat by (-RETURN * expression * -SEMICOLON).map { Statement.Return(it) }

    private val block: Parser<Statement.Block> by (
            -LBRACE.map { semanticChecker.enterBlock() }
                    * zeroOrMore(parser(this::statement))
                    * -RBRACE.map { semanticChecker.exitBlock() }
            ).map { Statement.Block(it) }

    private val parameter by type * ID

    private val parameterList by (parameter * zeroOrMore(-COMMA * parameter))
        .map { (first, rest) -> listOf(first) + rest }

    private val functionHeader: Parser<FunctionHeader> by (type * ID * -LPR * optional(parameterList) * -RPR)
        .map { (type, idToken, paramTypeAndId) ->
            semanticChecker.enterFuncDecl(idToken, type, paramTypeAndId)

            val parameters = paramTypeAndId?.map { (type, idToken) ->
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
            val funcType = BuiltinType.FUNCTION(header.parameters.map { it.type }, header.returnType)
            Statement.FunctionDeclaration(header.name, funcType, header.parameters, body)
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

    private val caseStat: Parser<Statement.Case> by (
            -CASE *
                    expression *
                    -COLON *
                    optional(parser(this::statement)) *
                    optional(breakStat)
            ).map { (condition, action, breakStat) -> Statement.Case(condition, action, breakStat != null) }

    private val switchStat: Parser<Statement.Switch> by (
            -SWITCH *
                    -LPR *
                    expression *
                    -RPR *
                    -LBRACE *
                    zeroOrMore(caseStat) *
                    optional(-DEFAULT * -COLON * parser(this::statement)) *
                    -RBRACE
            ).map { (expr, cases, defaultCase) -> Statement.Switch(expr, cases, defaultCase) }

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
            switchStat or
            block

    private val prog: Parser<Program> by oneOrMore(statement).map { Program(it) }

    override val rootParser: Parser<Program> by prog

    companion object Symbols {
        const val RETURN = "return"
        const val IF = "if"
        const val ELSE = "else"
        const val WHILE = "while"
        const val FOR = "for"
        const val BREAK = "break"
        const val CONTINUE = "continue"
        const val SWITCH = "switch"
        const val CASE = "case"
        const val DEFAULT = "default"
        const val VOID_TYPE = "void"
        const val BOOL_TYPE = "bool"
        const val INT_TYPE = "int"
        const val FLOAT_TYPE = "float"
        const val STRING_TYPE = "String"
        const val TRUE = "true"
        const val FALSE = "false"
        const val DOT = "."
        const val LPR = "("
        const val RPR = ")"
        const val LBRACE = "{"
        const val RBRACE = "}"
        const val LBRACKET = "["
        const val RBRACKET = "]"
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
        const val INC = "++"
        const val DEC = "--"
    }
}

fun main() {
    val input = """
        a[2]++;
//        f()[1][2];
    """.trimIndent()

    val semanticChecker = SemanticChecker()
    val grammar = CymplGrammar(semanticChecker)
    val program = grammar.parseToEnd(input)
    program.statements.forEach(::println)
}


