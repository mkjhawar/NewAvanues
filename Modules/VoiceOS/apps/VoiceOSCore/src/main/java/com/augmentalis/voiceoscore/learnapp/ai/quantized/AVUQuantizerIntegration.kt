/**
 * AVUQuantizerIntegration.kt - Integration layer for AVU Quantizer
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Provides NLU-optimized context from learned app data.
 * Converts raw exploration data into compact, LLM-friendly representations.
 *
 * Key capabilities:
 * - Generate QuantizedContext from learned app data
 * - Create LLM prompts with configurable formats
 * - Action prediction for voice commands
 * - Context caching for performance
 */
package com.augmentalis.voiceoscore.learnapp.ai.quantized

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.dto.ScreenContextDTO
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.database.repositories.IScreenContextRepository
import com.augmentalis.voiceoscore.accessibility.extractors.ScreenContextualText
import com.augmentalis.voiceoscore.learnapp.ai.LLMPromptFormat
import com.augmentalis.voiceoscore.learnapp.database.repository.LearnAppRepository
import com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * AVU Quantizer Integration
 *
 * Main entry point for quantized context generation and LLM prompt creation.
 * Converts exploration data into compact representations suitable for NLU/LLM consumption.
 *
 * Implements ExplorationDebugCallback to receive exploration events and chain them
 * to other callbacks while processing for quantization.
 *
 * @param context Android context
 * @param screenContextRepository Repository for screen context queries
 * @param generatedCommandRepository Repository for generated command queries
 * @param learnAppRepository Repository for learned app queries
 * @param databaseManager Database manager for direct queries when needed
 */
class AVUQuantizerIntegration(
    private val context: Context,
    private val screenContextRepository: IScreenContextRepository? = null,
    private val generatedCommandRepository: IGeneratedCommandRepository? = null,
    private val learnAppRepository: LearnAppRepository? = null,
    private val databaseManager: VoiceOSDatabaseManager? = null
) : com.augmentalis.voiceoscore.learnapp.exploration.ExplorationDebugCallback {

    companion object {
        private const val TAG = "AVUQuantizerIntegration"
        private const val CACHE_EXPIRY_MS = 5 * 60 * 1000L // 5 minutes
    }

    // Coroutine scope for background operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Cache for quantized contexts
    private val contextCache = ConcurrentHashMap<String, CachedContext>()
    private val cacheMutex = Mutex()

    // Package manager for app info
    private val packageManager: PackageManager = context.packageManager

    /**
     * Get quantized context for an app
     *
     * Returns a compact, NLU-optimized representation of the app's learned UI structure.
     *
     * @param packageName Package name of the learned app
     * @return QuantizedContext if available, null if app not learned
     */
    suspend fun getQuantizedContext(packageName: String): QuantizedContext? = withContext(Dispatchers.IO) {
        // Check cache first
        val cached = getCachedContext(packageName)
        if (cached != null) {
            Log.d(TAG, "Returning cached context for $packageName")
            return@withContext cached
        }

        // Generate new context from learned data
        val quantizedContext = generateQuantizedContext(packageName)

        // Cache the result
        if (quantizedContext != null) {
            cacheContext(packageName, quantizedContext)
        }

        quantizedContext
    }

    /**
     * Check if quantized context exists for an app
     *
     * @param packageName Package name to check
     * @return true if context is available (either cached or can be generated)
     */
    suspend fun hasQuantizedContext(packageName: String): Boolean = withContext(Dispatchers.IO) {
        // Check cache
        if (contextCache.containsKey(packageName)) {
            return@withContext true
        }

        // Check if app has been learned (has exploration data)
        hasLearnedData(packageName)
    }

    /**
     * List all packages with quantized contexts available
     *
     * @return List of package names
     */
    suspend fun listQuantizedPackages(): List<String> = withContext(Dispatchers.IO) {
        // Return packages that have learned data
        getLearnedPackages()
    }

    /**
     * Generate LLM prompt for a user goal
     *
     * Creates a prompt suitable for LLM consumption based on the app's quantized context.
     *
     * @param packageName Package name of the learned app
     * @param userGoal User's goal (e.g., "open settings", "send message")
     * @param format Prompt format (COMPACT, HTML, or FULL)
     * @param contextualText Optional contextual text for current screen (NLU enhancement)
     * @return LLM-ready prompt string, or null if no context available
     */
    suspend fun generateLLMPrompt(
        packageName: String,
        userGoal: String,
        format: LLMPromptFormat,
        contextualText: ScreenContextualText? = null
    ): String? = withContext(Dispatchers.IO) {
        val quantizedContext = getQuantizedContext(packageName) ?: return@withContext null

        when (format) {
            LLMPromptFormat.COMPACT -> generateCompactPrompt(quantizedContext, userGoal, contextualText)
            LLMPromptFormat.HTML -> generateHtmlPrompt(quantizedContext, userGoal, contextualText)
            LLMPromptFormat.FULL -> generateFullPrompt(quantizedContext, userGoal, contextualText)
        }
    }

    /**
     * Generate action prediction prompt for current screen
     *
     * Creates a prompt optimized for predicting the next action based on user intent.
     *
     * @param packageName Package name of the app
     * @param currentScreenHash Hash of the current screen
     * @param userIntent User's intent (e.g., "go back", "tap search")
     * @param contextualText Optional contextual text for current screen (NLU enhancement)
     * @return Action prediction prompt, or null if no context available
     */
    suspend fun generateActionPredictionPrompt(
        packageName: String,
        currentScreenHash: String,
        userIntent: String,
        contextualText: ScreenContextualText? = null
    ): String? = withContext(Dispatchers.IO) {
        val quantizedContext = getQuantizedContext(packageName) ?: return@withContext null
        val currentScreen = quantizedContext.findScreen(currentScreenHash)
            ?: return@withContext null

        buildString {
            appendLine("App: ${quantizedContext.appName}")
            appendLine("Screen: ${currentScreen.screenTitle}")
            appendLine("User wants: $userIntent")

            // NLU Enhancement: Include contextual text for better understanding
            contextualText?.let { ctx ->
                if (ctx.hasContent()) {
                    appendLine()
                    appendLine("Screen Context:")
                    ctx.screenTitle?.let { appendLine("  Title: $it") }
                    if (ctx.breadcrumbs.isNotEmpty()) {
                        appendLine("  Path: ${ctx.breadcrumbs.joinToString(" > ")}")
                    }
                    if (ctx.sectionHeaders.isNotEmpty()) {
                        appendLine("  Sections: ${ctx.sectionHeaders.joinToString(", ")}")
                    }
                }
            }

            appendLine()
            appendLine("Available actions:")

            currentScreen.elements.take(10).forEachIndexed { index, element ->
                appendLine("${index + 1}. [${element.type.name}] ${element.label}")
            }

            appendLine()
            appendLine("Navigation options:")
            quantizedContext.getNavigationFrom(currentScreenHash).take(5).forEach { nav ->
                appendLine("- ${nav.triggerLabel} -> ${nav.toScreenHash.take(8)}")
            }

            appendLine()
            appendLine("Predict the best action to achieve the user's goal.")
        }
    }

    /**
     * Invalidate cached context for an app
     *
     * Call this when app data is updated through new learning.
     *
     * @param packageName Package name to invalidate
     */
    suspend fun invalidateCache(packageName: String) = cacheMutex.withLock {
        contextCache.remove(packageName)
        Log.d(TAG, "Invalidated cache for $packageName")
    }

    /**
     * Clear all cached contexts
     */
    suspend fun clearCache() = cacheMutex.withLock {
        contextCache.clear()
        Log.d(TAG, "Cleared all cached contexts")
    }

    // ============================================
    // Private Implementation
    // ============================================

    private suspend fun getCachedContext(packageName: String): QuantizedContext? = cacheMutex.withLock {
        val cached = contextCache[packageName] ?: return@withLock null

        // Check if cache has expired
        if (System.currentTimeMillis() - cached.timestamp > CACHE_EXPIRY_MS) {
            contextCache.remove(packageName)
            return@withLock null
        }

        cached.context
    }

    private suspend fun cacheContext(packageName: String, quantizedContext: QuantizedContext) = cacheMutex.withLock {
        contextCache[packageName] = CachedContext(
            context = quantizedContext,
            timestamp = System.currentTimeMillis()
        )
    }

    private suspend fun generateQuantizedContext(packageName: String): QuantizedContext? =
        withContext(Dispatchers.IO) {
            try {
                // Get app info
                val appInfo = try {
                    packageManager.getApplicationInfo(packageName, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.w(TAG, "App not found: $packageName")
                    return@withContext null
                }

                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val packageInfo = packageManager.getPackageInfo(packageName, 0)

                // Build quantized context from learned data
                val screens = buildQuantizedScreens(packageName)
                val navigation = buildQuantizedNavigation(packageName, screens)
                val vocabulary = buildVocabulary(screens)
                val commands = buildKnownCommands(packageName)

                QuantizedContext(
                    packageName = packageName,
                    appName = appName,
                    versionCode = packageInfo.longVersionCode,
                    versionName = packageInfo.versionName ?: "unknown",
                    generatedAt = System.currentTimeMillis(),
                    screens = screens,
                    navigation = navigation,
                    vocabulary = vocabulary,
                    knownCommands = commands
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate quantized context for $packageName", e)
                null
            }
        }

    /**
     * Build quantized screens from database
     *
     * Queries screen contexts and their elements for the given package.
     */
    private suspend fun buildQuantizedScreens(packageName: String): List<QuantizedScreen> =
        withContext(Dispatchers.IO) {
            try {
                val repository = screenContextRepository
                if (repository == null) {
                    Log.w(TAG, "No screen context repository available")
                    return@withContext emptyList()
                }

                // Get all screens for this package
                val screens = repository.getByApp(packageName)
                if (screens.isEmpty()) {
                    Log.d(TAG, "No screens found for $packageName")
                    return@withContext emptyList()
                }

                Log.d(TAG, "Found ${screens.size} screens for $packageName")

                // Convert each screen to QuantizedScreen with elements
                screens.map { screen ->
                    val elements = getElementsForScreen(screen.screenHash)
                        .filter { it.isClickable != 0L || it.isEditable != 0L || it.isCheckable != 0L }
                        .map { convertToQuantizedElement(it) }

                    QuantizedScreen(
                        screenHash = screen.screenHash,
                        screenTitle = screen.windowTitle ?: screen.activityName ?: "Unknown",
                        activityName = screen.activityName,
                        elements = elements
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to build quantized screens for $packageName", e)
                emptyList()
            }
        }

    /**
     * Get elements for a specific screen
     *
     * Uses getByScreenHashOnly query which requires only the screen hash.
     */
    private suspend fun getElementsForScreen(screenHash: String): List<ScrapedElementEntity> =
        withContext(Dispatchers.IO) {
            try {
                val db = databaseManager
                if (db == null) {
                    Log.w(TAG, "No database manager available")
                    return@withContext emptyList()
                }

                // Get elements by screen hash using the new query
                db.scrapedElementQueries.getByScreenHashOnly(screenHash).executeAsList().map { row ->
                    ScrapedElementEntity(
                        id = row.id,
                        elementHash = row.elementHash,
                        appId = row.appId,
                        uuid = row.uuid,
                        className = row.className,
                        viewIdResourceName = row.viewIdResourceName,
                        text = row.text,
                        contentDescription = row.contentDescription,
                        bounds = row.bounds,
                        isClickable = row.isClickable,
                        isLongClickable = row.isLongClickable,
                        isEditable = row.isEditable,
                        isScrollable = row.isScrollable,
                        isCheckable = row.isCheckable,
                        isFocusable = row.isFocusable,
                        isEnabled = row.isEnabled,
                        depth = row.depth,
                        indexInParent = row.indexInParent,
                        scrapedAt = row.scrapedAt,
                        semanticRole = row.semanticRole,
                        inputType = row.inputType,
                        visualWeight = row.visualWeight,
                        isRequired = row.isRequired ?: 0L,
                        formGroupId = row.formGroupId,
                        placeholderText = row.placeholderText,
                        validationPattern = row.validationPattern,
                        backgroundColor = row.backgroundColor,
                        screen_hash = row.screen_hash
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get elements for screen $screenHash", e)
                emptyList()
            }
        }

    /**
     * Convert ScrapedElementEntity to QuantizedElement
     */
    private fun convertToQuantizedElement(entity: ScrapedElementEntity): QuantizedElement {
        return QuantizedElement(
            vuid = entity.uuid ?: entity.elementHash,
            type = classifyElementType(entity.className),
            label = entity.text
                ?: entity.contentDescription
                ?: entity.viewIdResourceName?.substringAfterLast("/")
                ?: "unlabeled",
            aliases = buildAliases(entity)
        )
    }

    /**
     * Build aliases list from element properties
     */
    private fun buildAliases(entity: ScrapedElementEntity): List<String> {
        return listOfNotNull(
            entity.text,
            entity.contentDescription,
            entity.viewIdResourceName?.substringAfterLast("/")
        ).distinct().filter { it.isNotBlank() }.take(3)
    }

    /**
     * Classify element type from Android className
     */
    private fun classifyElementType(className: String?): ElementType {
        if (className == null) return ElementType.OTHER

        return when {
            className.contains("Button", ignoreCase = true) -> ElementType.BUTTON
            className.contains("ImageButton", ignoreCase = true) -> ElementType.BUTTON
            className.contains("EditText", ignoreCase = true) -> ElementType.TEXT_FIELD
            className.contains("TextInput", ignoreCase = true) -> ElementType.TEXT_FIELD
            className.contains("AutoComplete", ignoreCase = true) -> ElementType.TEXT_FIELD
            className.contains("CheckBox", ignoreCase = true) -> ElementType.CHECKBOX
            className.contains("Switch", ignoreCase = true) -> ElementType.SWITCH
            className.contains("Toggle", ignoreCase = true) -> ElementType.SWITCH
            className.contains("Spinner", ignoreCase = true) -> ElementType.DROPDOWN
            className.contains("DropDown", ignoreCase = true) -> ElementType.DROPDOWN
            className.contains("Tab", ignoreCase = true) -> ElementType.TAB
            else -> ElementType.OTHER
        }
    }

    /**
     * Build quantized navigation from screen transitions
     *
     * Note: ScreenTransition table doesn't have package filtering, so we
     * filter by screen hashes that belong to this package's screens.
     */
    private suspend fun buildQuantizedNavigation(
        packageName: String,
        screens: List<QuantizedScreen>
    ): List<QuantizedNavigation> = withContext(Dispatchers.IO) {
        try {
            val db = databaseManager
            if (db == null) {
                Log.w(TAG, "No database manager available for navigation")
                return@withContext emptyList()
            }

            val screenHashes = screens.map { it.screenHash }.toSet()
            if (screenHashes.isEmpty()) {
                return@withContext emptyList()
            }

            // Get all transitions and filter to those between our package's screens
            db.screenTransitionQueries.getAll().executeAsList()
                .filter { it.fromScreenHash in screenHashes && it.toScreenHash in screenHashes }
                .map { transition ->
                    // Get trigger element label from ScrapedElement if we have the hash
                    val triggerLabel = transition.triggerElementHash?.let { hash ->
                        try {
                            db.scrapedElementQueries.getByHash(hash).executeAsOneOrNull()?.let {
                                it.text ?: it.contentDescription ?: "action"
                            }
                        } catch (e: Exception) {
                            null
                        }
                    } ?: transition.triggerAction

                    QuantizedNavigation(
                        fromScreenHash = transition.fromScreenHash,
                        toScreenHash = transition.toScreenHash,
                        triggerLabel = triggerLabel,
                        triggerVuid = transition.triggerElementHash ?: ""
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build navigation for $packageName", e)
            emptyList()
        }
    }

    private fun buildVocabulary(screens: List<QuantizedScreen>): Set<String> {
        // Extract unique element labels for vocabulary
        return screens.flatMap { screen ->
            screen.elements.flatMap { element ->
                listOf(element.label) + element.aliases
            }
        }.filter { it.isNotBlank() }.toSet()
    }

    /**
     * Build known commands from generated commands
     */
    private suspend fun buildKnownCommands(packageName: String): List<QuantizedCommand> =
        withContext(Dispatchers.IO) {
            try {
                val repository = generatedCommandRepository
                if (repository == null) {
                    Log.w(TAG, "No generated command repository available")
                    return@withContext emptyList()
                }

                // Get all commands and filter by package (through element hash lookup)
                // Since commands are linked to elements, we get all high-confidence commands
                repository.getHighConfidence(0.5)
                    .filter { it.isUserApproved == 1L || it.confidence >= 0.7 }
                    .sortedByDescending { it.usageCount }
                    .take(100) // Limit to top 100 commands
                    .map { cmd ->
                        QuantizedCommand(
                            phrase = cmd.commandText,
                            actionType = parseActionType(cmd.actionType),
                            targetVuid = cmd.elementHash,
                            confidence = cmd.confidence.toFloat()
                        )
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to build commands for $packageName", e)
                emptyList()
            }
        }

    /**
     * Parse action type string to enum
     */
    private fun parseActionType(actionType: String): CommandActionType {
        return try {
            CommandActionType.valueOf(actionType.uppercase())
        } catch (e: Exception) {
            CommandActionType.CLICK // Default fallback
        }
    }

    /**
     * Check if learned data exists for package
     */
    private suspend fun hasLearnedData(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val repository = learnAppRepository
            if (repository == null) {
                Log.w(TAG, "No learn app repository available")
                return@withContext false
            }

            repository.isAppLearned(packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check learned data for $packageName", e)
            false
        }
    }

    /**
     * Get all learned packages
     */
    private suspend fun getLearnedPackages(): List<String> = withContext(Dispatchers.IO) {
        try {
            val repository = learnAppRepository
            if (repository == null) {
                Log.w(TAG, "No learn app repository available")
                return@withContext emptyList()
            }

            repository.getAllLearnedApps().map { it.packageName }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get learned packages", e)
            emptyList()
        }
    }

    /**
     * Called when exploration completes for a package
     * Invalidates cache to ensure fresh data on next query
     */
    fun onExplorationCompleted(packageName: String) {
        scope.launch {
            invalidateCache(packageName)
            Log.d(TAG, "Cache invalidated for $packageName after exploration")
        }
    }

    // ============================================
    // Prompt Generation
    // ============================================

    private fun generateCompactPrompt(
        context: QuantizedContext,
        userGoal: String,
        contextualText: ScreenContextualText? = null
    ): String {
        return buildString {
            appendLine("App: ${context.appName}")
            appendLine("Goal: $userGoal")

            // NLU Enhancement: Include contextual text in compact format
            contextualText?.let { ctx ->
                if (ctx.hasContent()) {
                    appendLine("Context: ${ctx.toCompactString()}")
                }
            }

            appendLine("Screens: ${context.screens.size}")

            val relevantScreens = context.findScreensWithElement(userGoal)
            if (relevantScreens.isNotEmpty()) {
                appendLine("Relevant: ${relevantScreens.joinToString { it.screenTitle }}")
            }

            appendLine("Commands: ${context.vocabulary.take(10).joinToString()}")
        }
    }

    private fun generateHtmlPrompt(
        context: QuantizedContext,
        userGoal: String,
        contextualText: ScreenContextualText? = null
    ): String {
        return buildString {
            appendLine("<app name=\"${context.appName}\" pkg=\"${context.packageName}\">")
            appendLine("  <goal>$userGoal</goal>")

            // NLU Enhancement: Include contextual text as XML element
            contextualText?.let { ctx ->
                if (ctx.hasContent()) {
                    appendLine("  <context>")
                    ctx.screenTitle?.let { appendLine("    <title>$it</title>") }
                    if (ctx.breadcrumbs.isNotEmpty()) {
                        appendLine("    <path>${ctx.breadcrumbs.joinToString(" > ")}</path>")
                    }
                    if (ctx.sectionHeaders.isNotEmpty()) {
                        appendLine("    <sections>${ctx.sectionHeaders.joinToString(", ")}</sections>")
                    }
                    appendLine("  </context>")
                }
            }

            appendLine("  <screens count=\"${context.screens.size}\">")

            context.screens.take(5).forEach { screen ->
                appendLine("    <screen title=\"${screen.screenTitle}\">")
                screen.elements.take(5).forEach { element ->
                    appendLine("      <${element.type.name.lowercase()} label=\"${element.label}\"/>")
                }
                appendLine("    </screen>")
            }

            appendLine("  </screens>")
            appendLine("  <nav count=\"${context.navigation.size}\"/>")
            appendLine("</app>")
        }
    }

    private fun generateFullPrompt(
        context: QuantizedContext,
        userGoal: String,
        contextualText: ScreenContextualText? = null
    ): String {
        return buildString {
            appendLine("# Application Context")
            appendLine("- App: ${context.appName}")
            appendLine("- Package: ${context.packageName}")
            appendLine("- Version: ${context.versionName} (${context.versionCode})")
            appendLine()
            appendLine("## User Goal")
            appendLine(userGoal)
            appendLine()

            // NLU Enhancement: Current Screen Context section
            contextualText?.let { ctx ->
                if (ctx.hasContent()) {
                    appendLine("## Current Screen Context")
                    ctx.screenTitle?.let { appendLine("**Screen Title**: $it") }
                    if (ctx.breadcrumbs.isNotEmpty()) {
                        appendLine("**Navigation Path**: ${ctx.breadcrumbs.joinToString(" > ")}")
                    }
                    if (ctx.sectionHeaders.isNotEmpty()) {
                        appendLine("**Visible Sections**: ${ctx.sectionHeaders.joinToString(", ")}")
                    }
                    if (ctx.visibleLabels.isNotEmpty()) {
                        appendLine("**Context Labels**: ${ctx.visibleLabels.take(5).joinToString("; ")}")
                    }
                    appendLine("**Estimated Tokens**: ~${ctx.estimateTokenCount()}")
                    appendLine()
                }
            }

            appendLine("## Available Screens (${context.screens.size})")

            context.screens.forEach { screen ->
                appendLine()
                appendLine("### ${screen.screenTitle}")
                appendLine("Hash: ${screen.screenHash}")
                if (screen.activityName != null) {
                    appendLine("Activity: ${screen.activityName}")
                }
                appendLine()
                appendLine("Elements:")

                screen.elements.forEach { element ->
                    append("- [${element.type.name}] ${element.label}")
                    if (element.aliases.isNotEmpty()) {
                        append(" (aliases: ${element.aliases.joinToString()})")
                    }
                    appendLine()
                }
            }

            appendLine()
            appendLine("## Navigation Graph")
            context.navigation.forEach { nav ->
                appendLine("- ${nav.triggerLabel}: ${nav.fromScreenHash.take(8)} -> ${nav.toScreenHash.take(8)}")
            }

            appendLine()
            appendLine("## Known Commands")
            context.knownCommands.forEach { cmd ->
                appendLine("- \"${cmd.phrase}\" -> ${cmd.actionType.name}")
            }

            appendLine()
            appendLine("## Vocabulary")
            appendLine(context.vocabulary.joinToString(", "))
        }
    }

    /**
     * Cached context wrapper
     */
    private data class CachedContext(
        val context: QuantizedContext,
        val timestamp: Long
    )

    // ============================================
    // ExplorationDebugCallback Integration
    // ============================================

    private var chainedCallback: com.augmentalis.voiceoscore.learnapp.exploration.ExplorationDebugCallback? = null
    private var isQuantizing = false
    private var currentPackageName: String? = null
    private var elementsProcessed = 0
    private var screensProcessed = 0
    private var actionCandidates = 0

    /**
     * Set chained debug callback
     *
     * When set, this callback receives all exploration events after quantization processing.
     *
     * @param callback The callback to chain, or null to clear
     */
    fun setChainedCallback(callback: com.augmentalis.voiceoscore.learnapp.exploration.ExplorationDebugCallback?) {
        chainedCallback = callback
    }

    /**
     * Start quantization for a package
     *
     * @param packageName Package to quantize
     */
    fun startQuantization(packageName: String) {
        isQuantizing = true
        currentPackageName = packageName
        elementsProcessed = 0
        screensProcessed = 0
        actionCandidates = 0
        Log.d(TAG, "Started quantization for $packageName")
    }

    /**
     * Stop quantization and return the quantized context
     *
     * @return QuantizedContext if available, null otherwise
     */
    fun stopQuantization(): QuantizedContext? {
        if (!isQuantizing || currentPackageName == null) {
            return null
        }

        isQuantizing = false
        val packageName = currentPackageName
        currentPackageName = null

        Log.d(TAG, "Stopped quantization for $packageName - " +
            "$screensProcessed screens, $elementsProcessed elements, $actionCandidates actions")

        // Return cached context if available
        return if (packageName != null) {
            contextCache[packageName]?.context
        } else {
            null
        }
    }

    /**
     * Get quantization statistics
     *
     * @return QuantizerStats with current quantization statistics
     */
    fun getQuantizerStats(): QuantizerStats {
        return QuantizerStats(
            screensProcessed = screensProcessed,
            elementsProcessed = elementsProcessed,
            actionCandidates = actionCandidates
        )
    }

    /**
     * Process accessibility event during exploration
     *
     * Called by exploration engine for each accessibility event.
     *
     * @param event The accessibility event
     */
    fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent) {
        if (!isQuantizing) return

        // Track events for quantization statistics
        when (event.eventType) {
            android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Could process screen changes here
            }
            android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                actionCandidates++
            }
        }
    }

    /**
     * Called when a screen is explored during quantization
     */
    override fun onScreenExplored(
        elements: List<com.augmentalis.voiceoscore.learnapp.models.ElementInfo>,
        screenHash: String,
        activityName: String,
        packageName: String,
        parentScreenHash: String?
    ) {
        if (isQuantizing) {
            screensProcessed++
            elementsProcessed += elements.size
            actionCandidates += elements.count { it.isClickable }
        }

        // Forward to chained callback
        chainedCallback?.onScreenExplored(elements, screenHash, activityName, packageName, parentScreenHash)
    }

    /**
     * Called when navigation occurs during exploration
     */
    override fun onElementNavigated(elementKey: String, destinationScreenHash: String) {
        // Forward to chained callback
        chainedCallback?.onElementNavigated(elementKey, destinationScreenHash)
    }

    /**
     * Called when exploration progress updates
     */
    override fun onProgressUpdated(progress: Int) {
        // Forward to chained callback
        chainedCallback?.onProgressUpdated(progress)
    }

    /**
     * Called when an element is clicked
     */
    override fun onElementClicked(stableId: String, screenHash: String, vuid: String?) {
        // Forward to chained callback
        chainedCallback?.onElementClicked(stableId, screenHash, vuid)
    }

    /**
     * Called when an element is blocked
     */
    override fun onElementBlocked(stableId: String, screenHash: String, reason: String) {
        // Forward to chained callback
        chainedCallback?.onElementBlocked(stableId, screenHash, reason)
    }
}

/**
 * Quantizer Statistics
 *
 * Statistics from the quantization process.
 */
data class QuantizerStats(
    val screensProcessed: Int,
    val elementsProcessed: Int,
    val actionCandidates: Int
)
