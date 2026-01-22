# CodeAvenue Terminal App - Dual-API Architecture
ver: V1 | date: 260122 | status: draft

## Overview
Design for CodeAvenue Desktop terminal app with hybrid API integration supporting both:
1. **Node.js API** (existing, localhost:3850) - complex LLM/Playwright operations
2. **Rust Core** (new, embedded) - protected core logic, fast operations

## Current State Analysis

### Existing App Structure
```
codeavenue-desktop/
├── src-tauri/src/
│   ├── lib.rs          # App entry, Tauri setup
│   ├── terminal.rs     # PTY management (portable_pty)
│   ├── coordination.rs # File-based IPC (local only)
│   ├── health.rs       # Health monitoring
│   ├── agent.rs        # Agent backend abstraction
│   └── settings.rs     # Settings management
└── src/
    ├── App.tsx         # React frontend
    ├── store/          # Zustand state
    └── components/     # UI components
```

### Current Gaps
| area | current | needed |
|------|---------|--------|
| RAG search | none | API integration |
| Memory system | none | API integration |
| Chat history | none | API integration |
| Quality checks | none | API integration |
| LLM routing | agent.rs (basic) | Full provider support |
| Coordination | file-based only | API + file hybrid |

## Target Architecture

### High-Level Design
```
┌─────────────────────────────────────────────────────────────────────┐
│                    CodeAvenue Desktop App                           │
├─────────────────────────────────────────────────────────────────────┤
│                         React Frontend                              │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐      │
│  │Terminal │ │Terminal │ │Terminal │ │Consensus│ │Settings │      │
│  │  Grid   │ │ Output  │ │ Input   │ │  Panel  │ │  Modal  │      │
│  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘      │
│       └───────────┴───────────┴───────────┴───────────┘            │
│                              │                                      │
│                    ┌─────────▼─────────┐                           │
│                    │   Zustand Store    │                           │
│                    │ (terminals, state) │                           │
│                    └─────────┬─────────┘                           │
├──────────────────────────────┼──────────────────────────────────────┤
│                    ┌─────────▼─────────┐                           │
│                    │   API Adapter      │ ← Abstraction layer       │
│                    │  (chooses source)  │                           │
│                    └────┬─────────┬────┘                           │
│                         │         │                                 │
│          ┌──────────────▼───┐ ┌───▼──────────────┐                 │
│          │   Tauri IPC      │ │  HTTP Client     │                 │
│          │ (Rust commands)  │ │ (Node.js API)    │                 │
│          └────────┬─────────┘ └────────┬─────────┘                 │
├───────────────────┼────────────────────┼────────────────────────────┤
│                   │                    │                            │
│   ┌───────────────▼────────────────┐   │                           │
│   │         Rust Core              │   │                           │
│   │  (Protected, Compiled)         │   │                           │
│   │  ┌──────────┐ ┌──────────┐    │   │                           │
│   │  │ Terminal │ │ Coord.   │    │   │                           │
│   │  │ (PTY)    │ │ (local)  │    │   │                           │
│   │  └──────────┘ └──────────┘    │   │                           │
│   │  ┌──────────┐ ┌──────────┐    │   │                           │
│   │  │ Health   │ │ Settings │    │   │                           │
│   │  └──────────┘ └──────────┘    │   │                           │
│   │  ┌──────────┐ ┌──────────┐    │   │    ┌──────────────────┐   │
│   │  │ SQLite   │ │ Quality  │    │   │    │  Node.js API     │   │
│   │  │ (cipher) │ │ (tree-s) │    │◄──┼───►│  localhost:3850  │   │
│   │  └──────────┘ └──────────┘    │   │    │  (LLM, Inspect)  │   │
│   │  ┌──────────┐ ┌──────────┐    │   │    └──────────────────┘   │
│   │  │ Git Ops  │ │ Config   │    │   │                           │
│   │  └──────────┘ └──────────┘    │   │                           │
│   └────────────────────────────────┘   │                           │
└────────────────────────────────────────┴────────────────────────────┘
```

## API Routing Strategy

### Source Selection Matrix
| operation | primary | fallback | reason |
|-----------|---------|----------|--------|
| Terminal PTY | Rust | - | Native only |
| File locking | Rust | Node API | Fast, local |
| Health monitor | Rust | - | Real-time |
| RAG search | Rust | Node API | Protected |
| Memory ops | Rust | Node API | Protected |
| Chat history | Rust | Node API | Protected |
| Quality checks | Rust | Node API | tree-sitter |
| Git worktree | Rust | Node API | git2 fast |
| LLM completion | Node API | Rust HTTP | SDK support |
| LLM streaming | Node API | Rust HTTP | SDK support |
| Visual inspect | Node API | - | Playwright |
| A11y testing | Node API | - | Playwright |

### API Adapter Implementation
```typescript
// src/services/ApiAdapter.ts

type ApiSource = 'rust' | 'node' | 'auto';

interface ApiAdapterConfig {
  preferRust: boolean;
  nodeApiUrl: string;
  timeout: number;
}

class ApiAdapter {
  private config: ApiAdapterConfig;
  private rustAvailable: boolean = true;
  private nodeAvailable: boolean = false;

  constructor(config: ApiAdapterConfig) {
    this.config = config;
    this.checkAvailability();
  }

  private async checkAvailability() {
    // Check Rust (always available in Tauri)
    this.rustAvailable = true;

    // Check Node.js API
    try {
      const res = await fetch(`${this.config.nodeApiUrl}/health`);
      this.nodeAvailable = res.ok;
    } catch {
      this.nodeAvailable = false;
    }
  }

  // Route to appropriate source
  async call<T>(
    operation: string,
    params: any,
    preferredSource: ApiSource = 'auto'
  ): Promise<T> {
    const source = this.resolveSource(operation, preferredSource);

    if (source === 'rust') {
      return this.callRust(operation, params);
    } else {
      return this.callNode(operation, params);
    }
  }

  private resolveSource(operation: string, preferred: ApiSource): 'rust' | 'node' {
    // Operations that MUST use specific source
    const rustOnly = ['terminal_spawn', 'terminal_write', 'terminal_kill'];
    const nodeOnly = ['llm_stream', 'inspect_visual', 'inspect_a11y'];

    if (rustOnly.includes(operation)) return 'rust';
    if (nodeOnly.includes(operation)) return 'node';

    // Auto-select based on availability and preference
    if (preferred === 'rust' && this.rustAvailable) return 'rust';
    if (preferred === 'node' && this.nodeAvailable) return 'node';

    // Default: Rust if available, else Node
    return this.rustAvailable ? 'rust' : 'node';
  }

  private async callRust<T>(operation: string, params: any): Promise<T> {
    const { invoke } = await import('@tauri-apps/api/core');
    return invoke(operation, params);
  }

  private async callNode<T>(operation: string, params: any): Promise<T> {
    const endpoint = this.operationToEndpoint(operation);
    const res = await fetch(`${this.config.nodeApiUrl}${endpoint}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params),
    });
    return res.json();
  }

  private operationToEndpoint(op: string): string {
    const map: Record<string, string> = {
      'rag_search': '/rag/search',
      'memory_store': '/memory/memorize',
      'memory_retrieve': '/memory/retrieve',
      'chat_create': '/chat/sessions',
      'llm_complete': '/llm/complete',
      'quality_check': '/quality/check',
      'coordination_lock': '/coordination/lock',
    };
    return map[op] || `/${op.replace('_', '/')}`;
  }
}

export const api = new ApiAdapter({
  preferRust: true,
  nodeApiUrl: 'http://localhost:3850',
  timeout: 30000,
});
```

## Rust Core Extensions

### New Modules to Add
```
src-tauri/src/
├── lib.rs              # Updated with new managers
├── terminal.rs         # Existing (keep)
├── coordination.rs     # Enhanced (API sync)
├── health.rs           # Existing (keep)
├── agent.rs            # Existing (keep)
├── settings.rs         # Existing (keep)
├── api/                # NEW: API layer
│   ├── mod.rs
│   ├── client.rs       # HTTP client for Node API
│   └── sync.rs         # Sync between Rust/Node
├── db/                 # NEW: Database layer
│   ├── mod.rs
│   ├── pool.rs         # Connection pool
│   ├── rag.rs          # RAG operations
│   ├── memory.rs       # Memory operations
│   └── chat.rs         # Chat operations
├── quality/            # NEW: Code quality
│   ├── mod.rs
│   └── analyzer.rs     # tree-sitter analysis
├── git/                # NEW: Git operations
│   ├── mod.rs
│   └── worktree.rs     # Worktree management
└── protection/         # NEW: Code protection
    ├── mod.rs
    ├── config.rs       # Encrypted config
    └── strings.rs      # Protected strings
```

### Enhanced lib.rs
```rust
// src-tauri/src/lib.rs

mod terminal;
mod agent;
mod coordination;
mod health;
mod settings;
mod api;        // NEW
mod db;         // NEW
mod quality;    // NEW
mod git;        // NEW
mod protection; // NEW

use tauri::Manager;

pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .setup(|app| {
            // Existing managers
            app.manage(terminal::TerminalManager::new());
            app.manage(coordination::CoordinationManager::new());
            app.manage(health::HealthMonitor::new());
            app.manage(settings::SettingsManager::new()?);

            // NEW managers
            app.manage(db::DatabaseManager::new()?);
            app.manage(api::NodeApiClient::new("http://localhost:3850"));
            app.manage(quality::QualityAnalyzer::new());
            app.manage(git::GitManager::new());

            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            // Existing commands...

            // NEW: Database commands
            db::rag_search,
            db::rag_index,
            db::memory_store,
            db::memory_retrieve,
            db::chat_create_session,
            db::chat_add_message,

            // NEW: Quality commands
            quality::analyze_file,
            quality::check_security,

            // NEW: Git commands
            git::create_worktree,
            git::list_worktrees,
            git::close_worktree,

            // NEW: API sync commands
            api::sync_with_node,
            api::check_node_status,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
```

## Coordination Enhancement

### Dual-Mode Coordination
```rust
// src-tauri/src/coordination.rs (enhanced)

pub struct CoordinationManager {
    // Existing fields...
    session: Arc<Mutex<Option<SessionFile>>>,
    session_path: Arc<Mutex<Option<PathBuf>>>,

    // NEW: API client for remote sync
    api_client: Option<Arc<api::NodeApiClient>>,
    sync_mode: SyncMode,
}

#[derive(Clone)]
pub enum SyncMode {
    LocalOnly,        // File-based only (offline)
    RemoteOnly,       // API only (no local file)
    Hybrid,           // Both (default)
}

impl CoordinationManager {
    pub fn new_with_api(client: Arc<api::NodeApiClient>) -> Self {
        Self {
            session: Arc::new(Mutex::new(None)),
            session_path: Arc::new(Mutex::new(None)),
            watcher: Arc::new(Mutex::new(None)),
            api_client: Some(client),
            sync_mode: SyncMode::Hybrid,
        }
    }

    /// Register terminal - local + API
    pub async fn register_terminal(&self, reg: TerminalRegistration) -> Result<(), String> {
        // Local registration
        self.register_local(&reg)?;

        // API registration (if available)
        if let Some(client) = &self.api_client {
            if let Err(e) = client.register_terminal(&reg).await {
                tracing::warn!("API registration failed: {}", e);
                // Continue - local is sufficient
            }
        }

        Ok(())
    }

    /// Acquire lock - check API first, then local
    pub async fn acquire_lock(&self, path: &str, terminal_id: &str) -> Result<bool, String> {
        // Check API first (authoritative)
        if let Some(client) = &self.api_client {
            match client.check_lock(path).await {
                Ok(Some(lock)) if lock.terminal_id != terminal_id => {
                    return Ok(false); // Locked by another
                }
                _ => {}
            }
        }

        // Local lock
        let acquired = self.acquire_local(path, terminal_id)?;

        // Sync to API
        if acquired {
            if let Some(client) = &self.api_client {
                let _ = client.acquire_lock(path, terminal_id).await;
            }
        }

        Ok(acquired)
    }
}
```

## Frontend Store Updates

### Enhanced Zustand Store
```typescript
// src/store/terminalStore.ts (enhanced)

import { api } from '../services/ApiAdapter';

interface TerminalState {
  terminals: Map<string, Terminal>;
  activeTerminal: string | null;
  consensusGroups: Map<string, ConsensusGroup>;

  // NEW: API state
  apiStatus: {
    rust: 'online' | 'offline';
    node: 'online' | 'offline' | 'checking';
  };

  // NEW: Data caches
  ragCache: SearchResult[];
  memoryCache: Memory[];
  chatSessions: ChatSession[];

  // Actions
  spawnTerminal: (config: SpawnConfig) => Promise<string>;
  searchRAG: (query: string) => Promise<SearchResult[]>;
  storeMemory: (content: string, category: string) => Promise<void>;
}

export const useTerminalStore = create<TerminalState>((set, get) => ({
  terminals: new Map(),
  activeTerminal: null,
  consensusGroups: new Map(),

  apiStatus: {
    rust: 'online',
    node: 'checking',
  },

  ragCache: [],
  memoryCache: [],
  chatSessions: [],

  // Spawn terminal (Rust only)
  spawnTerminal: async (config) => {
    const id = await api.call<string>('spawn_terminal', config, 'rust');
    set((state) => ({
      terminals: new Map(state.terminals).set(id, {
        id,
        ...config,
        status: 'running',
      }),
    }));
    return id;
  },

  // RAG search (Rust preferred, Node fallback)
  searchRAG: async (query) => {
    const results = await api.call<SearchResult[]>('rag_search', {
      query,
      limit: 20,
    }, 'auto');
    set({ ragCache: results });
    return results;
  },

  // Store memory (Rust preferred)
  storeMemory: async (content, category) => {
    await api.call('memory_store', { content, category }, 'auto');
  },
}));
```

## Data Synchronization

### Sync Strategy
```
┌─────────────────────────────────────────────────────────────┐
│                    Data Flow                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Write Path:                                                │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐               │
│  │ Frontend│───►│Rust Core│───►│ SQLite  │               │
│  └─────────┘    └────┬────┘    └─────────┘               │
│                      │                                      │
│                      ▼ async sync                           │
│                 ┌─────────┐                                 │
│                 │Node API │ (if available)                  │
│                 └─────────┘                                 │
│                                                             │
│  Read Path:                                                 │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐               │
│  │ Frontend│◄───│Rust Core│◄───│ SQLite  │               │
│  └─────────┘    └─────────┘    └─────────┘               │
│       │                                                     │
│       │ fallback (if Rust unavailable)                     │
│       ▼                                                     │
│  ┌─────────┐                                                │
│  │Node API │                                                │
│  └─────────┘                                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Sync Implementation
```rust
// src-tauri/src/api/sync.rs

use crate::db::DatabaseManager;
use tokio::time::{interval, Duration};

pub struct SyncManager {
    db: Arc<DatabaseManager>,
    api_client: Arc<NodeApiClient>,
    sync_interval: Duration,
}

impl SyncManager {
    /// Start background sync task
    pub fn start_background_sync(&self) {
        let db = self.db.clone();
        let client = self.api_client.clone();
        let interval_duration = self.sync_interval;

        tokio::spawn(async move {
            let mut tick = interval(interval_duration);

            loop {
                tick.tick().await;

                // Sync RAG documents
                if let Err(e) = sync_rag(&db, &client).await {
                    tracing::warn!("RAG sync failed: {}", e);
                }

                // Sync memory
                if let Err(e) = sync_memory(&db, &client).await {
                    tracing::warn!("Memory sync failed: {}", e);
                }

                // Sync coordination state
                if let Err(e) = sync_coordination(&db, &client).await {
                    tracing::warn!("Coordination sync failed: {}", e);
                }
            }
        });
    }
}

async fn sync_rag(db: &DatabaseManager, client: &NodeApiClient) -> Result<(), SyncError> {
    // Get local changes since last sync
    let local_changes = db.rag_changes_since_sync().await?;

    // Push to Node API
    for doc in local_changes {
        client.rag_index(&doc).await?;
    }

    // Pull remote changes
    let remote_changes = client.rag_changes_since(db.last_sync_time()).await?;
    for doc in remote_changes {
        db.rag_upsert(&doc).await?;
    }

    db.update_sync_time().await?;
    Ok(())
}
```

## Migration Path

### Phase 1: API Adapter (Week 1)
| task | effort | outcome |
|------|--------|---------|
| Create ApiAdapter service | 2d | Abstraction layer |
| Add Node API health check | 0.5d | Availability detection |
| Update store to use adapter | 1d | Unified data access |
| Test dual-source routing | 0.5d | Verified fallback |

### Phase 2: Rust Database (Week 2)
| task | effort | outcome |
|------|--------|---------|
| Add rusqlite to Cargo.toml | 0.5d | Dependency |
| Create db module structure | 1d | Module skeleton |
| Implement RAG operations | 1.5d | Search/index |
| Implement memory operations | 1d | Store/retrieve |
| Implement chat operations | 1d | Sessions/messages |

### Phase 3: Quality & Git (Week 3)
| task | effort | outcome |
|------|--------|---------|
| Add tree-sitter dependency | 0.5d | AST parsing |
| Implement quality analyzer | 2d | Code checks |
| Add git2 dependency | 0.5d | Git operations |
| Implement worktree ops | 1.5d | Create/close |
| Test quality checks | 0.5d | Verified output |

### Phase 4: Protection & Sync (Week 4)
| task | effort | outcome |
|------|--------|---------|
| Add obfstr for strings | 0.5d | Encrypted literals |
| Implement config encryption | 1d | Protected config |
| Build sync manager | 2d | Rust↔Node sync |
| Integration testing | 1.5d | Full flow verified |

## UI Components

### API Status Indicator
```tsx
// src/components/ApiStatus.tsx

export function ApiStatus() {
  const { apiStatus } = useTerminalStore();

  return (
    <div className="flex gap-2 text-xs">
      <StatusDot
        status={apiStatus.rust}
        label="Core"
      />
      <StatusDot
        status={apiStatus.node}
        label="API"
      />
    </div>
  );
}

function StatusDot({ status, label }: { status: string; label: string }) {
  const color = {
    online: 'bg-green-500',
    offline: 'bg-red-500',
    checking: 'bg-yellow-500',
  }[status];

  return (
    <div className="flex items-center gap-1">
      <div className={`w-2 h-2 rounded-full ${color}`} />
      <span>{label}</span>
    </div>
  );
}
```

### RAG Search Panel
```tsx
// src/components/RAGSearch.tsx

export function RAGSearch() {
  const [query, setQuery] = useState('');
  const { searchRAG, ragCache } = useTerminalStore();

  const handleSearch = async () => {
    await searchRAG(query);
  };

  return (
    <div className="p-4">
      <div className="flex gap-2">
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search documentation..."
          className="flex-1 px-3 py-2 border rounded"
        />
        <button onClick={handleSearch}>Search</button>
      </div>

      <div className="mt-4 space-y-2">
        {ragCache.map((result) => (
          <div key={result.id} className="p-2 border rounded">
            <div className="font-medium">{result.title}</div>
            <div className="text-sm text-gray-600">{result.summary}</div>
            <div className="text-xs text-gray-400">{result.path}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
```

## Configuration

### Tauri Config Updates
```json
// src-tauri/tauri.conf.json
{
  "app": {
    "security": {
      "csp": "default-src 'self'; connect-src 'self' http://localhost:3850"
    }
  },
  "bundle": {
    "resources": [
      "config/config.enc",
      "config/schema.sql"
    ]
  }
}
```

### Environment Config
```typescript
// src/config/env.ts

export const config = {
  nodeApiUrl: import.meta.env.VITE_NODE_API_URL || 'http://localhost:3850',
  preferRust: import.meta.env.VITE_PREFER_RUST !== 'false',
  syncInterval: parseInt(import.meta.env.VITE_SYNC_INTERVAL || '30000'),
};
```

## Success Metrics

| metric | target |
|--------|--------|
| Rust operations | < 10ms response |
| Node fallback | < 100ms detection |
| Sync latency | < 5s lag |
| Offline capability | Full terminal function |
| Memory usage | < 150MB total |
| Binary size | < 60MB |

---
Author: Manoj Jhawar | v1 | 260122
