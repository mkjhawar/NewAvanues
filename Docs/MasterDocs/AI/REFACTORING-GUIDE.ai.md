# REFACTORING-GUIDE
# AI-Readable Refactoring Recommendations
# Version: 1.0 | Updated: 2026-01-11
# Based on: Analysis documents from 2026-01-08

---

## SOLID_COMPLIANCE_SUMMARY

```yaml
overall_score: 7.8/10
status: requires_refactoring

scores:
  SRP: 8/10  # Single Responsibility
  OCP: 8/10  # Open/Closed
  LSP: 8/10  # Liskov Substitution
  ISP: 6.5/10  # Interface Segregation (NEEDS WORK)
  DIP: 8.5/10  # Dependency Inversion
```

---

## CRITICAL_ISSUES

### ISSUE_1: Database Not Populated
```yaml
severity: critical
root_cause: No connection between VoiceOSAccessibilityService and persistence layer
impact: Learned commands not persisted across sessions

current_flow:
  - VoiceOSAccessibilityService explores UI
  - Stores in in-memory CommandRegistry only
  - Never calls AndroidCommandPersistence
  - Database file never created

required_fix:
  files:
    - VoiceOSCoreNGApplication.kt: Initialize database driver
    - VoiceOSAccessibilityService.kt: Wire to persistence
  steps:
    1: Create AndroidSqliteDriver in Application.onCreate()
    2: Instantiate SQLDelightGeneratedCommandRepository
    3: Create AndroidCommandPersistence
    4: Call persistence.insertBatch() after command generation
```

### ISSUE_2: Voice Engine Not Initialized
```yaml
severity: critical
root_cause: Two classes named VoiceOSCoreNG causing confusion
impact: Voice recognition never starts

conflicting_classes:
  - path: commonMain/VoiceOSCoreNG.kt
    type: class with Builder
    purpose: Main facade for voice processing
    usage: SHOULD be used but IS NOT

  - path: androidMain/handlers/VoiceOSCoreNG.kt
    type: object (singleton)
    purpose: Config/toggle management only
    usage: IS being used but SHOULD NOT be main entry

required_fix:
  option_a_recommended:
    location: VoiceOSAccessibilityService.kt
    code: |
      voiceOSCore = VoiceOSCoreNG.createForAndroid(
        service = this,
        configuration = ServiceConfiguration.DEFAULT
      )
      voiceOSCore.initialize()
      voiceOSCore.startListening()

  option_b_alternative:
    action: Rename handlers/VoiceOSCoreNG.kt to VoiceOSCoreNGConfig
```

### ISSUE_3: Screen Change Trigger Missing
```yaml
severity: critical
root_cause: onAccessibilityEvent doesn't trigger command regeneration
impact: Dynamic commands not updated on screen changes

missing_code:
  file: VoiceOSAccessibilityService.kt
  method: onAccessibilityEvent
  required: |
    when (event.eventType) {
      TYPE_WINDOW_STATE_CHANGED,
      TYPE_WINDOW_CONTENT_CHANGED -> {
        // Trigger: CommandGenerator → CommandRegistry.update()
      }
    }
```

---

## SOLID_VIOLATIONS

### ISP_1: IActionExecutor Too Large
```yaml
priority: high
file: src/commonMain/.../handlers/IActionExecutor.kt
problem: 24 methods in single interface
impact: Handlers must implement all methods even if only need subset

current_method_count: 24
categories:
  element_actions: [tap, longPress, focus, enterText]
  scroll_actions: [scroll]
  navigation_actions: [back, home, recentApps, appDrawer]
  system_actions: [openSettings, showNotifications, clearNotifications, screenshot, flashlight]
  media_actions: [mediaPlayPause, mediaNext, mediaPrevious, volume]
  app_actions: [openApp, openAppByPackage, closeApp]
  generic_execution: [executeCommand, executeAction]
  element_lookup: [elementExists, getElementBounds]

refactoring:
  split_into:
    - IElementActionExecutor: tap, longPress, focus, enterText
    - IScrollActionExecutor: scroll
    - INavigationActionExecutor: back, home, recentApps, appDrawer
    - ISystemActionExecutor: openSettings, showNotifications, clearNotifications, screenshot, flashlight
    - IMediaActionExecutor: mediaPlayPause, mediaNext, mediaPrevious, volume
    - IAppActionExecutor: openApp, openAppByPackage, closeApp
    - IElementLookupExecutor: elementExists, getElementBounds
    - ICommandExecutor: executeCommand, executeAction

  composite_interface: |
    interface IActionExecutor :
      IElementActionExecutor,
      IScrollActionExecutor,
      INavigationActionExecutor,
      ISystemActionExecutor,
      IMediaActionExecutor,
      IAppActionExecutor,
      IElementLookupExecutor,
      ICommandExecutor

  files_to_update:
    - NavigationHandler.kt: use INavigationActionExecutor
    - InputHandler.kt: use IElementActionExecutor
    - SystemHandler.kt: use ISystemActionExecutor
    - MediaHandler.kt: use IMediaActionExecutor
    - AndroidActionExecutor.kt: implement all
```

### SRP_1: ComponentFactory Multiple Responsibilities
```yaml
priority: high
file: src/commonMain/.../common/ComponentFactory.kt
problem: 5 responsibilities mixed in one class
responsibilities:
  1: YAML parsing
  2: Component validation
  3: Component caching
  4: Component loading
  5: Built-in component definitions

refactoring:
  split_into:
    ComponentParser:
      purpose: Parse YAML
      methods: [parse(yaml): ComponentDefinition]

    ComponentCache:
      purpose: Cache management
      methods: [get, put, clear, contains]

    ComponentValidator:
      purpose: Validation
      methods: [validate(definition): ValidationResult]

    ComponentLoader:
      purpose: Load from sources
      dependencies: [ComponentParser, ComponentCache, ComponentValidator]
      methods: [load, loadOrCache]

    BuiltInComponents:
      type: object
      purpose: Predefined components
      fields: [BUTTON, TEXT_FIELD, etc.]
```

### OCP_1: SpeechEngine Hardcoded Properties
```yaml
priority: medium
file: src/commonMain/.../features/SpeechEngine.kt
problem: Adding new engine requires modifying enum and 7+ extension functions

refactoring:
  create_data_class: |
    data class SpeechEngineCapabilities(
      val isOfflineCapable: Boolean,
      val requiresApiKey: Boolean,
      val displayName: String,
      val memoryUsageMB: Int,
      val supportedLanguages: List<String>
    )

  create_registry: |
    object SpeechEngineRegistry {
      private val capabilities = mutableMapOf<SpeechEngine, SpeechEngineCapabilities>()
      fun register(engine, capabilities)
      fun isOfflineCapable(engine): Boolean
      fun requiresApiKey(engine): Boolean
    }
```

### LSP_1: NativeHandler Fallback Violates Contract
```yaml
priority: medium
file: src/commonMain/.../handlers/NativeHandler.kt
problem: canHandle() always returns true, breaking substitution

current_code: |
  override fun canHandle(elements: List<ElementInfo>): Boolean {
    return true  // ALWAYS TRUE - violates contract!
  }

refactoring:
  option_1_explicit_fallback: |
    interface FrameworkHandler {
      val isFallbackHandler: Boolean get() = false
      fun canHandle(elements): Boolean
    }
    class NativeHandler : FrameworkHandler {
      override val isFallbackHandler = true
      override fun canHandle(elements) = elements.none { it.hasFrameworkMarker() }
    }

  option_2_sealed_type: |
    sealed interface FrameworkHandler
    class SpecializedHandler : FrameworkHandler { /* real logic */ }
    object FallbackNativeHandler : FrameworkHandler { /* documented fallback */ }
```

### ISP_2: IVivokaEngine Too Many Methods
```yaml
priority: medium
file: src/commonMain/.../features/IVivokaEngine.kt
problem: 13 methods added on top of ISpeechEngine

refactoring:
  split_into:
    IWakeWordCapable:
      fields: [isWakeWordEnabled, wakeWordDetected]
      methods: [enableWakeWord, disableWakeWord, getAvailableWakeWords]

    IModelManageable:
      fields: [availableModels, currentModel]
      methods: [loadModel, unloadModel, isModelDownloaded, downloadModel, deleteModel, getModelsDiskUsage]

    IVivokaEngine:
      extends: [ISpeechEngine, IWakeWordCapable, IModelManageable]
```

### SRP_2: OverlayManager Too Many Operations
```yaml
priority: medium
file: src/commonMain/.../features/OverlayManager.kt
problem: 10+ operation categories

refactoring:
  split_into:
    - OverlayRegistry: registration/unregistration
    - OverlayVisibilityManager: show/hide/visibility queries
    - OverlayDisposal: dispose/disposeAll/clear
```

### SRP_3: CommandDispatcher Multiple Concerns
```yaml
priority: medium
file: src/commonMain/.../handlers/CommandDispatcher.kt
problem: Handles static, dynamic, mode management, and events

refactoring:
  split_into:
    - StaticCommandDispatcher: static command matching/execution
    - DynamicCommandDispatcher: dynamic command matching/execution
    - CompositeDispatcher: orchestrates both
```

### OCP_2: ActionCoordinator Hardcoded Patterns
```yaml
priority: low
file: src/commonMain/.../handlers/ActionCoordinator.kt
lines: 151-180
problem: 20+ hardcoded voice patterns in when-expression

refactoring:
  extract_interpreter: |
    interface VoiceCommandInterpreter {
      fun interpret(command: String): String?
    }

    class RuleBasedVoiceCommandInterpreter : VoiceCommandInterpreter {
      data class InterpretationRule(val patterns: Set<String>, val action: String)
      private val rules = mutableListOf<InterpretationRule>()

      init {
        addRule(setOf("go back", "back"), "back")
        addRule(setOf("go home", "home"), "home")
      }

      override fun interpret(command): String? =
        rules.firstOrNull { it.matches(command) }?.action
    }
```

---

## IMPLEMENTATION_ORDER

```yaml
phase_1_interface_segregation:
  priority: P0
  effort: 2-3 hours
  tasks:
    - Split IActionExecutor into focused interfaces
    - Update all handlers to use appropriate interfaces
    - Split IVivokaEngine into focused interfaces
    - Update Vivoka implementations

phase_2_critical_fixes:
  priority: P0
  effort: 2 hours
  tasks:
    - Wire database persistence
    - Fix voice engine initialization
    - Implement screen change trigger

phase_3_single_responsibility:
  priority: P1
  effort: 3-4 hours
  tasks:
    - Extract ComponentFactory into separate classes
    - Split OverlayManager responsibilities
    - Split CommandDispatcher into specialized dispatchers

phase_4_open_closed:
  priority: P2
  effort: 2 hours
  tasks:
    - Refactor SpeechEngine to registry pattern
    - Extract VoiceCommandInterpreter from ActionCoordinator
    - Make FrameworkHandlerRegistry configuration-driven

phase_5_liskov_substitution:
  priority: P2
  effort: 30 min
  tasks:
    - Fix NativeHandler.canHandle() to respect contract

phase_6_verification:
  priority: P0
  effort: 1 hour
  checklist:
    - Run all existing tests
    - Verify no compilation errors
    - Check for missing implementations
    - Ensure backward compatibility
    - Test voice recognition on device
```

---

## KEY_FILES_REFERENCE

```yaml
main_facade: src/commonMain/.../VoiceOSCoreNG.kt
handler_interface: src/commonMain/.../handlers/IHandler.kt
action_executor: src/commonMain/.../handlers/IActionExecutor.kt
speech_engine: src/commonMain/.../features/ISpeechEngine.kt
vivoka_engine: src/commonMain/.../features/IVivokaEngine.kt
component_factory: src/commonMain/.../common/ComponentFactory.kt
handler_registry: src/commonMain/.../handlers/HandlerRegistry.kt
action_coordinator: src/commonMain/.../handlers/ActionCoordinator.kt
command_dispatcher: src/commonMain/.../handlers/CommandDispatcher.kt
framework_handlers: src/commonMain/.../handlers/FrameworkHandler.kt
native_handler: src/commonMain/.../handlers/NativeHandler.kt
overlay_manager: src/commonMain/.../features/OverlayManager.kt
android_executor: src/androidMain/.../handlers/AndroidActionExecutor.kt
```

---

## VERIFICATION_CHECKLIST

```yaml
after_refactoring:
  - ./gradlew :Modules:VoiceOSCoreNG:test
  - ./gradlew :Modules:VoiceOSCoreNG:compileDebugKotlinAndroid
  - ./gradlew :Modules:VoiceOSCoreNG:compileKotlinIosArm64
  - Verify no missing interface implementations
  - Verify backward compatibility (existing IActionExecutor code works)
  - Test voice recognition on device
  - Verify dynamic commands registered with Vivoka SDK
```

---

# END REFACTORING-GUIDE
