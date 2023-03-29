package demo.parser.compile.jvm

import demo.parser.domain.BuiltinType
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method

internal class ClassContext(
    val classType: Type,
    private val cw: ClassWriter,
    val rootContext: CompilationContext
) {
    fun defineMethod(
        method: Method,
        access: Int = Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC,
        block: MethodContext.() -> Unit
    ) =
        MethodContext(cw, access, method, this, classType.className.contains("Lambda")).apply(block)

    fun declare(name: String, type: BuiltinType, asWrapperType: Boolean = false) {
        val descriptor = if (asWrapperType) type.asmType.wrapperType.descriptor else type.asmType.descriptor
        cw.visitField(
            Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC, name, descriptor, type.signature, null
        ).visitEnd()
    }

    fun set(mv: GeneratorAdapter, name: String, type: BuiltinType) {
        mv.putStatic(classType, name, type.asmType)
    }

    fun get(mv: GeneratorAdapter, name: String, type: BuiltinType) {
        mv.getStatic(classType, name, type.asmType)
    }

    fun increment(mv: GeneratorAdapter, name: String, type: BuiltinType, postfix: Boolean, asStatement: Boolean) {
        get(mv, name, type)

        when (type) {
            BuiltinType.INT -> {
                if (postfix && !asStatement) {
                    mv.dup()
                }
                mv.push(1)
                mv.visitInsn(Opcodes.IADD)
                if (!postfix && !asStatement) {
                    mv.dup()
                }
            }

            BuiltinType.FLOAT -> {
                if (postfix && !asStatement) {
                    mv.dup2()
                }
                mv.push(1.0)
                mv.visitInsn(Opcodes.DADD)
                if (!postfix && !asStatement) {
                    mv.dup2()
                }
            }

            else -> throw IllegalArgumentException("unsupported type: $type")
        }

        set(mv, name, type)
    }

    fun decrement(mv: GeneratorAdapter, name: String, type: BuiltinType, postfix: Boolean, asStatement: Boolean) {
        get(mv, name, type)

        when (type) {
            BuiltinType.INT -> {
                if (postfix && !asStatement) {
                    mv.dup()
                }
                mv.push(1)
                mv.visitInsn(Opcodes.ISUB)
                if (!postfix && !asStatement) {
                    mv.dup()
                }
            }

            BuiltinType.FLOAT -> {
                if (postfix && !asStatement) {
                    mv.dup2()
                }
                mv.push(1.0)
                mv.visitInsn(Opcodes.DSUB)
                if (!postfix && !asStatement) {
                    mv.dup2()
                }
            }

            else -> throw IllegalArgumentException("unsupported type: $type")
        }
        set(mv, name, type)
    }
}
