# AVANUES-PLATFORM-INDEX
# AI-Readable Platform Documentation Index
# Version: 1.1 | Updated: 2026-01-13
# Changes: Added AVID module, deprecated Common/VUID

---

## PLATFORM_METADATA
```yaml
name: Avanues Platform
type: Voice-First Accessibility & AI Assistant Ecosystem
architecture: Kotlin Multiplatform (KMP)
status: Production
platforms: [Android, iOS-planned, Desktop-planned]
min_android_sdk: 28
target_android_sdk: 34
jdk_required: 17
```

---

## MODULE_REGISTRY

### VOICEOSCORENG
```yaml
id: voiceoscoreng
path: /Modules/VoiceOSCoreNG
type: kmp-library
status: production
version: 1.0.0
loc: 8000+
purpose: Voice command processing engine
key_classes:
  - VoiceOSCoreNG: Main facade, builder pattern
  - ActionCoordinator: Command routing, 5-level priority
  - HandlerRegistry: Handler lifecycle management
  - CommandRegistry: Dynamic screen-specific commands
  - StaticCommandRegistry: Predefined system commands
dependencies:
  - Modules/AVID
  - Common/VoiceOS/*
  - Common/Database
capabilities:
  - voice_command_processing
  - handler_system_11_categories
  - speech_engine_abstraction_6_engines
  - nlu_integration
  - llm_integration
  - dynamic_command_registry
command_flow:
  - level_1: Dynamic VUID commands (screen-specific)
  - level_2: Fuzzy matching
  - level_3: Static handlers (system)
  - level_4: NLU classification (BERT)
  - level_5: LLM interpretation (fallback)
handler_categories:
  - SYSTEM: priority=0, actions=[back,home,recents,notifications]
  - NAVIGATION: priority=1, actions=[scroll,swipe]
  - APP: priority=2, actions=[open,launch,start]
  - GAZE: priority=3
  - GESTURE: priority=4
  - UI: priority=5, actions=[click,tap,long_press,focus]
  - DEVICE: priority=6
  - INPUT: priority=7, actions=[type,input_text]
  - MEDIA: priority=8
  - ACCESSIBILITY: priority=9
  - CUSTOM: priority=10
```

### VOICEOS_APP
```yaml
id: voiceos-app
path: /android/apps/voiceoscoreng
type: android-app
status: production
version: 1.0.0
loc: 3600+
purpose: Android accessibility service for voice control
key_classes:
  - VoiceOSAccessibilityService: 1789 lines, main service
  - OverlayService: 666 lines, numbers overlay
  - ScreenExplorer: UI element discovery
  - NumbersOverlay: Visual element numbering
dependencies:
  - VoiceOSCoreNG
  - Common/Database
capabilities:
  - accessibility_service
  - numbers_overlay
  - screen_exploration
  - continuous_monitoring
  - boot_auto_start
```

### AVA
```yaml
id: ava
path: /Modules/AVA
type: kmp-library-and-app
status: production
version: 1.0.0
files: 288 kotlin files
purpose: AI voice assistant platform
key_classes:
  - ActionsManager: Intent execution, 90+ handlers
  - ChatViewModel: Conversation management
  - MemoryManager: Long-term memory with decay
  - HybridResponseGenerator: LLM response generation
dependencies:
  - LLM
  - NLU
  - RAG
  - Common/*
capabilities:
  - ai_voice_assistant
  - 90_intent_handlers
  - dual_nlu: [MobileBERT, mALBERT]
  - memory_system_with_decay
  - wake_word_detection
  - streaming_responses
action_categories:
  - SYSTEM
  - NAVIGATION
  - APP
  - UI
  - INPUT
  - MEDIA
  - ACCESSIBILITY
  - CUSTOM
```

### LLM
```yaml
id: llm
path: /Modules/LLM
type: kmp-library
status: production
version: 2.0
classes: 60+
purpose: Language model integration
key_classes:
  - HybridResponseGenerator: Primary response generation
  - LocalLLMProvider: On-device inference (MLC+TVM)
  - CloudLLMProvider: Cloud fallback (4 providers)
  - LLMConfig: Configuration management
dependencies:
  - Common/Core
capabilities:
  - on_device_inference: MLC LLM + TVM Runtime
  - cloud_fallback: [Claude, GPT-4, Gemini, OpenRouter]
  - multilingual: 140+ languages
  - streaming_responses
  - cost_tracking
  - token_management
response_types:
  - Streaming: chunk-by-chunk
  - Complete: full response with usage
  - Error: error handling
fallback_chain: Local -> Claude -> GPT-4 -> Gemini -> OpenRouter
```

### NLU
```yaml
id: nlu
path: /Modules/NLU
type: kmp-library
status: production
version: 1.0
classes: 50+
purpose: Natural language understanding
key_classes:
  - HybridIntentClassifier: Multi-method classification
  - ClassifyIntentUseCase: Primary classification API
  - BertTokenizer: BERT tokenization
  - IntentClassifier: Base classifier interface
dependencies:
  - Modules/AVID
capabilities:
  - bert_classification: MobileBERT, mALBERT
  - 52_languages: via mALBERT
  - hybrid_matching: [exact, fuzzy, semantic]
  - self_learning
  - latency: <100ms
classification_methods:
  - EXACT: threshold=0.95
  - FUZZY: min_similarity=0.7
  - SEMANTIC: min_similarity=0.6
  - HYBRID: combination
```

### RAG
```yaml
id: rag
path: /Modules/RAG
type: kmp-library
status: production
version: 1.0
classes: 40+
purpose: Retrieval-augmented generation
key_classes:
  - SQLiteRAGRepository: Primary repository
  - RAGChatEngine: Chat with context
  - KMeansClustering: 256 clusters
  - ChunkingStrategy: Document splitting
dependencies:
  - Common/Database
capabilities:
  - document_indexing: [PDF, DOCX, MD, HTML, EPUB, RTF]
  - semantic_search
  - k_means_clustering: 256 clusters
  - lru_query_cache
  - chat_integration
search_config:
  max_results: 10
  min_similarity: 0.5
chunking_strategies:
  - FIXED_SIZE
  - SEMANTIC
  - HYBRID
```

### WEBAVANUE
```yaml
id: webavanue
path: /Modules/WebAvanue
type: kmp-library-and-apps
status: development
version: 0.9
loc: 33000+
purpose: Voice-controlled web browser
key_classes:
  - DOMScraperBridge: 428 lines, JavaScript DOM extraction
  - VoiceCommandGenerator: 311 lines, fuzzy command matching
  - WebViewContainer: Platform WebView wrapper
  - TabManager: Tab management with LRU
dependencies:
  - VoiceOSCoreNG
  - AVID
  - Common/*
capabilities:
  - voice_navigation
  - dom_scraping_with_vuids
  - tab_management
  - reader_mode
  - encrypted_storage: AES-256-CBC
platform_support:
  android: production (WebView)
  ios: phase_2 (WKWebView)
  desktop: phase_2 (JCEF)
```

### AVID
```yaml
id: avid
path: /Modules/AVID
type: kmp-library
status: production
version: 1.0.0
loc: 800+
purpose: Unified identifier system (Avanues Voice ID)
key_classes:
  - AvidGenerator: Cloud/local ID generation with platform encoding
  - Platform: Platform enum (ANDROID, IOS, WEB, MACOS, WINDOWS, LINUX, UNKNOWN)
  - TypeCode: 40+ semantic type codes for UI elements
  - ElementFingerprint: Deterministic hash-based UI element identification
dependencies:
  - kotlinx-coroutines-core
platforms:
  - Android
  - iOS (arm64, x64, simulatorArm64)
  - JVM (Desktop)
  - JS (Web, conditional)
capabilities:
  - cloud_id_generation: AVID-{platform}-{sequence}
  - local_id_generation: AVIDL-{platform}-{sequence}
  - id_promotion: AVIDL -> AVID on cloud sync
  - deterministic_fingerprints: TypeCode:hash8
  - cross_device_reproducibility
  - collision_resistant_hashing
id_formats:
  cloud: "AVID-{A|I|W|M|X|L}-{sequence}"
  local: "AVIDL-{A|I|W|M|X|L}-{sequence}"
  fingerprint: "{TypeCode}:{hash8}"
migration_status:
  replaces: Common/VUID
  consumers_migrated: [VoiceOSCoreNG, WebAvanue]
```

---

## COMMON_LIBRARIES

```yaml
libraries:
  - id: vuid
    path: /Common/VUID
    status: DEPRECATED
    deprecated_by: Modules/AVID
    purpose: Unique ID generation (DEPRECATED - use AVID instead)
    key_functions:
      - generateCompact(packageName, version, typeName)
      - generateMessageVuid()
      - generateConversationVuid()
      - isValid(vuid)
      - parse(vuid)
    migration_note: "Use AvidGenerator for entity IDs, ElementFingerprint for UI elements"

  - id: voiceos-result
    path: /Common/VoiceOS/result
    purpose: Type-safe error handling
    types: [Success, Failure]
    methods: [map, flatMap, mapError]

  - id: voiceos-hash
    path: /Common/VoiceOS/hash
    purpose: SHA-256 hashing

  - id: voiceos-constants
    path: /Common/VoiceOS/constants
    purpose: Configuration constants

  - id: voiceos-validation
    path: /Common/VoiceOS/validation
    purpose: SQL escaping, input validation

  - id: voiceos-exceptions
    path: /Common/VoiceOS/exceptions
    purpose: Exception hierarchy

  - id: voiceos-database
    path: /Common/VoiceOS/database
    purpose: SQLDelight persistence

  - id: core-assetmanager
    path: /Common/Core/AssetManager
    purpose: Icon/image management

  - id: core-thememanager
    path: /Common/Core/ThemeManager
    purpose: Universal themes

  - id: avaelements
    path: /Common/AvaElements
    purpose: UI component system

  - id: database
    path: /Common/Database
    purpose: KMP database wrapper

  - id: ui
    path: /Common/UI
    platform: android
    purpose: Compose components

  - id: utils
    path: /Common/Utils
    platform: android
    purpose: Utility helpers
```

---

## DEPENDENCY_GRAPH

```
VoiceOS_App -> VoiceOSCoreNG -> [Modules/AVID, Common/VoiceOS/*, Common/Database]
AVA -> [LLM -> Common/Core, NLU -> Modules/AVID, RAG -> Common/Database, Common/*]
WebAvanue -> [VoiceOSCoreNG, Modules/AVID, Common/*]
AVID -> [kotlinx-coroutines-core]
All_Modules -> Common_Libraries
```

**Note:** Common/VUID is deprecated. All consumers should migrate to Modules/AVID.

---

## API_QUICK_REFERENCE

```yaml
voiceoscoreng:
  process_command: "core.processCommand(text: String, confidence: Float = 1.0f): HandlerResult"
  start_listening: "core.startListening(): Result<Unit>"
  stop_listening: "core.stopListening()"
  update_commands: "core.updateDynamicCommands(commands: List<QuantizedCommand>)"
  state: "core.state: StateFlow<ServiceState>"

nlu:
  classify: "useCase.execute(utterance: String, language: String = 'en-US'): Result<IntentClassification>"
  classify_fast: "classifier.classifyFast(input: String): IntentMatch?"

llm:
  generate: "generator.generateResponse(msg, classification, context): Flow<ResponseChunk>"
  chat: "provider.chat(messages, options): Flow<LLMResponse>"

rag:
  search: "repo.search(query: SearchQuery): Result<SearchResponse>"
  add_document: "repo.addDocument(request: AddDocumentRequest): Result<AddDocumentResult>"
  ask: "engine.ask(question: String, history: List<Message>): Flow<ChatResponse>"

ava:
  execute_action: "manager.executeAction(intent: String, utterance: String): ActionResult"
  has_handler: "manager.hasHandler(intent: String): Boolean"

webavanue:
  scrape_dom: "bridge.scrapeDOM(): List<DOMElement>"
  match_command: "generator.matchCommand(utterance, elements): MatchResult"
  execute_click: "bridge.executeClick(vuid: String)"
```

---

## HANDLER_RESULTS

```yaml
success:
  type: HandlerResult.Success
  fields: [message: String, data: Any?]

failure:
  type: HandlerResult.Failure
  fields: [reason: String, recoverable: Boolean = true]

not_handled:
  type: HandlerResult.NotHandled

requires_input:
  type: HandlerResult.RequiresInput
  fields: [prompt: String, inputType: InputType]

awaiting_selection:
  type: HandlerResult.AwaitingSelection
  fields: [message: String, matchCount: Int]
```

---

## SERVICE_STATES

```yaml
states:
  - Uninitialized
  - Initializing
  - Ready
  - Listening
  - Processing
  - Error
  - Paused
  - Stopped
```

---

## BUILD_CONFIG

```yaml
jdk: 17
gradle: 8.x
android_sdk: 34
kotlin: 1.9.20+
min_android: API 28
database: SQLDelight
di: Hilt
```

---

## DOCUMENTATION_PATHS

```yaml
human_readable:
  - /Docs/MasterDocs/VoiceOSCoreNG/README.md
  - /Docs/MasterDocs/VoiceOS/README.md
  - /Docs/MasterDocs/AVA/README.md
  - /Docs/MasterDocs/LLM/README.md
  - /Docs/MasterDocs/NLU/README.md
  - /Docs/MasterDocs/RAG/README.md
  - /Docs/MasterDocs/WebAvanue/README.md
  - /Docs/MasterDocs/AVID/README.md
  - /Docs/MasterDocs/Common/README.md
  - /Docs/MasterDocs/LD/LD-Platform-Overview-V1.md
  - /Docs/MasterDocs/LD/LD-Module-Registry-V1.md
  - /Docs/MasterDocs/LD/LD-API-Reference-V1.md

ai_readable:
  - /Docs/MasterDocs/AI/PLATFORM-INDEX.ai.md
  - /Docs/MasterDocs/AI/CLASS-INDEX.ai.md
  - /Docs/MasterDocs/AI/API-REFERENCE.ai.md
  - /Docs/MasterDocs/AI/DEPENDENCY-MAP.ai.md

html_diagrams:
  - /Docs/MasterDocs/VoiceOSCoreNG/html/index.html
  - /Docs/MasterDocs/VoiceOSCoreNG/html/architecture.html
  - /Docs/MasterDocs/VoiceOSCoreNG/html/flowcharts.html
  - /Docs/MasterDocs/VoiceOSCoreNG/html/sequence-diagrams.html
  - /Docs/MasterDocs/VoiceOSCoreNG/html/class-diagrams.html
```

---

# END PLATFORM-INDEX
