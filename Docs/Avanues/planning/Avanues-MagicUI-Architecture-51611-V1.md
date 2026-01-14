# AvaUI Architecture Overview

**Version**: 3.1.0
**Feature**: 002-avaui-uik-enhancements
**Last Updated**: 2025-10-27

## System Overview

AvaUI is a comprehensive UI component framework for AvanueRT with DSL-first layout capabilities, theme management, and cross-platform support. It enables declarative UI creation through compact DSL syntax, YAML/JSON configurations, and programmatic composition.

## Architecture Layers

### 1. Core Layer (`core/`)

**Purpose**: Foundational abstractions and interfaces

**Key Components**:
- `ComponentModel`: Core data structure representing any UI element
- `PluginComponent`: Interface for extensible components
- `PluginRegistry`: Central registry for component management
- `NamespacedUUID`: Unique component identifiers (`namespace/local-id`)
- `ComponentPosition`: 3D positioning (x, y, z)

**Design Principles**:
- Interface-based design for extensibility
- Zero `!!` (null assertion) operators
- Immutable data structures where possible

### 2. DSL Layer (`dsl/`)

**Purpose**: Ultra-compact layout definition language

**Performance**: 78,000+ layouts/second, <0.02ms per layout

**Key Components**:
- `DslLayoutEngine`: Main API integrating tokenizer + parser + templates
- `LayoutTokenizer`: Lexer (150 LOC)
- `LayoutParser`: Recursive descent parser (250 LOC)
- `LayoutAST`: Abstract syntax tree (sealed classes)
- `TemplateRegistry`: 50+ predefined templates
- `DslValidator`: Syntax validation with error recovery
- `DslFormatter`: Pretty-print DSL
- `DslErrors`: Rich error messages with suggestions

**DSL Syntax**:
```kotlin
// Simple row with buttons
"row:button[text=Save],button[text=Cancel]"

// Nested layout with properties
"column[gap=16]:text[value=Title,size=24],row:button[text=OK]"

// Template expansion
"template:simple_form"  // Expands to full form layout
```

**App Store Compliance**:
- DSL treated as declarative DATA (not executable code)
- All parsers bundled in app binary (no remote code loading)
- Compliant with iOS Section 2.5.2 and Google Play dynamic code policies

### 3. Layout Layer (`layout/`)

**Purpose**: Multi-format layout loading and management

**Key Components**:
- `UnifiedLayoutLoader`: Auto-detects YAML, JSON, or DSL format
- `YamlLayoutParser`: YAML parser (kaml library)
- `JsonLayoutParser`: JSON parser (kotlinx.serialization)
- `LayoutSchemaValidator`: JSON schema validation
- `LiveLayoutRegistry`: Active layout tracking with dirty state
- `LayoutTemplate`: Template system

**Format Decision Matrix**:
- **DSL**: Voice commands, quick prototyping, compact code
- **YAML**: Complex layouts, version control, human-readable
- **JSON**: API responses, tool integration, schema validation

### 4. Theme Layer (`theme/`)

**Purpose**: Centralized visual styling with overrides

**Key Components**:
- `ThemeManager`: Singleton for theme application
- `ThemeConfig`: Theme definition (palette, typography, spacing, effects)
- `ThemePalette`: Color definitions (primary, secondary, background, etc.)
- `ThemeTypography`: Text styles (h1, h2, body, caption)
- `SmartStylingEngine`: Per-widget style overrides

**Predefined Themes**:
- **Glass**: AR/VR optimized with glass morphism
- **Dark**: OLED-friendly dark mode
- **Light**: Standard light mode
- **ARVision**: Apple Vision Pro inspired

**Performance**: <200ms to apply theme to 100+ components

### 5. IMU Layer (`imu/`)

**Purpose**: Composable motion processing pipeline for hands-free interaction

**Key Components**:
- `IMUBridge`: Main API for sensor integration
- `MotionProcessorPipeline`: Chain of motion processors
- `MotionProcessor`: Base interface (AxisLocker, RateLimiter, Smoother)
- `AxisLockerProcessor`: Independent pitch/roll/yaw locking
- `RateLimiterProcessor`: Acceleration/deceleration curves
- `MotionSmootherProcessor`: Smoothing algorithms (Exponential, One Euro, Moving Average)
- `IMUPresets`: AR Mode, VR Mode, Accessibility Mode
- `IMUSettings`: Persistent configuration

**Performance**: 60 FPS processing (16.67ms budget)

### 6. Database Layer (`database/`)

**Purpose**: Room KMP persistence for components, layouts, themes, IMU configs

**Key Components**:
- `AvaUIDatabase`: Room database definition
- **Entities**: ComponentEntity, LayoutScenarioEntity, ThemeConfigEntity, IMUConfigEntity
- **DAOs**: ComponentDao, LayoutScenarioDao, ThemeConfigDao, IMUConfigDao
- **Converters**: PropertiesConverter, PositionConverter, ThemeDataConverter

**Performance**: <200ms for 1000+ component operations

**Migration Strategy**: Automated schema updates with version tracking

### 7. Export Layer (`export/`)

**Purpose**: Code generation and layout serialization

**Key Components**:
- `DslCodeGenerator`: Primary - generate DSL from layouts
- `KotlinCodeGenerator`: Generate Kotlin code
- `LayoutScenarioJsonHandler`: JSON export/import
- `LayoutSnapshotGenerator`: SVG/PNG/JPEG export with metadata
- `SvgMetadata`: Component metadata in SVG for AI processing

**Use Cases**:
- Designer → Developer workflow
- AI-assisted design tools
- Layout documentation
- Cross-tool integration

### 8. Inspector Layer (`inspector/`)

**Purpose**: Property editing for design tools

**Key Components**:
- `PropertyInspector`: Generic property editor for any ComponentModel
- `PropertyInspectorPanel`: Enhanced panel with live preview
- `LivePreview`: Real-time component rendering

**Features**:
- Separation of system vs custom properties
- Theme palette integration
- Real-time preview at 30 FPS

### 9. Canvas Layer (`canvas/`)

**Purpose**: Visual design tools

**Key Components**:
- `VoiceOSCanvas`: Design surface with grid, selection, component rendering
- `DraggableComponent`: Drag-drop with boundary constraints and snapping
- `UndoRedoManager`: Undo/redo stack (50 operations default)
- `LayoutCritiqueEngine`: Automated quality analysis (optional P4)

## Cross-Cutting Concerns

### Error Handling
- `Result<T>` pattern for operations that can fail
- Rich error messages with context and suggestions
- Graceful degradation for non-critical failures

### Logging
- KMP-compatible logger interface
- Structured logging with context
- Performance metrics collection

### Testing Strategy
- **Unit Tests**: 80%+ coverage for P1 features
- **Integration Tests**: Cross-layer validation
- **Performance Tests**: Benchmark against targets
- **Contract Tests**: API compliance verification

## Data Flow

### DSL Layout → Component Rendering

```
1. User Input: "row:button[text=Save],button[text=Cancel]"
2. DslLayoutEngine.parse()
3. LayoutTokenizer → Token stream
4. LayoutParser → LayoutAST
5. AST → ComponentModel instances
6. PluginRegistry.register(components)
7. ThemeManager.apply(components)
8. Render to screen
```

### Multi-Format Layout Loading

```
1. UnifiedLayoutLoader.load(file, LayoutFormat.AUTO)
2. Auto-detect format (YAML/JSON/DSL)
3. Route to appropriate parser
4. LayoutSchemaValidator.validate()
5. Parse → ComponentModel instances
6. LiveLayoutRegistry.register(layout)
7. Render
```

## Platform-Specific Implementations

### Common Main (`commonMain/`)
- All core logic, data structures, interfaces
- Platform-agnostic implementations

### Android (`androidMain/`)
- `AndroidSensorBridge`: SensorManager integration for IMU
- `DatabaseBuilder`: Android-specific Room configuration

### iOS (`iosMain/`)
- `IOSSensorBridge`: CoreMotion integration for IMU
- `DatabaseBuilder`: iOS-specific Room configuration

### JVM (`jvmMain/`)
- `DatabaseBuilder`: JVM-specific Room configuration

### JS/Web (`jsMain/`)
- `BrowserDatabaseAdapter`: IndexedDB wrapper
- `WebComponentRenderer`: DOM rendering
- `DslHighlighter`: Syntax highlighting for web tools

## Performance Optimization

### DSL Parsing
- Zero-copy tokenization where possible
- Minimal allocations in hot path
- AST node pooling for frequently used constructs
- Template expansion caching

### Database Operations
- Batch inserts/updates
- Index optimization on uuid, type, scenario_id
- Lazy loading for component children
- Query result caching

### Theme Application
- Change detection to avoid redundant updates
- Batch property updates
- CSS-like cascade optimization

## Security Considerations

### Layout File Access
- Read-only enforcement (FR-109)
- File permission verification
- No modification during parsing
- Reject files outside allowed directories

### Component Properties
- Type validation before setting
- Sanitization of user input
- XSS prevention in web renderer
- SQL injection prevention in queries

### Plugin System
- Isolated namespaces
- Permission-based access control
- Sandboxed execution
- Stable API contracts

## Future Extensions

### IDE Tools (Phases 14-16)
- VSCode extension (TypeScript/LSP)
- Android Studio plugin (IntelliJ Platform/PSI)
- AvaUIWeb designer (Kotlin/JS)

### Advanced Features (P3/P4)
- Glass UI components for AR/VR
- Multi-step flow system (wizards)
- Navigation components
- Intent-to-UI mapping
- Layout critique engine

## References

- **Specification**: `/specs/002-avaui-uik-enhancements/spec.md` (v1.2.0)
- **Implementation Plan**: `/specs/002-avaui-uik-enhancements/plan.md`
- **Data Model**: `/specs/002-avaui-uik-enhancements/data-model.md`
- **API Contracts**: `/specs/002-avaui-uik-enhancements/contracts/`
- **Constitution**: `/.ideacode/memory/principles.md`

---

**Created by Manoj Jhawar, manoj@ideahq.net**
