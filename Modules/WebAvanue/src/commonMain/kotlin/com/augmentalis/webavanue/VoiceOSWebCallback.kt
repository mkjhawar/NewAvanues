package com.augmentalis.webavanue

/**
 * Callback interface for VoiceOS integration with WebAvanue.
 *
 * This allows the browser to communicate with VoiceOS for:
 * - DOM scraping results (for voice command generation)
 * - Voice command execution
 * - Numbers overlay updates
 */
interface VoiceOSWebCallback {

    /**
     * Called when DOM has been scraped from the current page.
     *
     * @param result The scraped DOM elements and metadata
     */
    fun onDOMScraped(result: DOMScrapeResult)

    /**
     * Called when a page starts loading.
     *
     * @param url The URL being loaded
     */
    fun onPageLoadStarted(url: String)

    /**
     * Called when a page finishes loading.
     *
     * @param url The loaded URL
     * @param title The page title
     */
    fun onPageLoadFinished(url: String, title: String)

    /**
     * Called when DOM content changes (mutation observed).
     */
    fun onDOMContentChanged()

    /**
     * Execute a voice command on the current page.
     *
     * @param command The command to execute
     * @return True if command was executed successfully
     */
    suspend fun executeCommand(command: VoiceCommandGenerator.WebVoiceCommand): Boolean
}

/**
 * No-op implementation for when VoiceOS is not enabled.
 */
object NoOpVoiceOSCallback : VoiceOSWebCallback {
    override fun onDOMScraped(result: DOMScrapeResult) {}
    override fun onPageLoadStarted(url: String) {}
    override fun onPageLoadFinished(url: String, title: String) {}
    override fun onDOMContentChanged() {}
    override suspend fun executeCommand(command: VoiceCommandGenerator.WebVoiceCommand): Boolean = false
}
