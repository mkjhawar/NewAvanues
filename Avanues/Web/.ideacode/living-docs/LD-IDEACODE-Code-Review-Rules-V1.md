# Code Review Rules (Living Document)

## Complexity Checks (Auto-Run)

### KISS Violations

| Flag | Example | Fix |
|------|---------|-----|
| Abstraction for 1 use | Factory for single class | Direct instantiation |
| Generic when specific works | Generic<T> for 1 type | Use concrete type |
| Pattern for simple task | Strategy for if/else | Simple conditional |
| Config for hardcoded value | Config file for 1 constant | Inline constant |

### Over-Engineering Signs

| Sign | Question to Ask |
|------|-----------------|
| >3 indirection levels | Can caller reach target directly? |
| Unused extensibility | Will this ever be extended? |
| Premature abstraction | Do we have 3+ concrete cases? |
| Complex inheritance | Can composition work? |

### Verbose Code

| Verbose | Simpler |
|---------|---------|
| Explicit null checks everywhere | Null-safe operators |
| Manual loops | Collection methods |
| Boilerplate getters/setters | Data classes |
| Try-catch for everything | Let exceptions propagate |

---

## Before Implementing

| Check | Action |
|-------|--------|
| Simpler solution exists? | Use it |
| Already solved elsewhere? | Import it |
| Over-scoped? | Reduce scope |
| Future-proofing? | Remove it |

---

## Review Checklist

| Question | If Yes |
|----------|--------|
| More complex than problem? | Simplify |
| Adding unrequested features? | Remove |
| Abstracting prematurely? | Inline |
| Optimizing non-bottleneck? | Revert |

---

## MCP Server Discovery

When integration needed, search:

| Source | URL |
|--------|-----|
| Anthropic Official | github.com/anthropics/mcp-servers |
| MCP Registry | github.com/modelcontextprotocol |
| Community | Search: "mcp server {integration}" |

**Before building custom:** Check if MCP server exists for:
- Payment (Stripe, Square)
- Database (PostgreSQL, MongoDB)
- Cloud (AWS, GCP, Azure)
- APIs (GitHub, Slack, etc.)

---
*IDEACODE v9.0*
