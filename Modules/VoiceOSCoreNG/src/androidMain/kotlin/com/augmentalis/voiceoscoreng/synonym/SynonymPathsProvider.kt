/**
 * SynonymPathsProvider.kt - Android implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-08
 */
package com.augmentalis.voiceoscoreng.synonym

import android.content.Context
import java.io.File

/**
 * Android-specific synonym paths provider.
 */
actual object SynonymPathsProvider {
    private var paths: ISynonymPaths? = null
    private var context: Context? = null

    /**
     * Initialize with Android context.
     * Call this during app startup.
     */
    fun initialize(context: Context) {
        this.context = context.applicationContext
        this.paths = AndroidSynonymPaths(context.applicationContext)
    }

    actual fun getPaths(): ISynonymPaths {
        return paths ?: throw IllegalStateException(
            "SynonymPathsProvider not initialized. Call initialize(context) first."
        )
    }
}

/**
 * Android synonym paths implementation.
 */
class AndroidSynonymPaths(private val context: Context) : ISynonymPaths {

    private val filesDir = context.filesDir.absolutePath
    private val externalDir = context.getExternalFilesDir(null)?.absolutePath

    override fun getBuiltinPath(language: String, binary: Boolean): String {
        val ext = if (binary) "qsyn" else "syn"
        return "synonyms/$language.$ext"
    }

    override fun getDownloadedPath(language: String, binary: Boolean): String {
        val ext = if (binary) "qsyn" else "syn"
        val baseDir = externalDir ?: filesDir
        return "$baseDir/VoiceOS/synonyms/$language.$ext"
    }

    override fun getCustomPath(language: String, binary: Boolean): String {
        val ext = if (binary) "qsyn" else "syn"
        return "$filesDir/synonyms/custom/$language.$ext"
    }

    override fun getDownloadDirectory(): String {
        val baseDir = externalDir ?: filesDir
        return "$baseDir/VoiceOS/synonyms"
    }

    override fun getCustomDirectory(): String {
        return "$filesDir/synonyms/custom"
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
 * Android resource loader implementation.
 */
class AndroidResourceLoader(private val context: Context) : IResourceLoader {

    override fun loadResource(path: String): ByteArray? {
        return try {
            context.assets.open(path).use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    override fun resourceExists(path: String): Boolean {
        return try {
            context.assets.open(path).close()
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Android file loader implementation.
 */
class AndroidFileLoader : IFileLoader {

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
