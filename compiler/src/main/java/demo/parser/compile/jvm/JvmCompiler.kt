package demo.parser.compile.jvm

import demo.parser.compile.Compiler
import demo.parser.domain.Program
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter

class JvmCompileOptions(
    val debug: Boolean = false,
    val mainClassName: String
)

class JvmCompiler : Compiler<JvmCompileOptions, ByteArray> {

    private val programCompiler = ProgramCompiler()

    override fun compile(program: Program, options: JvmCompileOptions): ByteArray {
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)

        val cv: ClassVisitor = if (options.debug) {
            val printWriter = PrintWriter(System.out)
            TraceClassVisitor(cw, printWriter)
        } else {
            cw
        }

        val ctx = CompilationContext(cv, options)
        programCompiler.compile(program, ctx)
        cv.visitEnd()

        return cw.toByteArray()
    }
}
