# CodeAvenue Rust API - Implementation Plan
ver: V1 | date: 260122 | status: draft

## Overview
Convert CodeAvenue API from Node.js/Express to Rust with code protection, maintaining compatibility with existing clients while enabling Tauri desktop integration.

## Goals
| goal | priority | notes |
|------|----------|-------|
| code protection | P0 | compiled binary, encrypted strings |
| performance | P1 | faster than Node.js baseline |
| compatibility | P1 | existing API contracts preserved |
| tauri integration | P1 | native IPC for desktop app |
| maintainability | P2 | clean architecture, testable |

## Architecture

### Hybrid Design
```
┌─────────────────────────────────────────────────────────┐
│                   Tauri Desktop App                     │
│  ┌─────────────────────────────────────────────────┐   │
│  │              React Frontend (UI)                 │   │
│  └──────────────────────┬──────────────────────────┘   │
│                         │ Tauri IPC                     │
│  ┌──────────────────────▼──────────────────────────┐   │
│  │              Rust Core (Protected)               │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────────┐    │   │
│  │  │ axum API │ │ SQLite   │ │ Coordination │    │   │
│  │  │  :3850   │ │ (cipher) │ │   Service    │    │   │
│  │  └──────────┘ └──────────┘ └──────────────┘    │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────────┐    │   │
│  │  │ Git Ops  │ │ Quality  │ │ Config/Auth  │    │   │
│  │  │ (git2)   │ │(tree-sit)│ │  (encrypted) │    │   │
│  │  └──────────┘ └──────────┘ └──────────────┘    │   │
│  └──────────────────────┬──────────────────────────┘   │
│                         │ Sidecar IPC                   │
│  ┌──────────────────────▼──────────────────────────┐   │
│  │           Node.js Sidecar (Optional)             │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────────┐    │   │
│  │  │ LLM SDKs │ │Playwright│ │ Complex Xfrm │    │   │
│  │  └──────────┘ └──────────┘ └──────────────┘    │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### Standalone Mode
```
┌─────────────────────────────────────────────────────────┐
│              Rust API Server (Standalone)               │
│  ┌─────────────────────────────────────────────────┐   │
│  │              axum HTTP Server :3850              │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────────┐    │   │
│  │  │ RAG/Chat │ │ Memory   │ │ Coordination │    │   │
│  │  └──────────┘ └──────────┘ └──────────────┘    │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────────┐    │   │
│  │  │ Quality  │ │ Worktree │ │ LLM (HTTP)   │    │   │
│  │  └──────────┘ └──────────┘ └──────────────┘    │   │
│  └─────────────────────────────────────────────────┘   │
│  Config: encrypted file + env vars                      │
└─────────────────────────────────────────────────────────┘
```

## Module Migration Plan

### Phase 1: Core Infrastructure (Week 1)
| module | effort | deps | notes |
|--------|--------|------|-------|
| project setup | 2d | - | cargo workspace, CI |
| config system | 1d | obfstr | encrypted config loader |
| database layer | 2d | rusqlite/sqlcipher | connection pool, migrations |
| error handling | 1d | thiserror | unified error types |
| logging | 0.5d | tracing | structured logging |

### Phase 2: Data Services (Week 2)
| module | effort | deps | notes |
|--------|--------|------|-------|
| RAG service | 2d | rusqlite | search, index, CRUD |
| memory service | 2d | rusqlite | 3-layer system |
| chat service | 1.5d | rusqlite | sessions, messages |
| ADR service | 1d | rusqlite | decision records |
| summary service | 1d | rusqlite | code summaries |

### Phase 3: Operations (Week 3)
| module | effort | deps | notes |
|--------|--------|------|-------|
| coordination | 2d | tokio | file locking, terminals |
| worktree | 1.5d | git2 | git operations |
| quality checks | 2d | tree-sitter | AST analysis |
| safeguard | 0.5d | - | deletion protection |

### Phase 4: Integration (Week 4)
| module | effort | deps | notes |
|--------|--------|------|-------|
| LLM routing | 2d | reqwest | HTTP to providers |
| axum server | 2d | axum | REST endpoints |
| tauri commands | 1d | tauri | IPC bindings |
| sidecar bridge | 1d | - | Node.js communication |

## Crate Structure

```
codeavenue-api/
├── Cargo.toml              # Workspace root
├── crates/
│   ├── ca-core/            # Shared types, errors, config
│   │   ├── src/
│   │   │   ├── lib.rs
│   │   │   ├── config.rs   # Encrypted config
│   │   │   ├── error.rs    # Error types
│   │   │   └── types.rs    # Shared types
│   │   └── Cargo.toml
│   ├── ca-db/              # Database layer
│   │   ├── src/
│   │   │   ├── lib.rs
│   │   │   ├── pool.rs     # Connection pool
│   │   │   ├── migrations/ # SQL migrations
│   │   │   ├── rag.rs      # RAG queries
│   │   │   ├── memory.rs   # Memory queries
│   │   │   ├── chat.rs     # Chat queries
│   │   │   └── adr.rs      # ADR queries
│   │   └── Cargo.toml
│   ├── ca-services/        # Business logic
│   │   ├── src/
│   │   │   ├── lib.rs
│   │   │   ├── rag.rs      # RAG service
│   │   │   ├── memory.rs   # Memory service
│   │   │   ├── chat.rs     # Chat service
│   │   │   ├── coordination.rs
│   │   │   ├── worktree.rs
│   │   │   ├── quality.rs
│   │   │   └── llm.rs      # LLM routing
│   │   └── Cargo.toml
│   ├── ca-server/          # HTTP server
│   │   ├── src/
│   │   │   ├── main.rs     # Entry point
│   │   │   ├── routes/     # axum routes
│   │   │   └── middleware/ # Auth, logging
│   │   └── Cargo.toml
│   └── ca-tauri/           # Tauri integration
│       ├── src/
│       │   ├── lib.rs
│       │   └── commands.rs # Tauri IPC commands
│       └── Cargo.toml
└── config/
    ├── config.enc          # Encrypted config
    └── schema.sql          # DB schema
```

## Dependencies (Cargo.toml)

```toml
[workspace]
members = ["crates/*"]

[workspace.dependencies]
# Async runtime
tokio = { version = "1.35", features = ["full"] }

# Web framework
axum = "0.7"
tower = "0.4"
tower-http = { version = "0.5", features = ["cors", "trace"] }

# Database
rusqlite = { version = "0.31", features = ["bundled"] }
# sqlcipher alternative for encryption:
# libsqlite3-sys = { version = "0.28", features = ["sqlcipher"] }

# Serialization
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"

# Git operations
git2 = "0.18"

# Code analysis
tree-sitter = "0.22"
tree-sitter-typescript = "0.21"
tree-sitter-rust = "0.21"
tree-sitter-python = "0.21"

# HTTP client (for LLM APIs)
reqwest = { version = "0.11", features = ["json", "stream"] }

# Protection
obfstr = "0.4"

# Error handling
thiserror = "1.0"
anyhow = "1.0"

# Logging
tracing = "0.1"
tracing-subscriber = { version = "0.3", features = ["env-filter"] }

# Tauri
tauri = { version = "2.0", features = ["api-all"] }
```

## Protection Implementation

### 1. Encrypted Config
```rust
// ca-core/src/config.rs
use obfstr::obfstr;
use std::fs;

pub struct AppConfig {
    pub db_path: String,
    pub api_port: u16,
    pub llm_providers: Vec<LlmProvider>,
    // Sensitive fields decrypted at runtime
}

impl AppConfig {
    pub fn load() -> Result<Self, ConfigError> {
        // Key derived from hardware ID + embedded secret
        let key = derive_key(
            &get_hardware_id(),
            obfstr!("embedded-salt-here").as_bytes(),
        );

        let encrypted = fs::read("config.enc")?;
        let decrypted = decrypt_aes256(&encrypted, &key)?;

        serde_json::from_slice(&decrypted)
            .map_err(ConfigError::Parse)
    }
}
```

### 2. Protected Strings
```rust
// ca-services/src/llm.rs
use obfstr::obfstr;

fn get_system_prompt() -> &'static str {
    obfstr!(r#"You are a coding assistant. Follow these rules:
1. Write clean, maintainable code
2. Follow project conventions
..."#)
}

fn get_default_instructions() -> &'static str {
    // LLMI instructions encrypted at compile time
    obfstr!(include_str!("../../config/instructions.txt"))
}
```

### 3. Build Configuration
```toml
# Cargo.toml
[profile.release]
opt-level = 3
lto = true
codegen-units = 1
panic = "abort"
strip = true

[profile.release.package."*"]
opt-level = 3
```

### 4. Anti-Debug (Optional)
```rust
// ca-core/src/protection.rs
#[cfg(not(debug_assertions))]
pub fn check_environment() -> Result<(), SecurityError> {
    // Check for debugger
    #[cfg(target_os = "macos")]
    {
        use std::process::Command;
        let output = Command::new("sysctl")
            .args(["kern.proc.pid", &std::process::id().to_string()])
            .output()?;
        // Parse and check P_TRACED flag
    }

    // Check for known analysis tools
    // Verify binary integrity
    Ok(())
}
```

## API Endpoint Mapping

### Preserved Endpoints (100% compatible)
| endpoint | method | rust handler |
|----------|--------|--------------|
| /health | GET | health::check |
| /status | GET | health::status |
| /rag/search | POST | rag::search |
| /rag/index | POST | rag::index |
| /rag/documents | GET | rag::list |
| /rag/documents/:id | GET/DELETE | rag::get/delete |
| /memory/memorize | POST | memory::store |
| /memory/retrieve | POST | memory::retrieve |
| /memory/search | POST | memory::search |
| /chat/sessions | GET/POST | chat::sessions |
| /chat/sessions/:id | GET/PATCH/DELETE | chat::session |
| /chat/sessions/:id/messages | GET/POST | chat::messages |
| /coordination/register | POST | coord::register |
| /coordination/lock | POST | coord::lock |
| /coordination/unlock | POST | coord::unlock |
| /coordination/status | GET | coord::status |
| /quality/check | POST | quality::check |
| /quality/security | POST | quality::security |
| /worktree/create | POST | worktree::create |
| /worktree/list | GET | worktree::list |
| /worktree/close | POST | worktree::close |
| /llm/complete | POST | llm::complete |
| /llm/stream | POST | llm::stream |

### New Tauri-Only Commands
| command | purpose |
|---------|---------|
| ca_health | Internal health (no HTTP) |
| ca_rag_search | Direct RAG search |
| ca_terminal_spawn | PTY management |
| ca_consensus_start | Consensus session |
| ca_file_lock | Coordination |

## Migration Strategy

### Phase 1: Parallel Operation
```
┌─────────────┐     ┌─────────────┐
│ Node.js API │     │  Rust API   │
│   :3850     │     │   :3851     │
└──────┬──────┘     └──────┬──────┘
       │                   │
       └─────────┬─────────┘
                 │
         ┌───────▼───────┐
         │    Clients    │
         │ (test both)   │
         └───────────────┘
```

### Phase 2: Feature Parity Testing
```bash
# Run compatibility tests
./scripts/api-compat-test.sh

# Compare responses
curl localhost:3850/health > node-health.json
curl localhost:3851/health > rust-health.json
diff node-health.json rust-health.json
```

### Phase 3: Cutover
```
┌─────────────┐
│  Rust API   │
│   :3850     │ ← Primary
└──────┬──────┘
       │
┌──────▼──────┐
│   Clients   │
└─────────────┘

Node.js: deprecated, sidecar only
```

## Testing Plan

### Unit Tests
```rust
#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn test_rag_search() {
        let db = setup_test_db().await;
        let service = RagService::new(db);

        // Index document
        service.index(Document {
            path: "/test.md".into(),
            content: "test content".into(),
            ..Default::default()
        }).await.unwrap();

        // Search
        let results = service.search("test", 10).await.unwrap();
        assert_eq!(results.len(), 1);
    }
}
```

### Integration Tests
```rust
#[tokio::test]
async fn test_api_endpoints() {
    let app = create_test_app().await;

    let response = app
        .oneshot(Request::get("/health").body(Body::empty()).unwrap())
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::OK);
}
```

### Compatibility Tests
```bash
#!/bin/bash
# scripts/api-compat-test.sh

ENDPOINTS=(
    "GET /health"
    "GET /status"
    "GET /rag/stats"
    "GET /coordination/status"
)

for ep in "${ENDPOINTS[@]}"; do
    METHOD=$(echo $ep | cut -d' ' -f1)
    PATH=$(echo $ep | cut -d' ' -f2)

    NODE_RESP=$(curl -s -X $METHOD localhost:3850$PATH)
    RUST_RESP=$(curl -s -X $METHOD localhost:3851$PATH)

    if [ "$NODE_RESP" != "$RUST_RESP" ]; then
        echo "MISMATCH: $ep"
        diff <(echo "$NODE_RESP" | jq .) <(echo "$RUST_RESP" | jq .)
    fi
done
```

## Deliverables

| deliverable | format | location |
|-------------|--------|----------|
| rust api binary | executable | dist/codeavenue-api |
| tauri library | rust crate | crates/ca-tauri |
| db migrations | SQL | config/migrations/ |
| encrypted config | binary | config/config.enc |
| api docs | markdown | docs/api/ |
| compat tests | bash/rust | scripts/, tests/ |

## Risks & Mitigations

| risk | impact | mitigation |
|------|--------|------------|
| LLM SDK gaps | high | HTTP fallback, sidecar |
| tree-sitter coverage | medium | prioritize TS/Rust/Python |
| sqlcipher complexity | medium | start with rusqlite, add later |
| timeline slip | medium | phase deliverables |

## Success Criteria

| metric | target |
|--------|--------|
| API compatibility | 100% endpoint parity |
| Response time | <= Node.js baseline |
| Binary size | < 50MB (stripped) |
| Memory usage | < 100MB idle |
| Test coverage | > 80% |
| String extraction | 0 plain secrets |

---
Author: Manoj Jhawar | v1 | 260122
