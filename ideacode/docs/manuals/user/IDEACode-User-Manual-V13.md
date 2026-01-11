# IDEACode v13.0 User Manual

## Overview

IDEACode is a unified AI development framework that provides:
- **Multi-provider LLM** - Local (Ollama) and cloud (OpenAI, Anthropic, OpenRouter, Groq)
- **API Gateway** - Single endpoint on port 3850
- **IDC Config Format** - 71% smaller than YAML
- **Zero Tolerance Rules** - 9 mandatory rules for AI agents
- **Tech Stack Management** - Universal defaults with per-project overrides

---

## Quick Start

### 1. Start the API Server

```bash
cd /Volumes/M-Drive/Coding/ideacode
npm run dev
```

### 2. Verify Health

```bash
curl http://localhost:3850/health
```

Expected response:
```json
{
  "status": "healthy",
  "version": "13.0.0",
  "port": 3850,
  "providers": {
    "ollama": true,
    "openai": false,
    "anthropic": false
  }
}
```

### 3. Session Start (AI Agents)

AI agents must follow this sequence:
1. Check API: `curl http://localhost:3850/health`
2. Read: `.ideacode/memory/zero-tolerance.idc`
3. Read: `.ideacode/memory/memory-index.idc`
4. Read: `.ideacode/tech-stack.idc`
5. Acknowledge: "Loaded: [files], API: ✓, ~X tokens"

---

## API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/health` | GET | Health check |
| `/v1/status` | GET | Full status |
| `/v1/llm/status` | GET | LLM providers |
| `/v1/llm/models` | GET | Available models |
| `/v1/llm/complete` | POST | Generate completion |
| `/v1/llm/stream` | POST | Stream completion |
| `/v1/memory/load` | GET | Load memory index |
| `/v1/tokens/count` | POST | Count tokens |
| `/v1/tokens/budget` | GET | Token budget |
| `/v1/compress` | POST | Compress content (AVU) |
| `/v1/search` | POST | RAG search |
| `/v1/quality/all` | POST | Run quality gates |

---

## LLM Providers

### Priority Order (Fallback Chain)

1. **Ollama** (Local) - Free, no API key needed
2. **Anthropic** (Cloud) - Best quality
3. **OpenAI** (Cloud) - Widely available
4. **OpenRouter** (Gateway) - Multi-model access
5. **Groq** (Fast) - Ultra-fast inference

### Default Models

| Provider | Default Model |
|----------|---------------|
| Ollama | qwen2.5:7b |
| Anthropic | claude-3-5-sonnet-20241022 |
| OpenAI | gpt-4o |
| OpenRouter | anthropic/claude-3.5-sonnet |
| Groq | llama-3.1-70b-versatile |

### Configuration

Set API keys in environment:
```bash
export OPENAI_API_KEY="sk-..."
export ANTHROPIC_API_KEY="sk-ant-..."
export OPENROUTER_API_KEY="sk-or-..."
export GROQ_API_KEY="gsk_..."
```

---

## Zero Tolerance Rules

These 9 rules are **MANDATORY** for all AI agents:

| # | Rule | Enforcement | Description |
|---|------|-------------|-------------|
| 1 | Memory First | Block | Read memory index before ANY action |
| 2 | No Delete | Block | Never delete without approval + pros/cons |
| 3 | No Main Commits | Block | Never commit to main/master |
| 4 | No Hallucination | Warn | Never invent APIs, files, URLs |
| 5 | Wait for User | Block | Stop after questions, wait for answer |
| 6 | No Stubs | Block | No TODO, pass, placeholders |
| 7 | Source Grounding | Warn | Read file before answering about it |
| 8 | Save Analysis | Block | Save /i.analyze output to docs/ |
| 9 | Code Proximity | Block | Organize plans by file proximity |

---

## Commands

### Core Commands

| Command | Purpose |
|---------|---------|
| `/i.develop` | Full feature development workflow |
| `/i.fix` | Bug fixing workflow |
| `/i.spec` | Create specification |
| `/i.plan` | Create implementation plan |
| `/i.implement` | Execute plan |
| `/i.createui` | UI creation workflow |
| `/i.analyze` | Code analysis |

### Project Commands

| Command | Purpose |
|---------|---------|
| `/i.project` | Project operations |
| `/i.scan` | Scan project structure |
| `/i.document` | Generate documentation |

### Advanced Commands

| Command | Purpose |
|---------|---------|
| `/i.swarm` | Multi-agent coordination |
| `/i.refactor` | Guided refactoring |
| `/i.research` | Web research |
| `/i.design` | Product design workflow |

### Modifiers

| Modifier | Effect |
|----------|--------|
| `.yolo` | Autonomous mode (no questions) |
| `.swarm` | Multi-agent execution |
| `.tdd` | Test-driven development |
| `.ood` | Object-oriented design |
| `.solid` | SOLID principles enforcement |
| `.cot` | Chain of thought reasoning |
| `.tot` | Tree of thought reasoning |

---

## Tech Stack

### Universal Defaults

| Platform | Stack |
|----------|-------|
| Multiplatform | KMP (Kotlin Multiplatform) |
| Android | Compose + Material3 |
| iOS | SwiftUI |
| Desktop | Tauri (Rust + React) |
| Web | React + Tailwind |
| Glasses | MagicUI |

### Override

Create `{project}/.ideacode/tech-stack.idc` to override defaults.

---

## Quality Gates

| Gate | Threshold |
|------|-----------|
| Code (SOLID) | 0 violations |
| Security | 0 issues |
| Test Coverage | 90%+ |
| Performance | <200ms |

API: `POST /v1/quality/all`

---

## File Naming Conventions

| Type | Pattern |
|------|---------|
| Living docs | `LD-{App}-{Module}-V#.md` |
| Specs | `App-Spec-Feature-YYMMDD-V#.md` |
| Plans | `App-Plan-Feature-YYMMDD-V#.md` |
| Analysis | `App-Analysis-Subject-YYMMDD-V#.md` |
| Config | `.ideacode/*.idc` |

---

## Folder Structure

```
/Volumes/M-Drive/Coding/
├── ideacode/              # IDEACode API server
├── ideacode-org/          # Original ideacode (archived)
├── .ideacode/             # Global config
│   ├── config.idc         # Global configuration
│   ├── tech-stack.idc     # Tech stack defaults
│   ├── memory/            # Memory system
│   │   ├── zero-tolerance.idc
│   │   └── memory-index.idc
│   ├── adapters/          # Agent adapters
│   └── scripts/           # Utility scripts
└── .claude/               # Claude-specific config
    └── CLAUDE.md          # Claude instructions
```

---

## Troubleshooting

### API Not Starting

```bash
# Check if port is in use
lsof -i :3850

# Kill existing process
kill <PID>

# Start fresh
cd /Volumes/M-Drive/Coding/ideacode
npm run dev
```

### Ollama Not Detected

```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Start Ollama
ollama serve
```

### Missing Dependencies

```bash
cd /Volumes/M-Drive/Coding/ideacode
npm install
npm run build
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 13.0.0 | 2025-12-29 | Unified API (port 3850), multi-provider LLM, IDC format |
| 12.4.0 | 2025-12-29 | Previous version (ideacode-org) |

---

**Author:** Manoj Jhawar
**License:** Proprietary
**Updated:** 2025-12-29
