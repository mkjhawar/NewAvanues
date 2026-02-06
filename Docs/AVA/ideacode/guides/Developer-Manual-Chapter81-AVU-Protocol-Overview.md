# Developer Manual - Chapter 81: AVU Protocol Overview

**Date**: 2026-02-06
**Author**: Augmentalis Engineering
**Status**: Active

---

## Overview

The Avanues Universal (AVU) protocol is a compact, self-documenting format for voice-first applications. It uses 3-letter codes with colon-delimited fields to achieve 60-87% smaller messages than JSON, while maintaining human readability through self-documenting headers and code registries.

AVU v2.2 introduces a three-layer architecture that extends the existing wire protocol into a full declarative scripting system.

---

## Three-Layer Architecture

```
+-----------------------------------------------------------+
| Layer 3: AVU Runtime (Interpreter)                         |
|   Tree-walking evaluator with sandbox                      |
|   Dispatches codes via IAvuDispatcher                      |
|   Package: com.augmentalis.voiceoscore.dsl.interpreter     |
+-----------------------------------------------------------+
| Layer 2: AVU DSL (File Format)                             |
|   .vos/.avp files with YAML-like header + indented body    |
|   Lexer -> Parser -> Immutable AST                         |
|   Package: com.augmentalis.voiceoscore.dsl                 |
+-----------------------------------------------------------+
| Layer 1: AVU Wire Protocol                                 |
|   CODE:field1:field2 compact encoding                      |
|   77+ 3-letter codes, AvuEscape, AvuCodeRegistry           |
|   Package: com.augmentalis.avucodec.core                   |
+-----------------------------------------------------------+
```

### Layer 1: Wire Protocol

The foundation layer handles compact message encoding using 3-letter uppercase codes. Each message follows the format `CODE:field1:field2:...` with percent-encoding for special characters. Supports IPC over TCP, WebSocket, Android Intent, and iOS URL schemes.

**Key components:** `AvuEscape`, `AvuCodeRegistry`, `AvuCodeInfo`, `AvuHeader`

See: [Chapter 82: AVU Wire Protocol](Developer-Manual-Chapter82-AVU-Wire-Protocol.md)

### Layer 2: DSL Format

The file format layer defines `.vos` (workflow) and `.avp` (plugin) files with a YAML-like header section between `---` markers and an indentation-based body. Top-level declarations include `@workflow`, `@define`, and `@on`. Control flow uses `@if`/`@else`, `@repeat`, `@while`, `@wait`, and `@sequence`.

**Key components:** `AvuDslLexer`, `AvuDslParser`, `AvuAstNode` sealed hierarchy

See: [Chapter 83: AVU DSL Syntax](Developer-Manual-Chapter83-AVU-DSL-Syntax.md)

### Layer 3: Runtime Interpreter

The execution layer walks the AST and dispatches code invocations through the `IAvuDispatcher` interface. Runs inside a configurable sandbox with step limits, timeouts, and permission enforcement.

**Key components:** `AvuInterpreter`, `IAvuDispatcher`, `SandboxConfig` (future Phase 2)

See: [Chapter 85: AVU Runtime Interpreter](Developer-Manual-Chapter85-AVU-Runtime-Interpreter.md)

---

## Package Map

| Package | Layer | Contents |
|---------|-------|----------|
| `com.augmentalis.avucodec.core` | 1 | AvuEscape, AvuCodeRegistry, AvuCodeInfo, AvuHeader |
| `com.augmentalis.voiceoscore.dsl.lexer` | 2 | Token, AvuDslLexer |
| `com.augmentalis.voiceoscore.dsl.ast` | 2 | AvuAstNode sealed hierarchy |
| `com.augmentalis.voiceoscore.dsl.parser` | 2 | AvuDslParser, ParseError |
| `com.augmentalis.voiceoscore.dsl.interpreter` | 3 | AvuInterpreter, IAvuDispatcher (future) |
| `com.augmentalis.voiceoscore.dsl.plugin` | 3 | PluginLoader, PluginRegistry (future) |

---

## Design Principles

1. **KMP-first**: All DSL code lives in `commonMain` - no platform-specific APIs in parser or interpreter
2. **Immutable AST**: All AST nodes are `data class` inside a `sealed class` hierarchy
3. **Text-based plugins**: `.avp` files are data, not code - App Store compliant
4. **Sandbox security**: Step limits, permission enforcement, timeout protection
5. **Foundation reuse**: DSL parser delegates header parsing to `AvuHeader.parse()` from AVUCodec
6. **Generic extensibility**: `AvuHeader.HeaderData.sections` captures any header section without AVUCodec changes

---

## Size Comparison: JSON vs AVU

```
Voice Command (JSON): 156 bytes
{"type":"voice_command","requestId":"cmd_01","text":"scroll down","context":{"app":"chrome","screen":"main"}}

Voice Command (AVU): 47 bytes
VCM:cmd_01:scroll down:app=chrome,screen=main

Reduction: 70%
```

---

## Version History

| Version | Date | Key Changes |
|---------|------|-------------|
| v2.0 | - | Base AVU file format (Ch37) |
| v2.1 | - | AvuProtocol.kt per-module encoders |
| v2.2 | - | AVUCodec module: AvuEscape, AvuCodeRegistry, AvuHeader (Ch80) |
| DSL 1.0 | 2026-02-06 | Three-layer architecture, parser, .avp plugins (Ch81-87) |

---

## Related Documents

- [Ch37: Universal Format v2.0](Developer-Manual-Chapter37-Universal-Format-v2.0.md)
- [Ch51: 3Letter JSON Schema](Developer-Manual-Chapter51-3Letter-JSON-Schema.md)
- [Ch67: Avanues Plugin Development](Developer-Manual-Chapter67-Avanues-Plugin-Development.md) (SUPERSEDED by Ch86)
- [Ch68: Workflow Engine Architecture](Developer-Manual-Chapter68-Workflow-Engine-Architecture.md) (SUPERSEDED by Ch85)
- [Ch76: RPC Module Architecture](Developer-Manual-Chapter76-RPC-Module-Architecture.md)
- [Ch80: AVU Codec v2.2](Developer-Manual-Chapter80-AVU-Codec-v2.2.md)
- Ch82-87: Detailed layer documentation

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-02-06 | Initial chapter |
