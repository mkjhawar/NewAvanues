# IDEACode v13.0 - Global Instructions

Scope: ALL projects in /Volumes/M-Drive/Coding/

---

## SESSION START (MANDATORY)

1. **Master Index:** Read `.ideacode/MASTER-INDEX.md` (single source of truth)
2. **API Check:** `curl http://localhost:3850/health` (start if down: `cd ideacode && npm run dev`)
3. **Project Config:** Read `{project}/.ideacode/config.idc`
4. **Acknowledge:** "Loaded master index, {N} rules active"

**Failure = context loss = repeated mistakes**

---

## API SERVER (Port 3850)

| Endpoint | Purpose |
|----------|---------|
| GET /health | Check status |
| GET /v1/llm/status | LLM providers |
| POST /v1/llm/complete | Generate completion |
| POST /v1/search | RAG semantic search |
| POST /v1/compress | Quantize content (AVU) |
| POST /v1/quality/all | Run quality gates |
| GET /v1/tokens/budget | Token budget |

**Token savings:** 97% vs MCP | **MCP is fallback only**

---

## ZERO TOLERANCE (9 Rules)

| # | Rule | Enforcement |
|---|------|-------------|
| 1 | Memory first | Block |
| 2 | No delete without approval | Block |
| 3 | No main commits | Block |
| 4 | No hallucination | Warn |
| 5 | Wait for user | Block |
| 6 | No stubs | Block |
| 7 | Source grounding | Warn |
| 8 | Save analysis output | Block |
| 9 | Code proximity in plans | Block |

Full: `.ideacode/memory/zero-tolerance.idc`

---

## HARD RULES

| Rule | Value |
|------|-------|
| MCP tools | `ideacode_*` only |
| Blocked | Write, mv, rm |
| Branches | feature/, bugfix/, hotfix/ |
| Test coverage | 90%+ |
| SOLID | All code |

---

## TECH STACK (Universal)

| Platform | Stack |
|----------|-------|
| Multiplatform | KMP (Kotlin Multiplatform) |
| Android | Compose + Material3 |
| iOS | SwiftUI |
| Desktop | Tauri (Rust + React) |
| Web | React + Tailwind |
| Glasses | MagicUI |

Override: `{project}/.ideacode/tech-stack.idc`

---

## LLM PROVIDERS

| Provider | Type | Default Model |
|----------|------|---------------|
| Ollama | Local | qwen2.5:7b |
| Anthropic | Cloud | claude-3.5-sonnet |
| OpenAI | Cloud | gpt-4o |
| OpenRouter | Gateway | multi-model |
| Groq | Fast | llama-3.1-70b |

Fallback: Ollama → Anthropic → OpenAI → OpenRouter → Groq

---

## COMMANDS

Core: /i.develop /i.fix /i.spec /i.plan /i.implement /i.createui /i.analyze
Project: /i.project /i.scan /i.document
Advanced: /i.swarm /i.refactor /i.research /i.design

Modifiers: .yolo .swarm .tdd .ood .solid .cot .tot

Full: /ideacode

---

## SKILLS (Global)

Location: `.ideacode/workflows/skills/`

| Skill | When |
|-------|------|
| developing-android | Android/Kotlin/Compose |
| developing-ios | iOS/Swift/SwiftUI |
| developing-react | React/TypeScript |
| developing-kmp | Kotlin Multiplatform |
| develop-feature | New features |
| fix-bug | Bug fixes |
| create-ui | UI work |

---

## FILE NAMING

| Type | Pattern |
|------|---------|
| Living docs | LD-{App}-{Module}-V#.md |
| Specs | App-Spec-Feature-YYMMDD-V#.md |
| Plans | App-Plan-Feature-YYMMDD-V#.md |
| Analysis | App-Analysis-Subject-YYMMDD-V#.md |
| Config | .ideacode/*.idc |

---

## IDC FORMAT

Compact config (71% smaller than YAML)
Codes: ZTL: STK: LLM: GAT: VRF: AGT: CMD: PTH: REG:
Separators: `:` fields, `+` multi-value, `,` lists

---

## QUALITY GATES

| Gate | Threshold |
|------|-----------|
| Code (SOLID) | 0 violations |
| Security | 0 issues |
| Coverage | 90%+ |
| Performance | <200ms |

API: `POST /v1/quality/all`

---

Author: Manoj Jhawar | Version: 13.0 | Updated: 2025-12-29
