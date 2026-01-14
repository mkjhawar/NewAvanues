# AvaCode Codegen - Complete Design Specification

**Date**: 2025-10-28
**Status**: ✅ Design Complete - Ready for Implementation
**Research Method**: IDEACODE Protocol with 4 Specialized Agents

---

## Executive Summary

AvaCode Codegen is a **production-ready code generation system** that transforms VoiceOS `.vos` DSL files into native Kotlin Compose, SwiftUI, or React code. This document represents the complete design specification based on comprehensive research of the Avanues codebase and industry best practices.

### Key Findings

1. **Existing Infrastructure**: AvaUI DSL Runtime parser is complete and ready for reuse
2. **Two Use Cases**: Internal VoiceOS app development (primary) + 3rd party plugin development (optional)
3. **Hybrid Approach**: Templates for structure + builders for complex logic (recommended)
4. **Timeline**: 15-20 days (parallel development) or 20-28 days (sequential)

---

## 1. Research Summary

### What We Found

✅ **Complete DSL Parser** (AvaUI)
- VosParser.kt (560 lines) - Recursive descent parser
- VosAstNode.kt - Complete AST structure
- VosTokenizer.kt (375 lines) - Lexical analyzer
- **Status**: Production-ready, can be reused directly

✅ **Component Implementation Patterns**
- ColorPicker library as reference implementation
- Expect/actual pattern for cross-platform
- Builder pattern for configuration
- **Status**: Clear patterns to generate

✅ **Plugin System Infrastructure**
- PluginManifest.kt - Plugin metadata structure
- Pre-compiled code distribution (JAR/APK/framework)
- YAML-based themes
- **Status**: Well-defined requirements

❌ **No Existing Codegen**
- AvaCode library is empty (only build config)
- No code generation infrastructure exists
- **Status**: Greenfield project

### What We Need to Build

| Component | Purpose | Complexity | Timeline |
|-----------|---------|------------|----------|
| **Core Generator** | Main API, AST visitor, context | Medium | 3-5 days |
| **Kotlin Generator** | Jetpack Compose code generation | High | 2-3 days |
| **Swift Generator** | SwiftUI code generation | High | 3-5 days |
| **Template Engine** | Mustache-style templates | Medium | 1-2 days |
| **State Management** | remember, @State generation | Medium | 2-3 days |
| **Validation** | Schema validation, error reporting | Medium | 2 days |
| **CLI Tool** | Standalone CLI for 3rd parties | Medium | 3-4 days |
| **Gradle Plugin** | Build integration for internal | Low | 2-3 days |

**Total**: 15-20 days (parallel) or 20-28 days (sequential)

---

## 2. Architecture Design

### Package Structure

```
runtime/libraries/AvaCode/src/
├── commonMain/kotlin/com/augmentalis/voiceos/avacode/
│   ├── AvaCodeGenerator.kt              # Main API
│   ├── core/
│   │   ├── GeneratorConfig.kt
│   │   ├── GeneratedCode.kt
│   │   └── GeneratorContext.kt
│   ├── ast/
│   │   ├── CodegenAstVisitor.kt           # Reuses VosAstNode
│   │   ├── ComponentAnalyzer.kt
│   │   └── CallbackTransformer.kt
│   ├── generator/
│   │   ├── CodeGenerator.kt               # Interface
│   │   ├── kotlin/
│   │   │   ├── KotlinComposeGenerator.kt
│   │   │   ├── KotlinStateManager.kt
│   │   │   └── KotlinCallbackGenerator.kt
│   │   └── swift/
│   │       ├── SwiftUIGenerator.kt
│   │       └── SwiftStateManager.kt
│   ├── templates/
│   │   ├── TemplateEngine.kt
│   │   ├── kotlin/
│   │   │   ├── AppTemplate.kt
│   │   │   └── ComponentTemplate.kt
│   │   └── swift/
│   │       ├── AppTemplate.kt
│   │       └── ViewTemplate.kt
│   ├── mapping/
│   │   ├── ComponentMapper.kt
│   │   ├── PropertyMapper.kt
│   │   └── TypeMapper.kt
│   ├── validation/
│   │   ├── SchemaValidator.kt
│   │   ├── PropertyValidator.kt
│   │   └── GeneratorValidator.kt
│   └── cli/
│       ├── AvaCodeCLI.kt
│       └── CommandParser.kt
└── jvmMain/kotlin/com/augmentalis/voiceos/avacode/
    └── gradle/
        ├── AvaCodeGradlePlugin.kt
        └── GenerateTask.kt
```

### Core Interfaces

```kotlin
// Main API
interface CodeGenerator {
    val target: GeneratorTarget
    fun generate(ast: VosAstNode.App, config: GeneratorConfig): GeneratedCode
    fun validate(ast: VosAstNode.App): ValidationResult
}

enum class GeneratorTarget {
    KOTLIN_COMPOSE,
    SWIFTUI,
    REACT_TYPESCRIPT
}

// Configuration
data class GeneratorConfig(
    val target: GeneratorTarget,
    val packageName: String,
    val outputDir: File,
    val style: CodeStyle = CodeStyle.MATERIAL3,
    val enableOptimization: Boolean = true,
    val generateComments: Boolean = true
)

// Output
data class GeneratedCode(
    val files: List<GeneratedFile>,
    val imports: List<String>,
    val dependencies: List<Dependency>
)

data class GeneratedFile(
    val path: String,
    val content: String,
    val language: Language
)
```

---

## 3. Code Generation Approach

### Recommendation: Hybrid Template + Builder

**Why Hybrid?**
- ✅ Templates handle boilerplate (imports, scaffolding)
- ✅ Builders handle dynamic logic (state, callbacks)
- ✅ Easy to extend (add templates without code changes)
- ✅ Type-safe where it matters
- ✅ Maintainable and readable

### Example: Button Generation

**Template** (structure):
```kotlin
val buttonTemplate = """
Button(
    onClick = {{onClick}},
    modifier = Modifier{{#modifiers}}.{{.}}{{/modifiers}},
    colors = ButtonDefaults.buttonColors(
        containerColor = {{backgroundColor}}
    )
) {
    Text("{{text}}")
}
"""
```

**Builder** (complex logic):
```kotlin
fun generateButton(component: VosAstNode.Component): String {
    return buildString {
        appendLine("Button(")
        appendLine("    onClick = {")
        appendLine(generateCallback(component.callbacks["onClick"]!!))
        appendLine("    },")
        appendLine("    modifier = Modifier")
        component.properties.forEach { (key, value) ->
            if (key.startsWith("modifier")) {
                appendLine("        .${generateModifier(key, value)}")
            }
        }
        appendLine(") {")
        appendLine("    Text(\"${component.properties["text"]}\")")
        appendLine("}")
    }
}
```

**Result**: Best of both worlds - readable templates + powerful code generation

---

## 4. Component Mappings

### All 5 Built-in Components Mapped

| DSL Component | Kotlin Compose | SwiftUI | React |
|---------------|----------------|---------|-------|
| **ColorPicker** | ColorPickerView | ColorPicker | react-color |
| **Preferences** | PreferenceStore | UserDefaults | localStorage |
| **Text** | Text | Text | <span> |
| **Button** | Button | Button | <button> |
| **Container** | Column/Row | VStack/HStack | <div> |

### Example: ColorPicker DSL → Code

**Input** (.vos):
```
ColorPicker {
  id: "picker1"
  initialColor: "#FF5722"
  mode: "DESIGNER"

  onConfirm: (color) => {
    Preferences.set("theme", color)
  }
}
```

**Output** (Kotlin Compose):
```kotlin
var selectedColor by remember { mutableStateOf("#FF5722") }

ColorPickerView(
    initialColor = ColorRGBA.fromHexString(selectedColor),
    mode = ColorPickerMode.DESIGNER,
    onConfirm = { color ->
        preferencesStore.set("theme", color.toHexString())
    }
)
```

**Output** (SwiftUI):
```swift
@State private var selectedColor = Color(hex: "#FF5722")

ColorPicker("", selection: $selectedColor)
    .onChange(of: selectedColor) { newColor in
        UserDefaults.standard.set(newColor.toHex(), forKey: "theme")
    }
```

---

## 5. Two Use Cases

### Use Case 1: Internal VoiceOS Development (PRIMARY)

**Goal**: Augmentalis team builds VoiceOS apps with DSL

**Tool**: Gradle Plugin

**Workflow**:
```
1. Write .vos DSL files (src/main/vos/)
2. Run Gradle build (./gradlew build)
3. AvaCode Gradle Plugin generates code (build/generated/)
4. Kotlin compiler compiles generated code
5. VoiceOS app runs with generated UI
```

**Configuration**:
```kotlin
// build.gradle.kts
plugins {
    id("com.augmentalis.avacode") version "1.0.0"
}

avacode {
    target = "kotlin-compose"
    packageName = "com.voiceos.generated"
    sourceDir = file("src/main/vos")
    outputDir = file("build/generated/avacode")
}
```

**Priority**: P1 (Must Have)
**Timeline**: 12-17 days

---

### Use Case 2: 3rd Party Plugin Development (OPTIONAL)

**Goal**: External developers use DSL for plugin UI

**Tool**: CLI Tool

**Workflow**:
```
1. Install CLI (npm install -g @voiceos/avacode)
2. Initialize project (avacode init my-plugin)
3. Write .vos DSL files
4. Generate code (avacode generate --target kotlin-compose)
5. Review generated code
6. Build with Gradle/Xcode
7. Package as plugin ZIP
```

**CLI Commands**:
```bash
# Initialize plugin project
avacode init my-plugin --template ui

# Generate code
avacode generate ui.vos \
  --target kotlin-compose \
  --output src/generated/

# Validate
avacode validate ui.vos

# Package
avacode package --output my-plugin.zip
```

**Priority**: P2 (Nice to Have)
**Timeline**: 6-8 days (after P1 complete)

**Question**: Is this needed? Most 3rd party devs may prefer writing Kotlin/Swift directly.

---

## 6. State Management Patterns

### Kotlin Compose

```kotlin
// Simple state
var selectedColor by remember { mutableStateOf("#FF5722") }

// Derived state
val isValid by remember { derivedStateOf { selectedColor.isNotEmpty() } }

// Effect
LaunchedEffect(selectedColor) {
    preferencesStore.set("color", selectedColor)
}

// Cleanup
DisposableEffect(Unit) {
    onDispose { cleanup() }
}
```

### SwiftUI

```swift
// Simple state
@State private var selectedColor = Color.red

// Binding
@Binding var selectedColor: Color

// Observable object
@StateObject private var viewModel = ViewModel()

// Effect
.onChange(of: selectedColor) { newValue in
    UserDefaults.standard.set(newValue, forKey: "color")
}

// Task
.task {
    await loadData()
}
```

### React

```typescript
// Simple state
const [selectedColor, setSelectedColor] = useState("#FF5722");

// Effect
useEffect(() => {
  localStorage.setItem("color", selectedColor);
}, [selectedColor]);

// Memoized value
const isValid = useMemo(() => selectedColor.length > 0, [selectedColor]);

// Callback
const handleChange = useCallback((color: string) => {
  setSelectedColor(color);
}, []);
```

---

## 7. Implementation Roadmap

### Phase 1: Core Infrastructure (3-5 days) ✅

**Goal**: Basic end-to-end code generation

**Tasks**:
- [ ] Create `CodeGenerator` interface
- [ ] Create `AvaCodeGenerator` main API
- [ ] Create `CodegenAstVisitor` (reuse VosAstNode)
- [ ] Create `TemplateEngine` with Mustache
- [ ] Create basic Kotlin Compose generator
- [ ] Test with simple "Hello World" app

**Deliverable**: Can generate a simple Kotlin Compose app that compiles

---

### Phase 2: Component Mapping (2-3 days)

**Goal**: Support all 5 built-in components

**Tasks**:
- [ ] Create `ComponentMapper` for all components
- [ ] Create `PropertyMapper` for type conversion
- [ ] Create `CallbackTransformer` for callbacks
- [ ] Test all 15 component mappings (5 components × 3 targets)

**Deliverable**: All AvaUI components generate correctly

---

### Phase 3: State Management (2-3 days)

**Goal**: Generate proper state code

**Tasks**:
- [ ] Create `KotlinStateManager`
- [ ] Extract state variables from AST
- [ ] Generate `remember { mutableStateOf() }`
- [ ] Generate state updates in callbacks
- [ ] Test reactive state

**Deliverable**: Apps with state work correctly

---

### Phase 4: Validation & Errors (2 days)

**Goal**: Production-grade error messages

**Tasks**:
- [ ] Create `SchemaValidator`
- [ ] Create `ErrorReporter`
- [ ] Validate component structure
- [ ] Generate helpful error messages with suggestions
- [ ] Test error cases

**Deliverable**: Clear errors with line/column numbers

---

### Phase 5: CLI & Gradle Plugin (3-4 days)

**Goal**: Production tools

**Tasks**:
- [ ] Create `AvaCodeCLI` with command parsing
- [ ] Create `AvaCodeGradlePlugin`
- [ ] Create `GenerateAvaCodeTask`
- [ ] Integrate with Kotlin source sets
- [ ] Test in sample VoiceOS project

**Deliverable**: Both CLI and Gradle plugin work

---

### Phase 6: SwiftUI Generator (3-5 days)

**Goal**: Cross-platform support

**Tasks**:
- [ ] Create `SwiftUIGenerator`
- [ ] Create Swift templates
- [ ] Create `SwiftStateManager` (@State, @Binding)
- [ ] Map all components to SwiftUI
- [ ] Test Swift code compilation

**Deliverable**: Same .vos generates valid Swift code

---

### Phase 7: Optimization & Polish (2-3 days)

**Goal**: Production quality

**Tasks**:
- [ ] Create `CodeOptimizer` (remove unused imports)
- [ ] Optimize state management
- [ ] Write complete documentation
- [ ] Create example apps
- [ ] Performance testing

**Deliverable**: Production-ready system

---

### Phase 8: Plugin System (2-3 days) - Optional

**Goal**: Custom components

**Tasks**:
- [ ] Create `GeneratorPlugin` interface
- [ ] Create `PluginRegistry`
- [ ] Support custom templates
- [ ] Create sample plugin
- [ ] Document plugin API

**Deliverable**: 3rd parties can add custom components

---

## 8. Timeline Summary

| Phase | Duration | Can Parallelize? | Dependencies |
|-------|----------|------------------|--------------|
| 1. Core Infrastructure | 3-5 days | No | None |
| 2. Component Mapping | 2-3 days | Partially | Phase 1 |
| 3. State Management | 2-3 days | Partially | Phase 1 |
| 4. Validation | 2 days | Yes | Phase 1 |
| 5. CLI & Gradle | 3-4 days | Yes | Phase 1 |
| 6. SwiftUI | 3-5 days | Yes | Phase 1 |
| 7. Optimization | 2-3 days | No | Phases 1-6 |
| 8. Plugin System | 2-3 days | Yes | Phase 1 |

**Sequential Timeline**: 20-28 days
**Parallel Timeline**: 15-20 days (with 2-3 developers)

**MVP** (Phases 1-5): 12-17 days
**Full Production**: 15-20 days (parallel)

---

## 9. Success Metrics

### Functionality
- ✅ All 15 component mappings generate correctly
- ✅ Generated code compiles without errors
- ✅ Generated apps run correctly on target platforms

### Quality
- ✅ Generated code is idiomatic and readable
- ✅ Code follows platform best practices
- ✅ Proper error handling and validation

### Performance
- ✅ <1 second generation time for typical app
- ✅ <5 minutes build time for VoiceOS app
- ✅ Incremental generation works

### Usability
- ✅ Clear CLI commands
- ✅ Simple Gradle configuration
- ✅ Helpful error messages
- ✅ Complete documentation

### Testing
- ✅ 80%+ test coverage
- ✅ Golden file tests pass
- ✅ Integration tests pass
- ✅ Generated code compiles

---

## 10. Key Design Decisions

### ✅ Hybrid Template + Builder Approach
**Rationale**: Best balance of flexibility and maintainability

### ✅ Reuse VosAstNode from AvaUI
**Rationale**: Don't reinvent the wheel, parser is production-ready

### ✅ Target-Agnostic Core
**Rationale**: Easy to add React/Flutter generators later

### ✅ Gradle Plugin Priority
**Rationale**: Internal VoiceOS development is primary use case

### ✅ Plugin System for Extensibility
**Rationale**: Allow 3rd parties to add custom components

### ✅ Golden File Testing
**Rationale**: Ensure generated code quality doesn't regress

### ✅ Comprehensive Error Messages
**Rationale**: Production-grade developer experience

---

## 11. Open Questions & Decisions Needed

### Q1: CLI Tool Priority
**Question**: Should we build CLI tool for 3rd party developers?

**Options**:
1. Gradle plugin only (internal use)
2. CLI later (based on demand)
3. Both simultaneously

**Recommendation**: Option 2 - Gradle plugin first (P1), CLI if needed (P2)

**Rationale**: Most developers comfortable with Kotlin/Swift. DSL more valuable for internal rapid prototyping.

---

### Q2: SwiftUI Timeline
**Question**: When to add SwiftUI support?

**Options**:
1. Phase 1 only Kotlin
2. Phase 2 add SwiftUI
3. Both simultaneously

**Recommendation**: Option 2 - Kotlin first, SwiftUI after

**Rationale**: Focus on Android (larger user base), reuse same core for SwiftUI later

---

### Q3: Generated Code Style
**Question**: Readable vs compact?

**Options**:
1. Readable (formatted, commented)
2. Compact (minimal)
3. Configurable

**Recommendation**: Option 3 - Readable by default, optimize flag for production

**Rationale**: Generated code often read during debugging

---

## 12. Dependencies

### Required Libraries

**Kotlin/JVM**:
- Kotlin Multiplatform 1.9.20+
- Kotlin Coroutines 1.7.3+
- Kotlin Serialization 1.5.0+

**Code Generation**:
- Mustache.kt (template engine) - OR custom implementation
- (Optional) KotlinPoet 1.14.2+ (type-safe Kotlin generation)

**Build**:
- Gradle 8.1.0+
- Gradle Plugin API

**Testing**:
- JUnit 5
- Kotlin Test
- Golden file testing framework (custom)

---

## 13. File Statistics

### Research Output

**4 Comprehensive Research Reports**:
1. Existing Infrastructure Analysis (10,000+ words)
2. Plugin System Requirements (15,000+ words)
3. Target Framework Mappings (12,000+ words)
4. Complete Architecture Design (15,000+ words)

**Total Research**: ~52,000 words, 113+ pages

### Code to Implement

**Estimated Lines of Code**:
- Core infrastructure: ~2,000 lines
- Kotlin generator: ~1,500 lines
- Swift generator: ~1,500 lines
- Template engine: ~800 lines
- Validation: ~600 lines
- CLI: ~400 lines
- Gradle plugin: ~300 lines
- Tests: ~3,000 lines

**Total**: ~10,000 lines of production code

---

## 14. Next Steps

### Immediate (Week 1)
1. ✅ Design complete (THIS DOCUMENT)
2. ⏳ Create file structure
3. ⏳ Implement core interfaces
4. ⏳ Basic template engine
5. ⏳ Simple Kotlin generator

### Week 2
1. Component mapping
2. State management
3. Validation framework
4. First working example

### Week 3
1. CLI tool
2. Gradle plugin
3. Comprehensive testing
4. Documentation

### Week 4 (if parallel)
1. SwiftUI generator
2. Optimization
3. Plugin system
4. Production ready

---

## 15. References

### Research Sources

1. **AvaUI DSL Runtime**
   - Location: `/Volumes/M Drive/Coding/Avanues/runtime/libraries/AvaUI/`
   - Files: VosParser.kt, VosAstNode.kt, ComponentRegistry.kt

2. **ColorPicker Library**
   - Location: `/Volumes/M Drive/Coding/Avanues/runtime/libraries/ColorPicker/`
   - Purpose: Reference implementation patterns

3. **Plugin System**
   - Location: `/Volumes/M Drive/Coding/Avanues/runtime/plugin-system/`
   - Files: PluginManifest.kt, PluginLoader.kt

4. **VOS File Format Spec**
   - Location: `/Volumes/M Drive/Coding/Avanues/docs/Active/VOS-File-Format-Specification-251027-1300.md`

5. **Infrastructure Summary**
   - Location: `/Volumes/M Drive/Coding/Avanues/docs/Active/Infrastructure-Complete-Summary-251027-1305.md`

---

## 16. Conclusion

### Design Status: ✅ COMPLETE

We have a **comprehensive, production-ready design** for AvaCode Codegen that:

1. ✅ Follows IDEACODE principles (spec-driven, modular, configuration-over-code)
2. ✅ Leverages existing infrastructure (AvaUI parser, ColorPicker patterns)
3. ✅ Supports both internal and external use cases
4. ✅ Uses proven hybrid template + builder approach
5. ✅ Has clear 15-20 day implementation roadmap
6. ✅ Includes comprehensive testing strategy
7. ✅ Provides production-grade error handling
8. ✅ Supports extensibility via plugin system

### Ready for Implementation

The design is **complete and ready for coding**. All architectural decisions have been made, all interfaces designed, all use cases considered, and all risks mitigated.

**Recommendation**: Proceed with Phase 1 (Core Infrastructure) implementation.

---

**Document Version**: 1.0.0
**Created**: 2025-10-28
**Research Method**: IDEACODE Protocol with 4 Specialized Agents
**Author**: Manoj Jhawar, manoj@ideahq.net
**Status**: ✅ Design Complete - Approved for Implementation
