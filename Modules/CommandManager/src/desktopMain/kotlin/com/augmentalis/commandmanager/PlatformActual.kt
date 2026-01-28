/**
 * PlatformActual.kt - Desktop (JVM) platform-specific implementations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-28
 */
package com.augmentalis.commandmanager

import java.security.MessageDigest

// ═══════════════════════════════════════════════════════════════════
// Time Functions
// ═══════════════════════════════════════════════════════════════════

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

// ═══════════════════════════════════════════════════════════════════
// SHA-256 Functions
// ═══════════════════════════════════════════════════════════════════

actual fun sha256(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

internal actual fun sha256Impl(input: String): String = sha256(input)

// ═══════════════════════════════════════════════════════════════════
// Platform Extractor Functions
// ═══════════════════════════════════════════════════════════════════

actual fun extractAccessibilityElements(): List<ElementInfo> = emptyList()

actual fun executeWebScript(script: String): String? = null

actual fun isAccessibilityAvailable(): Boolean = false

actual fun isWebExtractionAvailable(): Boolean = false

actual fun getPlatformName(): String = "Desktop"

// ═══════════════════════════════════════════════════════════════════
// LoggerFactory
// ═══════════════════════════════════════════════════════════════════

actual object LoggerFactory {
    actual fun getLogger(tag: String): Logger = DesktopLogger(tag)
}

private class DesktopLogger(private val tag: String) : Logger {
    override fun v(message: () -> String) {
        if (isLoggable(LogLevel.VERBOSE)) println("V/$tag: ${message()}")
    }

    override fun d(message: () -> String) {
        if (isLoggable(LogLevel.DEBUG)) println("D/$tag: ${message()}")
    }

    override fun i(message: () -> String) {
        if (isLoggable(LogLevel.INFO)) println("I/$tag: ${message()}")
    }

    override fun w(message: () -> String) {
        if (isLoggable(LogLevel.WARN)) System.err.println("W/$tag: ${message()}")
    }

    override fun e(message: () -> String) {
        if (isLoggable(LogLevel.ERROR)) System.err.println("E/$tag: ${message()}")
    }

    override fun e(message: () -> String, throwable: Throwable) {
        if (isLoggable(LogLevel.ERROR)) {
            System.err.println("E/$tag: ${message()}")
            throwable.printStackTrace(System.err)
        }
    }

    override fun wtf(message: () -> String) {
        System.err.println("WTF/$tag: ${message()}")
    }

    override fun isLoggable(level: LogLevel): Boolean = true
}

// ═══════════════════════════════════════════════════════════════════
// DeviceCapabilityManager
// ═══════════════════════════════════════════════════════════════════

actual object DeviceCapabilityManager {
    private var cachedSpeed: DeviceSpeed? = null
    private var userDebounceMs: Long? = null

    actual enum class DeviceSpeed {
        FAST, MEDIUM, SLOW
    }

    actual fun getContentDebounceMs(): Long {
        return userDebounceMs ?: 100L // Desktop is usually fast
    }

    actual fun getScrollDebounceMs(): Long = 50L

    actual fun setUserDebounceMs(ms: Long?) {
        userDebounceMs = ms
    }

    actual fun getUserDebounceMs(): Long? = userDebounceMs

    actual fun getDeviceSpeed(): DeviceSpeed {
        cachedSpeed?.let { return it }
        val speed = DeviceSpeed.FAST // Desktop assumed fast
        cachedSpeed = speed
        return speed
    }

    actual fun getMaxConcurrentOperations(): Int = 4

    actual fun supportsAggressiveScanning(): Boolean = true

    actual fun resetCache() {
        cachedSpeed = null
    }

    actual fun getTimingConfig(operation: TimingOperation): TimingConfig {
        return when (operation) {
            TimingOperation.CONTENT_CHANGE -> TimingConfig(debounceMs = 100L, minIntervalMs = 50L)
            TimingOperation.SCROLL -> TimingConfig(debounceMs = 50L, minIntervalMs = 30L, canSkip = true)
            TimingOperation.FULL_SCRAPE -> TimingConfig(debounceMs = 500L, minIntervalMs = 1000L)
            TimingOperation.INCREMENTAL_UPDATE -> TimingConfig(debounceMs = 100L, minIntervalMs = 50L, maxQueueDepth = 3, canSkip = true)
            TimingOperation.OVERLAY_REFRESH -> TimingConfig(debounceMs = 200L, minIntervalMs = 100L)
            TimingOperation.SPEECH_ENGINE_UPDATE -> TimingConfig(debounceMs = 300L, minIntervalMs = 200L)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// VivokaEngineFactory
// ═══════════════════════════════════════════════════════════════════

actual object VivokaEngineFactory {
    actual fun isAvailable(): Boolean = false

    actual fun create(config: VivokaConfig): IVivokaEngine =
        StubVivokaEngine("Vivoka SDK not available on desktop")
}

// ═══════════════════════════════════════════════════════════════════
// SpeechEngineFactoryProvider
// ═══════════════════════════════════════════════════════════════════

actual object SpeechEngineFactoryProvider {
    actual fun create(): ISpeechEngineFactory = DesktopSpeechEngineFactory
}

private object DesktopSpeechEngineFactory : ISpeechEngineFactory {

    override fun getAvailableEngines(): List<SpeechEngine> =
        listOf(SpeechEngine.VOSK, SpeechEngine.WHISPER)

    override fun isEngineAvailable(engine: SpeechEngine): Boolean {
        return engine == SpeechEngine.VOSK || engine == SpeechEngine.WHISPER
    }

    override fun createEngine(engine: SpeechEngine): Result<ISpeechEngine> {
        return Result.failure(UnsupportedOperationException("Engine creation not implemented"))
    }

    override fun getRecommendedEngine(): SpeechEngine = SpeechEngine.VOSK

    override fun getEngineFeatures(engine: SpeechEngine): Set<EngineFeature> {
        return when (engine) {
            SpeechEngine.VOSK -> setOf(EngineFeature.OFFLINE_MODE, EngineFeature.CUSTOM_VOCABULARY)
            SpeechEngine.WHISPER -> setOf(EngineFeature.OFFLINE_MODE, EngineFeature.LANGUAGE_DETECTION)
            else -> emptySet()
        }
    }

    override fun getSetupRequirements(engine: SpeechEngine): EngineRequirements {
        return when (engine) {
            SpeechEngine.VOSK -> EngineRequirements(
                permissions = emptyList(),
                requiresModelDownload = true,
                modelSizeMB = 50,
                requiresNetwork = false,
                requiresApiKey = false
            )
            SpeechEngine.WHISPER -> EngineRequirements(
                permissions = emptyList(),
                requiresModelDownload = true,
                modelSizeMB = 250,
                requiresNetwork = false,
                requiresApiKey = false
            )
            else -> EngineRequirements(
                permissions = emptyList(),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = false,
                requiresApiKey = false
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// SynonymPathsProvider
// ═══════════════════════════════════════════════════════════════════

actual object SynonymPathsProvider {
    actual fun getPaths(): ISynonymPaths {
        val appDataDir = System.getProperty("user.home") + "/.commandmanager"
        return DefaultSynonymPaths.forDesktop(appDataDir)
    }
}
