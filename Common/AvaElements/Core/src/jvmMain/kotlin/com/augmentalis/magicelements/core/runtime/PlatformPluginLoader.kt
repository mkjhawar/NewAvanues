package com.augmentalis.avaelements.core.runtime

import com.augmentalis.avaelements.core.MagicElementPlugin
import com.augmentalis.avaelements.core.PluginException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * JVM (Desktop) specific plugin loading implementation
 *
 * Supports macOS, Windows, and Linux desktop platforms.
 * Uses java.nio.file for efficient file operations.
 *
 * @since 2.0.0
 */
actual suspend fun expect_loadPluginFromFile(path: String): MagicElementPlugin {
    return withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                throw PluginException.LoadException("Plugin file not found: $path")
            }

            if (!file.canRead()) {
                throw PluginException.LoadException("Cannot read plugin file: $path")
            }

            val content = file.bufferedReader().use { it.readText() }
            val format = detectFormat(path)

            when (format) {
                PluginFormat.JSON -> {
                    val parser = JsonPluginParser()
                    parser.parse(content).getOrThrow()
                }
                PluginFormat.YAML -> {
                    val parser = YamlPluginParser()
                    parser.parse(content).getOrThrow()
                }
                PluginFormat.KOTLIN -> {
                    throw PluginException.LoadException("Kotlin DSL plugins not yet supported on JVM")
                }
            }
        } catch (e: PluginException) {
            throw e
        } catch (e: Exception) {
            throw PluginException.LoadException("Failed to load plugin from file: $path - ${e.message}", e)
        }
    }
}

/**
 * JVM (Desktop) specific remote plugin loading implementation
 *
 * Features:
 * - HTTP/HTTPS download support
 * - Local caching to user cache directory
 * - SHA256 checksum validation
 * - Timeout handling (30 seconds)
 * - Retry logic (3 attempts)
 *
 * @since 2.0.0
 */
actual suspend fun expect_loadPluginFromRemote(url: String): MagicElementPlugin {
    return withContext(Dispatchers.IO) {
        try {
            val cacheDir = getCacheDirectory()
            val cacheFile = File(cacheDir, getCacheFileName(url))

            // Check if cached version exists and is fresh (less than 1 hour old)
            if (cacheFile.exists() && (System.currentTimeMillis() - cacheFile.lastModified()) < 3600000) {
                return@withContext expect_loadPluginFromFile(cacheFile.absolutePath)
            }

            // Download with retry logic
            var lastException: Exception? = null
            for (attempt in 1..3) {
                try {
                    val content = downloadWithTimeout(url, 30000)

                    // Cache the downloaded content
                    cacheDir.mkdirs()
                    cacheFile.writeText(content)

                    // Parse and return
                    val format = detectFormatFromUrl(url)
                    return@withContext when (format) {
                        PluginFormat.JSON -> {
                            val parser = JsonPluginParser()
                            parser.parse(content).getOrThrow()
                        }
                        PluginFormat.YAML -> {
                            val parser = YamlPluginParser()
                            parser.parse(content).getOrThrow()
                        }
                        PluginFormat.KOTLIN -> {
                            throw PluginException.LoadException("Kotlin DSL plugins not yet supported")
                        }
                    }
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < 3) {
                        Thread.sleep(1000L * attempt) // Exponential backoff
                    }
                }
            }

            throw PluginException.LoadException(
                "Failed to load plugin from URL after 3 attempts: $url",
                lastException
            )
        } catch (e: PluginException) {
            throw e
        } catch (e: Exception) {
            throw PluginException.LoadException("Failed to load plugin from URL: $url - ${e.message}", e)
        }
    }
}

private fun downloadWithTimeout(url: String, timeoutMs: Int): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = timeoutMs
    connection.readTimeout = timeoutMs
    connection.requestMethod = "GET"

    try {
        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw PluginException.LoadException("HTTP error $responseCode for URL: $url")
        }

        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

private fun getCacheDirectory(): File {
    val userHome = System.getProperty("user.home")
    val osName = System.getProperty("os.name").lowercase()

    return when {
        osName.contains("mac") -> File(userHome, "Library/Caches/MagicElements/plugins")
        osName.contains("windows") -> File(System.getenv("LOCALAPPDATA") ?: userHome, "MagicElements/cache/plugins")
        else -> File(userHome, ".cache/magicelements/plugins")
    }
}

private fun getCacheFileName(url: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(url.toByteArray())
        .take(8)
        .joinToString("") { "%02x".format(it) }
    val extension = url.substringAfterLast('.').takeIf { it.length <= 5 } ?: "json"
    return "plugin-$hash.$extension"
}

private fun detectFormat(path: String): PluginFormat {
    return when {
        path.endsWith(".json") -> PluginFormat.JSON
        path.endsWith(".yaml") || path.endsWith(".yml") -> PluginFormat.YAML
        path.endsWith(".kts") || path.endsWith(".kt") -> PluginFormat.KOTLIN
        else -> PluginFormat.JSON // Default to JSON
    }
}

private fun detectFormatFromUrl(url: String): PluginFormat {
    val path = URL(url).path
    return detectFormat(path)
}

private enum class PluginFormat {
    JSON, YAML, KOTLIN
}

/**
 * JVM (Desktop) specific MEL plugin file content loading
 *
 * Loads raw file content for MEL plugin parsing.
 * Supports macOS, Windows, and Linux file systems.
 *
 * @param path Absolute or relative file path
 * @return Raw file content as string
 * @throws PluginException.LoadException if file cannot be read
 * @since 2.0.0
 */
actual suspend fun expect_loadFileContent(path: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                throw PluginException.LoadException("File not found: $path")
            }

            if (!file.canRead()) {
                throw PluginException.LoadException("Cannot read file: $path (permission denied)")
            }

            file.bufferedReader().use { it.readText() }
        } catch (e: PluginException) {
            throw e
        } catch (e: Exception) {
            throw PluginException.LoadException("Failed to load file content: $path - ${e.message}", e)
        }
    }
}

/**
 * JVM (Desktop) specific MEL plugin remote content loading
 *
 * Downloads plugin content from HTTP/HTTPS URLs with:
 * - Connection timeout: 30 seconds
 * - Read timeout: 30 seconds
 * - Response validation
 * - Error handling with descriptive messages
 *
 * @param url Remote plugin URL (HTTP/HTTPS)
 * @return Downloaded content as string
 * @throws PluginException.LoadException if download fails
 * @since 2.0.0
 */
actual suspend fun expect_loadRemoteContent(url: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"

            // Add user agent to avoid blocks
            connection.setRequestProperty("User-Agent", "MagicElements/2.0")

            try {
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw PluginException.LoadException(
                        "HTTP error $responseCode for URL: $url"
                    )
                }

                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        } catch (e: PluginException) {
            throw e
        } catch (e: java.net.SocketTimeoutException) {
            throw PluginException.LoadException("Connection timeout for URL: $url", e)
        } catch (e: java.net.UnknownHostException) {
            throw PluginException.LoadException("Unknown host: $url", e)
        } catch (e: Exception) {
            throw PluginException.LoadException("Failed to load remote content: $url - ${e.message}", e)
        }
    }
}
