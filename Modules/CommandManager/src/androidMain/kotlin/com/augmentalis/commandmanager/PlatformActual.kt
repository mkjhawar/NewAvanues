/**
 * PlatformActual.kt - Android platform-specific implementations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-28
 */
package com.augmentalis.commandmanager

import android.util.Log
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

actual fun isAccessibilityAvailable(): Boolean = true

actual fun isWebExtractionAvailable(): Boolean = false

actual fun getPlatformName(): String = "Android"

// ═══════════════════════════════════════════════════════════════════
// LoggerFactory
// ═══════════════════════════════════════════════════════════════════

actual object LoggerFactory {
    actual fun getLogger(tag: String): Logger = AndroidLogger(tag)
}

private class AndroidLogger(private val tag: String) : Logger {
    override fun v(message: () -> String) {
        if (isLoggable(LogLevel.VERBOSE)) Log.v(tag, message())
    }

    override fun d(message: () -> String) {
        if (isLoggable(LogLevel.DEBUG)) Log.d(tag, message())
    }

    override fun i(message: () -> String) {
        if (isLoggable(LogLevel.INFO)) Log.i(tag, message())
    }

    override fun w(message: () -> String) {
        if (isLoggable(LogLevel.WARN)) Log.w(tag, message())
    }

    override fun e(message: () -> String) {
        if (isLoggable(LogLevel.ERROR)) Log.e(tag, message())
    }

    override fun e(message: () -> String, throwable: Throwable) {
        if (isLoggable(LogLevel.ERROR)) Log.e(tag, message(), throwable)
    }

    override fun wtf(message: () -> String) {
        Log.wtf(tag, message())
    }

    override fun isLoggable(level: LogLevel): Boolean {
        return Log.isLoggable(tag, when (level) {
            LogLevel.VERBOSE -> Log.VERBOSE
            LogLevel.DEBUG -> Log.DEBUG
            LogLevel.INFO -> Log.INFO
            LogLevel.WARN -> Log.WARN
            LogLevel.ERROR -> Log.ERROR
            LogLevel.ASSERT -> Log.ASSERT
        })
    }
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
        return userDebounceMs ?: when (getDeviceSpeed()) {
            DeviceSpeed.FAST -> 100L
            DeviceSpeed.MEDIUM -> 200L
            DeviceSpeed.SLOW -> 300L
        }
    }

    actual fun getScrollDebounceMs(): Long {
        return when (getDeviceSpeed()) {
            DeviceSpeed.FAST -> 50L
            DeviceSpeed.MEDIUM -> 100L
            DeviceSpeed.SLOW -> 150L
        }
    }

    actual fun setUserDebounceMs(ms: Long?) {
        userDebounceMs = ms
    }

    actual fun getUserDebounceMs(): Long? = userDebounceMs

    actual fun getDeviceSpeed(): DeviceSpeed {
        cachedSpeed?.let { return it }
        val speed = detectDeviceSpeed()
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
                debounceMs = 300L,
                minIntervalMs = 200L
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// VivokaEngineFactory
// ═══════════════════════════════════════════════════════════════════

actual object VivokaEngineFactory {
    actual fun isAvailable(): Boolean = false

    actual fun create(config: VivokaConfig): IVivokaEngine =
        StubVivokaEngine("Vivoka SDK not available on this build")
}

// ═══════════════════════════════════════════════════════════════════
// SpeechEngineFactoryProvider
// ═══════════════════════════════════════════════════════════════════

actual object SpeechEngineFactoryProvider {
    actual fun create(): ISpeechEngineFactory = AndroidSpeechEngineFactory
}

private object AndroidSpeechEngineFactory : ISpeechEngineFactory {

    override fun getAvailableEngines(): List<SpeechEngine> =
        listOf(SpeechEngine.ANDROID_STT, SpeechEngine.VOSK)

    override fun isEngineAvailable(engine: SpeechEngine): Boolean {
        return engine == SpeechEngine.ANDROID_STT || engine == SpeechEngine.VOSK
    }

    override fun createEngine(engine: SpeechEngine): Result<ISpeechEngine> {
        return Result.failure(UnsupportedOperationException("Engine creation not implemented"))
    }

    override fun getRecommendedEngine(): SpeechEngine = SpeechEngine.ANDROID_STT

    override fun getEngineFeatures(engine: SpeechEngine): Set<EngineFeature> {
        return when (engine) {
            SpeechEngine.ANDROID_STT -> setOf(EngineFeature.CONTINUOUS_RECOGNITION)
            SpeechEngine.VOSK -> setOf(EngineFeature.OFFLINE_MODE, EngineFeature.CUSTOM_VOCABULARY)
            else -> emptySet()
        }
    }

    override fun getSetupRequirements(engine: SpeechEngine): EngineRequirements {
        return when (engine) {
            SpeechEngine.ANDROID_STT -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = true,
                requiresApiKey = false
            )
            SpeechEngine.VOSK -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = true,
                modelSizeMB = 50,
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
    actual fun getPaths(): ISynonymPaths =
        DefaultSynonymPaths.forAndroid("/data/data/com.augmentalis.voiceos/files")
}
