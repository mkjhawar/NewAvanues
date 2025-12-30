package com.augmentalis.avaelements.core.runtime

import com.augmentalis.avaelements.core.MagicElementPlugin
import com.augmentalis.avaelements.core.PluginException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Android-specific implementation for loading plugin content from files.
 *
 * Loads file content from the Android file system using standard Java File APIs.
 *
 * @param path Absolute or relative file path
 * @return File content as string
 * @throws PluginException.LoadException if file cannot be read
 */
actual suspend fun expect_loadFileContent(path: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                throw PluginException.LoadException("File not found: $path")
            }
            if (!file.canRead()) {
                throw PluginException.LoadException("File not readable: $path")
            }
            file.readText()
        } catch (e: PluginException) {
            throw e
        } catch (e: Exception) {
            throw PluginException.LoadException(
                "Failed to read file '$path': ${e.message}",
                e
            )
        }
    }
}

/**
 * Android-specific implementation for loading plugin content from remote URLs.
 *
 * Uses HttpURLConnection for HTTP/HTTPS requests with proper timeout configuration.
 *
 * @param url Remote URL (http:// or https://)
 * @return Remote content as string
 * @throws PluginException.LoadException if remote content cannot be fetched
 */
actual suspend fun expect_loadRemoteContent(url: String): String {
    return withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 30000 // 30 seconds
                readTimeout = 30000    // 30 seconds
                setRequestProperty("Accept", "application/json, application/yaml, text/yaml")
                setRequestProperty("User-Agent", "AvaElements-Plugin-Loader/2.0")
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorMessage = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }
                } catch (e: Exception) {
                    "HTTP $responseCode"
                }
                throw PluginException.LoadException(
                    "Failed to fetch plugin from '$url': $errorMessage"
                )
            }

            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: PluginException) {
            throw e
        } catch (e: Exception) {
            throw PluginException.LoadException(
                "Failed to load plugin from remote '$url': ${e.message}",
                e
            )
        } finally {
            connection?.disconnect()
        }
    }
}

/**
 * Android-specific implementation for loading complete plugin from file.
 *
 * This is the legacy implementation that loads and parses the plugin in one step.
 * New code should use loadMELPlugin() from PluginLoader for MEL-based plugins.
 *
 * @param path File path
 * @return Parsed MagicElementPlugin
 * @throws PluginException.LoadException if plugin cannot be loaded or parsed
 */
actual suspend fun expect_loadPluginFromFile(path: String): MagicElementPlugin {
    return withContext(Dispatchers.IO) {
        try {
            val content = expect_loadFileContent(path)
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
                PluginFormat.KOTLIN_DSL -> {
                    throw PluginException.LoadException(
                        "Kotlin DSL plugins not yet supported on Android"
                    )
                }
            }
        } catch (e: PluginException) {
            throw e
        } catch (e: Exception) {
            throw PluginException.LoadException(
                "Failed to load plugin from file '$path': ${e.message}",
                e
            )
        }
    }
}

/**
 * Android-specific implementation for loading complete plugin from remote URL.
 *
 * This is the legacy implementation that loads and parses the plugin in one step.
 * New code should use loadMELPlugin() from PluginLoader for MEL-based plugins.
 *
 * @param url Remote URL
 * @return Parsed MagicElementPlugin
 * @throws PluginException.LoadException if plugin cannot be loaded or parsed
 */
actual suspend fun expect_loadPluginFromRemote(url: String): MagicElementPlugin {
    return withContext(Dispatchers.IO) {
        try {
            val content = expect_loadRemoteContent(url)
            val format = detectFormat(url)

            when (format) {
                PluginFormat.JSON -> {
                    val parser = JsonPluginParser()
                    parser.parse(content).getOrThrow()
                }
                PluginFormat.YAML -> {
                    val parser = YamlPluginParser()
                    parser.parse(content).getOrThrow()
                }
                PluginFormat.KOTLIN_DSL -> {
                    throw PluginException.LoadException(
                        "Kotlin DSL plugins not yet supported for remote loading"
                    )
                }
            }
        } catch (e: PluginException) {
            throw e
        } catch (e: Exception) {
            throw PluginException.LoadException(
                "Failed to load plugin from remote '$url': ${e.message}",
                e
            )
        }
    }
}

/**
 * Detect plugin format from file path or URL.
 *
 * @param path File path or URL
 * @return Detected plugin format
 */
private fun detectFormat(path: String): PluginFormat {
    return when {
        path.endsWith(".json") -> PluginFormat.JSON
        path.endsWith(".yaml") || path.endsWith(".yml") -> PluginFormat.YAML
        path.endsWith(".kts") || path.endsWith(".kt") -> PluginFormat.KOTLIN_DSL
        else -> PluginFormat.JSON // Default to JSON
    }
}

/**
 * Plugin format enumeration.
 */
enum class PluginFormat {
    /** JSON format */
    JSON,

    /** YAML format */
    YAML,

    /** Kotlin DSL format */
    KOTLIN_DSL
}

/**
 * Placeholder parser classes - these should be implemented or imported from proper locations
 */
private class JsonPluginParser {
    fun parse(content: String): Result<MagicElementPlugin> {
        // TODO: Implement JSON parsing or use existing parser
        return Result.failure(PluginException.LoadException("JSON parsing not yet implemented"))
    }
}

private class YamlPluginParser {
    fun parse(content: String): Result<MagicElementPlugin> {
        // TODO: Implement YAML parsing or use existing parser
        return Result.failure(PluginException.LoadException("YAML parsing not yet implemented"))
    }

    fun toJson(yaml: String): String {
        // TODO: Implement YAML to JSON conversion
        throw PluginException.LoadException("YAML to JSON conversion not yet implemented")
    }
}
