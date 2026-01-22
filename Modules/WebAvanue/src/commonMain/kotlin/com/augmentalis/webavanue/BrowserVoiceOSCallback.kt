package com.augmentalis.webavanue

import com.augmentalis.database.dto.ScrapedWebCommandDTO
import com.augmentalis.database.repositories.IScrapedWebCommandRepository
import com.augmentalis.database.repositories.IWebAppWhitelistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * VoiceOS callback implementation for WebAvanue browser.
 *
 * Handles DOM scraping results and voice command execution.
 * Maintains state for the current page's interactive elements.
 *
 * Supports optional persistence for whitelisted web apps.
 * When repositories are provided, commands for whitelisted domains
 * are persisted to the database for faster loading on future visits.
 *
 * @param webCommandRepository Optional repository for web command persistence
 * @param whitelistRepository Optional repository for whitelist checking
 */
class BrowserVoiceOSCallback(
    private val webCommandRepository: IScrapedWebCommandRepository? = null,
    private val whitelistRepository: IWebAppWhitelistRepository? = null
) : VoiceOSWebCallback {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _currentScrapeResult = MutableStateFlow<DOMScrapeResult?>(null)
    val currentScrapeResult: StateFlow<DOMScrapeResult?> = _currentScrapeResult.asStateFlow()

    private val _isPageLoading = MutableStateFlow(false)
    val isPageLoading: StateFlow<Boolean> = _isPageLoading.asStateFlow()

    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _currentTitle = MutableStateFlow("")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()

    private val _currentDomain = MutableStateFlow("")
    val currentDomain: StateFlow<String> = _currentDomain.asStateFlow()

    private val _isWhitelistedDomain = MutableStateFlow(false)
    val isWhitelistedDomain: StateFlow<Boolean> = _isWhitelistedDomain.asStateFlow()

    // Voice command generator for matching spoken phrases to elements
    private val commandGenerator = VoiceCommandGenerator()

    override fun onDOMScraped(result: DOMScrapeResult) {
        _currentScrapeResult.value = result

        // Clear previous commands and add new elements
        commandGenerator.clear()
        commandGenerator.addElements(result.elements)

        val commandCount = commandGenerator.getCommandCount()
        println("VoiceOS: DOM scraped - ${result.elementCount} elements, $commandCount voice commands generated")

        // Persist commands if domain is whitelisted
        if (_isWhitelistedDomain.value && webCommandRepository != null) {
            scope.launch {
                persistCommands(result)
            }
        }
    }

    override fun onPageLoadStarted(url: String) {
        _isPageLoading.value = true
        _currentUrl.value = url

        // Extract domain from URL
        val domain = extractDomain(url)
        _currentDomain.value = domain

        // Clear previous scrape result when new page starts loading
        _currentScrapeResult.value = null
        commandGenerator.clear()

        // Check if domain is whitelisted
        scope.launch {
            checkWhitelistAndLoadCommands(domain, url)
        }

        println("VoiceOS: Page load started - $url (domain: $domain)")
    }

    override fun onPageLoadFinished(url: String, title: String) {
        _isPageLoading.value = false
        _currentUrl.value = url
        _currentTitle.value = title

        // Record visit for whitelisted domains
        if (_isWhitelistedDomain.value && whitelistRepository != null) {
            scope.launch {
                val domain = _currentDomain.value
                whitelistRepository.recordVisit(domain, System.currentTimeMillis())
            }
        }

        println("VoiceOS: Page load finished - $title ($url)")
    }

    override fun onDOMContentChanged() {
        // DOM mutation detected - may need to rescrape
        println("VoiceOS: DOM content changed")
    }

    override suspend fun executeCommand(command: VoiceCommandGenerator.WebVoiceCommand): Boolean {
        println("VoiceOS: Executing command - ${command.action} on ${command.fullText}")

        // Increment usage count if persisted
        if (_isWhitelistedDomain.value && webCommandRepository != null) {
            val domain = _currentDomain.value
            webCommandRepository.incrementUsageByHash(
                command.vosId,
                domain,
                System.currentTimeMillis()
            )
        }

        // TODO: Execute the command via JavaScript injection
        return true
    }

    /**
     * Check if domain is whitelisted and load existing commands.
     */
    private suspend fun checkWhitelistAndLoadCommands(domain: String, url: String) {
        if (whitelistRepository == null || webCommandRepository == null) {
            _isWhitelistedDomain.value = false
            return
        }

        val isWhitelisted = whitelistRepository.isWhitelisted(domain)
        _isWhitelistedDomain.value = isWhitelisted

        if (isWhitelisted) {
            println("VoiceOS: Domain $domain is whitelisted, loading saved commands")

            // Load existing commands from database
            val savedCommands = webCommandRepository.getByDomainAndUrl(domain, url)
            if (savedCommands.isNotEmpty()) {
                println("VoiceOS: Loaded ${savedCommands.size} saved commands for $domain")
                // Commands will be merged when DOM scrape completes
            }
        }
    }

    /**
     * Persist commands to database for whitelisted domain.
     */
    private suspend fun persistCommands(result: DOMScrapeResult) {
        if (webCommandRepository == null || whitelistRepository == null) return

        val domain = _currentDomain.value
        val url = result.url
        val now = System.currentTimeMillis()

        // Get URL pattern (path without query params)
        val urlPattern = extractUrlPattern(url)

        val commands = commandGenerator.getAllCommands().map { cmd ->
            ScrapedWebCommandDTO(
                elementHash = generateElementHash(cmd),
                domainId = domain,
                urlPattern = urlPattern,
                cssSelector = cmd.selector,
                xpath = cmd.xpath,
                commandText = cmd.fullText,
                elementText = cmd.fullText,
                elementTag = cmd.metadata["tag"] ?: "",
                elementType = cmd.elementType,
                allowedActions = determineAllowedActions(cmd),
                primaryAction = cmd.action.name.lowercase(),
                confidence = 0.5f,
                createdAt = now,
                lastVerified = now,
                boundLeft = cmd.bounds.left,
                boundTop = cmd.bounds.top,
                boundWidth = cmd.bounds.width,
                boundHeight = cmd.bounds.height
            )
        }

        if (commands.isNotEmpty()) {
            webCommandRepository.insertBatch(commands)
            whitelistRepository.updateCommandCount(domain, commands.size, now)
            println("VoiceOS: Persisted ${commands.size} commands for $domain")
        }
    }

    /**
     * Extract domain from URL.
     */
    private fun extractDomain(url: String): String {
        return try {
            val withoutProtocol = url
                .removePrefix("https://")
                .removePrefix("http://")
            val host = withoutProtocol.substringBefore("/").substringBefore("?")
            host.removePrefix("www.")
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Extract URL pattern (path without query params).
     */
    private fun extractUrlPattern(url: String): String {
        return try {
            val withoutProtocol = url
                .removePrefix("https://")
                .removePrefix("http://")
            val afterHost = withoutProtocol.substringAfter("/", "")
            val path = afterHost.substringBefore("?")
            if (path.isEmpty()) "/" else "/$path"
        } catch (e: Exception) {
            "/"
        }
    }

    /**
     * Generate a hash for the element for stable identification.
     */
    private fun generateElementHash(cmd: VoiceCommandGenerator.WebVoiceCommand): String {
        val data = "${cmd.selector}:${cmd.elementType}:${cmd.fullText}"
        return data.hashCode().toString(16)
    }

    /**
     * Determine allowed actions based on element type.
     */
    private fun determineAllowedActions(cmd: VoiceCommandGenerator.WebVoiceCommand): List<String> {
        return when (cmd.action) {
            VoiceCommandGenerator.CommandAction.CLICK -> listOf("click", "tap", "press")
            VoiceCommandGenerator.CommandAction.FOCUS -> listOf("focus", "type", "input")
            VoiceCommandGenerator.CommandAction.INPUT -> listOf("type", "input", "fill")
            VoiceCommandGenerator.CommandAction.SCROLL_TO -> listOf("scroll_to", "show", "reveal")
            VoiceCommandGenerator.CommandAction.TOGGLE -> listOf("toggle", "check", "uncheck")
            VoiceCommandGenerator.CommandAction.SELECT -> listOf("select", "choose", "pick")
        }
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

    companion object {
        /**
         * Create a basic callback without persistence (memory-only).
         */
        fun createBasic(): BrowserVoiceOSCallback {
            return BrowserVoiceOSCallback()
        }

        /**
         * Create a callback with persistence support.
         */
        fun createWithPersistence(
            webCommandRepository: IScrapedWebCommandRepository,
            whitelistRepository: IWebAppWhitelistRepository
        ): BrowserVoiceOSCallback {
            return BrowserVoiceOSCallback(webCommandRepository, whitelistRepository)
        }
    }
}
