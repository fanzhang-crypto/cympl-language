package demo.parser.compile.jvm

import demo.parser.compile.CompilationException
import demo.parser.domain.BuiltinType
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter

internal sealed interface VariableContext {

    fun declare(name: String, type: BuiltinType): Int

    fun set(name: String, type: BuiltinType)

    fun get(name: String, type: BuiltinType)

    fun increment(name: String, type: BuiltinType, postfix: Boolean, asStatement: Boolean)

    fun decrement(name: String, type: BuiltinType, postfix: Boolean, asStatement: Boolean)
}

internal class GlobalVarContext(
    private val mainClassType: Type,
    private val cv: ClassVisitor,
    private val mv: GeneratorAdapter
) : VariableContext {

    override fun declare(name: String, type: BuiltinType): Int {
        cv.visitField(
            Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC, name, type.jvmDescription, null, null
        ).visitEnd()
        return -1
    }

    override fun set(name: String, type: BuiltinType) {
        mv.putStatic(mainClassType, name, type.asmType)
    }

    override fun get(name: String, type: BuiltinType) {
        mv.getStatic(mainClassType, name, type.asmType)
    }

    override fun increment(name: String, type: BuiltinType, postfix: Boolean, asStatement: Boolean) {
        get(name, type)

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

        set(name, type)
    }

    override fun decrement(name: String, type: BuiltinType, postfix: Boolean, asStatement: Boolean) {
        get(name, type)

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
        set(name, type)
    }
}

internal class LocalVarContext(
    private val mv: GeneratorAdapter,
    private val namingScope: () -> NamingScope
): VariableContext {

    private class LocalVarSlot(val index: Int, val type: BuiltinType)

    private val table = mutableMapOf<String, LocalVarSlot>()

    override fun declare(name: String, type: BuiltinType): Int {
        val index = mv.newLocal(type.asmType)

        // resolve local variable name conflicts
        val mappedName = namingScope().add(name)
        table[mappedName] = LocalVarSlot(index, type)

        return index
    }

    override fun get(name: String, type: BuiltinType) {
        val index = indexOf(name)!!
        mv.loadLocal(index)
    }

    override fun set(name: String, type: BuiltinType) {
        val index = indexOf(name)!!
        mv.storeLocal(index)
    }

    fun contains(name: String) = indexOf(name) != null

    override fun increment(name: String, type: BuiltinType, postfix: Boolean, asStatement: Boolean) {
        val index = indexOf(name)!!
        when (type) {
            BuiltinType.INT -> {
                if (postfix) {
                    if (!asStatement) {
                        mv.loadLocal(index)
                    }
                    mv.iinc(index, 1)
                } else {
                    mv.iinc(index, 1)
                    if (!asStatement) {
                        mv.loadLocal(index)
                    }
                }
            }

            BuiltinType.FLOAT -> {
                if (postfix) {
                    mv.loadLocal(index)
                    if (!asStatement) {
                        mv.dup2()
                    }
                    mv.push(1.0)
                    mv.visitInsn(Opcodes.DADD)
                    mv.storeLocal(index)
                } else {
                    mv.loadLocal(index)
                    mv.push(1.0)
                    mv.visitInsn(Opcodes.DADD)
                    if (!asStatement) {
                        mv.dup2()
                    }
                    mv.storeLocal(index)
                }
            }

            else -> throw CompilationException("unsupported type for increment: $type")
        }
    }

    override fun decrement(name: String, type: BuiltinType, postfix: Boolean, asStatement: Boolean) {
        val index = indexOf(name)!!
        when (type) {
            BuiltinType.INT -> {
                if (postfix) {
                    if (!asStatement) {
                        mv.loadLocal(index)
                    }
                    mv.iinc(index, -1)
                } else {
                    mv.iinc(index, -1)
                    if (!asStatement) {
                        mv.loadLocal(index)
                    }
                }
            }

            BuiltinType.FLOAT -> {
                if (postfix) {
                    mv.loadLocal(index)
                    if (!asStatement) {
                        mv.dup2()
                    }
                    mv.push(1.0)
                    mv.visitInsn(Opcodes.DSUB)
                    mv.storeLocal(index)
                } else {
                    mv.loadLocal(index)
                    mv.push(1.0)
                    mv.visitInsn(Opcodes.DSUB)
                    if (!asStatement) {
                        mv.dup2()
                    }
                    mv.storeLocal(index)
                }
            }

            else -> throw CompilationException("unsupported type for decrement: $type")
        }
    }

    private fun indexOf(name: String): Int? {
        val mappedName = namingScope().get(name) ?: name
        return table[mappedName]?.index
    }

    fun writeLocalVarTable(methodStart: Label, methodEnd: Label) {
        table.forEach { (name, slot) ->
            mv.visitLocalVariable(name, slot.type.jvmDescription, null, methodStart, methodEnd, slot.index)
        }
    }
}
