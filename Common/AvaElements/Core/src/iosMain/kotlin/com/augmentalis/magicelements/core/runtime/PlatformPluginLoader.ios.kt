package com.augmentalis.avaelements.core.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import platform.Foundation.*

/**
 * iOS-specific implementation for MEL plugin content loading
 *
 * @since 2.0.0
 */

/**
 * Load file content from iOS filesystem
 *
 * @param path File path to load
 * @return File contents as string
 */
actual suspend fun expect_loadFileContent(path: String): String {
    return withContext(Dispatchers.Default) {
        val fileManager = NSFileManager.defaultManager

        if (!fileManager.fileExistsAtPath(path)) {
            throw IllegalArgumentException("File not found: $path")
        }

        val content = NSString.stringWithContentsOfFile(
            path,
            encoding = NSUTF8StringEncoding,
            error = null
        ) ?: throw IllegalStateException("Failed to read file: $path")

        content.toString()
    }
}

/**
 * Load content from remote URL
 *
 * @param url Remote URL to fetch
 * @return Response content as string
 */
actual suspend fun expect_loadRemoteContent(url: String): String {
    return suspendCancellableCoroutine { continuation ->
        val nsUrl = NSURL.URLWithString(url)
            ?: run {
                continuation.resumeWithException(IllegalArgumentException("Invalid URL: $url"))
                return@suspendCancellableCoroutine
            }

        val session = NSURLSession.sharedSession
        val task = session.dataTaskWithURL(nsUrl) { data, response, error ->
            when {
                error != null -> {
                    continuation.resumeWithException(
                        IllegalStateException("Network error: ${error.localizedDescription}")
                    )
                }
                data == null -> {
                    continuation.resumeWithException(
                        IllegalStateException("No data received from URL: $url")
                    )
                }
                else -> {
                    val content = NSString.create(
                        data = data,
                        encoding = NSUTF8StringEncoding
                    )?.toString()

                    if (content != null) {
                        continuation.resume(content)
                    } else {
                        continuation.resumeWithException(
                            IllegalStateException("Failed to decode response from URL: $url")
                        )
                    }
                }
            }
        }
        task.resume()

        continuation.invokeOnCancellation {
            task.cancel()
        }
    }
}
