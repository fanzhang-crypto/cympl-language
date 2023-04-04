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

internal val BuiltinType.signature: String
    get() = SignatureWriter().also { it.visitBuiltinType(this) }.toString()

private fun SignatureWriter.visitBuiltinType(builtinType: BuiltinType, asWrapperType: Boolean = false) {
    when (builtinType) {
        is BuiltinType.FUNCTION -> {
            if (builtinType.isFirstClass) {
                val functionalInterface = builtinType.asJavaFunctionInterface
                visitClassType(Type.getInternalName(functionalInterface))
                // handle the generic types
                builtinType.paramTypes.forEach {
                    visitTypeArgument('=')
                    visitBuiltinType(it, true)
                    visitEnd()
                }
                builtinType.returnType.let {
                    visitTypeArgument('=')
                    visitBuiltinType(it, true)
                    visitEnd()
                }
                visitEnd()
            } else {
                visitParameterType()
                builtinType.paramTypes.forEach {
                    visitBuiltinType(it)
                }
                visitReturnType()
                visitBuiltinType(builtinType.returnType)
            }
        }
        is BuiltinType.ARRAY -> {
            visitArrayType()
            visitBuiltinType(builtinType.elementType, true)
        }
        BuiltinType.VOID, BuiltinType.BOOL, BuiltinType.INT, BuiltinType.FLOAT ->
            if(asWrapperType)
                visitClassType(builtinType.asmType.wrapperType.internalName)
            else
                visitBaseType(builtinType.jvmDescriptor[0])

        BuiltinType.STRING -> {
            visitClassType(builtinType.asmType.internalName)
        }
        BuiltinType.ANY -> visitClassType(builtinType.asmType.internalName)
    }
}


internal fun Statement.FunctionDeclaration.asMethod(): Method {
    val functionName = id
    val returnType = returnType.asmType
    val argTypes = parameters.map { it.type.asmType }.toTypedArray()
    return Method(functionName, returnType, argTypes)
}
