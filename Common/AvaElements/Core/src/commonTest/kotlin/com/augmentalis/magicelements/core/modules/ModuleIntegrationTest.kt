package com.augmentalis.magicelements.core.modules

import com.augmentalis.magicelements.core.mel.ExpressionEvaluator
import com.augmentalis.magicelements.core.mel.ExpressionLexer
import com.augmentalis.magicelements.core.mel.ExpressionNode
import com.augmentalis.magicelements.core.mel.ExpressionParser
import com.augmentalis.magicelements.core.mel.functions.PluginTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the AvaCode Module System integration with MEL.
 */
class ModuleIntegrationTest {

    // ========== Module Call Parsing Tests ==========

    @Test
    fun testIsModuleCall_validExpression() {
        assertTrue(ModuleIntegration.isModuleCall("@voice.listen()"))
        assertTrue(ModuleIntegration.isModuleCall("@device.info()"))
        assertTrue(ModuleIntegration.isModuleCall("@browser.open"))
    }

    @Test
    fun testIsModuleCall_invalidExpression() {
        assertFalse(ModuleIntegration.isModuleCall("\$math.add(1, 2)"))
        assertFalse(ModuleIntegration.isModuleCall("\$state.count"))
        assertFalse(ModuleIntegration.isModuleCall("voice.listen()"))
        assertFalse(ModuleIntegration.isModuleCall(""))
    }

    @Test
    fun testParseModuleCall_simpleMethod() {
        val call = ModuleIntegration.parseModuleCall("@voice.listen()")
        assertNotNull(call)
        assertEquals("voice", call.module)
        assertEquals("listen", call.method)
        assertEquals("", call.rawArgs)
    }

    @Test
    fun testParseModuleCall_nestedMethod() {
        val call = ModuleIntegration.parseModuleCall("@device.screen.width()")
        assertNotNull(call)
        assertEquals("device", call.module)
        assertEquals("screen.width", call.method)
        assertEquals("", call.rawArgs)
    }

    @Test
    fun testParseModuleCall_withArguments() {
        val call = ModuleIntegration.parseModuleCall("@browser.open(\"https://example.com\")")
        assertNotNull(call)
        assertEquals("browser", call.module)
        assertEquals("open", call.method)
        assertEquals("\"https://example.com\"", call.rawArgs)
    }

    @Test
    fun testParseModuleCall_multipleArguments() {
        val call = ModuleIntegration.parseModuleCall("@command.execute(\"open settings\", true)")
        assertNotNull(call)
        assertEquals("command", call.module)
        assertEquals("execute", call.method)
        assertEquals("\"open settings\", true", call.rawArgs)
    }

    @Test
    fun testParseModuleCall_noParens() {
        val call = ModuleIntegration.parseModuleCall("@device.info")
        assertNotNull(call)
        assertEquals("device", call.module)
        assertEquals("info", call.method)
        assertEquals("", call.rawArgs)
    }

    // ========== Lexer Tests ==========

    @Test
    fun testLexer_moduleCall() {
        val lexer = ExpressionLexer("@voice.listen()")
        val tokens = lexer.tokenize()

        // Should produce: AT("voice"), DOT, IDENTIFIER("listen"), LPAREN, RPAREN, EOF
        assertEquals(6, tokens.size)
        assertEquals("voice", tokens[0].value)
    }

    @Test
    fun testLexer_moduleCallWithArgs() {
        val lexer = ExpressionLexer("@browser.open(\"https://test.com\")")
        val tokens = lexer.tokenize()

        // Should produce: AT("browser"), DOT, IDENTIFIER("open"), LPAREN, STRING, RPAREN, EOF
        assertTrue(tokens.size >= 7)
        assertEquals("browser", tokens[0].value)
    }

    // ========== Parser Tests ==========

    @Test
    fun testParser_moduleCall() {
        val lexer = ExpressionLexer("@voice.listen()")
        val tokens = lexer.tokenize()
        val parser = ExpressionParser(tokens)
        val ast = parser.parse()

        assertTrue(ast is ExpressionNode.ModuleCall)
        val moduleCall = ast as ExpressionNode.ModuleCall
        assertEquals("voice", moduleCall.module)
        assertEquals("listen", moduleCall.method)
        assertTrue(moduleCall.args.isEmpty())
    }

    @Test
    fun testParser_moduleCallWithStringArg() {
        val lexer = ExpressionLexer("@browser.open(\"https://example.com\")")
        val tokens = lexer.tokenize()
        val parser = ExpressionParser(tokens)
        val ast = parser.parse()

        assertTrue(ast is ExpressionNode.ModuleCall)
        val moduleCall = ast as ExpressionNode.ModuleCall
        assertEquals("browser", moduleCall.module)
        assertEquals("open", moduleCall.method)
        assertEquals(1, moduleCall.args.size)
    }

    @Test
    fun testParser_nestedModuleMethod() {
        val lexer = ExpressionLexer("@device.screen.width()")
        val tokens = lexer.tokenize()
        val parser = ExpressionParser(tokens)
        val ast = parser.parse()

        assertTrue(ast is ExpressionNode.ModuleCall)
        val moduleCall = ast as ExpressionNode.ModuleCall
        assertEquals("device", moduleCall.module)
        assertEquals("screen.width", moduleCall.method)
    }

    // ========== Module Registry Tests ==========

    @Test
    fun testModuleRegistry_registerAndGet() {
        // Create a test module
        val testModule = object : AvaCodeModule {
            override val name = "test"
            override val version = "1.0.0"
            override val minimumTier = PluginTier.DATA

            override suspend fun execute(method: String, args: List<Any?>, tier: PluginTier): Any? {
                return when (method) {
                    "echo" -> args.firstOrNull()?.toString() ?: ""
                    "add" -> {
                        val a = (args.getOrNull(0) as? Number)?.toDouble() ?: 0.0
                        val b = (args.getOrNull(1) as? Number)?.toDouble() ?: 0.0
                        a + b
                    }
                    else -> throw ModuleMethodNotFoundException(name, method)
                }
            }

            override fun isMethodAvailable(method: String, tier: PluginTier): Boolean {
                return method in listOf("echo", "add")
            }

            override fun listMethods(tier: PluginTier): List<ModuleMethod> {
                return listOf(
                    ModuleMethod("echo", "Echo input", emptyList(), "String", PluginTier.DATA),
                    ModuleMethod("add", "Add two numbers", emptyList(), "Number", PluginTier.DATA)
                )
            }

            override suspend fun initialize() {}
            override suspend fun dispose() {}
        }

        ModuleRegistry.register(testModule)

        assertTrue(ModuleRegistry.isRegistered("test"))
        assertEquals(testModule, ModuleRegistry.get("test"))
        assertTrue(ModuleRegistry.isMethodAvailable("test", "echo", PluginTier.DATA))
    }

    @Test
    fun testModuleRegistry_listModules() {
        // Register stub modules
        registerStubModules()

        val modules = ModuleRegistry.listModules()
        assertTrue(modules.isNotEmpty())

        // Should have standard modules
        val moduleNames = modules.map { it.name }
        assertTrue("voice" in moduleNames)
        assertTrue("device" in moduleNames)
        assertTrue("command" in moduleNames)
        assertTrue("data" in moduleNames)
        assertTrue("browser" in moduleNames)
    }

    // ========== Tier Enforcement Tests ==========

    @Test
    fun testGetAvailableModules_dataTier() {
        registerStubModules()

        val modules = ModuleIntegration.getAvailableModules(PluginTier.DATA)

        // All modules with DATA tier should be available
        assertTrue(modules.isNotEmpty())
        modules.forEach { module ->
            assertTrue(module.minimumTier == PluginTier.DATA)
        }
    }

    @Test
    fun testGetAvailableMethods_dataTier() {
        registerStubModules()

        val methods = ModuleIntegration.getAvailableMethods("voice", PluginTier.DATA)

        // DATA tier methods should be available
        assertTrue(methods.any { it.name == "isListening" || it.name == "listen" })
    }

    @Test
    fun testIsValidCall_dataTier() {
        registerStubModules()

        // DATA tier methods should be valid
        assertTrue(ModuleIntegration.isValidCall("@device.info()", PluginTier.DATA))
        assertTrue(ModuleIntegration.isValidCall("@device.platform()", PluginTier.DATA))
    }
}
