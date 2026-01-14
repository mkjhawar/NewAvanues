package com.augmentalis.avaelements.core.runtime

import com.augmentalis.avaelements.core.*

/**
 * Desktop (JVM) implementations for expect/actual functions
 */

/**
 * Load plugin from file on desktop platform
 */
actual suspend fun expect_loadPluginFromFile(path: String): MagicElementPlugin {
    // TODO: Implement desktop file loading
    throw NotImplementedError("Desktop plugin loading from file not yet implemented")
}

/**
 * Load plugin from remote URL on desktop platform
 */
actual suspend fun expect_loadPluginFromRemote(url: String): MagicElementPlugin {
    // TODO: Implement desktop remote loading
    throw NotImplementedError("Desktop plugin loading from remote not yet implemented")
}

/**
 * Check permission on desktop platform
 */
actual fun expect_checkPermission(pluginId: String, permission: Permission): Boolean {
    // On desktop, permissions are less restrictive
    // Only block blacklisted permissions
    return permission !in Permission.BLACKLISTED
}

/**
 * Request permission on desktop platform
 */
actual suspend fun expect_requestPermission(pluginId: String, permission: Permission): Boolean {
    // On desktop, auto-grant non-blacklisted permissions
    // In production, this would show a dialog
    return permission !in Permission.BLACKLISTED
}

/**
 * Desktop (JVM) specific MEL plugin file content loading
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
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val file = java.io.File(path)
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
 * Desktop (JVM) specific MEL plugin remote content loading
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
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"

            // Add user agent to avoid blocks
            connection.setRequestProperty("User-Agent", "MagicElements/2.0")

            try {
                val responseCode = connection.responseCode
                if (responseCode != java.net.HttpURLConnection.HTTP_OK) {
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
