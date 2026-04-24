package net.sourceforge.moonstone.desktop

import net.sourceforge.moonstone.desktop.config.CliArgs
import net.sourceforge.moonstone.desktop.config.parseArgs
import net.sourceforge.moonstone.desktop.config.printUsage
import net.sourceforge.moonstone.desktop.runner.DebugModeRunner
import net.sourceforge.moonstone.desktop.runner.NormalModeRunner
import kotlin.system.exitProcess

/**
 * Main entry point for the Moonstone desktop application.
 */
fun main(args: Array<String>) {
    val cliArgs: CliArgs = parseArgs(args)

    if (cliArgs.scriptPath == null) {
        System.err.println("Error: No script file specified")
        printUsage()
        exitProcess(1)
    }

    // Use debug/hot-reload mode if either flag is set
    val useDebugRuntime = cliArgs.debugMode || cliArgs.hotReload

    if (useDebugRuntime) {
        DebugModeRunner.run(cliArgs)
    } else {
        NormalModeRunner.run(cliArgs.scriptPath)
    }
}
