# RustAI v3: KMP-First Plugin Architecture with Rust Acceleration

**Version:** 3.0
**Date:** 2026-01-22
**Status:** Draft - Recommended Approach
**Philosophy:** Validate in KMP, Accelerate with Rust

---

## Executive Summary

This document presents a **two-phase approach** to building the next-generation AI infrastructure:

1. **Phase 1 (KMP)**: Build the plugin-first architecture in Kotlin Multiplatform
2. **Phase 2 (Rust)**: Port performance-critical paths to Rust

This approach minimizes risk, validates the architecture quickly, and allows gradual Rust adoption.

### Format Decision: AVU over JSON

All configuration and data exchange uses **AVU (Avanues Universal Format)** - our compact, line-based format:

```avu
# RustAI Configuration v1.0
# Type: CONFIG
---
schema: avu-config-1.0
version: 1.0.0
---
MOD:llama-3-8b:gguf:./models/llama-3-8b-q4.gguf:100:general,fast
MOD:gpt-4o:openai:https://api.openai.com/v1:50:cloud,quality
PLG:weather:tool:./plugins/weather.klib:enabled
PLG:gguf:llm:builtin:enabled
PRF:balanced:4:true:false:2048:0.8
```

---

## Table of Contents

1. [Why KMP-First?](#1-why-kmp-first)
2. [AVU Configuration Format](#2-avu-configuration-format)
3. [Phase 1: KMP Plugin Architecture](#3-phase-1-kmp-plugin-architecture)
4. [Phase 2: Rust Acceleration](#4-phase-2-rust-acceleration)
5. [Plugin System Design](#5-plugin-system-design)
6. [Model Registry (AVU-Based)](#6-model-registry-avu-based)
7. [Mobile Optimization](#7-mobile-optimization)
8. [Migration Path](#8-migration-path)
9. [Implementation Roadmap](#9-implementation-roadmap)

---

## 1. Why KMP-First?

### 1.1 Risk Comparison

| Factor | Direct Rust | KMP-First |
|--------|-------------|-----------|
| **Team Learning** | Steep curve, blocks progress | Gradual, parallel learning |
| **Time to MVP** | 6+ months | 3 months |
| **Architecture Risk** | Unknown until late | Validated early |
| **Fallback Option** | None | Full KMP system |
| **Production Risk** | High (new tech) | Low (proven stack) |

### 1.2 The Strangler Fig Pattern

```
┌─────────────────────────────────────────────────────────────────┐
│                    CURRENT STATE                                 │
│  Legacy AI Modules (TVM, ONNX via JNI)                          │
└───────────────────────────┬─────────────────────────────────────┘
                            │
            ┌───────────────┴───────────────┐
            ▼                               ▼
┌───────────────────────┐       ┌───────────────────────┐
│   PHASE 1: KMP        │       │   PHASE 2: RUST       │
│   ───────────────     │       │   ───────────────     │
│   • Plugin arch       │       │   • LLM inference     │
│   • Model registry    │       │   • Vector search     │
│   • AVU config        │──────▶│   • Tokenization      │
│   • Mobile optimization│      │   • Embeddings        │
│   • Full functionality│       │   • Hot paths only    │
│                       │       │                       │
│   Timeline: 3 months  │       │   Timeline: 2 months  │
└───────────────────────┘       └───────────────────────┘
            │                               │
            └───────────────┬───────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    FINAL STATE                                   │
│  KMP Orchestration + Rust Performance Core                      │
└─────────────────────────────────────────────────────────────────┘
```

### 1.3 What We Learn in Phase 1

1. **Real hotspots** - Profile actual usage, not assumptions
2. **Plugin API stability** - Iterate fast in KMP, freeze before Rust
3. **Mobile constraints** - Understand battery/memory needs
4. **Edge cases** - Discover issues before Rust investment

---

## 2. AVU Configuration Format

### 2.1 Why AVU over JSON?

| Aspect | JSON | AVU |
|--------|------|-----|
| **Size** | Verbose | 40-60% smaller |
| **Readability** | Nested, hard to scan | Line-per-record, easy scan |
| **Parsing** | Complex | Simple line split |
| **Diffing** | Poor | Git-friendly |
| **Streaming** | Requires buffering | Line-by-line |
| **Comments** | Not supported | Supported (#) |

### 2.2 AVU Format Specification for AI Config

```
# Avanues Universal Format - AI Configuration
# Type: AI_CONFIG
# Version: 1.0
---
schema: avu-ai-1.0
version: 1.0.0
locale: en-US
project: rustai
metadata:
  file: ai-config.avu
  generated: 1737590400
---
# MODEL REGISTRY
# MOD:id:provider:location:priority:tags
MOD:llama-3-8b-fast:gguf:./models/llama-3-8b-q4.gguf:100:general,fast,mobile
MOD:llama-3-8b-quality:gguf:./models/llama-3-8b-q8.gguf:80:general,quality
MOD:gpt-4o:openai:https://api.openai.com/v1:50:cloud,quality,reasoning
MOD:claude-3-sonnet:anthropic:https://api.anthropic.com:50:cloud,quality
MOD:ollama-local:ollama:http://localhost:11434:90:local,server
MOD:embedding-minilm:embedding:./models/minilm-l6-v2.onnx:100:embedding

# MODEL CONFIGURATION
# CFG:model_id:key:value
CFG:llama-3-8b-fast:context_length:4096
CFG:llama-3-8b-fast:gpu_layers:0
CFG:llama-3-8b-fast:threads:4
CFG:gpt-4o:api_key_env:OPENAI_API_KEY
CFG:gpt-4o:model:gpt-4o

# MODEL CONSTRAINTS
# CON:model_id:min_memory_mb:max_battery_mw:thermal_limit
CON:llama-3-8b-fast:4000:500:serious
CON:llama-3-8b-quality:8000:800:fair

# PLUGIN REGISTRY
# PLG:id:category:location:status
PLG:gguf:llm:builtin:enabled
PLG:candle:llm:builtin:disabled
PLG:openai:llm:builtin:enabled
PLG:anthropic:llm:builtin:enabled
PLG:ollama:llm:builtin:enabled
PLG:minilm:embedding:builtin:enabled
PLG:hnsw:vector:builtin:enabled
PLG:weather:tool:./plugins/weather.klib:enabled
PLG:calculator:tool:./plugins/calculator.klib:enabled

# POWER PROFILES
# PRF:name:threads:use_gpu:prefer_cloud:max_tokens:quality
PRF:performance:8:true:false:4096:1.0
PRF:balanced:4:true:false:2048:0.8
PRF:power_saver:2:false:true:512:0.6
PRF:ultra_saver:1:false:true:0:0.5

# ADAPTIVE SETTINGS
# ADP:battery_aware:thermal_aware:memory_aware:throttle_threshold
ADP:true:true:true:20

# PATHS
# PTH:type:path
PTH:models:./models
PTH:models:~/.rustai/models
PTH:plugins:./plugins
PTH:cache:./cache
PTH:vectors:./data/vectors.db
```

### 2.3 AVU Parser (KMP)

```kotlin
// AVUConfigParser.kt

object AVUConfigParser {

    data class AIConfig(
        val models: List<ModelConfig>,
        val plugins: List<PluginConfig>,
        val powerProfiles: Map<String, PowerProfile>,
        val adaptive: AdaptiveConfig,
        val paths: PathConfig,
    )

    data class ModelConfig(
        val id: String,
        val provider: String,
        val location: String,
        val priority: Int,
        val tags: Set<String>,
        val config: Map<String, String> = emptyMap(),
        val constraints: ModelConstraints? = null,
    )

    fun parse(avu: String): AIConfig {
        val models = mutableListOf<ModelConfig>()
        val modelConfigs = mutableMapOf<String, MutableMap<String, String>>()
        val modelConstraints = mutableMapOf<String, ModelConstraints>()
        val plugins = mutableListOf<PluginConfig>()
        val profiles = mutableMapOf<String, PowerProfile>()
        var adaptive = AdaptiveConfig()
        val paths = PathConfig()

        var inDataSection = false

        for (line in avu.lines()) {
            val trimmed = line.trim()

            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            // Section delimiter
            if (trimmed == "---") {
                inDataSection = true
                continue
            }

            if (!inDataSection) continue

            // Parse data lines
            val code = trimmed.substringBefore(":")
            val content = trimmed.substringAfter(":")
            val parts = content.split(":")

            when (code) {
                "MOD" -> {
                    if (parts.size >= 5) {
                        models.add(ModelConfig(
                            id = parts[0],
                            provider = parts[1],
                            location = parts[2],
                            priority = parts[3].toIntOrNull() ?: 0,
                            tags = parts[4].split(",").toSet(),
                        ))
                    }
                }
                "CFG" -> {
                    if (parts.size >= 3) {
                        val modelId = parts[0]
                        val key = parts[1]
                        val value = parts[2]
                        modelConfigs.getOrPut(modelId) { mutableMapOf() }[key] = value
                    }
                }
                "CON" -> {
                    if (parts.size >= 4) {
                        modelConstraints[parts[0]] = ModelConstraints(
                            minMemoryMb = parts[1].toIntOrNull() ?: 0,
                            maxBatteryMw = parts[2].toIntOrNull() ?: 0,
                            thermalLimit = ThermalState.fromString(parts[3]),
                        )
                    }
                }
                "PLG" -> {
                    if (parts.size >= 4) {
                        plugins.add(PluginConfig(
                            id = parts[0],
                            category = PluginCategory.fromString(parts[1]),
                            location = parts[2],
                            enabled = parts[3] == "enabled",
                        ))
                    }
                }
                "PRF" -> {
                    if (parts.size >= 6) {
                        profiles[parts[0]] = PowerProfile(
                            name = parts[0],
                            threads = parts[1].toIntOrNull() ?: 4,
                            useGpu = parts[2].toBoolean(),
                            preferCloud = parts[3].toBoolean(),
                            maxTokens = parts[4].toIntOrNull() ?: 2048,
                            quality = parts[5].toFloatOrNull() ?: 0.8f,
                        )
                    }
                }
                "ADP" -> {
                    if (parts.size >= 4) {
                        adaptive = AdaptiveConfig(
                            batteryAware = parts[0].toBoolean(),
                            thermalAware = parts[1].toBoolean(),
                            memoryAware = parts[2].toBoolean(),
                            throttleThreshold = parts[3].toIntOrNull() ?: 20,
                        )
                    }
                }
                "PTH" -> {
                    if (parts.size >= 2) {
                        paths.add(parts[0], parts[1])
                    }
                }
            }
        }

        // Merge configs and constraints into models
        val mergedModels = models.map { model ->
            model.copy(
                config = modelConfigs[model.id] ?: emptyMap(),
                constraints = modelConstraints[model.id],
            )
        }

        return AIConfig(
            models = mergedModels,
            plugins = plugins,
            powerProfiles = profiles,
            adaptive = adaptive,
            paths = paths,
        )
    }

    fun serialize(config: AIConfig): String = buildString {
        // Header
        appendLine("# Avanues Universal Format - AI Configuration")
        appendLine("# Type: AI_CONFIG")
        appendLine("# Generated: ${System.currentTimeMillis()}")
        appendLine("---")
        appendLine("schema: avu-ai-1.0")
        appendLine("version: 1.0.0")
        appendLine("---")

        // Models
        appendLine("# MODEL REGISTRY")
        for (model in config.models) {
            appendLine("MOD:${model.id}:${model.provider}:${model.location}:${model.priority}:${model.tags.joinToString(",")}")
        }

        // Model configs
        appendLine("# MODEL CONFIGURATION")
        for (model in config.models) {
            for ((key, value) in model.config) {
                appendLine("CFG:${model.id}:$key:$value")
            }
        }

        // Constraints
        appendLine("# MODEL CONSTRAINTS")
        for (model in config.models) {
            model.constraints?.let { c ->
                appendLine("CON:${model.id}:${c.minMemoryMb}:${c.maxBatteryMw}:${c.thermalLimit.name.lowercase()}")
            }
        }

        // Plugins
        appendLine("# PLUGIN REGISTRY")
        for (plugin in config.plugins) {
            val status = if (plugin.enabled) "enabled" else "disabled"
            appendLine("PLG:${plugin.id}:${plugin.category.name.lowercase()}:${plugin.location}:$status")
        }

        // Profiles
        appendLine("# POWER PROFILES")
        for ((_, profile) in config.powerProfiles) {
            appendLine("PRF:${profile.name}:${profile.threads}:${profile.useGpu}:${profile.preferCloud}:${profile.maxTokens}:${profile.quality}")
        }

        // Adaptive
        appendLine("# ADAPTIVE SETTINGS")
        with(config.adaptive) {
            appendLine("ADP:$batteryAware:$thermalAware:$memoryAware:$throttleThreshold")
        }

        // Paths
        appendLine("# PATHS")
        for ((type, pathList) in config.paths.all()) {
            for (path in pathList) {
                appendLine("PTH:$type:$path")
            }
        }
    }
}
```

---

## 3. Phase 1: KMP Plugin Architecture

### 3.1 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │   Chat UI   │  │  Voice UI   │  │  Settings   │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
├─────────────────────────────────────────────────────────────────┤
│                    KMP AI CORE                                   │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                     RustAI (KMP)                            ││
│  │  ┌───────────────┐ ┌───────────────┐ ┌───────────────┐     ││
│  │  │PluginManager  │ │ ModelRegistry │ │ResourceMonitor│     ││
│  │  │ (KMP)         │ │ (AVU-based)   │ │ (Platform)    │     ││
│  │  └───────────────┘ └───────────────┘ └───────────────┘     ││
│  │  ┌───────────────┐ ┌───────────────┐ ┌───────────────┐     ││
│  │  │ModelSelector  │ │AdaptiveEngine │ │ MetricsEngine │     ││
│  │  └───────────────┘ └───────────────┘ └───────────────┘     ││
│  └─────────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────────┤
│                    PLUGIN LAYER (KMP)                            │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────┐ │
│  │LLM Plugins  │ │Embed Plugins│ │Vector Plugs │ │Tool Plugs │ │
│  │───────────  │ │───────────  │ │───────────  │ │─────────  │ │
│  │• GGUF(JNI)  │ │• MiniLM     │ │• HNSW       │ │• Weather  │ │
│  │• OpenAI     │ │• OpenAI     │ │• (future:   │ │• Search   │ │
│  │• Anthropic  │ │• Cohere     │ │  Qdrant)    │ │• Calc     │ │
│  │• Ollama     │ │             │ │             │ │           │ │
│  └─────────────┘ └─────────────┘ └─────────────┘ └───────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                    NATIVE LAYER (JNI)                            │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  llama.cpp (C++) │ ONNX Runtime (C++) │ (Future: Rust)     ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Module Structure

```
Modules/AI/
├── RustAI/                          # New KMP module (replaces others)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/com/augmentalis/rustai/
│       │   ├── RustAI.kt            # Main facade
│       │   ├── config/
│       │   │   ├── AVUConfigParser.kt
│       │   │   └── AIConfig.kt
│       │   ├── plugin/
│       │   │   ├── Plugin.kt        # Base plugin interface
│       │   │   ├── PluginManager.kt
│       │   │   ├── PluginLoader.kt
│       │   │   └── PluginCategory.kt
│       │   ├── registry/
│       │   │   ├── ModelRegistry.kt
│       │   │   ├── ModelSelector.kt
│       │   │   └── HotReloader.kt
│       │   ├── resource/
│       │   │   ├── ResourceMonitor.kt
│       │   │   ├── AdaptiveEngine.kt
│       │   │   └── PowerProfile.kt
│       │   ├── llm/
│       │   │   ├── LLMPlugin.kt     # LLM plugin interface
│       │   │   ├── GenerateRequest.kt
│       │   │   └── GenerateResponse.kt
│       │   ├── embedding/
│       │   │   ├── EmbeddingPlugin.kt
│       │   │   └── Embedding.kt
│       │   ├── vector/
│       │   │   ├── VectorPlugin.kt
│       │   │   └── SearchResult.kt
│       │   ├── nlu/
│       │   │   ├── NLUPlugin.kt
│       │   │   └── Classification.kt
│       │   ├── tool/
│       │   │   ├── ToolPlugin.kt
│       │   │   └── ToolDefinition.kt
│       │   └── metrics/
│       │       ├── MetricsEngine.kt
│       │       └── MetricEvent.kt
│       │
│       ├── androidMain/kotlin/
│       │   ├── ResourceMonitorAndroid.kt
│       │   └── PluginLoaderAndroid.kt
│       │
│       └── iosMain/kotlin/
│           ├── ResourceMonitorIOS.kt
│           └── PluginLoaderIOS.kt
│
├── RustAI-Plugins/                   # Built-in plugins
│   ├── llm-gguf/                    # GGUF via llama.cpp JNI
│   ├── llm-openai/                  # OpenAI cloud
│   ├── llm-anthropic/               # Anthropic cloud
│   ├── llm-ollama/                  # Ollama local server
│   ├── embedding-minilm/            # MiniLM via ONNX
│   ├── embedding-openai/            # OpenAI embeddings
│   ├── vector-hnsw/                 # Pure KMP HNSW
│   ├── nlu-mobilebert/              # MobileBERT via ONNX
│   └── tool-builtin/                # Built-in tools
│
└── RustAI-Native/                    # Phase 2: Rust components
    └── (empty for Phase 1)
```

### 3.3 Core Interfaces

```kotlin
// Plugin.kt - Universal plugin interface

interface Plugin {
    val id: String
    val name: String
    val version: Version
    val category: PluginCategory

    fun capabilities(): List<Capability>
    fun requirements(): ResourceRequirements

    suspend fun init(config: PluginConfig)
    fun health(): HealthStatus
    suspend fun shutdown()
}

enum class PluginCategory {
    LLM,
    EMBEDDING,
    VECTOR,
    NLU,
    TOKENIZER,
    TOOL,
    DOCUMENT_PARSER,
    RESOURCE_MONITOR,
}

data class ResourceRequirements(
    val minMemoryBytes: Long,
    val recommendedMemoryBytes: Long,
    val preferredCores: Int,
    val requiresGpu: Boolean,
    val batteryDrawMw: Int,
    val thermalImpact: Int,        // 0-100
    val backgroundCapable: Boolean,
)
```

```kotlin
// LLMPlugin.kt - LLM plugin interface

interface LLMPlugin : Plugin {
    suspend fun loadModel(config: ModelConfig): ModelHandle
    suspend fun unloadModel(handle: ModelHandle)

    suspend fun generate(
        handle: ModelHandle,
        prompt: String,
        params: GenerateParams,
    ): GenerateResponse

    fun generateStream(
        handle: ModelHandle,
        prompt: String,
        params: GenerateParams,
    ): Flow<Token>

    fun modelInfo(handle: ModelHandle): ModelInfo
    fun supportedFormats(): List<String>
    fun countTokens(handle: ModelHandle, text: String): Int
}

data class GenerateParams(
    val maxTokens: Int = 500,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repetitionPenalty: Float = 1.1f,
    val stopSequences: List<String> = emptyList(),
    val grammar: String? = null,
    val jsonSchema: String? = null,
)

data class GenerateResponse(
    val text: String,
    val tokensGenerated: Int,
    val tokensPrompt: Int,
    val finishReason: FinishReason,
    val tokensPerSecond: Float,
    val latencyMs: Long,
)

data class Token(
    val text: String,
    val logprob: Float,
    val isFinal: Boolean,
)
```

### 3.4 Main RustAI Facade

```kotlin
// RustAI.kt - Main entry point

class RustAI private constructor(
    private val config: AIConfig,
    private val plugins: PluginManager,
    private val registry: ModelRegistry,
    private val monitor: ResourceMonitor,
    private val adaptive: AdaptiveEngine,
    private val metrics: MetricsEngine,
) {
    companion object {
        /**
         * Create RustAI with AVU configuration
         */
        suspend fun create(avuConfig: String): RustAI {
            val config = AVUConfigParser.parse(avuConfig)
            return create(config)
        }

        /**
         * Create RustAI with default auto-discovery
         */
        suspend fun createWithDefaults(
            modelPaths: List<String> = listOf("./models"),
            pluginPaths: List<String> = listOf("./plugins"),
        ): RustAI {
            val config = AIConfig.autoDiscover(modelPaths, pluginPaths)
            return create(config)
        }

        private suspend fun create(config: AIConfig): RustAI {
            val monitor = ResourceMonitor.create()
            val plugins = PluginManager().apply {
                // Register built-in plugins
                registerBuiltin<GGUFPlugin>()
                registerBuiltin<OpenAIPlugin>()
                registerBuiltin<AnthropicPlugin>()
                registerBuiltin<OllamaPlugin>()
                registerBuiltin<MiniLMPlugin>()
                registerBuiltin<HNSWPlugin>()
                registerBuiltin<MobileBERTPlugin>()

                // Load external plugins
                for (pluginConfig in config.plugins) {
                    if (pluginConfig.enabled && pluginConfig.location != "builtin") {
                        load(pluginConfig.location)
                    }
                }
            }

            val registry = ModelRegistry(config.models, plugins)
            val adaptive = AdaptiveEngine(monitor, config.adaptive)
            val metrics = MetricsEngine()

            return RustAI(config, plugins, registry, monitor, adaptive, metrics)
        }
    }

    // ==================== Model Management ====================

    fun listModels(): List<ModelInfo> = registry.listModels()

    suspend fun loadModel(modelId: String) {
        registry.load(modelId)
    }

    suspend fun unloadModel(modelId: String) {
        registry.unload(modelId)
    }

    // ==================== Generation ====================

    /**
     * Generate text with automatic model selection
     */
    suspend fun generate(
        prompt: String,
        params: GenerateParams = GenerateParams(),
        criteria: SelectionCriteria = SelectionCriteria(),
    ): GenerateResponse {
        // Adapt params based on device state
        val adaptedParams = adaptive.adaptParams(params)

        // Select best model
        val model = registry.select(criteria, monitor.current())

        // Get plugin
        val plugin = plugins.getLLM(model.provider)

        // Ensure model is loaded
        val handle = registry.getOrLoad(model.id)

        // Generate with metrics
        val start = System.currentTimeMillis()
        val response = plugin.generate(handle, prompt, adaptedParams)
        val latency = System.currentTimeMillis() - start

        // Record metrics
        metrics.record(MetricEvent.Generation(
            modelId = model.id,
            tokens = response.tokensGenerated,
            latencyMs = latency,
            batteryDrawMw = monitor.current().powerDrawMw,
        ))

        return response.copy(latencyMs = latency)
    }

    /**
     * Generate with streaming
     */
    fun generateStream(
        prompt: String,
        params: GenerateParams = GenerateParams(),
        criteria: SelectionCriteria = SelectionCriteria(),
    ): Flow<Token> = flow {
        val adaptedParams = adaptive.adaptParams(params)
        val model = registry.select(criteria, monitor.current())
        val plugin = plugins.getLLM(model.provider)
        val handle = registry.getOrLoad(model.id)

        plugin.generateStream(handle, prompt, adaptedParams).collect { token ->
            emit(token)
        }
    }

    /**
     * Generate with specific model
     */
    suspend fun generateWithModel(
        modelId: String,
        prompt: String,
        params: GenerateParams = GenerateParams(),
    ): GenerateResponse {
        val adaptedParams = adaptive.adaptParams(params)
        val model = registry.get(modelId) ?: throw ModelNotFound(modelId)
        val plugin = plugins.getLLM(model.provider)
        val handle = registry.getOrLoad(model.id)

        return plugin.generate(handle, prompt, adaptedParams)
    }

    // ==================== Embeddings ====================

    suspend fun embed(text: String): Embedding {
        val plugin = plugins.getEmbedding("minilm") // Default
        return plugin.embed(text)
    }

    suspend fun embedBatch(texts: List<String>): List<Embedding> {
        val plugin = plugins.getEmbedding("minilm")
        return plugin.embedBatch(texts)
    }

    // ==================== RAG ====================

    suspend fun indexDocument(doc: Document) {
        val embedding = embed(doc.content)
        val vectorPlugin = plugins.getVector("hnsw")
        vectorPlugin.insert(VectorEntry(
            id = doc.id,
            vector = embedding.vector,
            payload = doc.metadata,
        ))
    }

    suspend fun search(query: String, topK: Int = 5): List<SearchResult> {
        val queryEmbedding = embed(query)
        val vectorPlugin = plugins.getVector("hnsw")
        return vectorPlugin.search(queryEmbedding.vector, topK)
    }

    // ==================== NLU ====================

    suspend fun classify(text: String): ClassificationResult {
        val plugin = plugins.getNLU("mobilebert")
        return plugin.classify(text)
    }

    // ==================== Tools ====================

    fun listTools(): List<ToolDefinition> {
        return plugins.getAllTools().flatMap { it.definitions() }
    }

    suspend fun executeTool(name: String, paramsJson: String): ToolResult {
        val plugin = plugins.getToolByName(name)
        return plugin.execute(name, paramsJson)
    }

    // ==================== Resources ====================

    fun getResources(): ResourceSnapshot = monitor.current()

    fun setPowerProfile(profile: PowerProfile) {
        adaptive.setProfile(profile)
    }

    fun estimateBattery(params: GenerateParams): BatteryEstimate {
        return adaptive.estimateBattery(params)
    }

    // ==================== Lifecycle ====================

    suspend fun shutdown() {
        plugins.shutdownAll()
        registry.unloadAll()
        monitor.stop()
    }
}
```

---

## 4. Phase 2: Rust Acceleration

### 4.1 What to Port to Rust

After Phase 1 profiling, we identify hotspots. Typical candidates:

| Component | Why Port? | Expected Speedup |
|-----------|-----------|------------------|
| **LLM Inference** | Already C++, reduce JNI | 10-30% |
| **Tokenization** | CPU-bound, HF tokenizers | 5-10x |
| **Vector Search** | O(log n), memory-bound | 10-100x |
| **Embedding Gen** | Matrix ops, SIMD | 2-5x |

### 4.2 Rust Module Structure

```
Modules/AI/RustAI-Native/
├── Cargo.toml                       # Workspace
├── crates/
│   ├── rustai-core/                 # UniFFI exports
│   │   ├── src/lib.rs
│   │   └── uniffi.toml
│   ├── rustai-llm/                  # LLM inference
│   ├── rustai-tokenizer/            # HF tokenizers
│   ├── rustai-vector/               # HNSW search
│   └── rustai-embedding/            # Embedding gen
├── bindings/
│   └── kotlin/                      # Generated
└── build.rs
```

### 4.3 KMP ↔ Rust Bridge

```kotlin
// RustAccelerator.kt - Bridge to Rust components

expect class RustAccelerator {
    suspend fun tokenize(text: String): TokenizeResult
    suspend fun embed(text: String): FloatArray
    suspend fun vectorSearch(query: FloatArray, topK: Int): List<SearchResult>
}

// RustAccelerator.android.kt
actual class RustAccelerator {
    init {
        System.loadLibrary("rustai_native")
    }

    actual suspend fun tokenize(text: String): TokenizeResult {
        return withContext(Dispatchers.Default) {
            // JNI call to Rust
            rustai_tokenize(text)
        }
    }

    private external fun rustai_tokenize(text: String): TokenizeResult
    private external fun rustai_embed(text: String): FloatArray
    private external fun rustai_vector_search(query: FloatArray, topK: Int): Array<SearchResult>
}
```

### 4.4 Feature Flags for A/B Testing

```kotlin
// FeatureFlags.kt

object FeatureFlags {
    // Rust acceleration flags
    var useRustTokenizer: Boolean = false
    var useRustEmbedding: Boolean = false
    var useRustVectorSearch: Boolean = false
    var useRustLLM: Boolean = false
}

// In RustAI.kt
suspend fun embed(text: String): Embedding {
    return if (FeatureFlags.useRustEmbedding && rustAccelerator.isAvailable()) {
        val vector = rustAccelerator.embed(text)
        Embedding(vector)
    } else {
        // KMP fallback
        val plugin = plugins.getEmbedding("minilm")
        plugin.embed(text)
    }
}
```

---

## 5. Plugin System Design

### 5.1 Plugin Discovery (AVU Manifest)

Each plugin has an AVU manifest:

```
# Plugin Manifest
# plugin.avu
---
schema: avu-plugin-1.0
version: 1.0.0
---
PLG:weather-tool:1.0.0:tool
NAM:Weather Tool
DSC:Get current weather and forecasts
REQ:10485760:1:false:50:5:true
CAP:tool:get_weather:Get current weather for a location
CAP:tool:get_forecast:Get weather forecast
DEP:network
```

### 5.2 Plugin Loader

```kotlin
// PluginLoader.kt

interface PluginLoader {
    suspend fun discover(paths: List<String>): List<PluginInfo>
    suspend fun load(path: String): Plugin
    suspend fun unload(pluginId: String)
}

// KMP Implementation
class KMPPluginLoader : PluginLoader {
    override suspend fun discover(paths: List<String>): List<PluginInfo> {
        val discovered = mutableListOf<PluginInfo>()

        for (path in paths) {
            val dir = File(path)
            if (!dir.exists()) continue

            for (file in dir.listFiles() ?: emptyArray()) {
                when {
                    file.name.endsWith(".klib") -> {
                        // KMP plugin (compile-time linked)
                        val manifest = readManifest(file)
                        discovered.add(parseManifest(manifest))
                    }
                    file.name.endsWith(".jar") -> {
                        // JVM plugin (reflection-loaded)
                        val manifest = readJarManifest(file)
                        discovered.add(parseManifest(manifest))
                    }
                    file.isDirectory && file.resolve("plugin.avu").exists() -> {
                        // Directory-based plugin
                        val manifest = file.resolve("plugin.avu").readText()
                        discovered.add(parseManifest(manifest))
                    }
                }
            }
        }

        return discovered
    }

    override suspend fun load(path: String): Plugin {
        // Load based on type
        return when {
            path.endsWith(".jar") -> loadJarPlugin(path)
            path == "builtin" -> throw IllegalArgumentException("Use registerBuiltin")
            else -> throw UnsupportedPluginFormat(path)
        }
    }

    private fun loadJarPlugin(path: String): Plugin {
        val classLoader = URLClassLoader(arrayOf(File(path).toURI().toURL()))
        val manifest = readJarManifest(File(path))
        val className = manifest.lines()
            .find { it.startsWith("CLS:") }
            ?.substringAfter("CLS:")
            ?: throw InvalidPluginManifest("Missing CLS entry")

        val pluginClass = classLoader.loadClass(className)
        return pluginClass.getDeclaredConstructor().newInstance() as Plugin
    }
}
```

### 5.3 Built-in Plugin Example: GGUF

```kotlin
// GGUFPlugin.kt - GGUF via llama.cpp JNI

class GGUFPlugin : LLMPlugin {
    override val id = "gguf"
    override val name = "GGUF/llama.cpp Plugin"
    override val version = Version(1, 0, 0)
    override val category = PluginCategory.LLM

    private val loadedModels = mutableMapOf<ModelHandle, LlamaModel>()
    private var nextHandle = 0L

    override fun capabilities() = listOf(
        Capability.LLM(
            formats = listOf("gguf", "ggml"),
            features = listOf("streaming", "grammar", "json_mode"),
        )
    )

    override fun requirements() = ResourceRequirements(
        minMemoryBytes = 1L * 1024 * 1024 * 1024,  // 1GB
        recommendedMemoryBytes = 4L * 1024 * 1024 * 1024,
        preferredCores = 4,
        requiresGpu = false,
        batteryDrawMw = 300,
        thermalImpact = 60,
        backgroundCapable = true,
    )

    override suspend fun init(config: PluginConfig) {
        // Load native library
        System.loadLibrary("llama")
    }

    override suspend fun loadModel(config: ModelConfig): ModelHandle {
        val params = LlamaParams(
            contextLength = config.config["context_length"]?.toIntOrNull() ?: 4096,
            gpuLayers = config.config["gpu_layers"]?.toIntOrNull() ?: 0,
            threads = config.config["threads"]?.toIntOrNull() ?: 4,
        )

        val model = LlamaModel.load(config.location, params)
        val handle = ModelHandle(nextHandle++)
        loadedModels[handle] = model

        return handle
    }

    override fun generateStream(
        handle: ModelHandle,
        prompt: String,
        params: GenerateParams,
    ): Flow<Token> = flow {
        val model = loadedModels[handle] ?: throw ModelNotLoaded(handle)

        model.generateStream(prompt, params).collect { llamaToken ->
            emit(Token(
                text = llamaToken.text,
                logprob = llamaToken.logprob,
                isFinal = llamaToken.isEos,
            ))
        }
    }

    override suspend fun generate(
        handle: ModelHandle,
        prompt: String,
        params: GenerateParams,
    ): GenerateResponse {
        val model = loadedModels[handle] ?: throw ModelNotLoaded(handle)
        return model.generate(prompt, params)
    }

    override fun supportedFormats() = listOf("gguf", "ggml")
}
```

---

## 6. Model Registry (AVU-Based)

### 6.1 Auto-Discovery

```kotlin
// ModelRegistry.kt

class ModelRegistry(
    initialModels: List<ModelConfig>,
    private val plugins: PluginManager,
) {
    private val models = mutableMapOf<String, ModelConfig>()
    private val loaded = mutableMapOf<String, ModelHandle>()

    init {
        for (model in initialModels) {
            models[model.id] = model
        }
    }

    /**
     * Auto-discover models in directories
     */
    suspend fun discover(paths: List<String>) {
        for (path in paths) {
            discoverInPath(path)
        }
    }

    private suspend fun discoverInPath(path: String) {
        val dir = File(path)
        if (!dir.exists()) return

        for (file in dir.walk()) {
            val format = detectFormat(file)
            if (format != null && !models.containsKey(file.nameWithoutExtension)) {
                val config = ModelConfig(
                    id = file.nameWithoutExtension,
                    provider = format.provider,
                    location = file.absolutePath,
                    priority = format.defaultPriority,
                    tags = inferTags(file, format),
                )
                models[config.id] = config
                println("Discovered model: ${config.id} (${format.name})")
            }
        }
    }

    private fun detectFormat(file: File): ModelFormat? {
        return when (file.extension.lowercase()) {
            "gguf" -> ModelFormat("gguf", "gguf", 100)
            "ggml" -> ModelFormat("ggml", "gguf", 90)
            "onnx" -> ModelFormat("onnx", "onnx", 80)
            "safetensors" -> ModelFormat("safetensors", "candle", 70)
            else -> null
        }
    }

    private fun inferTags(file: File, format: ModelFormat): Set<String> {
        val tags = mutableSetOf<String>()

        // Infer from filename
        val name = file.nameWithoutExtension.lowercase()

        if (name.contains("q4") || name.contains("q5")) tags.add("quantized")
        if (name.contains("q8")) tags.add("quality")
        if (name.contains("embed") || name.contains("minilm") || name.contains("bge")) {
            tags.add("embedding")
        }
        if (name.contains("llama") || name.contains("mistral") || name.contains("phi")) {
            tags.add("general")
        }
        if (name.contains("code") || name.contains("coder")) tags.add("code")

        // Infer from parent directory
        val parent = file.parentFile?.name?.lowercase() ?: ""
        if (parent == "embeddings") tags.add("embedding")
        if (parent == "nlu") tags.add("nlu")

        return tags
    }

    /**
     * Select best model based on criteria
     */
    fun select(
        criteria: SelectionCriteria,
        resources: ResourceSnapshot,
    ): ModelConfig {
        val candidates = models.values.filter { model ->
            meetsRequirements(model, resources) &&
            meetsCriteria(model, criteria)
        }

        if (candidates.isEmpty()) {
            throw NoSuitableModel(criteria)
        }

        // Score and rank
        return candidates.maxByOrNull { score(it, criteria, resources) }!!
    }

    private fun meetsRequirements(model: ModelConfig, resources: ResourceSnapshot): Boolean {
        val constraints = model.constraints ?: return true

        // Check memory
        if (resources.memoryAvailableBytes < constraints.minMemoryMb * 1024 * 1024) {
            return false
        }

        // Check thermal
        if (resources.thermalState > constraints.thermalLimit) {
            return false
        }

        // Check battery (if discharging and below threshold)
        if (resources.batteryState == BatteryState.DISCHARGING &&
            resources.batteryPercent != null &&
            resources.batteryPercent < 20) {
            // Prefer cloud models when battery is low
            if (!model.tags.contains("cloud")) {
                return false
            }
        }

        return true
    }

    private fun score(
        model: ModelConfig,
        criteria: SelectionCriteria,
        resources: ResourceSnapshot,
    ): Float {
        var score = model.priority.toFloat()

        if (criteria.preferLocal && !model.tags.contains("cloud")) score += 50
        if (criteria.preferSpeed && model.tags.contains("fast")) score += 30
        if (criteria.preferQuality && model.tags.contains("quality")) score += 30
        if (criteria.batterySensitive && model.tags.contains("cloud")) score += 40

        for (tag in criteria.requiredTags) {
            if (model.tags.contains(tag)) score += 10
        }

        return score
    }
}
```

---

## 7. Mobile Optimization

### 7.1 Resource Monitor (Platform-Specific)

```kotlin
// ResourceMonitor.kt (commonMain)

expect class ResourceMonitor {
    fun current(): ResourceSnapshot
    fun startMonitoring()
    fun stopMonitoring()
}

data class ResourceSnapshot(
    val timestamp: Long,

    // Memory
    val memoryUsedBytes: Long,
    val memoryAvailableBytes: Long,
    val memoryPressure: MemoryPressure,

    // CPU
    val cpuUsagePercent: Float,
    val cpuTemperatureCelsius: Float?,
    val thermalState: ThermalState,

    // Battery
    val batteryPercent: Int?,
    val batteryState: BatteryState,
    val powerDrawMw: Int?,
)

enum class ThermalState { NOMINAL, FAIR, SERIOUS, CRITICAL }
enum class BatteryState { CHARGING, DISCHARGING, FULL, UNKNOWN }
enum class MemoryPressure { NORMAL, WARNING, CRITICAL }
```

```kotlin
// ResourceMonitor.android.kt (androidMain)

actual class ResourceMonitor(private val context: Context) {
    private val activityManager = context.getSystemService<ActivityManager>()
    private val batteryManager = context.getSystemService<BatteryManager>()
    private val powerManager = context.getSystemService<PowerManager>()

    actual fun current(): ResourceSnapshot {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)

        return ResourceSnapshot(
            timestamp = System.currentTimeMillis(),
            memoryUsedBytes = memInfo.totalMem - memInfo.availMem,
            memoryAvailableBytes = memInfo.availMem,
            memoryPressure = when {
                memInfo.lowMemory -> MemoryPressure.CRITICAL
                memInfo.availMem < memInfo.totalMem * 0.2 -> MemoryPressure.WARNING
                else -> MemoryPressure.NORMAL
            },
            cpuUsagePercent = getCpuUsage(),
            cpuTemperatureCelsius = getCpuTemperature(),
            thermalState = getThermalState(),
            batteryPercent = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
            batteryState = getBatteryState(),
            powerDrawMw = getCurrentPowerDraw(),
        )
    }

    private fun getThermalState(): ThermalState {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (powerManager?.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE,
                PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.NOMINAL
                PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.FAIR
                PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.SERIOUS
                PowerManager.THERMAL_STATUS_CRITICAL,
                PowerManager.THERMAL_STATUS_EMERGENCY,
                PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalState.CRITICAL
                else -> ThermalState.NOMINAL
            }
        } else {
            ThermalState.NOMINAL
        }
    }
}
```

### 7.2 Adaptive Engine

```kotlin
// AdaptiveEngine.kt

class AdaptiveEngine(
    private val monitor: ResourceMonitor,
    private val config: AdaptiveConfig,
) {
    private var currentProfile: PowerProfile = PowerProfile.BALANCED

    fun setProfile(profile: PowerProfile) {
        currentProfile = profile
    }

    fun adaptParams(params: GenerateParams): GenerateParams {
        val resources = monitor.current()
        var adapted = params

        // Thermal throttling
        if (config.thermalAware) {
            adapted = when (resources.thermalState) {
                ThermalState.CRITICAL -> adapted.copy(
                    maxTokens = minOf(adapted.maxTokens, 50),
                )
                ThermalState.SERIOUS -> adapted.copy(
                    maxTokens = minOf(adapted.maxTokens, 200),
                )
                else -> adapted
            }
        }

        // Battery awareness
        if (config.batteryAware && resources.batteryState == BatteryState.DISCHARGING) {
            val battery = resources.batteryPercent ?: 100
            if (battery < config.throttleThreshold) {
                adapted = adapted.copy(
                    maxTokens = minOf(adapted.maxTokens, 256),
                )
            }
        }

        // Memory pressure
        if (config.memoryAware) {
            adapted = when (resources.memoryPressure) {
                MemoryPressure.CRITICAL -> adapted.copy(
                    maxTokens = minOf(adapted.maxTokens, 128),
                )
                MemoryPressure.WARNING -> adapted.copy(
                    maxTokens = minOf(adapted.maxTokens, 512),
                )
                else -> adapted
            }
        }

        return adapted
    }

    fun estimateBattery(params: GenerateParams): BatteryEstimate {
        // Rough estimation based on typical power draw
        val tokensPerSecond = 10f // Conservative estimate
        val powerDrawMw = 300 // Typical for LLM inference
        val durationSec = params.maxTokens / tokensPerSecond
        val mwh = (powerDrawMw * durationSec / 3600).toInt()

        return BatteryEstimate(
            mwh = mwh,
            durationMs = (durationSec * 1000).toLong(),
            percentDrain = mwh / 50f, // Rough estimate
        )
    }
}
```

---

## 8. Migration Path

### 8.1 From Current Modules to RustAI

```
Week 1-2: Setup
├── Create RustAI module
├── Define plugin interfaces
├── Implement AVU parser
└── Setup build configuration

Week 3-4: Core Infrastructure
├── Implement PluginManager
├── Implement ModelRegistry
├── Implement ResourceMonitor
└── Implement AdaptiveEngine

Week 5-6: Built-in Plugins
├── Port GGUF plugin (from current LLM)
├── Port OpenAI plugin (from current LLM)
├── Port MiniLM plugin (from current NLU)
├── Implement HNSW plugin (new)

Week 7-8: Integration
├── Implement RustAI facade
├── Integration tests
├── Migrate Chat module to use RustAI
├── Performance benchmarks

Week 9-10: Polish
├── Documentation
├── Example apps
├── AVU config generator UI
└── Release
```

### 8.2 Feature Flag Migration

```kotlin
// Gradual migration from old modules

object AIFeatureFlags {
    // Phase 1: Use RustAI for new features only
    var useRustAIForChat: Boolean = false
    var useRustAIForRAG: Boolean = false

    // Phase 2: Migrate existing features
    var useRustAIForNLU: Boolean = false
    var useRustAIForLLM: Boolean = false

    // Phase 3: Full migration
    var disableLegacyAI: Boolean = false
}

// In ChatViewModel
val aiEngine = if (AIFeatureFlags.useRustAIForChat) {
    RustAI.createWithDefaults()
} else {
    LegacyAIEngine.create()
}
```

---

## 9. Implementation Roadmap

### Phase 1: KMP Plugin Architecture (10 weeks)

```
Weeks 1-2: Foundation
├── [ ] Create RustAI module structure
├── [ ] Define all plugin interfaces
├── [ ] Implement AVUConfigParser
├── [ ] Setup multiplatform build

Weeks 3-4: Plugin Infrastructure
├── [ ] PluginManager implementation
├── [ ] Plugin discovery system
├── [ ] ModelRegistry with auto-discovery
├── [ ] Hot-reload watcher

Weeks 5-6: Core Plugins
├── [ ] GGUFPlugin (llama.cpp wrapper)
├── [ ] OpenAIPlugin
├── [ ] AnthropicPlugin
├── [ ] OllamaPlugin

Weeks 7-8: Supporting Plugins
├── [ ] MiniLMPlugin (embeddings)
├── [ ] HNSWPlugin (vector search)
├── [ ] MobileBERTPlugin (NLU)
├── [ ] Built-in tools

Weeks 9-10: Integration & Polish
├── [ ] RustAI facade
├── [ ] ResourceMonitor (Android/iOS)
├── [ ] AdaptiveEngine
├── [ ] Integration tests
├── [ ] Documentation
```

### Phase 2: Rust Acceleration (8 weeks)

```
Weeks 11-12: Rust Setup
├── [ ] Rust workspace structure
├── [ ] UniFFI configuration
├── [ ] Build scripts (cargo-ndk, lipo)
├── [ ] CI/CD for Rust

Weeks 13-14: Tokenizer
├── [ ] HuggingFace tokenizers integration
├── [ ] UniFFI bindings
├── [ ] KMP bridge
├── [ ] A/B testing

Weeks 15-16: Vector Search
├── [ ] HNSW in Rust
├── [ ] Persistence layer
├── [ ] UniFFI bindings
├── [ ] Performance benchmarks

Weeks 17-18: Production
├── [ ] Full integration testing
├── [ ] Performance optimization
├── [ ] Memory profiling
├── [ ] Release
```

---

## Summary

| Aspect | Phase 1 (KMP) | Phase 2 (Rust) |
|--------|---------------|----------------|
| **Timeline** | 10 weeks | 8 weeks |
| **Risk** | Low | Medium |
| **Output** | Full functionality | Performance boost |
| **Learning** | Plugin architecture | Rust + FFI |
| **Deliverable** | Production-ready | Optimized core |

**Key Decisions:**

1. **AVU over JSON** - Compact, line-based, git-friendly
2. **KMP-first** - Validate architecture before Rust investment
3. **Plugin-everything** - Drop-in model support
4. **Mobile-native** - Battery/thermal/memory awareness built-in
5. **Gradual Rust** - A/B test, profile, then port hotspots

This approach gives us:
- **Fast time to market** with KMP
- **Validated architecture** before Rust
- **Lower risk** with fallback options
- **Better performance** where it matters (Rust hotspots)
