# CLASS-INDEX
# AI-Readable Class Reference
# Version: 2.1 | Updated: 2026-01-13
# Note: Includes SOLID-refactored classes from commits cf7fe0ff, 9231c8d9, 5a0353a6
# Changes: Added AVID module classes, deprecated VUIDGenerator

---

## VOICEOSCORENG_CLASSES

### VoiceOSCoreNG
```yaml
package: com.augmentalis.voiceoscoreng
file: VoiceOSCoreNG.kt
type: class
pattern: facade, builder
purpose: Main entry point for voice command processing
initialization:
  android: VoiceOSCoreNG.createForAndroid(accessibilityService)
  android_with_persistence: VoiceOSCoreNG.createForAndroidWithPersistence(service, repo, locale)
methods:
  lifecycle:
    - initialize(): suspend -> Unit
    - dispose(): suspend -> Unit
  command:
    - processCommand(text: String, confidence: Float = 1.0f): suspend -> HandlerResult
  voice:
    - startListening(): suspend -> Result<Unit>
    - stopListening(): suspend -> Unit
    - updateCommands(commands: List<String>): suspend -> Result<Unit>
  dynamic:
    - updateDynamicCommands(commands: List<QuantizedCommand>): suspend -> Result<Unit>
    - clearDynamicCommands(): Unit
    - dynamicCommandCount: Int
  state:
    - state: StateFlow<ServiceState>
    - commandResults: SharedFlow<CommandResult>
```

### ActionCoordinator
```yaml
package: com.augmentalis.voiceoscoreng
file: ActionCoordinator.kt
type: class
purpose: Routes voice commands through 5-level priority system
flow:
  1_dynamic: VUID-based screen-specific commands
  2_fuzzy: Fuzzy string matching
  3_static: Predefined system handlers
  4_nlu: BERT-based classification
  5_llm: LLM natural language fallback
methods:
  - processVoiceCommand(text: String): suspend -> HandlerResult
  - registerHandler(handler: CommandHandler): Unit
  - unregisterHandler(id: String): Boolean
```

### HandlerRegistry
```yaml
package: com.augmentalis.voiceoscoreng.handlers
file: HandlerRegistry.kt
type: class
purpose: Manages handler lifecycle and priority
categories: 11
priority_order: [SYSTEM, NAVIGATION, APP, GAZE, GESTURE, UI, DEVICE, INPUT, MEDIA, ACCESSIBILITY, CUSTOM]
methods:
  - register(handler: CommandHandler): Unit
  - unregister(id: String): Boolean
  - getHandler(category: ActionCategory): CommandHandler?
  - getHandlerForCommand(command: String): CommandHandler?
```

### CommandRegistry
```yaml
package: com.augmentalis.voiceoscoreng
file: CommandRegistry.kt
type: class
purpose: Dynamic screen-specific command registration
features:
  - VUID-based element targeting
  - Screen context awareness
  - Automatic cleanup on screen change
methods:
  - registerCommand(command: QuantizedCommand): Unit
  - unregisterCommand(vuid: String): Boolean
  - clearAll(): Unit
  - findCommand(text: String): QuantizedCommand?
```

### StaticCommandRegistry
```yaml
package: com.augmentalis.voiceoscoreng
file: StaticCommandRegistry.kt
type: class
purpose: Predefined system command management
features:
  - Predefined command patterns
  - Locale-aware command matching
  - Synonym support
methods:
  - getCommands(locale: String): List<StaticCommand>
  - findCommand(text: String, locale: String): StaticCommand?
  - registerCustomCommand(command: StaticCommand): Unit
```

---

## AVID_CLASSES

### AvidGenerator
```yaml
package: com.augmentalis.avid
file: AvidGenerator.kt
type: object
purpose: Cloud and local ID generation with platform encoding
id_formats:
  cloud: "AVID-{platform}-{sequence}"
  local: "AVIDL-{platform}-{sequence}"
platform_codes:
  A: Android
  I: iOS
  W: Web
  M: macOS
  X: Windows
  L: Linux
  U: Unknown
methods:
  cloud:
    - generateCloud(platform: Platform = detectPlatform()): String
    - generateCloudBatch(count: Int, platform: Platform = detectPlatform()): List<String>
  local:
    - generateLocal(platform: Platform = detectPlatform()): String
    - generateLocalBatch(count: Int, platform: Platform = detectPlatform()): List<String>
  validation:
    - isCloudId(id: String): Boolean
    - isLocalId(id: String): Boolean
    - isValid(id: String): Boolean
  parsing:
    - parse(id: String): AvidComponents?
    - getPlatform(id: String): Platform?
  promotion:
    - promoteToCloud(localId: String): String?
  platform:
    - detectPlatform(): Platform
```

### Platform
```yaml
package: com.augmentalis.avid
file: Platform.kt
type: enum
purpose: Platform identification for cross-device IDs
values:
  - ANDROID: code='A'
  - IOS: code='I'
  - WEB: code='W'
  - MACOS: code='M'
  - WINDOWS: code='X'
  - LINUX: code='L'
  - UNKNOWN: code='U'
methods:
  - fromCode(code: Char): Platform
```

### TypeCode
```yaml
package: com.augmentalis.avid
file: TypeCode.kt
type: object
purpose: 40+ semantic type codes for UI element classification
categories:
  basic_ui:
    - ELEMENT: "ELM" (generic fallback)
    - BUTTON: "BTN"
    - INPUT: "INP"
    - TEXT: "TXT"
    - IMAGE: "IMG"
    - ICON: "ICN"
    - LINK: "LNK"
  selection:
    - CHECKBOX: "CHK"
    - RADIO: "RAD"
    - SWITCH: "SWT"
    - TOGGLE: "TGL"
    - DROPDOWN: "DRP"
    - PICKER: "PKR"
  containers:
    - CONTAINER: "CTN"
    - CARD: "CRD"
    - LIST: "LST"
    - GRID: "GRD"
    - TABLE: "TBL"
    - ROW: "ROW"
    - CELL: "CEL"
  navigation:
    - MENU: "MNU"
    - TAB: "TAB"
    - NAV: "NAV"
    - TOOLBAR: "TBR"
    - HEADER: "HDR"
    - FOOTER: "FTR"
  interaction:
    - SCROLL: "SCR"
    - SLIDER: "SLD"
    - PROGRESS: "PRG"
    - SPINNER: "SPN"
  feedback:
    - DIALOG: "DLG"
    - TOAST: "TST"
    - SNACKBAR: "SNK"
    - ALERT: "ALT"
    - TOOLTIP: "TIP"
  media:
    - VIDEO: "VID"
    - AUDIO: "AUD"
    - CANVAS: "CNV"
    - MAP: "MAP"
  messaging:
    - MESSAGE: "MSG"
    - CONVERSATION: "CNV"
    - PARTICIPANT: "PRT"
methods:
  - fromClassName(className: String): String
  - isValid(code: String): Boolean
  - getCategory(code: String): String?
```

### ElementFingerprint
```yaml
package: com.augmentalis.avid
file: Fingerprint.kt
type: object
purpose: Deterministic hash-based UI element identification
format: "{TypeCode}:{hash8}"
features:
  - Collision-resistant: SHA-256 based
  - Cross-device reproducibility
  - Deterministic: same inputs = same output
  - Human readable: semantic type prefix
methods:
  generation:
    - generate(className: String, packageName: String, resourceId: String, text: String, contentDesc: String): String
  validation:
    - isValid(fingerprint: String): Boolean
  parsing:
    - parse(fingerprint: String): Pair<String, String>?  # Returns (typeCode, hash)
  utilities:
    - getTypeCode(fingerprint: String): String?
    - getHash(fingerprint: String): String?
example:
  input:
    className: "Button"
    packageName: "com.example.app"
    resourceId: "btn_submit"
    text: "Submit"
    contentDesc: "Submit button"
  output: "BTN:a3f8b2c1"
```

### AvidComponents
```yaml
package: com.augmentalis.avid
file: AvidGenerator.kt
type: data_class
purpose: Parsed AVID components
fields:
  - prefix: String (AVID or AVIDL)
  - platform: Platform
  - sequence: String
  - isCloud: Boolean
```

---

## SOLID_REFACTORED_CLASSES

### IHandlerRegistry (Interface)
```yaml
package: com.augmentalis.voiceoscoreng.handlers
file: IHandlerRegistry.kt
type: interface
lines: 138
commit: 5a0353a6
purpose: Abstraction for handler registration (DIP compliance)
methods:
  - register(handler: CommandHandler): Unit
  - unregister(id: String): Boolean
  - getHandler(category: ActionCategory): CommandHandler?
  - getHandlerForCommand(command: String): CommandHandler?
  - getAllHandlers(): List<CommandHandler>
```

### IMetricsCollector (Interface)
```yaml
package: com.augmentalis.voiceoscoreng.handlers
file: IMetricsCollector.kt
type: interface
lines: 81
commit: 5a0353a6
purpose: Abstraction for metrics collection (DIP compliance)
methods:
  - recordCommandExecution(command: String, duration: Long): Unit
  - recordHandlerInvocation(handler: String, success: Boolean): Unit
  - getMetrics(): MetricsSnapshot
```

### YamlComponentParser (SRP Split)
```yaml
package: com.augmentalis.voiceoscoreng.common
file: YamlComponentParser.kt
type: class
lines: 433
commit: 9231c8d9
purpose: YAML parsing for component definitions (SRP from ComponentFactory)
methods:
  - parse(yaml: String): ParseResult<ComponentDefinition>
  - parseFile(path: String): ParseResult<ComponentDefinition>
  - validateSyntax(yaml: String): List<SyntaxError>
```

### ComponentValidator (SRP Split)
```yaml
package: com.augmentalis.voiceoscoreng.common
file: ComponentValidator.kt
type: class
lines: 167
commit: 9231c8d9
purpose: Component validation logic (SRP from ComponentFactory)
methods:
  - validate(component: ComponentDefinition): ValidationResult
  - validateSchema(component: ComponentDefinition): List<SchemaError>
  - validateDependencies(component: ComponentDefinition): List<DependencyError>
```

### ComponentLoader (SRP Split)
```yaml
package: com.augmentalis.voiceoscoreng.common
file: ComponentLoader.kt
type: class
lines: 71
commit: 9231c8d9
purpose: Component loading orchestration (SRP from ComponentFactory)
methods:
  - load(name: String): Result<Component>
  - loadAll(): List<Component>
  - reload(name: String): Result<Component>
```

### BuiltInComponents (SRP Split)
```yaml
package: com.augmentalis.voiceoscoreng.common
file: BuiltInComponents.kt
type: object
commit: 9231c8d9
purpose: Predefined component definitions (SRP from ComponentFactory)
contents:
  - SystemComponents
  - NavigationComponents
  - UIComponents
  - InputComponents
```

### OverlayRegistry (SRP Split)
```yaml
package: com.augmentalis.voiceoscoreng.features
file: OverlayRegistry.kt
type: class
lines: 125
commit: 9231c8d9
purpose: Overlay registration/unregistration (SRP from OverlayManager)
methods:
  - register(overlay: Overlay): Unit
  - unregister(id: String): Boolean
  - get(id: String): Overlay?
  - getAll(): List<Overlay>
```

### OverlayVisibilityManager (SRP Split)
```yaml
package: com.augmentalis.voiceoscoreng.features
file: OverlayVisibilityManager.kt
type: class
lines: 104
commit: 9231c8d9
purpose: Overlay show/hide operations (SRP from OverlayManager)
methods:
  - show(id: String): Result<Unit>
  - hide(id: String): Result<Unit>
  - showAll(): Unit
  - hideAll(): Unit
  - isVisible(id: String): Boolean
```

### OverlayDisposal (SRP Split)
```yaml
package: com.augmentalis.voiceoscoreng.features
file: OverlayDisposal.kt
type: class
lines: 66
commit: 9231c8d9
purpose: Overlay cleanup operations (SRP from OverlayManager)
methods:
  - dispose(id: String): Unit
  - disposeAll(): Unit
  - scheduleDisposal(id: String, delay: Long): Unit
```

### StaticCommandDispatcher (SRP Split)
```yaml
package: com.augmentalis.voiceoscoreng.handlers
file: StaticCommandDispatcher.kt
type: class
lines: 64
commit: 9231c8d9
purpose: Static command handling (SRP from CommandDispatcher)
methods:
  - dispatch(command: String): HandlerResult
  - canHandle(command: String): Boolean
  - getStaticCommands(): List<StaticCommand>
```

### DynamicCommandDispatcher (SRP Split)
```yaml
package: com.augmentalis.voiceoscoreng.handlers
file: DynamicCommandDispatcher.kt
type: class
lines: 126
commit: 9231c8d9
purpose: Dynamic command handling (SRP from CommandDispatcher)
methods:
  - dispatch(command: String): HandlerResult
  - canHandle(command: String): Boolean
  - registerDynamicCommand(command: QuantizedCommand): Unit
  - clearDynamicCommands(): Unit
```

### VoiceCommandInterpreter (OCP Compliance)
```yaml
package: com.augmentalis.voiceoscoreng.handlers
file: VoiceCommandInterpreter.kt
type: class
lines: 181
commit: cf7fe0ff
purpose: Rule-based voice command interpretation (OCP from ActionCoordinator)
implements: IVoiceCommandInterpreter
methods:
  - interpret(utterance: String): InterpretationResult
  - addRule(rule: InterpretationRule): Unit
  - removeRule(id: String): Boolean
  - getRules(): List<InterpretationRule>
```

### SpeechEngineRegistry (OCP Compliance)
```yaml
package: com.augmentalis.voiceoscoreng.features
file: SpeechEngine.kt
type: class
commit: cf7fe0ff
purpose: Extensible speech engine registration (OCP compliance)
methods:
  - register(engine: ISpeechEngine): Unit
  - unregister(id: String): Boolean
  - get(id: String): ISpeechEngine?
  - getDefault(): ISpeechEngine
  - setDefault(id: String): Unit
```

### IWakeWordCapable (ISP Compliance)
```yaml
package: com.augmentalis.voiceoscoreng.features
file: ISpeechEngine.kt
type: interface
commit: cf7fe0ff
purpose: Wake word detection capability (ISP from IVivokaEngine)
methods:
  - setWakeWord(phrase: String): Result<Unit>
  - enableWakeWord(): Result<Unit>
  - disableWakeWord(): Unit
  - isWakeWordEnabled(): Boolean
```

### IModelManageable (ISP Compliance)
```yaml
package: com.augmentalis.voiceoscoreng.features
file: ISpeechEngine.kt
type: interface
commit: cf7fe0ff
purpose: Model management capability (ISP from IVivokaEngine)
methods:
  - loadModel(path: String): Result<Unit>
  - unloadModel(): Unit
  - isModelLoaded(): Boolean
  - getModelInfo(): ModelInfo?
```

---

## VOICEOS_APP_CLASSES

### VoiceOSAccessibilityService
```yaml
package: com.augmentalis.voiceoscoreng.service
file: VoiceOSAccessibilityService.kt
type: class
extends: AccessibilityService
lines: 1789
purpose: Android accessibility service for UI automation
capabilities:
  - Screen content access
  - UI element discovery
  - Action execution (click, scroll, etc.)
  - Continuous monitoring
key_methods:
  - onAccessibilityEvent(event: AccessibilityEvent): Unit
  - onServiceConnected(): Unit
  - performGlobalAction(action: Int): Boolean
  - findFocus(focus: Int): AccessibilityNodeInfo?
```

### OverlayService
```yaml
package: com.augmentalis.voiceoscoreng.service
file: OverlayService.kt
type: class
extends: Service
lines: 666
purpose: Numbers overlay for visual element identification
features:
  - Dimension-based caching
  - Screen orientation handling
  - Touch-through overlay
key_methods:
  - showOverlay(): Unit
  - hideOverlay(): Unit
  - updateNumbers(elements: List<UIElement>): Unit
  - refreshForDimensions(width: Int, height: Int): Unit
```

### ScreenExplorer
```yaml
package: com.augmentalis.voiceoscoreng.exploration
file: ScreenExplorer.kt
type: class
purpose: UI element discovery and analysis
methods:
  - exploreScreen(): List<UIElement>
  - findElementByVuid(vuid: String): UIElement?
  - getInteractiveElements(): List<UIElement>
  - calculateScreenHash(): String
```

---

## AVA_CLASSES

### ActionsManager
```yaml
package: com.augmentalis.ava.actions
file: ActionsManager.kt
type: class
purpose: Intent-to-action mapping and execution
handlers: 90+
methods:
  - initialize(): Unit
  - hasHandler(intent: String): Boolean
  - executeAction(intent: String, utterance: String): suspend -> ActionResult
  - getCategoryForIntent(intent: String): String
  - registerHandler(intent: String, handler: ActionHandler): Unit
```

### ChatViewModel
```yaml
package: com.augmentalis.ava.chat
file: ChatViewModel.kt
type: class
extends: ViewModel
purpose: Conversation state management
state:
  - messages: StateFlow<List<Message>>
  - isGenerating: StateFlow<Boolean>
methods:
  - sendMessage(userMessage: String): Unit
  - clearConversation(): Unit
  - retryLastMessage(): Unit
```

### MemoryManager
```yaml
package: com.augmentalis.ava.memory
file: MemoryManager.kt
type: interface
purpose: Long-term memory with decay
memory_types: [EPISODIC, SEMANTIC, PROCEDURAL]
methods:
  - remember(type: MemoryType, content: String, importance: Float): suspend -> MemoryEntry
  - recall(id: String): suspend -> MemoryEntry?
  - search(query: String, limit: Int): suspend -> List<MemoryEntry>
  - consolidateMemories(): suspend -> Unit
  - forget(id: String): suspend -> Boolean
```

---

## LLM_CLASSES

### HybridResponseGenerator
```yaml
package: com.augmentalis.llm.generation
file: HybridResponseGenerator.kt
type: class
implements: ResponseGenerator
purpose: Primary response generation with fallback chain
fallback_chain: [LocalLLM, Claude, GPT-4, Gemini, OpenRouter]
methods:
  - generateResponse(userMessage: String, classification: IntentClassification, context: ResponseContext): Flow<ResponseChunk>
  - stop(): suspend -> Unit
  - setPreferredProvider(provider: LLMProvider): Unit
```

### LocalLLMProvider
```yaml
package: com.augmentalis.llm.providers
file: LocalLLMProvider.kt
type: class
implements: LLMProvider
purpose: On-device LLM inference
runtime: MLC LLM + TVM
methods:
  - initialize(config: LLMConfig): suspend -> Result<Unit>
  - generateResponse(prompt: String, options: GenerationOptions): Flow<LLMResponse>
  - chat(messages: List<ChatMessage>, options: GenerationOptions): Flow<LLMResponse>
  - stop(): suspend -> Unit
  - cleanup(): suspend -> Unit
```

### CloudLLMProvider
```yaml
package: com.augmentalis.llm.providers
file: CloudLLMProvider.kt
type: class
implements: LLMProvider
purpose: Cloud-based LLM access
providers: [Anthropic, OpenAI, Google, OpenRouter]
methods:
  - initialize(config: LLMConfig): suspend -> Result<Unit>
  - generateResponse(prompt: String, options: GenerationOptions): Flow<LLMResponse>
  - chat(messages: List<ChatMessage>, options: GenerationOptions): Flow<LLMResponse>
  - estimateCost(tokens: Int): Float
```

---

## NLU_CLASSES

### HybridIntentClassifier
```yaml
package: com.augmentalis.nlu.classification
file: HybridIntentClassifier.kt
type: class
purpose: Multi-method intent classification
methods:
  - EXACT: threshold=0.95
  - FUZZY: min_similarity=0.7
  - SEMANTIC: min_similarity=0.6 (BERT)
config: ClassifierConfig
methods:
  - classify(input: String): ClassificationResult
  - classifyFast(input: String): IntentMatch?
  - index(intents: List<UnifiedIntent>): Unit
```

### ClassifyIntentUseCase
```yaml
package: com.augmentalis.nlu.usecase
file: ClassifyIntentUseCase.kt
type: class
purpose: Primary classification API
methods:
  - execute(utterance: String, language: String = "en-US"): Result<IntentClassification>
  - supportsLanguage(code: String): Boolean
  - supportedLanguages(): List<String>
languages: 52+ (via mALBERT)
```

### BertTokenizer
```yaml
package: com.augmentalis.nlu.tokenization
file: BertTokenizer.kt
type: class
purpose: BERT-compatible tokenization
methods:
  - tokenize(text: String): List<String>
  - encode(text: String): List<Int>
  - decode(ids: List<Int>): String
```

---

## RAG_CLASSES

### SQLiteRAGRepository
```yaml
package: com.augmentalis.rag.repository
file: SQLiteRAGRepository.kt
type: class
implements: RAGRepository
purpose: SQLite-based document storage and retrieval
clustering: K-means (256 clusters)
methods:
  - addDocument(request: AddDocumentRequest): suspend -> Result<AddDocumentResult>
  - search(query: SearchQuery): suspend -> Result<SearchResponse>
  - deleteDocument(documentId: String): suspend -> Result<Unit>
  - listDocuments(status: DocumentStatus?): Flow<Document>
```

### RAGChatEngine
```yaml
package: com.augmentalis.rag.chat
file: RAGChatEngine.kt
type: class
purpose: Chat with document context
dependencies: [RAGRepository, LLMProvider]
methods:
  - ask(question: String, history: List<Message>): Flow<ChatResponse>
  - setContextWindow(size: Int): Unit
```

### KMeansClustering
```yaml
package: com.augmentalis.rag.clustering
file: KMeansClustering.kt
type: class
purpose: Document embedding clustering
clusters: 256
methods:
  - cluster(embeddings: List<FloatArray>): List<Int>
  - findNearestCluster(embedding: FloatArray): Int
  - getCentroid(clusterId: Int): FloatArray
```

---

## WEBAVANUE_CLASSES

### DOMScraperBridge
```yaml
package: com.augmentalis.webavanue.voiceos
file: DOMScraperBridge.kt
type: class
lines: 428
purpose: JavaScript bridge for DOM element extraction
features:
  - VUID generation for elements
  - Bounds extraction
  - Selector generation
  - ARIA label extraction
methods:
  - scrapeDOM(): suspend -> List<DOMElement>
  - executeClick(vuid: String): Unit
  - executeInput(vuid: String, text: String): Unit
  - executeScroll(direction: String): Unit
  - attach(): Unit
  - detach(): Unit
```

### VoiceCommandGenerator
```yaml
package: com.augmentalis.webavanue.voiceos
file: VoiceCommandGenerator.kt
type: class
lines: 311
purpose: Fuzzy voice command matching
matching: Word-based (minimum 2 words)
methods:
  - matchCommand(utterance: String, elements: List<DOMElement>): MatchResult
result:
  element: DOMElement?
  confidence: Float
  alternatives: List<DOMElement>
```

### TabManager
```yaml
package: com.augmentalis.webavanue.tabs
file: TabManager.kt
type: class
purpose: Tab management with LRU caching
performance: 4x faster retrieval
methods:
  - createTab(url: String): Tab
  - closeTab(tabId: String): Boolean
  - setActiveTab(tabId: String): Unit
  - getAllTabs(): List<Tab>
  - getTab(tabId: String): Tab?
```

---

## COMMON_CLASSES

### VUIDGenerator (DEPRECATED)
```yaml
package: com.augmentalis.common.vuid
file: VUIDGenerator.kt
type: object
status: DEPRECATED
deprecated_by: com.augmentalis.avid.AvidGenerator, com.augmentalis.avid.ElementFingerprint
purpose: Unique identifier generation (DEPRECATED - use AVID module instead)
migration_guide:
  entity_ids: "Use AvidGenerator.generateCloud() or AvidGenerator.generateLocal()"
  ui_fingerprints: "Use ElementFingerprint.generate()"
  message_ids: "Use AvidGenerator.generateCloud() with platform detection"
format: Compact, parseable VUIDs
methods:
  - generateCompact(packageName: String, version: String, typeName: String): String
  - generateMessageVuid(): String
  - generateConversationVuid(): String
  - isValid(vuid: String): Boolean
  - parse(vuid: String): ParsedVuid?
```

### VoiceOSResult
```yaml
package: com.augmentalis.common.voiceos.result
file: VoiceOSResult.kt
type: sealed_class
purpose: Type-safe error handling
variants:
  - Success<T, E>(value: T)
  - Failure<T, E>(error: E)
methods:
  - map<R>(transform: (T) -> R): VoiceOSResult<R, E>
  - flatMap<R>(transform: (T) -> VoiceOSResult<R, E>): VoiceOSResult<R, E>
  - mapError<F>(transform: (E) -> F): VoiceOSResult<T, F>
  - getOrNull(): T?
  - getOrThrow(): T
```

### ThemeManager
```yaml
package: com.augmentalis.common.core.theme
file: ThemeManager.kt
type: object
purpose: Universal theme management
methods:
  - setUniversalTheme(theme: Theme): suspend -> Unit
  - getUniversalTheme(): Theme
  - observeUniversalTheme(): StateFlow<Theme>
  - setAppTheme(appId: String, theme: Theme): suspend -> Unit
  - getTheme(appId: String): Theme
```

### AssetManager
```yaml
package: com.augmentalis.common.core.assets
file: AssetManager.kt
type: object
purpose: Icon and image management
methods:
  - registerIconLibrary(library: IconLibrary): suspend -> Unit
  - getIcon(reference: String): suspend -> Icon?
  - searchIcons(query: String): suspend -> List<IconSearchResult>
```

---

## DATA_CLASSES

### HandlerResult
```yaml
package: com.augmentalis.voiceoscoreng.common
file: HandlerResult.kt
type: sealed_class
variants:
  - Success(message: String, data: Any? = null)
  - Failure(reason: String, recoverable: Boolean = true)
  - NotHandled
  - RequiresInput(prompt: String, inputType: InputType)
  - AwaitingSelection(message: String, matchCount: Int)
```

### IntentClassification
```yaml
package: com.augmentalis.nlu.model
file: IntentClassification.kt
type: data_class
fields:
  - intent: String
  - confidence: Float
  - entities: Map<String, String>
```

### LLMResponse
```yaml
package: com.augmentalis.llm.model
file: LLMResponse.kt
type: sealed_class
variants:
  - Streaming(chunk: String)
  - Complete(fullText: String, usage: TokenUsage)
  - Error(message: String)
```

### DOMElement
```yaml
package: com.augmentalis.webavanue.voiceos
file: DOMElement.kt
type: data_class
lines: 135
fields:
  - vuid: String
  - tagName: String
  - text: String?
  - bounds: Rect
  - selector: String
  - ariaLabel: String?
  - role: String?
```

---

## ENUMS

### ServiceState
```yaml
values: [Uninitialized, Initializing, Ready, Listening, Processing, Error, Paused, Stopped]
```

### ActionCategory
```yaml
values: [SYSTEM, NAVIGATION, APP, UI, INPUT, MEDIA, ACCESSIBILITY, CUSTOM]
```

### MatchMethod
```yaml
values: [EXACT, FUZZY, SEMANTIC, HYBRID]
```

### DocumentType
```yaml
values: [PDF, DOCX, TXT, MD, HTML, EPUB, RTF]
```

### ChunkingStrategy
```yaml
values: [FIXED_SIZE, SEMANTIC, HYBRID]
```

---

# END CLASS-INDEX
