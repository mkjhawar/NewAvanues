# Monorepo Templates

**Purpose:** Templates for creating new IDEACODE-compliant monorepos with the same structure as MainAvanues.

---

## 📁 Templates Available

| Template | Purpose | Size |
|----------|---------|------|
| `FOLDER-REGISTRY-TEMPLATE.md` | Master folder registry template | ~4 KB |
| `PROJECT-INSTRUCTIONS-TEMPLATE.md` | Centralized app/platform instructions template | ~5 KB |
| `CLAUDE-MONOREPO-TEMPLATE.md` | Claude Code AI instructions template | ~3 KB |

---

## 🚀 How to Use

### Creating a New Monorepo

**Step 1: Create folder structure**
```bash
mkdir -p NewProject
cd NewProject

# Create platform folders
mkdir -p android/{app1,app2}
mkdir -p ios/{app1,app2}
mkdir -p desktop/{app1,app2}

# Create common modules (NO /libs!)
mkdir -p common/{database,shared}

# Create docs structure
mkdir -p docs/{app1,app2}/{Master,Platform/{android,ios,desktop}}
mkdir -p docs/ideacode/{specs,protocols,guides}
mkdir -p docs/{migration,archive/{android,ios,desktop}}

# Create scripts
mkdir -p scripts/{migration,build}

# Create framework folders
mkdir -p .ideacode/{config,context,agents}
mkdir -p .claude
```

**Step 2: Copy and customize templates**
```bash
# Copy templates
cp /Volumes/M-Drive/Coding/AVA/docs/templates/FOLDER-REGISTRY-TEMPLATE.md ./FOLDER-REGISTRY.md
cp /Volumes/M-Drive/Coding/AVA/docs/templates/PROJECT-INSTRUCTIONS-TEMPLATE.md ./PROJECT-INSTRUCTIONS.md
cp /Volumes/M-Drive/Coding/AVA/docs/templates/CLAUDE-MONOREPO-TEMPLATE.md ./.claude/CLAUDE.md

# Customize with your project values
# Replace all {PLACEHOLDER} values with actual values
```

**Step 3: Customize placeholders**

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `{PROJECT_NAME}` | Your monorepo name | `MainAvanues` |
| `{DATE}` | Creation date | `2025-11-28` |
| `{AUTHOR}` | Author name | `Your Name` |
| `{APP1}`, `{APP2}` | App names (lowercase) | `voiceos`, `ava` |
| `{APP1_NAME}`, `{APP2_NAME}` | App display names | `VoiceOS`, `AVA` |
| `{APP1_DESCRIPTION}` | App description | `Voice-controlled OS` |
| `{CODE_SHARING_PERCENTAGE}` | KMP sharing target | `70` |
| `{TEAM_MODEL}` | Team structure | `Solo`, `Mixed` |
| `{MODULE1}`, `{MODULE2}` | Common module names | `database`, `voice` |
| `{MIN_SDK}`, `{TARGET_SDK}` | Android SDK versions | `26`, `34` |
| `{MIN_IOS}` | Minimum iOS version | `14.0` |
| `{KOTLIN_VERSION}` | Kotlin version | `1.9.x` |
| `{GRADLE_VERSION}` | Gradle version | `8.x` |

---

## 📋 Template Contents

### FOLDER-REGISTRY-TEMPLATE.md

**Sections:**
1. Critical rules (5 mandatory rules)
2. Root-level folders table
3. Platform folders table
4. Common modules table
5. Documentation folders table
6. File naming conventions
7. Enforcement checklist
8. Validation command reference
9. Update history

**Customize:**
- Add your specific apps to platform folders
- Add your specific modules to common folder
- Update documentation folders for your apps

### PROJECT-INSTRUCTIONS-TEMPLATE.md

**Sections:**
1. How to use this file
2. Global monorepo rules
3. App sections (one per app)
4. Platform-specific instructions (Android, iOS, Desktop)
5. Common modules documentation
6. Build system instructions
7. Documentation placement guide
8. Archive instructions

**Customize:**
- Fill in app details, architecture, features
- Fill in performance requirements
- Fill in database tables
- Update build configuration

### CLAUDE-MONOREPO-TEMPLATE.md

**Sections:**
1. Pre-operation checklist
2. Monorepo registry system rules
3. Hard rules
4. IDEACODE commands
5. Folder structure overview
6. File naming conventions
7. MCP tools reference
8. Quality gates

**Customize:**
- Update project name
- Update key locations if different
- Add project-specific rules if needed

---

## ✅ Validation After Setup

**Run these checks:**

```bash
# Verify all required folders exist
ls -la android/ ios/ desktop/ common/ docs/

# Verify registry files exist
ls -la FOLDER-REGISTRY.md PROJECT-INSTRUCTIONS.md .claude/CLAUDE.md

# Verify no placeholder values remain
grep -r "{" FOLDER-REGISTRY.md PROJECT-INSTRUCTIONS.md .claude/CLAUDE.md | grep -v "```"
```

**All placeholders should be replaced with actual values.**

---

## 📚 Reference

These templates are based on:
- IDEACODE v9.0 framework
- MainAvanues monorepo structure
- Platform-First organization pattern
- KMP (Kotlin Multiplatform) best practices

**See also:**
- `docs/IDEACODE-MONOREPO-HANDOVER.md` - Full handover documentation
- `docs/MONOREPO-REGISTRY-SYSTEM-SUMMARY.md` - Registry system summary
- `.ideacode/agents/monorepo-migration-refactor/` - Migration agent

---

**Copy these templates to: `/Volumes/M-Drive/Coding/ideacode/templates/`**
