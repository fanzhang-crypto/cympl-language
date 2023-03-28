package demo.parser.compile.jvm

import demo.parser.domain.BuiltinType
import demo.parser.domain.Expression
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method

internal class CompilationContext(
    private val options: JvmCompileOptions
) {

    private val mainClassType: Type = Type.getObjectType(options.mainClassName)

    private val mainClassWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)

    private val innerClassWriters = mutableMapOf<String, ClassWriter>()

    lateinit var mv: GeneratorAdapter

    private lateinit var localVarContext: LocalVarContext
    private lateinit var globalVarContext: GlobalVarContext

    private lateinit var currentMethod: Method

    private val inMainMethod: Boolean get() = currentMethod.name == "main"

    fun toByteArrays(): Map<String, ByteArray> {
        innerClassWriters.forEach { (innerClassName, cw) ->
            mainClassWriter.visitInnerClass(
                "${options.mainClassName}\$$innerClassName",
                options.mainClassName,
                innerClassName,
                ACC_STATIC + ACC_PRIVATE
            )
            mainClassWriter.visitNestMember("${options.mainClassName}\$$innerClassName")

            cw.visitInnerClass(
                "${options.mainClassName}\$$innerClassName",
                options.mainClassName,
                innerClassName,
                ACC_STATIC + ACC_PRIVATE
            )
            cw.visitNestHost(options.mainClassName)
            cw.visitEnd()
        }
        mainClassWriter.visitEnd()

        val byteArrays = mutableMapOf<String, ByteArray>()
        byteArrays[options.mainClassName] = mainClassWriter.toByteArray()
        innerClassWriters.forEach { (className, cw) ->
            val qualifiedName = "${options.mainClassName}\$$className"
            byteArrays[qualifiedName] = cw.toByteArray()
        }
        return byteArrays
    }

    fun defineMainClass() {
        mainClassWriter.visit(
            V11, ACC_PUBLIC + ACC_SUPER,
            mainClassType.className, null, "java/lang/Object", null
        )
    }

    fun defineLambdaClass(lambda: Expression.Lambda): Type {
        val classIndex = innerClassWriters.size + 1
        val className = "Lambda$classIndex"
        val qualifiedName = "${options.mainClassName}\$$className"

        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)

        val interfaceType = lambda.type.asmType
        val classSignature = lambda.type.signature

        cw.visit(
            V11,
            ACC_PRIVATE + ACC_STATIC,
            qualifiedName,
            classSignature,
            "java/lang/Object",
            arrayOf(interfaceType.internalName)
        )
        innerClassWriters[className] = cw
        AsmUtil.generateDefaultConstructor(cw)

        val mv = GeneratorAdapter(
            ACC_PUBLIC,
            Method(
                "apply",
                lambda.type.returnType.asmType,
                lambda.type.paramTypes.map { it.asmType }.toTypedArray()
            ),
            null,
            null,
            cw
        )
        mv.visitCode()
        mv.push(1)
        mv.returnValue()
        mv.endMethod()

        return Type.getObjectType(qualifiedName)
    }

    fun defineMethod(method: Method, access: Int = ACC_PRIVATE + ACC_STATIC) {
        currentMethod = method

        mv = GeneratorAdapter(access, method, null, null, mainClassWriter)
        globalVarContext = GlobalVarContext(mainClassType, mainClassWriter, mv)
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


