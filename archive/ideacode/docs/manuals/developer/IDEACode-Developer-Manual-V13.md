# IDEACode v13.0 Developer Manual

## Architecture Overview

IDEACode is a unified API gateway providing multi-provider LLM access, RAG semantic search, memory management, and quality gates.

```
┌─────────────────────────────────────────────────────────────┐
│                    IDEACode API Server                      │
│                       Port 3850                             │
├─────────────────────────────────────────────────────────────┤
│  Express Middleware: Helmet | CORS | Compression | JSON     │
├─────────────────────────────────────────────────────────────┤
│                       Route Handlers                        │
│  /health | /v1/llm/* | /v1/memory/* | /v1/quality/*        │
├─────────────────────────────────────────────────────────────┤
│                       LLM Router                            │
│           (Fallback: Ollama → Anthropic → OpenAI →         │
│                    OpenRouter → Groq)                       │
├─────────────────────────────────────────────────────────────┤
│  Ollama   │  Anthropic  │  OpenAI  │ OpenRouter │  Groq    │
│  (local)  │   (cloud)   │ (cloud)  │  (gateway) │ (fast)   │
└───────────┴─────────────┴──────────┴────────────┴──────────┘
```

---

## Project Structure

```
/Volumes/M-Drive/Coding/ideacode/
├── src/
│   ├── index.ts              # Entry point
│   ├── server/
│   │   └── index.ts          # Express server (IDEACodeServer class)
│   ├── llm/
│   │   ├── index.ts          # LLM exports
│   │   ├── router/
│   │   │   └── index.ts      # LLMRouter (fallback chain)
│   │   └── providers/
│   │       ├── base.ts       # ILLMProvider interface
│   │       ├── index.ts      # Provider exports
│   │       ├── ollama/       # OllamaProvider
│   │       ├── anthropic/    # AnthropicProvider
│   │       ├── openai/       # OpenAIProvider
│   │       ├── openrouter/   # OpenRouterProvider
│   │       └── groq/         # GroqProvider
│   └── types/
│       ├── index.ts          # Type exports
│       └── llm.ts            # LLM type definitions
├── dist/                     # Compiled JavaScript
├── data/
│   └── rag.db               # RAG database (SQLite)
├── docs/
│   └── manuals/             # Documentation
├── package.json
└── tsconfig.json
```

---

## Core Classes

### IDEACodeServer

Main server class handling all HTTP endpoints.

```typescript
// src/server/index.ts
export class IDEACodeServer {
  private app: Express;
  private llmRouter: LLMRouter;
  private startTime: number;

  constructor();
  start(): void;
  getApp(): Express;  // For testing
}
```

**Key Methods:**
| Method | Route | Purpose |
|--------|-------|---------|
| healthHandler | GET /health | Server health + provider status |
| llmCompleteHandler | POST /v1/llm/complete | LLM completion |
| llmStreamHandler | POST /v1/llm/stream | SSE streaming |
| searchHandler | POST /v1/search | RAG semantic search |
| qualityAllHandler | POST /v1/quality/all | Run all quality gates |

---

### LLMRouter

Manages multiple LLM providers with automatic fallback.

```typescript
// src/llm/router/index.ts
export class LLMRouter {
  private providers: Map<LLMProviderType, ILLMProvider>;
  private config: LLMRouterConfig;
  private healthCache: Map<LLMProviderType, CachedHealth>;

  constructor(config?: Partial<LLMRouterConfig>);

  // Core methods
  complete(request: LLMCompletionRequest): Promise<LLMCompletionResponse>;
  stream(request: LLMCompletionRequest): AsyncIterable<LLMStreamChunk>;

  // Health management
  healthCheckAll(): Promise<Map<LLMProviderType, LLMProviderHealth>>;
  getFirstHealthyProvider(): Promise<ILLMProvider | null>;

  // Utilities
  countTokens(text: string): Promise<number>;
  listAllModels(): Promise<Map<LLMProviderType, string[]>>;
  getStatus(): Promise<RouterStatus>;
}
```

**Fallback Chain:**
1. Ollama (local, free, fastest)
2. Anthropic (cloud, best quality)
3. OpenAI (cloud, widely available)
4. OpenRouter (multi-model gateway)
5. Groq (ultra-fast inference)

---

### ILLMProvider Interface

All providers implement this interface:

```typescript
// src/llm/providers/base.ts
export interface ILLMProvider {
  type: LLMProviderType;

  healthCheck(): Promise<LLMProviderHealth>;
  complete(request: LLMCompletionRequest): Promise<LLMCompletionResponse>;
  stream(request: LLMCompletionRequest): AsyncIterable<LLMStreamChunk>;
  listModels(): Promise<string[]>;
  countTokens(text: string): Promise<number>;
}
```

---

## Type Definitions

### LLM Types

```typescript
// src/types/llm.ts
export type LLMProviderType =
  | 'ollama'
  | 'anthropic'
  | 'openai'
  | 'openrouter'
  | 'groq';

export interface LLMCompletionRequest {
  prompt: string;
  model?: string;
  provider?: LLMProviderType;
  temperature?: number;
  max_tokens?: number;
  system?: string;
  stop?: string[];
}

export interface LLMCompletionResponse {
  content: string;
  model: string;
  provider: LLMProviderType;
  usage: {
    prompt_tokens: number;
    completion_tokens: number;
    total_tokens: number;
  };
  finish_reason: string;
}

export interface LLMStreamChunk {
  content: string;
  done: boolean;
}

export interface LLMProviderHealth {
  healthy: boolean;
  latency?: number;
  models?: string[];
  error?: string;
}

export interface LLMRouterConfig {
  primary: LLMProviderType;
  fallback: LLMProviderType[];
  timeout: number;
  retries: number;
}
```

---

## Adding a New Provider

1. **Create provider directory:**
   ```
   src/llm/providers/newprovider/
   └── index.ts
   ```

2. **Implement ILLMProvider:**
   ```typescript
   // src/llm/providers/newprovider/index.ts
   import type { ILLMProvider, ... } from '../../types/llm.js';

   export class NewProvider implements ILLMProvider {
     type: LLMProviderType = 'newprovider' as LLMProviderType;

     async healthCheck(): Promise<LLMProviderHealth> {
       // Check if provider is reachable
     }

     async complete(request: LLMCompletionRequest): Promise<LLMCompletionResponse> {
       // Implement completion
     }

     async *stream(request: LLMCompletionRequest): AsyncIterable<LLMStreamChunk> {
       // Implement streaming
     }

     async listModels(): Promise<string[]> {
       // Return available models
     }

     async countTokens(text: string): Promise<number> {
       // Token counting
     }
   }
   ```

3. **Register in router:**
   ```typescript
   // src/llm/router/index.ts
   import { NewProvider } from '../providers/newprovider/index.js';

   private initializeProviders(): void {
     // ... existing providers
     this.providers.set('newprovider', new NewProvider());
   }
   ```

4. **Update types:**
   ```typescript
   // src/types/llm.ts
   export type LLMProviderType =
     | 'ollama' | 'anthropic' | 'openai'
     | 'openrouter' | 'groq' | 'newprovider';
   ```

---

## API Endpoint Implementation

### Adding a New Endpoint

1. **Define handler in server:**
   ```typescript
   // src/server/index.ts
   private newFeatureHandler(req: Request, res: Response): void {
     const { param1, param2 } = req.body;

     // Validate input
     if (!param1) {
       res.status(400).json({ error: 'param1 required' });
       return;
     }

     // Process
     const result = this.processFeature(param1, param2);

     // Return
     res.json(result);
   }
   ```

2. **Register route:**
   ```typescript
   private setupRoutes(): void {
     // ... existing routes
     this.app.post('/v1/feature/new', this.newFeatureHandler.bind(this));
   }
   ```

---

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `IDEACODE_PORT` | 3850 | Server port |
| `OLLAMA_HOST` | localhost:11434 | Ollama server URL |
| `OPENAI_API_KEY` | - | OpenAI API key |
| `ANTHROPIC_API_KEY` | - | Anthropic API key |
| `OPENROUTER_API_KEY` | - | OpenRouter API key |
| `GROQ_API_KEY` | - | Groq API key |

### IDC Configuration

Global config: `/Volumes/M-Drive/Coding/.ideacode/config.idc`

```
VER:ideacode:13.0.0
API:port:3850
LLM:primary:ollama:local
LLM:fallback:anthropic+openai+openrouter+groq:cloud
GAT:coverage:90:true
GAT:solid:0:true
```

---

## Development Workflow

### Setup

```bash
cd /Volumes/M-Drive/Coding/ideacode
npm install
npm run build
```

### Development Mode

```bash
npm run dev    # tsx watch - hot reload
```

### Production Mode

```bash
npm run build  # Compile TypeScript
npm run start  # Run compiled JS
```

### Testing

```bash
npm test                # Run tests
npm run test:coverage   # With coverage
```

### Linting

```bash
npm run lint       # ESLint
npm run typecheck  # TypeScript check
```

---

## Error Handling

### Provider Errors

LLMRouter handles provider failures with automatic fallback:

```typescript
try {
  return await provider.complete(request);
} catch (error) {
  // Invalidate health cache
  this.healthCache.delete(providerType);
  console.warn(`Provider ${providerType} failed: ${error.message}`);
  // Continue to next provider
}
```

### HTTP Errors

Express error middleware handles unhandled errors:

```typescript
this.app.use((err: Error, _req: Request, res: Response, _next: NextFunction) => {
  console.error('Server error:', err);
  res.status(500).json({ error: err.message });
});
```

---

## Testing Guidelines

### Unit Tests

```typescript
// src/llm/router/index.test.ts
import { describe, it, expect } from 'vitest';
import { LLMRouter } from './index.js';

describe('LLMRouter', () => {
  it('should fall back on provider failure', async () => {
    const router = new LLMRouter();
    const result = await router.complete({ prompt: 'test' });
    expect(result.content).toBeDefined();
  });
});
```

### Integration Tests

```typescript
// src/server/index.test.ts
import { describe, it, expect } from 'vitest';
import request from 'supertest';
import { createServer } from './index.js';

describe('API Server', () => {
  const server = createServer();

  it('GET /health returns healthy', async () => {
    const res = await request(server.getApp()).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('healthy');
  });
});
```

---

## Performance Considerations

### Health Caching

Provider health is cached for 60 seconds to avoid repeated checks:

```typescript
private readonly healthCacheTTL = 60000; // 1 minute
```

### Request Compression

Compression middleware reduces response size:

```typescript
this.app.use(compression());
```

### Token Estimation

Fallback token counting when no provider available:

```typescript
return Math.ceil(text.length / 4);  // ~4 chars per token
```

---

## Security

### Middleware Stack

1. **Helmet** - Security headers
2. **CORS** - Cross-origin (permissive for local dev)
3. **JSON limit** - 10MB max request size

### API Key Handling

API keys stored in environment variables only:

```bash
export OPENAI_API_KEY="sk-..."
export ANTHROPIC_API_KEY="sk-ant-..."
```

---

## Dependencies

| Package | Version | Purpose |
|---------|---------|---------|
| express | ^4.18.2 | HTTP server |
| cors | ^2.8.5 | CORS middleware |
| helmet | ^7.1.0 | Security headers |
| compression | ^1.7.4 | Response compression |
| better-sqlite3 | ^9.4.3 | RAG database |
| zod | ^3.22.4 | Schema validation |
| @anthropic-ai/sdk | ^0.20.0 | Anthropic provider |
| openai | ^4.28.0 | OpenAI provider |
| ollama | ^0.5.0 | Ollama provider |

---

**Author:** Manoj Jhawar
**Version:** 13.0.0
**Updated:** 2025-12-29
