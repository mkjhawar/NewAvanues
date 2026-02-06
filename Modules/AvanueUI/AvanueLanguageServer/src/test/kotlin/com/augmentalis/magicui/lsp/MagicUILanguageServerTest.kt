package com.augmentalis.avaui.lsp

import org.eclipse.lsp4j.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import io.mockk.*

/**
 * Unit tests for MagicUILanguageServer
 */
class MagicUILanguageServerTest {

    private lateinit var server: MagicUILanguageServer
    private lateinit var mockClient: org.eclipse.lsp4j.services.LanguageClient

    @BeforeEach
    fun setup() {
        server = MagicUILanguageServer()
        mockClient = mockk(relaxed = true)
        server.connect(mockClient)
    }

    @Test
    fun `initialize should return server capabilities`() {
        // Given
        val params = InitializeParams().apply {
            rootUri = "file:///workspace"
            capabilities = ClientCapabilities()
        }

        // When
        val result = server.initialize(params).get()

        // Then
        assertNotNull(result)
        assertNotNull(result.capabilities)

        // Verify text document sync
        assertNotNull(result.capabilities.textDocumentSync)

        // Verify completion support
        assertNotNull(result.capabilities.completionProvider)
        assertTrue(result.capabilities.completionProvider.resolveProvider)
        assertTrue(result.capabilities.completionProvider.triggerCharacters.contains("."))
        assertTrue(result.capabilities.completionProvider.triggerCharacters.contains(":"))

        // Verify hover support
        assertNotNull(result.capabilities.hoverProvider)

        // Verify definition support
        assertNotNull(result.capabilities.definitionProvider)

        // Verify formatting support
        assertNotNull(result.capabilities.documentFormattingProvider)

        // Verify code action support
        assertNotNull(result.capabilities.codeActionProvider)

        // Verify execute command support
        assertNotNull(result.capabilities.executeCommandProvider)
        assertTrue(result.capabilities.executeCommandProvider.commands.contains("magicui.generateTheme"))
        assertTrue(result.capabilities.executeCommandProvider.commands.contains("magicui.validateComponent"))
        assertTrue(result.capabilities.executeCommandProvider.commands.contains("magicui.formatDocument"))
        assertTrue(result.capabilities.executeCommandProvider.commands.contains("magicui.generateCode"))
    }

    @Test
    fun `initialize should set server info`() {
        // Given
        val params = InitializeParams().apply {
            rootUri = "file:///workspace"
            capabilities = ClientCapabilities()
        }

        // When
        val result = server.initialize(params).get()

        // Then
        assertNotNull(result.serverInfo)
        assertEquals("MagicUI Language Server", result.serverInfo.name)
        assertEquals("1.0.0", result.serverInfo.version)
    }

    @Test
    fun `initialized should complete without error`() {
        // Given
        val params = InitializedParams()

        // When/Then - Should not throw
        assertDoesNotThrow {
            server.initialized(params)
        }
    }

    @Test
    fun `shutdown should return successful future`() {
        // When
        val result = server.shutdown().get()

        // Then - Should complete without error
        assertNull(result)
    }

    @Test
    fun `getTextDocumentService should return non-null service`() {
        // When
        val service = server.textDocumentService

        // Then
        assertNotNull(service)
        assertTrue(service is MagicUITextDocumentService)
    }

    @Test
    fun `getWorkspaceService should return non-null service`() {
        // When
        val service = server.workspaceService

        // Then
        assertNotNull(service)
        assertTrue(service is MagicUIWorkspaceService)
    }

    @Test
    fun `getClientCapabilities should return capabilities after initialize`() {
        // Given
        val clientCaps = ClientCapabilities().apply {
            textDocument = TextDocumentClientCapabilities()
        }
        val params = InitializeParams().apply {
            rootUri = "file:///workspace"
            capabilities = clientCaps
        }

        // When
        server.initialize(params).get()

        // Then
        assertNotNull(server.getClientCapabilities())
        assertSame(clientCaps, server.getClientCapabilities())
    }

    @Test
    fun `getClientCapabilities should return null before initialize`() {
        // Given - Fresh server instance
        val freshServer = MagicUILanguageServer()

        // When
        val caps = freshServer.getClientCapabilities()

        // Then
        assertNull(caps)
    }

    @Test
    fun `server should support incremental text document sync`() {
        // Given
        val params = InitializeParams().apply {
            rootUri = "file:///workspace"
            capabilities = ClientCapabilities()
        }

        // When
        val result = server.initialize(params).get()

        // Then
        val syncKind = result.capabilities.textDocumentSync.left
        assertEquals(TextDocumentSyncKind.Incremental, syncKind)
    }

    @Test
    fun `completion provider should have correct trigger characters`() {
        // Given
        val params = InitializeParams().apply {
            rootUri = "file:///workspace"
            capabilities = ClientCapabilities()
        }

        // When
        val result = server.initialize(params).get()

        // Then
        val triggers = result.capabilities.completionProvider.triggerCharacters
        assertEquals(4, triggers.size)
        assertTrue(triggers.containsAll(listOf(".", ":", "-", " ")))
    }

    @Test
    fun `execute command provider should support all custom commands`() {
        // Given
        val params = InitializeParams().apply {
            rootUri = "file:///workspace"
            capabilities = ClientCapabilities()
        }

        // When
        val result = server.initialize(params).get()

        // Then
        val commands = result.capabilities.executeCommandProvider.commands
        assertEquals(4, commands.size)
        assertTrue(commands.contains("magicui.generateTheme"))
        assertTrue(commands.contains("magicui.validateComponent"))
        assertTrue(commands.contains("magicui.formatDocument"))
        assertTrue(commands.contains("magicui.generateCode"))
    }
}
