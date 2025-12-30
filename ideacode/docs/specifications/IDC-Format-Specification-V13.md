# IDC Format Specification v13.0

## Overview

IDC (IDEACode Configuration) is a compact configuration format designed for AI agents. It achieves **71% smaller file sizes** compared to YAML while maintaining human readability.

---

## Design Goals

1. **Token Efficiency** - Minimize tokens for AI context windows
2. **Human Readable** - Easy to read and write
3. **Structured** - Categorized with type codes
4. **Flexible** - Supports metadata, aliases, and extensions

---

## File Structure

```
# Header Comment
---
schema: idc-1.0
version: X.X.X
project: project-name
metadata:
  file: filename.idc
  category: config-category
  tokens: ~NNN
---
# Section Comment

CODE:field1:field2:field3:description

---
aliases:
  code: [alias1, alias2]
```

### Sections

| Section | Required | Description |
|---------|----------|-------------|
| Header | Yes | Schema, version, project info |
| Body | Yes | Configuration lines |
| Aliases | No | Code aliases |

---

## Syntax

### Basic Line Format

```
CODE:field1:field2:field3:description
```

| Part | Description |
|------|-------------|
| CODE | 3-letter type code (uppercase) |
| field1 | Primary identifier |
| field2 | Secondary value |
| field3+ | Additional values |
| description | Human-readable note |

### Multi-Value Fields

Use `+` to separate multiple values in a field:

```
STK:universal:targets:android+ios+desktop+web
LLM:fallback:anthropic+openai+openrouter+groq:cloud
```

### Lists

Use `,` for comma-separated lists:

```
DEV:glasses:platforms:rokid,xreal,viture,rayneo
```

---

## Type Codes

### Core Codes

| Code | Full Name | Purpose |
|------|-----------|---------|
| VER | Version | Version information |
| API | API | API configuration |
| PTH | Path | File/directory paths |
| CFG | Config | General configuration |

### Zero Tolerance Codes

| Code | Full Name | Purpose |
|------|-----------|---------|
| ZTL | Zero Tolerance | Mandatory rules |
| ENF | Enforcement | Rule enforcement actions |
| VIO | Violation | Rule violation consequences |

### LLM Codes

| Code | Full Name | Purpose |
|------|-----------|---------|
| LLM | LLM | Language model config |
| MOD | Model | Model selection |
| PRV | Provider | Provider config |

### Quality Codes

| Code | Full Name | Purpose |
|------|-----------|---------|
| GAT | Gate | Quality gate rules |
| QUA | Quality | Quality settings |
| COV | Coverage | Test coverage |

### Tech Stack Codes

| Code | Full Name | Purpose |
|------|-----------|---------|
| STK | Stack | Tech stack config |
| LNG | Language | Programming language |
| BLD | Build | Build system |
| UIK | UI Kit | UI framework |
| STA | State | State management |
| NET | Network | Networking |
| DBS | Database | Database |
| TST | Testing | Test framework |
| PSK | Platform Stack | Platform-specific stack |
| PTP | Project Type | Project type defaults |
| DEV | Device | Device requirements |
| INP | Input | Input modalities |

### Command Codes

| Code | Full Name | Purpose |
|------|-----------|---------|
| CMD | Command | Command definition |
| ARG | Argument | Command argument |
| MOD | Modifier | Command modifier |

### Memory Codes

| Code | Full Name | Purpose |
|------|-----------|---------|
| MEM | Memory | Memory config |
| IDX | Index | Memory index |
| REG | Registry | Registry reference |

### Workflow Codes

| Code | Full Name | Purpose |
|------|-----------|---------|
| WKF | Workflow | Workflow definition |
| SKL | Skill | Skill reference |
| AGT | Agent | Agent definition |
| VRF | Verification | Verification step |

---

## Examples

### Zero Tolerance Rules

```idc
# IDEACode Zero Tolerance Rules
---
schema: idc-1.0
version: 13.0.0
project: ideacode-zero-tolerance
metadata:
  file: zero-tolerance.idc
  category: mandatory
  tokens: ~120
---
# Rules (format: ZTL:id:name:enforcement:description)
ZTL:1:memory_first:block:Read memory index before ANY action
ZTL:2:no_delete:block:Never delete without approval + pros/cons
ZTL:3:no_main_commit:block:Never commit to main/master
ZTL:4:no_hallucination:warn:Never invent APIs, files, URLs
ZTL:5:wait_for_user:block:Stop after questions, wait for answer
ZTL:6:no_stubs:block:No TODO, pass, placeholders
ZTL:7:source_grounding:warn:Read file before answering about it
ZTL:8:save_analysis:block:Save analysis to docs/
ZTL:9:code_proximity:block:Organize plans by file proximity

# Enforcement (format: ENF:level:action:description)
ENF:block:stop:Halt execution, fix before proceeding
ENF:warn:log:Log warning, continue with caution
---
aliases:
  ztl: [zero_tolerance, mandatory]
  enf: [enforcement, enforce]
```

### Tech Stack Configuration

```idc
# Tech Stack
---
schema: idc-1.0
version: 13.0.0
project: myapp
---
# Framework
STK:universal:framework:KMP:2.0+
STK:universal:targets:android+ios+desktop

# UI Frameworks
UIK:android:compose+material3:jetpack
UIK:ios:swiftui:native
UIK:desktop:compose-multiplatform:kmp

# State Management
STA:universal:kmp-shared:viewmodel
STA:android:viewmodel+stateflow:jetpack

# Database
DBS:universal:sqldelight:multiplatform
---
aliases:
  stk: [stack, tech]
  uik: [ui, framework]
```

### LLM Configuration

```idc
# LLM Providers
---
schema: idc-1.0
version: 13.0.0
project: ideacode
---
# Providers (format: LLM:role:provider:type)
LLM:primary:ollama:local
LLM:fallback:anthropic+openai+openrouter+groq:cloud

# Default Models
MOD:ollama:qwen2.5:7b
MOD:anthropic:claude-3-5-sonnet-20241022
MOD:openai:gpt-4o
MOD:groq:llama-3.1-70b-versatile
---
aliases:
  llm: [provider, model_provider]
  mod: [model, default_model]
```

### Quality Gates

```idc
# Quality Gates
---
schema: idc-1.0
version: 13.0.0
---
# Gates (format: GAT:name:threshold:blocking)
GAT:coverage:90:true
GAT:solid:0:true
GAT:security:0:true
GAT:performance:200:false

# Phases
QUA:pre-commit:coverage+solid+security
QUA:pre-push:coverage+solid+security+performance
QUA:ci:all
---
aliases:
  gat: [gate, quality_gate]
  qua: [quality, check]
```

---

## Enforcement Levels

| Level | Action | Use Case |
|-------|--------|----------|
| block | Stop execution | Critical rules |
| warn | Log and continue | Best practices |
| info | Report only | Suggestions |

---

## Token Comparison

### YAML (Original)

```yaml
zero_tolerance:
  rules:
    - id: 1
      name: memory_first
      enforcement: block
      description: "Read memory index before ANY action"
    - id: 2
      name: no_delete
      enforcement: block
      description: "Never delete without approval + pros/cons"
```

**Tokens: ~85**

### IDC (Optimized)

```
ZTL:1:memory_first:block:Read memory index before ANY action
ZTL:2:no_delete:block:Never delete without approval + pros/cons
```

**Tokens: ~25**

**Reduction: 71%**

---

## Parsing Rules

1. **Lines starting with `#`** - Comments (ignored)
2. **Lines starting with `---`** - Section delimiter
3. **Empty lines** - Ignored
4. **`key: value` format** - Metadata (YAML-like header)
5. **`CODE:...:...` format** - Configuration line

### Parser Pseudocode

```python
def parse_idc(content):
    sections = content.split('---')

    # Parse header (YAML-like)
    header = parse_yaml(sections[0])

    # Parse body (IDC lines)
    config = {}
    for line in sections[1].split('\n'):
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split(':')
        code = parts[0]
        config.setdefault(code, []).append(parts[1:])

    # Parse aliases (optional)
    aliases = {}
    if len(sections) > 2:
        aliases = parse_yaml(sections[2])

    return { 'header': header, 'config': config, 'aliases': aliases }
```

---

## Best Practices

### Do

- Use 3-letter codes consistently
- Group related lines together
- Include section comments
- Define aliases for common codes
- Keep descriptions concise

### Don't

- Use codes longer than 3 letters
- Mix different separators (`:` vs `.`)
- Include sensitive data (API keys)
- Create deeply nested structures

---

## File Naming

| Type | Pattern | Example |
|------|---------|---------|
| Config | `config.idc` | `.ideacode/config.idc` |
| Tech Stack | `tech-stack.idc` | `.ideacode/tech-stack.idc` |
| Zero Tolerance | `zero-tolerance.idc` | `.ideacode/memory/zero-tolerance.idc` |
| Memory Index | `memory-index.idc` | `.ideacode/memory/memory-index.idc` |
| Project | `{project}.idc` | `.ideacode/myapp.idc` |

---

## Migration from YAML

1. **Extract type codes** from key names
2. **Flatten nested structures** to colon-separated lines
3. **Convert arrays** to `+` or `,` separated values
4. **Move metadata** to header section
5. **Add aliases** for backwards compatibility

---

## Schema Validation

```typescript
interface IDCLine {
  code: string;      // 3-letter code
  fields: string[];  // Variable number of fields
}

interface IDCDocument {
  schema: 'idc-1.0';
  version: string;
  project: string;
  metadata: {
    file: string;
    category: string;
    tokens?: number;
  };
  lines: IDCLine[];
  aliases?: Record<string, string[]>;
}
```

---

**Author:** Manoj Jhawar
**Version:** 13.0.0
**Updated:** 2025-12-29
