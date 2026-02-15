/*
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC.
 * All rights reserved.
 */
package com.augmentalis.foundation.platform

import com.augmentalis.foundation.IFileSystem
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
        val data = NSData.dataWithContentsOfFile(path)
            ?: throw IllegalStateException("Cannot read file: $path")
        NSString.create(data = data, encoding = NSUTF8StringEncoding) as? String
            ?: throw IllegalStateException("Cannot decode file as UTF-8: $path")
    }

    override suspend fun writeText(path: String, content: String): Unit = withContext(Dispatchers.Default) {
        val nsString = content as NSString
        val success = nsString.writeToFile(
            path,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
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
