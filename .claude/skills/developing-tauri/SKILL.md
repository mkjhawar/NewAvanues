---
name: developing-tauri
description: Develops cross-platform desktop apps with Tauri (Rust backend + web frontend). Use for native system access, IPC, window management, tray icons, and desktop app distribution.
---

# Tauri Development

## Tech Stack

| Component | Tech | Version |
|-----------|------|---------|
| Backend | Rust | 1.70+ |
| Frontend | Web (React/Vue/Svelte) | Any |
| Framework | Tauri | 2.0+ |
| Build | Cargo + npm | Latest |

## Structure

```
project/
├── src-tauri/
│   ├── src/
│   │   ├── main.rs     # Entry point
│   │   ├── lib.rs      # Commands
│   │   └── commands/   # IPC handlers
│   ├── Cargo.toml      # Rust deps
│   └── tauri.conf.json # Tauri config
├── src/                # Frontend
└── package.json        # Frontend deps
```

## IPC Patterns

| Pattern | Implementation |
|---------|----------------|
| Command | `#[tauri::command] fn cmd()` |
| Invoke | `invoke('cmd', { args })` |
| Events | `emit()` / `listen()` |
| State | `tauri::State<T>` |

## Rust Commands

```rust
#[tauri::command]
async fn read_file(path: String) -> Result<String, String> {
    std::fs::read_to_string(&path)
        .map_err(|e| e.to_string())
}
```

## Frontend Invoke

```typescript
import { invoke } from '@tauri-apps/api/core';
const content = await invoke<string>('read_file', { path: '/file.txt' });
```

## Security

| Rule | Implementation |
|------|----------------|
| CSP | Configure in tauri.conf.json |
| Allowlist | Explicit API permissions |
| IPC validation | Validate all inputs in Rust |

## Quality Gates

| Gate | Target |
|------|--------|
| Binary size | <10MB |
| Startup time | <500ms |
| Memory | <100MB idle |
