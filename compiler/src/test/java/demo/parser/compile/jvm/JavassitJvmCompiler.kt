package demo.parser.compile.jvm

import javassist.ClassPool
import javassist.CtField
import javassist.CtNewMethod
import java.io.File

class JavassitJvmCompiler {

    companion object {
        private const val DEFAULT_MAIN_CLASS_NAME = "JavassitGeneratedMain"

        @JvmStatic
        fun main(args: Array<String>) {
            val pool = ClassPool.getDefault()
            val cc = pool.makeClass(DEFAULT_MAIN_CLASS_NAME)

            CtField.make("private static double a = 0.0;", cc).also { cc.addField(it) }

            val method = CtNewMethod.make("public static void main(String[] args){}", cc)
            method.insertBefore(
            """
            int x = 2;
            String s = "";
            switch (x) {
                case 1:
                    s = "1";
                    break;
                case 2:
                    s = "2";
                    break;
                case 3:
                    s = "3";
                    break;
                default:
                    s = "0";
            }
            """.trimIndent()
            )
            cc.addMethod(method)


            File("$DEFAULT_MAIN_CLASS_NAME.class").writeBytes(cc.toBytecode())
        }
    }
}
