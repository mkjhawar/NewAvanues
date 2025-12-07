<!--
filename: DOCUMENT-ORGANIZATION-STRUCTURE.md
created: 2025-01-27 18:20:00 PST
author: VOS4 Development Team
purpose: Master document organization, naming conventions, and placement rules
version: 2.0.0
priority: MANDATORY
changelog:
- 2025-01-27 18:25:00 PST: Reorganized to place planning docs in Planning folder
-->

# 📁 Document Organization Structure & Naming Conventions

## 🔴 MANDATORY: Document Naming & Placement Rules

### General Document Naming Convention Format
```
[Module]-[DocumentType]-[Subtype].md
Examples:
- SpeechRecognition-Changelog.md
- VoiceUI-Architecture.md  
- CommandManager-API-Reference.md
- VOS4-Status-Current.md
```

### Time-Stamped & Tracking Document Naming Convention
**MANDATORY Format:** `MODULENAME/APPNAME-WhatItIs-YYMMDD-HHMM.md`

**Components:**
- **MODULENAME/APPNAME**: Module or application name (e.g., SPEECHRECOGNITION, VOS4, LEGACYAVENUE)
- **WhatItIs**: Brief description of what the file contains (e.g., MIGRATION-STATUS, BUILD-STATUS, INVENTORY)
- **YYMMDD**: Date in 6-digit format (year-month-day)
- **HHMM**: Time in 24-hour format (not 12-hour format)

**Examples:**
```
SPEECHRECOGNITION-MIGRATION-STATUS-250903-1430.md
VOS4-BUILD-STATUS-250903-0430.md
LEGACYAVENUE-INVENTORY-250903-0425.md
SPEECHRECOGNITION-IMPLEMENTATION-GUIDE-250903-1615.md
VOS4-ARCHITECTURE-DIAGRAM-250903-0930.md
```

**Apply time-stamped format to:**
- Status reports and tracking documents
- Migration documentation
- Implementation guides
- Architecture diagrams (when session-specific)
- Analysis reports
- Any temporary or session-specific files
- Precompaction reports
- Error logs and debugging documentation

### Case Rules
- **Module names:** PascalCase (e.g., SpeechRecognition)
- **Document types:** PascalCase with hyphens (e.g., API-Reference)
- **Filenames:** Hyphenated PascalCase for general docs, UPPERCASE for time-stamped docs
- **Directories:** lowercase (except special cases like CodeImport/, Planning/)

## 📂 Master Document Locations - IMPROVED STRUCTURE

### 1. Root Documentation (Minimal - Entry Points Only)
```
/VOS4/docs/
├── README.md                               # Documentation guide & index
├── INDEX.md                               # Complete document index
└── QUICK-START.md                        # Quick reference guide
```

### 2. Master Project Documents
```
/VOS4/docs/Master/
├── Changelogs/
│   ├── CHANGELOG-MASTER.md               # Master project changelog
│   ├── CHANGELOG-CURRENT.md              # Current sprint changelog
│   │
│   └── Archives/
│       ├── ARCHIVE-INDEX.md
│       └── CHANGELOG-2025Q1.md          # Quarterly archives
│
├── Status/
│   ├── VOS4-STATUS-CURRENT.md           # Live project status
│   ├── VOS4-STATUS-SUMMARY.md           # Executive summary
│   ├── VOS4-STATUS-METRICS.md           # Performance metrics
│   │
│   └── Archives/
│       ├── VOS4-STATUS-202501.md        # Monthly archives
│       └── VOS4-STATUS-202412.md
│
├── TODO/
│   ├── VOS4-TODO-MASTER.md              # Master TODO list
│   ├── VOS4-TODO-PRIORITY.md            # Priority items
│   ├── VOS4-TODO-BACKLOG.md             # Backlog items
│   │
│   └── Completed/
│       └── VOS4-TODO-COMPLETED-2025Q1.md
│
└── Inventories/
    ├── VOS4-MASTER-INVENTORY.md         # Complete component inventory
    └── VOS4-FEATURE-MATRIX.md           # Feature comparison matrix
```

### 3. Planning & Architecture Documents (WHERE THEY BELONG)
```
/VOS4/docs/Planning/
├── Project/
│   ├── VOS4-PRD.md                      # Product Requirements Document
│   ├── VOS4-ROADMAP.md                  # Project roadmap
│   ├── VOS4-MILESTONES.md               # Milestone definitions
│   └── VOS4-TIMELINE.md                 # Timeline and schedule
│
├── Architecture/
│   ├── VOS4-ARCHITECTURE-MASTER.md      # System architecture
│   ├── VOS4-INTERACTION-MAP.md          # Module interactions
│   ├── VOS4-DATA-FLOW.md                # Data flow design
│   ├── VOS4-TECHNOLOGY-STACK.md         # Tech stack decisions
│   │
│   └── Decisions/
│       ├── ADR-001-Direct-Implementation.md  # Architecture Decision Records
│       └── ADR-002-ObjectBox-Only.md
│
├── Implementation/
│   ├── VOS4-IMPLEMENTATION-STRATEGY.md  # Implementation approach
│   ├── VOS4-MIGRATION-PLAN.md           # Migration from VOS3
│   ├── VOS4-TESTING-STRATEGY.md         # Testing approach
│   └── VOS4-DEPLOYMENT-PLAN.md          # Deployment strategy
│
└── Features/
    ├── Feature-Specifications/
    │   ├── Feature-SpeechRecognition-Spec.md
    │   └── Feature-VoiceUI-Spec.md
    │
    └── Feature-Roadmap.md
```

### 4. Commits & Reviews
```
/VOS4/docs/Commits/
├── Current/
│   ├── PRE-COMMIT-SUMMARIES.md          # Active pre-commit reviews
│   └── POST-COMMIT-REVIEWS.md           # Post-commit analysis
│
└── Archives/
    ├── PRE-COMMIT-2025Q1.md            # Quarterly archives
    └── POST-COMMIT-2025Q1.md
```

### 5. Module-Level Documentation
```
/VOS4/docs/modules/
├── [modulename]/                         # lowercase directory
│   ├── [Module]-Changelog.md            # Module changelog (MANDATORY)
│   ├── [Module]-Status.md               # Module status
│   ├── [Module]-Architecture.md         # Module architecture
│   ├── [Module]-API-Reference.md        # API documentation
│   ├── [Module]-Developer-Manual.md     # Developer guide
│   ├── [Module]-Implementation-Status.md # Implementation progress
│   ├── [Module]-Master-Inventory.md     # Component inventory
│   ├── [Module]-TODO.md                 # Module-specific tasks
│   ├── [Module]-Test-Results.md         # Testing documentation
│   │
│   ├── Diagrams/                        # Visual documentation
│   │   ├── [Module]-Architecture-Diagram.mmd
│   │   ├── [Module]-Sequence-Diagram.mmd
│   │   ├── [Module]-Flow-Chart.mmd
│   │   └── [Module]-UI-Wireframes.png
│   │
│   └── Archives/
│       ├── [Module]-Changelog-202501.md
│       └── [Module]-Status-202501.md
│
└── speechrecognition/                    # Example
    ├── SpeechRecognition-Changelog.md
    ├── SpeechRecognition-Status.md
    └── ... (standard structure)
```

### 6. Visual Documentation
```
/VOS4/docs/Visuals/
├── System/                               # System-level visuals
│   ├── VOS4-System-Architecture.mmd
│   ├── VOS4-Module-Dependencies.mmd
│   └── VOS4-Data-Flow.mmd
│
├── UI-UX/
│   ├── Wireframes/
│   │   ├── Screen-[Name]-Wireframe.png
│   │   └── Screen-Flow-Diagram.mmd
│   │
│   ├── Mockups/
│   │   └── UI-Mockup-[Name].png
│   │
│   └── Design-System.md
│
├── Sequences/
│   ├── API-Sequence-Diagrams.mmd
│   ├── User-Flow-Sequences.mmd
│   └── System-Interaction-Sequences.mmd
│
└── Technical/
    ├── Database-Schema.mmd
    ├── Network-Topology.mmd
    └── Deployment-Diagram.mmd
```

### 7. Templates & Standards
```
/VOS4/docs/Templates/
├── Document-Templates/
│   ├── TEMPLATE-Changelog-Entry.md
│   ├── TEMPLATE-Status-Update.md
│   ├── TEMPLATE-PreCommit-Summary.md
│   ├── TEMPLATE-Module-Documentation.md
│   └── TEMPLATE-Architecture-Document.md
│
└── Standards/
    ├── NAMING-CONVENTIONS.md
    ├── DOCUMENTATION-STANDARDS.md
    └── DIAGRAM-STANDARDS.md
```

## 📋 Document Creation Rules

### MANDATORY for ALL Documents

#### 1. Header Block (Required)
```markdown
<!--
filename: [Exact-Filename].md
created: YYYY-MM-DD HH:MM:SS PST
author: [Author Name]
copyright: Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
purpose: [Brief description of document purpose]
last-modified: YYYY-MM-DD HH:MM:SS PST
version: X.Y.Z
module: [Module name if applicable]
location: /path/to/file/location
status: [Draft|Review|Approved|Active|Archived]
-->
```

## 🔍 Quick Location Reference - UPDATED

### Where to Find What:

| Document Type | Location | Purpose |
|--------------|----------|---------|
| **Project Planning** | `/docs/Planning/Project/` | PRD, Roadmap, Timeline |
| **Architecture** | `/docs/Planning/Architecture/` | System design, decisions |
| **Master Changelog** | `/docs/Master/Changelogs/` | Project-wide changes |
| **Master Status** | `/docs/Master/Status/` | Current project status |
| **Master TODO** | `/docs/Master/TODO/` | Project tasks |
| **Module Docs** | `/docs/modules/[module]/` | Module-specific docs |
| **Visual Docs** | `/docs/Visuals/` | All diagrams and visuals |
| **Pre-Commit** | `/docs/Commits/Current/` | Commit summaries |
| **Archives** | `[category]/Archives/` | Historical records |

## 📝 Migration Commands for Current Structure

### Move existing files to new structure
```bash
# Create new directory structure
mkdir -p docs/{Master/{Changelogs,Status,TODO/{Completed},Inventories},Planning/{Project,Architecture/{Decisions},Implementation,Features},Commits/{Current,Archives},Templates/{Document-Templates,Standards},Visuals/{System,UI-UX/{Wireframes,Mockups},Sequences,Technical}}

# Move planning documents to Planning folder
mv docs/PRD.md docs/Planning/Project/VOS4-PRD.md
mv docs/ROADMAP.md docs/Planning/Project/VOS4-ROADMAP.md
mv docs/ARCHITECTURE.md docs/Planning/Architecture/VOS4-ARCHITECTURE-MASTER.md
mv docs/INTERACTION_MAP.md docs/Planning/Architecture/VOS4-INTERACTION-MAP.md

# Move master documents to Master folder
mv docs/CHANGELOG-MASTER.md docs/Master/Changelogs/CHANGELOG-MASTER.md
mv docs/Status/Current/VOS4-Status-Current.md docs/Master/Status/VOS4-STATUS-CURRENT.md
mv docs/TODO/VOS4-TODO-Master.md docs/Master/TODO/VOS4-TODO-MASTER.md

# Move commits to proper location
mv docs/Commits/Pre-Commit-Summaries.md docs/Commits/Current/PRE-COMMIT-SUMMARIES.md

# Create archive directories
mkdir -p docs/Master/{Changelogs,Status,TODO}/Archives
mkdir -p docs/Planning/Architecture/Archives
```

## 🚨 Benefits of New Structure

### Clear Separation of Concerns
- **Planning:** All planning docs in one place
- **Master:** Project-wide tracking documents
- **Modules:** Module-specific documentation
- **Visuals:** Centralized visual assets
- **Templates:** Reusable templates

### Easier Navigation
- Logical hierarchy
- Predictable locations
- Clear naming conventions
- Consistent structure

### Better Archival
- Archives stay in their category
- Quarterly/monthly organization
- Easy to find historical data
- No clutter in root

## 📊 Document Size Management

### Archive Triggers
| Document Category | Max Size | Archive Frequency | Archive Location |
|------------------|----------|-------------------|------------------|
| Master Changelog | 500KB | Quarterly | `/Master/Changelogs/Archives/` |
| Master Status | 200KB | Monthly | `/Master/Status/Archives/` |
| Master TODO | 100KB | When 75% complete | `/Master/TODO/Completed/` |
| Pre-Commit | 400KB | Quarterly | `/Commits/Archives/` |
| Module Docs | 300KB | Quarterly | `/modules/[name]/Archives/` |

## 🔄 Cross-Reference Format

### Standard Cross-Reference Paths
```markdown
# From any document
See: `/docs/Planning/Project/VOS4-PRD.md#requirements`
Ref: `/docs/Master/Changelogs/CHANGELOG-MASTER.md#2025-01-27`
Related: `/docs/modules/speechrecognition/SpeechRecognition-Status.md`

# Relative references within same category
See: `../Status/VOS4-STATUS-CURRENT.md`
Ref: `../../Architecture/VOS4-ARCHITECTURE-MASTER.md`
```

---

**CRITICAL:** This improved structure keeps planning documents where they belong and maintains clear separation between project management, planning, and implementation documentation.