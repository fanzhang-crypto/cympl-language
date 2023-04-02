package demo.parser.compile.jvm

import demo.parser.domain.BuiltinType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SignatureTest {

    @Test
    fun `signature of high order function type that take a (int)int param`() {
        val paramTypes = listOf(
            BuiltinType.FUNCTION(listOf(BuiltinType.INT), BuiltinType.INT).apply { isFirstClass = true }
        )
        val returnType = BuiltinType.VOID
        val funcType = BuiltinType.FUNCTION(paramTypes, returnType)

        funcType.signature shouldBe "(Lcympl/runtime/Function1<Ljava/lang/Integer;Ljava/lang/Integer;>;)V"
    }

    @Test
    fun `signature of high order function type that return a (int)int`() {
        val paramTypes = listOf(
            BuiltinType.INT
        )
        val returnType = BuiltinType.FUNCTION(listOf(BuiltinType.INT), BuiltinType.INT).apply { isFirstClass = true }
        val funcType = BuiltinType.FUNCTION(paramTypes, returnType)

        funcType.signature shouldBe "(I)Lcympl/runtime/Function1<Ljava/lang/Integer;Ljava/lang/Integer;>;"
    }

    @Test
    fun `signature of high order function type that return a (int)int-array`() {
        val paramTypes = listOf(
            BuiltinType.INT
        )
        val returnType = BuiltinType.FUNCTION(listOf(BuiltinType.INT), BuiltinType.ARRAY(BuiltinType.INT)).apply { isFirstClass = true }
        val funcType = BuiltinType.FUNCTION(paramTypes, returnType)

        funcType.signature shouldBe "(I)Lcympl/runtime/Function1<Ljava/lang/Integer;[Ljava/lang/Integer;>;"

    }
}
