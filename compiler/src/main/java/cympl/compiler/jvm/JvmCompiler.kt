package cympl.compiler.jvm

import cympl.compiler.Compiler
import cympl.language.Program

class JvmCompileOptions(
    val debug: Boolean = false,
    val mainClassName: String
)

class JvmCompiler : Compiler<JvmCompileOptions, Map<String, ByteArray>> {

    override fun compile(program: Program, options: JvmCompileOptions): Map<String, ByteArray> {
        val ctx = CompilationContext(options)
        ProgramCompiler.compile(program, ctx)
        return ctx.toByteCodes()
    }
}
