package com.augmentalis.avaelements.core.runtime

import com.augmentalis.avaelements.core.MagicElementPlugin
import com.augmentalis.avaelements.core.PluginException
import com.augmentalis.avaelements.core.PluginFormat
import com.augmentalis.avaelements.core.json.JsonPluginParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import platform.Foundation.*

/**
 * iOS-specific plugin loading implementation
 *
 * @since 2.0.0
 */
actual suspend fun expect_loadPluginFromFile(path: String): MagicElementPlugin {
    return withContext(Dispatchers.Default) {
        try {
            val fileManager = NSFileManager.defaultManager

            if (!fileManager.fileExistsAtPath(path)) {
                throw PluginException.LoadException("Plugin file not found: $path")
            }

            val content = NSString.stringWithContentsOfFile(
                path,
                encoding = NSUTF8StringEncoding,
                error = null
            ) ?: throw PluginException.LoadException("Failed to read file: $path")

            val format = detectFormat(path)

            when (format) {
                PluginFormat.JSON -> JsonPluginParser().parse(content.toString()).getOrThrow()
                PluginFormat.YAML -> throw PluginException.LoadException("YAML parsing not yet implemented")
                PluginFormat.KOTLIN_DSL -> throw PluginException.LoadException("Kotlin DSL not yet implemented")
            }
        } catch (e: PluginException) {
            throw e
        } catch (e: Exception) {
            throw PluginException.LoadException("Failed to load plugin from file: ${e.message}", e)
        }
    }
}

/**
 * iOS-specific remote plugin loading implementation
 *
 * @since 2.0.0
 */
actual suspend fun expect_loadPluginFromRemote(url: String): MagicElementPlugin {
    return suspendCancellableCoroutine { continuation ->
        val nsUrl = NSURL.URLWithString(url)
            ?: run {
                continuation.resumeWithException(PluginException.LoadException("Invalid URL: $url"))
                return@suspendCancellableCoroutine
            }

        val session = NSURLSession.sharedSession
        val task = session.dataTaskWithURL(nsUrl) { data, response, error ->
            when {
                error != null -> {
                    continuation.resumeWithException(
                        PluginException.LoadException("Network error: ${error.localizedDescription}")
                    )
                }
                data == null -> {
                    continuation.resumeWithException(
                        PluginException.LoadException("No data received")
                    )
                }
                else -> {
                    try {
                        val content = NSString.create(
                            data = data,
                            encoding = NSUTF8StringEncoding
                        )?.toString() ?: throw PluginException.LoadException("Failed to decode response")

                        val format = detectFormat(url)
                        val plugin = when (format) {
                            PluginFormat.JSON -> JsonPluginParser().parse(content).getOrThrow()
                            else -> throw PluginException.LoadException("Only JSON supported for remote")
                        }
                        continuation.resume(plugin)
                    } catch (e: Exception) {
                        continuation.resumeWithException(
                            PluginException.LoadException("Parse error: ${e.message}", e)
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

/**
 * Detects plugin format from file path or URL
 */
private fun detectFormat(path: String): PluginFormat {
    return when {
        path.endsWith(".json") -> PluginFormat.JSON
        path.endsWith(".yaml") || path.endsWith(".yml") -> PluginFormat.YAML
        path.endsWith(".kts") -> PluginFormat.KOTLIN_DSL
        else -> PluginFormat.JSON
    }
}
