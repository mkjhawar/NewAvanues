# IDEACODE v5.3 Framework

**Version:** 5.3
**Last Updated:** 2025-11-03
**Framework Location:** `/Volumes/M-Drive/Coding/ideacode`

---

## 🚀 Quick Start

### Autonomous Mode (Recommended)
```
Use ideacode_specify to create a spec for "your feature"
Use ideacode_plan with spec file "specs/001-feature/spec.md"
Use ideacode_implement with plan file "specs/001-feature/plan.md"
Use ideacode_test to run "./gradlew test"
Use ideacode_commit with message "Add feature"
```

**MCP Tools:** 17 autonomous tools available - just describe what you need!

### Manual Mode (Learning/Control)
```
/ideacode.specify
/ideacode.plan
/ideacode.implement
/ideacode.test
```

**Slash Commands:** 33 commands available - load protocols on-demand

---

## 📋 Session Start Protocol

**MANDATORY: Execute at every session start:**

### 1. Load Project-Specific Instructions

```bash
# Check for project instructions
cat .ideacode/project-instructions.md 2>/dev/null
```

**If exists:** Load project-specific UI design, architecture, coding standards
**If not exists:** Use framework defaults only

### 2. Verify MCP Server

```bash
claude mcp list
# Should show: ideacode-mcp: ✓ Connected
```

**If not connected:** Run `/Volumes/M-Drive/Coding/ideacode/docs/mcp-server/setup-mcp.sh`

### 3. Check Framework Version

```bash
cat "/Volumes/M-Drive/Coding/ideacode/VERSION"
# Current: 5.3
```

---

## 🚨 Zero Tolerance Rules (CRITICAL)

**BEFORE any code or file changes:**

### Pre-Code Checklist

1. ✅ **Will I delete anything?**
   - If YES → STOP → Get explicit user approval first
   - If NO → Continue

2. ✅ **Am I using the right tool?**
   - Use `ideacode_*` MCP tools (autonomous)
   - Or use `/ideacode.*` slash commands (manual)
   - Don't reinvent workflows

3. ✅ **Is context safe?**
   - Before major changes: `Use ideacode_context_save`
   - At 75% context: Alert user
   - At 90% context: MANDATORY save + reset

4. ✅ **File naming correct?**
   - **Living documents:** NO timestamp (README.md, config.yml)
   - **Static documents:** WITH timestamp (CONTEXT-2511030700.md)

5. ✅ **Git staging safe?**
   - ONLY stage files YOU created/modified
   - NEVER use `git add .` or `git add -A`
   - Use explicit paths: `git add path/to/file.ts`

**Full rules:** See `/Volumes/M-Drive/Coding/ideacode/protocols/Protocol-Zero-Tolerance-Pre-Code.md`

---

## 🛠️ Available Tools

### MCP Tools (Autonomous - 17 tools)
- **Workflow:** specify, plan, implement, test, commit
- **Vision:** analyze_ui, from_mockup, debug_screenshot
- **Research:** research, think
- **Context:** context_show, context_save, context_reset
- **Project:** new_project

**Usage:** Just describe what you need:
```
"Create a specification for adding dark mode"
"Analyze this screenshot for accessibility issues"
"Research best practices for state management"
```

### Slash Commands (Manual - 33 commands)
Type `/ideacode` to see all available commands

**Common commands:**
- `/ideacode.specify` - Feature specification workflow
- `/ideacode.plan` - Implementation planning
- `/ideacode.contextsave` - Save context checkpoint
- `/ideacode.principles` - Load constitutional principles

---

## 📚 Protocols (Load On-Demand)

**Location:** `/Volumes/M-Drive/Coding/ideacode/protocols/`

**Core Protocols:**
- `Protocol-Zero-Tolerance-Pre-Code.md` - Pre-code checklist
- `Protocol-Context-Management-V3.md` - Context management
- `Protocol-Coding-Standards.md` - Code quality standards
- `Protocol-File-Organization.md` - File/folder structure
- `Protocol-Document-Lifecycle.md` - Document naming rules

**25+ protocols available** - MCP tools load them automatically when needed

---

## 🎯 Profile-Aware Behavior

IDEACODE adapts to your project profile:

**Detected from:** `.ideacode-v2/config.yml`

- **android-app:** Kotlin, Jetpack Compose, Material Design 3
- **backend-api:** REST/GraphQL, Express/Spring, database patterns
- **frontend-web:** React/Vue, TypeScript, ARIA, responsive design
- **library:** TypeScript, API design, versioning, documentation

All MCP tools and protocols adapt automatically.

---

## 📊 Context Management

**Thresholds:**
- 🟢 **0-75%** - Continue normally
- 🟡 **75-90%** - Alert user, recommend save
- 🔴 **90%+** - MANDATORY save + reset (zero tolerance)

**Tools:**
- `Use ideacode_context_show` - Check current usage
- `Use ideacode_context_save` - Save checkpoint
- `Use ideacode_context_reset` - Get reset instructions

**Format:** V3 compressed format (37% token savings)

---

## 🔍 Troubleshooting

### MCP tools not working?
```bash
# Check server status
claude mcp list

# If not connected, run setup
/Volumes/M-Drive/Coding/ideacode/docs/mcp-server/setup-mcp.sh
```

### Slash commands not showing?
```bash
# Verify commands directory
ls .claude/commands/ | wc -l
# Should show: 33-34 files

# If missing, redeploy
cp /Volumes/M-Drive/Coding/ideacode/.claude/commands/*.md .claude/commands/
```

### Protocol files not found?
```bash
# Verify framework location
ls /Volumes/M-Drive/Coding/ideacode/protocols/
# Should show 25+ .md files
```

---

## 📖 Documentation

**Framework Docs:** `/Volumes/M-Drive/Coding/ideacode/docs/`
**MCP Server Docs:** `/Volumes/M-Drive/Coding/ideacode/docs/mcp-server/`
**Quick Reference:** `/Volumes/M-Drive/Coding/ideacode/docs/mcp-server/Quick-Setup-Reference.md`

---

## 🎓 Learning Resources

**New to IDEACODE?**
1. Start with `/ideacode.instructions` - Overview
2. Try manual mode first: `/ideacode.specify`
3. Then try autonomous: `Use ideacode_specify`
4. Read protocols as needed from `/protocols/`

**Experienced user?**
- Use MCP tools for speed (93% time savings)
- Use slash commands when you need control
- Both modes work perfectly

---

## 🔄 Version History

**v5.3 (2025-11-03):**
- ✅ MCP server with 17 autonomous tools
- ✅ Minimal CLAUDE.md (framework bootstrap only)
- ✅ Project-specific instructions via `.ideacode/project-instructions.md`
- ✅ 93% smaller than v5.2 (44KB → 3KB)

**v5.2 (2025-10-29):**
- Context Management V3
- Hierarchical Registry
- Zero Tolerance updates

---

**Framework:** IDEACODE v5.3
**Author:** Manoj Jhawar <manoj@ideahq.net>
**Support:** https://github.com/anthropics/claude-code/issues

**This is a minimal bootstrap. All workflows are handled by MCP tools and slash commands.**
