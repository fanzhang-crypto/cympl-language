package demo.parser.compile.jvm

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method

internal object AsmUtil {

    val DEFAULT_CONSTRUCTOR = Method("<init>", Type.VOID_TYPE, arrayOf())

    fun generateDefaultConstructor(cw: ClassWriter) = with(
        GeneratorAdapter(Opcodes.ACC_PUBLIC, DEFAULT_CONSTRUCTOR, null, null, cw)
    ) {
        loadThis()
        invokeConstructor(Type.getType(Object::class.java), DEFAULT_CONSTRUCTOR)
        returnValue()
        endMethod()
    }

}
