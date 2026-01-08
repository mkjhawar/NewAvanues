---
name: mcp-builder
description: Build Model Context Protocol (MCP) servers for LLM tool integration. Use when creating tools/servers that allow Claude to interact with external services.
---

# MCP Server Development

## Overview
MCP servers expose tools that LLMs can invoke to interact with external services.

## Tech Stack

| Component | Recommendation |
|-----------|----------------|
| Language | TypeScript (preferred) or Python |
| Framework | @modelcontextprotocol/sdk |
| Validation | Zod (TS) / Pydantic (Python) |
| Transport | stdio or SSE |

## Project Structure (TypeScript)
```
mcp-server/
├── src/
│   ├── index.ts         # Entry, server setup
│   ├── tools/           # Tool implementations
│   ├── client.ts        # API client
│   └── types.ts         # TypeScript types
├── package.json
└── tsconfig.json
```

## Basic Server
```typescript
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";

const server = new Server({
  name: "my-mcp-server",
  version: "1.0.0",
}, {
  capabilities: { tools: {} }
});

// Register tool
server.setRequestHandler("tools/list", async () => ({
  tools: [{
    name: "my_tool",
    description: "Does something useful",
    inputSchema: {
      type: "object",
      properties: {
        param: { type: "string", description: "Input parameter" }
      },
      required: ["param"]
    }
  }]
}));

server.setRequestHandler("tools/call", async (request) => {
  if (request.params.name === "my_tool") {
    const { param } = request.params.arguments;
    // Implementation
    return { content: [{ type: "text", text: "Result" }] };
  }
});

// Start
const transport = new StdioServerTransport();
await server.connect(transport);
```

## Tool Design Principles

| Principle | Guideline |
|-----------|-----------|
| Naming | Consistent prefix (e.g., `github_create_issue`) |
| Descriptions | Clear, actionable, include examples |
| Errors | Guide toward solutions with next steps |
| Granularity | Balance comprehensive + workflow tools |

## Input Schema (Zod)
```typescript
import { z } from "zod";

const CreateIssueSchema = z.object({
  repo: z.string().describe("Repository name (owner/repo)"),
  title: z.string().describe("Issue title"),
  body: z.string().optional().describe("Issue body (markdown)"),
  labels: z.array(z.string()).optional()
});

type CreateIssueInput = z.infer<typeof CreateIssueSchema>;
```

## Error Handling
```typescript
server.setRequestHandler("tools/call", async (request) => {
  try {
    const result = await callExternalAPI(request.params);
    return { content: [{ type: "text", text: JSON.stringify(result) }] };
  } catch (error) {
    return {
      content: [{
        type: "text",
        text: `Error: ${error.message}\n\nSuggestion: Check API key is valid`
      }],
      isError: true
    };
  }
});
```

## API Client Pattern
```typescript
class APIClient {
  private baseUrl: string;
  private apiKey: string;

  constructor() {
    this.baseUrl = process.env.API_URL!;
    this.apiKey = process.env.API_KEY!;
  }

  async request<T>(endpoint: string, options?: RequestInit): Promise<T> {
    const res = await fetch(`${this.baseUrl}${endpoint}`, {
      ...options,
      headers: {
        "Authorization": `Bearer ${this.apiKey}`,
        "Content-Type": "application/json",
        ...options?.headers
      }
    });
    if (!res.ok) throw new Error(`API error: ${res.status}`);
    return res.json();
  }
}
```

## Configuration (Claude Desktop)
```json
{
  "mcpServers": {
    "my-server": {
      "command": "node",
      "args": ["/path/to/dist/index.js"],
      "env": {
        "API_KEY": "your-key"
      }
    }
  }
}
```

## Testing
```bash
# MCP Inspector
npx @anthropic-ai/mcp-inspector

# Direct test
echo '{"jsonrpc":"2.0","method":"tools/list","id":1}' | node dist/index.js
```

## Development Phases

| Phase | Focus |
|-------|-------|
| 1. Research | API docs, MCP patterns |
| 2. Implement | Client, tools, error handling |
| 3. Review | Type coverage, duplication, tests |
| 4. Evaluate | Create 10 test questions |

## Best Practices

| Practice | Reason |
|----------|--------|
| Type everything | Catch errors at compile time |
| Validate inputs | Zod/Pydantic for safety |
| Rate limit aware | Handle 429s gracefully |
| Pagination | Support large result sets |
| Caching | Reduce API calls where appropriate |

## Resources
- Protocol spec: https://modelcontextprotocol.io
- SDK: @modelcontextprotocol/sdk
- Examples: github.com/anthropics/mcp-servers
