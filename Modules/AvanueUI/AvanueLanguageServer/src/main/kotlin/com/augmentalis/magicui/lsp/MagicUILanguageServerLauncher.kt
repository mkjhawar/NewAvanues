package com.augmentalis.avaui.lsp

import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.services.LanguageClient
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import kotlin.system.exitProcess

/**
 * Main entry point for MagicUI Language Server
 *
 * Launches the LSP server and connects to the client via stdio
 */
object MagicUILanguageServerLauncher {
    private val logger = LoggerFactory.getLogger(MagicUILanguageServerLauncher::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("Starting MagicUI Language Server...")

        try {
            // Parse command line arguments
            val useStdio = args.isEmpty() || args.contains("--stdio")
            val port = args.find { it.startsWith("--port=") }?.substringAfter("=")?.toIntOrNull()

            when {
                useStdio -> launchStdioServer(System.`in`, System.out)
                port != null -> launchSocketServer(port)
                else -> {
                    logger.error("Invalid arguments. Use --stdio or --port=<port>")
                    exitProcess(1)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to start Language Server", e)
            exitProcess(1)
        }
    }

    /**
     * Launch server using stdio communication (default, recommended for IDE integrations)
     */
    private fun launchStdioServer(input: InputStream, output: OutputStream) {
        logger.info("Launching Language Server with stdio...")

        val server = MagicUILanguageServer()
        val launcher = Launcher.Builder<LanguageClient>()
            .setLocalService(server)
            .setRemoteInterface(LanguageClient::class.java)
            .setInput(input)
            .setOutput(output)
            .setExecutorService(Executors.newCachedThreadPool())
            .create()

        // Connect client to server
        val client = launcher.remoteProxy
        server.connect(client)

        // Start listening
        logger.info("Language Server listening on stdio")
        launcher.startListening().get()
    }

    /**
     * Launch server using socket communication (for testing/debugging)
     */
    private fun launchSocketServer(port: Int) {
        logger.info("Socket server not implemented yet. Use --stdio instead.")
        exitProcess(1)
    }
}
