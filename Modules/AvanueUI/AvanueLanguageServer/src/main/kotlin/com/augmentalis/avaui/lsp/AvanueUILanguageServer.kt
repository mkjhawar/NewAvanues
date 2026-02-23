package com.augmentalis.avaui.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.*
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

/**
 * AvanueUI Language Server implementation
 *
 * Implements the Language Server Protocol for AvanueUI DSL files.
 * Supports: .avanueui.yaml, .avanueui.json, .avanueui (and legacy .magic.yaml, .magic.json, .magicui)
 */
class AvanueUILanguageServer : LanguageServer, LanguageClientAware {

    private val logger = LoggerFactory.getLogger(AvanueUILanguageServer::class.java)

    private val textDocumentService = AvanueUITextDocumentService()
    private val workspaceService = AvanueUIWorkspaceService()

    private var client: LanguageClient? = null
    private var clientCapabilities: ClientCapabilities? = null

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        val workspaceUri = params.workspaceFolders?.firstOrNull()?.uri
            ?: @Suppress("DEPRECATION") params.rootUri
        logger.info("Initializing Language Server for workspace: $workspaceUri")
        clientCapabilities = params.capabilities

        val serverCapabilities = ServerCapabilities().apply {
            textDocumentSync = Either.forLeft(TextDocumentSyncKind.Incremental)
            completionProvider = CompletionOptions().apply {
                resolveProvider = true
                triggerCharacters = listOf(".", ":", "-", " ")
            }
            hoverProvider = Either.forLeft(true)
            definitionProvider = Either.forLeft(true)
            diagnosticProvider = DiagnosticRegistrationOptions()
            documentFormattingProvider = Either.forLeft(true)
            codeActionProvider = Either.forLeft(true)
            executeCommandProvider = ExecuteCommandOptions().apply {
                commands = listOf(
                    "avanueui.generateTheme",
                    "avanueui.validateComponent",
                    "avanueui.formatDocument",
                    "avanueui.generateCode"
                )
            }
        }

        val serverInfo = ServerInfo().apply {
            name = "AvanueUI Language Server"
            version = "2.0.0"
        }

        return CompletableFuture.completedFuture(InitializeResult(serverCapabilities, serverInfo))
    }

    override fun initialized(params: InitializedParams) {
        logger.info("Language Server initialized successfully")
        workspaceService.workspaceFolders?.forEach { folder ->
            logger.info("Workspace folder: ${folder.uri}")
        }
    }

    override fun shutdown(): CompletableFuture<Any> {
        logger.info("Shutting down Language Server...")
        return CompletableFuture.completedFuture(null)
    }

    override fun exit() {
        logger.info("Language Server exiting")
        System.exit(0)
    }

    override fun getTextDocumentService(): TextDocumentService = textDocumentService

    override fun getWorkspaceService(): WorkspaceService = workspaceService

    override fun connect(client: LanguageClient) {
        this.client = client
        textDocumentService.connect(client)
        workspaceService.connect(client)
        logger.info("Connected to language client")
    }

    fun getClientCapabilities(): ClientCapabilities? = clientCapabilities
}
