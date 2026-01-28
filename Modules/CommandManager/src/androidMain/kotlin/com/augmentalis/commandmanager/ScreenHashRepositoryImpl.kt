/**
 * ScreenHashRepositoryImpl.kt - In-memory implementation of ScreenHashRepository
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-19
 *
 * Provides in-memory caching of screen hashes and commands for quick lookup.
 * This avoids database overhead for frequently accessed screen cache data.
 */
package com.augmentalis.commandmanager

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of ScreenHashRepository.
 *
 * Uses ConcurrentHashMap for thread-safe screen hash caching.
 * Commands are stored per-screen for instant loading when returning to known screens.
 */
class ScreenHashRepositoryImpl : ScreenHashRepository {

    // Screen hash -> ScreenInfo
    private val screenCache = ConcurrentHashMap<String, ScreenInfo>()

    // Screen hash -> List<QuantizedCommand>
    private val commandCache = ConcurrentHashMap<String, List<QuantizedCommand>>()

    override suspend fun hasScreen(hash: String): Boolean {
        return screenCache.containsKey(hash)
    }

    override suspend fun getAppVersion(hash: String): String? {
        return screenCache[hash]?.appVersion
    }

    override suspend fun getCommandsForScreen(hash: String): List<QuantizedCommand> {
        return commandCache[hash] ?: emptyList()
    }

    override suspend fun getScreenInfo(hash: String): ScreenInfo? {
        return screenCache[hash]
    }

    override suspend fun saveScreen(
        hash: String,
        packageName: String,
        activityName: String?,
        appVersion: String,
        elementCount: Int
    ) {
        val existing = screenCache[hash]
        val commandCount = commandCache[hash]?.size ?: 0

        screenCache[hash] = ScreenInfo(
            hash = hash,
            packageName = packageName,
            activityName = activityName,
            appVersion = appVersion,
            elementCount = elementCount,
            actionableCount = existing?.actionableCount ?: 0,
            commandCount = commandCount,
            scannedAt = System.currentTimeMillis(),
            isCached = false
        )
    }

    override suspend fun saveCommandsForScreen(hash: String, commands: List<QuantizedCommand>) {
        commandCache[hash] = commands

        // Update command count in screen info
        screenCache[hash]?.let { info ->
            screenCache[hash] = info.copy(commandCount = commands.size)
        }
    }

    override suspend fun clearScreen(hash: String) {
        screenCache.remove(hash)
        commandCache.remove(hash)
    }

    override suspend fun clearScreensForPackage(packageName: String): Int {
        val toRemove = screenCache.entries
            .filter { it.value.packageName == packageName }
            .map { it.key }

        toRemove.forEach { hash ->
            screenCache.remove(hash)
            commandCache.remove(hash)
        }

        return toRemove.size
    }

    override suspend fun clearAllScreens(): Int {
        val count = screenCache.size
        screenCache.clear()
        commandCache.clear()
        return count
    }

    override suspend fun getScreenCount(): Int {
        return screenCache.size
    }

    override suspend fun getScreenCountForPackage(packageName: String): Int {
        return screenCache.values.count { it.packageName == packageName }
    }
}
