# AVA-Spec-PluginDSLBuilder-50512-V1

**Feature:** Plugin System, DSL Rendering, iOS Builder Pattern
**Version:** 1.0
**Created:** 2025-12-05
**Status:** APPROVED FOR IMPLEMENTATION
**Mode:** YOLO

---

## Executive Summary

Implement three critical missing features in AvaMagicUI:
1. **Plugin System** - JSON/YAML plugin loading (11 NotImplementedError stubs)
2. **DSL Rendering** - 32 component render() methods with TODO
3. **iOS Builder Pattern** - 5 builder mappers returning empty children

**Total Effort:** 9-13 days
**Priority:** P0 (Blocks plugin-based UI)

---

## Problem Statement

| Issue | Current State | Impact |
|-------|---------------|--------|
| Plugin Loading | All parsers throw `NotImplementedError` | Cannot load dynamic UI plugins |
| DSL render() | 32 components have `TODO()` | DSL-created components don't render |
| iOS Builders | 5 mappers return `emptyList()` | ListView.builder etc. show nothing |

---

## Functional Requirements

### FR-1: JSON Plugin Parser

| Requirement | Description |
|-------------|-------------|
| FR-1.1 | Parse plugin metadata from JSON |
| FR-1.2 | Deserialize component definitions |
| FR-1.3 | Validate against SecuritySandbox |
| FR-1.4 | Register components with ComponentRegistry |

**Data Model:**
```kotlin
@Serializable
data class PluginJsonFormat(
    val metadata: PluginMetadata,
    val components: List<ComponentJsonDef>,
    val themes: List<ThemeJsonDef>? = null
)

@Serializable
data class ComponentJsonDef(
    val type: String,
    val schema: ComponentSchema? = null,
    val defaultProps: Map<String, JsonElement>? = null
)
```

### FR-2: YAML Plugin Parser

| Requirement | Description |
|-------------|-------------|
| FR-2.1 | Reuse existing YamlParser patterns |
| FR-2.2 | Convert YAML to JSON structure |
| FR-2.3 | Support component + theme YAML |
| FR-2.4 | Handle multi-document YAML |

### FR-3: Platform File Loading

| Platform | Requirement |
|----------|-------------|
| Android | Use `java.io.File` + `BufferedReader` |
| iOS | Use Foundation `FileManager` via interop |
| JVM/Desktop | Use `java.nio.file.Files` |
| All | Add SHA256 validation |

### FR-4: DSL Render Delegates

| Requirement | Description |
|-------------|-------------|
| FR-4.1 | Update 32 components with render() delegate |
| FR-4.2 | Pattern: `renderer.render(this)` |
| FR-4.3 | Update platform renderers with DSL type cases |
| FR-4.4 | Add deprecation notices to DSL |

### FR-5: iOS Callback Registry

| Requirement | Description |
|-------------|-------------|
| FR-5.1 | Create `ItemBuilderRegistry` interface |
| FR-5.2 | Store builder callbacks by String ID |
| FR-5.3 | Resolve callbacks at mapper time |
| FR-5.4 | Inject registry into SwiftUIRenderer |

### FR-6: iOS Builder Mappers

| Mapper | Requirement |
|--------|-------------|
| ListViewBuilder | Materialize items via registry |
| GridViewBuilder | Same + handle grid delegate |
| ListViewSeparated | Interleave items + separators |
| ReorderableListView | Items + reorder gesture |
| AnimatedList | Items + animation config |

---

## Technical Constraints

| Constraint | Requirement |
|------------|-------------|
| Serialization | kotlinx.serialization 1.6.0 |
| Coroutines | 1.7.3 (async loading) |
| Security | Existing SecuritySandbox integration |
| Thread Safety | ComponentRegistry already thread-safe |

---

## Implementation Plan

### Phase 1: JSON Plugin Parser (2 days)

**Files to Create:**
```
Core/src/commonMain/kotlin/com/augmentalis/magicelements/core/runtime/
├── JsonPluginParser.kt      # NEW
├── PluginJsonFormat.kt      # NEW (data models)
└── PluginLoader.kt          # UPDATE (remove stubs)
```

**Tasks:**
1. Create `PluginJsonFormat` serializable data classes
2. Implement `JsonPluginParser.parsePlugin(json: String): MagicElementPlugin`
3. Update `PluginLoader.parseJsonPlugin()` to use new parser
4. Add unit tests

### Phase 2: YAML Plugin Parser (1 day)

**Files to Update:**
```
Core/src/commonMain/kotlin/com/augmentalis/magicelements/core/runtime/
└── YamlPluginParser.kt      # NEW (reuses YamlParser)
```

**Tasks:**
1. Create `YamlPluginParser` wrapper class
2. Leverage existing `YamlParser.kt` patterns
3. Convert YAML → JSON → Plugin
4. Update `PluginLoader.parseYamlPlugin()`

### Phase 3: Platform File Loading (2 days)

**Files to Update:**
```
Core/src/androidMain/kotlin/.../PlatformPluginLoader.kt   # UPDATE
Core/src/iosMain/kotlin/.../PlatformPluginLoader.kt       # UPDATE
Core/src/jvmMain/kotlin/.../PlatformPluginLoader.kt       # UPDATE
Core/src/desktopMain/kotlin/.../DesktopActuals.kt         # UPDATE
```

**Tasks per Platform:**
1. Implement `expect_loadPluginFromFile()` with file reading
2. Add SHA256 checksum validation
3. Parse using JSON/YAML parser
4. Return validated plugin

### Phase 4: DSL Render Delegates (0.5 days)

**Files to Update:**
```
Core/src/commonMain/kotlin/com/augmentalis/magicelements/dsl/
└── Components.kt            # UPDATE 32 render() methods
```

**Pattern:**
```kotlin
// Before (32 instances)
override fun render(renderer: Renderer): Any {
    TODO("Platform rendering not yet implemented")
}

// After
override fun render(renderer: Renderer): Any {
    return renderer.render(this)
}
```

### Phase 5: iOS Callback Registry (1 day)

**Files to Create:**
```
Renderers/iOS/src/iosMain/kotlin/com/augmentalis/magicelements/renderer/ios/
├── registry/
│   ├── ItemBuilderRegistry.kt       # NEW
│   └── DefaultItemBuilderRegistry.kt # NEW
└── SwiftUIRenderer.kt               # UPDATE (inject registry)
```

**Interface:**
```kotlin
interface ItemBuilderRegistry {
    fun registerBuilder(id: String, builder: (Int) -> Component)
    fun registerSeparator(id: String, separator: (Int) -> Component)
    fun resolveBuilder(id: String, index: Int): Component?
    fun resolveSeparator(id: String, index: Int): Component?
}
```

### Phase 6: iOS Builder Mappers (2 days)

**Files to Update:**
```
Renderers/iOS/src/iosMain/kotlin/.../mappers/
└── Scroll.kt                # UPDATE all 5 mappers
```

**Updated Mapper Pattern:**
```kotlin
object ListViewBuilderMapper {
    fun map(
        component: ListViewBuilderComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView,
        registry: ItemBuilderRegistry
    ): SwiftUIView {
        val itemCount = component.itemCount ?: 100

        val children = (0 until itemCount).mapNotNull { index ->
            registry.resolveBuilder(component.itemBuilder, index)
                ?.let { renderChild(it) }
        }

        return SwiftUIView(
            type = ViewType.Custom("ScrollView"),
            children = listOf(
                SwiftUIView(
                    type = ViewType.Custom("LazyVStack"),
                    children = children
                )
            )
        )
    }
}
```

### Phase 7: Remote Plugin Loading (2 days)

**Files to Create/Update:**
```
Core/src/commonMain/kotlin/.../PluginDownloader.kt    # NEW
Core/src/androidMain/kotlin/.../PlatformPluginLoader.kt
Core/src/iosMain/kotlin/.../PlatformPluginLoader.kt
Core/src/jvmMain/kotlin/.../PlatformPluginLoader.kt
```

**Features:**
1. HTTP download with progress
2. Local caching to app directory
3. Cache invalidation by version
4. Retry logic (3 attempts)
5. Timeout handling (30s)

### Phase 8: Testing (1.5 days)

**Test Files:**
```
Core/src/commonTest/kotlin/.../JsonPluginParserTest.kt   # NEW
Core/src/commonTest/kotlin/.../YamlPluginParserTest.kt   # NEW
Renderers/iOS/src/iosTest/kotlin/.../BuilderMapperTest.kt # NEW
```

---

## Swarm Agent Assignment

| Agent | Tasks | Files |
|-------|-------|-------|
| **Agent 1: JSON Parser** | Phase 1 | JsonPluginParser.kt, PluginJsonFormat.kt |
| **Agent 2: YAML Parser** | Phase 2 | YamlPluginParser.kt |
| **Agent 3: Android Loader** | Phase 3a | PlatformPluginLoader.kt (Android) |
| **Agent 4: iOS Loader** | Phase 3b | PlatformPluginLoader.kt (iOS) |
| **Agent 5: DSL Delegates** | Phase 4 | Components.kt (32 methods) |
| **Agent 6: Callback Registry** | Phase 5 | ItemBuilderRegistry.kt, SwiftUIRenderer.kt |
| **Agent 7: Builder Mappers** | Phase 6 | Scroll.kt (5 mappers) |
| **Agent 8: Remote Loading** | Phase 7 | PluginDownloader.kt, all platforms |

---

## Success Criteria

| Criterion | Verification |
|-----------|--------------|
| JSON plugin loads without error | Unit test passes |
| YAML plugin loads without error | Unit test passes |
| File plugin loads on Android | Integration test |
| File plugin loads on iOS | Integration test |
| DSL components render | Visual verification |
| ListView.builder shows items | iOS simulator test |
| GridView.builder shows grid | iOS simulator test |
| Remote plugin downloads | Network test |
| Security validation works | Security test |

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| iOS interop complexity | Use existing Foundation bindings |
| Kotlin-Swift callback bridge | Materialize items pre-render |
| Large list performance | Limit itemCount, add pagination |
| Plugin security | Existing SecuritySandbox handles |

---

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| kotlinx-serialization-json | 1.6.0 | JSON parsing |
| kotlinx-coroutines | 1.7.3 | Async loading |
| okhttp (Android) | 4.x | HTTP downloads |
| ktor-client (iOS) | 2.x | HTTP downloads |

---

## Timeline

| Phase | Duration | Parallel |
|-------|----------|----------|
| Phase 1-2: Parsers | 3 days | Yes |
| Phase 3: File Loading | 2 days | Yes |
| Phase 4: DSL Delegates | 0.5 days | No |
| Phase 5-6: iOS Registry | 3 days | Yes |
| Phase 7: Remote Loading | 2 days | Yes |
| Phase 8: Testing | 1.5 days | No |
| **Total** | **9-13 days** | - |

---

**Approved By:** Engineering Team
**Implementation Mode:** YOLO SWARM
