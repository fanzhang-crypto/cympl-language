package cympl.compiler.jvm

import cympl.runtime.*
import cympl.language.*
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.Method
import org.objectweb.asm.signature.SignatureWriter

internal object LambdaCompiler {

    fun compile(lambda: Expression.Lambda, ctx: CompilationContext): Type {
        val classIndex = ctx.innerClassWriters.size + 1
        val className = "Lambda$classIndex"

        val lambdaType = lambda.type
        val interfaceType = lambdaType.asmType
        val classSignature = lambdaType.signature

        if (!ctx.outerClassWriters.contains(interfaceType.className)) {
            compileFunctionInterface(lambda.type.asJavaFunctionInterface, ctx)
        }

        return ctx.defineInnerClass(className, classSignature, arrayOf(interfaceType.internalName)) {
            lambda.captures.forEach { (name, type) ->
                declareField(ACC_PRIVATE, name, type)
            }

            defineConstructor(lambda.captures) {
                mv.loadThis()
                mv.invokeConstructor(Type.getType(Object::class.java), Method("<init>", Type.VOID_TYPE, emptyArray()))
                lambda.captures.forEachIndexed { index, param ->
                    mv.loadThis()
                    mv.loadArg(index)
                    mv.putField(classType, param.id, param.type.asmType)
                }
                mv.returnValue()
                mv.endMethod()
            }

            val applyMethod = defineApplyMethod(lambda, this)
            defineBridgeMethod(applyMethod, lambdaType, interfaceType, this)
        }
    }

    private fun defineApplyMethod(lambda: Expression.Lambda, ctx: ClassContext): Method {
        val lambdaType = lambda.type
        val method = Method(
            "apply",
            lambdaType.returnType.asmType.wrapperType,
            lambdaType.paramTypes.map { it.asmType.wrapperType }.toTypedArray()
        )

        ctx.defineMethod(ACC_PUBLIC, method) {
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
        return method
    }

    private fun defineBridgeMethod(
        delegate: Method,
        lambdaType: BuiltinType.FUNCTION,
        interfaceType: Type,
        ctx: ClassContext
    ) {
        val bridgeMethod = getApplyBridgeMethod(interfaceType)

        ctx.defineMethod(ACC_PUBLIC + ACC_SYNTHETIC + ACC_BRIDGE, bridgeMethod) {
            mv.loadThis()

            lambdaType.paramTypes.forEachIndexed { argIndex, type ->
                mv.loadArg(argIndex)
                mv.checkCast(type.asmType.wrapperType)
            }
            mv.invokeVirtual(ctx.classType, delegate)
            if (lambdaType.returnType == BuiltinType.VOID) {
                mv.push(null as String?)
            }
            mv.returnValue()
            mv.endMethod()
        }
    }

    private val APPLY0_BRIDGE = Method.getMethod("Object apply()")
    private val APPLY1_BRIDGE = Method.getMethod("Object apply(Object)")
    private val APPLY2_BRIDGE = Method.getMethod("Object apply(Object, Object)")

    fun getApplyBridgeMethod(type: Type): Method = when (type.className) {
        Function0::class.java.name -> APPLY0_BRIDGE
        Function1::class.java.name -> APPLY1_BRIDGE
        Function2::class.java.name -> APPLY2_BRIDGE
        else -> throw IllegalArgumentException("Unsupported functional interface: $type")
    }

    private fun compileFunctionInterface(clazz: Class<*>, ctx: CompilationContext): Type = when (clazz) {
        Function0::class.java -> compileFunction0(ctx)
        Function1::class.java -> compileFunction1(ctx)
        Function2::class.java -> compileFunction2(ctx)
        else -> throw IllegalArgumentException("Unsupported functional interface: $clazz")
    }

    private fun compileFunction0(ctx: CompilationContext): Type {
        val classSignature = SignatureWriter().apply {
            visitFormalTypeParameter("R")
            visitClassType("java/lang/Object")
            visitEnd()

            visitSuperclass()
            visitClassType("java/lang/Object")
            visitEnd()
        }.toString()

        return ctx.defineOuterClass(
            Function0::class.java.name,
            classSignature,
            emptyArray(),
            ACC_PUBLIC + ACC_INTERFACE + ACC_ABSTRACT
        ) {
            val methodSignature = SignatureWriter().apply {
                visitReturnType()
                visitTypeVariable("R")
            }.toString()

            defineMethod(
                ACC_PUBLIC + ACC_ABSTRACT,
                Method.getMethod("Object apply ()"),
                methodSignature
            ) {
                mv.visitEnd()
            }
        }

    }

    private fun compileFunction1(ctx: CompilationContext): Type {
        val classSignature = SignatureWriter().apply {
            visitFormalTypeParameter("T1")
            visitClassType("java/lang/Object")
            visitEnd()
            visitFormalTypeParameter("R")
            visitClassType("java/lang/Object")
            visitEnd()

            visitSuperclass()
            visitClassType("java/lang/Object")
            visitEnd()
        }.toString()

        return ctx.defineOuterClass(
            Function1::class.java.name,
            classSignature,
            emptyArray(),
            ACC_PUBLIC + ACC_INTERFACE + ACC_ABSTRACT
        ) {
            val methodSignature = SignatureWriter().apply {
                visitParameterType()
                visitTypeVariable("T1")

                visitReturnType()
                visitTypeVariable("R")
            }.toString()

            defineMethod(
                ACC_PUBLIC + ACC_ABSTRACT,
                Method.getMethod("Object apply (Object)"),
                methodSignature
            ) {
                mv.visitEnd()
            }
        }
    }

    private fun compileFunction2(ctx: CompilationContext): Type {
        val classSignature = SignatureWriter().apply {
            visitFormalTypeParameter("T1")
            visitClassType("java/lang/Object")
            visitEnd()
            visitFormalTypeParameter("T2")
            visitClassType("java/lang/Object")
            visitEnd()
            visitFormalTypeParameter("R")
            visitClassType("java/lang/Object")
            visitEnd()

            visitSuperclass()
            visitClassType("java/lang/Object")
            visitEnd()
        }.toString()

        return ctx.defineOuterClass(
            Function2::class.java.name,
            classSignature,
            emptyArray(),
            ACC_PUBLIC + ACC_INTERFACE + ACC_ABSTRACT
        ) {
            val methodSignature = SignatureWriter().apply {
                visitParameterType()
                visitTypeVariable("T1")
                visitParameterType()
                visitTypeVariable("T2")

                visitReturnType()
                visitTypeVariable("R")
            }.toString()

            defineMethod(
                ACC_PUBLIC + ACC_ABSTRACT,
                Method.getMethod("Object apply (Object, Object)"),
                methodSignature
            ) {
                mv.visitEnd()
            }
        }
    }
}
