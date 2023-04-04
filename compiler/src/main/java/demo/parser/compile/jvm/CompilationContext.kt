package demo.parser.compile.jvm

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type

internal class CompilationContext(
    private val options: JvmCompileOptions
) {
    private lateinit var mainClassWriter: ClassWriter

    val innerClassWriters = mutableMapOf<String, ClassWriter>()
    val outerClassWriters = mutableMapOf<String, ClassWriter>()

    fun defineMainClass(block: ClassContext.() -> Unit) {
        mainClassWriter = createClassWriter(options.mainClassName, null, emptyArray(), ACC_PUBLIC + ACC_SUPER)
        val mainClassType: Type = Type.getObjectType(options.mainClassName)
        ClassContext(mainClassType, mainClassWriter, this).apply(block)
    }

    fun defineInnerClass(
        className: String,
        signature: String?,
        interfaces: Array<String>,
        access: Int = ACC_PRIVATE + ACC_STATIC,
        block: ClassContext.() -> Unit
    ): Type {
        val internalName = "${options.mainClassName}\$$className"
        val cw = createClassWriter(internalName, signature, interfaces, access)
        innerClassWriters[className] = cw

        return Type.getObjectType(internalName).also {
            ClassContext(it, cw, this).apply(block)
        }
    }

    inline fun defineOuterClass(
        qualifiedName: String,
        signature: String?,
        interfaces: Array<String>,
        access: Int,
        block: ClassContext.() -> Unit
    ): Type {
        val internalName = qualifiedName.replace('.', '/')
        val cw = createClassWriter(internalName, signature, interfaces, access)
        outerClassWriters[qualifiedName] = cw

        return Type.getObjectType(internalName).also {
            ClassContext(it, cw, this).apply(block)
        }
    }

    private fun createClassWriter(
        internalName: String, signature: String?,
        interfaces: Array<String>, access: Int,
    ): ClassWriter {
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        cw.visit(V11, access, internalName, signature, "java/lang/Object", interfaces)
        return cw
    }

    fun toByteCodes(): Map<String, ByteArray> {
        val byteCodes = mutableMapOf<String, ByteArray>()

        innerClassWriters.forEach { (innerClassName, cw) ->
            val qualifiedName = getQualifiedInnerClassName(innerClassName)

            mainClassWriter.visitInnerClass(
                qualifiedName, options.mainClassName,
                innerClassName, ACC_STATIC + ACC_PRIVATE
            )
            mainClassWriter.visitNestMember(qualifiedName)

            cw.visitInnerClass(
                qualifiedName, options.mainClassName,
                innerClassName, ACC_STATIC + ACC_PRIVATE
            )
            cw.visitNestHost(options.mainClassName)
            cw.visitEnd()
            byteCodes[qualifiedName] = cw.toByteArray()
        }
        mainClassWriter.visitEnd()

        byteCodes[options.mainClassName] = mainClassWriter.toByteArray()

        outerClassWriters.forEach { (name, cw) ->
            byteCodes[name] = cw.toByteArray()
        }

        return byteCodes
    }

    private fun getQualifiedInnerClassName(className: String) =
        "${options.mainClassName}\$$className"
}
