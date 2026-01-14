package com.augmentalis.magicui.lsp

import org.eclipse.lsp4j.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import io.mockk.*

/**
 * Unit tests for MagicUIWorkspaceService
 */
class MagicUIWorkspaceServiceTest {

    private lateinit var service: MagicUIWorkspaceService
    private lateinit var mockClient: org.eclipse.lsp4j.services.LanguageClient

    @BeforeEach
    fun setup() {
        service = MagicUIWorkspaceService()
        mockClient = mockk(relaxed = true)
        service.connect(mockClient)
    }

    @Test
    fun `executeCommand with magicui generateTheme should return theme output`() {
        // Given
        val themeJson = """
            {
                "name": "TestTheme",
                "platform": "ANDROID"
            }
        """.trimIndent()

        val params = ExecuteCommandParams().apply {
            command = "magicui.generateTheme"
            arguments = listOf("dsl", themeJson)
        }

        // When
        val result = service.executeCommand(params).get() as Map<*, *>

        // Then
        assertEquals(true, result["success"])
        assertNotNull(result["output"])
        assertEquals("TestTheme", result["themeName"])
    }

    @Test
    fun `executeCommand with magicui generateTheme and yaml format should return yaml`() {
        // Given
        val themeJson = """
            {
                "name": "YamlTheme"
            }
        """.trimIndent()

        val params = ExecuteCommandParams().apply {
            command = "magicui.generateTheme"
            arguments = listOf("yaml", themeJson)
        }

        // When
        val result = service.executeCommand(params).get() as Map<*, *>

        // Then
        assertEquals(true, result["success"])
        assertEquals("yaml", result["format"])
        assertTrue((result["output"] as String).startsWith("#"))
    }

    @Test
    fun `executeCommand with magicui generateTheme and json format should return json`() {
        // Given
        val themeJson = """
            {
                "name": "JsonTheme"
            }
        """.trimIndent()

        val params = ExecuteCommandParams().apply {
            command = "magicui.generateTheme"
            arguments = listOf("json", themeJson)
        }

        // When
        val result = service.executeCommand(params).get() as Map<*, *>

        // Then
        assertEquals(true, result["success"])
        assertEquals("json", result["format"])
        assertTrue((result["output"] as String).startsWith("{"))
    }

    @Test
    fun `executeCommand with magicui generateTheme and css format should return css`() {
        // Given
        val themeJson = """
            {
                "name": "CssTheme"
            }
        """.trimIndent()

        val params = ExecuteCommandParams().apply {
            command = "magicui.generateTheme"
            arguments = listOf("css", themeJson)
        }

        // When
        val result = service.executeCommand(params).get() as Map<*, *>

        // Then
        assertEquals(true, result["success"])
        assertEquals("css", result["format"])
        assertTrue((result["output"] as String).startsWith("/*"))
    }

    @Test
    fun `executeCommand with magicui generateTheme missing arguments should return error`() {
        // Given
        val params = ExecuteCommandParams().apply {
            command = "magicui.generateTheme"
            arguments = listOf("dsl") // Missing themeJson
        }

        // When
        val result = service.executeCommand(params).get() as Map<*, *>

        // Then
        assertEquals(false, result["success"])
        assertNotNull(result["error"])
    }

    @Test
    fun `executeCommand with magicui validateComponent should return validation result`() {
        // Given
        val params = ExecuteCommandParams().apply {
            command = "magicui.validateComponent"
            arguments = emptyList()
        }

        // When
        val result = service.executeCommand(params).get() as Map<*, *>

        // Then
        assertEquals(true, result["success"])
        assertEquals(true, result["valid"])
    }

    @Test
    fun `executeCommand with magicui formatDocument should return success`() {
        // Given
        val params = ExecuteCommandParams().apply {
            command = "magicui.formatDocument"
            arguments = listOf("file:///test.magic.yaml")
        }

        // When
        val result = service.executeCommand(params).get() as Map<*, *>

        // Then
        assertEquals(true, result["success"])
        assertEquals(true, result["formatted"])
    }

    @Test
    fun `executeCommand with magicui generateCode should return placeholder`() {
        // Given
        val params = ExecuteCommandParams().apply {
            command = "magicui.generateCode"
            arguments = listOf("kotlin", "Button: text: Test")
        }

        // When
        val result = service.executeCommand(params).get() as Map<*, *>

        // Then
        assertEquals(true, result["success"])
        assertNotNull(result["output"])
    }

    @Test
    fun `executeCommand with unknown command should return error`() {
        // Given
        val params = ExecuteCommandParams().apply {
            command = "magicui.unknownCommand"
            arguments = emptyList()
        }

        // When
        val result = service.executeCommand(params).get() as Map<*, *>

        // Then
        assertNotNull(result["error"])
        assertTrue((result["error"] as String).contains("Unknown command"))
    }

    @Test
    fun `didChangeConfiguration should log configuration change`() {
        // Given
        val params = DidChangeConfigurationParams().apply {
            settings = mapOf("magicui" to mapOf("enabled" to true))
        }

        // When/Then - Should not throw
        assertDoesNotThrow {
            service.didChangeConfiguration(params)
        }
    }

    @Test
    fun `didChangeWatchedFiles should log file changes`() {
        // Given
        val params = DidChangeWatchedFilesParams().apply {
            changes = listOf(
                FileEvent().apply {
                    uri = "file:///test.magic.yaml"
                    type = FileChangeType.Changed
                }
            )
        }

        // When/Then - Should not throw
        assertDoesNotThrow {
            service.didChangeWatchedFiles(params)
        }
    }

    @Test
    fun `didChangeWorkspaceFolders should log folder changes`() {
        // Given
        val params = DidChangeWorkspaceFoldersParams().apply {
            event = WorkspaceFoldersChangeEvent().apply {
                added = listOf(
                    WorkspaceFolder().apply {
                        uri = "file:///workspace"
                        name = "Test Workspace"
                    }
                )
                removed = emptyList()
            }
        }

        // When/Then - Should not throw
        assertDoesNotThrow {
            service.didChangeWorkspaceFolders(params)
        }
    }
}
