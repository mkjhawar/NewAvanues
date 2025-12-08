package com.augmentalis.avanues.avamagic.components.voicerouter

import com.augmentalis.avanues.avamagic.components.argscanner.*
import kotlin.test.*

class VoiceCommandRouterTest {

    private lateinit var registry: ARGRegistry
    private lateinit var router: VoiceCommandRouter

    @BeforeTest
    fun setup() {
        registry = ARGRegistry()

        // Register test apps
        registry.register(createBrowserApp())
        registry.register(createNoteApp())
        registry.register(createSearchApp())

        router = VoiceCommandRouter(registry)
    }

    // ==================== Route Tests ====================
    // Note: Full routing tests require Android Intent creation
    // These tests focus on matching logic. Intent creation is tested in Android instrumentation tests.

    @Test
    fun testRouteNoMatch() {
        val matcher = VoiceCommandMatcher(registry)
        val matches = matcher.match("fly to the moon")

        assertTrue(matches.isEmpty(), "Should have no matches for nonsense command")
    }

    // ==================== Matcher Tests ====================

    @Test
    fun testGetAllMatches() {
        val matches = router.getAllMatches("search for kotlin")

        assertTrue(matches.isNotEmpty())
        // Should return multiple apps that can search
        assertTrue(matches.size >= 2)
        // Results should be sorted by score
        for (i in 0 until matches.size - 1) {
            assertTrue(matches[i].score >= matches[i + 1].score)
        }
    }

    @Test
    fun testCanRouteViaMatching() {
        val matcher = VoiceCommandMatcher(registry)

        // Should find matches for valid commands
        assertTrue(matcher.match("open github.com").isNotEmpty(), "Should match browse command")
        assertTrue(matcher.match("create note test").isNotEmpty(), "Should match note creation with title")

        // Should not find matches for invalid commands
        assertTrue(matcher.match("impossible command xyz").isEmpty(), "Should not match nonsense")
    }

    // ==================== Pattern Matching Tests ====================

    @Test
    fun testPatternMatchingExact() {
        val matcher = VoiceCommandMatcher(registry)
        val matches = matcher.match("open google.com")

        assertTrue(matches.isNotEmpty())
        val topMatch = matches[0]
        assertEquals("open {url}", topMatch.pattern)
        assertEquals("google.com", topMatch.parameters["url"])
    }

    @Test
    fun testPatternMatchingFuzzy() {
        val matcher = VoiceCommandMatcher(registry)
        // Typo: "oppen" instead of "open" (1 extra character = distance 1)
        val matches = matcher.match("oppen google.com")

        // Fuzzy matching with distance 1 should still find it
        // Note: Standard Levenshtein distance counts transpositions as 2 operations
        // so "opne" would be distance 2. We use "oppen" (distance 1) for this test.
        assertTrue(matches.isNotEmpty(), "Fuzzy match should find similar patterns")
        if (matches.isNotEmpty()) {
            val topMatch = matches[0]
            assertEquals("google.com", topMatch.parameters["url"])
        }
    }

    @Test
    fun testPatternMatchingMultipleParameters() {
        val matcher = VoiceCommandMatcher(registry)
        val matches = matcher.match("create note shopping list with content buy milk")

        assertTrue(matches.isNotEmpty())
        val topMatch = matches[0]
        assertEquals("shopping list", topMatch.parameters["title"])
        assertEquals("buy milk", topMatch.parameters["content"])
    }

    @Test
    fun testPatternMatchingCaptureRemaining() {
        val matcher = VoiceCommandMatcher(registry)
        val matches = matcher.match("browse to https://github.com/kotlin/kotlinx.coroutines")

        assertTrue(matches.isNotEmpty())
        val topMatch = matches[0]
        assertEquals("https://github.com/kotlin/kotlinx.coroutines", topMatch.parameters["url"])
    }

    @Test
    fun testScoringSorting() {
        val matcher = VoiceCommandMatcher(registry)
        val matches = matcher.match("open website")

        // All matches should be sorted by score descending
        for (i in 0 until matches.size - 1) {
            assertTrue(matches[i].score >= matches[i + 1].score,
                "Match ${i} score (${matches[i].score}) should be >= match ${i+1} score (${matches[i + 1].score})")
        }
    }

    // ==================== Router Config Tests ====================
    // Note: Router configuration tests with actual routing require Android Intent creation
    // These are tested in Android instrumentation tests

    @Test
    fun testRouterConfigCreation() {
        // Test that router config can be created with custom values
        val config = RouterConfig(
            ambiguityThreshold = 0.05f,
            minimumScore = 0.8f,
            maxAmbiguousMatches = 3,
            enableFuzzyMatching = false
        )

        assertEquals(0.05f, config.ambiguityThreshold)
        assertEquals(0.8f, config.minimumScore)
        assertEquals(3, config.maxAmbiguousMatches)
        assertFalse(config.enableFuzzyMatching)
    }

    // ==================== Helper Functions ====================

    private fun createBrowserApp(): ARGFile {
        return ARGFile(
            version = "1.0",
            app = AppInfo(
                id = "com.augmentalis.avanue.browser",
                name = "BrowserAvanue",
                version = "1.0.0",
                description = "Voice-controlled web browser",
                packageName = "com.augmentalis.avanue.browser",
                category = AppCategory.PRODUCTIVITY
            ),
            capabilities = listOf(
                Capability(
                    id = "capability.browse_web",
                    name = "Browse Web",
                    description = "Open websites and browse the web",
                    type = CapabilityType.ACTION,
                    voiceCommands = listOf(
                        "open {url}",
                        "browse to {url}",
                        "go to {url}",
                        "navigate to {url}"
                    ),
                    params = listOf(
                        CapabilityParam(
                            name = "url",
                            type = ParamType.URL,
                            required = true,
                            description = "Website URL to open"
                        )
                    )
                ),
                Capability(
                    id = "capability.search_web",
                    name = "Search Web",
                    description = "Search the web",
                    type = CapabilityType.QUERY,
                    voiceCommands = listOf(
                        "search for {query}",
                        "search {query}",
                        "find {query}"
                    ),
                    params = listOf(
                        CapabilityParam(
                            name = "query",
                            type = ParamType.STRING,
                            required = true,
                            description = "Search query"
                        )
                    )
                )
            )
        )
    }

    private fun createNoteApp(): ARGFile {
        return ARGFile(
            version = "1.0",
            app = AppInfo(
                id = "com.augmentalis.avanue.notes",
                name = "NoteAvanue",
                version = "1.0.0",
                description = "Voice-controlled note taking",
                packageName = "com.augmentalis.avanue.notes",
                category = AppCategory.PRODUCTIVITY
            ),
            capabilities = listOf(
                Capability(
                    id = "capability.create_note",
                    name = "Create Note",
                    description = "Create a new note",
                    type = CapabilityType.ACTION,
                    voiceCommands = listOf(
                        "create note {title}",
                        "new note {title}",
                        "create note {title} with content {content}"
                    ),
                    params = listOf(
                        CapabilityParam(
                            name = "title",
                            type = ParamType.STRING,
                            required = true,
                            description = "Note title"
                        ),
                        CapabilityParam(
                            name = "content",
                            type = ParamType.STRING,
                            required = false,
                            description = "Note content"
                        )
                    )
                )
            )
        )
    }

    private fun createSearchApp(): ARGFile {
        return ARGFile(
            version = "1.0",
            app = AppInfo(
                id = "com.augmentalis.avanue.search",
                name = "SearchAvanue",
                version = "1.0.0",
                description = "Universal search app",
                packageName = "com.augmentalis.avanue.search",
                category = AppCategory.UTILITY
            ),
            capabilities = listOf(
                Capability(
                    id = "capability.universal_search",
                    name = "Universal Search",
                    description = "Search across all sources",
                    type = CapabilityType.QUERY,
                    voiceCommands = listOf(
                        "search for {query}",
                        "find {query}",
                        "look for {query}"
                    ),
                    params = listOf(
                        CapabilityParam(
                            name = "query",
                            type = ParamType.STRING,
                            required = true,
                            description = "Search query"
                        )
                    )
                )
            )
        )
    }
}
