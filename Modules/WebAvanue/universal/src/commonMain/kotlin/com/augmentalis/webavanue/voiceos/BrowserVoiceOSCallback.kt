package com.augmentalis.webavanue.voiceos

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * VoiceOS callback implementation for WebAvanue browser.
 *
 * Handles DOM scraping results and voice command execution.
 * Maintains state for the current page's interactive elements.
 */
class BrowserVoiceOSCallback : VoiceOSWebCallback {

    private val _currentScrapeResult = MutableStateFlow<DOMScrapeResult?>(null)
    val currentScrapeResult: StateFlow<DOMScrapeResult?> = _currentScrapeResult.asStateFlow()

    private val _isPageLoading = MutableStateFlow(false)
    val isPageLoading: StateFlow<Boolean> = _isPageLoading.asStateFlow()

    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _currentTitle = MutableStateFlow("")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()

    // Voice command generator for matching spoken phrases to elements
    private val commandGenerator = VoiceCommandGenerator()

    override fun onDOMScraped(result: DOMScrapeResult) {
        _currentScrapeResult.value = result

        // Clear previous commands and add new elements
        commandGenerator.clear()
        commandGenerator.addElements(result.elements)

        println("VoiceOS: DOM scraped - ${result.elementCount} elements, ${commandGenerator.getCommandCount()} voice commands generated")
    }

    override fun onPageLoadStarted(url: String) {
        _isPageLoading.value = true
        _currentUrl.value = url

        // Clear previous scrape result when new page starts loading
        _currentScrapeResult.value = null
        commandGenerator.clear()

        println("VoiceOS: Page load started - $url")
    }

    override fun onPageLoadFinished(url: String, title: String) {
        _isPageLoading.value = false
        _currentUrl.value = url
        _currentTitle.value = title

        println("VoiceOS: Page load finished - $title ($url)")
    }

    override fun onDOMContentChanged() {
        // DOM mutation detected - may need to rescrape
        println("VoiceOS: DOM content changed")
    }

    override suspend fun executeCommand(command: VoiceCommandGenerator.WebVoiceCommand): Boolean {
        // TODO: Execute the command via JavaScript injection
        println("VoiceOS: Executing command - ${command.action} on ${command.fullText}")
        return true
    }

    /**
     * Find matching voice commands for a spoken phrase.
     *
     * @param spokenPhrase The phrase the user spoke
     * @return List of matching commands sorted by confidence
     */
    fun findMatches(spokenPhrase: String): List<VoiceCommandGenerator.MatchResult> {
        return commandGenerator.findMatches(spokenPhrase)
    }

    /**
     * Get all available voice commands for the current page.
     */
    fun getAllCommands(): List<VoiceCommandGenerator.WebVoiceCommand> {
        return commandGenerator.getAllCommands()
    }

    /**
     * Get the count of available voice commands.
     */
    fun getCommandCount(): Int {
        return commandGenerator.getCommandCount()
    }

    /**
     * Check if a phrase could potentially match any command (fast check).
     */
    fun hasAnyPotentialMatch(spokenPhrase: String): Boolean {
        return commandGenerator.hasAnyPotentialMatch(spokenPhrase)
    }

    /**
     * Generate disambiguation options for NLU when multiple matches exist.
     */
    fun generateDisambiguationOptions(
        matches: List<VoiceCommandGenerator.MatchResult>
    ): List<VoiceCommandGenerator.DisambiguationOption> {
        return commandGenerator.generateDisambiguationOptions(matches)
    }
}
