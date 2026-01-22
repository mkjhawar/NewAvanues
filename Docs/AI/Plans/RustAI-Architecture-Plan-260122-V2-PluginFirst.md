# RustAI v2: Plugin-First Cross-Platform AI Engine

**Version:** 2.0
**Date:** 2026-01-22
**Status:** Draft - Enhanced Architecture
**Philosophy:** Everything is a Plugin

---

## Executive Summary

RustAI v2 is a **complete ground-up rewrite** (not a port) designed with three core principles:

1. **Everything is a Plugin** - LLMs, embeddings, vector stores, tools - all swappable
2. **Mobile-First** - Battery efficiency, CPU throttling, memory budgets baked in
3. **Zero Configuration** - Drop a model file, it just works

### Vision Statement

> "The most extensible, efficient, and developer-friendly AI engine that runs anywhere - from a Raspberry Pi to a data center - with the same codebase and plugin ecosystem."

---

## Table of Contents

1. [Design Philosophy](#1-design-philosophy)
2. [Plugin-First Architecture](#2-plugin-first-architecture)
3. [Model Registry System](#3-model-registry-system)
4. [Mobile Optimization Engine](#4-mobile-optimization-engine)
5. [Core Engine Design](#5-core-engine-design)
6. [Plugin Specifications](#6-plugin-specifications)
7. [Resource Management](#7-resource-management)
8. [Hot-Reload & Updates](#8-hot-reload--updates)
9. [Example Plugins](#9-example-plugins)
10. [API Design](#10-api-design)
11. [Implementation Roadmap](#11-implementation-roadmap)

---

## 1. Design Philosophy

### 1.1 Why Complete Rewrite?

| Current System | RustAI v2 |
|----------------|-----------|
| Built around TVM constraints | Built around plugin flexibility |
| JNI overhead everywhere | Native Rust, zero-copy where possible |
| Hardcoded backends | Everything pluggable |
| Mobile as afterthought | Mobile-first design |
| Complex configuration | Convention over configuration |

### 1.2 Core Principles

```
┌─────────────────────────────────────────────────────────────────┐
│                    RUSTAI DESIGN PRINCIPLES                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. PLUGIN-FIRST                                                │
│     ─────────────                                               │
│     Every capability is a plugin. The core is just a            │
│     plugin loader + router. Want a new LLM format?              │
│     Write a plugin. Want custom embeddings? Plugin.             │
│                                                                  │
│  2. MOBILE-NATIVE                                               │
│     ────────────                                                │
│     Not "mobile-compatible" but "mobile-first". Battery         │
│     budgets, thermal throttling, memory limits are              │
│     first-class citizens, not afterthoughts.                    │
│                                                                  │
│  3. ZERO-CONFIG                                                 │
│     ───────────                                                 │
│     Drop a .gguf file in /models, it works. Drop a plugin       │
│     in /plugins, it loads. No XML, no complex setup.            │
│                                                                  │
│  4. OBSERVABLE                                                  │
│     ──────────                                                  │
│     Every operation emits metrics. Battery drain per token,     │
│     memory per model, latency per request. Tunable in           │
│     real-time.                                                   │
│                                                                  │
│  5. FUTURE-PROOF                                                │
│     ────────────                                                │
│     New model format in 6 months? Plugin. New vector DB?        │
│     Plugin. We don't predict the future, we enable it.          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 1.3 What We're Building

```
                         ┌─────────────────────┐
                         │    Applications     │
                         │  (Voice, Chat, RAG) │
                         └──────────┬──────────┘
                                    │
                         ┌──────────▼──────────┐
                         │   RustAI Core       │
                         │  ────────────────   │
                         │  Plugin Loader      │
                         │  Resource Manager   │
                         │  Request Router     │
                         │  Metrics Engine     │
                         └──────────┬──────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────┐         ┌───────────────┐         ┌───────────────┐
│ LLM Plugins   │         │Embedding Plugs│         │ Tool Plugins  │
│ ───────────── │         │ ───────────── │         │ ───────────── │
│ • gguf        │         │ • minilm      │         │ • weather     │
│ • candle      │         │ • bge         │         │ • calculator  │
│ • openai      │         │ • openai      │         │ • web_search  │
│ • anthropic   │         │ • cohere      │         │ • code_exec   │
│ • ollama      │         │ • custom      │         │ • custom      │
│ • custom      │         │               │         │               │
└───────────────┘         └───────────────┘         └───────────────┘
        │                           │                           │
        └───────────────────────────┼───────────────────────────┘
                                    │
                         ┌──────────▼──────────┐
                         │   Model Registry    │
                         │  ────────────────   │
                         │  • Auto-discovery   │
                         │  • Hot-reload       │
                         │  • Version mgmt     │
                         └─────────────────────┘
```

---

## 2. Plugin-First Architecture

### 2.1 Plugin Categories

| Category | Purpose | Examples |
|----------|---------|----------|
| **LLM Provider** | Text generation | GGUF, Candle, OpenAI, Anthropic, Ollama |
| **Embedding Provider** | Vector embeddings | MiniLM, BGE, OpenAI, Cohere |
| **Vector Store** | Similarity search | HNSW, Qdrant, Milvus, Pinecone |
| **NLU Provider** | Intent classification | MobileBERT, mALBERT, custom |
| **Tokenizer** | Text tokenization | HuggingFace, SentencePiece, custom |
| **Document Parser** | File processing | PDF, DOCX, HTML, Markdown |
| **Tool** | Function calling | Weather, Search, Calculator, custom |
| **Resource Monitor** | System metrics | Battery, CPU, Memory, Thermal |

### 2.2 Plugin Lifecycle

```
                    ┌─────────────┐
                    │  DISCOVERED │
                    └──────┬──────┘
                           │ validate()
                           ▼
                    ┌─────────────┐
                    │  VALIDATED  │
                    └──────┬──────┘
                           │ load()
                           ▼
                    ┌─────────────┐
          ┌────────│   LOADED    │────────┐
          │        └──────┬──────┘        │
          │               │ activate()    │
          │               ▼               │
          │        ┌─────────────┐        │
          │        │   ACTIVE    │◀───────┤
          │        └──────┬──────┘        │
          │               │               │ hot_reload()
   unload()│               │ deactivate() │
          │               ▼               │
          │        ┌─────────────┐        │
          │        │  INACTIVE   │────────┘
          │        └──────┬──────┘
          │               │ unload()
          ▼               ▼
                    ┌─────────────┐
                    │  UNLOADED   │
                    └─────────────┘
```

### 2.3 Plugin Interface (Universal)

```rust
/// Universal plugin trait - ALL plugins implement this
pub trait Plugin: Send + Sync + 'static {
    /// Unique identifier
    fn id(&self) -> &str;

    /// Human-readable name
    fn name(&self) -> &str;

    /// Semantic version
    fn version(&self) -> Version;

    /// Plugin category
    fn category(&self) -> PluginCategory;

    /// Capabilities this plugin provides
    fn capabilities(&self) -> Vec<Capability>;

    /// Resource requirements
    fn requirements(&self) -> ResourceRequirements;

    /// Initialize with configuration
    fn init(&mut self, config: PluginConfig) -> Result<()>;

    /// Health check
    fn health(&self) -> HealthStatus;

    /// Graceful shutdown
    fn shutdown(&mut self) -> Result<()>;
}

/// Resource requirements for mobile optimization
#[derive(Clone, Debug)]
pub struct ResourceRequirements {
    /// Minimum RAM needed (bytes)
    pub min_memory: u64,

    /// Recommended RAM (bytes)
    pub recommended_memory: u64,

    /// CPU cores preferred
    pub preferred_cores: u32,

    /// GPU required?
    pub requires_gpu: bool,

    /// Estimated battery drain (mW during inference)
    pub battery_draw_mw: u32,

    /// Thermal impact (0-100 scale)
    pub thermal_impact: u8,

    /// Can run in background?
    pub background_capable: bool,
}
```

---

## 3. Model Registry System

### 3.1 Zero-Config Model Discovery

The Model Registry automatically discovers and manages models:

```
/rustai/
├── models/                          # Drop models here
│   ├── llama-3-8b-q4.gguf          # Auto-detected as GGUF
│   ├── phi-3-mini.gguf             # Auto-detected
│   ├── embeddings/
│   │   └── all-minilm-l6-v2.onnx   # Auto-detected as embedding
│   └── nlu/
│       └── mobilebert-intent.onnx  # Auto-detected as NLU
│
├── plugins/                         # Drop plugins here
│   ├── llm-ollama.so               # Auto-loaded
│   ├── tool-weather.wasm           # Auto-loaded
│   └── embedding-cohere.so         # Auto-loaded
│
└── config/                          # Optional overrides
    └── models.toml                  # Manual configuration
```

### 3.2 Model Registry Configuration

```toml
# config/models.toml - Optional, for advanced configuration

[registry]
auto_discover = true
scan_paths = ["./models", "~/.rustai/models"]
hot_reload = true
scan_interval_secs = 30

# Manual model registration (optional - auto-discovery handles most cases)
[[models]]
id = "llama-3-8b-fast"
provider = "gguf"                    # Which plugin handles this
location = "./models/llama-3-8b-q4.gguf"
priority = 100                       # Higher = preferred
tags = ["general", "fast", "mobile"]

[models.config]
context_length = 4096
gpu_layers = 0                       # CPU only for mobile
threads = 4

[models.constraints]
min_memory_mb = 4000
max_battery_draw_mw = 500

[[models]]
id = "gpt-4o"
provider = "openai"
location = "https://api.openai.com/v1"
priority = 50                        # Fallback when local unavailable
tags = ["general", "cloud", "high-quality"]

[models.config]
api_key_env = "OPENAI_API_KEY"       # Read from environment
model = "gpt-4o"

[[models]]
id = "claude-3-sonnet"
provider = "anthropic"
location = "https://api.anthropic.com"
priority = 50
tags = ["general", "cloud", "reasoning"]

[models.config]
api_key_env = "ANTHROPIC_API_KEY"
model = "claude-3-sonnet-20240229"

[[models]]
id = "local-ollama"
provider = "ollama"
location = "http://localhost:11434"
priority = 80
tags = ["general", "local", "server"]

[models.config]
model = "llama3:8b"
```

### 3.3 Model Selection Algorithm

```rust
/// Intelligent model selection based on constraints
pub struct ModelSelector {
    registry: ModelRegistry,
    resource_monitor: ResourceMonitor,
}

impl ModelSelector {
    /// Select best model for current conditions
    pub fn select(&self, request: &SelectionCriteria) -> Result<ModelHandle> {
        let available_resources = self.resource_monitor.current();

        // Filter models that meet requirements
        let candidates: Vec<_> = self.registry
            .models()
            .filter(|m| self.meets_requirements(m, &available_resources, request))
            .collect();

        // Score and rank
        let scored: Vec<_> = candidates
            .into_iter()
            .map(|m| (m, self.score(m, request, &available_resources)))
            .collect();

        // Return highest scoring
        scored
            .into_iter()
            .max_by(|a, b| a.1.partial_cmp(&b.1).unwrap())
            .map(|(m, _)| m)
            .ok_or(Error::NoSuitableModel)
    }

    fn score(
        &self,
        model: &Model,
        criteria: &SelectionCriteria,
        resources: &AvailableResources
    ) -> f32 {
        let mut score = model.priority as f32;

        // Prefer local over cloud
        if criteria.prefer_local && model.is_local() {
            score += 50.0;
        }

        // Battery efficiency bonus
        if criteria.battery_sensitive {
            let efficiency = 1.0 - (model.requirements.battery_draw_mw as f32 / 1000.0);
            score += efficiency * 30.0;
        }

        // Speed bonus
        if criteria.prefer_speed {
            score += model.benchmark_tokens_per_sec * 0.5;
        }

        // Quality bonus
        if criteria.prefer_quality {
            score += model.quality_score * 20.0;
        }

        // Tag matching
        for tag in &criteria.required_tags {
            if model.tags.contains(tag) {
                score += 10.0;
            }
        }

        score
    }
}

/// Selection criteria from application
pub struct SelectionCriteria {
    pub task: TaskType,
    pub prefer_local: bool,
    pub prefer_speed: bool,
    pub prefer_quality: bool,
    pub battery_sensitive: bool,
    pub max_latency_ms: Option<u32>,
    pub required_tags: Vec<String>,
    pub required_capabilities: Vec<Capability>,
}
```

### 3.4 Hot-Reload System

```rust
/// Watch for model/plugin changes and hot-reload
pub struct HotReloader {
    watcher: notify::Watcher,
    registry: Arc<RwLock<ModelRegistry>>,
    plugin_manager: Arc<RwLock<PluginManager>>,
}

impl HotReloader {
    pub fn start(&mut self) -> Result<()> {
        self.watcher.watch("./models", RecursiveMode::Recursive)?;
        self.watcher.watch("./plugins", RecursiveMode::Recursive)?;

        // Handle events
        for event in self.watcher.rx.iter() {
            match event {
                Event::Create(path) | Event::Modify(path) => {
                    self.handle_change(&path)?;
                }
                Event::Remove(path) => {
                    self.handle_removal(&path)?;
                }
                _ => {}
            }
        }

        Ok(())
    }

    fn handle_change(&self, path: &Path) -> Result<()> {
        let extension = path.extension().and_then(|e| e.to_str());

        match extension {
            Some("gguf") | Some("onnx") | Some("safetensors") => {
                // New or updated model
                log::info!("Model change detected: {:?}", path);
                let mut registry = self.registry.write().unwrap();
                registry.reload_model(path)?;
            }
            Some("so") | Some("dylib") | Some("dll") | Some("wasm") => {
                // New or updated plugin
                log::info!("Plugin change detected: {:?}", path);
                let mut manager = self.plugin_manager.write().unwrap();
                manager.hot_reload(path)?;
            }
            _ => {}
        }

        Ok(())
    }
}
```

---

## 4. Mobile Optimization Engine

### 4.1 Resource Monitor

```rust
/// Real-time system resource monitoring
pub struct ResourceMonitor {
    platform: Box<dyn PlatformMonitor>,
    history: RingBuffer<ResourceSnapshot>,
    config: MonitorConfig,
}

#[derive(Clone, Debug)]
pub struct ResourceSnapshot {
    pub timestamp: Instant,

    // Memory
    pub memory_used_bytes: u64,
    pub memory_available_bytes: u64,
    pub memory_pressure: MemoryPressure,

    // CPU
    pub cpu_usage_percent: f32,
    pub cpu_temperature_celsius: Option<f32>,
    pub thermal_state: ThermalState,

    // Battery (mobile)
    pub battery_percent: Option<u8>,
    pub battery_state: BatteryState,
    pub power_draw_mw: Option<u32>,

    // GPU (if available)
    pub gpu_memory_used: Option<u64>,
    pub gpu_utilization: Option<f32>,
}

#[derive(Clone, Debug, PartialEq)]
pub enum ThermalState {
    Nominal,      // Normal operation
    Fair,         // Slightly warm
    Serious,      // Throttling recommended
    Critical,     // Must reduce load immediately
}

#[derive(Clone, Debug, PartialEq)]
pub enum BatteryState {
    Charging,
    Discharging,
    Full,
    Unknown,
}

#[derive(Clone, Debug, PartialEq)]
pub enum MemoryPressure {
    Normal,
    Warning,
    Critical,
}
```

### 4.2 Adaptive Inference Engine

```rust
/// Automatically adjusts inference parameters based on device state
pub struct AdaptiveInference {
    monitor: Arc<ResourceMonitor>,
    config: AdaptiveConfig,
}

pub struct AdaptiveConfig {
    /// Enable battery-aware throttling
    pub battery_aware: bool,

    /// Battery threshold for aggressive throttling (0-100)
    pub battery_throttle_threshold: u8,

    /// Enable thermal throttling
    pub thermal_aware: bool,

    /// Enable memory pressure responses
    pub memory_aware: bool,

    /// Minimum quality threshold (0.0 - 1.0)
    pub min_quality: f32,
}

impl AdaptiveInference {
    /// Get adjusted inference parameters for current conditions
    pub fn get_params(&self, base_params: InferenceParams) -> InferenceParams {
        let snapshot = self.monitor.current();
        let mut params = base_params;

        // Thermal throttling
        if self.config.thermal_aware {
            match snapshot.thermal_state {
                ThermalState::Critical => {
                    params.max_tokens = params.max_tokens.min(50);
                    params.batch_size = 1;
                    params.use_gpu = false;
                    log::warn!("Critical thermal state - aggressive throttling");
                }
                ThermalState::Serious => {
                    params.max_tokens = params.max_tokens.min(200);
                    params.batch_size = params.batch_size.min(2);
                    log::info!("Serious thermal state - moderate throttling");
                }
                ThermalState::Fair => {
                    params.batch_size = params.batch_size.min(4);
                }
                ThermalState::Nominal => {}
            }
        }

        // Battery awareness
        if self.config.battery_aware {
            if let Some(battery) = snapshot.battery_percent {
                if snapshot.battery_state == BatteryState::Discharging {
                    if battery < self.config.battery_throttle_threshold {
                        // Low battery - prefer cloud or reduce local load
                        params.prefer_cloud = true;
                        params.threads = params.threads.min(2);
                        log::info!("Low battery ({}%) - reducing local inference", battery);
                    }
                }
            }
        }

        // Memory pressure
        if self.config.memory_aware {
            match snapshot.memory_pressure {
                MemoryPressure::Critical => {
                    params.context_length = params.context_length.min(1024);
                    params.cache_enabled = false;
                    log::warn!("Critical memory pressure - reducing context");
                }
                MemoryPressure::Warning => {
                    params.context_length = params.context_length.min(2048);
                }
                MemoryPressure::Normal => {}
            }
        }

        params
    }

    /// Estimate battery drain for a request
    pub fn estimate_battery_drain(&self, request: &InferenceRequest) -> BatteryEstimate {
        let model = self.get_model(&request.model_id);
        let tokens = request.estimated_tokens();

        BatteryEstimate {
            mwh: (model.requirements.battery_draw_mw as f32 * tokens as f32 / 3600.0) as u32,
            duration_ms: (tokens as f32 / model.tokens_per_sec * 1000.0) as u32,
            percent_drain: 0.01, // Rough estimate
        }
    }
}
```

### 4.3 Power Profiles

```rust
/// Predefined power profiles for different scenarios
pub enum PowerProfile {
    /// Maximum performance, ignore battery
    Performance,

    /// Balanced performance and battery
    Balanced,

    /// Prioritize battery life
    PowerSaver,

    /// Extreme battery saving (cloud-only)
    UltraSaver,

    /// Custom profile
    Custom(PowerProfileConfig),
}

pub struct PowerProfileConfig {
    pub max_cpu_threads: u32,
    pub use_gpu: bool,
    pub prefer_cloud: bool,
    pub max_local_tokens: u32,
    pub inference_quality: f32,        // 0.0 - 1.0
    pub batch_size: u32,
    pub context_length: u32,
    pub enable_caching: bool,
    pub background_inference: bool,
}

impl PowerProfile {
    pub fn to_config(&self) -> PowerProfileConfig {
        match self {
            PowerProfile::Performance => PowerProfileConfig {
                max_cpu_threads: 8,
                use_gpu: true,
                prefer_cloud: false,
                max_local_tokens: 4096,
                inference_quality: 1.0,
                batch_size: 8,
                context_length: 8192,
                enable_caching: true,
                background_inference: true,
            },
            PowerProfile::Balanced => PowerProfileConfig {
                max_cpu_threads: 4,
                use_gpu: true,
                prefer_cloud: false,
                max_local_tokens: 2048,
                inference_quality: 0.8,
                batch_size: 4,
                context_length: 4096,
                enable_caching: true,
                background_inference: false,
            },
            PowerProfile::PowerSaver => PowerProfileConfig {
                max_cpu_threads: 2,
                use_gpu: false,
                prefer_cloud: true,
                max_local_tokens: 512,
                inference_quality: 0.6,
                batch_size: 1,
                context_length: 2048,
                enable_caching: false,
                background_inference: false,
            },
            PowerProfile::UltraSaver => PowerProfileConfig {
                max_cpu_threads: 1,
                use_gpu: false,
                prefer_cloud: true,
                max_local_tokens: 0,  // Cloud only
                inference_quality: 0.5,
                batch_size: 1,
                context_length: 1024,
                enable_caching: false,
                background_inference: false,
            },
            PowerProfile::Custom(config) => config.clone(),
        }
    }
}
```

---

## 5. Core Engine Design

### 5.1 Minimal Core Architecture

The core is intentionally minimal - just a plugin loader and request router:

```rust
/// The RustAI core - minimal, everything else is plugins
pub struct RustAI {
    /// Plugin manager handles all capabilities
    plugins: PluginManager,

    /// Model registry for model discovery
    registry: ModelRegistry,

    /// Resource monitoring
    monitor: ResourceMonitor,

    /// Adaptive inference
    adaptive: AdaptiveInference,

    /// Metrics collection
    metrics: MetricsEngine,

    /// Event bus for plugin communication
    events: EventBus,

    /// Configuration
    config: RustAIConfig,
}

impl RustAI {
    /// Create new instance with auto-discovery
    pub fn new(config: RustAIConfig) -> Result<Self> {
        let mut plugins = PluginManager::new();

        // Load built-in plugins
        plugins.register_builtin::<GGUFPlugin>()?;
        plugins.register_builtin::<CandlePlugin>()?;
        plugins.register_builtin::<OpenAIPlugin>()?;
        plugins.register_builtin::<MiniLMPlugin>()?;
        plugins.register_builtin::<HNSWPlugin>()?;

        // Auto-discover external plugins
        plugins.discover(&config.plugin_paths)?;

        // Auto-discover models
        let registry = ModelRegistry::discover(&config.model_paths)?;

        // Start resource monitoring
        let monitor = ResourceMonitor::start()?;

        // Setup adaptive inference
        let adaptive = AdaptiveInference::new(
            Arc::clone(&monitor),
            config.adaptive.clone(),
        );

        Ok(Self {
            plugins,
            registry,
            monitor,
            adaptive,
            metrics: MetricsEngine::new(),
            events: EventBus::new(),
            config,
        })
    }

    /// Generate text using best available model
    pub async fn generate(&self, request: GenerateRequest) -> Result<GenerateResponse> {
        // Select best model for current conditions
        let criteria = SelectionCriteria::from_request(&request, &self.monitor.current());
        let model = self.registry.select(&criteria)?;

        // Get adaptive parameters
        let params = self.adaptive.get_params(request.params);

        // Get the LLM plugin for this model
        let plugin = self.plugins.get_llm(&model.provider)?;

        // Execute with metrics
        let start = Instant::now();
        let response = plugin.generate(&model, &request.prompt, &params).await?;

        // Record metrics
        self.metrics.record(MetricEvent::Generation {
            model_id: model.id.clone(),
            tokens: response.tokens_generated,
            latency: start.elapsed(),
            battery_draw: self.monitor.current().power_draw_mw,
        });

        Ok(response)
    }

    /// Stream generation
    pub fn generate_stream(
        &self,
        request: GenerateRequest,
    ) -> impl Stream<Item = Result<Token>> {
        // Similar to generate but returns a stream
        async_stream::stream! {
            let model = self.registry.select(&criteria)?;
            let plugin = self.plugins.get_llm(&model.provider)?;

            let stream = plugin.generate_stream(&model, &request.prompt, &params).await?;

            for await token in stream {
                yield token;
            }
        }
    }
}
```

### 5.2 Plugin Manager

```rust
/// Manages all plugins
pub struct PluginManager {
    /// Loaded plugins by category
    llm_plugins: HashMap<String, Box<dyn LLMPlugin>>,
    embedding_plugins: HashMap<String, Box<dyn EmbeddingPlugin>>,
    vector_plugins: HashMap<String, Box<dyn VectorPlugin>>,
    nlu_plugins: HashMap<String, Box<dyn NLUPlugin>>,
    tool_plugins: HashMap<String, Box<dyn ToolPlugin>>,

    /// Native plugin loader
    native_loader: NativeLoader,

    /// WASM plugin loader
    wasm_loader: WasmLoader,

    /// Plugin directory watcher
    watcher: Option<HotReloader>,
}

impl PluginManager {
    /// Discover and load plugins from directories
    pub fn discover(&mut self, paths: &[PathBuf]) -> Result<Vec<PluginInfo>> {
        let mut discovered = Vec::new();

        for path in paths {
            for entry in walkdir::WalkDir::new(path) {
                let entry = entry?;
                let path = entry.path();

                if let Some(info) = self.try_load(path)? {
                    discovered.push(info);
                }
            }
        }

        Ok(discovered)
    }

    /// Try to load a plugin from a path
    fn try_load(&mut self, path: &Path) -> Result<Option<PluginInfo>> {
        let ext = path.extension().and_then(|e| e.to_str());

        match ext {
            Some("so") | Some("dylib") | Some("dll") => {
                // Native plugin
                let plugin = self.native_loader.load(path)?;
                let info = plugin.info();
                self.register_by_category(plugin)?;
                Ok(Some(info))
            }
            Some("wasm") => {
                // WASM plugin (sandboxed)
                let plugin = self.wasm_loader.load(path)?;
                let info = plugin.info();
                self.register_by_category(plugin)?;
                Ok(Some(info))
            }
            _ => Ok(None),
        }
    }

    /// Get LLM plugin by provider name
    pub fn get_llm(&self, provider: &str) -> Result<&dyn LLMPlugin> {
        self.llm_plugins
            .get(provider)
            .map(|p| p.as_ref())
            .ok_or(Error::PluginNotFound(provider.into()))
    }
}
```

---

## 6. Plugin Specifications

### 6.1 LLM Plugin Interface

```rust
/// LLM plugin for text generation
#[async_trait]
pub trait LLMPlugin: Plugin {
    /// Load a model
    async fn load_model(&mut self, model: &ModelConfig) -> Result<ModelHandle>;

    /// Unload a model
    async fn unload_model(&mut self, handle: ModelHandle) -> Result<()>;

    /// Generate text
    async fn generate(
        &self,
        handle: ModelHandle,
        prompt: &str,
        params: &GenerateParams,
    ) -> Result<GenerateResponse>;

    /// Generate with streaming
    fn generate_stream(
        &self,
        handle: ModelHandle,
        prompt: &str,
        params: &GenerateParams,
    ) -> Pin<Box<dyn Stream<Item = Result<Token>> + Send>>;

    /// Get model info
    fn model_info(&self, handle: ModelHandle) -> Result<ModelInfo>;

    /// Supported model formats
    fn supported_formats(&self) -> Vec<ModelFormat>;

    /// Tokenize text (for token counting)
    fn tokenize(&self, handle: ModelHandle, text: &str) -> Result<Vec<u32>>;

    /// Count tokens
    fn count_tokens(&self, handle: ModelHandle, text: &str) -> Result<u32>;
}

#[derive(Clone, Debug)]
pub struct GenerateParams {
    pub max_tokens: u32,
    pub temperature: f32,
    pub top_p: f32,
    pub top_k: u32,
    pub repetition_penalty: f32,
    pub stop_sequences: Vec<String>,
    pub grammar: Option<String>,     // GBNF grammar
    pub json_schema: Option<String>, // JSON schema constraint
}

#[derive(Clone, Debug)]
pub struct GenerateResponse {
    pub text: String,
    pub tokens_generated: u32,
    pub tokens_prompt: u32,
    pub finish_reason: FinishReason,
    pub timing: GenerateTiming,
}

#[derive(Clone, Debug)]
pub struct GenerateTiming {
    pub prompt_eval_ms: u64,
    pub generation_ms: u64,
    pub tokens_per_second: f32,
}
```

### 6.2 Embedding Plugin Interface

```rust
/// Embedding plugin for vector generation
#[async_trait]
pub trait EmbeddingPlugin: Plugin {
    /// Load embedding model
    async fn load_model(&mut self, config: &ModelConfig) -> Result<ModelHandle>;

    /// Generate embeddings for text
    async fn embed(&self, handle: ModelHandle, texts: &[&str]) -> Result<Vec<Embedding>>;

    /// Embedding dimensions
    fn dimensions(&self, handle: ModelHandle) -> u32;

    /// Maximum input length
    fn max_input_length(&self, handle: ModelHandle) -> u32;
}

#[derive(Clone, Debug)]
pub struct Embedding {
    pub vector: Vec<f32>,
    pub dimensions: u32,
}
```

### 6.3 Vector Store Plugin Interface

```rust
/// Vector store plugin for similarity search
#[async_trait]
pub trait VectorPlugin: Plugin {
    /// Create a new collection
    async fn create_collection(&mut self, config: &CollectionConfig) -> Result<CollectionHandle>;

    /// Insert vectors
    async fn insert(
        &self,
        collection: CollectionHandle,
        vectors: &[VectorEntry],
    ) -> Result<Vec<String>>;

    /// Search similar vectors
    async fn search(
        &self,
        collection: CollectionHandle,
        query: &[f32],
        top_k: u32,
        filter: Option<Filter>,
    ) -> Result<Vec<SearchResult>>;

    /// Delete vectors
    async fn delete(
        &self,
        collection: CollectionHandle,
        ids: &[String],
    ) -> Result<()>;

    /// Get collection stats
    fn stats(&self, collection: CollectionHandle) -> Result<CollectionStats>;
}

#[derive(Clone, Debug)]
pub struct VectorEntry {
    pub id: Option<String>,
    pub vector: Vec<f32>,
    pub payload: HashMap<String, Value>,
}

#[derive(Clone, Debug)]
pub struct SearchResult {
    pub id: String,
    pub score: f32,
    pub payload: HashMap<String, Value>,
}
```

### 6.4 Tool Plugin Interface

```rust
/// Tool plugin for function calling
#[async_trait]
pub trait ToolPlugin: Plugin {
    /// Get tool definitions
    fn definitions(&self) -> Vec<ToolDefinition>;

    /// Execute a tool
    async fn execute(
        &self,
        name: &str,
        parameters: Value,
    ) -> Result<ToolResult>;
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct ToolDefinition {
    pub name: String,
    pub description: String,
    pub parameters: JsonSchema,
}

#[derive(Clone, Debug)]
pub struct ToolResult {
    pub output: Value,
    pub error: Option<String>,
}
```

---

## 7. Resource Management

### 7.1 Memory Pool

```rust
/// Memory pool for efficient allocation
pub struct MemoryPool {
    /// Pre-allocated buffers for common sizes
    pools: HashMap<usize, Vec<Vec<u8>>>,

    /// Current allocations
    allocated: AtomicUsize,

    /// Maximum allowed
    max_bytes: usize,

    /// Platform-specific allocator
    allocator: Box<dyn Allocator>,
}

impl MemoryPool {
    /// Allocate from pool
    pub fn allocate(&self, size: usize) -> Result<PooledBuffer> {
        // Check memory budget
        if self.allocated.load(Ordering::Relaxed) + size > self.max_bytes {
            return Err(Error::MemoryBudgetExceeded);
        }

        // Try to get from pool
        if let Some(buffer) = self.try_get_pooled(size) {
            return Ok(buffer);
        }

        // Allocate new
        self.allocate_new(size)
    }

    /// Return buffer to pool
    pub fn release(&self, buffer: PooledBuffer) {
        self.allocated.fetch_sub(buffer.len(), Ordering::Relaxed);
        self.return_to_pool(buffer);
    }

    /// Set memory budget based on available resources
    pub fn set_budget(&mut self, budget: MemoryBudget) {
        self.max_bytes = budget.max_bytes;
    }
}
```

### 7.2 Model Caching

```rust
/// LRU cache for loaded models
pub struct ModelCache {
    /// Cached models
    cache: LruCache<String, CachedModel>,

    /// Total memory used
    memory_used: AtomicU64,

    /// Memory budget
    budget: u64,
}

impl ModelCache {
    /// Get or load a model
    pub async fn get_or_load(
        &mut self,
        model_id: &str,
        loader: impl FnOnce() -> Result<LoadedModel>,
    ) -> Result<&LoadedModel> {
        if !self.cache.contains(model_id) {
            // Check if we need to evict
            let model = loader()?;
            let size = model.memory_size();

            while self.memory_used.load(Ordering::Relaxed) + size > self.budget {
                if let Some((_, evicted)) = self.cache.pop_lru() {
                    self.memory_used.fetch_sub(evicted.size, Ordering::Relaxed);
                    log::info!("Evicted model {} to free memory", evicted.id);
                } else {
                    return Err(Error::ModelTooLarge);
                }
            }

            self.cache.put(model_id.into(), CachedModel {
                id: model_id.into(),
                model,
                size,
                last_used: Instant::now(),
            });

            self.memory_used.fetch_add(size, Ordering::Relaxed);
        }

        Ok(&self.cache.get(model_id).unwrap().model)
    }
}
```

---

## 8. Hot-Reload & Updates

### 8.1 Plugin Hot-Reload

```rust
/// Hot-reload a plugin without restart
impl PluginManager {
    pub fn hot_reload(&mut self, path: &Path) -> Result<()> {
        let plugin_id = self.get_plugin_id(path)?;

        // Get old plugin
        let old_plugin = self.get_plugin(&plugin_id)?;

        // Notify dependents
        self.events.emit(Event::PluginReloading(plugin_id.clone()));

        // Load new version
        let new_plugin = self.native_loader.load(path)?;

        // Validate compatibility
        if !self.is_compatible(&old_plugin, &new_plugin) {
            return Err(Error::IncompatiblePluginVersion);
        }

        // Transfer state if possible
        if let Some(state) = old_plugin.export_state() {
            new_plugin.import_state(state)?;
        }

        // Swap plugins
        self.replace_plugin(plugin_id.clone(), new_plugin)?;

        // Notify completion
        self.events.emit(Event::PluginReloaded(plugin_id));

        Ok(())
    }
}
```

### 8.2 Model Hot-Swap

```rust
/// Hot-swap a model while preserving conversation state
impl ModelRegistry {
    pub async fn hot_swap(
        &mut self,
        old_model_id: &str,
        new_model_id: &str,
    ) -> Result<()> {
        // Get conversation state from old model
        let old_model = self.get(old_model_id)?;
        let state = old_model.export_kv_cache()?;

        // Load new model
        let new_model = self.load(new_model_id)?;

        // Try to transfer state (may fail if architectures differ)
        if let Err(e) = new_model.import_kv_cache(&state) {
            log::warn!("Could not transfer KV cache: {}", e);
            // Continue without state - conversation context lost
        }

        // Update active model
        self.set_active(new_model_id)?;

        Ok(())
    }
}
```

### 8.3 Over-the-Air Updates

```rust
/// OTA update system for models and plugins
pub struct UpdateManager {
    registry: Arc<ModelRegistry>,
    plugins: Arc<PluginManager>,
    config: UpdateConfig,
}

impl UpdateManager {
    /// Check for updates
    pub async fn check_updates(&self) -> Result<Vec<Update>> {
        let mut updates = Vec::new();

        // Check model updates
        for model in self.registry.models() {
            if let Some(update) = self.check_model_update(&model).await? {
                updates.push(update);
            }
        }

        // Check plugin updates
        for plugin in self.plugins.all() {
            if let Some(update) = self.check_plugin_update(&plugin).await? {
                updates.push(update);
            }
        }

        Ok(updates)
    }

    /// Download and apply update
    pub async fn apply_update(&self, update: &Update) -> Result<()> {
        // Download to temp location
        let temp_path = self.download(&update.url).await?;

        // Verify checksum
        self.verify_checksum(&temp_path, &update.checksum)?;

        // Apply based on type
        match &update.update_type {
            UpdateType::Model { id } => {
                self.registry.hot_swap_from_file(id, &temp_path)?;
            }
            UpdateType::Plugin { id } => {
                self.plugins.hot_reload(&temp_path)?;
            }
        }

        Ok(())
    }
}
```

---

## 9. Example Plugins

### 9.1 GGUF LLM Plugin (Built-in)

```rust
// plugins/builtin/gguf/src/lib.rs

use rustai_plugin::{Plugin, LLMPlugin, export_plugin};
use llama_cpp::{LlamaModel, LlamaContext, LlamaParams};

pub struct GGUFPlugin {
    models: HashMap<ModelHandle, LoadedModel>,
    next_handle: AtomicU64,
}

struct LoadedModel {
    model: LlamaModel,
    context: LlamaContext,
    tokenizer: Tokenizer,
}

impl Plugin for GGUFPlugin {
    fn id(&self) -> &str { "gguf" }
    fn name(&self) -> &str { "GGUF/GGML Model Plugin" }
    fn version(&self) -> Version { Version::new(1, 0, 0) }
    fn category(&self) -> PluginCategory { PluginCategory::LLM }

    fn capabilities(&self) -> Vec<Capability> {
        vec![
            Capability::LLM {
                formats: vec!["gguf".into(), "ggml".into()],
                features: vec![
                    "streaming".into(),
                    "grammar".into(),
                    "json_mode".into(),
                ],
            }
        ]
    }

    fn requirements(&self) -> ResourceRequirements {
        ResourceRequirements {
            min_memory: 1024 * 1024 * 1024, // 1GB minimum
            recommended_memory: 4 * 1024 * 1024 * 1024,
            preferred_cores: 4,
            requires_gpu: false,
            battery_draw_mw: 300,
            thermal_impact: 60,
            background_capable: true,
        }
    }
}

#[async_trait]
impl LLMPlugin for GGUFPlugin {
    async fn load_model(&mut self, config: &ModelConfig) -> Result<ModelHandle> {
        let params = LlamaParams {
            n_ctx: config.context_length.unwrap_or(4096),
            n_gpu_layers: config.gpu_layers.unwrap_or(0),
            n_threads: config.threads.unwrap_or(4),
            use_mmap: true,
            use_mlock: false,
        };

        let model = LlamaModel::load_from_file(&config.path, &params)?;
        let context = model.create_context(&params)?;
        let tokenizer = Tokenizer::from_model(&model)?;

        let handle = ModelHandle(self.next_handle.fetch_add(1, Ordering::Relaxed));

        self.models.insert(handle, LoadedModel {
            model,
            context,
            tokenizer,
        });

        Ok(handle)
    }

    fn generate_stream(
        &self,
        handle: ModelHandle,
        prompt: &str,
        params: &GenerateParams,
    ) -> Pin<Box<dyn Stream<Item = Result<Token>> + Send>> {
        let loaded = self.models.get(&handle).expect("Model not loaded");

        Box::pin(async_stream::stream! {
            // Tokenize prompt
            let tokens = loaded.tokenizer.encode(prompt)?;

            // Setup sampling
            let sampler = Sampler::new(params);

            // Generate tokens
            loaded.context.eval(&tokens)?;

            for _ in 0..params.max_tokens {
                let logits = loaded.context.get_logits();
                let token_id = sampler.sample(&logits);

                if token_id == loaded.tokenizer.eos_token() {
                    yield Ok(Token {
                        text: "".into(),
                        logprob: 0.0,
                        is_final: true,
                    });
                    break;
                }

                let text = loaded.tokenizer.decode(&[token_id])?;

                yield Ok(Token {
                    text,
                    logprob: logits[token_id as usize],
                    is_final: false,
                });

                loaded.context.eval(&[token_id])?;
            }
        })
    }
}

export_plugin!(GGUFPlugin);
```

### 9.2 OpenAI Cloud Plugin

```rust
// plugins/builtin/openai/src/lib.rs

pub struct OpenAIPlugin {
    client: reqwest::Client,
    api_key: Option<String>,
}

impl Plugin for OpenAIPlugin {
    fn id(&self) -> &str { "openai" }
    fn name(&self) -> &str { "OpenAI API Plugin" }

    fn capabilities(&self) -> Vec<Capability> {
        vec![
            Capability::LLM {
                formats: vec!["api".into()],
                features: vec![
                    "streaming".into(),
                    "json_mode".into(),
                    "function_calling".into(),
                    "vision".into(),
                ],
            },
            Capability::Embedding {
                models: vec!["text-embedding-3-small".into(), "text-embedding-3-large".into()],
            },
        ]
    }

    fn requirements(&self) -> ResourceRequirements {
        ResourceRequirements {
            min_memory: 10 * 1024 * 1024,  // 10MB for HTTP client
            recommended_memory: 50 * 1024 * 1024,
            preferred_cores: 1,
            requires_gpu: false,
            battery_draw_mw: 50,  // Just network
            thermal_impact: 5,
            background_capable: true,
        }
    }
}

#[async_trait]
impl LLMPlugin for OpenAIPlugin {
    fn generate_stream(
        &self,
        handle: ModelHandle,
        prompt: &str,
        params: &GenerateParams,
    ) -> Pin<Box<dyn Stream<Item = Result<Token>> + Send>> {
        let client = self.client.clone();
        let api_key = self.api_key.clone().expect("API key required");
        let model = self.get_model_name(handle);

        Box::pin(async_stream::stream! {
            let response = client
                .post("https://api.openai.com/v1/chat/completions")
                .header("Authorization", format!("Bearer {}", api_key))
                .json(&json!({
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "max_tokens": params.max_tokens,
                    "temperature": params.temperature,
                    "stream": true,
                }))
                .send()
                .await?;

            let mut stream = response.bytes_stream();

            while let Some(chunk) = stream.next().await {
                let chunk = chunk?;
                let text = String::from_utf8_lossy(&chunk);

                for line in text.lines() {
                    if line.starts_with("data: ") {
                        let data = &line[6..];
                        if data == "[DONE]" {
                            yield Ok(Token {
                                text: "".into(),
                                logprob: 0.0,
                                is_final: true,
                            });
                            break;
                        }

                        let parsed: OpenAIChunk = serde_json::from_str(data)?;
                        if let Some(content) = parsed.choices[0].delta.content {
                            yield Ok(Token {
                                text: content,
                                logprob: 0.0,
                                is_final: false,
                            });
                        }
                    }
                }
            }
        })
    }
}
```

### 9.3 Custom Weather Tool Plugin (WASM)

```rust
// plugins/examples/weather-tool/src/lib.rs
// Compiles to WASM for sandboxed execution

use rustai_plugin_wasm::{Plugin, ToolPlugin, export_wasm_plugin};

#[derive(Default)]
pub struct WeatherPlugin;

impl Plugin for WeatherPlugin {
    fn id(&self) -> &str { "weather" }
    fn name(&self) -> &str { "Weather Tool" }
}

#[async_trait]
impl ToolPlugin for WeatherPlugin {
    fn definitions(&self) -> Vec<ToolDefinition> {
        vec![
            ToolDefinition {
                name: "get_weather".into(),
                description: "Get current weather for a location".into(),
                parameters: json_schema!({
                    "type": "object",
                    "properties": {
                        "location": {
                            "type": "string",
                            "description": "City name or coordinates"
                        },
                        "units": {
                            "type": "string",
                            "enum": ["celsius", "fahrenheit"],
                            "default": "celsius"
                        }
                    },
                    "required": ["location"]
                }),
            },
            ToolDefinition {
                name: "get_forecast".into(),
                description: "Get weather forecast for next N days".into(),
                parameters: json_schema!({
                    "type": "object",
                    "properties": {
                        "location": { "type": "string" },
                        "days": { "type": "integer", "minimum": 1, "maximum": 7 }
                    },
                    "required": ["location"]
                }),
            },
        ]
    }

    async fn execute(&self, name: &str, params: Value) -> Result<ToolResult> {
        match name {
            "get_weather" => {
                let location = params["location"].as_str().unwrap();

                // In WASM, we use WASI HTTP to make requests
                let response = wasi_http::get(
                    &format!("https://api.weather.com/v1/current?q={}", location)
                ).await?;

                Ok(ToolResult {
                    output: response.json()?,
                    error: None,
                })
            }
            "get_forecast" => {
                // Similar implementation
                todo!()
            }
            _ => Err(Error::UnknownTool(name.into())),
        }
    }
}

export_wasm_plugin!(WeatherPlugin);
```

---

## 10. API Design

### 10.1 UniFFI Interface

```udl
// rustai.udl - Complete UniFFI interface

namespace rustai {
    [Throws=RustAIError]
    RustAI create(RustAIConfig config);

    [Throws=RustAIError]
    RustAI create_with_defaults();
};

[Error]
enum RustAIError {
    "ConfigError",
    "ModelNotFound",
    "PluginNotFound",
    "PluginLoadError",
    "InferenceError",
    "MemoryError",
    "NetworkError",
    "Timeout",
};

dictionary RustAIConfig {
    sequence<string> model_paths;
    sequence<string> plugin_paths;
    PowerProfile power_profile;
    boolean auto_discover;
    boolean hot_reload;
    AdaptiveConfig adaptive;
};

enum PowerProfile {
    "Performance",
    "Balanced",
    "PowerSaver",
    "UltraSaver",
};

dictionary AdaptiveConfig {
    boolean battery_aware;
    u8 battery_throttle_threshold;
    boolean thermal_aware;
    boolean memory_aware;
};

interface RustAI {
    // ===== Model Management =====

    /// List available models
    sequence<ModelInfo> list_models();

    /// Get model by ID
    [Throws=RustAIError]
    ModelInfo get_model(string model_id);

    /// Load a model explicitly
    [Throws=RustAIError]
    void load_model(string model_id);

    /// Unload a model
    [Throws=RustAIError]
    void unload_model(string model_id);

    // ===== Generation =====

    /// Generate text (sync)
    [Throws=RustAIError]
    GenerateResponse generate(GenerateRequest request);

    /// Generate with streaming
    [Throws=RustAIError]
    void generate_stream(GenerateRequest request, StreamCallback callback);

    /// Generate with automatic model selection
    [Throws=RustAIError]
    GenerateResponse generate_auto(string prompt, SelectionCriteria criteria);

    // ===== NLU =====

    /// Classify intent
    [Throws=RustAIError]
    ClassificationResult classify(string text);

    /// Extract entities
    [Throws=RustAIError]
    sequence<Entity> extract_entities(string text);

    // ===== RAG =====

    /// Index a document
    [Throws=RustAIError]
    void index_document(Document doc);

    /// Index from file
    [Throws=RustAIError]
    void index_file(string path);

    /// Search documents
    [Throws=RustAIError]
    sequence<SearchResult> search(string query, u32 top_k);

    /// Search with filter
    [Throws=RustAIError]
    sequence<SearchResult> search_filtered(string query, Filter filter, u32 top_k);

    // ===== Embeddings =====

    /// Generate embedding
    [Throws=RustAIError]
    Embedding embed(string text);

    /// Batch embeddings
    [Throws=RustAIError]
    sequence<Embedding> embed_batch(sequence<string> texts);

    // ===== Tools =====

    /// List available tools
    sequence<ToolDefinition> list_tools();

    /// Execute a tool
    [Throws=RustAIError]
    ToolResult execute_tool(string name, string params_json);

    // ===== Plugins =====

    /// List plugins
    sequence<PluginInfo> list_plugins();

    /// Load a plugin
    [Throws=RustAIError]
    void load_plugin(string path);

    /// Unload a plugin
    [Throws=RustAIError]
    void unload_plugin(string plugin_id);

    // ===== Resource Management =====

    /// Get current resource usage
    ResourceSnapshot get_resources();

    /// Set power profile
    void set_power_profile(PowerProfile profile);

    /// Get battery estimate for a request
    BatteryEstimate estimate_battery(GenerateRequest request);

    // ===== Metrics =====

    /// Get metrics summary
    MetricsSummary get_metrics();

    /// Reset metrics
    void reset_metrics();

    // ===== Lifecycle =====

    /// Graceful shutdown
    void shutdown();
};

// Callback for streaming
callback interface StreamCallback {
    void on_token(Token token);
    void on_complete(GenerateResponse response);
    void on_error(string message);
};

// Request/Response types
dictionary GenerateRequest {
    string? model_id;          // Optional - auto-select if not specified
    string prompt;
    u32 max_tokens;
    f32 temperature;
    f32 top_p;
    u32 top_k;
    sequence<string> stop_sequences;
    string? grammar;
    string? json_schema;
    SelectionCriteria? criteria;
};

dictionary SelectionCriteria {
    boolean prefer_local;
    boolean prefer_speed;
    boolean prefer_quality;
    boolean battery_sensitive;
    u32? max_latency_ms;
    sequence<string> required_tags;
};

dictionary GenerateResponse {
    string text;
    u32 tokens_generated;
    u32 tokens_prompt;
    string finish_reason;
    f32 tokens_per_second;
    u64 latency_ms;
    string model_used;
};

dictionary Token {
    string text;
    f32 logprob;
    boolean is_final;
};
```

### 10.2 Simple Kotlin Usage

```kotlin
// Simple usage - just works
class ChatViewModel : ViewModel() {
    private val ai = RustAI.createWithDefaults()

    fun chat(userMessage: String) = viewModelScope.launch {
        // Auto-selects best model based on device state
        ai.generateStream(
            GenerateRequest(
                prompt = userMessage,
                maxTokens = 500u,
                temperature = 0.7f,
            ),
            object : StreamCallback {
                override fun onToken(token: Token) {
                    _response.value += token.text
                }
                override fun onComplete(response: GenerateResponse) {
                    Log.d("Chat", "Used model: ${response.modelUsed}")
                    Log.d("Chat", "Speed: ${response.tokensPerSecond} t/s")
                }
                override fun onError(message: String) {
                    _error.value = message
                }
            }
        )
    }
}
```

### 10.3 Advanced Kotlin Usage

```kotlin
// Advanced usage with full control
class AdvancedAIService(context: Context) {
    private val ai: RustAI

    init {
        ai = RustAI.create(RustAIConfig(
            modelPaths = listOf(
                context.filesDir.resolve("models").absolutePath,
                "/sdcard/RustAI/models",
            ),
            pluginPaths = listOf(
                context.filesDir.resolve("plugins").absolutePath,
            ),
            powerProfile = PowerProfile.BALANCED,
            autoDiscover = true,
            hotReload = true,
            adaptive = AdaptiveConfig(
                batteryAware = true,
                batteryThrottleThreshold = 20u,
                thermalAware = true,
                memoryAware = true,
            ),
        ))
    }

    // Generate with specific model
    suspend fun generateWithModel(modelId: String, prompt: String): String {
        return ai.generate(GenerateRequest(
            modelId = modelId,
            prompt = prompt,
            maxTokens = 1000u,
            temperature = 0.8f,
        )).text
    }

    // Smart generation based on context
    suspend fun smartGenerate(prompt: String, preferQuality: Boolean): String {
        val resources = ai.getResources()

        val criteria = SelectionCriteria(
            preferLocal = resources.batteryPercent?.let { it > 30u } ?: true,
            preferSpeed = !preferQuality,
            preferQuality = preferQuality,
            batterySensitive = resources.batteryState == BatteryState.DISCHARGING,
            maxLatencyMs = if (preferQuality) null else 2000u,
        )

        return ai.generateAuto(prompt, criteria).text
    }

    // RAG-enhanced generation
    suspend fun ragGenerate(question: String): String {
        // Search relevant context
        val results = ai.search(question, 3u)
        val context = results.joinToString("\n\n") { it.chunk }

        // Generate with context
        val prompt = """
            Context:
            $context

            Question: $question

            Answer based on the context above:
        """.trimIndent()

        return ai.generate(GenerateRequest(
            prompt = prompt,
            maxTokens = 500u,
            temperature = 0.3f,
        )).text
    }

    // Tool-augmented generation
    suspend fun agentGenerate(prompt: String): String {
        val tools = ai.listTools()
        val toolsJson = tools.map { it.toJson() }

        val systemPrompt = """
            You have access to these tools:
            $toolsJson

            To use a tool, respond with:
            <tool>tool_name</tool>
            <params>{"param": "value"}</params>

            User: $prompt
        """.trimIndent()

        var response = ai.generate(GenerateRequest(
            prompt = systemPrompt,
            maxTokens = 1000u,
        )).text

        // Execute any tool calls
        while (response.contains("<tool>")) {
            val toolName = extractTag(response, "tool")
            val params = extractTag(response, "params")

            val result = ai.executeTool(toolName, params)

            response = ai.generate(GenerateRequest(
                prompt = "$systemPrompt\n\nTool result: ${result.output}\n\nContinue:",
                maxTokens = 500u,
            )).text
        }

        return response
    }
}
```

---

## 11. Implementation Roadmap

### Phase 1: Core Foundation (Weeks 1-6)

```
Week 1-2: Project Setup
├── Rust workspace structure
├── Plugin trait definitions
├── UniFFI scaffolding
└── CI/CD pipeline

Week 3-4: Plugin Infrastructure
├── Native plugin loader (dlopen2)
├── WASM plugin loader (wasmtime)
├── Plugin discovery
└── Plugin lifecycle management

Week 5-6: Model Registry
├── Auto-discovery system
├── Configuration parsing
├── Model selection algorithm
└── Hot-reload system
```

### Phase 2: LLM Plugins (Weeks 7-12)

```
Week 7-8: GGUF Plugin
├── llama_cpp integration
├── Streaming generation
├── Grammar support
└── Context caching

Week 9-10: Cloud Plugins
├── OpenAI plugin
├── Anthropic plugin
├── Ollama plugin
└── Common cloud interface

Week 11-12: Advanced Features
├── Candle plugin (GPU)
├── Model hot-swap
├── Benchmark suite
└── Performance optimization
```

### Phase 3: Supporting Plugins (Weeks 13-18)

```
Week 13-14: Embedding Plugins
├── MiniLM plugin (tract)
├── OpenAI embeddings
├── Batch optimization
└── Caching layer

Week 15-16: Vector Store Plugin
├── HNSW implementation
├── Persistence layer
├── Filtering system
└── Quantization

Week 17-18: NLU & Tokenizer
├── MobileBERT plugin
├── HuggingFace tokenizers
├── Multi-language support
└── Accuracy validation
```

### Phase 4: Mobile Optimization (Weeks 19-22)

```
Week 19-20: Resource Management
├── ResourceMonitor
├── AdaptiveInference
├── Power profiles
├── Memory pooling

Week 21-22: Platform Integration
├── Android integration
├── iOS integration
├── Battery optimization
├── Thermal management
```

### Phase 5: Tools & Updates (Weeks 23-26)

```
Week 23-24: Tool System
├── Tool plugin interface
├── Example tools
├── WASM sandboxing
└── Function calling

Week 25-26: Update System
├── OTA updates
├── Version management
├── Rollback support
└── Integrity verification
```

### Phase 6: Production (Weeks 27-30)

```
Week 27-28: Testing & QA
├── Integration tests
├── Performance benchmarks
├── Memory profiling
├── Security audit

Week 29-30: Documentation & Release
├── API documentation
├── Plugin development guide
├── Migration guide
├── v1.0 release
```

---

## Summary

**RustAI v2** is designed as a **complete rewrite** with:

1. **Everything is a Plugin**: LLMs, embeddings, vectors, tools - all swappable
2. **Zero-Config**: Drop models/plugins, they just work
3. **Mobile-First**: Battery, thermal, memory awareness baked in
4. **Model Registry**: Simple config to point to any model location
5. **Hot-Reload**: Update models and plugins without restart
6. **Future-Proof**: New format? Just add a plugin

This architecture ensures we're not locked into any specific model format, cloud provider, or inference engine. The core is minimal and stable; all intelligence is in plugins.

---

**Next Steps:**
1. Review and approve this architecture
2. Create the Rust workspace structure
3. Define plugin trait interfaces
4. Implement core plugin loader
5. Build first LLM plugin (GGUF)
