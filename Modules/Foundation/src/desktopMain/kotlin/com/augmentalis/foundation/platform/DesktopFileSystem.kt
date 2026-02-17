/*
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC.
 * All rights reserved.
 */

package com.augmentalis.foundation.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Desktop (JVM) implementation of [IFileSystem] using java.nio.file APIs.
 * Suspend operations are dispatched to [Dispatchers.IO].
 */
class DesktopFileSystem : IFileSystem {

    override fun getExternalStoragePath(): String? =
        System.getProperty("user.home")

    override fun getDocumentsPath(): String =
        Paths.get(System.getProperty("user.home"), "Documents").toString()

    override fun getAppFilesPath(): String =
        Paths.get(System.getProperty("user.home"), ".avanues").toString()

    override fun exists(path: String): Boolean =
        Files.exists(Paths.get(path))

    override suspend fun readText(path: String): String = withContext(Dispatchers.IO) {
        String(Files.readAllBytes(Paths.get(path)))
    }

    override suspend fun writeText(path: String, content: String): Unit = withContext(Dispatchers.IO) {
        val p = Paths.get(path)
        p.parent?.let { Files.createDirectories(it) }
        Files.write(p, content.toByteArray())
        Unit
    }

    override suspend fun delete(path: String): Boolean = withContext(Dispatchers.IO) {
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
