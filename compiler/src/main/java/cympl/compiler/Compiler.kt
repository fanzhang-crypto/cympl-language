package cympl.compiler

import cympl.language.Program

interface Compiler<O, T> {
    fun compile(program: Program, options: O): T
}
