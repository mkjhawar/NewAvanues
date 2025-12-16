# IDEACODE MCP Configuration v1.0
# Type: IDC
# Extension: .idc
---
schema: idc-1.0
version: 12.0.0
project: newavanues
metadata:
  file: mcp.idc
  category: mcp
  count: 8
---
MCP:ideacode:core:loaded
MCP:ideacode_discover:core:loaded
MCP:ideacode_context:core:loaded
MCP:ideacode_standards:core:loaded
MCP:ideacode_specify:deferred:unloaded
MCP:ideacode_plan:deferred:unloaded
MCP:ideacode_implement:deferred:unloaded
MCP:ideacode_research:deferred:unloaded
CFG:server_url:http://localhost:3847:string
CFG:auto_start:true:bool
CFG:per_repo:true:bool
---
aliases:
  mcp: [server, tool]
  cfg: [config]
