package demo.parser.antlr

import CymplBaseListener
import CymplParser.*
import demo.parser.antlr.TypeResolver.resolveType
import demo.parser.domain.SemanticException
import demo.parser.domain.TokenLocation
import demo.parser.domain.BuiltinType
import demo.parser.domain.EmptyArray
import demo.parser.domain.symbol.*
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeProperty
import org.antlr.v4.runtime.tree.ParseTreeWalker

class SemanticChecker {

    private val globals = GlobalScope()
    private val scopes = ParseTreeProperty<Scope>()

    fun check(programAST: ParseTree): List<SemanticException> {
        val walker = ParseTreeWalker.DEFAULT

        val defPhase = DefPhase()
        walker.walk(defPhase, programAST)

        val typeCheckPhase = TypeCheckPhase()
        walker.walk(typeCheckPhase, programAST)

        val refPhase = RefPhase()
        walker.walk(refPhase, programAST)

        return (defPhase.semanticErrors + typeCheckPhase.semanticErrors + refPhase.semanticErrors).sorted()
    }

    private inner class DefPhase : CymplBaseListener() {

        val semanticErrors = mutableListOf<SemanticException>()

        private var currentScope: Scope? = globals

        override fun enterFuncDecl(ctx: FuncDeclContext) {
            val function = defineFunc(ctx.ID().symbol, ctx.type(), ctx.paramDecls())
            saveScope(ctx, function)
            currentScope = function
        }

        override fun exitFuncDecl(ctx: FuncDeclContext) {
            currentScope = currentScope?.enclosingScope
            if (semanticErrors.isNotEmpty()) {
                // remove function from scope if there are errors
                currentScope?.remove(ctx.ID().text)
            }
        }

        override fun enterBlock(ctx: BlockContext) {
            currentScope = LocalScope(currentScope)
            saveScope(ctx, currentScope)
        }

        override fun exitBlock(ctx: BlockContext) {
            currentScope = currentScope?.enclosingScope
        }

        override fun exitParamDecl(ctx: ParamDeclContext) {
            defineVar(ctx.ID().symbol, ctx.type())
        }

        override fun exitVarDecl(ctx: VarDeclContext) {
            defineVar(ctx.ID().symbol, ctx.type())
        }

        private fun saveScope(ctx: ParserRuleContext, s: Scope?) {
            scopes.put(ctx, s)
        }

        private fun defineFunc(
            idToken: Token,
            typeContext: TypeContext?,
            paramsContext: ParamDeclsContext?
        ): FunctionSymbol {
            val name: String = idToken.text
            val functionSymbol: Symbol? = currentScope?.resolve(name)

            if (functionSymbol != null) {
                val location = getLocation(idToken)
                if (functionSymbol.scope == currentScope) {
                    semanticErrors += SemanticException("function $name already defined", location)
                } else {
                    println("function shadowed at $location: $name")
                }
            }

            val returnType: BuiltinType = resolveType(typeContext)

            val parameters = paramsContext?.paramDecl()?.map { param ->
                val paramName = param.ID().text
                val paramType = resolveType(param.type())
                VariableSymbol(paramName, paramType, currentScope)
            } ?: emptyList()

            return FunctionSymbol(name, returnType, parameters, currentScope)
                .also { currentScope?.define(it) }
        }

        private fun defineVar(idToken: Token, typeContext: TypeContext) {
            val name: String = idToken.text
            val variableSymbol: Symbol? = currentScope?.resolve(name)

            if (variableSymbol != null) {
                val location = getLocation(idToken)
                if (variableSymbol.scope == currentScope) {
                    semanticErrors += SemanticException("variable $name already defined", location)
                } else {
                    println("variable shadowed at $location: $name")
                }
            }

            val id = idToken.text
            val type = resolveType(typeContext)
            val symbol = VariableSymbol(id, type, currentScope)
            currentScope?.define(symbol)
        }
    }

    private inner class TypeCheckPhase : CymplBaseListener() {

        val semanticErrors = mutableListOf<SemanticException>()

        private val types = ParseTreeProperty<BuiltinType>()
        private var currentScope: Scope? = globals

        override fun enterFuncDecl(ctx: FuncDeclContext?) {
            currentScope = scopes[ctx]
        }

        override fun exitFuncDecl(ctx: FuncDeclContext?) {
            currentScope = currentScope?.enclosingScope
        }

        override fun enterBlock(ctx: BlockContext?) {
            currentScope = scopes[ctx]
        }

        override fun exitBlock(ctx: BlockContext?) {
            currentScope = currentScope?.enclosingScope
        }

        override fun exitParenthesizedExpression(ctx: ParenthesizedExpressionContext) {
            types.put(ctx, types.get(ctx.expr()))
        }

        override fun exitNegation(ctx: NegationContext) {
            when (types.get(ctx.expr())) {
                is BuiltinType.INT -> types.put(ctx, BuiltinType.INT)
                is BuiltinType.FLOAT -> types.put(ctx, BuiltinType.FLOAT)
                else -> {
                    val location = getLocation(ctx.expr().start)
                    semanticErrors += SemanticException("negation only works on INT or FLOAT", location)
                }
            }
        }

        override fun exitLogicalAnd(ctx: LogicalAndContext) {
            val leftType = types.get(ctx.expr(0))
            val rightType = types.get(ctx.expr(1))
            checkLogicalBop(ctx, leftType, rightType)
        }

        override fun exitLogicalOr(ctx: LogicalOrContext) {
            val leftType = types.get(ctx.expr(0))
            val rightType = types.get(ctx.expr(1))
            checkLogicalBop(ctx, leftType, rightType)
        }

        override fun exitLogicalNot(ctx: LogicalNotContext) {
            if (types.get(ctx.expr()) != BuiltinType.BOOL) {
                val location = getLocation(ctx.expr().start)
                semanticErrors += SemanticException("logical not only works on BOOL", location)
            }
        }

        override fun exitSTRING(ctx: STRINGContext) {
            types.put(ctx, BuiltinType.STRING)
        }

        override fun exitINT(ctx: INTContext) {
            types.put(ctx, BuiltinType.INT)
        }

        override fun exitBOOL(ctx: BOOLContext) {
            types.put(ctx, BuiltinType.BOOL)
        }

        override fun exitFLOAT(ctx: FLOATContext) {
            types.put(ctx, BuiltinType.FLOAT)
        }

        override fun exitComparison(ctx: ComparisonContext) {
            val leftType = types.get(ctx.expr(0))
            val rightType = types.get(ctx.expr(1))
            checkComparisonBop(ctx, leftType, rightType)
        }

        override fun exitArrayExpression(ctx: ArrayExpressionContext) {
            val elementTypes = ctx.exprlist()?.expr()?.map { types.get(it) }?.toSet() ?: emptySet()

            if (elementTypes.size > 1) {
                val location = getLocation(ctx.start)
                semanticErrors += SemanticException("array elements must be of the same type", location)
            }

            val elementType = elementTypes.firstOrNull() ?: BuiltinType.VOID
            types.put(ctx, BuiltinType.ARRAY(elementType))
        }

        override fun exitMulDiv(ctx: MulDivContext) {
            val leftType = types.get(ctx.expr(0))
            val rightType = types.get(ctx.expr(1))
            checkArithmeticBop(ctx, leftType, rightType)
        }

        override fun exitAddSub(ctx: AddSubContext) {
            val leftType = types.get(ctx.expr(0))
            val rightType = types.get(ctx.expr(1))
            val op = ctx.op.text

            if (op == "+" && (leftType == BuiltinType.STRING || rightType == BuiltinType.STRING)) {
                //string concatenation with + is allowed
                types.put(ctx, BuiltinType.STRING)
            } else {
                checkArithmeticBop(ctx, leftType, rightType)
            }
        }

        override fun exitPower(ctx: PowerContext) {
            val leftType = types.get(ctx.expr(0))
            val rightType = types.get(ctx.expr(1))
            checkArithmeticBop(ctx, leftType, rightType)
        }

        override fun exitPreIncDec(ctx: PreIncDecContext) {
            val targetType = types.get(ctx.expr())
            if (targetType != BuiltinType.INT && targetType != BuiltinType.FLOAT) {
                val location = getLocation(ctx.expr().start)
                semanticErrors += SemanticException(
                    "increment/decrement only works on INT or FLOAT, but got $targetType",
                    location
                )
            }
        }

        override fun exitPostIncDec(ctx: PostIncDecContext) {
            val targetType = types.get(ctx.expr())
            if (targetType != BuiltinType.INT && targetType != BuiltinType.FLOAT) {
                val location = getLocation(ctx.expr().start)
                semanticErrors += SemanticException(
                    "increment/decrement only works on INT or FLOAT, but got $targetType",
                    location
                )
            }
        }

        override fun exitVariable(ctx: VariableContext) {
            val variableSymbol = currentScope?.resolve(ctx.ID().text) as? VariableSymbol
            if (variableSymbol != null) {
                types.put(ctx, variableSymbol.type)
            }
        }

        override fun exitVarDecl(ctx: VarDeclContext) {
            val exprType = ctx.expr()?.let { types[it] } ?: BuiltinType.VOID
            val varType = resolveType(ctx.type())

            if (varType is BuiltinType.ARRAY && exprType == EmptyArray) {
                //empty array is allowed to assign to any array type
                return
            }
            if (exprType != varType) {
                val location = getLocation(ctx.expr().start)
                semanticErrors += SemanticException("type mismatch: expected $varType, but got $exprType", location)
            }
        }

        override fun exitAssign(ctx: AssignContext) {
            val variableSymbol = currentScope?.resolve(ctx.ID().text) as? VariableSymbol
            if (variableSymbol != null) {
                val varType = variableSymbol.type
                val exprType = types.get(ctx.expr())
                if (exprType != varType) {
                    val location = getLocation(ctx.expr().start)
                    semanticErrors += SemanticException(
                        "type mismatch: expected ${variableSymbol.type}, but got $exprType",
                        location
                    )
                }
            }
        }

        override fun exitFunctionCall(ctx: FunctionCallContext) {
            val idToken = ctx.ID()
            val functionName = idToken.text

            val functionSymbol: Symbol? = currentScope?.resolve(functionName)
            if (functionSymbol !is FunctionSymbol) {
                return
            }

            val parameterTypes = functionSymbol.parameters.map { it.type }
            val argumentTypes = ctx.exprlist()?.expr()?.map { types.get(it) } ?: emptyList()

            if (argumentTypes != parameterTypes) {
                val location = getLocation(idToken.symbol)
                semanticErrors += SemanticException(
                    "argument types mismatch: expected ${parameterTypes}, but got $argumentTypes",
                    location
                )
            }
            types.put(ctx, functionSymbol.type)
        }

        override fun exitReturnStat(ctx: ReturnStatContext) {
            val currentFunctionSymbol = getCurrentFunctionSymbol()
            if (currentFunctionSymbol == null) {
                val location = getLocation(ctx.start)
                semanticErrors += SemanticException("return statement outside of function", location)
                return
            }

            val returnType = currentFunctionSymbol.type
            val exprType = ctx.expr()?.let { types.get(it) } ?: BuiltinType.VOID
            if (returnType != exprType) {
                val location = getLocation(ctx.expr().start)
                semanticErrors += SemanticException(
                    "return expression type mismatch: expected $returnType, but got $exprType",
                    location
                )
            }
        }

        override fun exitIndex(ctx: IndexContext) {
            val arrayType = types.get(ctx.arrayExpr)
            if (arrayType !is BuiltinType.ARRAY) {
                val location = getLocation(ctx.arrayExpr.start)
                semanticErrors += SemanticException("indexing only works on arrays", location)
                return
            }

            val indexType = types.get(ctx.indexExpr)
            if (indexType != BuiltinType.INT) {
                val location = getLocation(ctx.expr(1).start)
                semanticErrors += SemanticException("array index must be of type INT, but got $indexType", location)
            }

            types.put(ctx, arrayType.elementType)
        }

        override fun exitIndexAssign(ctx: IndexAssignContext) {
            val arrayType = types.get(ctx.arrayExpr)
            if (arrayType !is BuiltinType.ARRAY) {
                val location = getLocation(ctx.arrayExpr.start)
                semanticErrors += SemanticException("indexing only works on arrays", location)
                return
            }

            val indexType = types.get(ctx.indexExpr)
            if (indexType != BuiltinType.INT) {
                val location = getLocation(ctx.expr(1).start)
                semanticErrors += SemanticException("array index must be of type INT, but got $indexType", location)
            }

            val exprType = types.get(ctx.valueExpr)
            if (exprType != arrayType.elementType) {
                val location = getLocation(ctx.valueExpr.start)
                semanticErrors += SemanticException(
                    "type mismatch: expected ${arrayType.elementType}, but got $exprType",
                    location
                )
            }
        }

        override fun exitProperty(ctx: PropertyContext) {
            val ownerType = types.get(ctx.expr())

            when (ownerType) {
                is BuiltinType.ARRAY, is BuiltinType.STRING -> {
                    val propertyName = ctx.ID().text
                    val propertySymbol = ownerType.scope?.resolve(propertyName)
                    if (propertySymbol !is VariableSymbol) {
                        val location = getLocation(ctx.ID().symbol)
                        semanticErrors += SemanticException("property $propertyName not found", location)
                        return
                    }
                    types.put(ctx, propertySymbol.type)
                }

                else -> {
                    val location = getLocation(ctx.expr().start)
                    semanticErrors += SemanticException(
                        "property access only works on arrays and strings for now, but got $ownerType",
                        location
                    )
                }
            }
        }

        private fun checkArithmeticBop(ctx: ParserRuleContext, leftType: BuiltinType?, rightType: BuiltinType?) {
            if (leftType == null || rightType == null) {
                return
            }
            if (leftType == BuiltinType.INT && rightType == BuiltinType.INT) {
                types.put(ctx, BuiltinType.INT)
            } else if (leftType == BuiltinType.FLOAT && rightType == BuiltinType.FLOAT) {
                types.put(ctx, BuiltinType.FLOAT)
            } else if (leftType == BuiltinType.FLOAT && rightType == BuiltinType.INT) {
                types.put(ctx, BuiltinType.FLOAT)
            } else if (leftType == BuiltinType.INT && rightType == BuiltinType.FLOAT) {
                types.put(ctx, BuiltinType.FLOAT)
            } else {
                val location = getLocation(ctx.start)
                semanticErrors += SemanticException(
                    "type mismatch: expected INT or FLOAT, but got $leftType and $rightType",
                    location
                )
            }
        }

        private fun checkComparisonBop(ctx: ParserRuleContext, leftType: BuiltinType, rightType: BuiltinType) {
            if (leftType == BuiltinType.INT && rightType == BuiltinType.INT) {
                types.put(ctx, BuiltinType.BOOL)
            } else if (leftType == BuiltinType.FLOAT && rightType == BuiltinType.FLOAT) {
                types.put(ctx, BuiltinType.BOOL)
            } else if (leftType == BuiltinType.FLOAT && rightType == BuiltinType.INT) {
                types.put(ctx, BuiltinType.BOOL)
            } else if (leftType == BuiltinType.INT && rightType == BuiltinType.FLOAT) {
                types.put(ctx, BuiltinType.BOOL)
            } else if (leftType == BuiltinType.STRING && rightType == BuiltinType.STRING) {
                types.put(ctx, BuiltinType.BOOL)
            } else if (leftType == BuiltinType.BOOL && rightType == BuiltinType.BOOL) {
                types.put(ctx, BuiltinType.BOOL)
            } else {
                val location = getLocation(ctx.start)
                semanticErrors += SemanticException(
                    "comparison only works between INT and FLOAT, INT and INT, FLOAT and FLOAT, STRING and STRING, BOOL and BOOL, but got $leftType and $rightType",
                    location
                )
            }
        }

        private fun checkLogicalBop(ctx: ParserRuleContext, leftType: BuiltinType, rightType: BuiltinType) {
            if (leftType == BuiltinType.BOOL && rightType == BuiltinType.BOOL) {
                types.put(ctx, BuiltinType.BOOL)
            } else {
                val location = getLocation(ctx.start)
                semanticErrors += SemanticException(
                    "type mismatch: expected BOOL, but got $leftType and $rightType",
                    location
                )
            }
        }

        private fun getCurrentFunctionSymbol(): FunctionSymbol? {
            var scope = currentScope
            while (scope != null) {
                if (scope is FunctionSymbol) {
                    return scope
                }
                scope = scope.enclosingScope
            }
            return null
        }
    }

    private inner class RefPhase : CymplBaseListener() {

        val semanticErrors = mutableListOf<SemanticException>()

        private var currentScope: Scope? = globals

        override fun enterFuncDecl(ctx: FuncDeclContext?) {
            currentScope = scopes[ctx]
        }

        override fun exitFuncDecl(ctx: FuncDeclContext?) {
            currentScope = currentScope?.enclosingScope
        }

        override fun enterBlock(ctx: BlockContext?) {
            currentScope = scopes[ctx]
        }

        override fun exitBlock(ctx: BlockContext?) {
            currentScope = currentScope?.enclosingScope
        }

        override fun exitVariable(ctx: VariableContext) {
            val idToken = ctx.ID()
            val varName: String = idToken.text
            val variableSymbol: Symbol? = currentScope?.resolve(varName)

            if (variableSymbol == null) {
                val location = getLocation(ctx.ID().symbol)
                semanticErrors += SemanticException("variable $varName not defined", location)
            } else if (variableSymbol !is VariableSymbol) {
                val location = getLocation(ctx.ID().symbol)
                semanticErrors += SemanticException("$varName is not a variable", location)
            }
        }

        override fun exitAssign(ctx: AssignContext) {
            val idToken = ctx.ID()
            val varName: String = idToken.text
            val variableSymbol: Symbol? = currentScope?.resolve(varName)

            if (variableSymbol == null) {
                val location = getLocation(ctx.ID().symbol)
                semanticErrors += SemanticException("variable $varName not defined", location)
            } else if (variableSymbol !is VariableSymbol) {
                val location = getLocation(ctx.ID().symbol)
                semanticErrors += SemanticException("$varName is not a variable", location)
            }
        }

        override fun exitFunctionCall(ctx: FunctionCallContext) {
            val idToken = ctx.ID()
            val functionName = idToken.text
            val functionSymbol: Symbol? = currentScope?.resolve(functionName)

            if (functionSymbol == null) {
                val location = getLocation(idToken.symbol)
                semanticErrors += SemanticException("function: $functionName not defined", location)
            } else if (functionSymbol !is FunctionSymbol) {
                val location = getLocation(idToken.symbol)
                semanticErrors += SemanticException("$functionName is not a function", location)
            }
        }
    }
}


private fun getLocation(token: Token): TokenLocation = TokenLocation(token.line, token.charPositionInLine)
