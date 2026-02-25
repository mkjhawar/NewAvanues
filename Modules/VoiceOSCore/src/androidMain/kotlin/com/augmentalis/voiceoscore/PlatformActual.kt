/**
 * PlatformActual.kt - Android platform-specific implementations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-28
 */
package com.augmentalis.voiceoscore

import android.annotation.SuppressLint
import android.util.Log

// ═══════════════════════════════════════════════════════════════════
// Time Functions
// ═══════════════════════════════════════════════════════════════════

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

// ═══════════════════════════════════════════════════════════════════
// Platform Extractor Functions
// ═══════════════════════════════════════════════════════════════════

actual fun extractAccessibilityElements(): List<ElementInfo> = emptyList()

actual fun executeWebScript(script: String): String? = null

actual fun isAccessibilityAvailable(): Boolean = true

actual fun isWebExtractionAvailable(): Boolean = false

actual fun getPlatformName(): String = "Android"

// ═══════════════════════════════════════════════════════════════════
// DeviceCapabilityManager
// ═══════════════════════════════════════════════════════════════════

@SuppressLint("StaticFieldLeak")
actual object DeviceCapabilityManager {
    private var cachedSpeed: DeviceSpeed? = null
    private var userDebounceMs: Long? = null
    private var contextRef: android.content.Context? = null

    actual enum class DeviceSpeed {
        FAST, MEDIUM, SLOW
    }

    /**
     * Initialize with Android context for accurate device detection.
     */
    actual fun init(context: Any) {
        val androidContext = context as? android.content.Context
            ?: throw IllegalArgumentException("DeviceCapabilityManager.init requires Android Context")
        contextRef = androidContext.applicationContext
    }

    actual fun getContentDebounceMs(): Long {
        return userDebounceMs ?: when (getDeviceSpeed()) {
            DeviceSpeed.FAST -> 400L
            DeviceSpeed.MEDIUM -> 600L
            DeviceSpeed.SLOW -> 800L
        }
    }

    actual fun getScrollDebounceMs(): Long {
        // Delegate to AdaptiveTimingManager for learned value.
        // Scroll is a lightweight operation (offset + overlay refresh),
        // so it uses a separate, faster debounce than content changes.
        return AdaptiveTimingManager.getScrollDebounceMs()
    }

    actual fun setUserDebounceMs(ms: Long?) {
        userDebounceMs = ms
    }

    actual fun getUserDebounceMs(): Long? = userDebounceMs

    actual fun getDeviceSpeed(): DeviceSpeed {
        cachedSpeed?.let { return it }
        val speed = detectDeviceSpeed()
        Log.i("TAG", "getDeviceSpeed: speed = $speed")
        cachedSpeed = speed
        return speed
    }

    private fun detectDeviceSpeed(): DeviceSpeed {
        val cores = Runtime.getRuntime().availableProcessors()
        val memory = Runtime.getRuntime().maxMemory() / (1024 * 1024) // MB

        return when {
            cores >= 8 && memory >= 512 -> DeviceSpeed.FAST
            cores >= 4 && memory >= 256 -> DeviceSpeed.MEDIUM
            else -> DeviceSpeed.SLOW
        }
    }

    actual fun getMaxConcurrentOperations(): Int {
        return when (getDeviceSpeed()) {
            DeviceSpeed.FAST -> 4
            DeviceSpeed.MEDIUM -> 2
            DeviceSpeed.SLOW -> 1
        }
    }

    actual fun supportsAggressiveScanning(): Boolean {
        return getDeviceSpeed() == DeviceSpeed.FAST
    }

    actual fun resetCache() {
        cachedSpeed = null
    }

    actual fun getTimingConfig(operation: TimingOperation): TimingConfig {
        val speed = getDeviceSpeed()
        return when (operation) {
            TimingOperation.CONTENT_CHANGE -> TimingConfig(
                debounceMs = getContentDebounceMs(),
                minIntervalMs = if (speed == DeviceSpeed.FAST) 50L else 100L
            )
            TimingOperation.SCROLL -> TimingConfig(
                debounceMs = getScrollDebounceMs(),
                minIntervalMs = 30L,
                canSkip = true
            )
            TimingOperation.FULL_SCRAPE -> TimingConfig(
                debounceMs = 500L,
                minIntervalMs = 1000L,
                maxQueueDepth = 1
            )
            TimingOperation.INCREMENTAL_UPDATE -> TimingConfig(
                debounceMs = 100L,
                minIntervalMs = 50L,
                maxQueueDepth = 3,
                canSkip = true
            )
            TimingOperation.OVERLAY_REFRESH -> TimingConfig(
                debounceMs = 200L,
                minIntervalMs = 100L
            )
            TimingOperation.SPEECH_ENGINE_UPDATE -> TimingConfig(
                debounceMs = AdaptiveTimingManager.getSpeechUpdateDebounceMs(),
                minIntervalMs = 100L
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// VivokaEngineFactory
// ═══════════════════════════════════════════════════════════════════

actual object VivokaEngineFactory {
    private var applicationContext: android.content.Context? = null

    /**
     * Initialize the factory with Android Application context.
     * Must be called once from Application.onCreate() before creating engines.
     */
    actual fun initialize(context: Any) {
        val androidContext = context as? android.content.Context
            ?: throw IllegalArgumentException("VivokaEngineFactory.initialize requires Android Context")
        applicationContext = androidContext.applicationContext
    }

    /**
     * Check if Vivoka is available on Android.
     * Returns true if Context is initialized and Vivoka SDK classes are available.
     */
    actual fun isAvailable(): Boolean {
        if (applicationContext == null) return false

        // Check if Vivoka SDK is available via reflection
        return try {
            Class.forName("com.vivoka.vsdk.asr.csdk.recognizer.Recognizer")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * Create Android-specific Vivoka engine.
     *
     * @param config Vivoka configuration
     * @return IVivokaEngine implementation (VivokaAndroidEngine or StubVivokaEngine)
     */
    actual fun create(config: VivokaConfig): IVivokaEngine {
        val context = applicationContext
            ?: throw IllegalStateException(
                "VivokaEngineFactory not initialized. " +
                "Call VivokaEngineFactory.initialize(context) in Application.onCreate()"
            )

        return if (isAvailable()) {
            VivokaAndroidEngine(context, config)
        } else {
            // Return stub if Vivoka SDK is not available
            StubVivokaEngine("Vivoka SDK not available. Ensure vivoka-sdk dependency is included.")
        }
    }

    /**
     * Create engine with explicit context (for cases where app context isn't set).
     * Useful for testing or late initialization.
     */
    fun createWithContext(context: android.content.Context, config: VivokaConfig): IVivokaEngine {
        val appContext = context.applicationContext

        // Also store it for future calls
        if (applicationContext == null) {
            applicationContext = appContext
        }

        return if (isAvailable()) {
            VivokaAndroidEngine(appContext, config)
        } else {
            StubVivokaEngine("Vivoka SDK not available. Ensure vivoka-sdk dependency is included.")
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// SpeechEngineFactoryProvider
// ═══════════════════════════════════════════════════════════════════

actual object SpeechEngineFactoryProvider {
    actual fun create(): ISpeechEngineFactory = AndroidSpeechEngineFactory()
}

/**
 * Android speech engine factory implementation.
 *
 * Provides access to Android-supported speech engines:
 * - ANDROID_STT (native)
 * - VOSK (offline)
 * - WHISPER (offline)
 * - VIVOKA (commercial, if SDK available)
 * - GOOGLE_CLOUD (online, requires API key)
 * - AZURE (online, requires subscription)
 */
internal class AndroidSpeechEngineFactory : ISpeechEngineFactory {

    override fun getAvailableEngines(): List<SpeechEngine> {
        return listOf(
            SpeechEngine.ANDROID_STT,
            SpeechEngine.VOSK,
            SpeechEngine.WHISPER,
            SpeechEngine.GOOGLE_CLOUD,
            SpeechEngine.AZURE
        ).filter { isEngineAvailable(it) }
    }

    override fun isEngineAvailable(engine: SpeechEngine): Boolean {
        return when (engine) {
            SpeechEngine.ANDROID_STT -> true // Always available on Android
            SpeechEngine.VOSK -> true // Requires model download
            SpeechEngine.WHISPER -> true // Requires model download
            SpeechEngine.VIVOKA -> VivokaEngineFactory.isAvailable()
            SpeechEngine.GOOGLE_CLOUD -> true // Requires API key
            SpeechEngine.AZURE -> true // Requires subscription
            SpeechEngine.APPLE_SPEECH -> false // iOS only
            SpeechEngine.AVX -> true // Requires model download (Sherpa-ONNX)
        }
    }

    override fun createEngine(engine: SpeechEngine): Result<ISpeechEngine> {
        return when (engine) {
            SpeechEngine.VIVOKA -> {
                if (VivokaEngineFactory.isAvailable()) {
                    Result.success(VivokaEngineFactory.create(VivokaConfig.DEFAULT))
                } else {
                    Result.failure(UnsupportedOperationException("Vivoka SDK not available"))
                }
            }
            else -> {
                // TODO: Implement actual engine creation for other engines
                Result.failure(UnsupportedOperationException("Engine $engine not yet implemented"))
            }
        }
    }

    override fun getRecommendedEngine(): SpeechEngine {
        // Prefer offline-capable engines, then native
        return when {
            isEngineAvailable(SpeechEngine.VIVOKA) -> SpeechEngine.VIVOKA
            isEngineAvailable(SpeechEngine.VOSK) -> SpeechEngine.VOSK
            else -> SpeechEngine.ANDROID_STT
        }
    }

    override fun getEngineFeatures(engine: SpeechEngine): Set<EngineFeature> {
        return when (engine) {
            SpeechEngine.ANDROID_STT -> setOf(
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.LANGUAGE_DETECTION
            )
            SpeechEngine.VOSK -> setOf(
                EngineFeature.OFFLINE_MODE,
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.CUSTOM_VOCABULARY
            )
            SpeechEngine.WHISPER -> setOf(
                EngineFeature.OFFLINE_MODE,
                EngineFeature.WORD_TIMESTAMPS,
                EngineFeature.LANGUAGE_DETECTION,
                EngineFeature.TRANSLATION,
                EngineFeature.PUNCTUATION
            )
            SpeechEngine.VIVOKA -> setOf(
                EngineFeature.OFFLINE_MODE,
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.WAKE_WORD,
                EngineFeature.CUSTOM_VOCABULARY
            )
            SpeechEngine.GOOGLE_CLOUD -> setOf(
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.WORD_TIMESTAMPS,
                EngineFeature.SPEAKER_DIARIZATION,
                EngineFeature.PUNCTUATION
            )
            SpeechEngine.AZURE -> setOf(
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.WORD_TIMESTAMPS,
                EngineFeature.SPEAKER_DIARIZATION,
                EngineFeature.TRANSLATION,
                EngineFeature.PUNCTUATION
            )
            SpeechEngine.APPLE_SPEECH -> emptySet() // Not available on Android
            SpeechEngine.AVX -> setOf(
                EngineFeature.OFFLINE_MODE,
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.CUSTOM_VOCABULARY
            )
        }
    }

    override fun getSetupRequirements(engine: SpeechEngine): EngineRequirements {
        return when (engine) {
            SpeechEngine.ANDROID_STT -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = true,
                requiresApiKey = false,
                notes = "Uses device's native speech recognizer"
            )
            SpeechEngine.VOSK -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = true,
                modelSizeMB = 50,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Download language model for offline recognition"
            )
            SpeechEngine.WHISPER -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = true,
                modelSizeMB = 230,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Download Whisper model (tiny/base/small/medium)"
            )
            SpeechEngine.VIVOKA -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = true,
                modelSizeMB = 60,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Requires Vivoka commercial license"
            )
            SpeechEngine.GOOGLE_CLOUD -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO", "android.permission.INTERNET"),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = true,
                requiresApiKey = true,
                notes = "Requires Google Cloud API key"
            )
            SpeechEngine.AZURE -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO", "android.permission.INTERNET"),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = true,
                requiresApiKey = true,
                notes = "Requires Azure subscription key"
            )
            SpeechEngine.APPLE_SPEECH -> EngineRequirements(
                permissions = emptyList(),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Not available on Android"
            )
            SpeechEngine.AVX -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = true,
                modelSizeMB = 70,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Download AVX/Sherpa-ONNX transducer model per language"
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// SynonymPathsProvider
// ═══════════════════════════════════════════════════════════════════

actual object SynonymPathsProvider {
    actual fun getPaths(): ISynonymPaths =
        DefaultSynonymPaths.forAndroid("/data/data/com.augmentalis.voiceos/files")
}
