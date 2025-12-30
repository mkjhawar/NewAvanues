package com.augmentalis.magicui.lsp

import org.eclipse.lsp4j.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import io.mockk.*

/**
 * Unit tests for MagicUITextDocumentService
 */
class MagicUITextDocumentServiceTest {

    private lateinit var service: MagicUITextDocumentService
    private lateinit var mockClient: org.eclipse.lsp4j.services.LanguageClient

    @BeforeEach
    fun setup() {
        service = MagicUITextDocumentService()
        mockClient = mockk(relaxed = true)
        service.connect(mockClient)
    }

    @Test
    fun `didOpen should cache document and publish diagnostics`() {
        // Given
        val uri = "file:///test.magic.yaml"
        val content = """
            Button:
              vuid: test-button
              text: Click Me
        """.trimIndent()

        val params = DidOpenTextDocumentParams().apply {
            textDocument = TextDocumentItem().apply {
                this.uri = uri
                this.text = content
            }
        }

        // When
        service.didOpen(params)

        // Then
        verify { mockClient.publishDiagnostics(any()) }
    }

    @Test
    fun `didChange should update document and publish diagnostics`() {
        // Given
        val uri = "file:///test.magic.yaml"
        val initialContent = "Button:"

        // Open document first
        val openParams = DidOpenTextDocumentParams().apply {
            textDocument = TextDocumentItem().apply {
                this.uri = uri
                this.text = initialContent
            }
        }
        service.didOpen(openParams)

        // Prepare change
        val changeParams = DidChangeTextDocumentParams().apply {
            textDocument = VersionedTextDocumentIdentifier().apply {
                this.uri = uri
                this.version = 2
            }
            contentChanges = listOf(
                TextDocumentContentChangeEvent().apply {
                    text = """
                        Button:
                          vuid: updated-button
                    """.trimIndent()
                }
            )
        }

        // When
        service.didChange(changeParams)

        // Then - should publish diagnostics twice (open + change)
        verify(atLeast = 2) { mockClient.publishDiagnostics(any()) }
    }

    @Test
    fun `completion should return component suggestions`() {
        // Given
        val uri = "file:///test.magic.yaml"
        val content = "B"

        val openParams = DidOpenTextDocumentParams().apply {
            textDocument = TextDocumentItem().apply {
                this.uri = uri
                this.text = content
            }
        }
        service.didOpen(openParams)

        val completionParams = CompletionParams().apply {
            textDocument = TextDocumentIdentifier(uri)
            position = Position(0, 1)
        }

        // When
        val result = service.completion(completionParams).get()

        // Then
        assertNotNull(result)
        val completions = result.right
        assertTrue(completions.items.size > 0)
        assertTrue(completions.items.any { it.label == "Button" })
    }

    @Test
    fun `hover should return component documentation`() {
        // Given
        val uri = "file:///test.magic.yaml"
        val content = "Button:"

        val openParams = DidOpenTextDocumentParams().apply {
            textDocument = TextDocumentItem().apply {
                this.uri = uri
                this.text = content
            }
        }
        service.didOpen(openParams)

        val hoverParams = HoverParams().apply {
            textDocument = TextDocumentIdentifier(uri)
            position = Position(0, 3) // On "Button"
        }

        // When
        val result = service.hover(hoverParams).get()

        // Then
        assertNotNull(result)
        assertNotNull(result.contents)
    }

    @Test
    fun `validation should detect missing required properties`() {
        // Given
        val uri = "file:///test.magic.yaml"
        val content = """
            Button:
              onClick: handleClick
        """.trimIndent() // Missing text/icon

        val params = DidOpenTextDocumentParams().apply {
            textDocument = TextDocumentItem().apply {
                this.uri = uri
                this.text = content
            }
        }

        // When
        service.didOpen(params)

        // Then - Should publish diagnostics with warnings
        verify {
            mockClient.publishDiagnostics(
                match { diagnosticsParams ->
                    diagnosticsParams.diagnostics.any {
                        it.severity == DiagnosticSeverity.Warning
                    }
                }
            )
        }
    }

    @Test
    fun `validation should detect invalid color values`() {
        // Given
        val uri = "file:///test.magic.yaml"
        val content = """
            Text:
              vuid: test-text
              text: Hello
              color: invalid-color-value
        """.trimIndent()

        val params = DidOpenTextDocumentParams().apply {
            textDocument = TextDocumentItem().apply {
                this.uri = uri
                this.text = content
            }
        }

        // When
        service.didOpen(params)

        // Then - Should detect invalid color
        verify {
            mockClient.publishDiagnostics(
                match { diagnosticsParams ->
                    diagnosticsParams.diagnostics.any {
                        it.message.contains("Invalid color")
                    }
                }
            )
        }
    }

    @Test
    fun `definition should find VUID declarations`() {
        // Given
        val uri = "file:///test.magic.yaml"
        val content = """
            Button:
              vuid: submit-button
              text: Submit
              onClick: submit-button
        """.trimIndent()

        val openParams = DidOpenTextDocumentParams().apply {
            textDocument = TextDocumentItem().apply {
                this.uri = uri
                this.text = content
            }
        }
        service.didOpen(openParams)

        val definitionParams = DefinitionParams().apply {
            textDocument = TextDocumentIdentifier(uri)
            position = Position(3, 20) // On "submit-button" in onClick
        }

        // When
        val result = service.definition(definitionParams).get()

        // Then
        assertNotNull(result)
        val locations = result.left
        assertTrue(locations.size > 0)
        assertEquals(uri, locations[0].uri)
    }

    @Test
    fun `formatting should return empty list for now`() {
        // Given
        val uri = "file:///test.magic.yaml"
        val content = "Button:\n  text: Click"

        val openParams = DidOpenTextDocumentParams().apply {
            textDocument = TextDocumentItem().apply {
                this.uri = uri
                this.text = content
            }
        }
        service.didOpen(openParams)

        val formattingParams = DocumentFormattingParams().apply {
            textDocument = TextDocumentIdentifier(uri)
            options = FormattingOptions().apply {
                tabSize = 2
                insertSpaces = true
            }
        }

        // When
        val result = service.formatting(formattingParams).get()

        // Then
        assertNotNull(result)
        // Current implementation returns empty list
        assertEquals(0, result.size)
    }
}
