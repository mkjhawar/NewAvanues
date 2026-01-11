# IDEACode v13.0 API Reference

**Base URL:** `http://localhost:3850`

---

## Health & Status

### GET /health

Health check endpoint.

**Response:**
```json
{
  "status": "healthy",
  "version": "13.0.0",
  "uptime": 3600,
  "port": 3850,
  "memory": {
    "tokens": 0,
    "loaded": []
  },
  "providers": {
    "ollama": true,
    "anthropic": false,
    "openai": false,
    "openrouter": false,
    "groq": false
  }
}
```

---

### GET /v1/status

Full status with detailed provider information.

**Response:**
```json
{
  "api": true,
  "version": "13.0.0",
  "uptime": 3600,
  "memory": {
    "loaded": [],
    "tokens": 0
  },
  "llm": {
    "primary": "ollama",
    "fallback": ["anthropic", "openai", "openrouter", "groq"],
    "providers": {
      "ollama": { "healthy": true, "models": ["qwen2.5:7b"] },
      "anthropic": { "healthy": false },
      "openai": { "healthy": false },
      "openrouter": { "healthy": false },
      "groq": { "healthy": false }
    }
  },
  "cache": {
    "session": 0,
    "search": 0,
    "response": 0
  }
}
```

---

## LLM Endpoints

### GET /v1/llm/status

LLM provider status.

**Response:**
```json
{
  "primary": "ollama",
  "fallback": ["anthropic", "openai", "openrouter", "groq"],
  "providers": {
    "ollama": { "healthy": true, "models": ["qwen2.5:7b", "llama3.2"] },
    "anthropic": { "healthy": false },
    "openai": { "healthy": false },
    "openrouter": { "healthy": false },
    "groq": { "healthy": false }
  }
}
```

---

### GET /v1/llm/models

List available models from all healthy providers.

**Response:**
```json
{
  "models": {
    "ollama": ["qwen2.5:7b", "llama3.2:3b", "codellama:7b"],
    "anthropic": ["claude-3-5-sonnet-20241022", "claude-3-haiku"],
    "openai": ["gpt-4o", "gpt-4o-mini"]
  }
}
```

---

### POST /v1/llm/complete

Generate completion with automatic fallback.

**Request:**
```json
{
  "prompt": "Explain TypeScript in one sentence.",
  "model": "qwen2.5:7b",
  "provider": "ollama",
  "temperature": 0.7,
  "max_tokens": 500,
  "system": "You are a helpful assistant.",
  "stop": ["\n\n"]
}
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| prompt | string | Yes | - | The prompt to complete |
| model | string | No | Provider default | Model to use |
| provider | string | No | Auto (fallback) | Specific provider |
| temperature | number | No | 0.7 | Randomness (0-2) |
| max_tokens | number | No | 1000 | Max response tokens |
| system | string | No | - | System prompt |
| stop | string[] | No | - | Stop sequences |

**Response:**
```json
{
  "content": "TypeScript is a typed superset of JavaScript that compiles to plain JavaScript.",
  "model": "qwen2.5:7b",
  "provider": "ollama",
  "usage": {
    "prompt_tokens": 15,
    "completion_tokens": 18,
    "total_tokens": 33
  },
  "finish_reason": "stop"
}
```

---

### POST /v1/llm/stream

Stream completion using Server-Sent Events.

**Request:** Same as `/v1/llm/complete`

**Response:** SSE stream
```
data: {"content":"Type","done":false}

data: {"content":"Script","done":false}

data: {"content":" is","done":false}

data: {"content":"...","done":false}

data: [DONE]
```

---

## Memory Endpoints

### GET /v1/memory/load

Load memory index.

**Response:**
```json
{
  "loaded": ["zero-tolerance.md", "memory-index.md"],
  "tokens": 150
}
```

---

### GET /v1/memory/load/:path

Load specific memory file.

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| path | string | File path to load |

**Example:** `GET /v1/memory/load/zero-tolerance.idc`

**Response:**
```json
{
  "path": "zero-tolerance.idc",
  "content": "ZTL:1:memory_first:block:Read memory index before ANY action\n...",
  "tokens": 45
}
```

---

### POST /v1/memory/save

Save content to memory.

**Request:**
```json
{
  "path": "session-notes.md",
  "content": "# Session Notes\n\nKey findings..."
}
```

**Response:**
```json
{
  "saved": true,
  "path": "session-notes.md",
  "tokens": 25
}
```

---

### DELETE /v1/memory/cache/session

Clear session cache.

**Response:**
```json
{
  "cleared": true
}
```

---

## Token Endpoints

### POST /v1/tokens/count

Count tokens in text.

**Request:**
```json
{
  "text": "Hello, world! This is a test."
}
```

**Response:**
```json
{
  "tokens": 9,
  "model": "cl100k_base"
}
```

---

### GET /v1/tokens/budget

Get token budget status.

**Response:**
```json
{
  "used": 0,
  "budget": 1000,
  "remaining": 1000
}
```

---

## Compression Endpoints

### POST /v1/compress

Compress content (AVU format).

**Request:**
```json
{
  "content": "function calculateTotal(items) { return items.reduce((sum, item) => sum + item.price, 0); }",
  "format": "avu"
}
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| content | string | Yes | - | Content to compress |
| format | string | No | "avu" | Compression format |

**Response:**
```json
{
  "compressed": "fn calculateTotal(items) { ret items.reduce((sum, item) => sum + item.price, 0); }",
  "original_tokens": 24,
  "compressed_tokens": 22,
  "reduction": "8%"
}
```

---

## Search Endpoints

### POST /v1/search

RAG semantic search.

**Request:**
```json
{
  "query": "How to handle authentication",
  "collection": "all",
  "limit": 5
}
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| query | string | Yes | - | Search query |
| collection | string | No | "all" | Collection to search |
| limit | number | No | 5 | Max results |

**Response:**
```json
{
  "results": [
    {
      "content": "Authentication is handled via JWT tokens...",
      "score": 0.92,
      "source": "docs/auth.md"
    }
  ],
  "tokens": 45,
  "collection": "all",
  "limit": 5
}
```

---

## Quality Gate Endpoints

### POST /v1/quality/all

Run all quality gates.

**Request:**
```json
{
  "path": "src/",
  "phase": "pre-commit"
}
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| path | string | No | - | Path to check |
| phase | string | No | "pre-commit" | Check phase |

**Response:**
```json
{
  "passed": true,
  "summary": {
    "code": "pass",
    "tests": "skip",
    "security": "pass",
    "performance": "skip",
    "docs": "skip"
  },
  "blockers": 0,
  "warnings": 0,
  "phase": "pre-commit",
  "path": "src/"
}
```

---

### POST /v1/quality/code

Run code quality checks.

**Request:**
```json
{
  "files": ["src/index.ts", "src/utils.ts"],
  "checks": ["solid", "duplication", "stubs"]
}
```

**Response:**
```json
{
  "passed": true,
  "violations": [],
  "files": ["src/index.ts", "src/utils.ts"],
  "checks": ["solid", "duplication", "stubs"]
}
```

---

### POST /v1/quality/security

Run security checks.

**Request:**
```json
{
  "files": ["src/"],
  "checks": ["secrets", "owasp"]
}
```

**Response:**
```json
{
  "passed": true,
  "issues": [],
  "files": ["src/"],
  "checks": ["secrets", "owasp"]
}
```

---

### POST /v1/quality/coverage

Check test coverage.

**Request:**
```json
{
  "path": "src/",
  "threshold": 90
}
```

**Response:**
```json
{
  "passed": true,
  "coverage": {
    "lines": 92,
    "branches": 88,
    "functions": 95
  },
  "path": "src/",
  "threshold": 90
}
```

---

### POST /v1/quality/performance

Run performance checks.

**Request:**
```json
{
  "benchmark": true,
  "iterations": 100
}
```

**Response:**
```json
{
  "passed": true,
  "results": {
    "health": { "p50": 5, "p95": 20, "p99": 50 },
    "api": { "p50": 50, "p95": 150, "p99": 200 }
  },
  "benchmark": true,
  "iterations": 100
}
```

---

## Error Responses

All endpoints return errors in this format:

```json
{
  "error": "Error message description"
}
```

**HTTP Status Codes:**

| Code | Meaning |
|------|---------|
| 200 | Success |
| 400 | Bad Request (missing/invalid params) |
| 404 | Not Found |
| 500 | Internal Server Error |

---

## Provider-Specific Models

### Ollama (Local)
| Model | Purpose |
|-------|---------|
| qwen2.5:7b | Default, general purpose |
| llama3.2:3b | Fast, lightweight |
| codellama:7b | Code generation |

### Anthropic
| Model | Purpose |
|-------|---------|
| claude-3-5-sonnet-20241022 | Default, best quality |
| claude-3-haiku-20240307 | Fast responses |

### OpenAI
| Model | Purpose |
|-------|---------|
| gpt-4o | Default, most capable |
| gpt-4o-mini | Fast, cost-effective |

### OpenRouter
| Model | Purpose |
|-------|---------|
| anthropic/claude-3.5-sonnet | Multi-provider access |
| meta-llama/llama-3.1-70b | Open source |

### Groq
| Model | Purpose |
|-------|---------|
| llama-3.1-70b-versatile | Ultra-fast inference |
| mixtral-8x7b-32768 | Long context |

---

## Rate Limits

No rate limits enforced locally. Cloud providers have their own limits:

| Provider | Rate Limit |
|----------|------------|
| Ollama | Unlimited (local) |
| Anthropic | Varies by plan |
| OpenAI | Varies by plan |
| OpenRouter | Varies by plan |
| Groq | ~30 req/min |

---

## cURL Examples

### Health Check
```bash
curl http://localhost:3850/health
```

### LLM Completion
```bash
curl -X POST http://localhost:3850/v1/llm/complete \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "What is TypeScript?",
    "max_tokens": 100
  }'
```

### Token Count
```bash
curl -X POST http://localhost:3850/v1/tokens/count \
  -H "Content-Type: application/json" \
  -d '{"text": "Hello, world!"}'
```

### Quality Check
```bash
curl -X POST http://localhost:3850/v1/quality/all \
  -H "Content-Type: application/json" \
  -d '{"path": "src/", "phase": "pre-commit"}'
```

---

**Author:** Manoj Jhawar
**Version:** 13.0.0
**Updated:** 2025-12-29
