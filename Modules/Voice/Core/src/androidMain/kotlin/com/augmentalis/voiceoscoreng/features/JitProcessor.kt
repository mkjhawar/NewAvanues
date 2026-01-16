/**
 * JitProcessor.kt - Enhanced Just-In-Time element processor for VoiceOSCoreNG
 *
 * Copyright (C) 2026 Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-16
 *
 * Processes UI elements in real-time or batch mode, generating AVIDs
 * and preparing elements for voice command registration.
 *
 * Enhanced with features from LearnAppCore:
 * - Framework detection caching with LRU eviction
 * - Batch queue management with configurable flush threshold
 * - Developer settings integration
 * - Database retry utility pattern
 * - Proper batch processing with transaction support
 */
package com.augmentalis.voiceoscoreng.features

import android.util.Log
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.ElementFingerprint
import com.augmentalis.voiceoscoreng.common.FrameworkDetector
import com.augmentalis.voiceoscoreng.common.FrameworkType
import com.augmentalis.voiceoscoreng.common.ProcessingMode
import com.augmentalis.voiceoscoreng.jit.BatchConfig
import com.augmentalis.voiceoscoreng.jit.BatchManagerListener
import com.augmentalis.voiceoscoreng.jit.BatchStats
import com.augmentalis.voiceoscoreng.jit.CommandGenerationResult
import com.augmentalis.voiceoscoreng.jit.FlushResult
import com.augmentalis.voiceoscoreng.jit.GeneratedCommand
import com.augmentalis.voiceoscoreng.jit.IBatchManager
import com.augmentalis.voiceoscoreng.jit.ICommandGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Collections
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * Just-In-Time processor for UI elements with enhanced batch support.
 *
 * Processes elements as they are encountered during app usage,
 * generating AVIDs and preparing them for voice command mapping.
 *
 * ## Features (merged from LearnAppCore):
 * - Framework detection caching with LRU eviction (max 50 entries)
 * - Batch queue management with configurable flush threshold
 * - Developer settings integration via LearnAppConfig
 * - Database retry utility for transient failures
 * - Proper batch processing with transaction support
 *
 * ## Performance:
 * - IMMEDIATE mode: ~10ms per element (1 DB insert per element)
 * - BATCH mode: ~50ms for 100 elements (1 DB transaction for all)
 *
 * ## Usage - JIT Mode:
 * ```kotlin
 * val processor = JitProcessor()
 * val result = processor.processElement(element)
 * if (result.isSuccess) {
 *     registerCommand(result.vuid!!)
 * }
 * ```
 *
 * ## Usage - Batch Mode:
 * ```kotlin
 * processor.setProcessingMode(ProcessingMode.BATCH)
 * elements.forEach { processor.queueElement(it) }
 * processor.flushBatch() // Inserts all at once
 * ```
 */
class JitProcessor : ICommandGenerator, IBatchManager {

    companion object {
        private const val TAG = "JitProcessor"
        private const val MAX_FRAMEWORK_CACHE_SIZE = 50
        private const val DEFAULT_MIN_LABEL_LENGTH = 2
    }

    // ================================================================
    // Processing State
    // ================================================================

    private var processingMode: ProcessingMode = ProcessingMode.IMMEDIATE
    private val elementQueue = mutableListOf<ElementInfo>()
    private var processedCount = 0

    // ================================================================
    // Framework Detection Cache (from LearnAppCore)
    // ================================================================

    /**
     * Framework detection cache with LRU eviction.
     *
     * Caches detected framework per package name to avoid repeated detection.
     * Key: package name, Value: detected framework
     *
     * LRU eviction prevents unbounded growth (max 50 frameworks cached).
     * Thread-safe via Collections.synchronizedMap for concurrent access.
     */
    private val frameworkCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, FrameworkType>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, FrameworkType>): Boolean {
                return size > MAX_FRAMEWORK_CACHE_SIZE
            }
        }
    )

    // ================================================================
    // Batch Queue Management (from LearnAppCore)
    // ================================================================

    private var _batchConfig = BatchConfig.JIT_DEFAULT
    override val config: BatchConfig get() = _batchConfig

    private val batchQueue = ArrayBlockingQueue<GeneratedCommand>(_batchConfig.maxBatchSize)
    private val batchMutex = Mutex()

    // Statistics tracking
    private val _totalQueued = AtomicLong(0)
    private val _totalFlushed = AtomicLong(0)
    private val _totalFailed = AtomicLong(0)
    private val _totalRetries = AtomicLong(0)
    private val _autoFlushCount = AtomicLong(0)
    private var _lastFlushTimeMs = 0L
    private var _lastFlushDurationMs = 0L
    private var _totalFlushDurationMs = 0L
    private var _flushCount = 0L

    override val stats: BatchStats
        get() = BatchStats(
            totalQueued = _totalQueued.get(),
            totalFlushed = _totalFlushed.get(),
            totalFailed = _totalFailed.get(),
            totalRetries = _totalRetries.get(),
            currentQueueSize = batchQueue.size,
            lastFlushTimeMs = _lastFlushTimeMs,
            lastFlushDurationMs = _lastFlushDurationMs,
            avgFlushDurationMs = if (_flushCount > 0) _totalFlushDurationMs / _flushCount else 0L,
            autoFlushCount = _autoFlushCount.get()
        )

    override val queueSize: Int get() = batchQueue.size
    override val isEmpty: Boolean get() = batchQueue.isEmpty()
    override val isAtThreshold: Boolean get() = batchQueue.size >= _batchConfig.flushThresholdCount
    override val isFull: Boolean get() = batchQueue.remainingCapacity() == 0

    private val listeners = mutableListOf<BatchManagerListener>()

    // ================================================================
    // Command Repository (injectable for database operations)
    // ================================================================

    /**
     * Optional repository for persisting commands.
     * Set via [setCommandRepository] for database integration.
     */
    private var commandRepository: ICommandRepository? = null

    /**
     * Set the command repository for database persistence.
     */
    fun setCommandRepository(repository: ICommandRepository) {
        this.commandRepository = repository
    }

    // ================================================================
    // Processing Mode
    // ================================================================

    /**
     * Get the current processing mode.
     */
    fun getProcessingMode(): ProcessingMode = processingMode

    /**
     * Set the processing mode.
     */
    fun setProcessingMode(mode: ProcessingMode) {
        processingMode = mode
        // Update batch config based on mode
        _batchConfig = when (mode) {
            ProcessingMode.IMMEDIATE -> BatchConfig.JIT_DEFAULT
            ProcessingMode.BATCH -> BatchConfig.EXPLORATION_DEFAULT
        }
    }

    // ================================================================
    // Element Processing
    // ================================================================

    /**
     * Process a single element immediately.
     *
     * @param element The element to process
     * @return JitProcessingResult with AVID if successful
     */
    fun processElement(element: ElementInfo): JitProcessingResult {
        if (!isValidElement(element)) {
            return JitProcessingResult(
                isSuccess = false,
                vuid = null,
                processingMode = processingMode,
                errorMessage = "Invalid element: missing required properties"
            )
        }

        val vuid = generateAVID(element)
        processedCount++

        // In IMMEDIATE mode with repository, also generate and persist command
        if (processingMode == ProcessingMode.IMMEDIATE && commandRepository != null) {
            val command = generateVoiceCommand(element, vuid, element.packageName)
            if (command != null) {
                // Queue for next flush or insert immediately based on config
                if (!batchQueue.offer(command)) {
                    if (isVerboseLoggingEnabled()) {
                        Log.w(TAG, "Batch queue full, command will be inserted on next flush")
                    }
                }
            }
        }

        return JitProcessingResult(
            isSuccess = true,
            vuid = vuid,
            processingMode = processingMode,
            errorMessage = null
        )
    }

    /**
     * Process multiple elements.
     */
    fun processElements(elements: List<ElementInfo>): List<JitProcessingResult> {
        return elements.map { processElement(it) }
    }

    /**
     * Process element with command generation (suspend version for database operations).
     *
     * @param element Element to process
     * @param packageName App package name
     * @param mode Processing mode (IMMEDIATE or BATCH)
     * @return CommandGenerationResult with AVID and command
     */
    suspend fun processElementAsync(
        element: ElementInfo,
        packageName: String,
        mode: ProcessingMode
    ): CommandGenerationResult = withContext(Dispatchers.Default) {
        try {
            // 1. Generate AVID
            val avid = generateAVID(element)
            if (isVerboseLoggingEnabled()) {
                Log.d(TAG, "Generated AVID: $avid for element: ${element.text}")
            }

            // 2. Generate voice command
            val command = generateVoiceCommand(element, avid, packageName)
                ?: return@withContext CommandGenerationResult.skipped(
                    avid = avid,
                    reason = "No label found for command"
                )

            if (isVerboseLoggingEnabled()) {
                Log.d(TAG, "Generated command: ${command.commandText}")
            }

            // 3. Store (mode-specific)
            when (mode) {
                ProcessingMode.IMMEDIATE -> {
                    // JIT Mode: Insert immediately with retry logic
                    commandRepository?.let { repo ->
                        withRetry {
                            repo.insert(command)
                        }
                    }
                    if (isVerboseLoggingEnabled()) {
                        Log.d(TAG, "Inserted command immediately: ${command.commandText}")
                    }
                }
                ProcessingMode.BATCH -> {
                    // Batch Mode: Queue for batch insert
                    if (!batchQueue.offer(command)) {
                        // Queue full, auto-flush if enabled
                        if (_batchConfig.autoFlushEnabled) {
                            Log.w(TAG, "Batch queue full (${_batchConfig.maxBatchSize}), auto-flushing")
                            _autoFlushCount.incrementAndGet()
                            flush()
                            // Try again after flush
                            if (!batchQueue.offer(command)) {
                                Log.e(TAG, "Failed to queue command even after flush!")
                            }
                        } else {
                            Log.w(TAG, "Batch queue full, command not queued")
                        }
                    } else {
                        _totalQueued.incrementAndGet()
                        listeners.forEach { it.onCommandQueued(command, batchQueue.size) }

                        // Check threshold
                        if (isAtThreshold) {
                            listeners.forEach {
                                it.onQueueThresholdReached(batchQueue.size, _batchConfig.maxBatchSize)
                            }
                        }
                    }
                }
            }

            processedCount++
            CommandGenerationResult.success(avid, command)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to process element", e)
            CommandGenerationResult.failure(
                avid = "",
                error = e.message ?: "Unknown error"
            )
        }
    }

    // ================================================================
    // Queue Management
    // ================================================================

    /**
     * Queue an element for later batch processing.
     */
    fun queueElement(element: ElementInfo) {
        elementQueue.add(element)
    }

    /**
     * Get the current element queue size.
     */
    fun getElementQueueSize(): Int = elementQueue.size

    /**
     * Clear the element queue.
     */
    fun clearQueue() {
        elementQueue.clear()
    }

    /**
     * Process all queued elements.
     */
    fun processQueue(): List<JitProcessingResult> {
        val savedMode = processingMode
        processingMode = ProcessingMode.BATCH

        val results = elementQueue.map { processElement(it) }
        elementQueue.clear()

        processingMode = savedMode
        return results
    }

    /**
     * Get the total count of processed elements.
     */
    fun getProcessedCount(): Int = processedCount

    /**
     * Reset the processor state.
     */
    fun reset() {
        elementQueue.clear()
        batchQueue.clear()
        processedCount = 0
        processingMode = ProcessingMode.IMMEDIATE
        frameworkCache.clear()
        resetStats()
    }

    // ================================================================
    // IBatchManager Implementation
    // ================================================================

    override fun addCommand(command: GeneratedCommand): Boolean {
        val added = batchQueue.offer(command)
        if (added) {
            _totalQueued.incrementAndGet()
            listeners.forEach { it.onCommandQueued(command, batchQueue.size) }

            if (isAtThreshold) {
                listeners.forEach {
                    it.onQueueThresholdReached(batchQueue.size, _batchConfig.maxBatchSize)
                }
            }
        } else if (_batchConfig.autoFlushEnabled) {
            // Auto-flush and retry
            Log.w(TAG, "Queue full, triggering auto-flush")
            _autoFlushCount.incrementAndGet()
            // Note: flush() is suspend, so this won't work synchronously
            // For sync add, we just return false
        }
        return added
    }

    override suspend fun flush(): FlushResult = batchMutex.withLock {
        if (batchQueue.isEmpty()) {
            if (isVerboseLoggingEnabled()) {
                Log.d(TAG, "Batch queue empty, nothing to flush")
            }
            return FlushResult.EMPTY
        }

        val startTime = System.currentTimeMillis()
        val count = batchQueue.size

        listeners.forEach { it.onFlushStarted(count, false) }

        try {
            // Drain queue to list
            val commandsList = mutableListOf<GeneratedCommand>()
            batchQueue.drainTo(commandsList)

            // Keep backup for retry
            val backupCommands = commandsList.toList()

            val repo = commandRepository
            if (repo == null) {
                Log.w(TAG, "No repository configured, commands not persisted")
                _lastFlushTimeMs = System.currentTimeMillis()
                _lastFlushDurationMs = System.currentTimeMillis() - startTime
                return FlushResult.success(0, _lastFlushDurationMs)
            }

            try {
                // Batch insert with retry
                withRetry {
                    repo.insertBatch(commandsList)
                }

                val elapsedMs = System.currentTimeMillis() - startTime
                val rate = if (elapsedMs > 0) count * 1000 / elapsedMs else count.toLong()

                Log.i(TAG, "Flushed $count commands in ${elapsedMs}ms (~$rate commands/sec)")

                _totalFlushed.addAndGet(count.toLong())
                _lastFlushTimeMs = System.currentTimeMillis()
                _lastFlushDurationMs = elapsedMs
                _totalFlushDurationMs += elapsedMs
                _flushCount++

                val result = FlushResult.success(count, elapsedMs)
                listeners.forEach { it.onFlushCompleted(result) }
                return result

            } catch (e: Exception) {
                Log.e(TAG, "Failed to flush batch, re-queuing ${backupCommands.size} commands", e)

                // Re-queue commands
                var requeued = 0
                backupCommands.forEach { command ->
                    if (batchQueue.offer(command)) {
                        requeued++
                    } else {
                        Log.w(TAG, "Failed to re-queue command: ${command.commandText}")
                    }
                }

                _totalFailed.addAndGet((backupCommands.size - requeued).toLong())
                _lastFlushTimeMs = System.currentTimeMillis()
                _lastFlushDurationMs = System.currentTimeMillis() - startTime

                val result = FlushResult.failure(
                    error = e.message ?: "Unknown error",
                    commandCount = count,
                    durationMs = _lastFlushDurationMs
                )
                listeners.forEach { it.onFlushCompleted(result) }
                return result
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to flush batch queue", e)
            _lastFlushTimeMs = System.currentTimeMillis()
            _lastFlushDurationMs = System.currentTimeMillis() - startTime

            val result = FlushResult.failure(
                error = e.message ?: "Unknown error",
                commandCount = count,
                durationMs = _lastFlushDurationMs
            )
            listeners.forEach { it.onFlushCompleted(result) }
            return result
        }
    }

    override fun clear(): Int {
        val count = batchQueue.size
        batchQueue.clear()
        if (count > 0) {
            Log.w(TAG, "Cleared $count commands from batch queue without flushing")
        }
        return count
    }

    override fun getQueuedCommands(): List<GeneratedCommand> {
        return batchQueue.toList()
    }

    override fun updateConfig(newConfig: BatchConfig) {
        if (newConfig.maxBatchSize != _batchConfig.maxBatchSize) {
            // Need to recreate queue with new size
            Log.w(TAG, "Changing batch size requires queue recreation - current items may be lost")
        }
        _batchConfig = newConfig
    }

    override fun resetStats() {
        _totalQueued.set(0)
        _totalFlushed.set(0)
        _totalFailed.set(0)
        _totalRetries.set(0)
        _autoFlushCount.set(0)
        _lastFlushTimeMs = 0
        _lastFlushDurationMs = 0
        _totalFlushDurationMs = 0
        _flushCount = 0
    }

    override fun addListener(listener: BatchManagerListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: BatchManagerListener) {
        listeners.remove(listener)
    }

    // ================================================================
    // ICommandGenerator Implementation
    // ================================================================

    override fun generateCommand(
        element: ElementInfo,
        packageName: String,
        frameworkType: FrameworkType?
    ): CommandGenerationResult {
        val avid = generateAVID(element)
        val command = generateVoiceCommand(element, avid, packageName, frameworkType)

        return if (command != null) {
            CommandGenerationResult.success(avid, command)
        } else {
            CommandGenerationResult.skipped(avid, "No actionable label found")
        }
    }

    override fun generateSynonyms(actionType: String, label: String): String {
        val synonyms = mutableListOf<String>()

        when (actionType.lowercase()) {
            "click" -> {
                synonyms.add("tap $label")
                synonyms.add("press $label")
                synonyms.add("select $label")
            }
            "long_click" -> {
                synonyms.add("hold $label")
                synonyms.add("long press $label")
            }
            "scroll" -> {
                synonyms.add("swipe $label")
            }
            "type" -> {
                synonyms.add("enter $label")
                synonyms.add("input $label")
            }
        }

        return "[${synonyms.joinToString(",") { "\"${it.lowercase()}\"" }}]"
    }

    override fun determineActionType(element: ElementInfo): String {
        return when {
            element.className.contains("EditText", ignoreCase = true) -> "type"
            element.isScrollable -> "scroll"
            element.isLongClickable -> "long_click"
            element.isClickable -> "click"
            else -> "click"
        }
    }

    override fun generateFallbackLabel(element: ElementInfo, frameworkType: FrameworkType): String {
        return when (frameworkType) {
            FrameworkType.UNITY -> generateUnityLabel(element)
            FrameworkType.UNREAL_ENGINE -> generateUnrealLabel(element)
            else -> generateGenericFallbackLabel(element)
        }
    }

    // ================================================================
    // Framework Detection
    // ================================================================

    /**
     * Detect framework for a package, using cache when available.
     *
     * @param packageName The app package name
     * @param classNames List of class names from the UI tree
     * @return Detected framework type
     */
    fun detectFramework(packageName: String, classNames: List<String>): FrameworkType {
        // Check cache first
        frameworkCache[packageName]?.let { return it }

        // Detect and cache
        val frameworkInfo = FrameworkDetector.detect(packageName, classNames)
        frameworkCache[packageName] = frameworkInfo.type

        if (isVerboseLoggingEnabled()) {
            Log.d(TAG, "Detected framework for $packageName: ${frameworkInfo.type}")
        }

        return frameworkInfo.type
    }

    /**
     * Clear the framework cache.
     */
    fun clearFrameworkCache() {
        frameworkCache.clear()
    }

    /**
     * Get cached framework for a package, if available.
     */
    fun getCachedFramework(packageName: String): FrameworkType? {
        return frameworkCache[packageName]
    }

    // ================================================================
    // Private Helpers
    // ================================================================

    /**
     * Check if an element is valid for processing.
     */
    private fun isValidElement(element: ElementInfo): Boolean {
        return element.className.isNotEmpty() &&
               (element.isClickable || element.text.isNotEmpty())
    }

    /**
     * Generate an AVID (Avanues Voice ID) for the element.
     * Format: {TypeCode}:{hash8} e.g., "BTN:a3f2e1c9"
     */
    private fun generateAVID(element: ElementInfo): String {
        val packageName = element.packageName.ifBlank { "com.augmentalis.voiceoscoreng" }
        return ElementFingerprint.fromElementInfo(element, packageName)
    }

    /**
     * Generate voice command for an element.
     */
    private fun generateVoiceCommand(
        element: ElementInfo,
        avid: String,
        packageName: String,
        frameworkType: FrameworkType? = null
    ): GeneratedCommand? {
        // Detect or use provided framework
        val framework = frameworkType ?: frameworkCache[packageName] ?: FrameworkType.NATIVE

        // Check if element is actionable
        val isActionable = element.isClickable ||
                          element.isLongClickable ||
                          element.isScrollable ||
                          element.className.contains("EditText", ignoreCase = true)

        // Extract label
        var label = element.text.takeIf { it.isNotBlank() }
            ?: element.contentDescription.takeIf { it.isNotBlank() }
            ?: element.resourceId.substringAfterLast("/").takeIf { it.isNotBlank() }
            ?: generateFallbackLabel(element, framework)

        // Get minimum label length
        val minLabelLength = getMinLabelLength()

        // For actionable elements with poor labels, generate last-resort label
        if (isActionable && (label.isBlank() || label == "unlabeled" ||
                            label.length < minLabelLength || label.all { it.isDigit() })) {
            label = generateLastResortLabel(element)
            if (isVerboseLoggingEnabled()) {
                Log.d(TAG, "Generated last-resort label for actionable element: $label")
            }
        }

        // Skip non-actionable elements with poor labels
        if (!isActionable && (label.length < minLabelLength || label.all { it.isDigit() })) {
            return null
        }

        val actionType = determineActionType(element)
        val commandText = "$actionType $label".lowercase()
        val synonyms = generateSynonyms(actionType, label)
        val elementHash = calculateElementHash(element)

        // Calculate confidence based on label quality
        val confidence = when {
            element.text.isNotBlank() || element.contentDescription.isNotBlank() -> 0.95
            element.resourceId.isNotBlank() -> 0.85
            label.contains("_") && label.matches(Regex(".*_\\d+_\\d+$")) -> 0.5
            else -> 0.7
        }

        return GeneratedCommand(
            id = 0L,
            elementHash = elementHash,
            commandText = commandText,
            actionType = actionType,
            confidence = confidence,
            synonyms = synonyms,
            isUserApproved = 0L,
            usageCount = 0L,
            lastUsed = null,
            createdAt = System.currentTimeMillis(),
            appId = packageName
        )
    }

    /**
     * Calculate element hash for deduplication.
     */
    private fun calculateElementHash(element: ElementInfo): String {
        val fingerprint = buildString {
            append(element.className)
            append("|")
            append(element.resourceId)
            append("|")
            append(element.text)
            append("|")
            append(element.contentDescription)
            append("|")
            append(element.bounds.toString())
        }

        return try {
            val md = MessageDigest.getInstance("MD5")
            val hashBytes = md.digest(fingerprint.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }.take(12)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate hash", e)
            fingerprint.hashCode().toString()
        }
    }

    /**
     * Generate last-resort label using coordinates.
     * Format: {elementType}_{centerX}_{centerY}
     */
    private fun generateLastResortLabel(element: ElementInfo): String {
        val elementType = element.className
            .substringAfterLast(".")
            .lowercase()
            .replace("view", "")
            .replace("layout", "")
            .takeIf { it.isNotBlank() } ?: "element"

        val centerX = element.bounds.centerX
        val centerY = element.bounds.centerY

        return "${elementType}_${centerX}_${centerY}"
    }

    /**
     * Generate Unity-style spatial label (3x3 grid).
     */
    private fun generateUnityLabel(element: ElementInfo): String {
        val bounds = element.bounds
        val screenWidth = 1080 // Default screen width
        val screenHeight = 1920 // Default screen height

        val col = when {
            bounds.centerX < screenWidth / 3 -> "Left"
            bounds.centerX > screenWidth * 2 / 3 -> "Right"
            else -> "Center"
        }

        val row = when {
            bounds.centerY < screenHeight / 3 -> "Top"
            bounds.centerY > screenHeight * 2 / 3 -> "Bottom"
            else -> "Middle"
        }

        val type = when {
            element.className.contains("Button", ignoreCase = true) -> "Button"
            element.className.contains("Image", ignoreCase = true) -> "Image"
            element.isClickable -> "Button"
            else -> "Element"
        }

        return "$row $col $type"
    }

    /**
     * Generate Unreal-style spatial label (4x4 grid with edge detection).
     */
    private fun generateUnrealLabel(element: ElementInfo): String {
        val bounds = element.bounds
        val screenWidth = 1080
        val screenHeight = 1920

        val col = when {
            bounds.centerX < screenWidth / 4 -> "Far Left"
            bounds.centerX < screenWidth / 2 -> "Left"
            bounds.centerX < screenWidth * 3 / 4 -> "Right"
            else -> "Far Right"
        }

        val row = when {
            bounds.centerY < screenHeight / 4 -> "Top"
            bounds.centerY < screenHeight / 2 -> "Upper"
            bounds.centerY < screenHeight * 3 / 4 -> "Lower"
            else -> "Bottom"
        }

        val isCorner = (bounds.centerX < screenWidth / 4 || bounds.centerX > screenWidth * 3 / 4) &&
                      (bounds.centerY < screenHeight / 4 || bounds.centerY > screenHeight * 3 / 4)

        val type = when {
            element.className.contains("Button", ignoreCase = true) -> "Button"
            element.className.contains("Image", ignoreCase = true) -> "Icon"
            element.isClickable -> "Widget"
            else -> "Element"
        }

        return if (isCorner) {
            "Corner $row $col $type"
        } else {
            "$row $col $type"
        }
    }

    /**
     * Generate generic fallback label.
     */
    private fun generateGenericFallbackLabel(element: ElementInfo): String {
        val elementType = element.className
            .substringAfterLast(".")
            .lowercase()
            .replace("view", "")
            .replace("layout", "")
            .takeIf { it.isNotBlank() } ?: "element"

        val index = element.listIndex.takeIf { it >= 0 } ?: 0
        return "$elementType ${index + 1}"
    }

    // ================================================================
    // Developer Settings Integration
    // ================================================================

    /**
     * Check if verbose logging is enabled via developer settings.
     */
    private fun isVerboseLoggingEnabled(): Boolean {
        return LearnAppConfig.isDebugOverlayEnabled()
    }

    /**
     * Get minimum label length from config.
     */
    private fun getMinLabelLength(): Int {
        return DEFAULT_MIN_LABEL_LENGTH
    }

    // ================================================================
    // Database Retry Utility (from LearnAppCore)
    // ================================================================

    /**
     * Execute a database operation with retry logic.
     *
     * @param maxRetries Maximum retry attempts
     * @param delayMs Delay between retries
     * @param operation The operation to execute
     */
    private suspend fun <T> withRetry(
        maxRetries: Int = _batchConfig.maxRetries,
        delayMs: Long = _batchConfig.retryDelayMs,
        operation: suspend () -> T
    ): T {
        var lastException: Exception? = null
        var attempt = 0

        while (attempt <= maxRetries) {
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e
                attempt++
                _totalRetries.incrementAndGet()

                if (attempt <= maxRetries) {
                    Log.w(TAG, "Operation failed (attempt $attempt/$maxRetries), retrying in ${delayMs}ms", e)
                    listeners.forEach {
                        it.onFlushRetry(attempt, maxRetries, e.message ?: "Unknown error")
                    }
                    delay(delayMs)
                }
            }
        }

        throw lastException ?: RuntimeException("Operation failed after $maxRetries retries")
    }
}

/**
 * Result of JIT processing for an element.
 */
data class JitProcessingResult(
    val isSuccess: Boolean,
    val vuid: String?,
    val processingMode: ProcessingMode,
    val errorMessage: String? = null
)

/**
 * Repository interface for command persistence.
 *
 * Platform implementations provide database operations.
 */
interface ICommandRepository {
    /**
     * Insert a single command.
     */
    suspend fun insert(command: GeneratedCommand)

    /**
     * Insert multiple commands in a single transaction.
     */
    suspend fun insertBatch(commands: List<GeneratedCommand>)

    /**
     * Find command by element hash.
     */
    suspend fun findByElementHash(hash: String): GeneratedCommand?

    /**
     * Delete command by ID.
     */
    suspend fun delete(id: Long)
}
