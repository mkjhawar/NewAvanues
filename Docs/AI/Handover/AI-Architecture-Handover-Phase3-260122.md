# AI Architecture Rework - Phase 3 Handover Document

**Date:** 2026-01-22
**Branch:** `AI-Architecture-Rework`
**Session:** Phase 2 Complete → Phase 3 Ready
**Status:** Phase 2 Complete, Phase 3 Ready for Implementation
**Previous Phase:** Phase 2 Complete (this session)

---

## Phase 2 Summary (Completed)

Phase 2 established module-specific plugin contracts, the Plugin SDK, and the first migrated handler.

### Implementation Statistics

| Task | Files | Lines/Bytes | Status |
|------|-------|-------------|--------|
| 2.1 VoiceOSCore Contracts | 5 | ~2,350 lines | ✅ Complete |
| 2.2 AI Contracts | 5 | ~69,000 bytes | ✅ Complete |
| 2.3 Speech Contracts | 4 | ~57,000 bytes | ✅ Complete |
| 2.4 AccessibilityDataProvider | 3 | ~46,000 bytes | ✅ Complete |
| 2.5 Plugin SDK | 4 | ~59,000 bytes | ✅ Complete |
| 2.6 NavigationHandler Migration | 1 | ~400 lines | ✅ Complete |
| **Total** | **22** | **~230,000 bytes** | ✅ **Complete** |

### Files Created

```
Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/
├── universal/
│   ├── contracts/
│   │   ├── voiceoscore/
│   │   │   ├── HandlerPlugin.kt              ✅ Handler interface with patterns
│   │   │   ├── SynonymProviderPlugin.kt      ✅ Vocabulary/synonym expansion
│   │   │   ├── OverlayPlugin.kt              ✅ Accessibility overlays
│   │   │   ├── ThemeProviderPlugin.kt        ✅ Visual themes
│   │   │   └── CommandPersistencePlugin.kt   ✅ Command storage
│   │   ├── ai/
│   │   │   ├── AIModelInfo.kt                ✅ Model metadata
│   │   │   ├── LLMPlugin.kt                  ✅ Text generation
│   │   │   ├── NLUPlugin.kt                  ✅ Intent/entity extraction
│   │   │   ├── EmbeddingPlugin.kt            ✅ Vector embeddings
│   │   │   └── RAGPlugin.kt                  ✅ Retrieval-augmented generation
│   │   └── speech/
│   │       ├── SpeechTypes.kt                ✅ Shared types
│   │       ├── SpeechEnginePlugin.kt         ✅ Speech-to-text
│   │       ├── TTSPlugin.kt                  ✅ Text-to-speech
│   │       └── WakeWordPlugin.kt             ✅ Wake word detection
│   └── data/
│       ├── AccessibilityDataProvider.kt      ✅ Interface
│       ├── AccessibilityDataProviderImpl.kt  ✅ Implementation
│       └── CachedAccessibilityData.kt        ✅ Caching layer
├── sdk/
│   ├── BasePlugin.kt                         ✅ Abstract base class
│   ├── PluginContextBuilder.kt               ✅ Context builder
│   ├── PluginConfigLoader.kt                 ✅ Config loading
│   └── PluginTestHarness.kt                  ✅ Testing utilities
└── builtin/
    └── NavigationHandlerPlugin.kt            ✅ First migrated handler
```

### Key Features Implemented

1. **Contract Segregation**: Separate interfaces per module (voiceoscore/, ai/, speech/)
2. **Extension Pattern**: All contracts extend `UniversalPlugin`
3. **BasePlugin SDK**: Reduces boilerplate with automatic lifecycle management
4. **AccessibilityDataProvider**: Read-only access to VoiceOSCore data with caching
5. **Reactive Flows**: StateFlow for real-time UI updates
6. **Thread Safety**: Mutex-based synchronization in data providers
7. **Testing Support**: PluginTestHarness with mock event bus
8. **NavigationHandler Migration**: Working prototype demonstrating migration pattern

---

## Phase 3 Overview

**Goal:** Platform-specific implementations and additional handler migrations.

**Timeline:** Weeks 5-6 of the overall plan

### Deliverables

1. Android plugin host implementation
2. Platform-specific AccessibilityDataProvider bindings
3. Migrate remaining VoiceOSCore handlers to plugins
4. Integration tests for plugin lifecycle
5. Plugin discovery and hot-reload support

---

## Phase 3 Tasks

### Task 3.1: Android Plugin Host

Create the Android-specific plugin host that loads and manages plugins.

**Files to Create:**
```
Modules/PluginSystem/android/src/main/kotlin/com/augmentalis/magiccode/plugins/android/
├── AndroidPluginHost.kt                    # Main plugin host
├── AndroidPluginContext.kt                 # Android-specific context
├── AndroidAccessibilityDataProvider.kt     # Wraps Android repositories
└── PluginServiceConnection.kt              # Service binding for plugins
```

**AndroidPluginHost.kt Interface:**
```kotlin
class AndroidPluginHost(
    private val context: Context,
    private val serviceRegistry: ServiceRegistry,
    private val eventBus: PluginEventBus
) {
    private val registry = UniversalPluginRegistry()
    private val lifecycleManager = PluginLifecycleManager(registry, eventBus)

    suspend fun loadPlugin(pluginId: String): Result<UniversalPlugin>
    suspend fun unloadPlugin(pluginId: String): Result<Unit>
    fun getPlugin(pluginId: String): UniversalPlugin?
    fun getPluginsByCapability(capabilityId: String): List<UniversalPlugin>

    // Lifecycle integration
    fun onActivityCreated(activity: Activity)
    fun onActivityDestroyed(activity: Activity)
    fun onServiceConnected(service: AccessibilityService)
    fun onServiceDisconnected()
}
```

---

### Task 3.2: Handler Migrations

Migrate remaining VoiceOSCore handlers to the plugin architecture.

**Handlers to Migrate (Priority Order):**

| Handler | Plugin Name | Complexity |
|---------|-------------|------------|
| UIHandler | UIInteractionPlugin | Medium |
| InputHandler | TextInputPlugin | Medium |
| SystemHandler | SystemCommandPlugin | Low |
| GestureHandler | GesturePlugin | Medium |
| SelectHandler | SelectionPlugin | Low |
| AppHandler | AppLauncherPlugin | Low |

**Migration Pattern:**
```kotlin
class UIInteractionPlugin(
    private val executorProvider: () -> UIInteractionExecutor
) : BasePlugin(), HandlerPlugin {

    override val handlerType = HandlerType.UI_INTERACTION

    override val patterns = listOf(
        CommandPattern(
            regex = Regex("^(click|tap|press)\\s+(.+)$", RegexOption.IGNORE_CASE),
            intent = "CLICK",
            requiredEntities = setOf("target"),
            examples = listOf("click submit", "tap next", "press button")
        ),
        // ... more patterns
    )

    // Wrap existing UIHandler logic
}
```

---

### Task 3.3: Platform Data Bindings

Connect AccessibilityDataProvider to actual Android repositories.

**Files to Create:**
```
Modules/PluginSystem/android/src/main/kotlin/com/augmentalis/magiccode/plugins/android/data/
├── AndroidAccessibilityDataProvider.kt     # Main implementation
├── RepositoryAdapter.kt                    # Adapts existing repos
└── LiveDataFlowBridge.kt                   # Converts LiveData to Flow
```

**Implementation Approach:**
```kotlin
class AndroidAccessibilityDataProvider(
    private val elementRepository: IScrapedAppRepository,
    private val commandRepository: ILearnedCommandRepository,
    private val preferenceRepository: IContextPreferenceRepository,
    private val coroutineScope: CoroutineScope
) : AccessibilityDataProvider {

    private val _screenElementsFlow = MutableStateFlow<List<QuantizedElement>>(emptyList())
    override val screenElementsFlow: StateFlow<List<QuantizedElement>> = _screenElementsFlow

    // Subscribe to repository changes
    init {
        coroutineScope.launch {
            elementRepository.observeCurrentScreen().collect { elements ->
                _screenElementsFlow.value = elements.map { it.toQuantizedElement() }
            }
        }
    }

    // Implement interface methods using repositories
}
```

---

### Task 3.4: Plugin Discovery

Implement plugin discovery for built-in and third-party plugins.

**Files to Create:**
```
Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/discovery/
├── PluginDiscovery.kt                      # Discovery interface
├── BuiltinPluginDiscovery.kt               # Built-in plugins
├── FileSystemPluginDiscovery.kt            # File-based plugins
└── PluginManifestReader.kt                 # AVU manifest parsing
```

**Discovery Interface:**
```kotlin
interface PluginDiscovery {
    suspend fun discoverPlugins(): List<PluginDescriptor>
    suspend fun loadPlugin(descriptor: PluginDescriptor): Result<UniversalPlugin>
}

data class PluginDescriptor(
    val pluginId: String,
    val name: String,
    val version: String,
    val capabilities: Set<String>,
    val source: PluginSource
)

sealed class PluginSource {
    object Builtin : PluginSource()
    data class FileSystem(val path: String) : PluginSource()
    data class Remote(val url: String) : PluginSource()
}
```

---

### Task 3.5: Integration Tests

Create comprehensive integration tests for the plugin system.

**Test Files to Create:**
```
Modules/PluginSystem/src/commonTest/kotlin/com/augmentalis/magiccode/plugins/
├── integration/
│   ├── PluginLifecycleIntegrationTest.kt   # Full lifecycle tests
│   ├── HandlerPluginIntegrationTest.kt     # Handler execution tests
│   ├── EventBusIntegrationTest.kt          # Event routing tests
│   └── DataProviderIntegrationTest.kt      # Data access tests
└── android/
    └── AndroidPluginHostTest.kt            # Android-specific tests
```

---

## Dependencies & Imports

Phase 3 will need imports from:

```kotlin
// Phase 1 & 2 types
import com.augmentalis.magiccode.plugins.universal.*
import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.*
import com.augmentalis.magiccode.plugins.universal.contracts.ai.*
import com.augmentalis.magiccode.plugins.universal.contracts.speech.*
import com.augmentalis.magiccode.plugins.universal.data.*
import com.augmentalis.magiccode.plugins.sdk.*
import com.augmentalis.magiccode.plugins.builtin.*

// VoiceOSCore types
import com.augmentalis.voiceoscore.*

// Android specifics
import android.content.Context
import android.accessibilityservice.AccessibilityService
import androidx.lifecycle.LiveData
```

---

## How to Continue

### Start Command
```bash
cd /Volumes/M-Drive/Coding/NewAvanues
git checkout AI-Architecture-Rework
```

### Prompt for New Session
```
Continue the Universal Plugin Architecture implementation. Read the handover:
Docs/AI/Handover/AI-Architecture-Handover-Phase3-260122.md

Phase 3 tasks:
1. Task 3.1: Create Android Plugin Host
2. Task 3.2: Migrate remaining handlers (UIHandler, InputHandler, SystemHandler, etc.)
3. Task 3.3: Create platform-specific AccessibilityDataProvider bindings
4. Task 3.4: Implement plugin discovery system
5. Task 3.5: Create integration tests

Use .swarm mode for parallel implementation where appropriate.
```

---

## Swarm Recommendations

For efficient parallel execution:

| Agent | Tasks |
|-------|-------|
| Agent 1 | Task 3.1 - Android Plugin Host |
| Agent 2 | Task 3.2a - UIInteractionPlugin migration |
| Agent 3 | Task 3.2b - TextInputPlugin migration |
| Agent 4 | Task 3.3 - Platform data bindings |
| Agent 5 | Task 3.4 - Plugin discovery |

Task 3.5 (integration tests) should wait until other tasks complete.

---

## Estimated Scope

| Task | Files | LOC (est.) |
|------|-------|------------|
| 3.1 Android Host | 4 | ~600 |
| 3.2 Handler Migrations | 6 | ~1,200 |
| 3.3 Data Bindings | 3 | ~400 |
| 3.4 Plugin Discovery | 4 | ~500 |
| 3.5 Integration Tests | 5 | ~800 |
| **Total** | **22** | **~3,500** |

---

## Success Criteria

1. Android plugin host loads and manages plugins correctly
2. All migrated handlers pass existing tests
3. AccessibilityDataProvider provides real data on Android
4. Plugin discovery finds both built-in and file-based plugins
5. Integration tests achieve >90% coverage of plugin lifecycle
6. No regressions in existing VoiceOSCore functionality

---

## Architecture Decisions Made in Phase 2

### ADR-001: Separate ScreenContext Definitions
**Decision:** Keep `ScreenContext` in `HandlerPlugin.kt` for handlers, separate from the richer `ScreenContext` in data contracts.
**Rationale:** Handler context needs minimal data for fast dispatch; data provider context needs full metadata for plugins.

### ADR-002: Executor Provider Pattern
**Decision:** Use `executorProvider: () -> Executor` instead of direct executor injection in plugins.
**Rationale:** Allows lazy initialization when platform services aren't available at plugin creation time.

### ADR-003: Delegation for Caching
**Decision:** `CachedAccessibilityData` uses Kotlin delegation (`by delegate`) with selective overrides.
**Rationale:** Clean separation of caching logic from data access logic.

---

## Known Issues / Technical Debt

1. **Duplicate ScreenContext**: Two definitions exist - consolidate in Phase 3
2. **currentTimeMillis expect/actual**: Needs platform implementations
3. **Repository types as Any**: Should be typed interfaces when available
4. **No hot-reload yet**: Plugin unload/reload needs careful lifecycle management

---

## Git Status at Phase 2 End

```bash
# New files (Phase 2)
Modules/PluginSystem/src/commonMain/.../contracts/voiceoscore/  (5 files)
Modules/PluginSystem/src/commonMain/.../contracts/ai/          (5 files)
Modules/PluginSystem/src/commonMain/.../contracts/speech/      (4 files)
Modules/PluginSystem/src/commonMain/.../data/                  (3 files)
Modules/PluginSystem/src/commonMain/.../sdk/                   (4 files)
Modules/PluginSystem/src/commonMain/.../builtin/               (1 file)

# Recommended commit message
feat(plugin-system): Complete Phase 2 - Module contracts and SDK

- Add VoiceOSCore contracts (HandlerPlugin, SynonymProvider, Overlay, Theme, Persistence)
- Add AI contracts (LLMPlugin, NLUPlugin, EmbeddingPlugin, RAGPlugin)
- Add Speech contracts (SpeechEngine, TTS, WakeWord)
- Add AccessibilityDataProvider with caching layer
- Add Plugin SDK (BasePlugin, ContextBuilder, ConfigLoader, TestHarness)
- Migrate NavigationHandler as first plugin prototype

Phase 2 of Universal Plugin Architecture complete.
```

---

**End of Phase 3 Handover Document**
