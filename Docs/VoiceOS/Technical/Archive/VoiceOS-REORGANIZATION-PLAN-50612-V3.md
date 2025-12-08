# VOS4 Documentation Reorganization Plan V3

## FINAL STRUCTURE WITH ARCHITECTURE UNDER PLANNING

```
/VOS4/ProjectDocs/
│
├── DOCUMENT-CONTROL-MASTER.md      # STAYS IN ROOT (as requested)
├── README.md                        # Project overview
│
├── Planning/                        # ALL planning, architecture, and design docs
│   ├── VOS4-Planning-Master.md     # Master planning document
│   ├── VOS4-Planning-Timeline.md   # Timeline and milestones
│   ├── VOS4-Planning-Resources.md  # Resource allocation
│   ├── VOS4-Planning-Risks.md      # Risk analysis
│   ├── VOS4-Planning-Decisions.md  # Key decisions log
│   │
│   ├── Architecture/                # ALL architecture & design docs
│   │   ├── VOS4-Architecture-Master.md    # CONSOLIDATED system architecture
│   │   ├── VOS4-PRD-Master.md            # CONSOLIDATED master PRD
│   │   ├── VOS4-Roadmap-Master.md        # CONSOLIDATED roadmap
│   │   ├── VOS4-Implementation-Master.md  # CONSOLIDATED implementation
│   │   │
│   │   ├── Apps/                    # App-specific architecture
│   │   │   ├── VoiceAccessibility/
│   │   │   │   ├── AccessibilityService-Architecture.md
│   │   │   │   ├── AccessibilityService-PRD.md
│   │   │   │   ├── AccessibilityService-Implementation.md
│   │   │   │   ├── AccessibilityService-Analysis.md
│   │   │   │   └── TODO.md
│   │   │   │
│   │   │   ├── SpeechRecognition/
│   │   │   │   ├── SpeechRecognition-Architecture.md
│   │   │   │   ├── SpeechRecognition-PRD.md
│   │   │   │   ├── SpeechRecognition-Implementation.md
│   │   │   │   ├── SpeechRecognition-Analysis.md
│   │   │   │   └── TODO.md
│   │   │   │
│   │   │   └── VoiceUI/
│   │   │       ├── VoiceUI-Architecture.md
│   │   │       ├── VoiceUI-PRD.md
│   │   │       ├── VoiceUI-Implementation.md
│   │   │       ├── VoiceUI-Analysis.md
│   │   │       └── TODO.md
│   │   │
│   │   ├── Managers/                # Manager-specific architecture
│   │   │   ├── CoreMGR/
│   │   │   │   ├── CoreMGR-Architecture.md
│   │   │   │   ├── CoreMGR-PRD.md
│   │   │   │   ├── CoreMGR-Implementation.md
│   │   │   │   └── TODO.md
│   │   │   │
│   │   │   ├── CommandsMGR/
│   │   │   │   ├── CommandsMGR-Architecture.md
│   │   │   │   ├── CommandsMGR-PRD.md
│   │   │   │   ├── CommandsMGR-Implementation.md
│   │   │   │   └── TODO.md
│   │   │   │
│   │   │   ├── DataMGR/
│   │   │   │   ├── DataMGR-Architecture.md
│   │   │   │   ├── DataMGR-PRD.md
│   │   │   │   ├── DataMGR-Implementation.md
│   │   │   │   ├── DataMGR-Guide-Developer.md
│   │   │   │   └── TODO.md
│   │   │   │
│   │   │   ├── LocalizationMGR/
│   │   │   │   ├── LocalizationMGR-Architecture.md
│   │   │   │   ├── LocalizationMGR-PRD.md
│   │   │   │   └── TODO.md
│   │   │   │
│   │   │   └── LicenseMGR/
│   │   │       ├── LicenseMGR-Architecture.md
│   │   │       ├── LicenseMGR-PRD.md
│   │   │       └── TODO.md
│   │   │
│   │   ├── Libraries/               # Library architecture
│   │   │   ├── DeviceMGR/
│   │   │   │   ├── DeviceMGR-Architecture.md
│   │   │   │   └── TODO.md
│   │   │   ├── VoiceUIElements/
│   │   │   │   ├── VoiceUIElements-Architecture.md
│   │   │   │   └── TODO.md
│   │   │   └── UUIDCreator/
│   │   │       ├── UUIDCreator-Architecture.md
│   │   │       └── TODO.md
│   │   │
│   │   └── System/                  # System-wide docs
│   │       ├── VOS4-Implementation-ProcessingEnhancement.md
│   │       ├── VOS4-Implementation-NativeComponents.md
│   │       ├── Smartglasses-Architecture.md
│   │       └── Namespace-Architecture-Review.md
│   │
│   ├── Sprints/                     # Sprint planning
│   │   ├── VOS4-Sprint-Current.md
│   │   ├── VOS4-Sprint-Backlog.md
│   │   └── Archive/
│   │       └── VOS4-Sprint-2025-01.md
│   │
│   └── Strategies/                  # Strategic planning
│       ├── Optimum-Approach-Decision.md
│       ├── Option-Analysis.md
│       └── AI-Implementation-Timeline.md
│
├── Status/                          # All status and reporting
│   ├── Current/
│   │   ├── VOS4-Status-Comprehensive.md
│   │   ├── VOS4-Status-DocumentReorganization.md
│   │   └── VOS4-Status-CodeCompleteness.md
│   │
│   ├── Analysis/                    # Performance and system analysis
│   │   ├── VOS4-Analysis-PerformanceOverhead.md
│   │   ├── VOS4-Analysis-CPUOptimization.md
│   │   ├── VOS4-Analysis-Critical.md
│   │   └── Gradle-Analysis-Review.md
│   │
│   ├── Migration/                   # Migration tracking
│   │   ├── VOS4-Migration-Summary.md
│   │   ├── VOS4-Migration-Tracker.md
│   │   └── Phase-Reports/
│   │       └── Phase[1-3]-*.md
│   │
│   └── Archive/
│       └── [Old status reports]
│
├── TODO/                            # Master TODO tracking
│   ├── VOS4-TODO-Master.md
│   ├── VOS4-TODO-CurrentSprint.md
│   └── VOS4-TODO-Backlog.md
│
├── AI-Instructions/                 # AI system instructions
│   ├── CODING-STANDARDS.md
│   ├── DOCUMENT-STANDARDS.md       # UPDATED with new structure
│   ├── NAMESPACE-CLARIFICATION.md
│   └── PROJECT-SPECIFIC.md
│
└── Archive/                         # Historical documents
    └── [Obsolete documents with date prefix]
```

---

## KEY CHANGES IN V3

### All Architecture Now Under Planning
- `/Planning/Architecture/` contains ALL design documents
- Each module/app/library has its own subfolder
- Master consolidated docs at Architecture root
- System-wide docs in Architecture/System/

### Simplified Top-Level Structure
Only 5 main folders in ProjectDocs root:
1. **Planning/** - All forward-looking docs (plans, architecture, PRDs)
2. **Status/** - All backward-looking docs (reports, analysis, migration)
3. **TODO/** - Task tracking
4. **AI-Instructions/** - AI guidance
5. **Archive/** - Historical docs

---

## UPDATED AI INSTRUCTIONS

### Document Location Guide

```markdown
## WHERE TO PUT DOCUMENTS

### Planning Documents → /Planning/
- Master planning docs → /Planning/
- Sprint planning → /Planning/Sprints/
- Strategic decisions → /Planning/Strategies/

### Architecture & Design → /Planning/Architecture/
- System-wide → /Planning/Architecture/
- App-specific → /Planning/Architecture/Apps/[AppName]/
- Manager-specific → /Planning/Architecture/Managers/[ManagerName]/
- Library-specific → /Planning/Architecture/Libraries/[LibraryName]/

### Status & Reports → /Status/
- Current status → /Status/Current/
- Analysis reports → /Status/Analysis/
- Migration tracking → /Status/Migration/

### Task Tracking → /TODO/
- Master TODO → /TODO/VOS4-TODO-Master.md
- Sprint tasks → /TODO/VOS4-TODO-CurrentSprint.md
- Module TODOs → /Planning/Architecture/[Category]/[Module]/TODO.md

### NEVER CREATE:
- Duplicate folders at same level
- Version-numbered files
- Scattered architecture docs outside Planning/Architecture/
```

### Update Triggers

```markdown
## MANDATORY UPDATE CYCLES

### After Every Commit that Changes Architecture:
1. Update: /Planning/Architecture/[affected module]/[Module]-Architecture.md
2. Update: /Planning/Architecture/VOS4-Architecture-Master.md (if system-wide)
3. Add changelog entry with commit reference

### After Every Sprint (Weekly):
1. Archive: /Planning/Sprints/VOS4-Sprint-Current.md → Archive/
2. Create: New /Planning/Sprints/VOS4-Sprint-Current.md
3. Update: /Planning/VOS4-Planning-Master.md
4. Update: /TODO/VOS4-TODO-Master.md
5. Update: /Status/Current/VOS4-Status-Comprehensive.md

### After Every Phase (Bi-weekly):
1. Update: /Planning/Architecture/VOS4-Roadmap-Master.md
2. Update: /Planning/VOS4-Planning-Timeline.md
3. Update: All module TODO.md files
4. Create: Phase completion report in /Status/Migration/

### After Requirements Change:
1. Update: Affected module PRD
2. Update: /Planning/Architecture/VOS4-PRD-Master.md
3. Update: /Planning/VOS4-Planning-Decisions.md (log decision)
4. Update version and changelog

### After Performance Analysis:
1. Create/Update: /Status/Analysis/[Analysis]-Report.md
2. Update: /Planning/Architecture/VOS4-Implementation-Master.md (if impacts implementation)
3. Update: /Planning/VOS4-Planning-Risks.md (if new risks identified)
```

---

## FILE MAPPING (FROM CURRENT TO NEW)

### From ProjectDocs Root:
```
VOS4-Final-Architecture.md → Planning/Architecture/VOS4-Architecture-Master.md
VOS4-Implementation-Roadmap.md → Planning/Architecture/VOS4-Roadmap-Master.md
Smartglasses-Architecture.md → Planning/Architecture/System/
Namespace-Architecture-Review.md → Planning/Architecture/System/
VoiceUI-Merge-Analysis.md → Planning/Architecture/Apps/VoiceUI/VoiceUI-Analysis.md
CHANGELOG.md → Archive/ (use document changelogs instead)
MODULE_ANALYSIS.md → Archive/
PHASE-3-STATUS-*.md → Status/Migration/Phase-Reports/
VOS4-Migration-*.md → Status/Migration/
VOS4-Critical-Analysis-Report.md → Status/Analysis/VOS4-Analysis-Critical.md
```

### From Existing Folders:
```
Architecture/* → Planning/Architecture/
PRD/* → Planning/Architecture/[Module]/[Module]-PRD.md
Implementation/* → Planning/Architecture/System/
Analysis/AccessibilityService-* → Planning/Architecture/Apps/VoiceAccessibility/
Analysis/SpeechRecognition-* → Planning/Architecture/Apps/SpeechRecognition/
Analysis/VOS4-Analysis-* → Status/Analysis/
CurrentStatus/* → Status/Current/
Migration/* → Status/Migration/
Roadmap/* → Planning/Strategies/
```

---

## BENEFITS OF V3 STRUCTURE

1. **Ultra-Simple**: Only 5 main folders at root
2. **Clear Separation**: Planning (future) vs Status (current/past)
3. **Centralized Architecture**: All design docs under Planning/Architecture/
4. **Module Ownership**: Each module has ONE folder with ALL its docs
5. **No Confusion**: Clear where every document type belongs
6. **Easy Navigation**: Maximum 4 levels deep
7. **Living Documents**: Single source of truth with changelogs

---

## IMPLEMENTATION CHECKLIST

1. [ ] Create new folder structure
2. [ ] Move all architecture files to Planning/Architecture/
3. [ ] Consolidate PRDs into master and module-specific
4. [ ] Create master planning documents
5. [ ] Move status/analysis/migration files
6. [ ] Create TODO files in each module folder
7. [ ] Update DOCUMENT-CONTROL-MASTER.md
8. [ ] Update AI-Instructions/DOCUMENT-STANDARDS.md
9. [ ] Archive obsolete documents
10. [ ] Verify no broken references

---

## APPROVAL CHECKLIST V3

Please confirm:
- [x] Architecture under Planning folder structure
- [x] Simplified 5-folder root structure
- [x] Module folders contain all related docs
- [x] Update instructions are clear
- [ ] Ready to proceed with implementation

**Type "APPROVE V3" to proceed with this final reorganization.**