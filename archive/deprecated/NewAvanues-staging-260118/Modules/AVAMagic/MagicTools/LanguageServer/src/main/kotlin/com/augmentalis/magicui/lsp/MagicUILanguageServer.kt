package com.augmentalis.magicui.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.*
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

/**
 * MagicUI Language Server implementation
 *
 * Implements the Language Server Protocol for MagicUI DSL files
 * Supports: .magic.yaml, .magic.json, .magicui files
 */
class MagicUILanguageServer : LanguageServer, LanguageClientAware {

    private val logger = LoggerFactory.getLogger(MagicUILanguageServer::class.java)

    private val textDocumentService = MagicUITextDocumentService()
    private val workspaceService = MagicUIWorkspaceService()

    private var client: LanguageClient? = null
    private var clientCapabilities: ClientCapabilities? = null

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        logger.info("Initializing Language Server for workspace: ${params.rootUri}")

        clientCapabilities = params.capabilities

        // Define server capabilities
        val serverCapabilities = ServerCapabilities().apply {
            // Text document sync
            textDocumentSync = Either.forLeft(TextDocumentSyncKind.Incremental)

            // Completion support
            completionProvider = CompletionOptions().apply {
                resolveProvider = true
                triggerCharacters = listOf(".", ":", "-", " ")
            }

            // Hover support
            hoverProvider = Either.forLeft(true)

            // Definition support (go-to-definition)
            definitionProvider = Either.forLeft(true)

            // Diagnostic support (error checking)
            diagnosticProvider = DiagnosticRegistrationOptions()

            // Document formatting
            documentFormattingProvider = Either.forLeft(true)

            // Code actions
            codeActionProvider = Either.forLeft(true)

            // Execute command support (custom commands)
            executeCommandProvider = ExecuteCommandOptions().apply {
                commands = listOf(
                    "magicui.generateTheme",
                    "magicui.validateComponent",
                    "magicui.formatDocument",
                    "magicui.generateCode"
                )
            }
        }

        val serverInfo = ServerInfo().apply {
            name = "MagicUI Language Server"
            version = "1.0.0"
        }

        return CompletableFuture.completedFuture(
            InitializeResult(serverCapabilities, serverInfo)
        )
    }

    override fun initialized(params: InitializedParams) {
        logger.info("Language Server initialized successfully")

        // Log workspace folders
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

    override fun getTextDocumentService(): TextDocumentService {
        return textDocumentService
    }

    override fun getWorkspaceService(): WorkspaceService {
        return workspaceService
    }

    override fun connect(client: LanguageClient) {
        this.client = client
        textDocumentService.connect(client)
        workspaceService.connect(client)
        logger.info("Connected to language client")
    }

    /**
     * Get client capabilities
     */
    fun getClientCapabilities(): ClientCapabilities? = clientCapabilities
}
