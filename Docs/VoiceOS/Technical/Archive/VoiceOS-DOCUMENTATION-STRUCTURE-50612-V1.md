# VOS3 Documentation Structure Guide
**Author:** Manoj Jhawar  
**Code-Reviewed-By:** CCA  
**Date:** 2025-01-19

## Documentation Organization

### ProjectDocs/ (Main Documentation)

All project-wide documentation belongs in `/ProjectDocs/`:

#### PRD/ (Product Requirements Documents)
- **Location:** `/ProjectDocs/PRD/`
- **Naming:** `PRD-[MODULE-NAME].md`
- **Content:** Product requirements, user stories, success metrics
- **Examples:**
  - PRD-CORE.md
  - PRD-ACCESSIBILITY.md
  - PRD-VOS-UIKIT.md
  - PRD-DATA-COMPLETE.md

#### Modules/ (Module-Specific Documentation)
- **Location:** `/ProjectDocs/Modules/[ModuleName]/`
- **Content:** Developer guides, API docs, integration guides
- **Structure:**
  ```
  ProjectDocs/Modules/
  ├── Core/
  │   ├── DEVELOPER.md
  │   ├── API.md
  │   └── INTEGRATION.md
  ├── Data/
  │   ├── DEVELOPER.md
  │   └── SCHEMA.md
  ├── VOS-UIKit/
  │   ├── DEVELOPER.md
  │   ├── COMPONENTS.md
  │   └── THEMING.md
  └── DeviceInfo/
      └── DEVELOPER.md
  ```

#### AI-Instructions/ (AI Development Guidelines)
- **Location:** `/ProjectDocs/AI-Instructions/`
- **Content:** Guidelines for AI-assisted development
- **Files:**
  - VOS3-DEVELOPMENT-GUIDELINES.md
  - VOS3-Q&A-PROTOCOL.md
  - VOS3-DESIGN-SYSTEM.md

#### CurrentStatus/ (Progress Tracking)
- **Location:** `/ProjectDocs/CurrentStatus/`
- **Content:** Issue tracking, migration status, current work
- **Files:**
  - ISSUE-1-INTERFACE-DEFINITIONS.md
  - ISSUE-2-MODULE-MIGRATION-CONTINUITY.md

#### Architecture/ (System Design)
- **Location:** `/ProjectDocs/Architecture/`
- **Content:** System architecture, design patterns, diagrams

#### Module-Contracts/ (Interface Definitions)
- **Location:** `/ProjectDocs/Module-Contracts/`
- **Content:** Module interfaces, API contracts, dependencies

#### Knowledge/ (Knowledge Base)
- **Location:** `/ProjectDocs/Knowledge/`
- **Content:** Best practices, lessons learned, FAQs

### modules/ (Code Documentation)

Module source code folders should only contain:

#### README.md
- **Location:** `/modules/[module-name]/README.md`
- **Purpose:** Quick start guide for developers
- **Content:** 
  - Module overview
  - Installation/setup
  - Basic usage examples
  - Link to full docs in ProjectDocs

#### API Documentation (if generated)
- **Location:** `/modules/[module-name]/docs/`
- **Purpose:** Auto-generated API documentation
- **Tools:** Dokka, KDoc, JavaDoc

### uiblocks/ (UI Components)

#### Component Documentation
- **Location:** `/uiblocks/[component]/README.md`
- **Purpose:** Component usage and examples
- **Content:**
  - Component API
  - Props/parameters
  - Usage examples
  - Theming

## Documentation Rules

### DO ✅
1. Place all PRDs in `/ProjectDocs/PRD/`
2. Place developer guides in `/ProjectDocs/Modules/[ModuleName]/`
3. Keep module README.md files with the code for quick reference
4. Use consistent naming: `PRD-[MODULE].md`, `DEVELOPER.md`
5. Link from module README to full docs in ProjectDocs

### DON'T ❌
1. Don't place PRDs in module source folders
2. Don't duplicate documentation
3. Don't mix product docs with technical docs
4. Don't put project-wide docs in module folders

## File Naming Conventions

### PRDs
- Pattern: `PRD-[MODULE-NAME].md`
- Example: `PRD-VOS-UIKIT.md`

### Developer Guides
- Main guide: `DEVELOPER.md`
- API docs: `API.md`
- Integration: `INTEGRATION.md`
- Schema: `SCHEMA.md`

### Status Documents
- Pattern: `ISSUE-[NUMBER]-[DESCRIPTION].md`
- Example: `ISSUE-2-MODULE-MIGRATION-CONTINUITY.md`

## Documentation Templates

### PRD Template
```markdown
# Product Requirements Document: [Module Name]
**Author:** Manoj Jhawar  
**Code-Reviewed-By:** CCA  
**Version:** 1.0.0  
**Date:** [Date]

## Executive Summary
## Vision Statement
## Core Objectives
## Key Features
## Technical Architecture
## Success Metrics
## Implementation Roadmap
```

### Developer Guide Template
```markdown
# [Module Name] Developer Guide
**Author:** Manoj Jhawar  
**Code-Reviewed-By:** CCA  

## Overview
## Architecture
## API Reference
## Integration Guide
## Examples
## Testing
## Troubleshooting
```

## Migration Checklist

When creating new documentation:

- [ ] Determine document type (PRD, Developer Guide, etc.)
- [ ] Place in correct ProjectDocs subfolder
- [ ] Use proper naming convention
- [ ] Include standard headers (Author, Code-Reviewed-By)
- [ ] Link from module README if applicable
- [ ] Update MASTER-DOCUMENTATION-INDEX.md

## Quick Reference

| Document Type | Location | Example |
|--------------|----------|---------|
| PRD | `/ProjectDocs/PRD/` | PRD-VOS-UIKIT.md |
| Developer Guide | `/ProjectDocs/Modules/[Module]/` | DEVELOPER.md |
| Module README | `/modules/[module]/` | README.md |
| AI Instructions | `/ProjectDocs/AI-Instructions/` | VOS3-GUIDELINES.md |
| Status Updates | `/ProjectDocs/CurrentStatus/` | ISSUE-1-*.md |
| Architecture | `/ProjectDocs/Architecture/` | SYSTEM-DESIGN.md |