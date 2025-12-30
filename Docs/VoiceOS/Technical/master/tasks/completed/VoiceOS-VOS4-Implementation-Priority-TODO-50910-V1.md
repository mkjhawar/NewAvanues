# VOS4 Implementation Priority TODO List

**Document:** VOS4-Implementation-Priority-TODO-251009-0244.md
**Branch:** vos4-legacyintegration
**Created:** 2025-10-09 02:44:00 PDT
**Purpose:** Reorganized priority list with implementation details
**Priority System:** 🔴 CRITICAL | 🟠 HIGH | 🟡 MEDIUM | 🟢 LOW

---

## Executive Summary

**Total Remaining Tasks:** 68 tasks
**Total Estimated Time:** 247 hours (31 working days)

**Priority Breakdown:**
- 🔴 CRITICAL: 12 tasks (63 hours) - Speech recognition core features
- 🟠 HIGH: 35 tasks (122 hours) - Infrastructure and critical stubs
- 🟡 MEDIUM: 17 tasks (54 hours) - Enhancements and polish
- 🟢 LOW: 4 tasks (8 hours) - Optional optimizations

---

## 🔴 CRITICAL PRIORITY - Speech Recognition Core (63 hours)

**Goal:** Achieve feature parity across all speech recognition engines

### Group A: Real-Time Confidence Scoring System (15 hours)

#### CONF-1: Advanced Confidence Scoring Architecture
- **Priority:** 🔴 CRITICAL
- **Estimated Time:** 8 hours
- **Current State:** Only basic SDK confidence number from Vivoka
- **What's Missing:** Advanced confidence system with thresholds, filtering, and user feedback

**Implementation Plan:**

**1. Create Confidence Scoring Framework (3 hours)**
```kotlin
// File: modules/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/confidence/ConfidenceScorer.kt

data class ConfidenceResult(
    val text: String,
    val confidence: Float,  // 0.0 to 1.0
    val alternates: List<Alternate>,
    val scoringMethod: ScoringMethod,
    val timestamp: Long = System.currentTimeMillis()
)

data class Alternate(
    val text: String,
    val confidence: Float,
    val rank: Int
)

enum class ScoringMethod {
    VIVOKA_SDK,      // Direct from Vivoka
    VOSK_ACOUSTIC,   // VOSK acoustic confidence
    GOOGLE_CLOUD,    // Google Cloud confidence
    SIMILARITY,      // Fuzzy match confidence
    COMBINED         // Multiple sources averaged
}

class ConfidenceScorer {
    // Confidence thresholds
    companion object {
        const val THRESHOLD_HIGH = 0.85f      // Execute immediately
        const val THRESHOLD_MEDIUM = 0.70f    // Ask for confirmation
        const val THRESHOLD_LOW = 0.50f       // Suggest alternatives
        // Below LOW = reject
    }

    /**
     * Normalize confidence scores from different engines to 0.0-1.0
     */
    fun normalizeConfidence(
        rawScore: Float,
        engine: RecognitionEngine
    ): Float {
        return when (engine) {
            RecognitionEngine.VIVOKA -> {
                // Vivoka returns 0-100, normalize to 0.0-1.0
                rawScore / 100f
            }
            RecognitionEngine.VOSK -> {
                // VOSK returns acoustic score (negative log likelihood)
                // Convert to 0.0-1.0 range using sigmoid
                1f / (1f + exp(-rawScore))
            }
            RecognitionEngine.GOOGLE -> {
                // Google already returns 0.0-1.0
                rawScore
            }
        }
    }

    /**
     * Determine confidence level for UI feedback
     */
    fun getConfidenceLevel(confidence: Float): ConfidenceLevel {
        return when {
            confidence >= THRESHOLD_HIGH -> ConfidenceLevel.HIGH
            confidence >= THRESHOLD_MEDIUM -> ConfidenceLevel.MEDIUM
            confidence >= THRESHOLD_LOW -> ConfidenceLevel.LOW
            else -> ConfidenceLevel.REJECT
        }
    }

    /**
     * Combine multiple confidence scores (e.g., acoustic + language model)
     */
    fun combineScores(
        acousticScore: Float,
        languageModelScore: Float,
        weight: Float = 0.7f  // 70% acoustic, 30% language
    ): Float {
        return (acousticScore * weight) + (languageModelScore * (1f - weight))
    }
}

enum class ConfidenceLevel {
    HIGH,    // Green indicator, execute immediately
    MEDIUM,  // Yellow indicator, ask confirmation
    LOW,     // Orange indicator, show alternatives
    REJECT   // Red indicator, command not recognized
}
```

**2. Integrate with Vivoka Engine (2 hours)**
```kotlin
// File: modules/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaEngine.kt

class VivokaEngine(context: Context) {
    private val confidenceScorer = ConfidenceScorer()

    private fun onRecognitionResult(result: VSDKResult) {
        // Get raw confidence from Vivoka SDK
        val rawConfidence = result.confidence  // 0-100

        // Normalize to 0.0-1.0
        val normalizedConfidence = confidenceScorer.normalizeConfidence(
            rawConfidence.toFloat(),
            RecognitionEngine.VIVOKA
        )

        // Get alternates if available
        val alternates = result.alternateHypotheses.mapIndexed { index, alt ->
            Alternate(
                text = alt.text,
                confidence = confidenceScorer.normalizeConfidence(
                    alt.confidence.toFloat(),
                    RecognitionEngine.VIVOKA
                ),
                rank = index + 1
            )
        }

        // Create confidence result
        val confidenceResult = ConfidenceResult(
            text = result.bestHypothesis,
            confidence = normalizedConfidence,
            alternates = alternates,
            scoringMethod = ScoringMethod.VIVOKA_SDK
        )

        // Emit with confidence info
        _recognitionResults.emit(
            RecognitionResult(
                text = result.bestHypothesis,
                confidence = confidenceResult,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
```

**3. Add Visual Feedback System (3 hours)**
```kotlin
// File: modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/ConfidenceIndicator.kt

@Composable
fun ConfidenceIndicator(confidence: Float) {
    val level = ConfidenceScorer().getConfidenceLevel(confidence)

    val color = when (level) {
        ConfidenceLevel.HIGH -> Color.Green
        ConfidenceLevel.MEDIUM -> Color.Yellow
        ConfidenceLevel.LOW -> Color.Orange
        ConfidenceLevel.REJECT -> Color.Red
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(8.dp)
    ) {
        // Confidence circle indicator
        Canvas(modifier = Modifier.size(16.dp)) {
            drawCircle(
                color = color,
                radius = size.minDimension / 2f
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Confidence percentage
        Text(
            text = "${(confidence * 100).toInt()}%",
            color = color,
            fontSize = 12.sp
        )
    }
}
```

#### CONF-2: Confidence-Based Command Filtering
- **Priority:** 🔴 CRITICAL
- **Estimated Time:** 4 hours
- **Implementation:** Filter commands based on confidence thresholds

```kotlin
// File: modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/CommandManager.kt

suspend fun executeCommandWithConfidence(
    command: Command,
    confidence: ConfidenceResult
): CommandResult {
    val level = confidenceScorer.getConfidenceLevel(confidence.confidence)

    return when (level) {
        ConfidenceLevel.HIGH -> {
            // Execute immediately
            executeCommand(command)
        }

        ConfidenceLevel.MEDIUM -> {
            // Ask for confirmation
            showConfirmationDialog(
                message = "Did you mean '${command.text}'?",
                confidence = confidence.confidence,
                onConfirm = { executeCommand(command) },
                onReject = { CommandResult.rejected() }
            )
        }

        ConfidenceLevel.LOW -> {
            // Show alternatives
            showAlternativesDialog(
                primary = command,
                alternates = confidence.alternates.map { alt ->
                    Command.parse(alt.text)
                },
                onSelect = { selected -> executeCommand(selected) }
            )
        }

        ConfidenceLevel.REJECT -> {
            // Reject and log
            Log.w(TAG, "Command rejected: low confidence ${confidence.confidence}")
            CommandResult.rejected("Confidence too low")
        }
    }
}
```

#### CONF-3: Confidence Learning System
- **Priority:** 🔴 CRITICAL
- **Estimated Time:** 3 hours
- **Implementation:** Track which commands consistently score low and improve them

```kotlin
// File: modules/managers/VoiceDataManager/src/main/java/com/augmentalis/datamanager/repositories/ConfidenceTrackingRepository.kt

data class CommandConfidenceHistory(
    val commandText: String,
    val averageConfidence: Float,
    val executionCount: Int,
    val successRate: Float,  // % of times user confirmed
    val lastUpdated: Long
)

class ConfidenceTrackingRepository(private val db: AppDatabase) {

    suspend fun trackExecution(
        command: String,
        confidence: Float,
        userConfirmed: Boolean
    ) {
        val history = db.confidenceDao().getHistory(command) ?: CommandConfidenceHistory(
            commandText = command,
            averageConfidence = confidence,
            executionCount = 1,
            successRate = if (userConfirmed) 1f else 0f,
            lastUpdated = System.currentTimeMillis()
        )

        // Update rolling average
        val newAverage = (history.averageConfidence * history.executionCount + confidence) /
                        (history.executionCount + 1)

        val newSuccessRate = (history.successRate * history.executionCount +
                             if (userConfirmed) 1f else 0f) /
                             (history.executionCount + 1)

        db.confidenceDao().update(
            history.copy(
                averageConfidence = newAverage,
                executionCount = history.executionCount + 1,
                successRate = newSuccessRate,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    /**
     * Get commands that need improvement (low confidence, high usage)
     */
    suspend fun getCommandsNeedingImprovement(): List<CommandConfidenceHistory> {
        return db.confidenceDao().getAll()
            .filter { it.averageConfidence < 0.75f && it.executionCount > 5 }
            .sortedByDescending { it.executionCount }
    }
}
```

---

### Group B: Similarity Matching Algorithms (8 hours)

#### SIM-1: Port VoiceUtils Similarity Matching
- **Priority:** 🔴 CRITICAL
- **Estimated Time:** 4 hours
- **Current State:** VoiceUtils.kt not ported from legacy
- **Legacy Reference:** `/LegacyAvenue/voiceos/src/main/java/com/augmentalis/voiceos/utils/VoiceUtils.kt`

**Implementation Plan:**

```kotlin
// File: modules/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/utils/SimilarityMatcher.kt

/**
 * Similarity matching algorithms for fuzzy command matching
 * Ported from legacy VoiceUtils.kt
 */
object SimilarityMatcher {

    /**
     * Find most similar command with confidence score
     *
     * @param input User's spoken input
     * @param commands Available commands to match against
     * @param threshold Minimum similarity threshold (0.0-1.0)
     * @return Pair of best match and confidence score, or null if below threshold
     */
    fun findMostSimilarWithConfidence(
        input: String,
        commands: List<String>,
        threshold: Float = 0.70f
    ): Pair<String, Float>? {
        if (commands.isEmpty()) return null

        val normalizedInput = input.lowercase().trim()

        // Calculate similarity for each command
        val similarities = commands.map { command ->
            val normalizedCommand = command.lowercase().trim()
            val similarity = calculateSimilarity(normalizedInput, normalizedCommand)
            command to similarity
        }

        // Get best match
        val bestMatch = similarities.maxByOrNull { it.second } ?: return null

        // Return only if above threshold
        return if (bestMatch.second >= threshold) {
            bestMatch
        } else {
            null
        }
    }

    /**
     * Calculate similarity between two strings (0.0-1.0)
     * Uses Levenshtein distance with normalization
     */
    fun calculateSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f

        val distance = levenshteinDistance(s1, s2)
        val maxLength = maxOf(s1.length, s2.length)

        // Convert distance to similarity (0.0-1.0)
        return 1.0f - (distance.toFloat() / maxLength.toFloat())
    }

    /**
     * Levenshtein distance algorithm
     * Measures minimum number of single-character edits needed
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        // Create distance matrix
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        // Initialize first row and column
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        // Calculate distances
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1

                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Find all commands within similarity threshold
     * Useful for showing alternatives
     */
    fun findAllSimilar(
        input: String,
        commands: List<String>,
        threshold: Float = 0.70f,
        maxResults: Int = 5
    ): List<Pair<String, Float>> {
        val normalizedInput = input.lowercase().trim()

        return commands
            .map { command ->
                val similarity = calculateSimilarity(
                    normalizedInput,
                    command.lowercase().trim()
                )
                command to similarity
            }
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }
            .take(maxResults)
    }
}
```

#### SIM-2: Integrate Similarity Matching with Recognition
- **Priority:** 🔴 CRITICAL
- **Estimated Time:** 4 hours
- **Implementation:** Use similarity matching for fallback when exact match fails

```kotlin
// File: modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/CommandManager.kt

suspend fun executeCommand(recognizedText: String): CommandResult {
    // 1. Try exact match first
    val exactMatch = findExactCommand(recognizedText)
    if (exactMatch != null) {
        return execute(exactMatch)
    }

    // 2. Try fuzzy matching
    val availableCommands = getAllAvailableCommands()
    val similarMatch = SimilarityMatcher.findMostSimilarWithConfidence(
        input = recognizedText,
        commands = availableCommands.map { it.text },
        threshold = 0.70f
    )

    if (similarMatch != null) {
        val (matchedCommand, similarity) = similarMatch

        // If similarity is very high (>90%), execute
        if (similarity > 0.90f) {
            return execute(findCommand(matchedCommand)!!)
        }

        // Otherwise, ask for confirmation
        return showConfirmationDialog(
            message = "Did you mean '$matchedCommand'?",
            confidence = similarity,
            onConfirm = { execute(findCommand(matchedCommand)!!) },
            onReject = { CommandResult.notFound() }
        )
    }

    // 3. Show alternatives if any are close
    val alternatives = SimilarityMatcher.findAllSimilar(
        input = recognizedText,
        commands = availableCommands.map { it.text },
        threshold = 0.60f,
        maxResults = 5
    )

    if (alternatives.isNotEmpty()) {
        return showAlternativesDialog(alternatives)
    }

    // 4. No match found
    return CommandResult.notFound("No similar commands found")
}
```

---

### Group C: VOSK Offline Engine (40 hours)

**Note:** See detailed VOSK tasks in previous TODO list (VOSK-1 through VOSK-8)
- VOSK-1: Core engine implementation (12h)
- VOSK-2: Four-tier caching system (8h)
- VOSK-3: Grammar constraint generation (6h)
- VOSK-4: Command learning system (6h)
- VOSK-5: Confidence scoring integration (included in CONF tasks)
- VOSK-6: Similarity matching integration (included in SIM tasks)
- VOSK-7: Model management (5h)
- VOSK-8: Testing suite (6h)

**Total VOSK:** 43 hours (tracked separately)

---

## 🟠 HIGH PRIORITY - Infrastructure & Critical Stubs (122 hours)

### Group D: HILT Dependency Injection (15 hours)

#### DI-1: Application Module Setup
- **Priority:** 🟠 HIGH
- **Estimated Time:** 3 hours
- **Files to Create:**
  - `app/src/main/java/com/augmentalis/voiceos/di/AppModule.kt`
  - `app/src/main/java/com/augmentalis/voiceos/VoiceOSApplication.kt`

**Implementation Plan:**

```kotlin
// File: app/src/main/java/com/augmentalis/voiceos/VoiceOSApplication.kt

@HiltAndroidApp
class VoiceOSApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize VoiceOsLogger
        VoiceOsLogger.initialize(this)

        Timber.d("VoiceOS Application initialized")
    }
}
```

```kotlin
// File: app/src/main/java/com/augmentalis/voiceos/di/AppModule.kt

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationContext
    fun provideContext(application: Application): Context {
        return application.applicationContext
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences(
            "voiceos_prefs",
            Context.MODE_PRIVATE
        )
    }

    @Provides
    @Singleton
    fun providePackageManager(
        @ApplicationContext context: Context
    ): PackageManager {
        return context.packageManager
    }

    @Provides
    @Singleton
    fun provideResources(
        @ApplicationContext context: Context
    ): Resources {
        return context.resources
    }
}
```

#### DI-2: Speech Recognition Module
- **Priority:** 🟠 HIGH
- **Estimated Time:** 4 hours
- **File:** `modules/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/di/SpeechModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SpeechModule {

    @Provides
    @Singleton
    fun provideRecognitionConfig(
        @ApplicationContext context: Context
    ): RecognitionConfig {
        return RecognitionConfig.Builder()
            .setDefaultLanguage("en-US")
            .setCommandMode(RecognitionMode.COMMAND)
            .setTimeout(5000)
            .build()
    }

    @Provides
    @Singleton
    fun provideVivokaEngine(
        @ApplicationContext context: Context,
        config: RecognitionConfig
    ): VivokaEngine {
        return VivokaEngine(context, config).apply {
            initialize()
        }
    }

    @Provides
    @Singleton
    fun provideVoskEngine(
        @ApplicationContext context: Context,
        config: RecognitionConfig
    ): VoskEngine {
        return VoskEngine(context, config)
    }

    @Provides
    @Singleton
    fun provideGoogleEngine(
        @ApplicationContext context: Context,
        config: RecognitionConfig
    ): GoogleEngine {
        return GoogleEngine(context, config)
    }

    @Provides
    @Singleton
    fun provideEngineFactory(
        vivoka: VivokaEngine,
        vosk: VoskEngine,
        google: GoogleEngine
    ): RecognitionEngineFactory {
        return RecognitionEngineFactory(
            engines = mapOf(
                RecognitionEngine.VIVOKA to vivoka,
                RecognitionEngine.VOSK to vosk,
                RecognitionEngine.GOOGLE to google
            )
        )
    }

    @Provides
    @Singleton
    fun provideSpeechRecognitionServiceManager(
        factory: RecognitionEngineFactory,
        config: RecognitionConfig
    ): SpeechRecognitionServiceManager {
        return SpeechRecognitionServiceManager(factory, config)
    }
}
```

#### DI-3: Accessibility Service Module
- **Priority:** 🟠 HIGH
- **Estimated Time:** 3 hours
- **File:** `modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/di/AccessibilityModule.kt`

```kotlin
@Module
@InstallIn(ServiceComponent::class)
object AccessibilityModule {

    @Provides
    fun provideUIScrapingEngine(): UIScrapingEngine {
        return UIScrapingEngine()
    }

    @Provides
    fun provideInstalledAppsProcessor(
        @ApplicationContext context: Context
    ): InstalledAppsProcessor {
        return InstalledAppsProcessor(context)
    }

    @Provides
    fun provideClickHandler(
        service: VoiceOSService
    ): ClickHandler {
        return ClickHandler(service)
    }

    @Provides
    fun provideDragHandler(
        service: VoiceOSService
    ): DragHandler {
        return DragHandler(service)
    }

    @Provides
    fun provideGestureHandler(
        service: VoiceOSService
    ): GestureHandler {
        return GestureHandler(service)
    }

    @Provides
    fun provideSelectHandler(
        service: VoiceOSService
    ): SelectHandler {
        return SelectHandler(service)
    }

    @Provides
    fun provideNumberHandler(
        service: VoiceOSService
    ): NumberHandler {
        return NumberHandler(service)
    }
}
```

#### DI-4: Data Module
- **Priority:** 🟠 HIGH
- **Estimated Time:** 3 hours
- **File:** `app/src/main/java/com/augmentalis/voiceos/di/DataModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "voiceos_database"
        )
        .addMigrations(/* migrations here */)
        .fallbackToDestructiveMigration() // Remove in production
        .build()
    }

    @Provides
    @Singleton
    fun provideCommandHistoryRepository(
        db: AppDatabase
    ): CommandHistoryRepository {
        return CommandHistoryRepository(db.commandHistoryDao())
    }

    @Provides
    @Singleton
    fun provideRecognitionLearningRepository(
        db: AppDatabase
    ): RecognitionLearningRepository {
        return RecognitionLearningRepository(db.recognitionLearningDao())
    }

    @Provides
    @Singleton
    fun provideConfidenceTrackingRepository(
        db: AppDatabase
    ): ConfidenceTrackingRepository {
        return ConfidenceTrackingRepository(db)
    }

    // Add all other repository providers
}
```

#### DI-5: Manager Module
- **Priority:** 🟠 HIGH
- **Estimated Time:** 2 hours
- **File:** `app/src/main/java/com/augmentalis/voiceos/di/ManagerModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ManagerModule {

    @Provides
    @Singleton
    fun provideCommandManager(
        @ApplicationContext context: Context
    ): CommandManager {
        return CommandManager(context)
    }

    @Provides
    @Singleton
    fun provideHUDManager(
        @ApplicationContext context: Context
    ): HUDManager {
        return HUDManager(context)
    }

    @Provides
    @Singleton
    fun provideLocalizationManager(
        @ApplicationContext context: Context
    ): LocalizationManager {
        return LocalizationManager(context)
    }

    @Provides
    @Singleton
    fun provideLicenseManager(
        @ApplicationContext context: Context
    ): LicenseManager {
        return LicenseManager(context)
    }

    @Provides
    @Singleton
    fun provideVoiceOsLogger(
        @ApplicationContext context: Context
    ): VoiceOsLogger {
        return VoiceOsLogger.getInstance(context)
    }
}
```

---

### Group E: VoiceOsLogger Implementation (13 hours)

#### LOG-1: Core Logger Infrastructure
- **Priority:** 🟠 HIGH
- **Estimated Time:** 4 hours
- **File:** Create new module `modules/libraries/VoiceOsLogger/`

```kotlin
// File: modules/libraries/VoiceOsLogger/src/main/java/com/augmentalis/logger/VoiceOsLogger.kt

object VoiceOsLogger {

    private lateinit var context: Context
    private val logBuffer = ConcurrentLinkedQueue<LogEntry>()
    private val logFile: File by lazy {
        File(context.filesDir, "logs/voiceos-${getCurrentDate()}.log")
    }

    // Log levels
    enum class Level {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }

    data class LogEntry(
        val timestamp: Long,
        val level: Level,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null
    )

    // Module-specific log levels
    private val moduleLevels = mutableMapOf<String, Level>()

    // Global log level (default: INFO for production, DEBUG for debug builds)
    private var globalLevel: Level = if (BuildConfig.DEBUG) Level.DEBUG else Level.INFO

    fun initialize(context: Context) {
        this.context = context.applicationContext

        // Create logs directory
        File(context.filesDir, "logs").mkdirs()

        // Start background log writer
        startLogWriter()
    }

    // Logging methods
    fun v(tag: String, message: String) = log(Level.VERBOSE, tag, message)
    fun d(tag: String, message: String) = log(Level.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(Level.INFO, tag, message)
    fun w(tag: String, message: String) = log(Level.WARN, tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) =
        log(Level.ERROR, tag, message, throwable)

    private fun log(
        level: Level,
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) {
        // Check if should log based on module and global levels
        val moduleLevel = moduleLevels[tag] ?: globalLevel
        if (level.ordinal < moduleLevel.ordinal) return

        // Log to Android logcat
        when (level) {
            Level.VERBOSE -> Log.v(tag, message, throwable)
            Level.DEBUG -> Log.d(tag, message, throwable)
            Level.INFO -> Log.i(tag, message, throwable)
            Level.WARN -> Log.w(tag, message, throwable)
            Level.ERROR -> Log.e(tag, message, throwable)
        }

        // Add to buffer for file writing
        logBuffer.offer(LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        ))
    }

    // Set module-specific log level
    fun setModuleLogLevel(module: String, level: Level) {
        moduleLevels[module] = level
    }

    // Performance tracking
    private val timings = mutableMapOf<String, Long>()

    fun startTiming(operation: String) {
        timings[operation] = System.currentTimeMillis()
    }

    fun endTiming(operation: String) {
        val startTime = timings.remove(operation)
        if (startTime != null) {
            val duration = System.currentTimeMillis() - startTime
            d("PERFORMANCE", "$operation took ${duration}ms")
        }
    }

    // Background log writer
    private fun startLogWriter() {
        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                delay(5000) // Write every 5 seconds
                flushLogs()
            }
        }
    }

    private suspend fun flushLogs() {
        val entries = mutableListOf<LogEntry>()
        while (logBuffer.isNotEmpty()) {
            logBuffer.poll()?.let { entries.add(it) }
        }

        if (entries.isEmpty()) return

        logFile.parentFile?.mkdirs()
        logFile.appendText(
            entries.joinToString("\n") { entry ->
                "${formatTimestamp(entry.timestamp)} ${entry.level.name[0]} ${entry.tag}: ${entry.message}"
            } + "\n"
        )
    }

    // Export logs
    fun exportLogs(): File {
        runBlocking { flushLogs() }
        return logFile
    }
}
```

**Remaining LOG tasks:**
- LOG-2: File-based logging (3h) - Rotating logs, size limits
- LOG-3: Remote logging (3h) - Firebase integration
- LOG-4: Performance profiling (3h) - Memory tracking, UI performance

---

### Group F: UI Overlays (26 hours)

**10 stub implementations needed:**

#### UI-1: Numbered Command Selection Overlay
- **Priority:** 🟠 HIGH
- **Time:** 4 hours
- **Current Stub:** `NumberHandler.kt:454`
- **Implementation:** Visual numbered overlay for command disambiguation

#### UI-2: Context Menu System
- **Priority:** 🟠 HIGH
- **Time:** 4 hours
- **Current Stubs:** `SelectHandler.kt:493, 499, 505`
- **Implementation:** Voice-activated context menus for copy/paste/select

#### UI-3: Selection Mode Indicators
- **Priority:** 🟠 HIGH
- **Time:** 2 hours
- **Current Stubs:** `SelectHandler.kt:156, 448`
- **Implementation:** Visual feedback for selection mode

#### UI-4: Help Menu Overlay
- **Priority:** 🟠 HIGH
- **Time:** 3 hours
- **Current Stub:** `HelpMenuHandler.kt:387`
- **Implementation:** Interactive help overlay system

#### UI-5-10: Additional overlays (13 hours)
- Voice status view (3h)
- Click feedback animation (2h)
- Voice command overlay (5h)
- Initialization view (3h)

---

### Group G: VoiceAccessibility Cursor Integration (18 hours)

**11 stubs to implement - cursor manager integration**

#### VA-1-4: Cursor Manager Integration (12 hours)
- Current Stubs: `SelectHandler.kt:483, 488`
- Integrate cursor positioning with selection
- Context menu triggers
- Cursor visibility management

#### VA-5-8: UI Components (6 hours)
- Voice command visibility
- Service initialization
- Accessibility event handling

---

### Group H: LearnApp Completion (12 hours)

**7 stubs to implement:**

#### LA-1: App Hash Calculation
- **Time:** 2 hours
- **Stub:** `LearnAppRepository.kt:231`
- **Implementation:** Proper hash algorithm for app fingerprinting

#### LA-2: Element Fetching
- **Time:** 2 hours
- **Stub:** `LearnAppRepository.kt:168`

#### LA-3: Version Info Integration
- **Time:** 2 hours
- **Stubs:** `VOS4LearnAppIntegration.kt:257-258`

#### LA-4: Login Prompt Overlay
- **Time:** 3 hours
- **Stub:** `VOS4LearnAppIntegration.kt:241`

#### LA-5: Error Notification System
- **Time:** 2 hours
- **Stub:** `VOS4LearnAppIntegration.kt:269`

#### LA-6: Scrollable Container Tracking
- **Time:** 1 hour
- **Stub:** `ExplorationEngine.kt:522`

---

### Group I: DeviceManager Features (14 hours)

**7 stubs to implement:**

#### DM-1: UWB Support Detection
- **Time:** 2 hours
- **Stub:** `DeviceViewModel.kt:418`

#### DM-2: IMU Public Methods
- **Time:** 3 hours
- **Stub:** `DeviceViewModel.kt:436, 580`

#### DM-3: Bluetooth Public Methods
- **Time:** 3 hours
- **Stub:** `DeviceViewModel.kt:464`

#### DM-4: WiFi Public Methods
- **Time:** 3 hours
- **Stub:** `DeviceViewModel.kt:492`

#### DM-5: Audio Manager State
- **Time:** 2 hours
- **Stub:** `DeviceInfoUI.kt:397`

#### DM-6: NFC Detection Migration
- **Time:** 1 hour
- **Stub:** `NfcManager.kt:124`

---

## 🟡 MEDIUM PRIORITY - Enhancements (54 hours)

### Group J: CommandManager Dynamic Features (23 hours)

See previous TODO for CMD-1 through CMD-6 details

### Group K: VoiceKeyboard Polish (31 hours)

See previous TODO for KB-1 through KB-10 details (special layouts, emoji, suggestions, etc.)

---

## 🟢 LOW PRIORITY - Optional Features (8 hours)

### Group L: Google Cloud Speech Engine (19 hours - moved to MEDIUM)

### Group M: Optimization (8 hours)

#### OPT-1: Cursor Smoothing Algorithm
- **Time:** 3 hours
- **Stub:** `CursorAdapter.kt:211`

#### OPT-2: DeviceManager Optimizations
- **Time:** 5 hours
- **Stubs:** Various DeviceManager enhancements

---

## 📊 Implementation Priority Order

### Week 1 (40 hours):
1. **CONF-1, CONF-2, CONF-3:** Real-time confidence scoring system (15h) 🔴
2. **SIM-1, SIM-2:** Similarity matching (8h) 🔴
3. **DI-1, DI-2:** HILT Application + Speech modules (7h) 🟠
4. **LOG-1:** Core VoiceOsLogger (4h) 🟠
5. **VOSK-1:** Start VOSK engine (6h) 🔴

### Week 2 (40 hours):
1. **VOSK-1 completion + VOSK-2:** Core engine + caching (20h) 🔴
2. **DI-3, DI-4, DI-5:** Remaining HILT modules (8h) 🟠
3. **UI-1, UI-2:** Critical UI overlays (8h) 🟠
4. **VA-1:** Start cursor integration (4h) 🟠

### Week 3 (40 hours):
1. **VOSK-3, VOSK-4:** Grammar + learning (12h) 🔴
2. **UI-3, UI-4, UI-5:** UI overlays completion (8h) 🟠
3. **LA-1 through LA-6:** LearnApp completion (12h) 🟠
4. **DM-1 through DM-3:** DeviceManager features (8h) 🟠

### Week 4+ (remaining time):
- Complete VOSK testing
- VoiceAccessibility cursor integration
- CommandManager dynamic features
- VoiceKeyboard polish
- Google engine implementation

---

## Success Metrics

**Phase 2 Complete When:**
- ✅ Real-time confidence scoring working on all engines
- ✅ Similarity matching integrated
- ✅ VOSK offline engine functional
- ✅ All 5 HILT modules implemented
- ✅ VoiceOsLogger operational

**Phase 3 Complete When:**
- ✅ All 10 UI overlay stubs implemented
- ✅ All 11 VoiceAccessibility stubs complete
- ✅ All 7 LearnApp stubs complete
- ✅ All 7 DeviceManager stubs complete

---

**Document Status:** ✅ Complete with implementation details
**Next Update:** After completing CONF-1 (Real-time confidence scoring)
**Estimated Total Time:** 247 hours (31 working days for 1 developer, 15 days for 2 developers)
