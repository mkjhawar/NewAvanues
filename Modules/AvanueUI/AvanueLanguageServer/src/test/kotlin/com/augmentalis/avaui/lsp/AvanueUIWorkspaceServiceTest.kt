package com.augmentalis.avaui.lsp

import org.eclipse.lsp4j.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import io.mockk.*

/**
 * Unit tests for AvanueUIWorkspaceService
 */
class AvanueUIWorkspaceServiceTest {

    private lateinit var service: AvanueUIWorkspaceService
    private lateinit var mockClient: org.eclipse.lsp4j.services.LanguageClient

    @BeforeEach
    fun setup() {
        service = AvanueUIWorkspaceService()
        mockClient = mockk(relaxed = true)
        service.connect(mockClient)
    }

    @Test
    fun `executeCommand with avanueui generateTheme should return theme output`() {
        val themeJson = """
            {
                "name": "TestTheme",
                "platform": "ANDROID"
            }
        """.trimIndent()

        val params = ExecuteCommandParams().apply {
            command = "avanueui.generateTheme"
            arguments = listOf("dsl", themeJson)
        }

        val result = service.executeCommand(params).get() as Map<*, *>

        assertEquals(true, result["success"])
        assertNotNull(result["output"])
        assertEquals("TestTheme", result["themeName"])
    }

    @Test
    fun `executeCommand with avanueui generateTheme and yaml format should return yaml`() {
        val themeJson = """{ "name": "YamlTheme" }""".trimIndent()

        val params = ExecuteCommandParams().apply {
            command = "avanueui.generateTheme"
            arguments = listOf("yaml", themeJson)
        }

        val result = service.executeCommand(params).get() as Map<*, *>

        assertEquals(true, result["success"])
        assertEquals("yaml", result["format"])
        assertTrue((result["output"] as String).startsWith("#"))
    }

    @Test
    fun `executeCommand with avanueui generateTheme and json format should return json`() {
        val themeJson = """{ "name": "JsonTheme" }""".trimIndent()

        val params = ExecuteCommandParams().apply {
            command = "avanueui.generateTheme"
            arguments = listOf("json", themeJson)
        }

        val result = service.executeCommand(params).get() as Map<*, *>

        assertEquals(true, result["success"])
        assertEquals("json", result["format"])
        assertTrue((result["output"] as String).startsWith("{"))
    }

    @Test
    fun `executeCommand with avanueui generateTheme and css format should return css`() {
        val themeJson = """{ "name": "CssTheme" }""".trimIndent()

        val params = ExecuteCommandParams().apply {
            command = "avanueui.generateTheme"
            arguments = listOf("css", themeJson)
        }

        val result = service.executeCommand(params).get() as Map<*, *>

        assertEquals(true, result["success"])
        assertEquals("css", result["format"])
        assertTrue((result["output"] as String).startsWith("/*"))
    }

    @Test
    fun `executeCommand with avanueui generateTheme missing arguments should return error`() {
        val params = ExecuteCommandParams().apply {
            command = "avanueui.generateTheme"
            arguments = listOf("dsl")
        }

        val result = service.executeCommand(params).get() as Map<*, *>

        assertEquals(false, result["success"])
        assertNotNull(result["error"])
    }

    @Test
    fun `executeCommand with avanueui validateComponent should return validation result`() {
        val params = ExecuteCommandParams().apply {
            command = "avanueui.validateComponent"
            arguments = emptyList()
        }

        val result = service.executeCommand(params).get() as Map<*, *>

        assertEquals(true, result["success"])
        assertEquals(true, result["valid"])
    }

    @Test
    fun `executeCommand with avanueui formatDocument should return success`() {
        val params = ExecuteCommandParams().apply {
            command = "avanueui.formatDocument"
            arguments = listOf("file:///test.avanueui.yaml")
        }

        val result = service.executeCommand(params).get() as Map<*, *>

        assertEquals(true, result["success"])
        assertEquals(true, result["formatted"])
    }

    @Test
    fun `executeCommand with avanueui generateCode should return placeholder`() {
        val params = ExecuteCommandParams().apply {
            command = "avanueui.generateCode"
            arguments = listOf("kotlin", "Button: text: Test")
        }

        val result = service.executeCommand(params).get() as Map<*, *>

        assertEquals(true, result["success"])
        assertNotNull(result["output"])
    }

    @Test
    fun `executeCommand with unknown command should return error`() {
        val params = ExecuteCommandParams().apply {
            command = "avanueui.unknownCommand"
            arguments = emptyList()
        }

        val result = service.executeCommand(params).get() as Map<*, *>

        assertNotNull(result["error"])
        assertTrue((result["error"] as String).contains("Unknown command"))
    }

    @Test
    fun `didChangeConfiguration should not throw`() {
        val params = DidChangeConfigurationParams().apply {
            settings = mapOf("avanueui" to mapOf("enabled" to true))
        }
        assertDoesNotThrow { service.didChangeConfiguration(params) }
    }

    @Test
    fun `didChangeWatchedFiles should not throw`() {
        val params = DidChangeWatchedFilesParams().apply {
            changes = listOf(FileEvent().apply {
                uri = "file:///test.avanueui.yaml"
                type = FileChangeType.Changed
            })
        }
        assertDoesNotThrow { service.didChangeWatchedFiles(params) }
    }

    @Test
    fun `didChangeWorkspaceFolders should not throw`() {
        val params = DidChangeWorkspaceFoldersParams().apply {
            event = WorkspaceFoldersChangeEvent().apply {
                added = listOf(WorkspaceFolder().apply { uri = "file:///workspace"; name = "Test Workspace" })
                removed = emptyList()
            }
        }
        assertDoesNotThrow { service.didChangeWorkspaceFolders(params) }
    }
}
