/**
 * LearnWebActivity.kt - UI for website learning with Hybrid Smart caching
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnweb/LearnWebActivity.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-13
 *
 * Activity for learning website commands with WebView integration and Hybrid Smart caching
 */

package com.augmentalis.voiceoscore.learnweb

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Learn Web Activity
 *
 * Activity for learning and caching website commands.
 * Implements Hybrid Smart caching with background refresh and hierarchy tracking.
 *
 * Features:
 * - WebView with JavaScript injection
 * - DOM extraction and command generation
 * - 24-hour TTL with stale cache handling
 * - URL change detection
 * - Structure-based invalidation
 *
 * @since 1.0.0
 */
class LearnWebActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LearnWebActivity"
        private const val DEFAULT_URL = "https://www.google.com"
    }

    // UI Components
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView

    // Core Components
    private lateinit var database: WebScrapingDatabase
    private lateinit var scrapingEngine: WebViewScrapingEngine
    private lateinit var commandGenerator: WebCommandGenerator
    private lateinit var cache: WebCommandCache

    // State
    private var currentUrl: String? = null
    private var currentUrlHash: String? = null
    private var isLearning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        initializeComponents()
        setupWebView()

        // Load initial URL
        val initialUrl = intent.getStringExtra("url") ?: DEFAULT_URL
        webView.loadUrl(initialUrl)
    }

    /**
     * Setup UI components
     */
    private fun setupUI() {
        // Note: In production, this should load from a layout XML
        // For this example, we're creating views programmatically

        setContentView(android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL

            // Status text
            statusText = TextView(this@LearnWebActivity).apply {
                text = "Ready to learn website"
                setPadding(16, 16, 16, 16)
            }
            addView(statusText)

            // Progress bar
            progressBar = ProgressBar(
                this@LearnWebActivity,
                null,
                android.R.attr.progressBarStyleHorizontal
            ).apply {
                visibility = View.GONE
            }
            addView(progressBar)

            // WebView
            webView = WebView(this@LearnWebActivity).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }
            addView(webView)
        })
    }

    /**
     * Initialize core components
     */
    private fun initializeComponents() {
        database = WebScrapingDatabase.getInstance(applicationContext)
        scrapingEngine = WebViewScrapingEngine(applicationContext)
        commandGenerator = WebCommandGenerator()
        cache = WebCommandCache(database)
    }

    /**
     * Setup WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                url?.let {
                    Log.d(TAG, "Page loaded: $url")
                    handlePageLoad(it)
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val newUrl = request?.url?.toString() ?: return false

                // Detect URL change
                if (currentUrl != null && currentUrl != newUrl) {
                    lifecycleScope.launch {
                        cache.invalidateByUrlChange(currentUrl!!, newUrl)
                    }
                }

                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress

                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                } else {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Handle page load
     *
     * Checks cache and triggers learning if needed.
     *
     * @param url Loaded URL
     */
    private fun handlePageLoad(url: String) {
        currentUrl = url
        currentUrlHash = cache.hashURL(url)

        lifecycleScope.launch {
            try {
                updateStatus("Checking cache for $url")

                when (val result = cache.getCommands(url)) {
                    is CacheResult.Hit -> {
                        updateStatus("Cache HIT: ${result.commands.size} commands available")
                        showCommands(result.commands)
                    }

                    is CacheResult.Stale -> {
                        updateStatus("Cache STALE: ${result.commands.size} commands (refreshing in background)")
                        showCommands(result.commands)
                        // Background refresh already triggered by cache
                    }

                    is CacheResult.Miss -> {
                        updateStatus("Cache MISS: Learning website...")
                        learnWebsite(url)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle page load", e)
                updateStatus("Error: ${e.message}")
            }
        }
    }

    /**
     * Learn website by scraping and generating commands
     *
     * @param url Website URL
     */
    private suspend fun learnWebsite(url: String) {
        if (isLearning) {
            Log.d(TAG, "Already learning, skipping")
            return
        }

        isLearning = true

        try {
            withContext(Dispatchers.Main) {
                updateStatus("Extracting DOM structure...")
            }

            // Extract DOM
            val elements = scrapingEngine.extractDOMStructure(webView)

            withContext(Dispatchers.Main) {
                updateStatus("Generating commands from ${elements.size} elements...")
            }

            // Generate commands
            val urlHash = cache.hashURL(url)
            val rawCommands = commandGenerator.generateCommands(elements, urlHash)
            val filteredCommands = commandGenerator.filterCommands(rawCommands)

            withContext(Dispatchers.Main) {
                updateStatus("Storing ${filteredCommands.size} commands in cache...")
            }

            // Get page title
            val title = scrapingEngine.getPageTitle(webView)

            // Create website entity
            val structureHash = cache.hashStructure(elements)
            val domain = cache.extractDomain(url)
            val now = System.currentTimeMillis()

            val website = ScrapedWebsite(
                urlHash = urlHash,
                url = url,
                domain = domain,
                title = title,
                structureHash = structureHash,
                parentUrlHash = null, // Set by navigation tracking
                scrapedAt = now,
                lastAccessedAt = now,
                accessCount = 1,
                isStale = false
            )

            // Store in cache
            cache.store(website, elements, filteredCommands)

            withContext(Dispatchers.Main) {
                updateStatus("Learning complete: ${filteredCommands.size} commands cached")
                showCommands(filteredCommands)
            }

            // Show statistics
            val stats = commandGenerator.getStatistics(filteredCommands)
            Log.d(TAG, "Learning stats: $stats")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to learn website", e)
            withContext(Dispatchers.Main) {
                updateStatus("Learning failed: ${e.message}")
                Toast.makeText(this@LearnWebActivity, "Failed to learn website", Toast.LENGTH_SHORT).show()
            }
        } finally {
            isLearning = false
        }
    }

    /**
     * Show commands in UI
     *
     * @param commands List of commands
     */
    private fun showCommands(commands: List<GeneratedWebCommand>) {
        // Group by action
        val grouped = commands.groupBy { it.action }

        Log.d(TAG, "Available commands:")
        grouped.forEach { (action, cmds) ->
            Log.d(TAG, "  $action: ${cmds.size} commands")
            cmds.take(5).forEach { cmd ->
                Log.d(TAG, "    - ${cmd.commandText} [${cmd.synonyms}]")
            }
        }

        // In production, display in a RecyclerView or similar UI
        Toast.makeText(
            this,
            "Loaded ${commands.size} commands for this page",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Update status text
     *
     * @param status Status message
     */
    private fun updateStatus(status: String) {
        statusText.text = status
        Log.d(TAG, status)
    }

    /**
     * Test command execution
     *
     * Example usage of command execution.
     *
     * @param command Command to execute
     */
    private suspend fun executeCommand(command: GeneratedWebCommand) {
        try {
            when (command.action) {
                "CLICK" -> {
                    val success = scrapingEngine.clickElement(webView, command.xpath)
                    if (success) {
                        // Increment usage
                        database.generatedWebCommandDao().incrementUsage(
                            command.id,
                            System.currentTimeMillis()
                        )
                        Log.d(TAG, "Clicked element: ${command.commandText}")
                    } else {
                        Log.e(TAG, "Failed to click element: ${command.commandText}")
                    }
                }

                "SCROLL_TO" -> {
                    val success = scrapingEngine.scrollToElement(webView, command.xpath)
                    if (success) {
                        database.generatedWebCommandDao().incrementUsage(
                            command.id,
                            System.currentTimeMillis()
                        )
                        Log.d(TAG, "Scrolled to element: ${command.commandText}")
                    } else {
                        Log.e(TAG, "Failed to scroll to element: ${command.commandText}")
                    }
                }

                "FOCUS" -> {
                    // Focus implementation (similar to scroll + click)
                    val success = scrapingEngine.scrollToElement(webView, command.xpath)
                    if (success) {
                        database.generatedWebCommandDao().incrementUsage(
                            command.id,
                            System.currentTimeMillis()
                        )
                        Log.d(TAG, "Focused element: ${command.commandText}")
                    }
                }

                else -> {
                    Log.w(TAG, "Unknown action: ${command.action}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command: ${command.commandText}", e)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cache.close()
        webView.destroy()
    }
}

/**
 * Extension function for TextView padding
 */
private fun TextView.padding(padding: Int) {
    val px = (padding * resources.displayMetrics.density).toInt()
    setPadding(px, px, px, px)
}
