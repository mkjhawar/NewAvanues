# Documentation Migration Tracking Table
# Generated: 2025-02-07
# Purpose: Track migration of all documentation to new compartmentalized structure

## Migration Status Legend
- ⬜ Not Started
- 🟨 In Progress  
- ✅ Completed
- ❌ Error/Issue
- 🔄 Needs Review

## Phase 1: Create New Folder Structure

| Task | Status | Notes |
|------|--------|-------|
| Create /docs/voiceos-master/ | ⬜ | Main system documentation |
| Create /docs/modules/ | ⬜ | Module container |
| Create standard subfolders for voiceos-master | ⬜ | 13 standard folders |
| Create module folders (12 modules identified) | ⬜ | Each with 13 standard subfolders |
| Create /docs/archive/ | ⬜ | For deprecated content |

## Phase 2: Root Level Files Migration (34 files)

| File | Current Location | New Location | Status | ROT Check |
|------|-----------------|--------------|--------|-----------|
| CLAUDE-BUILD-ERROR-FIXER-TEMPLATE.md | /docs/ | /voiceos-master/reference/ | ⬜ | Template = reference |
| DATABASE-MIGRATION-OBJECTBOX-TO-ROOM-GUIDE.md | /docs/ | /voiceos-master/implementation/ | ⬜ | Migration guide = implementation |
| DeviceManager-Conditional-Loading-Guide.md | /docs/ | /modules/DeviceManager/developer-manual/ | ⬜ | Module-specific guide |
| DeviceManager-Developer-Guide.md | /docs/ | /modules/DeviceManager/developer-manual/ | ⬜ | Module developer manual |
| DeviceManager-Refactoring-Complete.md | /docs/ | /modules/DeviceManager/status/ | ⬜ | Refactoring status |
| KEYBOARD-MODULE-MIGRATION-PLAN.md | /docs/ | /modules/keyboard/roadmap/ | ⬜ | Migration plan = roadmap |
| ROOM-IMPLEMENTATION-GUIDE.md | /docs/ | /voiceos-master/implementation/ | ⬜ | System-wide implementation |
| VoiceAccessibility-Compilation-Status-250128-1445.md | /docs/ | /modules/voice-accessibility/status/ | ⬜ | Module status |
| VoiceCursor-Gesture-Migration-Guide.md | /docs/ | /modules/VoiceCursor/implementation/ | ⬜ | Module implementation |
| VOS4-Architecture-Specification.md | /docs/ | /voiceos-master/architecture/ | ⬜ | System architecture |
| VOS4-Changelog-Master.md | /docs/ | /voiceos-master/changelog/ | ⬜ | System changelog |
| VOS4-Documentation-Index.md | /docs/ | /voiceos-master/reference/ | ⬜ | Documentation index |
| VOS4-Filing-Norms-Guide.md | /docs/ | /voiceos-master/standards/ | ⬜ | System standards |
| VOS4-Initialization-Framework-Implementation-Guide.md | /docs/ | /voiceos-master/implementation/ | ⬜ | System implementation |
| VOS4-Initialization-Implementation-Guide.md | /docs/ | /voiceos-master/implementation/ | ⬜ | System implementation |
| VOS4-Initialization-Knowledge-Base.md | /docs/ | /voiceos-master/reference/ | ⬜ | Knowledge base |
| VOS4-Initialization-Monitoring-Design.md | /docs/ | /voiceos-master/architecture/ | ⬜ | System design |
| VOS4-Initialization-Technical-Design-Document.md | /docs/ | /voiceos-master/architecture/ | ⬜ | Technical design |
| VOS4-Initialization-Training-Materials.md | /docs/ | /voiceos-master/developer-manual/ | ⬜ | Training materials |
| VOS4-Initialization-Troubleshooting-Runbook.md | /docs/ | /voiceos-master/reference/ | ⬜ | Troubleshooting guide |
| VOS4-LEGACY-AVENUE-FEATURE-COMPARISON-REPORT.md | /docs/ | /voiceos-master/reference/ | ⬜ | Comparison report |
| VOS4-Master-Changelog.md | /docs/ | /voiceos-master/changelog/ | ⬜ | Master changelog |
| VOS4-Master-Inventory-Deprecated.md | /docs/ | /archive/2025/ | ⬜ | Deprecated |
| VOS4-Master-Inventory.md | /docs/ | /voiceos-master/reference/ | ⬜ | Master inventory |
| VOS4-Overview-Guide.md | /docs/ | /voiceos-master/architecture/ | ⬜ | System overview |
| VOS4-Project-Status-250130.md | /docs/ | /voiceos-master/status/ | ⬜ | Project status |
| VOS4-SETTINGS-PARITY-CHECKLIST.md | /docs/ | /voiceos-master/testing/ | ⬜ | Testing checklist |
| VOS4-Speech-Engine-Initialization-Framework-Design.md | /docs/ | /modules/SpeechRecognition/architecture/ | ⬜ | Module architecture |
| VOS4-STATUS-ROOM-MIGRATION.md | /docs/ | /voiceos-master/status/ | ⬜ | Migration status |
| VOS4-Test-Coverage-Report.md | /docs/ | /voiceos-master/testing/ | ⬜ | Test coverage |
| VOS4-Testing-Automation-Guide.md | /docs/ | /voiceos-master/testing/ | ⬜ | Testing guide |
| VOS4-Testing-Enhancement-Guide.md | /docs/ | /voiceos-master/testing/ | ⬜ | Testing guide |
| VOS4-Testing-QuickStart-Guide.md | /docs/ | /voiceos-master/testing/ | ⬜ | Testing guide |
| VOS4-TODO-Master.md | /docs/ | /voiceos-master/project-management/ | ⬜ | Master TODO |

## Phase 3: Module Documentation Migration

### Modules Identified:
1. **speech-recognition** - 20+ files
2. **voice-cursor** - 15+ files  
3. **voice-accessibility** - 10+ files
4. **device-manager** - 8+ files
5. **command-manager** - 5+ files
6. **data-manager** - 5+ files
7. **hud-manager** - 3+ files
8. **localization-manager** - 5+ files
9. **voice-ui** - 10+ files
10. **vos-data-manager** - 5+ files
11. **keyboard** - New module from migration plan
12. **settings** - New module from parity checklist

## Phase 4: Folder Content Migration

| Source Folder | File Count | Destination | Status | Notes |
|--------------|------------|-------------|--------|-------|
| /ai-context/ | 2 | /voiceos-master/project-management/ | ⬜ | AI context docs |
| /ainotes/ | 1 | /archive/2025/ai-notes/ | ⬜ | Old AI notes |
| /analysis/ | 25+ | Various module /architecture/ | ⬜ | Split by module |
| /api/ | 1 | /voiceos-master/reference/ | ⬜ | API reference |
| /apps/ | 1 | /modules/VoiceRecognition/ | ⬜ | App docs |
| /architecture/ | 30+ | Split between master and modules | ⬜ | Architecture docs |
| /archive/ | 27+ | /archive/ | ⬜ | Keep in archive |
| /commits/ | 1 | /voiceos-master/project-management/ | ⬜ | Commit history |
| /currentstatus/ | 1 | /voiceos-master/status/ | ⬜ | Merge with status |
| /deprecated-do-not-read/ | 1+ | /archive/deprecated/ | ⬜ | Archive |
| /development/ | 3+ | /voiceos-master/developer-manual/ | ⬜ | Dev resources |
| /diagrams/ | 6+ | Split between master and modules | ⬜ | Visual docs |
| /documentation-control/ | 6+ | /voiceos-master/standards/ | ⬜ | Doc standards |
| /engines/ | 1 | /modules/SpeechRecognition/ | ⬜ | Engine docs |
| /guides/ | 6+ | /voiceos-master/developer-manual/ | ⬜ | Dev guides |
| /implementation/ | 5+ | /voiceos-master/implementation/ | ⬜ | Implementation docs |
| /implementation-plans/ | 3+ | /voiceos-master/roadmap/ | ⬜ | Future plans |
| /issues/ | 6+ | /voiceos-master/project-management/ | ⬜ | Issue tracking |
| /metrics/ | 1 | /voiceos-master/project-management/ | ⬜ | Metrics |
| /migration/ | 1 | /voiceos-master/implementation/ | ⬜ | Migration docs |
| /objectbox/ | 1 | /archive/2025/objectbox/ | ⬜ | Deprecated DB |
| /planning/ | 40+ | /voiceos-master/roadmap/ | ⬜ | Planning docs |
| /porting/ | 2 | Merge with /migration/ | ⬜ | Porting plans |
| /precompaction-reports/ | 5+ | /voiceos-master/project-management/ | ⬜ | Reports |
| /project-instructions/ | 8+ | /voiceos-master/standards/ | ⬜ | Standards |
| /project-management/ | 8+ | /voiceos-master/project-management/ | ⬜ | PM docs |
| /reference/ | 1 | /voiceos-master/reference/ | ⬜ | Quick ref |
| /research/ | 3+ | /voiceos-master/project-management/ | ⬜ | Research |
| /status/ | 50+ | Split between master and module /status/ | ⬜ | Status docs |
| /technical/ | 1 | /voiceos-master/implementation/ | ⬜ | Technical docs |
| /technicalnotes/ | 1 | Merge with /technical/ | ⬜ | Tech notes |
| /todo/ | 7+ | Split between master and module /project-management/ | ⬜ | TODOs |

## Phase 5: Update Agent-Instructions

| File | Update Required | Status | Notes |
|------|----------------|--------|-------|
| /Volumes/M Drive/Coding/Warp/Agent-Instructions/DOCUMENTATION-GUIDE.md | Update folder structure | ⬜ | New paths |
| /Volumes/M Drive/Coding/Warp/Agent-Instructions/MASTER-AGENT-INSTRUCTIONS.md | Update doc references | ⬜ | New structure |
| /Volumes/M Drive/Coding/Warp/Agent-Instructions/MASTER-STANDARDS.md | Update standards paths | ⬜ | New locations |
| /Volumes/M Drive/Coding/vos4/claude.md | Update all doc paths | ⬜ | Critical |
| /Volumes/M Drive/Coding/vos4/CLAUDE.md | Update all doc paths | ⬜ | Critical |
| /Volumes/M Drive/Coding/Warp/claude.md | Update all doc paths | ⬜ | Critical |

## ROT Verification Checklist

### Before Moving Each File:
- [ ] Does the destination make logical sense?
- [ ] Is this the most specific appropriate location?
- [ ] Are related files going to the same place?
- [ ] Will this improve discoverability?

### After Moving Each File:
- [ ] File exists in new location
- [ ] File removed from old location
- [ ] Any internal links updated
- [ ] Any external references updated

### Final Verification:
- [ ] All 475 markdown files accounted for
- [ ] No files lost during migration
- [ ] All modules have standard folder structure
- [ ] Agent-Instructions updated
- [ ] Claude.md files updated