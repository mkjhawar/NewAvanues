# AvaCode Codegen Design Summary

**Executive Overview**

Version: 1.0.0
Last Updated: 2025-10-28

---

## Overview

This document summarizes the comprehensive design for AvaCode's multi-target code generation system, which generates native code for **Kotlin Jetpack Compose**, **SwiftUI**, and **React/TypeScript**.

---

## Key Deliverables

### 1. Complete Component Mappings (5 Components × 3 Targets)

All built-in components have been fully mapped:

| Component | Category | Properties | Callbacks | Supports Children |
|-----------|----------|------------|-----------|-------------------|
| **ColorPicker** | INPUT | 4 (id, initialColor, mode, showAlpha) | 3 (onColorChanged, onConfirm, onCancel) | No |
| **Preferences** | GENERAL | 0 (API component) | 0 | No |
| **Text** | DISPLAY | 3 (text, size, color) | 0 | No |
| **Button** | INPUT | 2 (text, enabled) | 1 (onClick) | No |
| **Container** | CONTAINER | 1 (orientation) | 0 | Yes |

### 2. State Management Patterns

Complete patterns documented for each platform:

#### Kotlin Compose
- `remember { mutableStateOf() }` for local state
- `derivedStateOf` for computed values
- `LaunchedEffect` for side effects
- `DisposableEffect` for cleanup
- State hoisting pattern for parent-child communication

#### SwiftUI
- `@State` for local state
- `@Binding` for two-way bindings
- `@StateObject` / `@ObservedObject` for complex state
- `.onAppear` / `.onDisappear` for lifecycle
- `.onChange` for reactive updates
- `.task` for async operations

#### React/TypeScript
- `useState` for local state
- `useMemo` for computed values
- `useEffect` for side effects and cleanup
- `useCallback` for memoized callbacks
- `useContext` for global state
- Custom hooks for reusable logic

### 3. Callback/Event Handler Patterns

Complete signature mappings:

| DSL Pattern | Kotlin | Swift | TypeScript |
|-------------|--------|-------|------------|
| `onClick: () => void` | `onClick: () -> Unit` | `action: () -> Void` | `onClick: () => void` |
| `onColorChanged: (color) => void` | `onColorChanged: (ColorRGBA) -> Unit` | `onColorChanged: (Color) -> Void` | `onColorChanged: (color: string) => void` |
| `onConfirm: (color) => void` | `onConfirmed: (ColorRGBA) -> Unit` | `onConfirm: (Color) -> Void` | `onConfirm: (color: string) => void` |

### 4. Import Generation Templates

Platform-specific import sets documented:

**Kotlin Compose**:
```kotlin
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
```

**SwiftUI**:
```swift
import SwiftUI
import Foundation
import Combine
```

**React/TypeScript**:
```typescript
import React, { useState, useEffect } from 'react';
import type { FC, ReactNode } from 'react';
```

### 5. Code Structure Templates

Complete file structure templates for:
- Component definitions
- State management
- Side effects
- Event handlers
- Documentation comments
- Preview/testing code

### 6. Full Working Examples

Three complete ColorPicker applications provided:
- **Kotlin Compose** (173 lines) - Full Android/Desktop app
- **SwiftUI** (147 lines) - Complete iOS app
- **React/TypeScript** (203 lines with CSS) - Web application

Each example includes:
- Complete imports
- State management
- Color history tracking
- Modal presentation
- Event handling
- Color conversion utilities
- Styling

### 7. Type Conversion Reference

Comprehensive type mappings:

| DSL Type | Kotlin | Swift | TypeScript |
|----------|--------|-------|------------|
| STRING | String | String | string |
| INT | Int | Int | number |
| FLOAT | Float | Double | number |
| BOOLEAN | Boolean | Bool | boolean |
| COLOR | Color / ColorRGBA | Color | string (hex) |
| ENUM | enum class | enum | union type |

With color conversion utilities for all platforms.

---

## Architecture Design

### Code Generation Pipeline

```
┌─────────────┐
│  DSL Input  │
│  (*.magic)  │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Parser     │
│  (AST)      │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ Component   │
│ Model       │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ Validator   │
└──────┬──────┘
       │
       ▼
       ┌────────────┬────────────┬────────────┐
       ▼            ▼            ▼
┌──────────────┐ ┌──────────┐ ┌──────────┐
│    Kotlin    │ │  Swift   │ │  React   │
│    Mapper    │ │  Mapper  │ │  Mapper  │
└──────┬───────┘ └────┬─────┘ └────┬─────┘
       │              │            │
       ▼              ▼            ▼
┌──────────────┐ ┌──────────┐ ┌──────────┐
│   Template   │ │ Template │ │ Template │
│   Engine     │ │  Engine  │ │  Engine  │
└──────┬───────┘ └────┬─────┘ └────┬─────┘
       │              │            │
       ▼              ▼            ▼
┌──────────────┐ ┌──────────┐ ┌──────────┐
│  Formatter   │ │Formatter │ │Formatter │
│  (ktlint)    │ │(SwiftFmt)│ │(Prettier)│
└──────┬───────┘ └────┬─────┘ └────┬─────┘
       │              │            │
       ▼              ▼            ▼
┌──────────────────────────────────────┐
│        Generated Code Output          │
│   .kt files  .swift files  .tsx files│
└──────────────────────────────────────┘
```

### Core Interfaces

1. **CodeGenerator Interface**
   - `generate(component, context): CodeGenResult<GeneratedFile>`
   - `generateMultiple(components, context): CodeGenResult<List<GeneratedFile>>`
   - `validate(component): ValidationResult`

2. **ComponentMapper Interface**
   - `mapComponent(component): TargetComponent`
   - `mapProperty(name, value, type): TargetProperty`
   - `mapCallback(callback): TargetCallback`
   - `mapChildren(children): List<TargetComponent>`

3. **TemplateEngine Interface**
   - `render(template, data): String`
   - `loadTemplate(name): String?`
   - `registerHelper(name, helper): Unit`

4. **TypeConverter**
   - `convertType(type, target, nullable): String`
   - `convertValue(value, type, target): String`

5. **ComponentValidator**
   - `validate(component): ValidationResult`
   - Checks required properties, types, enum values, children support

---

## Implementation Recommendations

### 1. Template-Based Generation (Recommended)

**Pros**:
- Easy to maintain and update
- Non-programmers can edit templates
- Clear separation of concerns
- Fast development cycle

**Structure**:
```
codegen/
├── templates/
│   ├── kotlin/
│   ├── swift/
│   └── typescript/
├── generators/
│   ├── KotlinGenerator.kt
│   ├── SwiftGenerator.kt
│   └── TypeScriptGenerator.kt
└── core/
    ├── ComponentMapper.kt
    ├── TypeConverter.kt
    └── Validator.kt
```

### 2. Alternative: AST-Based Generation

**Pros**:
- Type-safe
- Better IDE support
- Refactoring-friendly

**Tools**:
- **Kotlin**: KotlinPoet
- **Swift**: SwiftSyntax
- **TypeScript**: ts-morph

### 3. Hybrid Approach (Best)

Combine templates for structure with AST manipulation for complex logic:
- Templates for boilerplate
- AST builders for type-safe property/callback handling
- Formatters for final output

---

## Component-Specific Notes

### ColorPicker
- Most complex component
- Requires factory initialization (Android)
- Modal presentation differs by platform:
  - Kotlin: Dialog/BottomSheet
  - Swift: `.sheet()` modifier
  - React: Modal overlay with backdrop
- State management: color selection, modal visibility, history
- Callbacks: live updates vs confirmation

### Preferences
- Not a UI component - pure API
- Platform storage:
  - Android: SharedPreferences
  - iOS: UserDefaults / Keychain
  - Web: localStorage / IndexedDB
- Reactive updates via listeners/observers
- Type-safe access through PreferenceKey

### Text
- Simplest component
- Direct mapping to native text views
- Property handling:
  - Size: sp (Kotlin), points (Swift), px (React)
  - Color: Color object vs hex string
  - Weight/style variations

### Button
- Standard across platforms
- Click/tap handling
- Enabled/disabled state
- Styling variations:
  - Kotlin: Material 3 styles
  - Swift: `.buttonStyle()`
  - React: CSS classes

### Container
- Maps to layout primitives:
  - Kotlin: Column/Row
  - Swift: VStack/HStack
  - React: div with flex
- Orientation property drives layout choice
- Children rendering strategy

---

## Type Conversion Examples

### Color Conversions

**Hex to Platform Color**:
```kotlin
// Kotlin
Color(0xFFFF5722)

// Swift
Color(hex: "#FF5722")

// TypeScript
"#FF5722"
```

**Color to Hex**:
```kotlin
// Kotlin
"#${color.value.toString(16).uppercase().takeLast(6)}"

// Swift
color.toHex()

// TypeScript
color // already hex string
```

### Enum Conversions

**DSL Enum to Platform**:
```kotlin
// Kotlin
enum class ColorPickerMode { FULL, COMPACT }
val mode = ColorPickerMode.valueOf("FULL")

// Swift
enum ColorPickerMode: String { case full = "FULL" }
let mode = ColorPickerMode(rawValue: "FULL")

// TypeScript
type ColorPickerMode = 'FULL' | 'COMPACT';
const mode: ColorPickerMode = 'FULL';
```

---

## Testing Strategy

### 1. Unit Tests
- Test component mapping logic
- Test type conversions
- Test validation rules
- Test template rendering

### 2. Integration Tests
- Test full generation pipeline
- Test multi-component generation
- Test cross-references between components

### 3. Golden File Tests
- Compare generated code against known-good outputs
- Detect unintended changes
- Version control for generated samples

### 4. Runtime Tests
- Compile generated code
- Run generated apps
- UI testing on each platform

---

## Performance Considerations

### 1. Generation Speed
- **Template caching**: Load templates once
- **Parallel generation**: Generate multiple files concurrently
- **Incremental updates**: Only regenerate changed components

### 2. Output Size
- **Minification**: Optional for production builds
- **Tree shaking**: Remove unused imports
- **Code splitting**: Separate large components

### 3. Compile Time
- **Optimize imports**: Only import what's needed
- **Avoid redundancy**: Share common code
- **Lazy loading**: Components on demand

---

## Error Handling

### Validation Errors
```kotlin
sealed class CodeGenError {
    data class InvalidComponent(type: String, message: String)
    data class InvalidProperty(name: String, expectedType: PropertyType)
    data class MissingRequiredProperty(componentType: String, propertyName: String)
    data class UnsupportedCallback(name: String, target: TargetPlatform)
}
```

### Error Messages
- Clear, actionable messages
- Show component path/location
- Provide hints for fixes
- List available alternatives

### Recovery Strategies
- Skip invalid components (with warning)
- Use default values for missing properties
- Fallback to stub implementations
- Generate error comments in code

---

## Build System Integration

### Gradle Plugin (Kotlin/Android)
```kotlin
plugins {
    id("com.augmentalis.avacode") version "1.0.0"
}

magicCode {
    inputDir = file("src/main/avacode")
    outputDir = file("build/generated/avacode")
    target = TargetPlatform.KOTLIN_COMPOSE
    formatCode = true
    generateTests = true
}
```

### SPM Plugin (Swift/iOS)
```swift
// Package.swift
.plugin(
    name: "AvaCodeGenerator",
    capability: .buildTool(),
    dependencies: ["AvaCodeCore"]
)
```

### npm Scripts (TypeScript/Web)
```json
{
  "scripts": {
    "generate": "avacode generate --target react-ts",
    "generate:watch": "avacode generate --watch"
  }
}
```

---

## Recommended Tools

### Code Generation
- **Kotlin**: [KotlinPoet](https://square.github.io/kotlinpoet/)
- **Swift**: [SwiftSyntax](https://github.com/apple/swift-syntax)
- **TypeScript**: [ts-morph](https://ts-morph.com/)

### Formatting
- **Kotlin**: [ktlint](https://pinterest.github.io/ktlint/)
- **Swift**: [SwiftFormat](https://github.com/nicklockwood/SwiftFormat)
- **TypeScript**: [Prettier](https://prettier.io/)

### Template Engines
- [Mustache](https://mustache.github.io/) - Logic-less
- [Handlebars](https://handlebarsjs.com/) - Extended Mustache
- [Velocity](https://velocity.apache.org/) - Java-based
- Custom (included in utilities doc)

### Testing
- **Kotlin**: JUnit 5, Kotest
- **Swift**: XCTest
- **TypeScript**: Jest, Vitest

---

## Next Steps

### Phase 1: Foundation (Weeks 1-2)
1. Implement core interfaces
2. Build template engine
3. Create validation framework
4. Set up project structure

### Phase 2: Kotlin Generator (Weeks 3-4)
1. Implement KotlinComposeMapper
2. Create Kotlin templates
3. Build Kotlin generator
4. Write unit tests
5. Generate sample apps

### Phase 3: Swift Generator (Weeks 5-6)
1. Implement SwiftUIMapper
2. Create Swift templates
3. Build Swift generator
4. Write unit tests
5. Generate sample apps

### Phase 4: React Generator (Weeks 7-8)
1. Implement ReactMapper
2. Create TypeScript templates
3. Build React generator
4. Write unit tests
5. Generate sample apps

### Phase 5: Integration (Weeks 9-10)
1. Build CLI tool
2. Create Gradle plugin
3. Create SPM plugin
4. Create npm package
5. Documentation
6. Examples repository

### Phase 6: Polish (Weeks 11-12)
1. Performance optimization
2. Error message improvements
3. Additional components
4. Advanced features
5. User testing
6. Release preparation

---

## File References

This design consists of three documents:

1. **TARGET_FRAMEWORK_MAPPINGS.md** (Main Design)
   - Complete component mappings
   - State management patterns
   - Callback patterns
   - Import templates
   - Code structure
   - Full examples
   - Type conversions
   - Recommendations

2. **CODE_GENERATION_UTILITIES.md** (Implementation)
   - Core interfaces
   - Template engine
   - Component mapper
   - Type converter
   - Validation framework
   - Example implementations

3. **CODEGEN_DESIGN_SUMMARY.md** (This Document)
   - Executive overview
   - Key deliverables
   - Architecture
   - Recommendations
   - Next steps

---

## Success Metrics

### Code Quality
- ✓ Generated code compiles without errors
- ✓ Generated code passes linting
- ✓ Generated code follows platform conventions
- ✓ Generated code is readable and maintainable

### Completeness
- ✓ All 5 components mapped to all 3 targets
- ✓ All properties correctly converted
- ✓ All callbacks properly typed
- ✓ All state patterns implemented
- ✓ All lifecycle hooks covered

### Performance
- ✓ Generation completes in <1s per component
- ✓ Generated code has minimal overhead
- ✓ Build time impact <10% for typical project

### Developer Experience
- ✓ Clear error messages
- ✓ IDE integration available
- ✓ Documentation comprehensive
- ✓ Examples working out-of-box

---

## Conclusion

This comprehensive design provides:

1. **Complete mappings** for all components across all targets
2. **Working examples** demonstrating full applications
3. **Implementation guide** with code utilities
4. **Type-safe approach** with validation
5. **Extensible architecture** for future components
6. **Best practices** for each platform
7. **Clear roadmap** for implementation

The design is production-ready and can serve as the foundation for building AvaCode's multi-target code generation system.

---

**Document Status**: ✅ Complete and Ready for Implementation

**Generated**: 2025-10-28
**Author**: Claude Code
**Version**: 1.0.0
