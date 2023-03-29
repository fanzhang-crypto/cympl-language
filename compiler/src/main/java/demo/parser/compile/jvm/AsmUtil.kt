package demo.parser.compile.jvm

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method

internal object AsmUtil {

    val DEFAULT_CONSTRUCTOR = Method("<init>", Type.VOID_TYPE, arrayOf())

    fun ClassWriter.generateDefaultConstructor() = with(
        GeneratorAdapter(ACC_PUBLIC, DEFAULT_CONSTRUCTOR, null, null, this)
    ) {
        loadThis()
        invokeConstructor(Type.getType(Object::class.java), DEFAULT_CONSTRUCTOR)
        returnValue()
        endMethod()
    }

}
