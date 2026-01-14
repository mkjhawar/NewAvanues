package com.augmentalis.ava.platform

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.io.FileNotFoundException

/**
 * Android implementation of FileSystem.
 */
actual class FileSystem actual constructor() {
    private lateinit var context: Context

    internal fun init(context: Context) {
        this.context = context.applicationContext
    }

    actual fun getFilesDirectory(): String {
        return context.filesDir.absolutePath
    }

    actual fun getCacheDirectory(): String {
        return context.cacheDir.absolutePath
    }

    actual fun getDocumentsDirectory(): String {
        return context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.absolutePath
            ?: context.filesDir.absolutePath
    }

    actual fun exists(path: String): Boolean {
        return File(path).exists()
    }

    actual fun readBytes(path: String): ByteArray {
        val file = File(path)
        if (!file.exists()) {
            throw FileNotFoundException("File not found: $path")
        }
        return file.readBytes()
    }

    actual fun readText(path: String): String {
        val file = File(path)
        if (!file.exists()) {
            throw FileNotFoundException("File not found: $path")
        }
        return file.readText(Charsets.UTF_8)
    }

    actual fun writeBytes(path: String, data: ByteArray) {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeBytes(data)
    }

    actual fun writeText(path: String, text: String) {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(text, Charsets.UTF_8)
    }

    actual fun delete(path: String): Boolean {
        val file = File(path)
        return if (file.exists()) {
            file.deleteRecursively()
        } else {
            false
        }
    }

    actual fun mkdirs(path: String): Boolean {
        val dir = File(path)
        return dir.exists() || dir.mkdirs()
    }

    actual fun listFiles(directory: String): List<String> {
        val dir = File(directory)
        return dir.listFiles()?.map { it.name } ?: emptyList()
    }

    actual fun getFileSize(path: String): Long {
        val file = File(path)
        return if (file.exists()) file.length() else -1L
    }

    actual fun getAvailableSpace(): Long {
        val stat = StatFs(context.filesDir.absolutePath)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    companion object {
        private var appContext: Context? = null

        fun initialize(context: Context) {
            appContext = context.applicationContext
        }

        internal fun getContext(): Context {
            return appContext ?: throw IllegalStateException(
                "FileSystem not initialized. Call FileSystem.initialize(context) in Application.onCreate()"
            )
        }
    }
}

/**
 * Android factory for FileSystem.
 */
actual object FileSystemFactory {
    actual fun create(): FileSystem {
        return FileSystem().apply {
            init(FileSystem.getContext())
        }
    }
}
