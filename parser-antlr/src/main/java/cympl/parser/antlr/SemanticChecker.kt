package cympl.parser.antlr

import cympl.parser.antlr.CymplParser.*
import cympl.language.*
import cympl.language.symbol.*
import cympl.parser.SemanticException
import cympl.parser.SyntaxException
import cympl.parser.TokenLocation
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeProperty
import org.antlr.v4.runtime.tree.ParseTreeWalker

class SemanticChecker : TypeResolver, ScopeResolver {

    private val globals = GlobalScope()
    private val scopes = ParseTreeProperty<Scope>()
    private val types = ParseTreeProperty<BuiltinType>()

    private val semanticErrors = mutableListOf<SemanticException>()

    private var currentScope: Scope? = globals

    fun check(root: ParseTree): List<SemanticException> {
        val walker = ParseTreeWalker.DEFAULT

        val defPhase = DefPhase()
        walker.walk(defPhase, root)

        val refPhase = RefPhase()
        walker.walk(refPhase, root)

        val typeCheckPhase = TypeCheckPhase()
        walker.walk(typeCheckPhase, root)

        return semanticErrors.sorted().also { semanticErrors.clear() }
    }

    override fun resolveScope(ctx: LambdaContext): LambdaScope {
        return scopes.get(ctx) as LambdaScope
    }

    override fun resolveType(node: ExprContext): BuiltinType {
        return types.get(node) ?: throw IllegalStateException("No type found for node $node")
    }

    override fun resolveType(typeContext: TypeContext?): BuiltinType = when (typeContext) {
        null, is VoidTypeContext -> BuiltinType.VOID
        is BoolTypeContext -> BuiltinType.BOOL
        is IntTypeContext -> BuiltinType.INT
        is FloatTypeContext -> BuiltinType.FLOAT
        is StringTypeContext -> BuiltinType.STRING
        is FunctionTypeContext -> {
            val funcTypeContext = typeContext.funcType()
            val returnType = resolveType(funcTypeContext.retType)
            val parameterTypes =
                funcTypeContext.paramTypes?.type()?.map { resolveType(it as TypeContext) }
                    ?: emptyList()
            BuiltinType.FUNCTION(parameterTypes, returnType)
        }

        is FunctionArrayTypeContext -> {
            val funcTypeContext = typeContext.funcType()
            val returnType = resolveType(funcTypeContext.retType)
            val parameterTypes =
                funcTypeContext.paramTypes?.type()?.map { resolveType(it as TypeContext) }
                    ?: emptyList()
            BuiltinType.ARRAY(BuiltinType.FUNCTION(parameterTypes, returnType))
        }

        is ArrayTypeContext -> {
            val elementTypeContext = typeContext.type()
            val elementType = resolveType(elementTypeContext)
            BuiltinType.ARRAY(elementType)
        }

        else -> {
            val location = TokenLocation(typeContext.start.line, typeContext.start.charPositionInLine)
            throw SyntaxException("unknown type ${typeContext.text}", location)
        }
    }

    private fun defineFunc(
        idToken: Token,
        typeContext: TypeContext?,
        paramsContext: ParamDeclsContext?
    ): FunctionSymbol {
        val id: String = idToken.text
        val returnType: BuiltinType = resolveType(typeContext)

        val fixParams = paramsContext?.paramDecl()?.map { param ->
            val paramName = param.ID().text
            val paramType = resolveType(param.type())
            VariableSymbol(paramName, paramType, currentScope)
        } ?: emptyList()

        val variableParam = paramsContext?.variableParamDecl()?.let { param ->
            val paramName = param.ID().text
            val paramType = resolveType(param.type())
            VariableSymbol(paramName, paramType, currentScope)
        }

        val parameters = if (variableParam != null) fixParams + variableParam else fixParams
        val newSymbol = FunctionSymbol(id, returnType, parameters, currentScope, supportVarargs = variableParam != null)

        defineSymbol(newSymbol, idToken)

        return newSymbol
    }

    private fun defineVar(idToken: Token, typeContext: TypeContext, isVarargs: Boolean = false) {
        val id: String = idToken.text
        val type = resolveType(typeContext)
        if (type is BuiltinType.FUNCTION) {
            type.isFirstClass = true
        }

        val resolvedType = if (isVarargs) BuiltinType.ARRAY(type) else type
        val newSymbol = VariableSymbol(id, resolvedType, currentScope)

        defineSymbol(newSymbol, idToken)
    }

    private fun defineSymbol(newSymbol: Symbol, idToken: Token) {
        val id = newSymbol.name
        val existingSymbol: Symbol? = currentScope?.resolve(id)
        if (existingSymbol != null && !shadowingAllowed(newSymbol, existingSymbol)) {
            val location = idToken.location
            semanticErrors += SemanticException("symbol $id already defined", location)
        }

        currentScope?.define(newSymbol)
    }

    //shadowing only allowed when a symbol inside function scope overrides a symbol outside that function scope
    private fun shadowingAllowed(newSymbol: Symbol, existingSymbol: Symbol): Boolean {
        var scope = newSymbol.scope
        while (scope != null && scope != existingSymbol.scope) {
            if (scope is FunctionSymbol) {
                return true
            }
            scope = scope.enclosingScope
        }
        return false
    }

    private inner class DefPhase : CymplBaseListener() {

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

        private fun saveScope(ctx: ParserRuleContext, s: Scope?) {
            scopes.put(ctx, s)
        }
    }

    private inner class RefPhase : CymplBaseListener() {

        override fun exitParamDecl(ctx: ParamDeclContext) {
            defineVar(ctx.ID().symbol, ctx.type())
        }

        override fun exitVariableParamDecl(ctx: VariableParamDeclContext) {
            defineVar(ctx.ID().symbol, ctx.type(), isVarargs = true)
        }

        override fun exitVarDecl(ctx: VarDeclContext) {
            defineVar(ctx.ID().symbol, ctx.type())
        }

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
            }
        }
    }

    private inner class TypeCheckPhase : CymplBaseListener() {

        override fun enterFuncDecl(ctx: FuncDeclContext) {
            currentScope = scopes[ctx]
        }

        override fun exitFuncDecl(ctx: FuncDeclContext) {
            val functionSymbol = currentScope as FunctionSymbol
            val functionBody = ctx.block()

            if (functionSymbol.returnType != BuiltinType.VOID
                && !functionBody.hasReturnStat
            ) {
                val location = ctx.stop.location
                semanticErrors += SemanticException(
                    "missing return statement in function ${functionSymbol.name}", location
                )
            }
            currentScope = currentScope?.enclosingScope
        }

        override fun enterBlock(ctx: BlockContext) {
            currentScope = scopes[ctx]
        }

        override fun exitBlock(ctx: BlockContext) {
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
            val inferredLambdaType: BuiltinType = when (val parent = ctx.parent) {
                is VarDeclContext -> resolveType(parent.type())

                is AssignContext -> {
                    types.get(parent.lvalue)
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

                //lambda in a parent lambda
                is LambdaContext -> {
                    val parentLambdaType = types.get(parent)
                    if (parentLambdaType !is BuiltinType.FUNCTION) {
                        val location = parent.start.location
                        semanticErrors += SemanticException("parent lambda is not a function", location)
                        return
                    }
                    parentLambdaType.returnType
                }

                //lambda as an argument in a function call
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

            if (inferredLambdaType !is BuiltinType.FUNCTION) {
                val location = ctx.start.location
                semanticErrors += SemanticException(
                    "lambda expression must be assigned to or returned as a function type",
                    location
                )
                return
            }
            inferredLambdaType.isFirstClass = true
            if (inferredLambdaType.paramTypes.size != (ctx.idList()?.ID()?.size ?: 0)) {
                val location = ctx.start.location
                semanticErrors += SemanticException(
                    "lambda expression expected to have ${inferredLambdaType.paramTypes.size} parameters, but got ${
                        ctx.idList().ID().size
                    }",
                    location
                )
            }

            currentScope = scopes[ctx]
            //update return type of current lambda scope
            (currentScope as LambdaScope).returnType = inferredLambdaType.returnType

            ctx.idList()?.ID()?.zip(inferredLambdaType.paramTypes)?.forEach { (idToken, paramType) ->
                val variableSymbol = VariableSymbol(idToken.text, paramType, null)
                currentScope?.define(variableSymbol)
            }
            types.put(ctx, inferredLambdaType)
        }

        override fun exitLambda(ctx: LambdaContext) {
            val lambdaRetType = (currentScope as LambdaScope).returnType

            val (actualRetType, retLocation) = when (val lambdaBody = ctx.expr() ?: ctx.statement()) {
                // lambda body is an expression, return type is the type of the expression
                is ExprContext -> {
                    val type = types.get(lambdaBody)
                    val location = lambdaBody.start.location
                    type to location
                }
                // lambda body is a block statement, return type is the type of the last return statement
                is StatementContext -> {
                    val retStat = (lambdaBody as? BlockStatementContext)?.block()?.statement()?.lastOrNull()
                    if (retStat is ReturnStatementContext)
                        types.get(retStat.returnStat().expr()) to retStat.start.location
                    else
                        BuiltinType.VOID to lambdaBody.start.location
                }

                else -> BuiltinType.VOID to ctx.start.location
            }

            if (actualRetType != lambdaRetType) {
                semanticErrors += SemanticException(
                    "lambda expression expected to return $lambdaRetType, but got $actualRetType",
                    retLocation
                )
            }

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
            val elementTypes = ctx.elements?.expr()?.map { types.get(it) }?.toSet() ?: emptySet()

            if (elementTypes.size > 1) {
                val location = ctx.start.location
                semanticErrors += SemanticException("array elements must be of the same type", location)
            }
            val elementType = elementTypes.firstOrNull() ?: BuiltinType.ANY
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

            if (varType is BuiltinType.ARRAY && exprType is BuiltinType.ARRAY && exprType.elementType == BuiltinType.ANY) {
                //empty array is allowed to assign to any array type
                return
            }
            if (!TypeChecker.typeMatch(exprType, varType)) {
                val location = ctx.expr().start.location
                semanticErrors += SemanticException("type mismatch: expected $varType, but got $exprType", location)
            }
        }

        override fun exitAssign(ctx: AssignContext) {
            val lvalueType = types.get(ctx.lvalue)
            val rvalueType = types.get(ctx.rvalue)

            if (lvalueType == null || rvalueType == null) {
                return
            }

            if (!TypeChecker.typeMatch(lvalueType, rvalueType)) {
                val location = ctx.rvalue.start.location
                semanticErrors += SemanticException(
                    "type mismatch: expected ${lvalueType}, but got $rvalueType",
                    location
                )
            }
        }

        override fun exitFunctionCall(ctx: FunctionCallContext) {
            val functionType = types.get(ctx.funcExpr) ?: return

            if (functionType !is BuiltinType.FUNCTION) {
                val location = ctx.funcExpr.start.location
                semanticErrors += SemanticException("callee is not a function", location)
                return
            }

            val parameterTypes = functionType.paramTypes
            val argumentTypes = ctx.exprlist()?.expr()?.mapNotNull { types.get(it) } ?: emptyList()

            val fixParamTypes = if (functionType.supportVarargs) parameterTypes.dropLast(1) else parameterTypes
            val variableParamType = if (functionType.supportVarargs) parameterTypes.last() else null

            if (argumentTypes.size < fixParamTypes.size) {
                val location = ctx.start.location
                semanticErrors += SemanticException(
                    "argument count mismatch: expected at least ${fixParamTypes.size}, but got ${argumentTypes.size}",
                    location
                )
            }
            if (argumentTypes.size > parameterTypes.size && !functionType.supportVarargs) {
                val location = ctx.start.location
                semanticErrors += SemanticException(
                    "argument count mismatch: expected ${parameterTypes.size}, but got ${argumentTypes.size}",
                    location
                )
            }

            argumentTypes.forEachIndexed { i, argType ->
                val paramType = if (i < fixParamTypes.size) fixParamTypes[i] else variableParamType

                if (paramType != null && !TypeChecker.typeMatch(argType, paramType)) {
                    val location = ctx.exprlist().expr(i).start.location
                    semanticErrors += SemanticException(
                        "argument type mismatch at index $i: expected $paramType, but got $argType",
                        location
                    )
                }
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

        override fun exitArrayAccess(ctx: ArrayAccessContext) {
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

        override fun exitIfStat(ctx: IfStatContext) {
            val conditionType = types.get(ctx.expr())
            if (conditionType != BuiltinType.BOOL) {
                val location = ctx.expr().start.location
                semanticErrors += SemanticException(
                    "if condition must be of type bool, but got $conditionType",
                    location
                )
            }
        }

        override fun exitWhileStat(ctx: WhileStatContext) {
            val conditionType = types.get(ctx.expr())
            if (conditionType != BuiltinType.BOOL) {
                val location = ctx.expr().start.location
                semanticErrors += SemanticException(
                    "while condition must be of type bool, but got $conditionType",
                    location
                )
            }
        }

        override fun exitForStat(ctx: ForStatContext) {
            val conditionType = types.get(ctx.cond) ?: return
            if (conditionType != BuiltinType.BOOL) {
                val location = ctx.cond.start.location
                semanticErrors += SemanticException(
                    "for condition must be of type bool, but got $conditionType",
                    location
                )
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
}

private val BlockContext.hasReturnStat
    get(): Boolean = statement().any { it.hasReturnStat }

/**
 * @return true if the statement has a return statement in all possible branches
 */
private val StatementContext.hasReturnStat
    get(): Boolean = when (this) {
        is ReturnStatementContext -> true

        is IfStatementContext -> {
            val thenBranch = ifStat().thenBranch
            val elseBranch = ifStat().elseBranch
            thenBranch.hasReturnStat && (elseBranch?.hasReturnStat ?: true)
        }

        is SwitchStatementContext -> {
            switchStat().caseClause().all { it.statement().hasReturnStat } &&
                    (switchStat().defaultClause()?.statement()?.hasReturnStat ?: true)
        }

        is WhileStatementContext -> whileStat().statement().hasReturnStat
        is ForStatementContext -> forStat().body.hasReturnStat
        is BlockStatementContext -> block().statement().any { it.hasReturnStat }
        else -> false
    }

private val Token.location get() = TokenLocation(line, charPositionInLine + 1)
