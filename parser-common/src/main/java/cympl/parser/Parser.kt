package cympl.parser

import java.io.InputStream

interface Parser<T> {
    fun parse(inputStream: InputStream): ParseResult<T>
}

sealed interface ParseResult<T> {
    class Success<T>(val value: T) : ParseResult<T>
    class Failure<T>(val errors: List<ParseException>) : ParseResult<T>
}


