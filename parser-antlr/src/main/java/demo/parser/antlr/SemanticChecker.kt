package demo.parser.antlr

import demo.parser.domain.SemanticException
import demo.parser.domain.TokenLocation
import demo.parser.domain.VariableType
import org.antlr.v4.runtime.Token

class SemanticChecker {

    private val vars = mutableMapOf<String, VariableType>()

    private val semanticErrors: MutableList<SemanticException> = mutableListOf()

    fun getSemanticErrors(): List<SemanticException> = semanticErrors.toList()

    fun clearSemanticErrors() {
        semanticErrors.clear()
    }

    fun checkVariableDeclared(idToken: Token) {
        val id = idToken.text
        if (!vars.contains(id)) {
            val location = TokenLocation(idToken.line, idToken.charPositionInLine)
            semanticErrors += SemanticException("variable $id not defined", location)
        }
    }

    fun checkVariableUndeclared(idToken: Token, type: VariableType) {
        val id = idToken.text
        if (vars.contains(id)) {
            val location = TokenLocation(idToken.line, idToken.charPositionInLine)
            semanticErrors += SemanticException("variable $id already declared", location)
        } else {
            vars[id] = type
        }
    }
}
