package demo.parser.compile.jvm

import demo.parser.domain.Program
import demo.parser.domain.Statement
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.commons.Method

internal class ProgramCompiler {

    private val statementCompiler = StatementCompiler()

    fun compile(program: Program, ctx: CompilationContext) = ctx.apply {
        defineMainClass()

        generateMainMethod(program, ctx)

        program.specificProcess(Statement.FunctionDeclaration::class.java) { functionDeclaration ->
            generateSubMethod(functionDeclaration, ctx)
        }
    }

    private fun generateMainMethod(program: Program, ctx: CompilationContext) = with(ctx){
        val mainMethod = Method.getMethod("void main (String[])")
        defineMethod(mainMethod, ACC_PUBLIC + ACC_STATIC)

        val methodStart = Label()
        val methodEnd = Label()

        mv.mark(methodStart)
        program.statements.forEach { statementCompiler.compile(it, ctx) }
        mv.returnValue()
        mv.mark(methodEnd)
        ctx.writeLocalVarTable(methodStart, methodEnd)
        mv.endMethod()
    }

    private fun generateSubMethod(functionDecl: Statement.FunctionDeclaration, ctx: CompilationContext): Unit = with(ctx) {
        val functionName = functionDecl.id
        val returnType = functionDecl.returnType.asmType
        val argTypes = functionDecl.parameters.map { it.type.asmType }.toTypedArray()

        val method = Method(functionName, returnType, argTypes)
        ctx.defineMethod(method)

        val methodStart = Label()
        val methodEnd = Label()

        mv.mark(methodStart)
        ctx.enterScope()

        functionDecl.parameters.forEachIndexed { argIndex, arg ->
            val localIndex = ctx.declareVar(arg.id, arg.type)
            mv.loadArg(argIndex)
            mv.storeLocal(localIndex)
        }

        statementCompiler.compile(functionDecl.body, ctx)
        ctx.exitScope()
        mv.returnValue()
        mv.mark(methodEnd)

        ctx.writeLocalVarTable(methodStart, methodEnd)

        mv.endMethod()
    }
}


