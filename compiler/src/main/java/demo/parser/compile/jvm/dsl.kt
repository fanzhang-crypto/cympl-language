package demo.parser.compile.jvm

import cympl.runtime.*
import demo.parser.compile.CompilationException
import demo.parser.domain.BuiltinType
import demo.parser.domain.Statement
import org.objectweb.asm.Type
import org.objectweb.asm.commons.Method
import org.objectweb.asm.signature.SignatureWriter

internal val BuiltinType.asmType
    get():Type = when (this) {
        BuiltinType.VOID -> Type.VOID_TYPE
        BuiltinType.INT -> Type.INT_TYPE
        BuiltinType.BOOL -> Type.BOOLEAN_TYPE
        BuiltinType.FLOAT -> Type.DOUBLE_TYPE
        BuiltinType.STRING -> Type.getType(String::class.java)
        is BuiltinType.ARRAY -> Type.getType("[${elementType.asmType.descriptor}")
        is BuiltinType.FUNCTION -> Type.getType(asJavaFunctionInterface)

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
        0 -> Function0::class.java
        1 -> Function1::class.java
        2 -> Function2::class.java
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
            paramTypes.forEach { visitBuiltinType(it) }
            visitBuiltinType(returnType)
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
