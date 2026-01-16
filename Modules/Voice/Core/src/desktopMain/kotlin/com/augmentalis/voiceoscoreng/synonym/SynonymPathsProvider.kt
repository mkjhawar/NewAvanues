/**
 * SynonymPathsProvider.kt - Desktop/JVM implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-08
 */
package com.augmentalis.voiceoscoreng.synonym

import java.io.File

/**
 * Desktop-specific synonym paths provider.
 */
actual object SynonymPathsProvider {
    actual fun getPaths(): ISynonymPaths {
        return DesktopSynonymPaths()
    }
}

/**
 * Desktop synonym paths implementation.
 */
class DesktopSynonymPaths : ISynonymPaths {

    private val userHome = System.getProperty("user.home")
    private val appDataDir = when {
        System.getProperty("os.name").lowercase().contains("mac") ->
            "$userHome/Library/Application Support/VoiceOS"
        System.getProperty("os.name").lowercase().contains("win") ->
            System.getenv("APPDATA")?.let { "$it/VoiceOS" } ?: "$userHome/VoiceOS"
        else -> "$userHome/.voiceos"
    }

    override fun getBuiltinPath(language: String, binary: Boolean): String {
        val ext = if (binary) "qsyn" else "syn"
        return "synonyms/$language.$ext"
    }

    override fun getDownloadedPath(language: String, binary: Boolean): String {
        val ext = if (binary) "qsyn" else "syn"
        return "$appDataDir/synonyms/$language.$ext"
    }

    override fun getCustomPath(language: String, binary: Boolean): String {
        val ext = if (binary) "qsyn" else "syn"
        return "$appDataDir/synonyms/custom/$language.$ext"
    }

    override fun getDownloadDirectory(): String {
        return "$appDataDir/synonyms"
    }

    override fun getCustomDirectory(): String {
        return "$appDataDir/synonyms/custom"
    }

    override fun listAvailableLanguages(tier: SynonymTier): List<String> {
        val dir = when (tier) {
            SynonymTier.BUILTIN -> return DefaultSynonymPaths.BUILTIN_LANGUAGES
            SynonymTier.DOWNLOADED -> File(getDownloadDirectory())
            SynonymTier.CUSTOM -> File(getCustomDirectory())
        }

        if (!dir.exists() || !dir.isDirectory) return emptyList()

        return dir.listFiles { file ->
            file.extension == "syn" || file.extension == "qsyn"
        }?.map { it.nameWithoutExtension }?.distinct() ?: emptyList()
    }
}

/**
 * Desktop resource loader implementation.
 */
class DesktopResourceLoader : IResourceLoader {

    override fun loadResource(path: String): ByteArray? {
        return try {
            val classLoader = Thread.currentThread().contextClassLoader
                ?: this::class.java.classLoader

            classLoader?.getResourceAsStream(path)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    override fun resourceExists(path: String): Boolean {
        val classLoader = Thread.currentThread().contextClassLoader
            ?: this::class.java.classLoader
        return classLoader?.getResource(path) != null
    }
}

/**
 * Desktop file loader implementation.
 */
class DesktopFileLoader : IFileLoader {

    override fun readFile(path: String): ByteArray? {
        return try {
            File(path).readBytes()
        } catch (e: Exception) {
            null
        }
    }

    override fun writeFile(path: String, data: ByteArray): Boolean {
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeBytes(data)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun fileExists(path: String): Boolean {
        return File(path).exists()
    }

    override fun listFiles(directory: String, extension: String): List<String> {
        val dir = File(directory)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        return dir.listFiles { file ->
            file.extension == extension
        }?.map { it.name } ?: emptyList()
    }
}
