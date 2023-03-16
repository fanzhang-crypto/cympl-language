package demo.parser.compile

import demo.parser.domain.Program
import javassist.ClassPool
import javassist.CtNewMethod

class JavassitBytecodeCompiler: ByteCodeCompiler {

    override fun compile(program: Program): ByteArray {
        val pool = ClassPool.getDefault()
        val cc= pool.makeClass(DEFAULT_MAIN_CLASS_NAME)

        val method = CtNewMethod.make("public static void main(String[] args){}", cc)
//        method.setBody("System.out.println(\"Hello World\");")
        method.insertBefore("""
            String a = "hello";
            String b = "world";
            System.out.println(!a.equals(b));
            """.trimIndent())
        cc.addMethod(method)

        return cc.toBytecode()
    }

    companion object {
        const val DEFAULT_MAIN_CLASS_NAME = "JavassitGeneratedMain"
    }
}
