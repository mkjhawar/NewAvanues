# Protocol: MCP API Connector v1.0

**Purpose:** Configure and use MCP servers with Claude Messages API
**Status:** RECOMMENDED for API-based deployments
**Priority:** MEDIUM
**Version:** 1.0
**Date:** 2025-11-14
**Source:** https://docs.claude.com/en/docs/agents-and-tools/mcp-connector

---

## Overview

The MCP connector enables direct integration with remote Model Context Protocol servers through the Messages API, bypassing the need for a separate MCP client implementation.

**Golden Rule:** Use MCP API connector for serverless/cloud deployments where STDIO servers aren't practical.

---

## When to Use MCP API Connector

### ✅ Use MCP API Connector For

**1. Cloud/Serverless Deployments**
- AWS Lambda functions
- Google Cloud Functions
- Azure Functions
- Vercel/Netlify serverless
- Any stateless API deployment

**2. Web Applications**
- Browser-based Claude integration
- SaaS platforms using Claude API
- Mobile app backends
- Public-facing APIs

**3. Multi-Tenant Systems**
- Each tenant needs isolated MCP servers
- Dynamic server configuration per request
- Horizontal scaling requirements

### ❌ Don't Use For

**1. Desktop Applications (Use STDIO Instead)**
- Claude Code CLI
- Local development tools
- IDE integrations
- Desktop AI assistants

**2. Bedrock/Vertex Deployments**
- Not currently supported
- Use native integration methods

---

## Configuration

### Basic Server Setup

**Required Fields:**
```json
{
  "type": "url",           // Only "url" supported (STDIO not available)
  "url": "https://...",    // HTTPS endpoint (required)
  "name": "unique-id"      // Unique server identifier
}
```

**Optional Fields:**
```json
{
  "tool_configuration": {
    "enabled": true,              // Enable/disable all tools
    "allowed_tools": ["tool_1"]   // Whitelist specific tools
  },
  "authorization_token": "token"  // OAuth Bearer token
}
```

---

### Complete Configuration Example

```json
{
  "model": "claude-sonnet-4-5-20250929",
  "max_tokens": 1024,
  "messages": [...],
  "mcp_servers": [
    {
      "type": "url",
      "url": "https://mcp.example.com/sse",
      "name": "ideacode-mcp",
      "tool_configuration": {
        "enabled": true,
        "allowed_tools": [
          "ideacode_specify",
          "ideacode_plan",
          "ideacode_implement",
          "ideacode_test",
          "ideacode_archive"
        ]
      },
      "authorization_token": "sk_live_abc123..."
    }
  ],
  "headers": {
    "anthropic-beta": "mcp-client-2025-04-04"  // REQUIRED beta header
  }
}
```

---

## Multi-Server Integration

**Combine multiple MCP servers in single request:**

```json
{
  "mcp_servers": [
    {
      "type": "url",
      "url": "https://ideacode-mcp.example.com/sse",
      "name": "ideacode",
      "tool_configuration": {
        "enabled": true,
        "allowed_tools": ["ideacode_specify", "ideacode_implement"]
      }
    },
    {
      "type": "url",
      "url": "https://database-mcp.example.com/sse",
      "name": "database",
      "tool_configuration": {
        "enabled": true,
        "allowed_tools": ["query_db", "migrate_schema"]
      }
    },
    {
      "type": "url",
      "url": "https://search-mcp.example.com/sse",
      "name": "search",
      "authorization_token": "search_token_xyz"
    }
  ]
}
```

**Key Points:**
- Each server MUST have unique `name`
- Names used for disambiguation in tool calls
- All servers processed in parallel
- Tool namespace: `server_name.tool_name`

---

## Response Handling

### Tool Use Block

**Structure:**
```json
{
  "type": "mcp_tool_use",
  "id": "mcptoolu_014Q35RayjACSWkSj4X2yov1",
  "name": "ideacode_specify",
  "server_name": "ideacode",
  "input": {
    "feature_request": "Add dark mode toggle"
  }
}
```

**vs Standard Tool Use:**
```json
{
  "type": "tool_use",          // Standard (no MCP)
  "id": "toolu_01A...",
  "name": "get_weather",
  "input": {...}
}
```

**Difference:** `mcp_tool_use` includes `server_name` for multi-server disambiguation.

---

### Tool Result Block

**Client must return:**
```json
{
  "type": "mcp_tool_result",
  "tool_use_id": "mcptoolu_014Q35RayjACSWkSj4X2yov1",
  "is_error": false,
  "content": [
    {
      "type": "text",
      "text": "Feature specification created successfully:\n/specs/dark-mode-toggle.md"
    }
  ]
}
```

**Error Response:**
```json
{
  "type": "mcp_tool_result",
  "tool_use_id": "mcptoolu_014Q35RayjACSWkSj4X2yov1",
  "is_error": true,
  "content": [
    {
      "type": "text",
      "text": "Error: Project path not found. Please initialize IDEACODE first."
    }
  ]
}
```

---

## Authentication & OAuth

### OAuth Flow for Protected MCP Servers

**Step 1: Obtain Access Token (Testing)**

Use MCP Inspector:
```bash
npx @modelcontextprotocol/inspector \
  --url https://your-mcp-server.com/sse \
  --transport sse
```

**Step 2: Configure OAuth in Production**

**Supported Transports:**
- SSE (Server-Sent Events)
- Streamable HTTP

**Step 3: Pass Token in API Call**

```json
{
  "mcp_servers": [{
    "authorization_token": "YOUR_ACCESS_TOKEN",
    ...
  }]
}
```

**Step 4: Implement Token Refresh**

```typescript
class MCPTokenManager {
  private token: string;
  private expiresAt: Date;

  async getToken(): Promise<string> {
    // Check if token expired
    if (new Date() >= this.expiresAt) {
      await this.refreshToken();
    }
    return this.token;
  }

  async refreshToken() {
    // OAuth refresh flow
    const response = await fetch('https://oauth.example.com/token', {
      method: 'POST',
      body: JSON.stringify({
        grant_type: 'refresh_token',
        refresh_token: this.refreshToken,
        client_id: process.env.CLIENT_ID,
        client_secret: process.env.CLIENT_SECRET
      })
    });

    const data = await response.json();
    this.token = data.access_token;
    this.expiresAt = new Date(Date.now() + data.expires_in * 1000);
  }
}

// Usage
const tokenManager = new MCPTokenManager();

const response = await anthropic.messages.create({
  mcp_servers: [{
    authorization_token: await tokenManager.getToken(),
    ...
  }],
  ...
});
```

---

## Integration with IDEACODE

### Deployment Pattern: API-Based IDEACODE

**Scenario:** Deploy IDEACODE MCP server to cloud, use from web app

**Architecture:**
```
[Web App] → [Claude API + MCP Connector] → [IDEACODE MCP Server (Cloud)]
```

**Configuration:**

**1. Deploy IDEACODE MCP Server**

```bash
# Deploy to cloud (e.g., Railway, Render, Fly.io)
git push production main

# Server exposes HTTPS endpoint
https://ideacode-mcp.yourapp.com/sse
```

**2. Configure Web App**

```typescript
import Anthropic from '@anthropic-ai/sdk';

const anthropic = new Anthropic({
  apiKey: process.env.ANTHROPIC_API_KEY
});

async function callIDEACODE(userMessage: string) {
  const response = await anthropic.messages.create({
    model: 'claude-sonnet-4-5-20250929',
    max_tokens: 4096,
    messages: [{ role: 'user', content: userMessage }],
    mcp_servers: [{
      type: 'url',
      url: process.env.IDEACODE_MCP_URL, // https://ideacode-mcp.yourapp.com/sse
      name: 'ideacode',
      tool_configuration: {
        enabled: true,
        // Whitelist only public-facing tools
        allowed_tools: [
          'ideacode_specify',
          'ideacode_plan',
          'ideacode_validate'
        ]
      },
      authorization_token: await getIDEACODEToken() // OAuth token
    }],
    headers: {
      'anthropic-beta': 'mcp-client-2025-04-04'
    }
  });

  return response;
}
```

**3. Handle Tool Calls**

```typescript
async function handleToolCalls(response: any) {
  // Check if Claude called MCP tools
  for (const block of response.content) {
    if (block.type === 'mcp_tool_use') {
      console.log(`MCP Tool Called: ${block.server_name}.${block.name}`);
      console.log(`Input:`, block.input);

      // Tool execution handled by MCP server automatically
      // Results returned in next API call
    }
  }
}
```

---

## Tool Whitelisting

### Security Best Practice

**Problem:** Exposing all MCP tools publicly can be risky.

**Solution:** Use `allowed_tools` to restrict access.

**Example:**

```json
{
  "tool_configuration": {
    "enabled": true,
    "allowed_tools": [
      "ideacode_specify",    // ✅ Safe for public use
      "ideacode_plan",       // ✅ Safe for public use
      "ideacode_validate"    // ✅ Safe for public use
      // ❌ NOT whitelisted:
      // - ideacode_implement (could execute arbitrary code)
      // - ideacode_commit (could modify git repo)
      // - ideacode_archive (could delete files)
    ]
  }
}
```

**Rationale:**
- Public-facing: Only read/analysis tools
- Internal-facing: All tools including write/execute
- Per-user permissions: Different whitelists per user role

---

## Best Practices

### 1. Use Descriptive Server Names ✅

```json
✅ GOOD:
"name": "ideacode-production"
"name": "ideacode-staging"
"name": "database-postgres"

❌ BAD:
"name": "server1"
"name": "mcp"
"name": "tools"
```

**Why:** Disambiguation in multi-server scenarios.

---

### 2. Implement Tool Whitelisting ✅

```json
✅ GOOD:
"allowed_tools": ["safe_tool_1", "safe_tool_2"]

❌ BAD:
"enabled": true  // No whitelist = all tools exposed
```

**Why:** Limits attack surface, prevents misuse.

---

### 3. Handle Token Refresh ✅

```typescript
✅ GOOD:
async function callClaude() {
  const token = await tokenManager.getValidToken();
  // token guaranteed fresh
}

❌ BAD:
const token = "hardcoded_token"; // Expires eventually
```

**Why:** Long-running operations require fresh tokens.

---

### 4. Verify Server Accessibility ✅

```typescript
✅ GOOD:
async function verifyMCPServer(url: string) {
  try {
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`MCP server unreachable: ${url}`);
    }
  } catch (error) {
    console.error('MCP server verification failed:', error);
    // Use fallback or alert user
  }
}

❌ BAD:
// Assume server is always available
```

**Why:** Prevents silent failures in production.

---

### 5. Use HTTPS Only ✅

```json
✅ GOOD:
"url": "https://mcp.example.com/sse"

❌ BAD:
"url": "http://mcp.example.com/sse"  // Insecure
"url": "ws://mcp.example.com"        // Not supported
```

**Why:** Security + API requirement.

---

## Feature Support Matrix

| Feature | Supported | Notes |
|---------|-----------|-------|
| **Tool Calling** | ✅ Yes | Full support via Messages API |
| **HTTPS URL Servers** | ✅ Yes | Required transport |
| **Multi-Server** | ✅ Yes | Multiple servers per request |
| **OAuth Authentication** | ✅ Yes | Via `authorization_token` |
| **Tool Whitelisting** | ✅ Yes | `allowed_tools` configuration |
| **STDIO Servers** | ❌ No | Use desktop clients instead |
| **Amazon Bedrock** | ❌ No | Not yet supported |
| **Google Vertex AI** | ❌ No | Not yet supported |
| **Non-HTTP Transport** | ❌ No | HTTPS/SSE only |

---

## Error Handling

### Common Errors

**1. Missing Beta Header**

```
Error: Invalid request - missing required beta header
```

**Fix:**
```json
{
  "headers": {
    "anthropic-beta": "mcp-client-2025-04-04"
  }
}
```

---

**2. Server Unreachable**

```
Error: MCP server at https://... unreachable
```

**Fix:**
- Verify server is running
- Check HTTPS configuration
- Verify network connectivity
- Check firewall rules

---

**3. Token Expired**

```
Error: Authorization failed - token expired
```

**Fix:**
Implement token refresh:
```typescript
if (error.message.includes('token expired')) {
  await tokenManager.refreshToken();
  // Retry request
}
```

---

**4. Tool Not Allowed**

```
Error: Tool 'ideacode_implement' not in allowed_tools list
```

**Fix:**
Add to whitelist or remove restriction:
```json
{
  "allowed_tools": ["ideacode_specify", "ideacode_implement"]
}
```

---

## Complete Example: IDEACODE Web Integration

```typescript
// ideacode-api-client.ts
import Anthropic from '@anthropic-ai/sdk';

export class IDEACODEAPIClient {
  private anthropic: Anthropic;
  private mcpServerURL: string;
  private tokenManager: TokenManager;

  constructor() {
    this.anthropic = new Anthropic({
      apiKey: process.env.ANTHROPIC_API_KEY
    });
    this.mcpServerURL = process.env.IDEACODE_MCP_URL!;
    this.tokenManager = new TokenManager();
  }

  async createSpecification(featureRequest: string): Promise<string> {
    const response = await this.anthropic.messages.create({
      model: 'claude-sonnet-4-5-20250929',
      max_tokens: 4096,
      messages: [{
        role: 'user',
        content: `Create specification for: ${featureRequest}`
      }],
      mcp_servers: [{
        type: 'url',
        url: this.mcpServerURL,
        name: 'ideacode',
        tool_configuration: {
          enabled: true,
          allowed_tools: ['ideacode_specify', 'ideacode_validate']
        },
        authorization_token: await this.tokenManager.getToken()
      }],
      headers: {
        'anthropic-beta': 'mcp-client-2025-04-04'
      }
    });

    // Process response
    return this.extractSpecification(response);
  }

  private extractSpecification(response: any): string {
    for (const block of response.content) {
      if (block.type === 'mcp_tool_result') {
        return block.content[0].text;
      }
    }
    throw new Error('No specification generated');
  }
}

// Usage
const client = new IDEACODEAPIClient();
const spec = await client.createSpecification('Add dark mode toggle');
console.log('Specification:', spec);
```

---

## Integration with IDEACODE MCP Server

**IDEACODE MCP Server (existing) works with API connector:**

**Server Configuration (No Changes Needed):**
```typescript
// /Volumes/M-Drive/Coding/ideacode/ideacode-mcp/src/index.ts
// Already implements MCP protocol - works with API connector automatically

const server = new Server({
  name: 'ideacode-mcp',
  version: '8.3.0'
}, {
  capabilities: {
    tools: {} // Tool definitions
  }
});

// Tools already defined - accessible via API connector
server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: [
    { name: 'ideacode_specify', ... },
    { name: 'ideacode_plan', ... },
    { name: 'ideacode_implement', ... },
    // ... all 29 tools
  ]
}));
```

**Deployment:**
```bash
# Deploy IDEACODE MCP to cloud platform
cd /Volumes/M-Drive/Coding/ideacode/ideacode-mcp
npm run build

# Deploy to Railway/Render/Fly.io
git push production main

# Server now accessible via HTTPS
# https://ideacode-mcp.railway.app/sse
```

---

## Comparison: Desktop vs API Deployment

| Aspect | Desktop (STDIO) | API (MCP Connector) |
|--------|-----------------|---------------------|
| **Transport** | STDIO pipes | HTTPS/SSE |
| **Use Case** | CLI, IDE, desktop apps | Web apps, cloud functions |
| **Authentication** | Not needed (local) | OAuth tokens required |
| **Scaling** | Single instance | Horizontal scaling |
| **Network** | Localhost only | Internet accessible |
| **Configuration** | `~/.claude/mcp.json` | API request payload |
| **Latency** | <10ms | 50-200ms (network) |

---

## Compliance with Claude Best Practices

**From Anthropic Documentation:**
> "The MCP connector enables direct integration with remote MCP servers through the Messages API, bypassing the need for a separate MCP client implementation."

**IDEACODE Implementation:**
- ✅ Uses HTTPS-only URLs
- ✅ Implements OAuth authentication
- ✅ Whitelists tools for security
- ✅ Handles token refresh
- ✅ Verifies server accessibility
- ✅ Uses descriptive server names
- ✅ Includes required beta header
- ✅ Properly handles tool use/result blocks

**Coverage:** 100% ✅

---

**Version:** 1.0
**Status:** Recommended for API-based deployments
**Last Updated:** 2025-11-14
**Owner:** IDEACODE Framework
**Source:** Anthropic MCP Connector Documentation

---

**Remember: MCP API connector enables cloud deployment while desktop clients use STDIO. Choose based on deployment environment.**
