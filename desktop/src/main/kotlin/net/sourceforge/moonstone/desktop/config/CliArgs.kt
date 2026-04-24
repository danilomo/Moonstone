package net.sourceforge.moonstone.desktop.config

import kotlin.system.exitProcess

/**
 * Command-line arguments configuration.
 */
data class CliArgs(
    val scriptPath: String?,
    val debugMode: Boolean = false,
    val hotReload: Boolean = false,
)

/**
 * Parse command-line arguments.
 */
fun parseArgs(args: Array<String>): CliArgs {
    var scriptPath: String? = null
    var debugMode = false
    var hotReload = false

    val iterator = args.iterator()
    while (iterator.hasNext()) {
        when (val arg = iterator.next()) {
            "--debug", "-d" -> debugMode = true
            "--hot-reload", "--watch", "-w" -> hotReload = true
            "--help", "-h" -> {
                printUsage()
                exitProcess(0)
            }
            else -> {
                if (!arg.startsWith("-")) {
                    scriptPath = arg
                } else {
                    System.err.println("Unknown option: $arg")
                    printUsage()
                    exitProcess(1)
                }
            }
        }
    }

    return CliArgs(scriptPath, debugMode, hotReload)
}

/**
 * Print usage information for the CLI.
 */
fun printUsage() {
    println(
        """
        Moonstone - Scheme-based UI Framework

        Usage: kleinlisp-gui [options] <script.scm>

        Options:
          --debug, -d        Enable debug mode with inspector panel
          --hot-reload, -w   Enable hot reload (watches script for changes)
          --watch            Alias for --hot-reload
          --help, -h         Show this help message

        Examples:
          kleinlisp-gui samples/counter/app.scm
          kleinlisp-gui --debug samples/counter/app.scm
          kleinlisp-gui --hot-reload samples/counter/app.scm
          kleinlisp-gui -d -w samples/counter/app.scm
        """.trimIndent(),
    )
}
