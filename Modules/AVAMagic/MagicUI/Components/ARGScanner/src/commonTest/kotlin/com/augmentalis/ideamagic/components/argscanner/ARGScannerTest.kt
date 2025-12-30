package com.augmentalis.magicui.components.argscanner

import kotlin.test.*

class ARGScannerTest {

    private val browserARGJson = """
        {
          "version": "1.0",
          "app": {
            "id": "com.augmentalis.avanue.browser",
            "name": "BrowserAvanue",
            "version": "1.0.0",
            "description": "Voice-controlled web browser",
            "packageName": "com.augmentalis.avanue.browser",
            "category": "PRODUCTIVITY"
          },
          "capabilities": [
            {
              "id": "capability.browse_web",
              "name": "Browse Web",
              "description": "Open web pages with voice",
              "type": "ACTION",
              "voiceCommands": ["open {url}", "browse to {url}"],
              "params": [
                {
                  "name": "url",
                  "type": "URL",
                  "required": true,
                  "description": "Web URL to open"
                }
              ],
              "requiresPermissions": ["android.permission.INTERNET"]
            }
          ],
          "intentFilters": [
            {
              "action": "android.intent.action.VIEW",
              "categories": ["android.intent.category.BROWSABLE"],
              "dataSchemes": ["http", "https"],
              "dataMimeTypes": [],
              "priority": 100
            }
          ],
          "services": [],
          "contentProviders": []
        }
    """.trimIndent()

    @Test
    fun testParseARGFile() {
        val parser = ARGParser()
        val argFile = parser.parse(browserARGJson)

        assertEquals("1.0", argFile.version)
        assertEquals("com.augmentalis.avanue.browser", argFile.app.id)
        assertEquals("BrowserAvanue", argFile.app.name)
        assertEquals("1.0.0", argFile.app.version)
        assertEquals(AppCategory.PRODUCTIVITY, argFile.app.category)
        assertEquals(1, argFile.capabilities.size)
        assertEquals(1, argFile.intentFilters.size)
    }

    @Test
    fun testValidateARGFile() {
        val parser = ARGParser()
        val argFile = parser.parse(browserARGJson)

        val errors = parser.validate(argFile)
        assertTrue(errors.isEmpty(), "Valid ARG file should have no validation errors")
    }

    @Test
    fun testValidateInvalidVersion() {
        val invalidJson = """
            {
              "version": "invalid",
              "app": {
                "id": "test.app",
                "name": "Test",
                "version": "1.0.0",
                "description": "Test app",
                "packageName": "test.app",
                "category": "UTILITY"
              },
              "capabilities": [],
              "intentFilters": [],
              "services": [],
              "contentProviders": []
            }
        """.trimIndent()

        val parser = ARGParser()
        val argFile = parser.parse(invalidJson)
        val errors = parser.validate(argFile)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it is ValidationError.InvalidVersion })
    }

    @Test
    fun testValidateMissingParameters() {
        val invalidJson = """
            {
              "version": "1.0",
              "app": {
                "id": "test.app",
                "name": "Test",
                "version": "1.0.0",
                "description": "Test app",
                "packageName": "test.app",
                "category": "UTILITY"
              },
              "capabilities": [
                {
                  "id": "test.capability",
                  "name": "Test",
                  "description": "Test capability",
                  "type": "ACTION",
                  "voiceCommands": ["do {action} with {target}"],
                  "params": [
                    {
                      "name": "action",
                      "type": "STRING",
                      "required": true
                    }
                  ],
                  "requiresPermissions": []
                }
              ],
              "intentFilters": [],
              "services": [],
              "contentProviders": []
            }
        """.trimIndent()

        val parser = ARGParser()
        val argFile = parser.parse(invalidJson)
        val errors = parser.validate(argFile)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it is ValidationError.MissingParameter })
    }

    @Test
    fun testRegistryRegister() {
        val registry = ARGRegistry()
        val parser = ARGParser()
        val argFile = parser.parse(browserARGJson)

        registry.register(argFile)

        assertEquals(1, registry.count())
        assertNotNull(registry.findByAppId("com.augmentalis.avanue.browser"))
    }

    @Test
    fun testRegistrySearchCapabilities() {
        val registry = ARGRegistry()
        val parser = ARGParser()
        val argFile = parser.parse(browserARGJson)

        registry.register(argFile)

        val results = registry.searchCapabilities("browse")
        assertEquals(1, results.size)
        assertEquals("capability.browse_web", results[0].capability.id)
    }

    @Test
    fun testRegistryFindByIntent() {
        val registry = ARGRegistry()
        val parser = ARGParser()
        val argFile = parser.parse(browserARGJson)

        registry.register(argFile)

        val apps = registry.findByIntent(
            action = "android.intent.action.VIEW",
            dataUri = "https://example.com"
        )

        assertEquals(1, apps.size)
        assertEquals("com.augmentalis.avanue.browser", apps[0].app.id)
    }

    @Test
    fun testRegistryGetByCategory() {
        val registry = ARGRegistry()
        val parser = ARGParser()
        val argFile = parser.parse(browserARGJson)

        registry.register(argFile)

        val apps = registry.getByCategory(AppCategory.PRODUCTIVITY)
        assertEquals(1, apps.size)

        val utilityApps = registry.getByCategory(AppCategory.UTILITY)
        assertEquals(0, utilityApps.size)
    }

    @Test
    fun testToJson() {
        val parser = ARGParser()
        val original = parser.parse(browserARGJson)

        val jsonString = parser.toJson(original)
        val reparsed = parser.parse(jsonString)

        assertEquals(original.app.id, reparsed.app.id)
        assertEquals(original.capabilities.size, reparsed.capabilities.size)
        assertEquals(original.intentFilters.size, reparsed.intentFilters.size)
    }

    @Test
    fun testCapabilityTypes() {
        val types = CapabilityType.values()
        assertEquals(5, types.size)
        assertTrue(types.contains(CapabilityType.ACTION))
        assertTrue(types.contains(CapabilityType.QUERY))
        assertTrue(types.contains(CapabilityType.TRANSFORM))
        assertTrue(types.contains(CapabilityType.PROVIDER))
        assertTrue(types.contains(CapabilityType.HANDLER))
    }

    @Test
    fun testParamTypes() {
        val types = ParamType.values()
        assertTrue(types.contains(ParamType.STRING))
        assertTrue(types.contains(ParamType.INT))
        assertTrue(types.contains(ParamType.URL))
        assertTrue(types.contains(ParamType.JSON))
    }

    @Test
    fun testRegistryUnregister() {
        val registry = ARGRegistry()
        val parser = ARGParser()
        val argFile = parser.parse(browserARGJson)

        registry.register(argFile)
        assertEquals(1, registry.count())

        registry.unregister("com.augmentalis.avanue.browser")
        assertEquals(0, registry.count())
        assertNull(registry.findByAppId("com.augmentalis.avanue.browser"))
    }

    @Test
    fun testRegistryClear() {
        val registry = ARGRegistry()
        val parser = ARGParser()
        val argFile = parser.parse(browserARGJson)

        registry.register(argFile)
        assertEquals(1, registry.count())

        registry.clear()
        assertEquals(0, registry.count())
    }
}
