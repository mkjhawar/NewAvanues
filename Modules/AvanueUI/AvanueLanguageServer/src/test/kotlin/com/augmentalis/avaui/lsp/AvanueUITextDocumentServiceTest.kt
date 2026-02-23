package com.augmentalis.avaui.lsp

import org.eclipse.lsp4j.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import io.mockk.*

/**
 * Unit tests for AvanueUITextDocumentService
 */
class AvanueUITextDocumentServiceTest {

    private lateinit var service: AvanueUITextDocumentService
    private lateinit var mockClient: org.eclipse.lsp4j.services.LanguageClient

    @BeforeEach
    fun setup() {
        service = AvanueUITextDocumentService()
        mockClient = mockk(relaxed = true)
        service.connect(mockClient)
    }

    @Test
    fun `didOpen should cache document and publish diagnostics`() {
        val uri = "file:///test.avanueui.yaml"
        val content = """
            Button:
              avid: test-button
              text: Click Me
        """.trimIndent()

        val params = DidOpenTextDocumentParams().apply {
            textDocument = TextDocumentItem().apply {
                this.uri = uri
                this.text = content
            }
        }

        service.didOpen(params)

        verify { mockClient.publishDiagnostics(any()) }
    }

    @Test
    fun `didChange should update document and publish diagnostics`() {
        val uri = "file:///test.avanueui.yaml"
        val initialContent = "Button:"

        val openParams = DidOpenTextDocumentParams().apply {
            textDocument = TextDocumentItem().apply {
                this.uri = uri
                this.text = initialContent
            }
        }
        service.didOpen(openParams)

        val changeParams = DidChangeTextDocumentParams().apply {
            textDocument = VersionedTextDocumentIdentifier().apply {
                this.uri = uri
                this.version = 2
            }
            contentChanges = listOf(
                TextDocumentContentChangeEvent().apply {
                    text = """
                        Button:
                          avid: updated-button
                    """.trimIndent()
                }
            )
        }

        service.didChange(changeParams)

        verify(atLeast = 2) { mockClient.publishDiagnostics(any()) }
    }

    @Test
    fun `completion should return component suggestions`() {
        val uri = "file:///test.avanueui.yaml"
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

        val result = service.completion(completionParams).get()

        assertNotNull(result)
        val completions = result.right
        assertTrue(completions.items.isNotEmpty())
        assertTrue(completions.items.any { it.label == "Button" })
    }

    @Test
    fun `hover should return component documentation`() {
        val uri = "file:///test.avanueui.yaml"
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
            position = Position(0, 3)
        }

        val result = service.hover(hoverParams).get()

        assertNotNull(result)
        assertNotNull(result.contents)
    }

    @Test
    fun `validation should detect missing required properties`() {
        val uri = "file:///test.avanueui.yaml"
        val content = """
            Button:
              onClick: handleClick
        """.trimIndent()

        val params = DidOpenTextDocumentParams().apply {
            textDocument = TextDocumentItem().apply {
                this.uri = uri
                this.text = content
            }
        }

        service.didOpen(params)

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
        val uri = "file:///test.avanueui.yaml"
        val content = """
            Text:
              avid: test-text
              text: Hello
              color: invalid-color-value
        """.trimIndent()

        val params = DidOpenTextDocumentParams().apply {
            textDocument = TextDocumentItem().apply {
                this.uri = uri
                this.text = content
            }
        }

        service.didOpen(params)

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
    fun `definition should find AVID declarations`() {
        val uri = "file:///test.avanueui.yaml"
        val content = """
            Button:
              avid: submit-button
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
            position = Position(3, 20)
        }

        val result = service.definition(definitionParams).get()

        assertNotNull(result)
        val locations = result.left
        assertTrue(locations.isNotEmpty())
        assertEquals(uri, locations[0].uri)
    }

    @Test
    fun `formatting should return empty list for now`() {
        val uri = "file:///test.avanueui.yaml"
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
                isInsertSpaces = true
            }
        }

        val result = service.formatting(formattingParams).get()

        assertNotNull(result)
        assertEquals(0, result.size)
    }
}
