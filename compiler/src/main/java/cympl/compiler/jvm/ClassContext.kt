package cympl.compiler.jvm

import cympl.language.BuiltinType
import cympl.language.Expression
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method

internal class ClassContext(
    val classType: Type,
    private val cw: ClassWriter,
    val rootContext: CompilationContext
) {
    fun defineConstructor(initializingParams: List<Expression.Variable>, block: MethodContext.() -> Unit) {
        val constructor = Method("<init>", Type.VOID_TYPE, initializingParams.map { it.type.asmType }.toTypedArray())
        defineMethod(ACC_PUBLIC, constructor, block = block)
    }

    inline fun defineMethod(
        access: Int,
        method: Method,
        signature: String? = null,
        block: MethodContext.() -> Unit
    ) {
        val inLambda = classType.className.contains("Lambda")
        MethodContext(cw, access, method, signature, this, inLambda).apply(block)
    }

    private val instanceFieldNames = mutableSetOf<String>()
    private val staticFieldNames = mutableSetOf<String>()

    fun declareField(access: Int, name: String, type: BuiltinType, asWrapperType: Boolean = false) {
        val descriptor = if (asWrapperType) type.asmType.wrapperType.descriptor else type.asmType.descriptor
        cw.visitField(access, name, descriptor, type.signature, null).visitEnd()

        if (access and ACC_STATIC != 0)
            staticFieldNames += name
        else
            instanceFieldNames += name
    }

    fun set(mv: GeneratorAdapter, name: String, type: BuiltinType, value: () -> Unit) {
        if (name in instanceFieldNames) {
            mv.loadThis()
            value()
            mv.putField(classType, name, type.asmType)
        } else {
            value()
            mv.putStatic(classType, name, type.asmType)
        }
    }

    fun get(mv: GeneratorAdapter, name: String, type: BuiltinType) {
        if (name in instanceFieldNames) {
            mv.loadThis()
            mv.getField(classType, name, type.asmType)
        } else {
            mv.getStatic(classType, name, type.asmType)
        }
    }

    fun increment(mv: GeneratorAdapter, name: String, type: BuiltinType, postfix: Boolean, asStatement: Boolean) {
        get(mv, name, type)

        when (type) {
            BuiltinType.INT -> {
                if (postfix && !asStatement) {
                    mv.dup()
                }
                mv.push(1)
                mv.visitInsn(IADD)
                if (!postfix && !asStatement) {
                    mv.dup()
                }
            }

            BuiltinType.FLOAT -> {
                if (postfix && !asStatement) {
                    mv.dup2()
                }
                mv.push(1.0)
                mv.visitInsn(DADD)
                if (!postfix && !asStatement) {
                    mv.dup2()
                }
            }

            else -> throw IllegalArgumentException("unsupported type: $type")
        }

        set(mv, name, type) {}
    }

    fun decrement(mv: GeneratorAdapter, name: String, type: BuiltinType, postfix: Boolean, asStatement: Boolean) {
        get(mv, name, type)

        when (type) {
            BuiltinType.INT -> {
                if (postfix && !asStatement) {
                    mv.dup()
                }
                mv.push(1)
                mv.visitInsn(ISUB)
                if (!postfix && !asStatement) {
                    mv.dup()
                }
            }

            BuiltinType.FLOAT -> {
                if (postfix && !asStatement) {
                    mv.dup2()
                }
                mv.push(1.0)
                mv.visitInsn(DSUB)
                if (!postfix && !asStatement) {
                    mv.dup2()
                }
            }

            else -> throw IllegalArgumentException("unsupported type: $type")
        }
        set(mv, name, type){}
    }
}
