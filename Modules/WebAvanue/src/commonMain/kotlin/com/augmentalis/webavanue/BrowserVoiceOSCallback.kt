package com.augmentalis.webavanue

import com.augmentalis.database.dto.ScrapedWebCommandDTO
import com.augmentalis.database.repositories.IScrapedWebCommandRepository
import com.augmentalis.database.repositories.IWebAppWhitelistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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

    // ===== UI State for feedback components =====

    // Scanning state for DOMScrapingIndicator
    private val _scrapingState = MutableStateFlow<DOMScrapingState>(DOMScrapingState.Idle)
    val scrapingState: StateFlow<DOMScrapingState> = _scrapingState.asStateFlow()

    // Voice command status for VoiceCommandStatusBar
    private val _voiceStatus = MutableStateFlow<VoiceCommandStatus>(VoiceCommandStatus.Idle)
    val voiceStatus: StateFlow<VoiceCommandStatus> = _voiceStatus.asStateFlow()

    // Command count for quick display
    private val _commandCount = MutableStateFlow(0)
    val commandCount: StateFlow<Int> = _commandCount.asStateFlow()

    // Recent executed commands for history
    private val _recentCommands = MutableStateFlow<List<String>>(emptyList())
    val recentCommands: StateFlow<List<String>> = _recentCommands.asStateFlow()

    // Last execution result for feedback UI
    private val _lastExecutionResult = MutableStateFlow<CommandExecutionResult?>(null)
    val lastExecutionResult: StateFlow<CommandExecutionResult?> = _lastExecutionResult.asStateFlow()

    // Voice command generator for matching spoken phrases to elements
    private val commandGenerator = VoiceCommandGenerator()

    // ===== Session Cache (Phase 2: DOM Scraping Persistence) =====

    /**
     * Cached page data for session-level LRU cache.
     * Stores scraped DOM elements and derived phrases so returning to a
     * previously visited page restores voice commands instantly without re-scraping.
     */
    private data class CachedPage(
        val urlHash: String,
        val url: String,
        val domain: String,
        val structureHash: String,
        val elements: List<DOMElement>,
        val phrases: List<String>,
        val elementCount: Int,
        val commandCount: Int,
        val cachedAt: Long
    )

    /**
     * Session cache keyed by URL hash.
     * Uses a plain map with manual eviction (KMP-safe — no JVM-specific
     * LinkedHashMap accessOrder or removeEldestEntry).
     * Evicts the oldest entry (by cachedAt) when exceeding MAX_SESSION_CACHE_SIZE.
     *
     * Thread safety: All callers (onPageLoadStarted, onDOMScraped, onPageLoadFinished)
     * are WebView callbacks that fire on the Android main thread only.
     * The scope coroutines that touch sessionCache also dispatch on Default but
     * only for whitelist/persist operations that do NOT read or write sessionCache.
     * No synchronization is needed — matches the existing pattern in this class
     * (StateFlow writes, commandGenerator mutations all unsynchronized, main-thread only).
     */
    private val sessionCache = mutableMapOf<String, CachedPage>()

    override fun onDOMScraped(result: DOMScrapeResult) {
        _currentScrapeResult.value = result

        // Clear previous commands and add new elements
        commandGenerator.clear()
        commandGenerator.addElements(result.elements)

        val count = commandGenerator.getCommandCount()
        _commandCount.value = count

        // Update scraping state to complete
        _scrapingState.value = DOMScrapingState.Complete(
            elementCount = result.elementCount,
            commandCount = count,
            isWhitelisted = _isWhitelistedDomain.value
        )

        // Update voice status to ready
        _voiceStatus.value = VoiceCommandStatus.Ready(
            commandCount = count,
            isWhitelisted = _isWhitelistedDomain.value
        )

        println("VoiceOS: DOM scraped - ${result.elementCount} elements, $count voice commands generated")

        // Emit phrases to static flow for speech engine grammar integration.
        // The accessibility service collects this and routes to VoiceOSCore.updateWebCommands().
        val phrases = commandGenerator.getAllCommands().map { it.fullText }
        _activeWebPhrases.value = phrases

        // Update session cache — next visit to this URL will restore instantly.
        // Cap element storage to limit memory: 200 elements × ~500 bytes × 5 pages ≈ 500KB.
        val urlHash = computeUrlHash(result.url)
        val cachedElements = if (result.elements.size > MAX_CACHED_ELEMENTS_PER_PAGE) {
            result.elements.take(MAX_CACHED_ELEMENTS_PER_PAGE)
        } else {
            result.elements
        }
        sessionCache[urlHash] = CachedPage(
            urlHash = urlHash,
            url = result.url,
            domain = _currentDomain.value,
            structureHash = result.structureHash,
            elements = cachedElements,
            phrases = phrases,
            elementCount = result.elementCount,
            commandCount = count,
            cachedAt = System.currentTimeMillis()
        )
        evictSessionCacheIfNeeded()

        // Persist commands if domain is whitelisted
        if (_isWhitelistedDomain.value && webCommandRepository != null) {
            scope.launch {
                persistCommands(result)
            }
        }

        // Auto-clear scraping complete state after delay
        scope.launch {
            delay(3000)
            if (_scrapingState.value is DOMScrapingState.Complete) {
                _scrapingState.value = DOMScrapingState.Idle
            }
        }
    }

    override fun onPageLoadStarted(url: String) {
        _isPageLoading.value = true
        _currentUrl.value = url

        // Extract domain from URL
        val domain = extractDomain(url)
        _currentDomain.value = domain

        // Check session cache — if we've visited this URL recently, restore
        // voice commands instantly instead of waiting for a full DOM scrape.
        val urlHash = computeUrlHash(url)
        val cached = sessionCache[urlHash]

        if (cached != null) {
            // Session cache HIT: restore commands immediately
            commandGenerator.clear()
            commandGenerator.addElements(cached.elements)
            val count = commandGenerator.getCommandCount()
            _commandCount.value = count
            _activeWebPhrases.value = cached.phrases

            _scrapingState.value = DOMScrapingState.Complete(
                elementCount = cached.elementCount,
                commandCount = count,
                isWhitelisted = _isWhitelistedDomain.value,
                fromCache = true
            )
            _voiceStatus.value = VoiceCommandStatus.Ready(
                commandCount = count,
                isWhitelisted = _isWhitelistedDomain.value
            )

            println("VoiceOS: Session cache HIT for $url ($count commands, cached ${(System.currentTimeMillis() - cached.cachedAt) / 1000}s ago)")

            // Auto-clear the "from cache" indicator after a short delay
            scope.launch {
                delay(3000)
                if (_scrapingState.value is DOMScrapingState.Complete) {
                    _scrapingState.value = DOMScrapingState.Idle
                }
            }

            // Still check whitelist async (doesn't affect cached commands)
            scope.launch {
                checkWhitelistAndLoadCommands(domain, url)
            }

            // NOTE: The page will still load and onDOMScraped will fire when
            // the JS bridge scrapes. That fresh scrape will update the cache
            // and overwrite these restored commands with current DOM state.
            return
        }

        // Session cache MISS: normal flow — clear and wait for scrape
        _currentScrapeResult.value = null
        commandGenerator.clear()
        _commandCount.value = 0

        // Update UI states for scanning
        _scrapingState.value = DOMScrapingState.Scanning()
        _voiceStatus.value = VoiceCommandStatus.Scanning

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

        // Update voice status to processing
        _voiceStatus.value = VoiceCommandStatus.Processing(command.fullText)

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
        val success = true

        // Update execution result for UI feedback
        _lastExecutionResult.value = if (success) {
            CommandExecutionResult.Success(
                command = command.fullText,
                action = command.action.name.lowercase(),
                target = command.metadata["tag"]
            )
        } else {
            CommandExecutionResult.Failure(
                command = command.fullText,
                reason = "Element not found or not interactable"
            )
        }

        // Update voice status
        _voiceStatus.value = VoiceCommandStatus.Executed(command.fullText, success)

        // Add to recent commands
        val recent = _recentCommands.value.toMutableList()
        recent.add(0, command.fullText)
        _recentCommands.value = recent.take(10)

        // Reset voice status after delay
        scope.launch {
            delay(2000)
            val count = _commandCount.value
            _voiceStatus.value = VoiceCommandStatus.Ready(count, _isWhitelistedDomain.value)
        }

        return success
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

    // ===== Session Cache Control =====

    /**
     * Invalidate the current page's session cache entry.
     *
     * Called when the user says "retrain page" or "rescan page" to force
     * a fresh DOM scrape on the next visit instead of restoring from cache.
     */
    fun invalidateCurrentPage() {
        val url = _currentUrl.value
        if (url.isNotBlank()) {
            val urlHash = computeUrlHash(url)
            sessionCache.remove(urlHash)
            println("VoiceOS: Session cache invalidated for $url")
        }
    }

    /**
     * Clear the entire session cache.
     * Called on service destroy or when memory pressure requires eviction.
     */
    fun clearSessionCache() {
        sessionCache.clear()
        println("VoiceOS: Session cache cleared")
    }

    /**
     * Get the number of pages in session cache (for debugging/display).
     */
    fun getSessionCacheSize(): Int = sessionCache.size

    /**
     * Evict the oldest cache entry if over capacity.
     * Uses cachedAt timestamp for LRU-like behavior (oldest = least useful).
     */
    private fun evictSessionCacheIfNeeded() {
        while (sessionCache.size > MAX_SESSION_CACHE_SIZE) {
            val oldest = sessionCache.entries.minByOrNull { it.value.cachedAt }
            if (oldest != null) {
                sessionCache.remove(oldest.key)
                println("VoiceOS: Session cache evicted ${oldest.value.url}")
            } else break
        }
    }

    /**
     * Compute a stable hash for a URL, stripping fragment and trailing slash.
     * Returns an 8-character hex string suitable for cache keys.
     */
    private fun computeUrlHash(url: String): String {
        val normalized = url.substringBefore("#").trimEnd('/')
        return normalized.hashCode().toUInt().toString(16).padStart(8, '0')
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

    // ===== UI Control Methods =====

    /**
     * Start listening for voice input.
     * Updates voice status to Listening state.
     */
    fun startListening() {
        _voiceStatus.value = VoiceCommandStatus.Listening()
    }

    /**
     * Update partial speech recognition result.
     */
    fun updatePartialResult(partialText: String) {
        if (_voiceStatus.value is VoiceCommandStatus.Listening) {
            _voiceStatus.value = VoiceCommandStatus.Listening(partialText)
        }
    }

    /**
     * Stop listening and process the spoken phrase.
     */
    suspend fun processSpokenPhrase(phrase: String) {
        _voiceStatus.value = VoiceCommandStatus.Processing(phrase)

        val matches = findMatches(phrase)

        when {
            matches.isEmpty() -> {
                // No matches found
                _lastExecutionResult.value = CommandExecutionResult.NotFound(
                    command = phrase,
                    suggestions = getSimilarCommands(phrase)
                )
                _voiceStatus.value = VoiceCommandStatus.Ready(_commandCount.value, _isWhitelistedDomain.value)
            }
            matches.size == 1 -> {
                // Single match - execute directly
                executeCommand(matches[0].command)
            }
            else -> {
                // Multiple matches - need disambiguation
                val options = matches.take(5).mapIndexed { index, match ->
                    DisambiguationOption(
                        index = index + 1,
                        text = match.command.fullText,
                        elementType = match.command.elementType,
                        preview = match.command.words.take(3).joinToString(" ")
                    )
                }
                _lastExecutionResult.value = CommandExecutionResult.Disambiguate(
                    command = phrase,
                    options = options
                )
                _voiceStatus.value = VoiceCommandStatus.Ready(_commandCount.value, _isWhitelistedDomain.value)
            }
        }
    }

    /**
     * Cancel listening without processing.
     */
    fun cancelListening() {
        _voiceStatus.value = VoiceCommandStatus.Ready(_commandCount.value, _isWhitelistedDomain.value)
    }

    /**
     * Select a disambiguation option and execute it.
     */
    suspend fun selectDisambiguationOption(index: Int) {
        val matches = getAllCommands()
        if (index in 1..matches.size) {
            val command = matches[index - 1]
            executeCommand(command)
        }
    }

    /**
     * Clear the last execution result (dismiss feedback).
     */
    fun clearExecutionResult() {
        _lastExecutionResult.value = null
    }

    /**
     * Get commands similar to the spoken phrase for suggestions.
     */
    private fun getSimilarCommands(phrase: String): List<String> {
        val allCommands = getAllCommands()
        return allCommands
            .filter { cmd ->
                cmd.words.any { word ->
                    phrase.lowercase().contains(word.take(3))
                }
            }
            .take(3)
            .map { it.fullText }
    }

    /**
     * Get commands as display objects for UI.
     */
    fun getCommandsForDisplay(): List<WebVoiceCommandDisplay> {
        return getAllCommands().map { cmd ->
            WebVoiceCommandDisplay(
                phrase = cmd.fullText,
                description = "${cmd.action.name.lowercase()} ${cmd.elementType}",
                elementType = cmd.elementType,
                action = cmd.action.name.lowercase(),
                isSaved = _isWhitelistedDomain.value
            )
        }
    }

    companion object {
        /** Maximum pages held in session-level LRU cache. */
        private const val MAX_SESSION_CACHE_SIZE = 5

        /** Maximum DOM elements stored per cached page to bound memory usage. */
        private const val MAX_CACHED_ELEMENTS_PER_PAGE = 200

        /**
         * Active web voice command phrases for the current page.
         *
         * This static flow bridges web-scraped commands to VoiceOSCore's speech engine.
         * The accessibility service collects from this flow and routes phrases to
         * VoiceOSCore.updateWebCommands(), which includes them in the speech grammar
         * alongside static + dynamic + app phrases.
         *
         * Engine-agnostic: phrases flow through ISpeechEngine.updateCommands(),
         * which all engines (Vivoka, VOSK, Android STT, Whisper) implement identically.
         *
         * Browser-scoped: The accessibility service clears this flow when the
         * foreground package changes away from the browser.
         */
        private val _activeWebPhrases = MutableStateFlow<List<String>>(emptyList())
        val activeWebPhrases: StateFlow<List<String>> = _activeWebPhrases.asStateFlow()

        /**
         * Clear active web phrases (called when leaving the browser or on package change).
         */
        fun clearActiveWebPhrases() {
            _activeWebPhrases.value = emptyList()
        }

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
