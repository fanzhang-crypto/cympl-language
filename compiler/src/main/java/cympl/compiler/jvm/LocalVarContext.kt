package cympl.compiler.jvm

import cympl.compiler.CompilationException
import cympl.language.BuiltinType
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.GeneratorAdapter

internal class LocalVarContext(
    private val mv: GeneratorAdapter,
    private val namingScope: () -> NamingScope
) {

    private class LocalVarSlot(val index: Int, val type: BuiltinType)

    private val table = mutableMapOf<String, LocalVarSlot>()

    fun declare(name: String, type: BuiltinType, asWrapperType: Boolean = false): Int {
        val asmType = if (asWrapperType) type.asmType.wrapperType else type.asmType
        val index = mv.newLocal(asmType)

        // resolve local variable name conflicts
        val mappedName = namingScope().add(name)
        table[mappedName] = LocalVarSlot(index, type)

        return index
    }

    fun get(name: String) {
        val index = indexOf(name)!!
        mv.loadLocal(index)
    }

    fun set(name: String) {
        val index = indexOf(name)!!
        mv.storeLocal(index)
    }

    fun contains(name: String) = indexOf(name) != null

    fun increment(name: String, type: BuiltinType, postfix: Boolean, asStatement: Boolean) {
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

    fun decrement(name: String, type: BuiltinType, postfix: Boolean, asStatement: Boolean) {
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
            mv.visitLocalVariable(name, slot.type.jvmDescriptor, null, methodStart, methodEnd, slot.index)
        }
    }
}
