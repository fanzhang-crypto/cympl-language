package demo.parser.app

import demo.parser.antlr.AntlrProgramParser
import demo.parser.domain.Parser
import demo.parser.domain.Program
import org.jline.reader.LineReader
import org.jline.terminal.Terminal
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.convert.ApplicationConversionService
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.convert.converter.Converter
import java.io.File

@SpringBootApplication(proxyBeanMethods = false)
open class CliApp {

    @Bean
    open fun parserProvider(): () -> Parser<Program> = ::AntlrProgramParser

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
    open fun runtime(lineReader: LineReader): demo.parser.interpret.Runtime = object : demo.parser.interpret.Runtime {
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
