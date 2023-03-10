package demo.parser.app

import demo.parser.antlr.AntlrProgramParser
import demo.parser.domain.Parser
import demo.parser.domain.Program
import org.jline.builtins.Completers.FileNameCompleter
import org.jline.reader.Completer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.shell.boot.TerminalCustomizer
import org.springframework.shell.standard.ValueProvider

@SpringBootApplication(proxyBeanMethods = false)
open class ShellApplication {

    @Bean
    open fun parserFactory(): () -> Parser<Program> = ::AntlrProgramParser

//    @Bean
//    open fun terminalCustomizer(): TerminalCustomizer = TerminalCustomizer { terminal ->
//        terminal.color()
//    }
}

fun main(args: Array<String>) {
    runApplication<ShellApplication>(*args)
}
