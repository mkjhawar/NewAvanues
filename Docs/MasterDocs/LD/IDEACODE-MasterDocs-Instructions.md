# IDEACODE MasterDocs Instructions

Instructions for IDEACODE framework to maintain dual-format documentation.

---

## FOR GLOBAL CLAUDE.md (Add to IDEACODE)

```markdown
## MASTERDOCS SYSTEM

Dual-format documentation for AI and human consumption.

### Structure

```
{project}/Docs/MasterDocs/
├── {Module}/README.md          # Human: Overview, architecture, usage
├── {Module}/html/              # Human: Visual diagrams (optional)
├── AI/PLATFORM-INDEX.ai.md     # AI: Module registry, deps, APIs
├── AI/CLASS-INDEX.ai.md        # AI: All classes with methods
├── AI/REFACTORING-GUIDE.ai.md  # AI: Code health, SOLID status
└── LD/LD-*.md                  # Living Documents (meta)
```

### AI Document Format

All `.ai.md` files use YAML blocks:
```yaml
### ClassName
\`\`\`yaml
package: com.company.module
file: ClassName.kt
type: class|interface|object
purpose: What this class does
methods:
  - methodName(param: Type): ReturnType
\`\`\`
```

### Update Triggers

| Event | Update Required |
|-------|-----------------|
| New class | AI/CLASS-INDEX.ai.md |
| New module | AI/PLATFORM-INDEX.ai.md + {Module}/README.md |
| API changed | AI/PLATFORM-INDEX.ai.md |
| Refactoring | AI/REFACTORING-GUIDE.ai.md (mark resolved with commit) |
| Architecture | {Module}/README.md + html diagrams |

### Session Start (if MasterDocs exists)

1. Read AI/PLATFORM-INDEX.ai.md for module overview
2. Read AI/CLASS-INDEX.ai.md for class reference
3. Read AI/REFACTORING-GUIDE.ai.md for code health

### Before Commit

If code changes affect >3 files, verify MasterDocs updated.
```

---

## FOR PROJECT-SPECIFIC CLAUDE.md (Per Project)

```markdown
## MASTERDOCS

Location: `/Docs/MasterDocs/`

### Document Types

| Type | Path | Purpose |
|------|------|---------|
| Human | {Module}/README.md | Overview, diagrams, usage |
| AI | AI/*.ai.md | Structured YAML for LLMs |
| Living | LD/LD-*.md | Meta documentation |

### Maintenance Rules

1. **New class added** → Update AI/CLASS-INDEX.ai.md
2. **New module added** → Create README.md + update PLATFORM-INDEX.ai.md
3. **API changed** → Update PLATFORM-INDEX.ai.md API section
4. **Issue fixed** → Mark resolved in REFACTORING-GUIDE.ai.md with commit hash

### AI Document Keys

```yaml
# PLATFORM-INDEX.ai.md
id, path, type, status, version, purpose, key_classes, dependencies, capabilities

# CLASS-INDEX.ai.md
package, file, type, lines, commit, purpose, methods, fields

# REFACTORING-GUIDE.ai.md
solid_compliance, critical_issues, status, resolved_issues
```

### Commit Format

When updating MasterDocs:
```
docs(masterdocs): update {document} for {reason}

- Specific change 1
- Specific change 2
```
```

---

## IDEACODE COMMAND INTEGRATION

### /i.document Command

Add MasterDocs generation to `/i.document`:

```markdown
## /i.document

### Modifiers

| Modifier | Action |
|----------|--------|
| .all | Generate full MasterDocs suite |
| .ai | Generate AI-readable docs only |
| .human | Generate human-readable docs only |
| .module {name} | Generate docs for specific module |
| .update | Update existing docs based on changes |

### .all Workflow

1. Scan all modules in project
2. For each module:
   - Generate/update README.md
   - Generate/update html diagrams (if complex)
3. Generate AI/PLATFORM-INDEX.ai.md
4. Generate AI/CLASS-INDEX.ai.md
5. Generate AI/REFACTORING-GUIDE.ai.md
6. Update LD/ living documents
```

### /i.develop Post-Hook

After `/i.develop` completion, auto-check:

```markdown
If feature adds new classes:
  - Prompt: "Update AI/CLASS-INDEX.ai.md? (Y/n)"

If feature adds new module:
  - Prompt: "Create MasterDocs for new module? (Y/n)"
```

### /i.fix Post-Hook

After `/i.fix` completion:

```markdown
If fix resolves documented issue:
  - Auto-update AI/REFACTORING-GUIDE.ai.md
  - Mark issue as RESOLVED with commit hash
```

---

## CREATING MASTERDOCS FOR NEW PROJECT

### Initial Setup Command

```bash
/i.document .init
```

Creates:
```
Docs/MasterDocs/
├── AI/
│   ├── PLATFORM-INDEX.ai.md
│   ├── CLASS-INDEX.ai.md
│   └── REFACTORING-GUIDE.ai.md
├── LD/
│   └── LD-MasterDocs-Protocol-V1.md
└── {FirstModule}/
    └── README.md
```

### Template: Empty AI/PLATFORM-INDEX.ai.md

```markdown
# PLATFORM-INDEX
# AI-Readable Platform Documentation Index
# Version: 1.0 | Updated: {DATE}

---

## PLATFORM_METADATA
\`\`\`yaml
name: {Project Name}
type: {Type}
architecture: {Architecture}
status: development
\`\`\`

---

## MODULE_REGISTRY

### {FIRST_MODULE}
\`\`\`yaml
id: {module-id}
path: /{path}
type: {type}
status: development
purpose: {purpose}
key_classes: []
dependencies: []
capabilities: []
\`\`\`

---

# END PLATFORM-INDEX
```

### Template: Empty AI/CLASS-INDEX.ai.md

```markdown
# CLASS-INDEX
# AI-Readable Class Reference
# Version: 1.0 | Updated: {DATE}

---

## {MODULE}_CLASSES

(Classes will be added as development progresses)

---

# END CLASS-INDEX
```

### Template: Empty AI/REFACTORING-GUIDE.ai.md

```markdown
# REFACTORING-GUIDE
# AI-Readable Refactoring Status
# Version: 1.0 | Updated: {DATE}
# Status: NEW PROJECT

---

## CURRENT_STATUS

\`\`\`yaml
solid_compliance: N/A
critical_issues: 0
high_issues: 0
status: NEW
\`\`\`

---

## ISSUES_TRACKING

(Issues will be tracked as development progresses)

---

# END REFACTORING-GUIDE
```

---

## VALIDATION

### Pre-Commit Check

```bash
# If MasterDocs exists and code files changed
if [ -d "Docs/MasterDocs/AI" ]; then
  # Warn if AI docs older than code changes
  echo "Warning: Verify MasterDocs are updated"
fi
```

### /i.review Integration

Add to `/i.review` checklist:
- [ ] New classes reflected in CLASS-INDEX.ai.md
- [ ] API changes reflected in PLATFORM-INDEX.ai.md
- [ ] Resolved issues marked in REFACTORING-GUIDE.ai.md

---

*Use this document as the source of truth for IDEACODE MasterDocs integration.*
