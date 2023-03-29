package demo.parser.compile.jvm

import demo.parser.compile.Compiler
import demo.parser.domain.Program

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
