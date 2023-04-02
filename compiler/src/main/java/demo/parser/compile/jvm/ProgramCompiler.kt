package demo.parser.compile.jvm

import demo.parser.domain.Program
import demo.parser.domain.ProgramVisitor.forStatementType
import demo.parser.domain.Statement
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.commons.Method

internal object ProgramCompiler {

    fun compile(program: Program, ctx: CompilationContext) = ctx.defineMainClass {
        val mainMethod = Method.getMethod("void main (String[])")

        defineMethod(ACC_PUBLIC + ACC_STATIC, mainMethod) {
            val methodStart = Label()
            val methodEnd = Label()

            mv.mark(methodStart)
            program.statements.forEach { StatementCompiler.compile(it, this) }
            mv.returnValue()
            mv.mark(methodEnd)
            writeLocalVarTable(methodStart, methodEnd)
            mv.endMethod()
        }

        //define non-main methods
        program.forStatementType(Statement.FunctionDeclaration::class.java) { funcDecl ->
            definePrivateStaticMethod(funcDecl)
        }
    }

    private fun ClassContext.definePrivateStaticMethod(funcDecl: Statement.FunctionDeclaration) {
        val method = funcDecl.asMethod()
        val signature = funcDecl.resolvedType.signature
        defineMethod(ACC_PRIVATE + ACC_STATIC, method, signature) {
            val methodStart = Label()
            val methodEnd = Label()

            mv.mark(methodStart)

            funcDecl.parameters.forEachIndexed { argIndex, arg ->
                val localIndex = declareVar(arg.id, arg.type)
                mv.loadArg(argIndex)
                mv.storeLocal(localIndex)
            }

            StatementCompiler.compile(funcDecl.body, this)

            mv.returnValue()
            mv.mark(methodEnd)
            writeLocalVarTable(methodStart, methodEnd)
            mv.endMethod()
        }
    }
}
