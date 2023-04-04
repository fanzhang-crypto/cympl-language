package demo.parser.antlr

import CymplBaseListener
import CymplParser.*
import demo.parser.domain.*
import demo.parser.domain.symbol.*
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeProperty
import org.antlr.v4.runtime.tree.ParseTreeWalker

class SemanticChecker : TypeResolver, ScopeResolver {

    private val globals = GlobalScope()
    private val scopes = ParseTreeProperty<Scope>()
    private val types = ParseTreeProperty<BuiltinType>()

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

    override fun resolveScope(ctx: LambdaContext): LambdaScope {
        return scopes.get(ctx) as LambdaScope
    }

    override fun resolveType(node: ExprContext): BuiltinType {
        return types.get(node) ?: throw IllegalStateException("No type found for node $node")
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
//            if (semanticErrors.isNotEmpty()) {
//                // remove function from scope if there are errors
//                currentScope?.remove(ctx.ID().text)
//            }
        }

        override fun enterBlock(ctx: BlockContext) {
            currentScope = LocalScope(currentScope)
            saveScope(ctx, currentScope)
        }

        override fun exitBlock(ctx: BlockContext) {
            currentScope = currentScope?.enclosingScope
        }

        override fun enterLambda(ctx: LambdaContext) {
            val parameters = ctx.idList()?.ID()?.map { VariableSymbol(it.text, BuiltinType.ANY, null) } ?: emptyList()
            val lambdaScope = LambdaScope(BuiltinType.ANY, parameters, currentScope)
            saveScope(ctx, lambdaScope)
            currentScope = lambdaScope
        }

        override fun exitLambda(ctx: LambdaContext) {
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
            val existingSymbol: Symbol? = currentScope?.resolve(name)

            if (existingSymbol != null) {
                val location = idToken.location
                if (existingSymbol.scope == currentScope) {
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
                val location = idToken.location
                if (variableSymbol.scope == currentScope) {
                    semanticErrors += SemanticException("symbol $name already defined", location)
                } else {
//                    println("variable shadowed at $location: $name")
                }
            }

            val id = idToken.text
            val type = resolveType(typeContext)
            if (type is BuiltinType.FUNCTION) {
                type.isFirstClass = true
            }
            val symbol = VariableSymbol(id, type, currentScope)
            currentScope?.define(symbol)
        }
    }

    private inner class TypeCheckPhase : CymplBaseListener() {

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

        /**
         * lambda's type can only be determined by its context which could be:
         * 1. a variable declaration
         * 2. an assign statement
         * 3. a function call with lambda as argument
         * 4. a return statement in a function definition
         */
        override fun enterLambda(ctx: LambdaContext) {
            val lambdaType: BuiltinType = when (val parent = ctx.parent) {
                is VarDeclContext -> resolveType(parent.type())

                is AssignContext -> {
                    val assignVarSymbol = currentScope?.resolve(parent.ID().text) as? VariableSymbol
                    assignVarSymbol?.type ?: BuiltinType.VOID
                }

                is ExprlistContext -> {
                    val functionCallContext = parent.parent
                    if (functionCallContext !is FunctionCallContext) {
                        return
                    }
                    val functionType = types.get(functionCallContext.funcExpr)
                    if (functionType !is BuiltinType.FUNCTION) {
                        val location = functionCallContext.funcExpr.start.location
                        semanticErrors += SemanticException("callee is not a function", location)
                        return
                    }
                    val parameterTypes = functionType.paramTypes
                    val argumentIndex = parent.expr().indexOf(ctx)
                    parameterTypes[argumentIndex]
                }

                is ExprContext -> {
                    types.get(parent)
                }

                is ReturnStatContext -> {
                    val currentFunctionSymbol = getCurrentFunctionScope()
                    if (currentFunctionSymbol == null) {
                        val location = ctx.start.location
                        semanticErrors += SemanticException("return statement outside of function", location)
                        return
                    }
                    currentFunctionSymbol.returnType
                }

                else -> BuiltinType.VOID
            }

            if (lambdaType !is BuiltinType.FUNCTION) {
                val location = ctx.start.location
                semanticErrors += SemanticException(
                    "lambda expression must be assigned to or returned as a function type",
                    location
                )
                return
            }
            lambdaType.isFirstClass = true
            if (lambdaType.paramTypes.size != (ctx.idList()?.ID()?.size ?: 0)) {
                val location = ctx.start.location
                semanticErrors += SemanticException(
                    "lambda expression must have ${lambdaType.paramTypes.size} parameters, but got ${
                        ctx.idList().ID().size
                    }",
                    location
                )
                return
            }

            currentScope = scopes[ctx]
            //update return type of current lambda scope
            (currentScope as LambdaScope).returnType = lambdaType.returnType

            ctx.idList()?.ID()?.zip(lambdaType.paramTypes)?.forEach { (idToken, paramType) ->
                val variableSymbol = VariableSymbol(idToken.text, paramType, null)
                currentScope?.define(variableSymbol)
            }
            types.put(ctx, lambdaType)
        }

        override fun exitLambda(ctx: LambdaContext) {
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
                    val location = ctx.expr().start.location
                    semanticErrors += SemanticException("negation only works on int or float", location)
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
                val location = ctx.expr().start.location
                semanticErrors += SemanticException("logical not only works on bool", location)
            }
            types.put(ctx, BuiltinType.BOOL)
        }

        override fun exitStringLiteral(ctx: StringLiteralContext) {
            types.put(ctx, BuiltinType.STRING)
        }

        override fun exitIntLiteral(ctx: IntLiteralContext) {
            types.put(ctx, BuiltinType.INT)
        }

        override fun exitBoolLiteral(ctx: BoolLiteralContext) {
            types.put(ctx, BuiltinType.BOOL)
        }

        override fun exitFloatLiteral(ctx: FloatLiteralContext) {
            types.put(ctx, BuiltinType.FLOAT)
        }

        override fun exitComparison(ctx: ComparisonContext) {
            val leftType = types.get(ctx.expr(0))
            val rightType = types.get(ctx.expr(1))
            checkComparisonBop(ctx, leftType, rightType)
        }

        override fun exitArrayLiteral(ctx: ArrayLiteralContext) {
            val elementTypes = ctx.exprlist()?.expr()?.map { types.get(it) }?.toSet() ?: emptySet()

            if (elementTypes.size > 1) {
                val location = ctx.start.location
                semanticErrors += SemanticException("array elements must be of the same type", location)
            }

            val elementType = elementTypes.firstOrNull() ?: BuiltinType.VOID
            types.put(ctx, BuiltinType.ARRAY(elementType))
        }

        override fun exitNewArray(ctx: NewArrayContext) {
            val elementType = resolveType(ctx.type())
            val arrayType = ctx.expr().fold(elementType) { acc, dimExpr ->
                val dimType = types.get(dimExpr)
                if (dimType != BuiltinType.INT) {
                    val location = dimExpr.start.location
                    semanticErrors += SemanticException("array dimensions must be of type int", location)
                }
                BuiltinType.ARRAY(acc)
            }
            types.put(ctx, arrayType)
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
                val location = ctx.expr().start.location
                semanticErrors += SemanticException(
                    "increment/decrement only works on int or float, but got $targetType",
                    location
                )
            }
            types.put(ctx, targetType)
        }

        override fun exitPostIncDec(ctx: PostIncDecContext) {
            val targetType = types.get(ctx.expr())
            if (targetType != BuiltinType.INT && targetType != BuiltinType.FLOAT) {
                val location = ctx.expr().start.location
                semanticErrors += SemanticException(
                    "increment/decrement only works on int or float, but got $targetType",
                    location
                )
            }
            types.put(ctx, targetType)
        }

        override fun exitVariable(ctx: VariableContext) {
            when (val symbol = currentScope?.resolve(ctx.ID().text)) {
                is VariableSymbol, is FunctionSymbol -> types.put(ctx, symbol.type)
                else -> {}
            }
        }

        override fun exitVarDecl(ctx: VarDeclContext) {
            val exprType = ctx.expr()?.let { types[it] } ?: BuiltinType.VOID
            val varType = resolveType(ctx.type())

            if (varType is BuiltinType.ARRAY && exprType == EmptyArray) {
                //empty array is allowed to assign to any array type
                return
            }
            if (!TypeChecker.typeMatch(exprType, varType)) {
                val location = ctx.expr().start.location
                semanticErrors += SemanticException("type mismatch: expected $varType, but got $exprType", location)
            }
        }

        override fun exitAssign(ctx: AssignContext) {
            val variableSymbol = currentScope?.resolve(ctx.ID().text) as? VariableSymbol
            if (variableSymbol != null) {
                val varType = variableSymbol.type
                val exprType = types.get(ctx.expr())
                if (!TypeChecker.typeMatch(exprType, varType)) {
                    val location = ctx.expr().start.location
                    semanticErrors += SemanticException(
                        "type mismatch: expected ${variableSymbol.type}, but got $exprType",
                        location
                    )
                }
            }
        }

        override fun exitFunctionCall(ctx: FunctionCallContext) {
            val functionType = when (val funcExpr = ctx.funcExpr) {
                is VariableContext -> {
                    val functionName = funcExpr.ID().text
                    val functionSymbol: Symbol = currentScope?.resolve(functionName) ?: return
                    if (functionSymbol.type !is BuiltinType.FUNCTION) {
                        val location = funcExpr.ID().symbol.location
                        semanticErrors += SemanticException("${functionSymbol.name} is not a function:", location)
                        return
                    }
                    functionSymbol.type
                }

                is FunctionCallContext -> {
                    types.get(funcExpr)
                }

                else -> BuiltinType.VOID
            }

            if (functionType !is BuiltinType.FUNCTION) {
                val location = ctx.funcExpr.start.location
                semanticErrors += SemanticException("callee is not a function", location)
                return
            }

            val parameterTypes = functionType.paramTypes
            val argumentTypes = ctx.exprlist()?.expr()?.mapNotNull { types.get(it) } ?: emptyList()

            if (!TypeChecker.typesMatch(argumentTypes, parameterTypes)) {
                val location = ctx.start.location
                semanticErrors += SemanticException(
                    "argument types mismatch: expected ${parameterTypes}, but got $argumentTypes",
                    location
                )
            }
            types.put(ctx, functionType.returnType)
        }

        override fun exitReturnStat(ctx: ReturnStatContext) {
            val currentFunctionSymbol = getCurrentFunctionScope()
            if (currentFunctionSymbol == null) {
                val location = ctx.start.location
                semanticErrors += SemanticException("return statement outside of function", location)
                return
            }

            val returnType = currentFunctionSymbol.returnType
            val exprType = ctx.expr()?.let { types.get(it) } ?: BuiltinType.VOID
            if (returnType != exprType) {
                val location = ctx.expr().start.location
                semanticErrors += SemanticException(
                    "return expression type mismatch: expected $returnType, but got $exprType",
                    location
                )
            }
        }

        override fun exitIndex(ctx: IndexContext) {
            val arrayType = types.get(ctx.arrayExpr)
            if (arrayType !is BuiltinType.ARRAY) {
                val location = ctx.arrayExpr.start.location
                semanticErrors += SemanticException("indexing only works on arrays", location)
                return
            }

            val indexType = types.get(ctx.indexExpr)
            if (indexType != BuiltinType.INT) {
                val location = ctx.expr(1).start.location
                semanticErrors += SemanticException("array index must be of type int, but got $indexType", location)
            }

            types.put(ctx, arrayType.elementType)
        }

        override fun exitIndexAssign(ctx: IndexAssignContext) {
            val arrayType = types.get(ctx.arrayExpr)
            if (arrayType !is BuiltinType.ARRAY) {
                val location = ctx.arrayExpr.start.location
                semanticErrors += SemanticException("indexing only works on arrays", location)
                return
            }

            val indexType = types.get(ctx.indexExpr)
            if (indexType != BuiltinType.INT) {
                val location = ctx.expr(1).start.location
                semanticErrors += SemanticException("array index must be of type int, but got $indexType", location)
            }

            val exprType = types.get(ctx.valueExpr)
            if (exprType != arrayType.elementType) {
                val location = ctx.valueExpr.start.location
                semanticErrors += SemanticException(
                    "type mismatch: expected ${arrayType.elementType}, but got $exprType",
                    location
                )
            }
        }

        override fun exitProperty(ctx: PropertyContext) {
            when (val ownerType = types.get(ctx.expr())) {
                is BuiltinType.ARRAY, is BuiltinType.STRING -> {
                    val propertyName = ctx.ID().text
                    val scope = if (ownerType is BuiltinType.ARRAY) ArrayScope else StringScope
                    val propertySymbol = scope.resolve(propertyName)
                    if (propertySymbol !is VariableSymbol) {
                        val location = ctx.ID().symbol.location
                        semanticErrors += SemanticException("property $propertyName not found", location)
                        return
                    }
                    types.put(ctx, propertySymbol.type)
                }

                else -> {
                    val location = ctx.expr().start.location
                    semanticErrors += SemanticException(
                        "property access only works on array and string for now, but got $ownerType",
                        location
                    )
                }
            }
        }

        private fun checkArithmeticBop(ctx: ParserRuleContext, leftType: BuiltinType?, rightType: BuiltinType?) {
            if (leftType == null || rightType == null) {
                return
            }

            val compatibleType = BuiltinType.compatibleTypeOf(leftType, rightType)
            if (compatibleType.numericCompatible) {
                types.put(ctx, compatibleType)
            } else {
                val location = ctx.start.location
                semanticErrors += SemanticException(
                    "type mismatch: expected bool, int or float, but got $leftType and $rightType",
                    location
                )
            }
        }

        private fun checkComparisonBop(ctx: ParserRuleContext, leftType: BuiltinType, rightType: BuiltinType) {
            val compatibleType = BuiltinType.compatibleTypeOf(leftType, rightType)
            if (!compatibleType.numericCompatible && compatibleType != BuiltinType.STRING) {
                val location = ctx.start.location
                semanticErrors += SemanticException(
                    "type mismatch: expected bool, int, float or String, but got $leftType and $rightType",
                    location
                )
            } else {
                types.put(ctx, BuiltinType.BOOL)
            }
        }

        private fun checkLogicalBop(ctx: ParserRuleContext, leftType: BuiltinType, rightType: BuiltinType) {
            if (leftType == BuiltinType.BOOL && rightType == BuiltinType.BOOL) {
                types.put(ctx, BuiltinType.BOOL)
            } else {
                val location = ctx.start.location
                semanticErrors += SemanticException(
                    "type mismatch: expected bool, but got $leftType and $rightType",
                    location
                )
            }
        }

        private fun getCurrentFunctionScope(): FunctionScope? {
            var scope = currentScope
            while (scope != null) {
                if (scope is FunctionScope) {
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

        override fun enterFuncDecl(ctx: FuncDeclContext) {
            currentScope = scopes[ctx]
        }

        override fun exitFuncDecl(ctx: FuncDeclContext) {
            currentScope = currentScope?.enclosingScope
        }

        override fun enterBlock(ctx: BlockContext) {
            currentScope = scopes[ctx]
        }

        override fun exitBlock(ctx: BlockContext) {
            currentScope = currentScope?.enclosingScope
        }

        override fun enterLambda(ctx: LambdaContext) {
            currentScope = scopes[ctx]
        }

        override fun exitLambda(ctx: LambdaContext) {
            currentScope = currentScope?.enclosingScope
        }

        override fun exitVariable(ctx: VariableContext) {
            if (ctx.parent is FunctionCallContext) {
                //leave it to the function call check
                return
            }

            val idToken = ctx.ID()
            val varName: String = idToken.text
            val variableSymbol: Symbol? = currentScope?.resolve(varName)

            if (variableSymbol == null) {
                val location = ctx.ID().symbol.location
                semanticErrors += SemanticException("variable $varName not defined", location)
            }
        }

        override fun exitAssign(ctx: AssignContext) {
            val idToken = ctx.ID()
            val varName: String = idToken.text
            val variableSymbol: Symbol? = currentScope?.resolve(varName)

            if (variableSymbol == null) {
                val location = ctx.ID().symbol.location
                semanticErrors += SemanticException("variable $varName not defined", location)
            } else if (variableSymbol !is VariableSymbol) {
                val location = ctx.ID().symbol.location
                semanticErrors += SemanticException("$varName is not a variable", location)
            }
        }

        override fun exitFunctionCall(ctx: FunctionCallContext) {
            val funcExprCtx = ctx.funcExpr
            if (funcExprCtx !is VariableContext) {
                // function to call is not a variable, so skip the reference check
                return
            }

            val idToken = funcExprCtx.ID()
            val functionName = idToken.text
            val functionSymbol: Symbol? = currentScope?.resolve(functionName)

            if (functionSymbol == null) {
                val location = idToken.symbol.location
                semanticErrors += SemanticException("function: $functionName not defined", location)
            } else if (functionSymbol.type !is BuiltinType.FUNCTION) {
                val location = idToken.symbol.location
                semanticErrors += SemanticException("$functionName is not a function", location)
            }
        }
    }
}


private val Token.location get() = TokenLocation(line, charPositionInLine)
