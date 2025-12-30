# IDEACODE Configuration Format v1.0
# Type: IDC
# Extension: .idc
---
schema: idc-1.0
version: 12.0.0
project: newavanues
metadata:
  file: settings.idc
  category: configuration
  count: 15
---
PRJ:newavanues:monorepo:12.0.0
CFG:statusline:./.claude/statusline-command.sh:string
CFG:workdir:/Volumes/M-Drive/Coding/NewAvanues:string
CFG:allowed_commands:git+gradle+npm+adb:list
PTH:statusline:.claude/statusline-command.sh
PTH:hooks:.claude/hooks
PTH:agents:.claude/agents
PTH:mcp:.claude/mcp.json
PTH:living-docs:Docs/{Module}/LivingDocs
PTH:registries:.ideacode/registries
PTH:module-registries:Modules/{Module}/.ideacode/registries
PTH:project-info:docs/project-info
PTH:contextsave:contextsave
---
aliases:
  cfg: [config, configuration]
  prj: [project]
  pth: [path]
