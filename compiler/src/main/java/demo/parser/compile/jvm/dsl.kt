package demo.parser.compile.jvm

import demo.parser.compile.CompilationException
import demo.parser.domain.BuiltinType
import org.objectweb.asm.Type

internal val BuiltinType.asmType
    get():Type = when (this) {
        BuiltinType.VOID -> Type.VOID_TYPE
        BuiltinType.INT -> Type.INT_TYPE
        BuiltinType.BOOL -> Type.BOOLEAN_TYPE
        BuiltinType.FLOAT -> Type.DOUBLE_TYPE
        BuiltinType.STRING -> Type.getType(String::class.java)
        is BuiltinType.ARRAY -> Type.getType("[${elementType.asmType.descriptor}")
        else -> throw CompilationException("unsupported type: $this")
    }

internal val BuiltinType.jvmDescription get() = asmType.descriptor
