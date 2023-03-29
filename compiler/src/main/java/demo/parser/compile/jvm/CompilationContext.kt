package demo.parser.compile.jvm

import demo.parser.compile.jvm.AsmUtil.generateDefaultConstructor
import demo.parser.domain.Expression
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.Method

internal class CompilationContext(
    private val options: JvmCompileOptions
) {
    private val mainClassType: Type = Type.getObjectType(options.mainClassName)
    private val mainClassWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)
    private val innerClassWriters = mutableMapOf<String, ClassWriter>()

    fun defineMainClass(block: ClassContext.() -> Unit) {
        mainClassWriter.visit(
            V11, ACC_PUBLIC + ACC_SUPER,
            mainClassType.className, null, "java/lang/Object", null
        )
        ClassContext(mainClassType, mainClassWriter, this).apply(block)
    }

    private fun defineInnerClass(
        className: String,
        signature: String?,
        interfaces: Array<String>,
        block: ClassContext.() -> Unit
    ): Type {
        val qualifiedName = "${options.mainClassName}\$$className"

        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        cw.visit(
            V11, ACC_PRIVATE + ACC_STATIC,
            qualifiedName, signature, "java/lang/Object", interfaces
        )
        cw.generateDefaultConstructor()

        innerClassWriters[className] = cw

        ClassContext(Type.getObjectType(qualifiedName), cw, this).apply(block)

        return Type.getObjectType(qualifiedName)
    }

    fun defineLambdaClass(lambda: Expression.Lambda): Type {
        val classIndex = innerClassWriters.size + 1
        val className = "Lambda$classIndex"
        val interfaceType = lambda.type.asmType
        val classSignature = lambda.type.signature

        return defineInnerClass(className, classSignature, arrayOf(interfaceType.internalName)) {
            val method = Method(
                "apply",
                lambda.type.returnType.asmType.wrapperType,
                lambda.type.paramTypes.map { it.asmType.wrapperType }.toTypedArray()
            )

            defineMethod(method, ACC_PUBLIC) {
                val methodStart = Label()
                val methodEnd = Label()

                mv.mark(methodStart)

                lambda.paramNames.forEachIndexed { argIndex, name ->
                    val localIndex = declareVar(name, lambda.type.paramTypes[argIndex])
                    mv.loadArg(argIndex)
                    mv.unbox(lambda.type.paramTypes[argIndex].asmType)
                    mv.storeLocal(localIndex)
                }

                StatementCompiler.compile(lambda.body, this)

                mv.mark(methodEnd)
                writeLocalVarTable(methodStart, methodEnd)
                mv.endMethod()
            }

            val bridgeMethod = Method(
                "apply",
                Type.getType("Ljava/lang/Object;"),
                arrayOf(Type.getType("Ljava/lang/Object;"))
            )

            defineMethod(bridgeMethod, ACC_PUBLIC + ACC_SYNTHETIC + ACC_BRIDGE) {
                mv.loadThis()
                mv.loadArg(0)
                mv.checkCast(lambda.type.paramTypes[0].asmType.wrapperType)
                mv.invokeVirtual(classType, method)
                mv.returnValue()
                mv.endMethod()
            }
        }
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

        return byteCodes
    }

    private fun getQualifiedInnerClassName(className: String) =
        "${options.mainClassName}\$$className"
}


