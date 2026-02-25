package com.augmentalis.fileavanue

/**
 * Platform factory for creating the local storage provider.
 *
 * Each platform implements this with its native file system API:
 * - Android: MediaStore + java.io.File
 * - Desktop: java.nio.file
 * - iOS: NSFileManager
 * - Web: File System Access API + IndexedDB
 */
expect fun createLocalStorageProvider(): IStorageProvider
