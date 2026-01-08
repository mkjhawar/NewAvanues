# /i.handover - AI Context Continuity System

## Usage
`/i.handover [.operation] [target] [.modifiers]`

## Purpose
Create, manage, and convert handover files for AI session continuity.
Uses AVU format (.hov extension) for 80%+ token reduction while maintaining full reversibility.

---

## Operations

| Operation | Purpose |
|-----------|---------|
| `.create` | Create new handover from current session (default) |
| `.expand` | Convert .hov to human-readable markdown |
| `.compact` | Convert markdown to .hov format |
| `.index` | Rebuild HANDOVER-INDEX.md |
| `.read` | Load handover context for current task |
| `.chunk` | Split large handover into context-proximate chunks |
| `.merge` | Merge multiple .hov files |
| `.validate` | Validate .hov file format |

---

## .create - Create Handover

### Default Behavior
Creates chunked .hov files organized by context proximity:

```
{repo}/.claude/handover/
├── HANDOVER-INDEX.md          # Mandatory read - routing table
├── architecture.hov           # ARC, DEC entries
├── state.hov                  # STA, WIP, BLK entries
├── modules/
│   ├── {module-name}.hov      # Module-specific context
│   └── ...
├── tasks.hov                  # TSK, PRI entries
└── learnings.hov              # LEA, BUG entries
```

### Process
1. Analyze current session context
2. Extract key information by type
3. Generate .hov entries using HandoverCodes
4. Chunk by context proximity (max 64KB per file)
5. Update HANDOVER-INDEX.md
6. Index into RAG for semantic search

### Example Output (architecture.hov)
```
# Avanues Universal Format v1.0
# Type: HOV
# Extension: .hov
---
schema: avu-1.0
version: 1.0.0
project: ideacode
module: core
metadata:
  file: architecture.hov
  category: handover
  created: 2025-12-29
  session: abc123
  chunk: 1/5
---
ARC:pattern:MVVM+Clean Architecture
ARC:di:Hilt for Android, Koin for KMP
ARC:db:SQLDelight (KMP), Room (Android-only)
DEC:d001:JWT over session|scalability+stateless
DEC:d002:KMP shared|60% code reuse target
FIL:core:[AuthService.kt,TokenManager.kt,UserRepo.kt]
DEP:auth:ktor-client,kotlinx-serialization
---
```

**Token comparison:**
- Markdown equivalent: ~800 tokens
- .hov format: ~120 tokens (85% reduction)

---

## .expand - Convert to Human-Readable

### Usage
`/i.handover .expand [file.hov]`

### Process
Converts compact .hov to readable markdown:

**Input (architecture.hov):**
```
ARC:pattern:MVVM+Clean Architecture
DEC:d001:JWT over session|scalability+stateless
```

**Output (architecture-expanded.md):**
```markdown
## Architecture

### Patterns
- **Pattern:** MVVM + Clean Architecture

### Decisions
- **DEC-001:** Chose JWT over session
  - **Rationale:** scalability + stateless
```

---

## .compact - Convert to .hov Format

### Usage
`/i.handover .compact [file.md]`

### Process
Converts human-readable markdown to .hov:

**Input (notes.md):**
```markdown
## Current State
The authentication module is being refactored from session-based to JWT.
Main files: AuthService.kt, TokenManager.kt

## Blockers
- Refresh token rotation fails with multiple devices
```

**Output (notes.hov):**
```
STA:auth:refactoring session→JWT
FIL:auth:[AuthService.kt,TokenManager.kt]
BLK:b001:refresh rotation fails|multi-device
```

---

## .read - Load Handover Context

### Usage
`/i.handover .read [module]`

### Process
1. Read HANDOVER-INDEX.md (mandatory)
2. Identify relevant chunks for current task
3. Load only needed .hov files
4. Parse and present context

### Smart Loading
```
Task: "Fix auth refresh bug"
  → HANDOVER-INDEX.md (2KB)
  → modules/auth.hov (15KB)
  → state.hov filtered to auth entries (3KB)
Total: 20KB vs 500KB+ for monolithic handover
```

---

## .chunk - Split Large Handover

### Usage
`/i.handover .chunk [file.md] [--max-size 64KB]`

### Process
1. Parse input file
2. Group entries by context proximity:
   - Same module → same chunk
   - Related decisions → same chunk
   - Dependent files → same chunk
3. Split at size boundaries
4. Generate index

### Context Proximity Rules
| Entries | Grouping |
|---------|----------|
| Same MOD code | Always together |
| FIL + related BUG | Together |
| DEC + related ARC | Together |
| TSK + related WIP | Together |

---

## Entry Codes Reference

| Code | Meaning | Example |
|------|---------|---------|
| `ARC` | Architecture | `ARC:pattern:MVVM+Clean` |
| `STA` | State | `STA:auth:refactoring` |
| `WIP` | Work in Progress | `WIP:w001:TokenManager 60% done` |
| `BLK` | Blocker | `BLK:b001:multi-device refresh` |
| `DEC` | Decision | `DEC:d001:JWT over session\|scalability` |
| `FIL` | Files | `FIL:auth:[Auth.kt,Token.kt]` |
| `MOD` | Module | `MOD:auth:handles all authentication` |
| `LEA` | Learning | `LEA:l001:sessions don't scale` |
| `TSK` | Task | `TSK:t001:implement refresh rotation` |
| `DEP` | Dependency | `DEP:auth:ktor,serialization` |
| `CFG` | Config | `CFG:jwt:expiry=1h,refresh=7d` |
| `API` | API Change | `API:auth:added refreshToken endpoint` |
| `BUG` | Known Bug | `BUG:b001:race condition in logout` |
| `REF` | Reference | `REF:spec:Auth-Spec-251229-V1.md` |
| `CTX` | Context | `CTX:session:working on auth refactor` |
| `PRI` | Priority | `PRI:P0:fix refresh before release` |

---

## Modifiers

| Modifier | Effect |
|----------|--------|
| `.human` | Output in human-readable markdown |
| `.compact` | Output in .hov format (default) |
| `.full` | Include all context (no chunking) |
| `.module <name>` | Filter to specific module |
| `.since <date>` | Only entries after date |
| `.priority <P0\|P1\|P2>` | Filter by priority |

---

## HANDOVER-INDEX.md Format

```markdown
# Handover Index

**Last Updated:** 2025-12-29
**Session:** abc123
**Total Chunks:** 5

## Quick Lookup

| Topic | File | Size | Last Modified |
|-------|------|------|---------------|
| Architecture | architecture.hov | 12KB | 2025-12-29 |
| Current State | state.hov | 8KB | 2025-12-29 |
| Auth Module | modules/auth.hov | 15KB | 2025-12-29 |
| Tasks | tasks.hov | 5KB | 2025-12-29 |
| Learnings | learnings.hov | 6KB | 2025-12-29 |

## Entry Distribution

| Code | Count | Primary File |
|------|-------|--------------|
| ARC | 12 | architecture.hov |
| DEC | 8 | architecture.hov |
| STA | 5 | state.hov |
| WIP | 3 | state.hov |
| BLK | 2 | state.hov |
| MOD | 4 | modules/*.hov |
| TSK | 7 | tasks.hov |

## Semantic Tags
- #authentication #jwt #refactoring
- #multi-device #token-rotation
- #architecture #clean-architecture
```

---

## RAG Integration

Handover chunks are automatically indexed into the RAG system:
- Semantic search: "what's blocking auth?" → finds BLK entries
- Context routing: "fix token refresh" → loads auth.hov
- Cross-reference: links related entries across chunks

---

## Examples

| Command | Result |
|---------|--------|
| `/i.handover` | Create handover from current session |
| `/i.handover .create` | Same as above |
| `/i.handover .expand state.hov` | Convert to markdown |
| `/i.handover .compact notes.md` | Convert to .hov |
| `/i.handover .read auth` | Load auth module context |
| `/i.handover .chunk large.md` | Split into context-proximate chunks |
| `/i.handover .human` | Create human-readable handover |
| `/i.handover .module auth` | Handover for auth module only |

---

## Size Limits

| Item | Limit | Reason |
|------|-------|--------|
| Single .hov chunk | 64KB | Optimal context window usage |
| HANDOVER-INDEX.md | 4KB | Mandatory read, must be small |
| Total handover | No limit | Chunked automatically |
| Entry data field | 500 chars | Use REF for longer content |

---

## Mandatory Reading Protocol

**On session start, AI MUST:**
1. Read `{repo}/.claude/handover/HANDOVER-INDEX.md`
2. Identify relevant chunks for current task
3. Load only needed chunks
4. Acknowledge context loaded

**Failure to read = context loss = repeated mistakes**

---

## Related

| Command | Purpose |
|---------|---------|
| `/i.memory` | Shared memory for multi-terminal |
| `/i.context` | Session context management |
| `/i.project` | Project-level instructions |

---

**Version:** 1.0
**Format:** AVU (Avanues Universal Format)
**Extension:** .hov
