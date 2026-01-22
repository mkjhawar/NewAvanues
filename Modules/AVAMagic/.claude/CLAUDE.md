# AVAMagic - Module Instructions

Module: AVAMagic - MagicUI/MagicCode Generation System

---

## PURPOSE

AVAMagic provides UI component generation and code generation capabilities for the Avanues platform. It includes renderers, components, and libraries for dynamic UI creation.

---

## MODULE STRUCTURE

| Path | Purpose |
|------|---------|
| Code/ | Core code utilities |
| CodeGen/ | Code generation tools |
| Components/ | Reusable UI components |
| Libraries/ | Supporting libraries |
| Renderers/ | UI rendering engines |
| UI/ | UI definitions and templates |

---

## TECH STACK

| Component | Technology |
|-----------|------------|
| UI Framework | MagicUI (cross-platform) |
| Code Generation | MagicCode |
| Platforms | Android, iOS, Web, Desktop |
| Language Server | Kotlin LSP (DSL tooling) |
| Web UI | React + TypeScript + Monaco Editor |
| IDE Plugins | VS Code Extension + IntelliJ Plugin |

---

## LSP TOOLING ARCHITECTURE

**MagicUI Language Server (Kotlin)**
- Location: `MagicTools/LanguageServer/`
- Implements: Language Server Protocol (LSP)
- Features:
  - DSL syntax validation (YAML, JSON, Compact)
  - Code completion (components, properties, themes)
  - Hover documentation
  - Go-to-definition (VUID navigation)
  - Code generation (theme → Kotlin DSL)
  - Real-time error checking
- Integration: ThemeCompiler.kt, VUIDCreator, all parsers
- Size: ~25MB (one-time install, shared)

**Web UI Components (React + TypeScript)**
- Location: `MagicTools/WebComponents/`
- Components:
  - ThemePicker: Color palette editor (WCAG contrast checker)
  - PropertyInspector: Visual property editing
  - ComponentPalette: Drag-and-drop component library
  - LivePreview: Real-time platform rendering
  - MonacoEditor: Code editor integration
- Size: ~5MB bundled
- Embeddable in: VS Code webviews, IntelliJ JCEF, standalone apps

**IDE Extensions**
1. **VS Code Extension** (`MagicTools/VSCodeExtension/`)
   - TypeScript + VS Code API
   - LSP client (connects to MagicUI Language Server)
   - Webview panels (embeds React components)
   - Features: Theme creator, DSL editor, live preview
   - Size: ~2MB

2. **IntelliJ Plugin** (`MagicTools/IntelliJPlugin/`)
   - Kotlin + IntelliJ Platform SDK
   - LSP integration (native support)
   - JCEF browser (embeds web components)
   - Features: Same as VS Code + Android preview
   - Size: ~3MB

**Standalone Apps (Optional)**
- Location: `MagicTools/ThemeCreator/`, `MagicTools/DSLEditor/`
- Options:
  - Tauri v2: ~8-10MB (Rust + web frontend)
  - Compose Desktop + CEF: ~60-65MB (full Kotlin stack)
- Use case: Offline tools, no IDE required

---

## DEVELOPMENT RULES

| Rule | Requirement |
|------|-------------|
| Components | Must be cross-platform compatible |
| Renderers | Platform-specific implementations allowed |
| CodeGen | Output must follow IDEACODE standards |
| LSP Server | MUST reuse existing Kotlin logic (no duplication) |
| Web Components | MUST be embeddable (no framework lock-in) |
| IDE Plugins | MUST share LSP server (single source of truth) |
---

## RELATED MODULES

| Module | Relationship |
|--------|--------------|
| AVA | Primary consumer |
| VoiceOS | UI integration |
| WebAvanue | Web rendering |

---

Updated: 2025-12-24 | Version: 2.0 (LSP Architecture)
