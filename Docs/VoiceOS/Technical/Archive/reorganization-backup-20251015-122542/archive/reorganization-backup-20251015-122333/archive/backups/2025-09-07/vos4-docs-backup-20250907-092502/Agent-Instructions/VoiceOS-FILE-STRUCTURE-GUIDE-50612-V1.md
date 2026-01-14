/**
 * FILE-STRUCTURE-GUIDE.md - Complete VOS4 File Structure Navigation Guide
 * Path: /Agent-Instructions/FILE-STRUCTURE-GUIDE.md
 * 
 * Created: 2025-01-21
 * Last Modified: 2025-01-21
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Comprehensive guide for AI agents on VOS4 project structure navigation
 * Module: System
 * 
 * Changelog:
 * - v1.0.0 (2025-01-21): Initial creation with complete project hierarchy
 */

# VOS4 File Structure Navigation Guide

## CRITICAL: Project Root
**ALL file paths in this guide start from:** `/Volumes/M Drive/Coding/Warp/VOS4/`

## Master Project Hierarchy

```
VOS4/
├── docs/                           # All documentation
│   ├── Agent-Instructions/        # AI agent instructions
│   ├── Analysis/                  # System and module analysis
│   ├── Architecture/              # System architecture docs
│   ├── Guides/                    # Developer guides
│   ├── Implementation/            # Implementation plans
│   ├── Migration/                 # Legacy migration docs
│   ├── PRD/                       # Product Requirements Documents
│   ├── Roadmap/                   # Implementation roadmaps
│   └── Status/                    # Current status reports
├── app/                           # Main Android application
├── modules/                       # All modules organized here
│   ├── apps/                      # Modular applications
│   │   ├── SpeechRecognition/     # Speech recognition app module
│   │   ├── VoiceAccessibility/    # Accessibility service app
│   │   └── VoiceUI/               # Voice UI framework app
│   ├── managers/                  # Core system managers
│   │   ├── CommandsMGR/           # Command processing manager
│   │   ├── CoreMGR/               # Core system manager
│   │   ├── DataMGR/               # Data persistence manager
│   │   ├── LicenseMGR/            # License management
│   │   └── LocalizationMGR/       # Localization manager
│   └── libraries/                 # Reusable libraries
│       ├── DeviceMGR/             # Device management library
│       ├── UUIDManager/           # UUID management library
│       └── VoiceUIElements/       # Voice UI components
├── docs/                          # Technical documentation
├── gradle/                        # Gradle configuration
└── tools/                         # Development tools
```

---

## Apps Module Deep Dive

### SpeechRecognition App Structure
```
modules/apps/SpeechRecognition/
├── docs/                          # Module documentation
│   ├── API_USAGE_EXAMPLES.md
│   └── DEVELOPER_GUIDE.md
├── src/main/java/com/ai/
│   ├── api/                       # Public API interfaces
│   │   ├── RecognitionResult.kt
│   │   └── RecognitionTypes.kt
│   ├── cache/                     # Caching subsystem
│   ├── config/                    # Configuration management
│   │   ├── EngineConfig.kt
│   │   └── RecognitionParameters.kt
│   ├── data/                      # Data persistence layer
│   │   ├── ObjectBoxManager.kt
│   │   ├── converters/            # Data type converters
│   │   ├── entities/              # ObjectBox entities
│   │   │   ├── CommandHistoryEntity.kt
│   │   │   ├── CompiledGrammar.kt
│   │   │   ├── CustomCommandEntity.kt
│   │   │   ├── GrammarCache.kt
│   │   │   ├── LanguageModelEntity.kt
│   │   │   ├── RecognitionHistoryEntity.kt
│   │   │   ├── StaticCommandCacheEntity.kt
│   │   │   ├── UniversalGrammar.kt
│   │   │   └── UsageStats.kt
│   │   ├── migration/             # Database migration scripts
│   │   └── repositories/          # Repository pattern implementations
│   ├── engines/                   # Speech recognition engines
│   │   ├── GrammarAgent.kt
│   │   ├── IRecognitionEngine.kt
│   │   ├── android/               # Android native engine
│   │   ├── azure/                 # Microsoft Azure engine
│   │   ├── google/                # Google Cloud engine
│   │   ├── openai/                # OpenAI Whisper engine
│   │   ├── vosk/                  # Vosk engine
│   │   └── vivoka/                # Vivoka engine
│   ├── events/                    # Event handling system
│   │   └── RecognitionEventBus.kt
│   ├── optimization/              # Performance optimization
│   │   └── ProcessorOptimizedScheduler.kt
│   ├── processing/                # Command processing pipeline
│   │   ├── CommandProcessor.kt
│   │   ├── CommandType.kt
│   │   ├── ResponseDelayManager.kt
│   │   ├── SimilarityMatcher.kt
│   │   └── VocabularyCache.kt
│   ├── service/                   # Background services
│   │   └── VadTypes.kt
│   ├── startup/                   # Initialization components
│   │   └── StaticCommandLoader.kt
│   ├── utils/                     # Utility classes
│   │   ├── LanguageUtils.kt
│   │   ├── VoiceOsLogger.kt
│   │   ├── VoiceUtils.kt
│   │   └── VsdkHandlerUtils.kt
│   ├── vad/                       # Voice Activity Detection
│   └── wakeword/                  # Wake word detection
│       └── WakeWordDetector.kt
└── objectbox-models/              # ObjectBox model definitions
    └── default.json
```

#### SpeechRecognition Engines (6 Total):
1. **android/** - Android native speech recognition
2. **azure/** - Microsoft Azure Cognitive Services
3. **google/** - Google Cloud Speech-to-Text
4. **openai/** - OpenAI Whisper (planned)
5. **vosk/** - Vosk offline recognition
6. **vivoka/** - Vivoka Voice Development Kit

---

### VoiceAccessibility App Structure
```
modules/apps/VoiceAccessibility/
├── src/main/java/com/ai/voiceaccessibility/
│   ├── AccessibilityModule.kt     # Main module entry point
│   ├── actions/                   # Action processors
│   │   └── AccessibilityActionProcessor.kt
│   ├── api/                       # Public API
│   │   └── IAccessibilityModule.kt
│   ├── events/                    # Event system
│   │   ├── AccessibilityEventBus.kt
│   │   └── AccessibilityEvents.kt
│   ├── extractors/                # UI element extraction
│   │   └── UIElementExtractor.kt
│   ├── processors/                # Processing components
│   │   └── DuplicateResolver.kt
│   ├── service/                   # Accessibility service
│   │   ├── AccessibilityServiceWrapper.kt
│   │   └── VOS4AccessibilityService.kt
│   ├── touch/                     # Touch interaction bridge
│   │   └── TouchBridge.kt
│   └── ui/                        # User interface
│       └── AccessibilitySettingsActivity.kt
└── assets/static_commands/        # Static command definitions
    ├── static_commands_en_us.json
    ├── static_commands_es_es.json
    ├── static_commands_fr_fr.json
    └── [40+ language files]
```

#### VoiceAccessibility Subsystems:
- **TouchBridge**: Direct touch interaction system
- **UITreeTraversal**: UI hierarchy navigation
- **AccessibilityEventBus**: Event distribution
- **UIElementExtractor**: Screen element analysis
- **DuplicateResolver**: Conflict resolution

---

### VoiceUI App Structure
```
modules/apps/VoiceUI/
├── migration/                     # Legacy UIKit migration
│   ├── MIGRATION-PLAN.md
│   ├── analysis/
│   ├── build-config/
│   └── legacy-backup/
├── src/main/java/com/ai/
│   ├── VoiceUIModule.kt          # Main module
│   ├── api/                      # Public interfaces
│   │   └── IVoiceUIModule.kt
│   ├── gestures/                 # Gesture management
│   │   └── GestureManager.kt
│   ├── hud/                      # Heads-up display
│   │   └── HUDSystem.kt
│   ├── notifications/            # Notification system
│   │   └── NotificationSystem.kt
│   ├── theme/                    # Theme engine
│   │   └── ThemeEngine.kt
│   ├── visualization/            # Data visualization
│   │   └── DataVisualization.kt
│   ├── voice/                    # Voice command system
│   │   └── VoiceCommandSystem.kt
│   └── windows/                  # Window management
│       └── WindowManager.kt
```

---

## Managers Module Deep Dive

### CommandsMGR Structure
```
modules/managers/CommandsMGR/
├── src/main/java/com/ai/
│   ├── CommandsManager.kt         # Main manager
│   ├── actions/                   # Action categories (11 total)
│   │   ├── AppActions.kt          # Application control
│   │   ├── BaseAction.kt          # Base action interface
│   │   ├── CursorActions.kt       # Cursor manipulation
│   │   ├── DictationActions.kt    # Text dictation
│   │   ├── DragActions.kt         # Drag and drop operations
│   │   ├── GestureActions.kt      # Gesture recognition
│   │   ├── NavigationActions.kt   # Screen navigation
│   │   ├── OverlayActions.kt      # Overlay management
│   │   ├── ScrollActions.kt       # Scrolling operations
│   │   ├── SystemActions.kt       # System-level actions
│   │   ├── TextActions.kt         # Text manipulation
│   │   └── VolumeActions.kt       # Volume control
│   ├── api/                       # Public interfaces
│   │   └── ICommandModule.kt
│   ├── context/                   # Context management
│   │   └── ContextManager.kt
│   ├── definitions/               # Command definitions
│   │   └── CommandDefinitions.kt
│   ├── history/                   # Command history
│   │   └── CommandHistory.kt
│   ├── models/                    # Data models
│   │   ├── ActionResult.kt
│   │   └── CommandModels.kt
│   ├── processor/                 # Command processing
│   │   └── CommandProcessor.kt
│   ├── registry/                  # Command registry
│   │   └── CommandRegistry.kt
│   └── validation/                # Input validation
│       └── CommandValidator.kt
```

#### CommandsMGR Action Categories (11 Total):
1. **AppActions** - Application lifecycle and control
2. **CursorActions** - Mouse cursor positioning and clicks
3. **DictationActions** - Speech-to-text input
4. **DragActions** - Drag and drop operations
5. **GestureActions** - Touch and gesture recognition
6. **NavigationActions** - Screen and app navigation
7. **OverlayActions** - System overlay management
8. **ScrollActions** - Scrolling and paging
9. **SystemActions** - System-level operations
10. **TextActions** - Text selection and editing
11. **VolumeActions** - Audio volume control

---

### DataMGR Structure
```
modules/managers/DataMGR/
├── objectbox-models/              # ObjectBox database models
│   └── default.json
├── src/main/java/com/ai/
│   ├── DatabaseModule.kt          # Main database module
│   ├── DatabaseObjectBox.kt       # ObjectBox implementation
│   ├── datamgr/
│   │   └── ObjectBox.kt
│   ├── entities/                  # Data entities (12 types)
│   │   ├── AnalyticsSettings.kt
│   │   ├── CommandHistoryEntry.kt
│   │   ├── CustomCommand.kt
│   │   ├── DeviceProfile.kt
│   │   ├── ErrorReport.kt
│   │   ├── GestureLearningData.kt
│   │   ├── LanguageModel.kt
│   │   ├── RetentionSettings.kt
│   │   ├── TouchGesture.kt
│   │   ├── UsageStatistic.kt
│   │   ├── UserPreference.kt
│   │   └── UserSequence.kt
│   ├── export/                    # Data import/export
│   │   ├── DataExporter.kt
│   │   └── DataImporter.kt
│   └── repository/                # Repository implementations
│       ├── AnalyticsSettings.kt
│       ├── BaseRepository.kt
│       ├── CommandHistory.kt
│       ├── CustomCommand.kt
│       ├── DeviceProfile.kt
│       ├── ErrorReport.kt
│       ├── GestureLearning.kt
│       ├── LanguageModel.kt
│       ├── RetentionSettings.kt
│       ├── TouchGesture.kt
│       ├── UsageStatistic.kt
│       ├── UserPreference.kt
│       └── UserSequence.kt
```

---

## Libraries Module Deep Dive

### UUIDManager Library Structure
```
modules/libraries/UUIDManager/
├── src/main/java/com/ai/uuidmgr/
│   ├── UUIDManager.kt             # Main UUID manager
│   ├── api/                       # Public interfaces
│   │   └── IUUIDManager.kt
│   ├── compose/                   # Jetpack Compose extensions
│   │   └── ComposeExtensions.kt
│   ├── core/                      # Core UUID functionality
│   │   ├── UUIDGenerator.kt
│   │   └── UUIDRegistry.kt
│   ├── models/                    # UUID data models
│   │   ├── CommandResult.kt
│   │   ├── Position.kt
│   │   ├── TargetType.kt
│   │   ├── UUIDCommandResult.kt
│   │   ├── UUIDElement.kt
│   │   ├── UUIDHierarchy.kt
│   │   ├── UUIDMetadata.kt
│   │   ├── UUIDPosition.kt
│   │   ├── VoiceCommand.kt
│   │   └── VoiceTarget.kt
│   ├── spatial/                   # Spatial navigation
│   │   └── SpatialNavigator.kt
│   ├── targeting/                 # Target resolution
│   │   └── TargetResolver.kt
│   └── view/                      # View extensions
```

---

## Function Subfolder Creation Rules

### When to Create Function Subfolders

#### ALWAYS Create Subfolders When:
1. **Module has 6+ main functions** (like SpeechRecognition engines)
2. **Category has 5+ implementations** (like CommandsMGR actions)
3. **Logical grouping exists** (api/, utils/, models/, etc.)

#### SpeechRecognition Example:
```
engines/                           # 6 engines = separate folders
├── android/
│   ├── AndroidRecognitionEngine.kt
│   ├── AndroidConfig.kt
│   └── AndroidUtils.kt
├── azure/
│   ├── AzureRecognitionEngine.kt
│   ├── AzureCredentials.kt
│   └── AzureStreamHandler.kt
├── google/
├── openai/
├── vosk/
└── vivoka/
```

#### CommandsMGR Example:
```
actions/                           # 11 actions = all in same level
├── AppActions.kt                  # Each file is self-contained
├── CursorActions.kt
├── DictationActions.kt
└── [remaining actions...]
```

### Naming Conventions for Function Folders

#### Use These Patterns:
- **Implementation folders**: lowercase (android/, azure/, google/)
- **Category folders**: lowercase (actions/, processors/, utils/)
- **API folders**: lowercase (api/, interfaces/, models/)
- **Specific features**: camelCase only for files, not folders

---

## Navigation Patterns

### Finding Module Components

#### Pattern 1: Direct Navigation
```bash
# Go directly to known module
cd /Volumes/M\ Drive/Coding/Warp/VOS4/modules/apps/SpeechRecognition/src/main/java/com/ai/
```

#### Pattern 2: Search by Function
```bash
# Find all recognition engines
find VOS4/apps/SpeechRecognition -name "*Engine*" -type f
```

#### Pattern 3: Search by Category
```bash
# Find all action implementations
find VOS4/managers/CommandsMGR -path "*/actions/*" -name "*.kt"
```

### Search Strategies

#### For Code Implementation:
```bash
# Find specific functionality
grep -r "class.*Manager" VOS4/managers/*/src --include="*.kt"
```

#### For Configuration:
```bash
# Find config files
find VOS4 -name "*config*" -o -name "*Config*" | grep -v build
```

#### For Documentation:
```bash
# Find module docs
find VOS4 -path "*/docs/*" -name "*.md"
```

---

## File Organization Examples

### Example 1: Adding New Speech Engine
**Location**: `/modules/apps/SpeechRecognition/src/main/java/com/ai/engines/newengine/`
```
newengine/
├── NewEngineRecognitionEngine.kt  # Main implementation
├── NewEngineConfig.kt             # Engine configuration
├── NewEngineUtils.kt              # Helper utilities
└── README.md                      # Engine-specific docs
```

### Example 2: Adding New Command Action
**Location**: `/modules/managers/CommandsMGR/src/main/java/com/ai/actions/`
```
actions/
├── [existing actions...]
├── NewActions.kt                  # New action category
└── BaseAction.kt                  # Updated base class
```

### Example 3: Adding New Data Entity
**Location**: `/modules/managers/DataMGR/src/main/java/com/ai/entities/`
```
entities/
├── [existing entities...]
├── NewEntity.kt                   # New data entity
└── [corresponding repository in repository/ folder]
```

---

## Documentation Cross-References

### Module Documentation Locations:
- **SpeechRecognition**: `/modules/apps/SpeechRecognition/docs/`
- **VoiceAccessibility**: `/docs/PRD/PRD-ACCESSIBILITY.md`
- **CommandsMGR**: `/docs/PRD/PRD-COMMANDS.md`
- **DataMGR**: `/docs/PRD/PRD-DATA-COMPLETE.md`
- **Architecture**: `/docs/Architecture/VOS4-Final-Architecture.md`

### Related Documentation:
- Module specifications: `/docs/Modules/`
- Implementation plans: `/docs/Implementation/`
- Analysis reports: `/docs/Analysis/`

---

## Quick Reference Commands

### Navigation Shortcuts:
```bash
# Main application
cd VOS4/app/src/main/java/com/augmentalis/voiceos/

# Speech recognition
cd VOS4/modules/apps/SpeechRecognition/src/main/java/com/ai/

# Commands manager
cd VOS4/modules/managers/CommandsMGR/src/main/java/com/ai/

# Data manager  
cd VOS4/modules/managers/DataMGR/src/main/java/com/ai/

# UUID manager
cd VOS4/modules/libraries/UUIDManager/src/main/java/com/ai/uuidmgr/
```

### Structure Verification:
```bash
# Check module structure
tree VOS4/modules/apps/ -d -L 3

# Find all source files
find VOS4 -name "*.kt" | wc -l

# Check for missing components
find VOS4 -type d -name "src" -exec find {} -name "*.kt" \; | head -10
```

---

*END OF FILE STRUCTURE GUIDE*