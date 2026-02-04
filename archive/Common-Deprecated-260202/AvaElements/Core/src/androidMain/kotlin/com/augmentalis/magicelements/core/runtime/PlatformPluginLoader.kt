package com.augmentalis.avaelements.core.runtime

import com.augmentalis.avaelements.core.MagicElementPlugin
import com.augmentalis.avaelements.core.PluginException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android-specific plugin loading implementation
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

            val content = file.bufferedReader().use { it.readText() }
            val format = detectFormat(path)

            when (format) {
                PluginFormat.JSON -> JsonPluginParser().parse(content).getOrThrow()
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

private fun detectFormat(path: String): PluginFormat {
    return when {
        path.endsWith(".json") -> PluginFormat.JSON
        path.endsWith(".yaml") || path.endsWith(".yml") -> PluginFormat.YAML
        path.endsWith(".kts") -> PluginFormat.KOTLIN_DSL
        else -> PluginFormat.JSON // default
    }
}

/**
 * Android-specific remote plugin loading implementation
 *
 * @since 2.0.0
 */
actual suspend fun expect_loadPluginFromRemote(url: String): MagicElementPlugin {
    return withContext(Dispatchers.IO) {
        try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            if (connection.responseCode != 200) {
                throw PluginException.LoadException("HTTP error: ${connection.responseCode}")
            }

            val content = connection.inputStream.bufferedReader().use { it.readText() }
            val format = detectFormat(url)

            when (format) {
                PluginFormat.JSON -> JsonPluginParser().parse(content).getOrThrow()
                PluginFormat.YAML -> throw PluginException.LoadException("YAML parsing not yet implemented")
                PluginFormat.KOTLIN_DSL -> throw PluginException.LoadException("Kotlin DSL not yet implemented")
            }
        } catch (e: PluginException) {
            throw e
        } catch (e: Exception) {
            throw PluginException.LoadException("Failed to load plugin from remote: ${e.message}", e)
        }
    }
}
