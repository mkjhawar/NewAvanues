# Living Document: MasterDocs Protocol

**Version:** 1.0
**Updated:** 2026-01-11
**Status:** Active
**Scope:** IDEACODE Framework - Global and Project-Specific

---

## Purpose

This protocol defines how to create and maintain dual-format documentation (human-readable and AI-readable) in the MasterDocs system. It ensures documentation stays synchronized with code changes and is consumable by both humans and LLMs.

---

## Directory Structure

```
{project}/Docs/MasterDocs/
├── {Module}/                    # Per-module documentation
│   ├── README.md               # Human: Overview, architecture, usage
│   └── html/                   # Human: Visual diagrams (optional)
│       ├── index.html          # Landing page
│       ├── architecture.html   # System architecture
│       ├── flowcharts.html     # Process flows
│       ├── sequence-diagrams.html
│       └── class-diagrams.html
├── AI/                         # AI-readable documents
│   ├── PLATFORM-INDEX.ai.md    # Module registry, deps, APIs
│   ├── CLASS-INDEX.ai.md       # All classes with methods/fields
│   ├── REFACTORING-GUIDE.ai.md # Code health, SOLID compliance
│   ├── API-REFERENCE.ai.md     # API endpoints and signatures
│   └── DEPENDENCY-MAP.ai.md    # Module dependency graph
└── LD/                         # Living Documents (meta)
    ├── LD-Platform-Overview-V1.md
    ├── LD-Module-Registry-V1.md
    ├── LD-API-Reference-V1.md
    └── LD-MasterDocs-Protocol-V1.md (this file)
```

---

## Human-Readable Documents

### Format Requirements

1. **README.md (per module)**
   - Overview section with purpose
   - Architecture diagrams (ASCII or Mermaid)
   - Key classes table with descriptions
   - Usage examples with code blocks
   - API reference table

2. **HTML Diagrams (optional)**
   - Interactive visual diagrams
   - Mermaid.js for rendering
   - Dark/light theme support
   - Navigation between diagram types

### Template: Module README.md

```markdown
# {Module Name}

{One-line description}

## Overview
{2-3 paragraph explanation}

## Architecture
\`\`\`
┌─────────────────┐
│   Component A   │
└────────┬────────┘
         │
┌────────▼────────┐
│   Component B   │
└─────────────────┘
\`\`\`

## Key Classes

| Class | Purpose | Lines |
|-------|---------|-------|
| ClassA | Does X | 150 |
| ClassB | Does Y | 200 |

## Usage

\`\`\`kotlin
val instance = Module.create()
instance.initialize()
\`\`\`

## API Reference

| Method | Parameters | Returns |
|--------|------------|---------|
| method1 | param: Type | Result |
```

---

## AI-Readable Documents

### Format Requirements

All AI documents use:
- `.ai.md` extension
- YAML code blocks for structured data
- Flat hierarchy (no deep nesting)
- Explicit field names
- Commit references for traceability

### Document Types

#### 1. PLATFORM-INDEX.ai.md
Module registry with capabilities, dependencies, APIs.

```yaml
## MODULE_REGISTRY

### {MODULE_NAME}
\`\`\`yaml
id: module-id
path: /path/to/module
type: kmp-library|android-app|etc
status: production|development
version: x.y.z
loc: number
purpose: One-line description
key_classes:
  - ClassName: Short description
dependencies:
  - DependencyModule
capabilities:
  - capability_one
  - capability_two
\`\`\`
```

#### 2. CLASS-INDEX.ai.md
All classes with methods, fields, patterns.

```yaml
### ClassName
\`\`\`yaml
package: com.company.module
file: ClassName.kt
type: class|interface|object|sealed_class|data_class
lines: number (optional)
commit: hash (if recently added/modified)
purpose: What this class does
extends: ParentClass (optional)
implements: InterfaceName (optional)
pattern: singleton|factory|builder|etc (optional)
methods:
  - methodName(param: Type): ReturnType
  - anotherMethod(): Unit
fields: (optional)
  - fieldName: Type
\`\`\`
```

#### 3. REFACTORING-GUIDE.ai.md
Code health status and SOLID compliance.

```yaml
## CURRENT_STATUS
\`\`\`yaml
solid_compliance: 9.5/10
critical_issues: 0
high_issues: 0
status: HEALTHY|NEEDS_ATTENTION|CRITICAL

last_refactoring_commits:
  - hash: "commit message"
\`\`\`

## RESOLVED_ISSUES

### ISSUE_NAME [RESOLVED]
\`\`\`yaml
status: RESOLVED
resolved_in: file.kt or commit hash
evidence:
  - What was done
  - How it was verified
verification: How to confirm it works
\`\`\`

## REMAINING_IMPROVEMENTS (Optional/Future)
\`\`\`yaml
optional_enhancements:
  - Enhancement description
technical_debt: LOW|MEDIUM|HIGH
\`\`\`
```

#### 4. API-REFERENCE.ai.md
API signatures and endpoints.

```yaml
### {module_name}
\`\`\`yaml
process_command: "method.signature(params): ReturnType"
another_api: "signature"
\`\`\`
```

---

## Update Triggers

| Event | Documents to Update |
|-------|---------------------|
| New class added | CLASS-INDEX.ai.md |
| New module added | PLATFORM-INDEX.ai.md + README.md |
| API changed | PLATFORM-INDEX.ai.md + API-REFERENCE.ai.md |
| SOLID refactoring | REFACTORING-GUIDE.ai.md + CLASS-INDEX.ai.md |
| Architecture change | Module README.md + html diagrams |
| New dependency | PLATFORM-INDEX.ai.md + DEPENDENCY-MAP.ai.md |
| Bug fix | REFACTORING-GUIDE.ai.md (if structural) |
| Major feature | All relevant docs |

---

## Maintenance Workflow

### On Every Commit

1. **If new classes/interfaces added:**
   ```bash
   # Update CLASS-INDEX.ai.md with new entries
   ```

2. **If APIs changed:**
   ```bash
   # Update PLATFORM-INDEX.ai.md API section
   # Update API-REFERENCE.ai.md
   ```

### On Feature Completion

1. Review all affected modules
2. Update README.md files for changed modules
3. Regenerate HTML diagrams if architecture changed
4. Update REFACTORING-GUIDE.ai.md with current status

### On Sprint/Release

1. Full audit of all MasterDocs
2. Version increment for Living Documents
3. Archive previous versions if major changes

---

## CLAUDE.md Integration

### Global CLAUDE.md Addition

Add to `{CODING_ROOT}/.claude/CLAUDE.md`:

```markdown
## MASTERDOCS PROTOCOL

MasterDocs contains dual-format documentation that must stay synchronized:

**Human-Readable:** `/Docs/MasterDocs/{Module}/README.md`
**AI-Readable:** `/Docs/MasterDocs/AI/*.ai.md`

### Update Triggers

| Event | Action Required |
|-------|-----------------|
| New class added | Update AI/CLASS-INDEX.ai.md |
| New module added | Update AI/PLATFORM-INDEX.ai.md + create README.md |
| API changed | Update AI/PLATFORM-INDEX.ai.md + LD/LD-API-Reference |
| Refactoring done | Update AI/REFACTORING-GUIDE.ai.md with commit refs |

### AI Document Format

All `.ai.md` files use YAML blocks:
- Flat structure (no deep nesting)
- Explicit field names
- Commit hashes for traceability
- Version header at top

### Verification

Before commits touching >3 files, verify:
1. CLASS-INDEX.ai.md reflects new/changed classes
2. PLATFORM-INDEX.ai.md reflects API changes
3. Module README.md reflects architecture changes
```

### Project-Specific CLAUDE.md Addition

Add to `{project}/.claude/CLAUDE.md`:

```markdown
## MASTERDOCS MAINTENANCE

This project uses the MasterDocs dual-format documentation system.

**Location:** `/Docs/MasterDocs/`

### Required Updates

When modifying code, update these documents:

| Change Type | Documents |
|-------------|-----------|
| New class | AI/CLASS-INDEX.ai.md |
| New module | AI/PLATFORM-INDEX.ai.md, {Module}/README.md |
| API change | AI/PLATFORM-INDEX.ai.md, AI/API-REFERENCE.ai.md |
| Refactoring | AI/REFACTORING-GUIDE.ai.md (mark issues resolved) |

### Session Start

1. Read AI/PLATFORM-INDEX.ai.md for module overview
2. Read AI/CLASS-INDEX.ai.md for class reference
3. Read AI/REFACTORING-GUIDE.ai.md for code health status

### Before Commit

Verify MasterDocs are updated if:
- New classes/interfaces created
- APIs modified
- Architecture changed
- Technical debt resolved
```

---

## Validation Checklist

Before considering MasterDocs complete:

- [ ] Every module has README.md
- [ ] AI/PLATFORM-INDEX.ai.md lists all modules
- [ ] AI/CLASS-INDEX.ai.md lists all public classes
- [ ] AI/REFACTORING-GUIDE.ai.md has current status
- [ ] All resolved issues have commit references
- [ ] No TODO/FIXME items in AI documents
- [ ] Version numbers match across documents

---

## Example: Adding a New Module

1. **Create README.md:**
   ```
   /Docs/MasterDocs/NewModule/README.md
   ```

2. **Update PLATFORM-INDEX.ai.md:**
   ```yaml
   ### NEWMODULE
   \`\`\`yaml
   id: newmodule
   path: /Modules/NewModule
   ...
   \`\`\`
   ```

3. **Update CLASS-INDEX.ai.md:**
   ```yaml
   ## NEWMODULE_CLASSES

   ### MainClass
   \`\`\`yaml
   ...
   \`\`\`
   ```

4. **If complex architecture, create HTML diagrams:**
   ```
   /Docs/MasterDocs/NewModule/html/index.html
   ```

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-11 | Initial protocol |

---

*This is a Living Document. Update as the MasterDocs system evolves.*
