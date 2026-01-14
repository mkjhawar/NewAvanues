---
description: System utilities | /i.system {operation}
---

# /i.system - System Utilities

## Usage
`/i.system {operation}`

## Operations

| Operation | Purpose | Alias |
|-----------|---------|-------|
| `registry` | Manage file/folder registries | - |
| `reload` | Reload IDEACODE framework | - |
| `version` | Show framework version | - |
| `update` | Check for framework updates | - |

## Examples
```bash
/i.system registry          # Manage registries
/i.system reload            # Reload framework
/i.system version           # Show version
/i.system update            # Check updates
```

## Modifiers
| Modifier | Effect |
|----------|--------|
| `.yolo` | Auto-apply updates |
| `.cot` | Show update reasoning |

## MCP Integration
Uses: `ideacode_context`, `ideacode_validate`
