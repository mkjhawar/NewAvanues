package com.augmentalis.avaui.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.TextDocumentService
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import com.augmentalis.avaui.lsp.stubs.*

/**
 * Text Document Service for AvanueUI Language Server
 *
 * Thin orchestrator delegating to focused providers:
 * - DocumentManager: document lifecycle
 * - CompletionProvider: autocompletion
 * - HoverProvider: hover documentation
 * - DefinitionProvider: go-to-definition
 * - DiagnosticsProvider: validation and diagnostics
 */
class AvanueUITextDocumentService : TextDocumentService {

    private val logger = LoggerFactory.getLogger(AvanueUITextDocumentService::class.java)

    private var client: LanguageClient? = null

    private val documentManager = DocumentManager()
    private val completionProvider = CompletionProvider()
    private val hoverProvider = HoverProvider()
    private val definitionProvider = DefinitionProvider()
    private val diagnosticsProvider = DiagnosticsProvider(VosParser(), JsonDSLParser(), CompactSyntaxParser())

    fun connect(client: LanguageClient) {
        this.client = client
    }

    override fun didOpen(params: DidOpenTextDocumentParams) {
        val uri = params.textDocument.uri
        val content = params.textDocument.text
        documentManager.open(uri, content)
        diagnosticsProvider.publishDiagnostics(uri, content, client)
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        val uri = params.textDocument.uri
        val newContent = documentManager.change(uri, params.contentChanges)
        if (newContent != null) {
            diagnosticsProvider.publishDiagnostics(uri, newContent, client)
        }
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        documentManager.close(params.textDocument.uri)
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
        logger.info("Document saved: ${params.textDocument.uri}")
    }

    override fun completion(params: CompletionParams): CompletableFuture<Either<List<CompletionItem>, CompletionList>> {
        val content = documentManager.getContent(params.textDocument.uri) ?: ""
        return CompletableFuture.supplyAsync {
            val completions = completionProvider.getCompletionItems(content, params.position)
            Either.forRight(CompletionList(false, completions))
        }
    }

    override fun hover(params: HoverParams): CompletableFuture<Hover> {
        val content = documentManager.getContent(params.textDocument.uri) ?: ""
        return CompletableFuture.supplyAsync {
            hoverProvider.getHoverInfo(content, params.position)
        }
    }

    override fun definition(params: DefinitionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> {
        val content = documentManager.getContent(params.textDocument.uri) ?: ""
        return CompletableFuture.supplyAsync {
            Either.forLeft(definitionProvider.getDefinitionLocations(content, params.position, params.textDocument.uri))
        }
    }

    override fun formatting(params: DocumentFormattingParams): CompletableFuture<List<TextEdit>> {
        return CompletableFuture.supplyAsync { emptyList() }
    }
}
