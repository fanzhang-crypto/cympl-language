package demo.parser.compile.jvm

import demo.parser.domain.Program
import demo.parser.domain.Statement
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.commons.Method
import org.objectweb.asm.signature.SignatureWriter

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

        program.forEvery(Statement.FunctionDeclaration::class.java) { funcDecl ->
            defineMethod(ACC_PRIVATE + ACC_STATIC, funcDecl.asMethod()) {
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

    private fun defineInterfaceFunction0(ctx: CompilationContext) = with(ctx) {
        val classSignature = SignatureWriter().apply {
            visitFormalTypeParameter("T")
            visitClassType("java/lang/Object")
            visitEnd()
            visitSuperclass()

            visitClassType("java/lang/Object")
            visitEnd()
        }.toString()

        defineInnerClass(
            "Function0",
            classSignature,
            emptyArray(),
            ACC_INTERFACE + ACC_ABSTRACT
        ) {
            val methodSignature = SignatureWriter().apply {
                visitParameterType()
                visitReturnType()
                visitTypeVariable("T")
            }.toString()

            defineMethod(
                ACC_PUBLIC + ACC_ABSTRACT,
                Method.getMethod("T apply ()"),
                methodSignature
            ) {
                mv.visitEnd()
            }
        }
    }

}


