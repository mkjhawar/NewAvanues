package com.augmentalis.voiceoscoreng.persistence

import com.augmentalis.voiceoscoreng.common.QuantizedCommand

/**
 * Repository interface for screen hash persistence.
 * Allows caching of scanned screens and their associated commands.
 */
interface ScreenHashRepository {
    suspend fun hasScreen(screenHash: String): Boolean
    suspend fun getAppVersion(screenHash: String): String?
    suspend fun getCommandsForScreen(screenHash: String): List<QuantizedCommand>
    suspend fun getScreenInfo(screenHash: String): ScreenInfo?
    suspend fun saveScreen(
        hash: String,
        packageName: String,
        activityName: String?,
        appVersion: String,
        elementCount: Int
    )
    suspend fun saveCommandsForScreen(screenHash: String, commands: List<QuantizedCommand>)
    suspend fun clearScreensForPackage(packageName: String): Int
    suspend fun clearAllScreens(): Int
    suspend fun getScreenCount(): Int
    suspend fun getScreenCountForPackage(packageName: String): Int
}

/**
 * In-memory implementation of ScreenHashRepository.
 * For production, replace with SQLDelight-backed implementation.
 */
class ScreenHashRepositoryImpl : ScreenHashRepository {
    private val screens = mutableMapOf<String, ScreenInfo>()
    private val commands = mutableMapOf<String, List<QuantizedCommand>>()

    override suspend fun hasScreen(screenHash: String): Boolean = screens.containsKey(screenHash)

    override suspend fun getAppVersion(screenHash: String): String? = screens[screenHash]?.appVersion

    override suspend fun getCommandsForScreen(screenHash: String): List<QuantizedCommand> =
        commands[screenHash] ?: emptyList()

    override suspend fun getScreenInfo(screenHash: String): ScreenInfo? = screens[screenHash]

    override suspend fun saveScreen(
        hash: String,
        packageName: String,
        activityName: String?,
        appVersion: String,
        elementCount: Int
    ) {
        screens[hash] = ScreenInfo(
            hash = hash,
            packageName = packageName,
            activityName = activityName,
            appVersion = appVersion,
            elementCount = elementCount,
            actionableCount = 0,
            commandCount = 0,
            scannedAt = System.currentTimeMillis(),
            isCached = false
        )
    }

    override suspend fun saveCommandsForScreen(screenHash: String, commands: List<QuantizedCommand>) {
        this.commands[screenHash] = commands
    }

    override suspend fun clearScreensForPackage(packageName: String): Int {
        val toRemove = screens.filter { it.value.packageName == packageName }.keys
        toRemove.forEach {
            screens.remove(it)
            commands.remove(it)
        }
        return toRemove.size
    }

    override suspend fun clearAllScreens(): Int {
        val count = screens.size
        screens.clear()
        commands.clear()
        return count
    }

    override suspend fun getScreenCount(): Int = screens.size

    override suspend fun getScreenCountForPackage(packageName: String): Int =
        screens.count { it.value.packageName == packageName }
}
