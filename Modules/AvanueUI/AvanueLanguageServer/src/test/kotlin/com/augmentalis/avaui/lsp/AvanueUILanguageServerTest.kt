package com.augmentalis.avaui.lsp

import org.eclipse.lsp4j.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import io.mockk.*

@Suppress("DEPRECATION") // InitializeParams.rootUri deprecated in LSP spec, still needed for client compat
class AvanueUILanguageServerTest {

    private lateinit var server: AvanueUILanguageServer
    private lateinit var mockClient: org.eclipse.lsp4j.services.LanguageClient

    @BeforeEach
    fun setup() {
        server = AvanueUILanguageServer()
        mockClient = mockk(relaxed = true)
        server.connect(mockClient)
    }

    @Test
    fun `initialize should return server capabilities`() {
        val params = InitializeParams().apply {
            rootUri = "file:///workspace"
            capabilities = ClientCapabilities()
        }

        val result = server.initialize(params).get()

        assertNotNull(result)
        assertNotNull(result.capabilities)
        assertNotNull(result.capabilities.textDocumentSync)
        assertNotNull(result.capabilities.completionProvider)
        assertTrue(result.capabilities.completionProvider.resolveProvider)
        assertTrue(result.capabilities.completionProvider.triggerCharacters.contains("."))
        assertTrue(result.capabilities.completionProvider.triggerCharacters.contains(":"))
        assertNotNull(result.capabilities.hoverProvider)
        assertNotNull(result.capabilities.definitionProvider)
        assertNotNull(result.capabilities.documentFormattingProvider)
        assertNotNull(result.capabilities.codeActionProvider)
        assertNotNull(result.capabilities.executeCommandProvider)
        assertTrue(result.capabilities.executeCommandProvider.commands.contains("avanueui.generateTheme"))
        assertTrue(result.capabilities.executeCommandProvider.commands.contains("avanueui.validateComponent"))
        assertTrue(result.capabilities.executeCommandProvider.commands.contains("avanueui.formatDocument"))
        assertTrue(result.capabilities.executeCommandProvider.commands.contains("avanueui.generateCode"))
    }

    @Test
    fun `initialize should set server info`() {
        val params = InitializeParams().apply {
            rootUri = "file:///workspace"
            capabilities = ClientCapabilities()
        }

        val result = server.initialize(params).get()

        assertNotNull(result.serverInfo)
        assertEquals("AvanueUI Language Server", result.serverInfo.name)
        assertEquals("2.0.0", result.serverInfo.version)
    }

    @Test
    fun `initialized should complete without error`() {
        assertDoesNotThrow { server.initialized(InitializedParams()) }
    }

    @Test
    fun `shutdown should return successful future`() {
        val result = server.shutdown().get()
        assertNull(result)
    }

    @Test
    fun `getTextDocumentService should return non-null service`() {
        val service = server.textDocumentService
        assertNotNull(service)
        assertTrue(service is AvanueUITextDocumentService)
    }

    @Test
    fun `getWorkspaceService should return non-null service`() {
        val service = server.workspaceService
        assertNotNull(service)
        assertTrue(service is AvanueUIWorkspaceService)
    }

    @Test
    fun `getClientCapabilities should return capabilities after initialize`() {
        val clientCaps = ClientCapabilities().apply {
            textDocument = TextDocumentClientCapabilities()
        }
        val params = InitializeParams().apply {
            rootUri = "file:///workspace"
            capabilities = clientCaps
        }

        server.initialize(params).get()

        assertNotNull(server.getClientCapabilities())
        assertSame(clientCaps, server.getClientCapabilities())
    }

    @Test
    fun `getClientCapabilities should return null before initialize`() {
        val freshServer = AvanueUILanguageServer()
        assertNull(freshServer.getClientCapabilities())
    }

    @Test
    fun `server should support incremental text document sync`() {
        val params = InitializeParams().apply {
            rootUri = "file:///workspace"
            capabilities = ClientCapabilities()
        }

        val result = server.initialize(params).get()

        assertEquals(TextDocumentSyncKind.Incremental, result.capabilities.textDocumentSync.left)
    }

    @Test
    fun `completion provider should have correct trigger characters`() {
        val params = InitializeParams().apply {
            rootUri = "file:///workspace"
            capabilities = ClientCapabilities()
        }

        val result = server.initialize(params).get()
        val triggers = result.capabilities.completionProvider.triggerCharacters

        assertEquals(4, triggers.size)
        assertTrue(triggers.containsAll(listOf(".", ":", "-", " ")))
    }

    @Test
    fun `execute command provider should support all custom commands`() {
        val params = InitializeParams().apply {
            rootUri = "file:///workspace"
            capabilities = ClientCapabilities()
        }

        val result = server.initialize(params).get()
        val commands = result.capabilities.executeCommandProvider.commands

        assertEquals(4, commands.size)
        assertTrue(commands.contains("avanueui.generateTheme"))
        assertTrue(commands.contains("avanueui.validateComponent"))
        assertTrue(commands.contains("avanueui.formatDocument"))
        assertTrue(commands.contains("avanueui.generateCode"))
    }
}
