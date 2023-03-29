package demo.parser.compile.jvm

import demo.parser.compile.CompilationException
import demo.parser.domain.BuiltinType
import demo.parser.domain.Statement
import org.objectweb.asm.Type
import org.objectweb.asm.commons.Method
import org.objectweb.asm.signature.SignatureWriter
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Supplier

internal val BuiltinType.asmType
    get():Type = when (this) {
        BuiltinType.VOID -> Type.VOID_TYPE
        BuiltinType.INT -> Type.INT_TYPE
        BuiltinType.BOOL -> Type.BOOLEAN_TYPE
        BuiltinType.FLOAT -> Type.DOUBLE_TYPE
        BuiltinType.STRING -> Type.getType(String::class.java)
        is BuiltinType.ARRAY -> Type.getType("[${elementType.asmType.descriptor}")
        is BuiltinType.FUNCTION -> {
            val javaFunctionalInterface = asJavaFunctionInterface
            Type.getType(javaFunctionalInterface)
        }

        else -> throw CompilationException("unsupported type: $this")
    }

internal val Type.wrapperType
    get() = when (this) {
        Type.INT_TYPE -> Type.getType(Integer::class.java)
        Type.BOOLEAN_TYPE -> Type.getType(Boolean::class.java)
        Type.DOUBLE_TYPE -> Type.getType(Double::class.java)
        else -> this
    }

internal val BuiltinType.jvmDescriptor get() = asmType.descriptor

internal val BuiltinType.FUNCTION.asJavaFunctionInterface
    get() = when (paramTypes.size) {
        0 -> when (returnType) {
            BuiltinType.VOID -> Runnable::class.java
            else -> Supplier::class.java
        }

        1 -> when (returnType) {
            BuiltinType.VOID -> Consumer::class.java
            else -> java.util.function.Function::class.java
        }

        2 -> when (returnType) {
            BuiltinType.VOID -> BiConsumer::class.java
            else -> BiFunction::class.java
        }

        else -> throw CompilationException("unsupported function type: $this")
    }

internal val BuiltinType.signature: String?
    get() = when (this) {
        is BuiltinType.FUNCTION -> SignatureWriter().apply {
            visitClassType("java/lang/Object")
            visitEnd()

            val functionalInterface = asJavaFunctionInterface
            visitClassType(Type.getInternalName(functionalInterface))
            // handle the generic types
            when (functionalInterface) {
                Supplier::class.java -> visitBuiltinType(returnType)

                Consumer::class.java, BiConsumer::class.java ->
                    paramTypes.forEach { visitBuiltinType(it) }

                java.util.function.Function::class.java, BiFunction::class.java -> {
                    paramTypes.forEach { visitBuiltinType(it) }
                    visitBuiltinType(returnType)
                }

                else -> throw CompilationException("unsupported function type: $this")
            }
            visitEnd()
        }.toString()

        else -> null
    }

private fun SignatureWriter.visitBuiltinType(builtinType: BuiltinType) {
    visitTypeArgument('=')
    visitClassType(builtinType.asmType.wrapperType.internalName)
    visitEnd()
}

internal fun Statement.FunctionDeclaration.asMethod(): Method {
    val functionName = id
    val returnType = returnType.asmType
    val argTypes = parameters.map { it.type.asmType }.toTypedArray()
    return Method(functionName, returnType, argTypes)
}
