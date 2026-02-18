/*
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC.
 * All rights reserved.
 */
package com.augmentalis.foundation.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSLibraryDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class)
class IosFileSystem : IFileSystem {
    private val fileManager = NSFileManager.defaultManager

    override fun getExternalStoragePath(): String? = getDocumentsPath()

    override fun getDocumentsPath(): String {
        @Suppress("UNCHECKED_CAST")
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ) as List<String>
        return paths.firstOrNull() ?: ""
    }

    override fun getAppFilesPath(): String {
        @Suppress("UNCHECKED_CAST")
        val paths = NSSearchPathForDirectoriesInDomains(
            NSLibraryDirectory,
            NSUserDomainMask,
            true
        ) as List<String>
        return paths.firstOrNull() ?: ""
    }

    override fun exists(path: String): Boolean = fileManager.fileExistsAtPath(path)

    override suspend fun readText(path: String): String = withContext(Dispatchers.Default) {
        @Suppress("CAST_NEVER_SUCCEEDS")
        val text = NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null) as? String
        text ?: throw IllegalStateException("Cannot read file: $path")
    }

    override suspend fun writeText(path: String, content: String): Unit = withContext(Dispatchers.Default) {
        val data = (content as NSString).dataUsingEncoding(NSUTF8StringEncoding)
            ?: throw IllegalStateException("Cannot encode content as UTF-8")
        val success = data.writeToFile(path, atomically = true)
        if (!success) throw IllegalStateException("Cannot write file: $path")
    }

    override suspend fun delete(path: String): Boolean = withContext(Dispatchers.Default) {
        if (!fileManager.fileExistsAtPath(path)) return@withContext false
        fileManager.removeItemAtPath(path, error = null)
    }

    override suspend fun listFiles(directoryPath: String): List<String> = withContext(Dispatchers.Default) {
        @Suppress("UNCHECKED_CAST")
        val contents = fileManager.contentsOfDirectoryAtPath(directoryPath, error = null) as? List<String>
            ?: throw IllegalStateException("Cannot list directory: $directoryPath")
        contents
    }

    override suspend fun createDirectories(path: String): Boolean = withContext(Dispatchers.Default) {
        fileManager.createDirectoryAtPath(
            path,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
    }
}
