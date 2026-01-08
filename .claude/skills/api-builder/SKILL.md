---
name: api-builder
description: Build production REST/GraphQL APIs with LLM integration, RAG, tokenization, and AI features. Use when creating backend APIs, especially AI-powered ones.
---

# API Builder

## Tech Stack Options

| Framework | Language | Best For |
|-----------|----------|----------|
| FastAPI | Python | AI/ML APIs, async |
| Express/Fastify | TypeScript | Real-time, WebSockets |
| Ktor | Kotlin | KMP backends |
| Spring Boot | Kotlin/Java | Enterprise |

## REST Design

### Resource Naming
| Pattern | Example |
|---------|---------|
| Plural nouns | `/users`, `/documents` |
| Nested resources | `/users/{id}/documents` |
| Actions as sub-resources | `/documents/{id}/summarize` |

### HTTP Methods
| Method | Usage | Idempotent |
|--------|-------|------------|
| GET | Read | Yes |
| POST | Create | No |
| PUT | Replace | Yes |
| PATCH | Update | Yes |
| DELETE | Remove | Yes |

### Status Codes
| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 204 | No Content |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |
| 422 | Validation Error |
| 429 | Rate Limited |
| 500 | Server Error |

## LLM Integration

### Multi-Provider Pattern
```python
from abc import ABC, abstractmethod

class LLMProvider(ABC):
    @abstractmethod
    async def complete(self, prompt: str, **kwargs) -> str: ...

    @abstractmethod
    async def stream(self, prompt: str, **kwargs): ...

class OllamaProvider(LLMProvider):
    async def complete(self, prompt: str, model: str = "qwen2.5:7b"):
        async with httpx.AsyncClient() as client:
            response = await client.post(
                "http://localhost:11434/api/generate",
                json={"model": model, "prompt": prompt, "stream": False}
            )
            return response.json()["response"]

class OpenAIProvider(LLMProvider):
    async def complete(self, prompt: str, model: str = "gpt-4"):
        client = AsyncOpenAI()
        response = await client.chat.completions.create(
            model=model,
            messages=[{"role": "user", "content": prompt}]
        )
        return response.choices[0].message.content
```

### Fallback Chain
```python
class LLMRouter:
    def __init__(self):
        self.providers = [
            OllamaProvider(),      # Local, free
            AnthropicProvider(),   # Best quality
            OpenAIProvider(),      # Fallback
        ]

    async def complete(self, prompt: str) -> str:
        for provider in self.providers:
            try:
                if await provider.health_check():
                    return await provider.complete(prompt)
            except Exception as e:
                logger.warning(f"{provider.__class__.__name__} failed: {e}")
        raise Exception("No healthy providers")
```

## Tokenization

### Token Counting
```python
import tiktoken

def count_tokens(text: str, model: str = "gpt-4") -> int:
    encoding = tiktoken.encoding_for_model(model)
    return len(encoding.encode(text))

def truncate_to_tokens(text: str, max_tokens: int) -> str:
    encoding = tiktoken.encoding_for_model("gpt-4")
    tokens = encoding.encode(text)
    if len(tokens) <= max_tokens:
        return text
    return encoding.decode(tokens[:max_tokens])
```

### Token Budget Management
```python
class TokenBudget:
    def __init__(self, max_tokens: int = 4096):
        self.max_tokens = max_tokens
        self.used = 0

    def can_fit(self, text: str) -> bool:
        return self.used + count_tokens(text) <= self.max_tokens

    def add(self, text: str) -> bool:
        tokens = count_tokens(text)
        if self.used + tokens <= self.max_tokens:
            self.used += tokens
            return True
        return False

    @property
    def remaining(self) -> int:
        return self.max_tokens - self.used
```

## RAG (Retrieval Augmented Generation)

### Vector Store Setup
```python
import chromadb
from sentence_transformers import SentenceTransformer

class RAGStore:
    def __init__(self, collection_name: str = "documents"):
        self.client = chromadb.PersistentClient(path="./rag_data")
        self.collection = self.client.get_or_create_collection(collection_name)
        self.embedder = SentenceTransformer("all-MiniLM-L6-v2")

    def add_documents(self, docs: list[str], ids: list[str], metadata: list[dict] = None):
        embeddings = self.embedder.encode(docs).tolist()
        self.collection.add(
            documents=docs,
            embeddings=embeddings,
            ids=ids,
            metadatas=metadata
        )

    def search(self, query: str, n_results: int = 5) -> list[dict]:
        query_embedding = self.embedder.encode([query]).tolist()
        results = self.collection.query(
            query_embeddings=query_embedding,
            n_results=n_results
        )
        return [
            {"document": doc, "metadata": meta, "distance": dist}
            for doc, meta, dist in zip(
                results["documents"][0],
                results["metadatas"][0],
                results["distances"][0]
            )
        ]
```

### RAG Pipeline
```python
class RAGPipeline:
    def __init__(self, store: RAGStore, llm: LLMRouter):
        self.store = store
        self.llm = llm

    async def query(self, question: str, n_context: int = 5) -> str:
        # Retrieve relevant documents
        context_docs = self.store.search(question, n_results=n_context)

        # Build context string
        context = "\n\n".join([
            f"[Source: {doc['metadata'].get('source', 'unknown')}]\n{doc['document']}"
            for doc in context_docs
        ])

        # Generate response with context
        prompt = f"""Answer based on the following context:

{context}

Question: {question}

Answer:"""

        return await self.llm.complete(prompt)
```

## Quantization & Compression

### Content Compression
```python
def compress_content(text: str, format: str = "avu") -> str:
    """Compress content to reduce tokens."""
    if format == "avu":
        # AVU: Abbreviate common patterns
        replacements = {
            "function": "fn",
            "return": "ret",
            "const": "c",
            "import": "imp",
            "export": "exp",
        }
        for old, new in replacements.items():
            text = text.replace(old, new)
        # Remove redundant whitespace
        text = " ".join(text.split())
    return text

def quantize_embeddings(embeddings: list[float], bits: int = 8) -> bytes:
    """Reduce embedding precision for storage efficiency."""
    import numpy as np
    arr = np.array(embeddings)
    # Normalize to 0-255 range for 8-bit
    min_val, max_val = arr.min(), arr.max()
    normalized = ((arr - min_val) / (max_val - min_val) * 255).astype(np.uint8)
    return normalized.tobytes()
```

### Response Streaming
```python
from fastapi import FastAPI
from fastapi.responses import StreamingResponse

app = FastAPI()

@app.post("/v1/chat/stream")
async def stream_chat(request: ChatRequest):
    async def generate():
        async for chunk in llm.stream(request.prompt):
            yield f"data: {json.dumps({'content': chunk})}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(
        generate(),
        media_type="text/event-stream"
    )
```

## Authentication

### JWT Pattern
```python
from jose import jwt, JWTError
from datetime import datetime, timedelta

SECRET_KEY = os.getenv("JWT_SECRET")
ALGORITHM = "HS256"

def create_token(user_id: str, expires_delta: timedelta = timedelta(hours=24)) -> str:
    expire = datetime.utcnow() + expires_delta
    return jwt.encode(
        {"sub": user_id, "exp": expire},
        SECRET_KEY,
        algorithm=ALGORITHM
    )

def verify_token(token: str) -> str:
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        return payload["sub"]
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")
```

### API Key Pattern
```python
from fastapi import Security, HTTPException
from fastapi.security import APIKeyHeader

api_key_header = APIKeyHeader(name="X-API-Key")

async def verify_api_key(api_key: str = Security(api_key_header)):
    if not await is_valid_key(api_key):
        raise HTTPException(status_code=401, detail="Invalid API key")
    return api_key
```

## Validation (Pydantic)

```python
from pydantic import BaseModel, Field, validator

class CompletionRequest(BaseModel):
    prompt: str = Field(..., min_length=1, max_length=10000)
    model: str = Field(default="gpt-4")
    max_tokens: int = Field(default=1000, ge=1, le=4096)
    temperature: float = Field(default=0.7, ge=0, le=2)

    @validator("prompt")
    def validate_prompt(cls, v):
        if count_tokens(v) > 8000:
            raise ValueError("Prompt exceeds token limit")
        return v

class CompletionResponse(BaseModel):
    content: str
    tokens_used: int
    model: str
    cached: bool = False
```

## Error Handling (RFC 7807)

```python
from fastapi import HTTPException
from fastapi.responses import JSONResponse

class ProblemDetail(BaseModel):
    type: str = "about:blank"
    title: str
    status: int
    detail: str
    instance: str = None

@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    return JSONResponse(
        status_code=exc.status_code,
        content=ProblemDetail(
            title=exc.detail,
            status=exc.status_code,
            detail=str(exc.detail),
            instance=str(request.url)
        ).dict()
    )
```

## Rate Limiting

```python
from slowapi import Limiter
from slowapi.util import get_remote_address

limiter = Limiter(key_func=get_remote_address)

@app.post("/v1/complete")
@limiter.limit("100/minute")
async def complete(request: Request, body: CompletionRequest):
    return await llm.complete(body.prompt)
```

## Caching

```python
import hashlib
from functools import lru_cache

class ResponseCache:
    def __init__(self, redis_url: str = None):
        self.redis = redis.from_url(redis_url) if redis_url else None
        self.local = {}

    def key(self, prompt: str, model: str) -> str:
        return hashlib.sha256(f"{model}:{prompt}".encode()).hexdigest()

    async def get(self, prompt: str, model: str) -> str | None:
        k = self.key(prompt, model)
        if self.redis:
            return await self.redis.get(k)
        return self.local.get(k)

    async def set(self, prompt: str, model: str, response: str, ttl: int = 3600):
        k = self.key(prompt, model)
        if self.redis:
            await self.redis.setex(k, ttl, response)
        else:
            self.local[k] = response
```

## OpenAPI Documentation

```python
from fastapi import FastAPI

app = FastAPI(
    title="AI API",
    version="1.0.0",
    description="AI-powered API with LLM, RAG, and embeddings",
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json"
)
```

## Full Example Endpoint

```python
@app.post("/v1/rag/query", response_model=RAGResponse)
@limiter.limit("50/minute")
async def rag_query(
    request: Request,
    body: RAGRequest,
    api_key: str = Security(verify_api_key)
):
    # Check cache
    cached = await cache.get(body.query, "rag")
    if cached:
        return RAGResponse(answer=cached, cached=True, tokens=0)

    # RAG pipeline
    answer = await rag_pipeline.query(
        question=body.query,
        n_context=body.context_count
    )

    # Cache and return
    await cache.set(body.query, "rag", answer)
    return RAGResponse(
        answer=answer,
        cached=False,
        tokens=count_tokens(answer)
    )
```

## Dependencies

```bash
# Python
pip install fastapi uvicorn pydantic python-jose tiktoken
pip install chromadb sentence-transformers  # RAG
pip install openai anthropic httpx          # LLM providers
pip install slowapi redis                   # Rate limiting, caching
```
