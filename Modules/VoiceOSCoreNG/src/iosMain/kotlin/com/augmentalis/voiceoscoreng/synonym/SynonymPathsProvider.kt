/**
 * SynonymPathsProvider.kt - iOS implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-08
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.augmentalis.voiceoscoreng.synonym

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.memcpy

/**
 * iOS-specific synonym paths provider.
 */
actual object SynonymPathsProvider {
    actual fun getPaths(): ISynonymPaths {
        return IOSSynonymPaths()
    }
}

/**
 * iOS synonym paths implementation.
 */
class IOSSynonymPaths : ISynonymPaths {

    private val documentsDir: String by lazy {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        )
        (paths.firstOrNull() as? String) ?: ""
    }

    override fun getBuiltinPath(language: String, binary: Boolean): String {
        val ext = if (binary) "qsyn" else "syn"
        return "synonyms/$language.$ext"
    }

    override fun getDownloadedPath(language: String, binary: Boolean): String {
        val ext = if (binary) "qsyn" else "syn"
        return "$documentsDir/VoiceOS/synonyms/$language.$ext"
    }

    override fun getCustomPath(language: String, binary: Boolean): String {
        val ext = if (binary) "qsyn" else "syn"
        return "$documentsDir/synonyms/custom/$language.$ext"
    }

    override fun getDownloadDirectory(): String {
        return "$documentsDir/VoiceOS/synonyms"
    }

    override fun getCustomDirectory(): String {
        return "$documentsDir/synonyms/custom"
    }

    override fun listAvailableLanguages(tier: SynonymTier): List<String> {
        val dirPath = when (tier) {
            SynonymTier.BUILTIN -> return DefaultSynonymPaths.BUILTIN_LANGUAGES
            SynonymTier.DOWNLOADED -> getDownloadDirectory()
            SynonymTier.CUSTOM -> getCustomDirectory()
        }

        val fileManager = NSFileManager.defaultManager
        @Suppress("UNCHECKED_CAST")
        val contents = fileManager.contentsOfDirectoryAtPath(dirPath, null) as? List<String>
            ?: return emptyList()

        return contents.mapNotNull { name ->
            if (name.endsWith(".syn") || name.endsWith(".qsyn")) {
                name.substringBeforeLast(".")
            } else null
        }.distinct()
    }
}

/**
 * iOS resource loader implementation.
 */
class IOSResourceLoader : IResourceLoader {

    override fun loadResource(path: String): ByteArray? {
        val bundle = NSBundle.mainBundle
        val components = path.split("/")
        val fileName = components.lastOrNull()?.substringBeforeLast(".") ?: return null
        val fileExt = components.lastOrNull()?.substringAfterLast(".") ?: return null
        val subdir = if (components.size > 1) components.dropLast(1).joinToString("/") else null

        val resourcePath = bundle.pathForResource(fileName, fileExt, subdir) ?: return null

        return try {
            NSData.dataWithContentsOfFile(resourcePath)?.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    override fun resourceExists(path: String): Boolean {
        return loadResource(path) != null
    }
}

/**
 * iOS file loader implementation.
 */
class IOSFileLoader : IFileLoader {

    private val fileManager = NSFileManager.defaultManager

    override fun readFile(path: String): ByteArray? {
        return try {
            NSData.dataWithContentsOfFile(path)?.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    override fun writeFile(path: String, data: ByteArray): Boolean {
        return try {
            val parent = path.substringBeforeLast("/")
            fileManager.createDirectoryAtPath(parent, true, null, null)

            data.toNSData().writeToFile(path, true)
        } catch (e: Exception) {
            false
        }
    }

    override fun fileExists(path: String): Boolean {
        return fileManager.fileExistsAtPath(path)
    }

    override fun listFiles(directory: String, extension: String): List<String> {
        @Suppress("UNCHECKED_CAST")
        val contents = fileManager.contentsOfDirectoryAtPath(directory, null) as? List<String>
            ?: return emptyList()

        return contents.filter { it.endsWith(".$extension") }
    }
}

// Extension functions for NSData <-> ByteArray conversion
@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    if (size == 0) return ByteArray(0)

    return ByteArray(size).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()

    return usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
    }
}
