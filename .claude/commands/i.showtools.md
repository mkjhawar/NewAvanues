# /showtools - Watch MCP Tool Usage

Opens real-time view of IDEACODE MCP tool calls.

## Instructions

Run this command in a **separate terminal window**:

```bash
tail -f ~/.claude/ideacode-tools.log
```

## What You'll See

```
2025-12-01T00:05:23.123Z | ideacode_discover
2025-12-01T00:05:45.456Z | ideacode_fs
2025-12-01T00:06:12.789Z | ideacode_research
```

Each line appears instantly when a tool is called.

## To Stop

Press `Ctrl+C` in the terminal.

## Log Location

`~/.claude/ideacode-tools.log`
