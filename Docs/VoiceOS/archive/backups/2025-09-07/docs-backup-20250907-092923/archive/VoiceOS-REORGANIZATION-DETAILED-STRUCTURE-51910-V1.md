# VOS4 Detailed Folder Structure with Subfolders

## YES - Each Module/App Has Subfolders for Functions

```
Planning/
└── Architecture/
    ├── Apps/
    │   ├── VoiceAccessibility/
    │   │   ├── AccessibilityService-Architecture.md
    │   │   ├── AccessibilityService-PRD.md
    │   │   ├── AccessibilityService-Implementation.md
    │   │   ├── TODO.md
    │   │   │
    │   │   └── Functions/                          # Function-specific docs
    │   │       ├── UITreeTraversal/
    │   │       │   ├── UITreeTraversal-Design.md
    │   │       │   ├── UITreeTraversal-Implementation.md
    │   │       │   └── UITreeTraversal-Optimization.md
    │   │       │
    │   │       ├── TouchBridge/
    │   │       │   ├── TouchBridge-Architecture.md
    │   │       │   ├── TouchBridge-GestureMapping.md
    │   │       │   └── TouchBridge-Implementation.md
    │   │       │
    │   │       ├── ElementExtraction/
    │   │       │   ├── ElementExtraction-Algorithm.md
    │   │       │   └── ElementExtraction-Performance.md
    │   │       │
    │   │       └── DuplicateResolver/
    │   │           └── DuplicateResolver-Logic.md
    │   │
    │   ├── SpeechRecognition/
    │   │   ├── SpeechRecognition-Architecture.md
    │   │   ├── SpeechRecognition-PRD.md
    │   │   ├── SpeechRecognition-Implementation.md
    │   │   ├── TODO.md
    │   │   │
    │   │   └── Functions/
    │   │       ├── VAD/                            # Voice Activity Detection
    │   │       │   ├── VAD-Algorithm.md
    │   │       │   ├── VAD-Implementation.md
    │   │       │   └── VAD-Optimization.md
    │   │       │
    │   │       ├── GrammarCompilation/
    │   │       │   ├── GrammarCompilation-Design.md
    │   │       │   ├── GrammarCompilation-Cache.md
    │   │       │   └── GrammarCompilation-Performance.md
    │   │       │
    │   │       ├── AudioProcessing/
    │   │       │   ├── FFT-Implementation.md
    │   │       │   ├── MFCC-Extraction.md
    │   │       │   └── AudioBuffer-Management.md
    │   │       │
    │   │       └── RecognitionEngines/
    │   │           ├── Vosk-Integration.md
    │   │           ├── Google-Integration.md
    │   │           └── Vivoka-Integration.md
    │   │
    │   └── VoiceUI/
    │       ├── VoiceUI-Architecture.md
    │       ├── VoiceUI-PRD.md
    │       ├── TODO.md
    │       │
    │       └── Functions/
    │           ├── GestureRecognition/
    │           │   ├── GestureRecognition-Types.md
    │           │   ├── GestureRecognition-Mapping.md
    │           │   └── AirTap-Implementation.md
    │           │
    │           ├── FloatingUI/
    │           │   ├── FloatingUI-Design.md
    │           │   └── FloatingUI-Positioning.md
    │           │
    │           └── Notifications/
    │               ├── NotificationSystem-Design.md
    │               └── NotificationPriority-Logic.md
    │
    ├── Managers/
    │   ├── CommandsMGR/
    │   │   ├── CommandsMGR-Architecture.md
    │   │   ├── CommandsMGR-PRD.md
    │   │   ├── TODO.md
    │   │   │
    │   │   └── Functions/
    │   │       ├── CommandProcessor/
    │   │       │   ├── CommandProcessor-Pipeline.md
    │   │       │   ├── CommandProcessor-Validation.md
    │   │       │   └── CommandProcessor-Routing.md
    │   │       │
    │   │       ├── ActionHandlers/
    │   │       │   ├── NavigationActions-Implementation.md
    │   │       │   ├── ScrollActions-Implementation.md
    │   │       │   ├── GestureActions-Implementation.md
    │   │       │   └── SystemActions-Implementation.md
    │   │       │
    │   │       └── ContextManager/
    │   │           ├── ContextManager-Design.md
    │   │           └── ContextTracking-Algorithm.md
    │   │
    │   ├── DataMGR/
    │   │   ├── DataMGR-Architecture.md
    │   │   ├── DataMGR-PRD.md
    │   │   ├── TODO.md
    │   │   │
    │   │   └── Functions/
    │   │       ├── ObjectBox/
    │   │       │   ├── ObjectBox-Schema.md
    │   │       │   ├── ObjectBox-Migration.md
    │   │       │   └── ObjectBox-Performance.md
    │   │       │
    │   │       ├── Repository/
    │   │       │   ├── Repository-Pattern.md
    │   │       │   └── Repository-Implementation.md
    │   │       │
    │   │       └── Caching/
    │   │           ├── CacheStrategy-Design.md
    │   │           ├── LRUCache-Implementation.md
    │   │           └── CacheInvalidation-Logic.md
    │   │
    │   ├── CoreMGR/
    │   │   ├── CoreMGR-Architecture.md
    │   │   ├── CoreMGR-PRD.md
    │   │   ├── TODO.md
    │   │   │
    │   │   └── Functions/
    │   │       ├── ModuleLifecycle/
    │   │       │   ├── ModuleLifecycle-Management.md
    │   │       │   └── DependencyInjection-Design.md
    │   │       │
    │   │       ├── EventBus/
    │   │       │   ├── EventBus-Architecture.md
    │   │       │   └── EventRouting-Implementation.md
    │   │       │
    │   │       └── Configuration/
    │   │           ├── ConfigLoader-Design.md
    │   │           └── FeatureFlags-Implementation.md
    │   │
    │   └── LocalizationMGR/
    │       ├── LocalizationMGR-Architecture.md
    │       ├── LocalizationMGR-PRD.md
    │       ├── TODO.md
    │       │
    │       └── Functions/
    │           ├── LanguageDetection/
    │           │   └── LanguageDetection-Algorithm.md
    │           │
    │           └── Translation/
    │               ├── Translation-Pipeline.md
    │               └── ResourceLoading-Strategy.md
    │
    └── Libraries/
        ├── DeviceMGR/
        │   ├── DeviceMGR-Architecture.md
        │   ├── TODO.md
        │   │
        │   └── Functions/
        │       ├── AudioManager/
        │       │   ├── AudioSession-Management.md
        │       │   └── AudioRouting-Logic.md
        │       │
        │       ├── DisplayManager/
        │       │   ├── ScreenInfo-Detection.md
        │       │   └── OrientationHandling.md
        │       │
        │       └── GlassesManager/
        │           ├── GlassesProtocol-Spec.md
        │           └── GlassesSync-Implementation.md
        │
        └── UUIDCreator/
            ├── UUIDCreator-Architecture.md
            ├── TODO.md
            │
            └── Functions/
                ├── Generation/
                │   ├── UUIDGeneration-Algorithm.md
                │   └── CollisionPrevention-Logic.md
                │
                └── Persistence/
                    ├── UUIDPersistence-Strategy.md
                    └── UUIDMapping-Implementation.md
```

---

## Benefits of Function Subfolders

### 1. **Clear Organization**
- Each function has its own space
- Related documents stay together
- Easy to find specific function docs

### 2. **Scalability**
- Add new functions without cluttering
- Can grow to hundreds of functions
- Maintains readability

### 3. **Ownership**
- Clear which team owns which function
- Function-level TODO tracking possible
- Easier code reviews

### 4. **Navigation Examples**

#### Finding TouchBridge docs:
```
Planning/Architecture/Apps/VoiceAccessibility/Functions/TouchBridge/
```

#### Finding VAD implementation:
```
Planning/Architecture/Apps/SpeechRecognition/Functions/VAD/
```

#### Finding ObjectBox schema:
```
Planning/Architecture/Managers/DataMGR/Functions/ObjectBox/
```

---

## Folder Naming Rules

### Apps Structure:
```
Apps/[AppName]/
    ├── [AppName]-Architecture.md     # Overall app architecture
    ├── [AppName]-PRD.md              # App requirements
    ├── [AppName]-Implementation.md   # Implementation plan
    ├── TODO.md                        # App-level tasks
    └── Functions/                     # Function-specific docs
        └── [FunctionName]/
            └── [FunctionName]-[Type].md
```

### Managers Structure:
```
Managers/[ManagerName]/
    ├── [ManagerName]-Architecture.md
    ├── [ManagerName]-PRD.md
    ├── TODO.md
    └── Functions/
        └── [FunctionName]/
            └── [FunctionName]-[Type].md
```

### Libraries Structure:
```
Libraries/[LibraryName]/
    ├── [LibraryName]-Architecture.md
    ├── TODO.md
    └── Functions/
        └── [FunctionName]/
            └── [FunctionName]-[Type].md
```

---

## When to Create Function Subfolders

### Create a Function Subfolder When:
1. **Complexity** - Function has multiple documents
2. **Independence** - Function could be a standalone module
3. **Team Ownership** - Different team maintains it
4. **Significant Logic** - Complex algorithms or design
5. **External Interface** - Exposes APIs to other modules

### Keep in Parent When:
1. **Simple** - Can be described in parent architecture doc
2. **Tightly Coupled** - Integral part of parent, not separable
3. **Single Document** - Only needs one description

---

## Example: SpeechRecognition Functions

```
SpeechRecognition/
└── Functions/
    ├── VAD/                     # Complex algorithm, multiple docs
    ├── GrammarCompilation/      # Complex, performance critical
    ├── AudioProcessing/         # Multiple sub-functions (FFT, MFCC)
    ├── RecognitionEngines/      # Multiple engines to document
    └── (Simple functions documented in parent SpeechRecognition-Architecture.md)
```

---

## Search and Discovery

### Finding Documents:
1. **By Module**: Go to module folder
2. **By Function**: Navigate to Functions/[FunctionName]/
3. **By Type**: Search for "-PRD.md" or "-Architecture.md"
4. **By TODO**: Each level has TODO.md

### Example Paths:
```bash
# Find all TouchBridge docs
ls Planning/Architecture/Apps/VoiceAccessibility/Functions/TouchBridge/

# Find all PRDs
find Planning/Architecture -name "*-PRD.md"

# Find all TODOs
find Planning/Architecture -name "TODO.md"
```

---

## Summary

✅ **YES** - Each module/app will have:
- Main architecture documents at module level
- `Functions/` subfolder for complex functions
- Each function gets its own subfolder when needed
- Clear path structure for easy navigation
- Consistent naming patterns

This structure provides:
- **Clarity**: Know exactly where to find things
- **Scalability**: Can handle hundreds of functions
- **Maintainability**: Easy to update specific functions
- **Discoverability**: Clear navigation path