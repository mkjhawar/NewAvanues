# Chapter 15: Plugin System

**Version:** 5.3.0
**Date:** 2025-11-02
**Author:** Manoj Jhawar, manoj@ideahq.net
**Word Count:** ~2,500 words

---

## Overview

The Plugin System enables extending IDEAMagic with custom components, generators, and functionality without modifying core code.

## Plugin Architecture

```
┌─────────────────────────────────────────────────────┐
│                  Plugin Manager                     │
│  - Discovery                                        │
│  - Loading                                          │
│  - Lifecycle                                        │
└────────────┬────────────────────────────────────────┘
             │
     ┌───────┴────────┬──────────────┬────────────────┐
     │                │              │                │
┌────▼─────┐  ┌──────▼─────┐  ┌────▼─────┐  ┌──────▼─────┐
│Component │  │  Generator │  │  Theme   │  │   Utility  │
│ Plugin   │  │   Plugin   │  │  Plugin  │  │   Plugin   │
└──────────┘  └────────────┘  └──────────┘  └────────────┘
```

## Plugin Interface

```kotlin
// Plugin.kt

interface AvaUIPlugin {
    val id: String
    val name: String
    val version: String
    val author: String
    val description: String

    fun initialize(context: PluginContext)
    fun shutdown()
}

interface ComponentPlugin : AvaUIPlugin {
    fun registerComponents(registry: ComponentRegistry)
}

interface GeneratorPlugin : AvaUIPlugin {
    fun createGenerator(platform: Platform): CodeGenerator?
}

interface ThemePlugin : AvaUIPlugin {
    fun registerThemes(themeManager: ThemeManager)
}
```

## Plugin Manifest

```json
{
  "id": "com.example.custom-components",
  "name": "Custom Components Pack",
  "version": "1.0.0",
  "author": "John Doe",
  "description": "Collection of custom UI components",
  "type": "component",
  "main": "CustomComponentsPlugin",
  "dependencies": {
    "avamagic": "^5.0.0"
  },
  "components": [
    {
      "type": "CustomCard",
      "name": "Custom Card",
      "properties": {
        "title": { "type": "string", "required": true },
        "elevation": { "type": "int", "default": 4 }
      }
    }
  ]
}
```

## Component Plugin Example

```kotlin
// CustomComponentsPlugin.kt

class CustomComponentsPlugin : ComponentPlugin {
    override val id = "com.example.custom-components"
    override val name = "Custom Components Pack"
    override val version = "1.0.0"
    override val author = "John Doe"
    override val description = "Collection of custom UI components"

    override fun initialize(context: PluginContext) {
        context.logger.info("Initializing $name")
    }

    override fun registerComponents(registry: ComponentRegistry) {
        registry.register(ComponentDescriptor(
            type = "CustomCard",
            properties = mapOf(
                "title" to PropertyDescriptor("title", PropertyType.STRING, required = true),
                "subtitle" to PropertyDescriptor("subtitle", PropertyType.STRING, required = false),
                "elevation" to PropertyDescriptor("elevation", PropertyType.INT, required = false, default = 4)
            ),
            supportsChildren = true,
            category = "custom"
        ))

        registry.register(ComponentDescriptor(
            type = "CustomChart",
            properties = mapOf(
                "type" to PropertyDescriptor("type", PropertyType.ENUM, required = true, options = listOf("bar", "line", "pie")),
                "data" to PropertyDescriptor("data", PropertyType.LIST, required = true)
            ),
            supportsChildren = false,
            category = "custom"
        ))
    }

    override fun shutdown() {
        // Cleanup
    }
}
```

## Generator Plugin Example

```kotlin
// VueGeneratorPlugin.kt

class VueGeneratorPlugin : GeneratorPlugin {
    override val id = "com.example.vue-generator"
    override val name = "Vue.js Generator"
    override val version = "1.0.0"
    override val author = "Jane Smith"
    override val description = "Generates Vue.js components"

    override fun initialize(context: PluginContext) {
        // Setup
    }

    override fun createGenerator(platform: Platform): CodeGenerator? {
        return if (platform == Platform.WEB) {
            VueComponentGenerator()
        } else null
    }

    override fun shutdown() {
        // Cleanup
    }
}

class VueComponentGenerator : CodeGenerator {
    override fun generate(screen: ScreenNode): GeneratedCode {
        val code = buildString {
            appendLine("<template>")
            appendLine("  <div>")
            generateComponent(screen.root, this, indent = 2)
            appendLine("  </div>")
            appendLine("</template>")
            appendLine()
            appendLine("<script setup lang=\"ts\">")
            screen.stateVariables.forEach { stateVar ->
                appendLine("const ${stateVar.name} = ref(${formatValue(stateVar.initialValue)})")
            }
            appendLine("</script>")
        }

        return GeneratedCode(
            code = code,
            language = Language.TYPESCRIPT,
            platform = Platform.WEB
        )
    }
}
```

## Plugin Manager

```kotlin
// PluginManager.kt

class PluginManager {
    private val plugins = mutableMapOf<String, AvaUIPlugin>()
    private val pluginClassLoaders = mutableMapOf<String, ClassLoader>()

    fun loadPlugin(pluginPath: String): Result<AvaUIPlugin> {
        try {
            // 1. Read manifest
            val manifestPath = "$pluginPath/plugin.json"
            val manifest = Json.decodeFromString<PluginManifest>(
                FileIO.readFile(manifestPath)
            )

            // 2. Load plugin class
            val classLoader = URLClassLoader(arrayOf(URL("file://$pluginPath/")))
            val pluginClass = classLoader.loadClass(manifest.main)
            val plugin = pluginClass.getDeclaredConstructor().newInstance() as AvaUIPlugin

            // 3. Validate
            if (plugin.id != manifest.id) {
                return Result.failure(Exception("Plugin ID mismatch"))
            }

            // 4. Initialize
            plugin.initialize(PluginContext(this))

            // 5. Register
            plugins[plugin.id] = plugin
            pluginClassLoaders[plugin.id] = classLoader

            return Result.success(plugin)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun unloadPlugin(pluginId: String): Result<Unit> {
        val plugin = plugins[pluginId] ?: return Result.failure(Exception("Plugin not found"))

        try {
            plugin.shutdown()
            plugins.remove(pluginId)
            pluginClassLoaders.remove(pluginId)
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun getPlugin(pluginId: String): AvaUIPlugin? {
        return plugins[pluginId]
    }

    fun getAllPlugins(): List<AvaUIPlugin> {
        return plugins.values.toList()
    }
}
```

## Plugin Sandbox

```kotlin
// PluginSandbox.kt

class PluginSandbox(private val plugin: AvaUIPlugin) {
    private val allowedPackages = setOf(
        "kotlin",
        "kotlinx",
        "com.augmentalis.avamagic.api"
    )

    fun execute(action: () -> Unit) {
        val securityManager = System.getSecurityManager()
        try {
            System.setSecurityManager(PluginSecurityManager(allowedPackages))
            action()
        } finally {
            System.setSecurityManager(securityManager)
        }
    }
}

class PluginSecurityManager(
    private val allowedPackages: Set<String>
) : SecurityManager() {
    override fun checkPackageAccess(pkg: String) {
        if (!allowedPackages.any { pkg.startsWith(it) }) {
            throw SecurityException("Package access denied: $pkg")
        }
    }

    override fun checkPermission(perm: Permission) {
        // Restrict file system access, network access, etc.
        when (perm) {
            is FilePermission -> throw SecurityException("File access denied")
            is SocketPermission -> throw SecurityException("Network access denied")
        }
    }
}
```

## Plugin Distribution

### Plugin Repository

```kotlin
// PluginRepository.kt

interface PluginRepository {
    suspend fun search(query: String): List<PluginInfo>
    suspend fun getPlugin(id: String): PluginInfo?
    suspend fun downloadPlugin(id: String, targetPath: String): Result<Unit>
}

data class PluginInfo(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val downloadUrl: String,
    val rating: Float,
    val downloads: Int,
    val screenshots: List<String>
)

class DefaultPluginRepository(
    private val baseUrl: String = "https://plugins.avamagic.com"
) : PluginRepository {
    override suspend fun search(query: String): List<PluginInfo> {
        val response = httpClient.get("$baseUrl/search?q=$query")
        return Json.decodeFromString(response.body)
    }

    override suspend fun downloadPlugin(id: String, targetPath: String): Result<Unit> {
        val plugin = getPlugin(id) ?: return Result.failure(Exception("Plugin not found"))
        httpClient.download(plugin.downloadUrl, targetPath)
        return Result.success(Unit)
    }
}
```

## Summary

Plugin System features:
- **Component plugins** - Custom UI components
- **Generator plugins** - Custom code generators
- **Theme plugins** - Custom themes
- **Plugin manager** - Discovery, loading, lifecycle
- **Sandbox** - Security restrictions
- **Repository** - Plugin marketplace

**Next:** Chapter 16 covers Expansion & Future roadmap.

---

**Created by Manoj Jhawar, manoj@ideahq.net**
