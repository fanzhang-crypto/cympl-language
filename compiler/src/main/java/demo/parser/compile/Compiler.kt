package demo.parser.compile

import demo.parser.domain.Program

interface Compiler<O, T> {
    fun compile(program: Program, options: O): T
}
