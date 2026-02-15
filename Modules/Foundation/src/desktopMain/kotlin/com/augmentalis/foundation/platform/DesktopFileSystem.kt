/*
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC.
 * All rights reserved.
 */

package com.augmentalis.foundation.platform

import com.augmentalis.foundation.IFileSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Desktop (JVM) implementation of [IFileSystem] using java.nio.file APIs.
 * All I/O operations are dispatched to [Dispatchers.IO].
 */
class DesktopFileSystem : IFileSystem {

    override suspend fun getExternalStoragePath(): String =
        System.getProperty("user.home") ?: throw IllegalStateException("user.home not available")

    override suspend fun getDocumentsPath(): String =
        Paths.get(System.getProperty("user.home"), "Documents").toString()

    override suspend fun getAppFilesPath(): String =
        Paths.get(System.getProperty("user.home"), ".avanues").toString()

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        Files.exists(Paths.get(path))
    }

    override suspend fun readText(path: String): String = withContext(Dispatchers.IO) {
        String(Files.readAllBytes(Paths.get(path)))
    }

    override suspend fun writeText(path: String, content: String) = withContext(Dispatchers.IO) {
        val p = Paths.get(path)
        p.parent?.let { Files.createDirectories(it) }
        Files.write(p, content.toByteArray())
    }

    override suspend fun delete(path: String) = withContext(Dispatchers.IO) {
        Files.deleteIfExists(Paths.get(path))
    }

    override suspend fun listFiles(directoryPath: String): List<String> = withContext(Dispatchers.IO) {
        Files.list(Paths.get(directoryPath)).use { stream ->
            stream.map { it.fileName.toString() }.toList()
        }
    }

    override suspend fun createDirectories(path: String): Boolean = withContext(Dispatchers.IO) {
        Files.createDirectories(Paths.get(path))
        true
    }
}
