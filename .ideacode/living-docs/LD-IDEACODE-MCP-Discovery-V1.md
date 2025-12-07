# LD-IDEACODE-MCP-Discovery-V1

## Purpose
Guidelines for MCP tool discovery and lazy loading - accessing tools only when needed.

---

## Lazy Loading (On-Demand Tools)

### Always Available (Core)
| Tool | Purpose | Why Always |
|------|---------|------------|
| `ideacode_discover` | Find/load other tools | Gateway to all tools |
| `ideacode_context` | Context management | Essential for sessions |
| `ideacode_standards` | Load coding standards | Needed for all code |

### On-Demand (Deferred)
| Category | Tools | Load When |
|----------|-------|-----------|
| **Spec/Plan** | `ideacode_specify`, `ideacode_plan` | `/ispecify`, `/iplan` called |
| **Implementation** | `ideacode_implement`, `ideacode_test` | `/iimplement` called |
| **Vision** | `ideacode_vision` | UI/screenshot analysis needed |
| **Research** | `ideacode_research` | `/iresearch` called |
| **Files** | `ideacode_fs` | File operations needed |
| **Git** | `ideacode_commit` | Commit requested |

### Discovery Commands
```bash
# List all available deferred tools
ideacode_discover --list_all true

# Search for specific tool
ideacode_discover --query "vision"

# Load specific tools
ideacode_discover --load ["ideacode_vision", "ideacode_research"]
```

### Command Auto-Loading
| Command | Tools Loaded |
|---------|--------------|
| `/ispecify` | ideacode_specify, ideacode_standards |
| `/iplan` | ideacode_plan |
| `/iimplement` | ideacode_implement, ideacode_test |
| `/ifix` | ideacode_standards, ideacode_test |
| `/icreateui` | ideacode_vision (if mockup) |
| `/iresearch` | ideacode_research |

---

## Official Sources

| Source | URL | Type |
|--------|-----|------|
| MCP Registry | registry.modelcontextprotocol.io | Official |
| GitHub MCP Registry | github.com/modelcontextprotocol/registry | Official |
| Reference Servers | github.com/modelcontextprotocol/servers | Official |
| Awesome MCP | github.com/wong2/awesome-mcp-servers | Community |

---

## Official Reference Servers

| Server | Purpose |
|--------|---------|
| Filesystem | Secure file operations |
| Git | Repository operations |
| GitHub | GitHub API integration |
| Fetch | Web content fetching |
| Memory | Knowledge graph persistence |
| Sequential Thinking | Problem-solving chains |
| PostgreSQL | Database operations |
| Puppeteer | Browser automation |
| Slack | Slack integration |
| Google Drive | Drive operations |

---

## Common Integrations

| Need | Search For |
|------|------------|
| Payment | "mcp server stripe" / "mcp server square" |
| Database | "mcp server postgresql" / "mcp server mongodb" |
| Cloud | "mcp server aws" / "mcp server gcp" |
| Auth | "mcp server auth0" / "mcp server okta" |
| Email | "mcp server sendgrid" / "mcp server mailgun" |
| Storage | "mcp server s3" / "mcp server cloudflare" |

---

## Before Building Custom

1. Search registry.modelcontextprotocol.io
2. Search github.com/modelcontextprotocol/servers
3. Search github.com/wong2/awesome-mcp-servers
4. Search GitHub: "mcp server {integration}"
5. Only build custom if none exists

---

## Installing MCP Server

```json
// In claude_desktop_config.json or settings
{
  "mcpServers": {
    "server-name": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-name"]
    }
  }
}
```

---

## Metadata
- **Document:** LD-IDEACODE-MCP-Discovery-V1
- **Version:** 1.1
- **Updated:** 2025-12-05
- **Author:** IDEACODE
