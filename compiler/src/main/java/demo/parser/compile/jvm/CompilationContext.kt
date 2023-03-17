package demo.parser.compile.jvm

import demo.parser.domain.BuiltinType
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method

internal class CompilationContext(
    private val cv: ClassVisitor,
    options: JvmCompileOptions
) {

    private val mainClassType: Type = Type.getObjectType(options.mainClassName)

    lateinit var mv: GeneratorAdapter

    private lateinit var localVarContext: LocalVarContext
    private lateinit var globalVarContext: GlobalVarContext

    private lateinit var currentMethod: Method

    private val inMainMethod: Boolean get() = currentMethod.name == "main"

    fun defineMainClass() {
        cv.visit(
            V11, ACC_PUBLIC + ACC_SUPER,
            mainClassType.className, null, "java/lang/Object", null
        )
    }

    fun defineMethod(method: Method, access: Int = ACC_PRIVATE + ACC_STATIC) {
        currentMethod = method

        mv = GeneratorAdapter(access, method, null, null, cv)
        globalVarContext = GlobalVarContext(mainClassType, cv, mv)
        localVarContext = LocalVarContext(mv, this::namingScope)
    }

    fun invokeMethod(methodName: String, argTypes: Array<Type>, returnType: Type) {
        val method = Method(methodName, returnType, argTypes)
        mv.invokeStatic(mainClassType, method)
    }

    fun declareVar(name: String, type: BuiltinType): Int =
        if (inMainMethod && namingScope.isRoot) {
            globalVarContext.declare(name, type)
        } else {
            localVarContext.declare(name, type)
        }

    fun getVar(name: String, type: BuiltinType) {
        if ((inMainMethod && namingScope.isRoot) || !localVarContext.contains(name)) {
            globalVarContext.get(name, type)
        } else {
            localVarContext.get(name, type)
        }
    }

    fun setVar(name: String, type: BuiltinType) {
        if ((inMainMethod && namingScope.isRoot) || !localVarContext.contains(name)) {
            globalVarContext.set(name, type)
        } else {
            localVarContext.set(name, type)
        }
    }

    fun increment(name: String, type: BuiltinType, postfix: Boolean, asStatement: Boolean) {
        if ((inMainMethod && namingScope.isRoot) || !localVarContext.contains(name)) {
            globalVarContext.increment(name, type, postfix, asStatement)
        } else {
            localVarContext.increment(name, type, postfix, asStatement)
        }
    }

    fun decrement(name: String, type: BuiltinType, postfix: Boolean, asStatement: Boolean) {
        if ((inMainMethod && namingScope.isRoot) || !localVarContext.contains(name)) {
            globalVarContext.decrement(name, type, postfix, asStatement)
        } else {
            localVarContext.decrement(name, type, postfix, asStatement)
        }
    }

    private var namingScope = NamingScope(null)

    fun enterScope() {
        namingScope = NamingScope(namingScope)
    }

    fun exitScope() {
        namingScope = namingScope.parent!!
    }

    fun writeLocalVarTable(methodStart: Label, methodEnd: Label) {
        localVarContext.writeLocalVarTable(methodStart, methodEnd)
    }

    val loopContext = LoopContext()

    var returnGenerated: Boolean = false
}


