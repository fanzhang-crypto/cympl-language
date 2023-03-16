package demo.parser.compile

import demo.parser.domain.Program

interface ByteCodeCompiler {
    fun compile(program: Program): ByteArray
}
