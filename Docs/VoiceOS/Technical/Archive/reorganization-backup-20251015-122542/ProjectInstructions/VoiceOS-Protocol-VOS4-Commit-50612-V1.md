<!--
filename: VOS4-COMMIT-PROTOCOL.md
created: 2025-01-27 19:00:00 PST
author: VOS4 Development Team
purpose: Mandatory Git workflow and commit protocols for all VOS4 development
last-modified: 2025-01-27 19:00:00 PST
version: 1.0.0
-->

# VOS4 COMMIT PROTOCOL - MANDATORY FOR ALL DEVELOPERS

## 🔴 CRITICAL: No AI References Rule

**ZERO TOLERANCE POLICY - NEVER include AI/tool references in commits:**

### ❌ FORBIDDEN in Commit Messages:
- "Claude", "Anthropic", "AI" mentions
- "Generated with [tool]" statements  
- "Co-Authored-By: Claude <noreply@anthropic.com>"
- Any reference to AI assistants or tools

### ✅ KEEP PROFESSIONAL:
- Focus on WHAT changed and WHY
- Use professional, tool-agnostic language
- Credit human authors only
- Maintain corporate code standards

## 🚨 MANDATORY Pre-Commit Checklist

**ALL ITEMS MUST BE COMPLETED BEFORE EVERY COMMIT:**

### 1️⃣ Functional Equivalency (ZERO TOLERANCE)
- [ ] **100% feature parity maintained** (unless explicitly approved otherwise)
- [ ] All original functionality preserved
- [ ] No methods/features removed without written approval
- [ ] Backward compatibility verified
- [ ] Feature comparison matrix created for major changes

### 2️⃣ File/Folder Preservation
- [ ] **NO files deleted without explicit written approval**
- [ ] **NO folders removed without permission**
- [ ] Archives created instead of deletions
- [ ] User approval documented if any deletions made

### 3️⃣ Documentation Updates (MANDATORY)
- [ ] **Module changelog updated** (`/docs/modules/[module]/[Module]-Changelog.md`)
- [ ] Architecture docs updated (if structure changed)
- [ ] API documentation updated (if interfaces changed)
- [ ] Status and TODO files updated
- [ ] Visual documentation updated (diagrams, flowcharts)

### 4️⃣ Code Quality
- [ ] Compilation errors fixed
- [ ] No broken dependencies
- [ ] Performance requirements met
- [ ] Security requirements maintained

## 📝 Git Workflow Rules

### Branch Management
- **VOS4 Branch**: Stay on VOS4 branch for all development
- **Main Branch**: Use for pull requests and production releases
- **Feature Branches**: Create only when explicitly requested

### Multi-Agent Environment Rules
⚠️ **CRITICAL**: Multiple agents work simultaneously on same repository

#### Staging Rules:
```bash
# ✅ CORRECT - Stage only your files
git add path/to/specific/file.kt
git add docs/module/Module-Changelog.md

# ❌ WRONG - Will stage other agents' work
git add .
git add -A
git add *
```

#### Verification Commands:
```bash
# Always check before staging
git status

# Verify what you're staging
git diff --cached

# Unstage if you added wrong files
git reset path/to/wrong/file.kt
```

## 🔄 Staging by Category (MANDATORY)

When told to "stage files" or when committing, follow this order:

### Stage 1: Documentation Changes
```bash
# Stage all documentation updates first
git add docs/modules/[module]/[Module]-Changelog.md
git add docs/Planning/Architecture/[module]/
git add docs/Status/Current/
```

### Stage 2: Code Changes by Module
```bash
# Stage code files by module/component
git add modules/apps/VoiceAccessibility/
git add managers/CommandsManager/
```

### Stage 3: Configuration/Build Files
```bash
# Stage config changes last
git add build.gradle
git add AndroidManifest.xml
```

### NEVER Mix Categories:
- **Documentation and code in same commit = VIOLATION**
- **Multiple modules in same commit = VIOLATION** (unless very small changes)
- **Config and code in same commit = VIOLATION**

## 📋 Commit Message Format

### Standard Format:
```
type(scope): Brief description

- List key changes made
- Document what was updated
- Note any documentation changes
- Explain impact or benefits

Author: [Human Author Name]
```

### Commit Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation updates
- `refactor`: Code refactoring (no functional change)
- `perf`: Performance improvements
- `test`: Test additions or modifications
- `chore`: Build process or auxiliary tool changes

### Examples:

#### ✅ CORRECT Commit Messages:
```
feat(voice-accessibility): Add command recognition for window switching

- Implemented window switching voice commands
- Added support for application-specific navigation
- Updated VoiceAccessibility-Changelog.md
- Enhanced command parser for multi-word commands

Author: Manoj Jhawar
```

```
docs(speech-recognition): Update module architecture documentation

- Updated SpeechRecognition-Architecture.md with new engine flow
- Added sequence diagrams for engine switching
- Documented performance characteristics
- Updated master TODO list

Author: Manoj Jhawar
```

#### ❌ WRONG Commit Messages:
```
# AI references forbidden
feat: Add voice commands (Generated with Claude Code)

# Too vague
fix: Updated stuff

# Missing details
docs: Changes

# Mixed categories (code + docs in one commit)
feat(voice-ui): Add overlay + update all documentation
```

## 🔧 SCP Command (Stage, Commit, Push)

Standard workflow when instructed to "SCP" or "stage, commit, push":

### 1. Update Documentation FIRST
```bash
# Verify all required docs are updated
ls -la docs/modules/[affected-module]/
git status
```

### 2. Stage by Category
```bash
# Stage documentation
git add docs/

# Verify staging
git status

# Commit documentation
git commit -m "docs([scope]): [Description]

- Updated [Module]-Changelog.md
- [Other doc updates]

Author: [Name]"
```

### 3. Stage Code Changes
```bash
# Stage specific code files
git add modules/apps/VoiceAccessibility/src/main/java/com/ai/voiceaccessibility/VoiceAccessibilityService.kt
git add managers/CommandsManager/src/main/java/com/ai/commandsmanager/CommandsManager.kt

# Verify staging
git status

# Commit code changes
git commit -m "feat([module]): [Description]

- [Specific changes]
- [Impact/benefits]

Author: [Name]"
```

### 4. Push Changes
```bash
git push origin VOS4
```

## 🚨 Common Violations & Fixes

### Violation: Mixed Commits
```bash
# ❌ WRONG
git add docs/ modules/ 
git commit -m "Updated everything"

# ✅ CORRECT
git add docs/
git commit -m "docs: Updated module documentation"
git add modules/
git commit -m "feat: Implemented new features"
```

### Violation: AI References
```bash
# ❌ WRONG
git commit -m "feat: Add commands

Generated with Claude Code

Co-Authored-By: Claude <noreply@anthropic.com>"

# ✅ CORRECT
git commit -m "feat(commands): Add voice command recognition

- Implemented speech-to-command mapping
- Added support for 15 new commands
- Enhanced recognition accuracy

Author: Manoj Jhawar"
```

### Violation: Inadequate Documentation
```bash
# ❌ WRONG - No changelog updated
git commit -m "feat: New feature"

# ✅ CORRECT - Docs updated first
# 1. Update docs/modules/[module]/[Module]-Changelog.md
# 2. Then commit with reference to doc updates
git commit -m "feat([module]): New feature implementation

- Implemented [specific feature]
- Updated [Module]-Changelog.md
- [Other changes]

Author: [Name]"
```

## 📊 Quick Reference Commands

### Pre-Commit Verification:
```bash
# Check what files changed
git status

# Check for unapproved deletions
git status --porcelain | grep "^D"

# Verify documentation was updated
find docs -name "*.md" -newer .git/index

# Check changelog specifically
git diff docs/modules/[module]/[Module]-Changelog.md
```

### Safe Staging:
```bash
# List your changed files
git diff --name-only

# Stage one file at a time
git add path/to/specific/file.kt

# Verify what's staged
git diff --cached --name-only
```

### Emergency Fixes:
```bash
# Unstage everything
git reset

# Unstage specific file
git reset path/to/file.kt

# Amend last commit (if needed)
git commit --amend -m "New message"
```

## 🔍 Verification Checklist

Before every commit, verify:

```
Pre-Commit Verification:
- [ ] Only my files are staged (git status check)
- [ ] Documentation updated and staged
- [ ] No AI references in commit message
- [ ] Functional equivalency maintained
- [ ] No unauthorized deletions
- [ ] Clear, descriptive commit message
- [ ] Human author specified
- [ ] Appropriate commit type used
```

## 📋 Pull Request Protocol

When creating pull requests:

### 1. Pre-PR Checklist:
- [ ] All commits follow this protocol
- [ ] Documentation completely updated
- [ ] No AI references anywhere
- [ ] Feature/bug testing completed
- [ ] Performance requirements verified

### 2. PR Creation:
```bash
# Ensure branch is up to date
git pull origin main

# Push final changes
git push origin VOS4

# Create PR (no AI references!)
gh pr create --title "feat: [Clear description]" --body "
## Summary
- [Key changes made]
- [Features added/modified]

## Testing
- [Testing performed]
- [Results verified]

## Documentation
- [Docs updated]
- [Changelogs modified]

Author: [Human Author Name]
"
```

### 3. NO AI References in PRs:
- **Title**: No AI tool mentions
- **Description**: Focus on technical changes
- **Comments**: Professional communication only

## 🎯 Success Metrics

A properly executed commit includes:
- ✅ Zero AI references
- ✅ Complete documentation updates
- ✅ Staged by category
- ✅ Clear, professional messages
- ✅ Human authorship
- ✅ Functional equivalency maintained
- ✅ No unauthorized deletions

## 📋 Quick Copy/Paste Checklist

```
VOS4 Commit Protocol Checklist:
- [ ] No AI references in commit message
- [ ] Documentation updated BEFORE staging
- [ ] Files staged by category (docs → code → config)
- [ ] Only my files staged (not other agents')
- [ ] Clear commit message with human author
- [ ] Functional equivalency verified
- [ ] No files deleted without approval
- [ ] git status verified before commit
```

---

**Remember:** Professional commits build professional software. Keep all AI tool usage internal to development process - never expose it in version control history.

**Violation Policy:** Any commit violating these protocols must be amended or reverted immediately.