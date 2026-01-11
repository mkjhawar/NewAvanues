# IDEACode v13.0 Quick Start Guide

Get up and running with IDEACode in under 5 minutes.

---

## Prerequisites

- Node.js 18+
- npm
- (Optional) Ollama for local LLM

---

## 1. Start the API Server

```bash
cd /Volumes/M-Drive/Coding/ideacode
npm install    # First time only
npm run dev    # Development mode
```

Expected output:
```
╔════════════════════════════════════════════════╗
║  IDEACode API Server v13.0.0                   ║
║  Port: 3850                                    ║
║  Status: Running                               ║
╚════════════════════════════════════════════════╝
```

---

## 2. Verify Health

```bash
curl http://localhost:3850/health
```

Response:
```json
{
  "status": "healthy",
  "version": "13.0.0",
  "port": 3850,
  "providers": { "ollama": true }
}
```

---

## 3. AI Agent Session Start

Every AI agent session must begin with:

```
1. curl http://localhost:3850/health
2. Read .ideacode/memory/zero-tolerance.idc
3. Read .ideacode/memory/memory-index.idc
4. Read .ideacode/tech-stack.idc
5. Acknowledge: "Loaded: [files], API: ✓, ~X tokens"
```

---

## 4. Core Commands

| Command | Purpose |
|---------|---------|
| `/i.develop` | Full feature development |
| `/i.fix` | Bug fixing |
| `/i.spec` | Create specification |
| `/i.plan` | Create implementation plan |
| `/i.implement` | Execute plan |
| `/i.analyze` | Code analysis |

### Modifiers

| Modifier | Effect |
|----------|--------|
| `.yolo` | Autonomous mode |
| `.tdd` | Test-driven development |
| `.swarm` | Multi-agent |

### Example

```
/i.develop .yolo "Add dark mode toggle"
```

---

## 5. Zero Tolerance Rules

**MANDATORY** for all AI agents:

| Rule | Description |
|------|-------------|
| Memory First | Read memory index before ANY action |
| No Delete | Never delete without approval |
| No Main Commits | Never commit to main/master |
| No Hallucination | Never invent APIs, files, URLs |
| Wait for User | Stop after questions |
| No Stubs | No TODO or placeholders |

---

## 6. API Quick Reference

### LLM Completion

```bash
curl -X POST http://localhost:3850/v1/llm/complete \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Hello, world!"}'
```

### Token Count

```bash
curl -X POST http://localhost:3850/v1/tokens/count \
  -H "Content-Type: application/json" \
  -d '{"text": "Count my tokens"}'
```

### Quality Check

```bash
curl -X POST http://localhost:3850/v1/quality/all \
  -H "Content-Type: application/json" \
  -d '{"path": "src/"}'
```

---

## 7. Tech Stack Defaults

| Platform | Stack |
|----------|-------|
| Android | Compose + Material3 |
| iOS | SwiftUI |
| Web | React + Tailwind |
| Desktop | Tauri |
| Multiplatform | KMP |

---

## 8. LLM Providers

| Provider | Type | API Key |
|----------|------|---------|
| Ollama | Local | None needed |
| Anthropic | Cloud | `ANTHROPIC_API_KEY` |
| OpenAI | Cloud | `OPENAI_API_KEY` |
| OpenRouter | Gateway | `OPENROUTER_API_KEY` |
| Groq | Fast | `GROQ_API_KEY` |

Fallback chain: Ollama → Anthropic → OpenAI → OpenRouter → Groq

---

## 9. File Structure

```
/Volumes/M-Drive/Coding/
├── ideacode/              # API Server
├── .ideacode/             # Global Config
│   ├── config.idc
│   ├── tech-stack.idc
│   └── memory/
│       ├── zero-tolerance.idc
│       └── memory-index.idc
└── .claude/               # Claude Config
    └── CLAUDE.md
```

---

## 10. Troubleshooting

### API not starting?

```bash
lsof -i :3850        # Check port
kill <PID>           # Free port
npm run dev          # Restart
```

### Ollama not detected?

```bash
ollama serve         # Start Ollama
curl localhost:11434 # Verify
```

### Missing dependencies?

```bash
cd /Volumes/M-Drive/Coding/ideacode
npm install
npm run build
```

---

## Next Steps

1. Read the [User Manual](IDEACode-User-Manual-V13.md)
2. Explore the [API Reference](../../api/IDEACode-API-Reference-V13.md)
3. Learn the [IDC Format](../../specifications/IDC-Format-Specification-V13.md)

---

**Questions?** File issues at the project repository.

**Author:** Manoj Jhawar | **Version:** 13.0.0 | **Updated:** 2025-12-29
