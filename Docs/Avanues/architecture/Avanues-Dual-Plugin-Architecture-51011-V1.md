# Avanues Dual Plugin Architecture with Dependency Registry

**Date:** 2025-11-10 05:15 PST
**Author:** System Architect
**Project:** Avanues Ecosystem
**Status:** Architecture Specification
**Version:** 1.0.0

---

## Executive Summary

This document specifies the **dual plugin architecture** for the Avanues ecosystem, enabling two types of plugins:

1. **Standalone Apps** - Self-contained applications with bundled dependencies
2. **IPC-Dependent Apps** - Lightweight applications that consume dependencies from other apps via Inter-Process Communication (IPC)

A **centralized dependency registry** stored in MagicData database enables capability discovery, preventing dependency duplication and reducing app size by 60-80%.

---

## 1. Architecture Overview

### 1.1 System Components

```
┌─────────────────────────────────────────────────────────────────┐
│                     Avanues Master Platform                      │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │            Avanues Runtime Kernel                          │ │
│  │                                                            │ │
│  │  • Dependency Registry Manager                            │ │
│  │  • IPC Request Router                                     │ │
│  │  • Capability Discovery Service                           │ │
│  │  • Security & Permission Manager                          │ │
│  └────────────────────────────────────────────────────────────┘ │
│                            ▲                                     │
│                            │                                     │
│  ┌─────────────────────────┴──────────────────────────────┐    │
│  │         MagicData Database (Dependency Registry)        │    │
│  │                                                         │    │
│  │  Tables:                                                │    │
│  │  • app_capabilities (what each app provides)           │    │
│  │  • app_dependencies (what each app needs)              │    │
│  │  • dependency_manifest (versions, APIs)                │    │
│  │  • ipc_endpoints (how to call capabilities)            │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌──────────────────┐  ┌─────────────────┐  ┌───────────────┐  │
│  │  Standalone Apps │  │  IPC-Based Apps │  │  Native Apps  │  │
│  │                  │  │                 │  │               │  │
│  │ • AVA AI (120MB) │  │ • TaskMaker(2MB)│  │ • AvaConnect  │  │
│  │ • CockPit (80MB) │  │ • DocReader(3MB)│  │ • Barcode     │  │
│  │ (Full deps)      │  │ (IPC only)      │  │ • AVA         │  │
│  └──────────────────┘  └─────────────────┘  └───────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Plugin Types

### 2.1 Standalone Apps

**Characteristics:**
- ✅ Self-contained with all dependencies bundled
- ✅ Can run independently without other apps
- ✅ Larger app size (50-150MB)
- ✅ Faster startup (no IPC overhead)
- ✅ Works offline without kernel
- ❌ Duplicates common dependencies

**Example: AVA AI Assistant**
```kotlin
// AVA AI is standalone - bundles everything
class AVAAIApp : StandalonePlugin {
    override val manifest = PluginManifest(
        id = "com.augmentalis.avanue.ava",
        name = "AVA AI Assistant",
        version = "1.0.0",
        type = PluginType.STANDALONE,

        // Bundles all dependencies
        bundledDependencies = listOf(
            "avaelements-ui:2.0.0",
            "tensorflow-lite:2.14.0",
            "whisper-cpp:1.5.0",
            "sqlite:3.42.0"
        ),

        // Also provides capabilities to others
        providedCapabilities = listOf(
            Capability("ai.text-generation", "1.0"),
            Capability("ai.voice-recognition", "1.0"),
            Capability("ai.translation", "1.0")
        )
    )
}
```

### 2.2 IPC-Dependent Apps

**Characteristics:**
- ✅ Lightweight (2-10MB)
- ✅ Uses shared dependencies via IPC
- ✅ No dependency duplication
- ✅ Faster updates (only app code)
- ❌ Requires kernel runtime
- ❌ Slight IPC overhead (~5-10ms)
- ❌ Needs network/IPC for external capabilities

**Example: TaskMaker**
```kotlin
// TaskMaker is IPC-dependent - uses shared dependencies
class TaskMakerApp : IPCPlugin {
    override val manifest = PluginManifest(
        id = "com.augmentalis.avanue.taskmaker",
        name = "TaskMaker",
        version = "1.0.0",
        type = PluginType.IPC_DEPENDENT,

        // Declares required dependencies (doesn't bundle them)
        requiredCapabilities = listOf(
            CapabilityRequest(
                capability = "ui.avaelements",
                version = "2.0.0+",
                source = CapabilitySource.KERNEL
            ),
            CapabilityRequest(
                capability = "database.magicdata",
                version = "1.5.0+",
                source = CapabilitySource.KERNEL
            ),
            CapabilityRequest(
                capability = "ai.text-generation",
                version = "1.0+",
                source = CapabilitySource.ANY_APP // Can use AVA or other AI app
            )
        ),

        // Lightweight - only app code
        bundledDependencies = listOf(
            "taskmaker-core:1.0.0" // Only its own logic
        )
    )
}
```

---

## 3. Dependency Registry Database Schema

### 3.1 MagicData Schema

```sql
-- Table: app_registry
-- Stores basic app information
CREATE TABLE app_registry (
    app_id TEXT PRIMARY KEY,                -- e.g., "com.augmentalis.avanue.ava"
    app_name TEXT NOT NULL,                 -- "AVA AI Assistant"
    version TEXT NOT NULL,                  -- "1.0.0"
    plugin_type TEXT NOT NULL,              -- "STANDALONE" | "IPC_DEPENDENT"
    install_date INTEGER NOT NULL,          -- Unix timestamp
    last_used INTEGER,                      -- Unix timestamp
    size_bytes INTEGER NOT NULL,            -- App size
    status TEXT NOT NULL                    -- "ACTIVE" | "INACTIVE" | "UPDATING"
);

-- Table: provided_capabilities
-- What each app provides to others
CREATE TABLE provided_capabilities (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    provider_app_id TEXT NOT NULL,          -- App providing the capability
    capability_id TEXT NOT NULL,            -- e.g., "ai.text-generation"
    capability_version TEXT NOT NULL,       -- "1.0.0"
    api_type TEXT NOT NULL,                 -- "AIDL" | "CONTENT_PROVIDER" | "BROADCAST"
    endpoint TEXT NOT NULL,                 -- IPC endpoint URI
    description TEXT,

    FOREIGN KEY (provider_app_id) REFERENCES app_registry(app_id),
    UNIQUE(provider_app_id, capability_id)
);

-- Table: required_capabilities
-- What each app needs from others
CREATE TABLE required_capabilities (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    consumer_app_id TEXT NOT NULL,          -- App needing the capability
    capability_id TEXT NOT NULL,            -- e.g., "database.magicdata"
    min_version TEXT NOT NULL,              -- "1.5.0"
    max_version TEXT,                       -- Optional version ceiling
    preferred_provider TEXT,                -- Preferred provider app_id (optional)
    fallback_bundled BOOLEAN DEFAULT 0,     -- Has bundled fallback?

    FOREIGN KEY (consumer_app_id) REFERENCES app_registry(app_id)
);

-- Table: dependency_manifest
-- Detailed info about each dependency/capability
CREATE TABLE dependency_manifest (
    capability_id TEXT PRIMARY KEY,         -- e.g., "ui.avaelements"
    category TEXT NOT NULL,                 -- "UI" | "DATABASE" | "AI" | "NETWORK"
    api_contract TEXT NOT NULL,             -- JSON/Protobuf schema
    latest_version TEXT NOT NULL,           -- "2.0.0"
    size_bytes INTEGER,                     -- Typical size if bundled
    description TEXT
);

-- Table: ipc_endpoints
-- Active IPC endpoints for capability requests
CREATE TABLE ipc_endpoints (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    provider_app_id TEXT NOT NULL,
    capability_id TEXT NOT NULL,
    endpoint_type TEXT NOT NULL,            -- "AIDL" | "CONTENT_PROVIDER" | "BROADCAST"
    endpoint_uri TEXT NOT NULL,             -- "content://com.avanue.ava/ai-api"
    is_active BOOLEAN DEFAULT 1,
    last_ping INTEGER,                      -- Health check timestamp

    FOREIGN KEY (provider_app_id) REFERENCES app_registry(app_id),
    UNIQUE(provider_app_id, capability_id, endpoint_type)
);

-- Table: capability_usage_log
-- Track which apps use which capabilities
CREATE TABLE capability_usage_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    consumer_app_id TEXT NOT NULL,
    provider_app_id TEXT NOT NULL,
    capability_id TEXT NOT NULL,
    request_count INTEGER DEFAULT 1,
    last_used INTEGER NOT NULL,             -- Unix timestamp
    avg_latency_ms INTEGER,

    FOREIGN KEY (consumer_app_id) REFERENCES app_registry(app_id),
    FOREIGN KEY (provider_app_id) REFERENCES app_registry(app_id)
);

-- Indexes for performance
CREATE INDEX idx_capabilities_provider ON provided_capabilities(provider_app_id);
CREATE INDEX idx_capabilities_id ON provided_capabilities(capability_id);
CREATE INDEX idx_required_consumer ON required_capabilities(consumer_app_id);
CREATE INDEX idx_required_capability ON required_capabilities(capability_id);
CREATE INDEX idx_endpoints_active ON ipc_endpoints(is_active, capability_id);
```

---

## 4. Capability Discovery & Resolution

### 4.1 Discovery Flow

```kotlin
// Step 1: App requests capability
class TaskMakerApp {
    fun onCreate() {
        val aiCapability = AvanuésKernel.requestCapability(
            CapabilityRequest(
                capability = "ai.text-generation",
                minVersion = "1.0",
                timeout = 5000.milliseconds
            )
        )

        when (aiCapability) {
            is CapabilityResolved -> {
                // Use the capability
                val aiService = aiCapability.service as AITextGeneration
                aiService.generateText("Create a todo list")
            }
            is CapabilityNotAvailable -> {
                // Prompt user to install AVA AI
                showInstallPrompt("AVA AI", "ai.text-generation")
            }
        }
    }
}
```

### 4.2 Kernel Resolution Logic

```kotlin
class AvanuésKernel {
    private val registry = DependencyRegistry()

    suspend fun requestCapability(request: CapabilityRequest): CapabilityResult {
        // Step 1: Query database for providers
        val providers = registry.findProviders(
            capabilityId = request.capability,
            minVersion = request.minVersion
        )

        if (providers.isEmpty()) {
            return CapabilityNotAvailable(
                reason = "No app provides ${request.capability}",
                suggestedApps = registry.suggestAppsProviding(request.capability)
            )
        }

        // Step 2: Select best provider (prefer already-running apps)
        val provider = selectProvider(providers, request.preferred)

        // Step 3: Establish IPC connection
        val connection = when (provider.apiType) {
            "AIDL" -> connectViaAIDL(provider.endpoint)
            "CONTENT_PROVIDER" -> connectViaContentProvider(provider.endpoint)
            "BROADCAST" -> connectViaBroadcast(provider.endpoint)
            else -> throw UnsupportedOperationException("Unknown API type")
        }

        // Step 4: Verify capability contract
        if (!connection.supportsVersion(request.minVersion)) {
            return CapabilityVersionMismatch(
                required = request.minVersion,
                available = connection.version
            )
        }

        // Step 5: Return capability proxy
        return CapabilityResolved(
            provider = provider,
            service = createProxy(connection, request.capability)
        )
    }

    private fun selectProvider(
        providers: List<CapabilityProvider>,
        preferred: String?
    ): CapabilityProvider {
        // Priority:
        // 1. Preferred provider (if specified and running)
        // 2. Already-running provider (no startup cost)
        // 3. Highest-rated provider
        // 4. Most recently used provider

        return providers
            .filter { it.isRunning() || preferred == it.appId }
            .maxByOrNull { provider ->
                var score = 0
                if (provider.appId == preferred) score += 1000
                if (provider.isRunning()) score += 100
                score += provider.rating * 10
                score
            } ?: providers.first()
    }
}
```

---

## 5. IPC Protocols for Capability Sharing

### 5.1 AIDL-based Capability (Android)

```kotlin
// Define capability interface (AIDL)
interface IAITextGeneration {
    fun generateText(prompt: String): String
    fun getVersion(): String
}

// Provider (AVA AI) implements the interface
class AVAAIService : Service() {
    private val binder = object : IAITextGeneration.Stub() {
        override fun generateText(prompt: String): String {
            // AVA's actual AI implementation
            return llmModel.generate(prompt)
        }

        override fun getVersion(): String = "1.0.0"
    }

    override fun onBind(intent: Intent): IBinder {
        // Register in kernel on first bind
        AvanuésKernel.registerCapability(
            appId = "com.augmentalis.avanue.ava",
            capability = "ai.text-generation",
            endpoint = "aidl://com.augmentalis.avanue.ava.AIService",
            binder = binder
        )
        return binder
    }
}

// Consumer (TaskMaker) uses the capability
class TaskMakerApp {
    suspend fun useAI() {
        val ai = AvanuésKernel.requestCapability(
            CapabilityRequest("ai.text-generation", "1.0+")
        ) as? IAITextGeneration

        ai?.let {
            val result = it.generateText("Create a task list")
            displayResult(result)
        }
    }
}
```

### 5.2 ContentProvider-based Capability

```kotlin
// Provider (MagicData) exposes database via ContentProvider
class MagicDataProvider : ContentProvider() {
    companion object {
        const val AUTHORITY = "com.augmentalis.avanue.magicdata"
        val CONTENT_URI = Uri.parse("content://$AUTHORITY/data")
    }

    override fun onCreate(): Boolean {
        // Register capability
        AvanuésKernel.registerCapability(
            appId = "com.augmentalis.avanue.kernel",
            capability = "database.magicdata",
            endpoint = "content://$AUTHORITY",
            provider = this
        )
        return true
    }

    override fun query(...): Cursor? {
        // Provide database access
    }
}

// Consumer uses ContentProvider
class TaskMakerApp {
    fun saveTask(task: Task) {
        val uri = Uri.parse("content://com.augmentalis.avanue.magicdata/tasks")
        contentResolver.insert(uri, task.toContentValues())
    }
}
```

---

## 6. Dependency Management Examples

### 6.1 App Installation with Dependency Check

```kotlin
class AvanuésPackageManager {

    suspend fun installApp(apkFile: File): InstallResult {
        // Step 1: Extract manifest
        val manifest = extractManifest(apkFile)

        // Step 2: Check dependencies
        val missingDeps = checkDependencies(manifest)

        if (missingDeps.isNotEmpty()) {
            return InstallResult.MissingDependencies(
                app = manifest.name,
                missing = missingDeps,
                suggestions = suggestProvidingApps(missingDeps)
            )
        }

        // Step 3: Install
        installPackage(apkFile)

        // Step 4: Register in database
        registry.registerApp(manifest)

        // Step 5: Setup IPC endpoints if providing capabilities
        if (manifest.providedCapabilities.isNotEmpty()) {
            setupIPCEndpoints(manifest)
        }

        return InstallResult.Success(manifest.appId)
    }

    private fun checkDependencies(
        manifest: PluginManifest
    ): List<MissingDependency> {
        val missing = mutableListOf<MissingDependency>()

        for (required in manifest.requiredCapabilities) {
            val providers = registry.findProviders(
                capabilityId = required.capability,
                minVersion = required.minVersion
            )

            if (providers.isEmpty()) {
                missing.add(MissingDependency(
                    capability = required.capability,
                    minVersion = required.minVersion,
                    canBundleFallback = required.fallbackBundled
                ))
            }
        }

        return missing
    }
}
```

### 6.2 Runtime Dependency Resolution

```kotlin
// TaskMaker needs AI capability at runtime
class TaskMakerApp {

    private var aiService: IAITextGeneration? = null

    suspend fun initializeDependencies() {
        // Resolve AI capability
        val aiResult = AvanuésKernel.requestCapability(
            CapabilityRequest(
                capability = "ai.text-generation",
                minVersion = "1.0",
                preferredProvider = "com.augmentalis.avanue.ava"
            )
        )

        when (aiResult) {
            is CapabilityResolved -> {
                aiService = aiResult.service as IAITextGeneration
                log("✅ AI capability resolved: ${aiResult.provider.appName}")
            }

            is CapabilityNotAvailable -> {
                // Show user a prompt
                showDialog(
                    title = "AI Feature Unavailable",
                    message = "TaskMaker needs AVA AI for smart suggestions. Install now?",
                    positiveButton = "Install AVA AI" to {
                        openPlayStore("com.augmentalis.avanue.ava")
                    },
                    negativeButton = "Use Basic Mode" to {
                        useBasicMode()
                    }
                )
            }

            is CapabilityVersionMismatch -> {
                // Provider exists but wrong version
                showDialog(
                    title = "Update Required",
                    message = "AVA AI needs to be updated to version ${aiResult.required}",
                    positiveButton = "Update" to {
                        updateApp(aiResult.provider.appId)
                    }
                )
            }
        }
    }
}
```

---

## 7. Benefits & Trade-offs

### 7.1 Benefits

**For Users:**
- 📦 **60-80% smaller app sizes** (IPC-dependent apps)
- 🚀 **Faster downloads & updates** (only app code, not deps)
- 💾 **Less storage usage** (shared dependencies)
- 🔄 **Automatic dependency updates** (kernel updates shared libs)

**For Developers:**
- 🛠️ **Easier dependency management** (declare needs, kernel resolves)
- 🔌 **Plugin interoperability** (apps can use each other's features)
- 📊 **Usage analytics** (see which capabilities are most used)
- 🧪 **Easier testing** (mock capability providers)

**For Ecosystem:**
- 🌐 **App interconnectivity** (apps become platforms)
- 🔐 **Centralized security** (kernel controls capability access)
- 📈 **Scalability** (new capabilities discoverable)

### 7.2 Trade-offs

| Aspect | Standalone | IPC-Dependent |
|--------|-----------|---------------|
| **App Size** | 50-150MB | 2-10MB |
| **Startup Time** | Fast (0 overhead) | Slightly slower (+50-100ms) |
| **Offline Support** | ✅ Full | ❌ Needs kernel |
| **Dependency Duplication** | ❌ Yes | ✅ No |
| **IPC Overhead** | 0ms | 5-10ms per call |
| **Update Frequency** | Lower (large downloads) | Higher (small updates) |
| **Complexity** | Low | Medium |

---

## 8. Implementation Roadmap

### Phase 1: Database & Registry (Week 1)
- [ ] Create MagicData schema for dependency registry
- [ ] Implement `DependencyRegistry` class
- [ ] Implement `AvanuésKernel` core services
- [ ] Write unit tests for registry operations

### Phase 2: Capability Resolution (Week 2)
- [ ] Implement capability discovery algorithm
- [ ] Implement provider selection logic
- [ ] Create IPC proxy generation system
- [ ] Handle capability versioning

### Phase 3: IPC Protocols (Week 3)
- [ ] Implement AIDL-based capability sharing
- [ ] Implement ContentProvider-based sharing
- [ ] Implement Broadcast-based sharing
- [ ] Add health checks and failover

### Phase 4: Plugin Manifest (Week 4)
- [ ] Define `PluginManifest` schema
- [ ] Implement manifest parser
- [ ] Add manifest validation
- [ ] Create manifest generator tool

### Phase 5: Package Manager Integration (Week 5)
- [ ] Implement dependency checking on install
- [ ] Add "missing dependency" UI flows
- [ ] Implement automatic dependency resolution
- [ ] Add capability usage analytics

### Phase 6: Developer Tools (Week 6)
- [ ] Create CLI for querying registry
- [ ] Build UI for viewing dependency graph
- [ ] Add debug tools for IPC inspection
- [ ] Write developer documentation

---

## 9. Security Considerations

### 9.1 Capability Permissions

```kotlin
// Apps must declare permissions to access capabilities
<manifest>
    <uses-permission android:name="com.augmentalis.avanue.CAPABILITY.AI_TEXT_GENERATION" />
    <uses-permission android:name="com.augmentalis.avanue.CAPABILITY.DATABASE_ACCESS" />
</manifest>

// Kernel enforces permissions
class AvanuésKernel {
    fun requestCapability(request: CapabilityRequest): CapabilityResult {
        // Check if requesting app has permission
        if (!hasPermission(callingApp, request.capability)) {
            return CapabilityDenied(
                reason = "App lacks permission for ${request.capability}"
            )
        }

        // ... rest of resolution
    }
}
```

### 9.2 Rate Limiting

```kotlin
// Prevent capability abuse
class CapabilityRateLimiter {
    private val limits = mapOf(
        "ai.text-generation" to RateLimit(100.calls per 1.hours),
        "database.magicdata" to RateLimit(1000.calls per 1.minutes)
    )

    fun checkLimit(appId: String, capability: String): Boolean {
        val limit = limits[capability] ?: return true
        val usage = getUsage(appId, capability)
        return usage.count < limit.max
    }
}
```

---

## 10. Example: Complete Flow

### Scenario: User Installs TaskMaker

**Step 1: Installation**
```
User downloads TaskMaker (3MB) from Play Store
↓
Package Manager extracts manifest:
  - Requires: "ui.avaelements" (v2.0+)
  - Requires: "database.magicdata" (v1.5+)
  - Requires: "ai.text-generation" (v1.0+)
↓
Kernel checks registry:
  ✅ "ui.avaelements" v2.0.1 (provided by Kernel)
  ✅ "database.magicdata" v1.5.0 (provided by Kernel)
  ❌ "ai.text-generation" NOT FOUND
↓
Show user prompt:
  "TaskMaker needs AVA AI for smart features. Install AVA AI (80MB)?"
  [Install AVA AI] [Skip - Use Basic Mode]
```

**Step 2: Runtime**
```
User launches TaskMaker
↓
TaskMaker requests capabilities:
  requestCapability("ui.avaelements")
    → Resolved: Kernel UI service
  requestCapability("database.magicdata")
    → Resolved: Kernel DB service
  requestCapability("ai.text-generation")
    → Not Available (AVA not installed)
    → TaskMaker uses basic mode (no AI suggestions)
↓
User clicks "Get AI Features"
  → Opens Play Store to install AVA AI
  → After AVA install, capability becomes available
  → TaskMaker detects new capability and enables AI features
```

**Step 3: Capability Usage**
```
TaskMaker calls AI:
  aiService.generateText("Create a shopping list")
↓
Kernel routes request:
  1. Checks permission ✅
  2. Checks rate limit ✅
  3. Finds provider: AVA AI
  4. Establishes AIDL connection
  5. Forwards request to AVA
  6. Returns result to TaskMaker (latency: ~8ms)
↓
Kernel logs usage:
  INSERT INTO capability_usage_log
    (consumer_app_id, provider_app_id, capability_id, ...)
  VALUES ('taskmaker', 'ava', 'ai.text-generation', ...)
```

---

## 11. Next Steps

1. **Fix YamlParser.kt** - Unblock Core framework compilation
2. **Implement Registry Database** - Create MagicData schema
3. **Build Kernel Services** - Capability resolution & IPC routing
4. **Define Manifest Format** - Plugin declaration standard
5. **Create Example Apps** - One standalone, one IPC-dependent
6. **Write Tests** - End-to-end capability resolution

---

**Author:** Manoj Jhawar, manoj@ideahq.net
**Created:** 2025-11-10 05:15:20 PST
**Framework:** Avanues Master Platform
**Version:** 1.0.0
