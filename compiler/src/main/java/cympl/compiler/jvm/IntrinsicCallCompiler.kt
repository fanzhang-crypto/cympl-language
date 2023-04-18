package cympl.compiler.jvm

import cympl.language.Expression
import org.objectweb.asm.Type
import org.objectweb.asm.commons.Method
import java.io.InputStream
import java.io.PrintStream
import java.util.*

internal object IntrinsicCallCompiler {

    fun compilePrintLineCall(functionCall: Expression.FunctionCall, ctx: MethodContext): Unit =
        with(ctx) {
            mv.getStatic(Type.getType(System::class.java), "out", Type.getType(PrintStream::class.java))
            val arg = functionCall.args[0]
            ExpressionCompiler.compile(arg, this)

            val argType = arg.resolvedType

            if (argType is cympl.language.BuiltinType.ARRAY) {
                if (argType.elementType is cympl.language.BuiltinType.ARRAY) {
                    // for arrays with dimension > 1
                    mv.invokeStatic(
                        Type.getType(Arrays::class.java),
                        Method.getMethod("String deepToString (Object[])")
                    )
                } else {
                    mv.invokeStatic(
                        Type.getType(Arrays::class.java),
                        Method.getMethod("String toString (${arg.resolvedType.asmType.className})")
                    )
                }
            }

            val typeSignature = if (arg.resolvedType.isPrimitive) arg.resolvedType.asmType.className else "Object"

            mv.invokeVirtual(
                Type.getType(PrintStream::class.java),
                Method.getMethod("void println ($typeSignature)")
            )
        }

    fun compileReadLineCall(functionCall: Expression.FunctionCall, ctx: MethodContext): Unit =
        with(ctx) {
            mv.getStatic(Type.getType(System::class.java), "out", Type.getType(PrintStream::class.java))
            val arg = functionCall.args[0]
            ExpressionCompiler.compile(arg, this)

            mv.invokeVirtual(
                Type.getType(PrintStream::class.java),
                Method.getMethod("void print (Object)")
            )

            mv.newInstance(Type.getType(Scanner::class.java))
            mv.dup()
            mv.getStatic(Type.getType(System::class.java), "in", Type.getType(InputStream::class.java))
            mv.invokeConstructor(Type.getType(Scanner::class.java), Method.getMethod("void <init> (java.io.InputStream)"))
            val scannerLocalIndex = mv.newLocal(Type.getType(Scanner::class.java))
            mv.storeLocal(scannerLocalIndex)
            mv.loadLocal(scannerLocalIndex)
            mv.invokeVirtual(Type.getType(Scanner::class.java), Method.getMethod("String nextLine ()"))
        }
}
