package demo.parser.domain

import java.io.InputStream

interface Parser {
    fun parse(inputStream: InputStream): ParserResult
}

sealed interface ParserResult {
    class Success(val program: Program) : ParserResult
    class Failure(val errors: List<RuntimeException>) : ParserResult
}


