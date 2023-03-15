package demo.parser.app

import demo.parser.antlr.AntlrProgramParser
import demo.parser.domain.Parser
import demo.parser.domain.Program
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
    open fun stingToFileConverter() = object : Converter<String, File> { // use object instead of lambda to avoid type erasure
        override fun convert(source: String): File? {
            return ApplicationConversionService.getSharedInstance().convert(source, File::class.java)
        }
    }
}

fun main(args: Array<String>) {
    runApplication<CliApp>(*args)
}
