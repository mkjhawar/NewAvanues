---
description: MCP server management (per-repo) | /i.mcp status | /i.mcp start | /i.mcp stop
---

# Task: MCP Server Management

Manage MCP server per repository/terminal. MCP does NOT auto-load (API server does).

---

## Commands

| Command | Action |
|---------|--------|
| `/i.mcp status` | Show if MCP running + server name |
| `/i.mcp start` | Start MCP server for this repo |
| `/i.mcp stop` | Stop MCP server for this terminal |
| `/i.mcp restart` | Restart MCP server |
| `/i.mcp config` | Show MCP configuration |

---

## Status Check

**Execution:**
1. Check if MCP server process running
2. Read server name from `.claude/mcp.json`
3. Show status + name

**Output:**
```
MCP Server Status
─────────────────
Running: YES
Server: ideacode-mcp
Port: Auto-assigned
Config: .claude/mcp.json
PID: 12345
```

**If not running:**
```
MCP Server Status
─────────────────
Running: NO
Config: .claude/mcp.json exists
Start: /i.mcp start
```

---

## Start MCP Server

**Execution:**
1. Read `.claude/mcp.json` for server config
2. Start server process for this terminal only
3. Save PID to temp file (terminal-specific)
4. Update statusline

**Output:**
```
Starting MCP server...
✓ Server: ideacode-mcp
✓ Config: .claude/mcp.json
✓ Started (PID: 12345)
✓ Scope: This terminal only

Statusline updated: ... | MCP:ideacode | ...
```

**If already running:**
```
MCP already running
Server: ideacode-mcp (PID: 12345)
Stop: /i.mcp stop
Restart: /i.mcp restart
```

---

## Stop MCP Server

**Execution:**
1. Find MCP PID for this terminal
2. Kill process gracefully
3. Remove PID file
4. Update statusline

**Output:**
```
Stopping MCP server...
✓ Stopped: ideacode-mcp (PID: 12345)
✓ Scope: This terminal only

Statusline updated: ... | no MCP | ...
```

---

## Restart MCP Server

**Execution:**
1. Stop current MCP (if running)
2. Start new MCP instance
3. Update statusline

---

## Configuration

**Location:** `{repo}/.claude/mcp.json`

**Check:**
```bash
/i.mcp config
```

**Output:**
```
MCP Configuration
─────────────────
File: .claude/mcp.json
Server: ideacode-mcp
Entry: /path/to/server/index.js
Auto-load: NO (manual start only)

Status: Not running
Start: /i.mcp start
```

---

## Statusline Integration

**Format:** `IDEACODE v11.2.5 | MCP:{name} | API | ...`

**States:**
- `MCP:ideacode` - Running with server name
- `no MCP` - Not running
- `MCP:error` - Error state

---

## Detection Methods

**Check running:**
```bash
# Method 1: PID file
[ -f /tmp/mcp-server-$TERMINAL_ID.pid ] && cat /tmp/mcp-server-$TERMINAL_ID.pid

# Method 2: Process search
ps aux | grep -i "mcp-server\|ideacode-mcp" | grep -v grep

# Method 3: Port check (if known port)
lsof -i :PORT 2>/dev/null
```

**Get server name:**
```bash
jq -r '.mcpServers | keys[]' .claude/mcp.json
```

---

## Auto-Load Rules

**IMPORTANT:**

| Server | Auto-Load | Scope |
|--------|-----------|-------|
| **API Server** | YES | Global (all terminals) |
| **MCP Server** | NO | Per-repo/terminal (manual start) |

**Why:**
- API: Shared service, stateless, single instance
- MCP: Repo-specific, stateful, per-terminal isolation

---

## Terminal Isolation

**MCP servers are terminal-specific:**
- Terminal 1: Can run `ideacode-mcp`
- Terminal 2: Can run different MCP or no MCP
- Terminal 3: Can run same MCP independently

**PID storage:**
```
/tmp/mcp-server-{TERMINAL_ID}.pid
/tmp/mcp-server-{TERMINAL_ID}.name
```

**Cleanup on terminal close:**
- Remove PID files
- Stop MCP process
- Clean temp files

---

## Examples

```bash
# Check status
/i.mcp status
# → Running: NO

# Start MCP
/i.mcp start
# → Started: ideacode-mcp (PID: 12345)
# → Statusline: ... | MCP:ideacode | ...

# Check again
/i.mcp status
# → Running: YES, Server: ideacode-mcp

# Stop MCP
/i.mcp stop
# → Stopped: ideacode-mcp
# → Statusline: ... | no MCP | ...
```

---

## Error Handling

**No mcp.json:**
```
Error: No MCP configuration
File: .claude/mcp.json not found
Help: Create MCP config first
```

**Server start fails:**
```
Error: Failed to start MCP server
Server: ideacode-mcp
Log: /tmp/mcp-server-{TERMINAL_ID}.log
Check: cat /tmp/mcp-server-{TERMINAL_ID}.log
```

**Multiple instances:**
```
Warning: MCP already running (PID: 12345)
Options:
  1. Stop current: /i.mcp stop
  2. Restart: /i.mcp restart
  3. Leave running: Do nothing
```

---

## Related

| Command | Purpose |
|---------|---------|
| API auto-loads | Global service (all terminals) |
| MCP manual | Per-terminal (repo-specific) |
| Statusline | Shows: API + MCP status |

---

**Version:** 1.0 | **Updated:** 2025-12-11

