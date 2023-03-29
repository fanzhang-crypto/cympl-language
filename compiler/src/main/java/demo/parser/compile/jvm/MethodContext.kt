package demo.parser.compile.jvm

import demo.parser.domain.BuiltinType
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method

internal class MethodContext(
    cw: ClassWriter,
    access: Int = ACC_PRIVATE + ACC_STATIC,
    private val method: Method,
    val classContext: ClassContext,
    val inLambda: Boolean = false,
) {
    val mv = GeneratorAdapter(access, method, null, null, cw)

    private var namingScope = NamingScope(null)

    private val localVarContext = LocalVarContext(mv, this::namingScope)

    val loopContext = LoopContext()

    fun invokeMethod(methodName: String, argTypes: Array<Type>, returnType: Type) {
        val method = Method(methodName, returnType, argTypes)
        mv.invokeStatic(classContext.classType, method)
    }

    fun declareVar(name: String, type: BuiltinType, asWrapperType: Boolean = false): Int =
        if (inMainMethod && namingScope.isRoot) {
            classContext.declare(name, type, asWrapperType)
            -1
        } else {
            localVarContext.declare(name, type, asWrapperType)
        }

    fun getVar(name: String, type: BuiltinType) {
        if ((inMainMethod && namingScope.isRoot) || !localVarContext.contains(name)) {
            classContext.get(mv, name, type)
        } else {
            localVarContext.get(name)
        }
    }

    fun setVar(name: String, type: BuiltinType) {
        if ((inMainMethod && namingScope.isRoot) || !localVarContext.contains(name)) {
            classContext.set(mv, name, type)
        } else {
            localVarContext.set(name)
        }
    }

    fun increment(name: String, type: BuiltinType, postfix: Boolean, asStatement: Boolean) {
        if ((inMainMethod && namingScope.isRoot) || !localVarContext.contains(name)) {
            classContext.increment(mv, name, type, postfix, asStatement)
        } else {
            localVarContext.increment(name, type, postfix, asStatement)
        }
    }

    fun decrement(name: String, type: BuiltinType, postfix: Boolean, asStatement: Boolean) {
        if ((inMainMethod && namingScope.isRoot) || !localVarContext.contains(name)) {
            classContext.decrement(mv, name, type, postfix, asStatement)
        } else {
            localVarContext.decrement(name, type, postfix, asStatement)
        }
    }

    fun writeLocalVarTable(methodStart: Label, methodEnd: Label) {
        localVarContext.writeLocalVarTable(methodStart, methodEnd)
    }

    fun enterScope() {
        namingScope = NamingScope(namingScope)
    }

    fun exitScope() {
        namingScope = namingScope.parent!!
    }

    private val inMainMethod: Boolean get() = method.name == "main"


}
