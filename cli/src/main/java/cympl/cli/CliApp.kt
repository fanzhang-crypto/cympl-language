package cympl.cli

import cympl.parser.antlr.AntlrProgramParser
import cympl.parser.Parser
import cympl.language.Program
import org.jline.reader.LineReader
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle
import org.jline.utils.AttributedStyle.YELLOW
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.convert.ApplicationConversionService
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.convert.converter.Converter
import org.springframework.shell.jline.PromptProvider
import java.io.File

@SpringBootApplication(proxyBeanMethods = false)
open class CliApp {

    @Bean
    open fun parserProvider(): () -> Parser<Program> = ::AntlrProgramParser

    @Bean("cymplPromptProvider")
    open fun promptProvider() = PromptProvider {
        "cympl:>".fg(YELLOW)
    }

//    @Bean
//    open fun terminalCustomizer(): TerminalCustomizer = TerminalCustomizer { terminal ->
//        terminal.color()
//    }

    @Bean
    open fun stingToFileConverter() =
        object : Converter<String, File> { // use object instead of lambda to avoid type erasure
            override fun convert(source: String): File? {
                return ApplicationConversionService.getSharedInstance().convert(source, File::class.java)
            }
        }

    @Bean
    open fun runtime(lineReader: LineReader): cympl.interpreter.Runtime = object : cympl.interpreter.Runtime {
        override fun printLine(value: Any) {
            val message = AttributedString(value.toString(), AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)).toAnsi()
            lineReader.terminal.writer().println(message)
        }

        override fun readLine(prompt: String): String {
            return lineReader.readLine(prompt)
        }
    }
}

fun main(args: Array<String>) {
    runApplication<CliApp>(*args)
}
