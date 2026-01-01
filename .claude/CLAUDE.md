# IDEACode v13.0 - Global Instructions

Scope: ALL projects in /Volumes/M-Drive/Coding/

---

## SESSION START (MANDATORY)

1. **Master Index:** Read `.ideacode/MASTER-INDEX.md`
2. **API Check:** `curl http://localhost:3850/health`
3. **Project Config:** Read `{project}/.ideacode/config.idc`
4. **Acknowledge:** "Loaded: [files], API: ✓"

---

## FOLDER STRUCTURE

```
/Volumes/M-Drive/Coding/           # Global root
├── .claude/
│   ├── CLAUDE.md                  # Global instructions
│   ├── commands/i.*.md            # Slash commands
│   └── skills/{name}/SKILL.md     # 32 skills
├── .ideacode/
│   ├── MASTER-INDEX.md            # Single source
│   ├── config.idc, tech-stack.idc
│   └── memory/*.idc
└── ideacode/                      # API server :3850

{project}/                         # Per-project
├── .ideacode/config.idc           # Overrides
├── specs/App-Spec-*-YYMMDD-V#.md
├── plans/App-Plan-*-YYMMDD-V#.md
├── docs/LD-{App}-{Module}-V#.md
└── src/
```

---

## SKILLS (32)

**Docs:** pdf, docx, xlsx, pptx
**Platform:** developing-android, developing-ios, developing-kmp, developing-kotlin, developing-react, developing-tauri, developing-web
**Workflow:** develop-feature, fix-bug, create-spec, create-ui, tdd-development, ood-development, verification
**Tools:** api-builder, mcp-builder, webapp-testing, managing-cloud-storage, implementing-webrtc
**Design:** theme-factory, algorithmic-art
**Meta:** skill-creator, analyze-code, analyzing-security, reviewing-code, running-tests, writing-documentation, building-html-artifacts

---

## API SERVER (:3850)

| Endpoint | Purpose |
|----------|---------|
| /health | Status |
| /v1/llm/complete | LLM completion |
| /v1/llm/stream | SSE streaming |
| /v1/search | RAG search |
| /v1/compress | AVU quantize |
| /v1/tokens/count | Token count |
| /v1/quality/all | Quality gates |

---

## ZERO TOLERANCE

| Rule | Action |
|------|--------|
| Memory first | Block |
| No delete w/o approval | Block |
| No main commits | Block |
| No hallucination | Warn |
| No stubs | Block |

---

## TECH STACK

KMP:Multiplatform | Compose+M3:Android | SwiftUI:iOS | Tauri:Desktop | React+TW+shadcn:Web

---

## LLM FALLBACK

Ollama → Anthropic → OpenAI → OpenRouter → Groq

---

## FILE NAMING

LD-{App}-{Module}-V#.md | App-Spec-*-YYMMDD-V#.md | App-Plan-*-YYMMDD-V#.md | *.idc

---

## COMMANDS

/i.develop /i.fix /i.spec /i.plan /i.implement /i.createui /i.analyze /i.swarm

Modifiers: .yolo .swarm .tdd .ood .solid

---

Author: Manoj Jhawar | v13.0 | 2025-12-31 | 32 skills
