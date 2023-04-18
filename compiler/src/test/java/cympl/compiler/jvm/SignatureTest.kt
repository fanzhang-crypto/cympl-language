package cympl.compiler.jvm

import cympl.language.BuiltinType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SignatureTest {

    @Test
    fun `signature of high order function type that take a (int)int param`() {
        val paramTypes = listOf(
            cympl.language.BuiltinType.FUNCTION(listOf(cympl.language.BuiltinType.INT), cympl.language.BuiltinType.INT).apply { isFirstClass = true }
        )
        val returnType = cympl.language.BuiltinType.VOID
        val funcType = cympl.language.BuiltinType.FUNCTION(paramTypes, returnType)

        funcType.signature shouldBe "(Lcympl/runtime/Function1<Ljava/lang/Integer;Ljava/lang/Integer;>;)V"
    }

    @Test
    fun `signature of high order function type that return a (int)int`() {
        val paramTypes = listOf(
            cympl.language.BuiltinType.INT
        )
        val returnType = cympl.language.BuiltinType.FUNCTION(listOf(cympl.language.BuiltinType.INT), cympl.language.BuiltinType.INT).apply { isFirstClass = true }
        val funcType = cympl.language.BuiltinType.FUNCTION(paramTypes, returnType)

        funcType.signature shouldBe "(I)Lcympl/runtime/Function1<Ljava/lang/Integer;Ljava/lang/Integer;>;"
    }

    @Test
    fun `signature of high order function type that return a (int)int-array`() {
        val paramTypes = listOf(
            cympl.language.BuiltinType.INT
        )
        val returnType = cympl.language.BuiltinType.FUNCTION(listOf(cympl.language.BuiltinType.INT), cympl.language.BuiltinType.ARRAY(
            cympl.language.BuiltinType.INT)).apply { isFirstClass = true }
        val funcType = cympl.language.BuiltinType.FUNCTION(paramTypes, returnType)

        funcType.signature shouldBe "(I)Lcympl/runtime/Function1<Ljava/lang/Integer;[Ljava/lang/Integer;>;"

    }
}
