# VOS4 Documentation Reorganization Plan V2

## REVISED STRUCTURE WITH PLANNING FOLDER

```
/VOS4/ProjectDocs/
│
├── DOCUMENT-CONTROL-MASTER.md      # STAYS IN ROOT (as requested)
├── README.md                        # Project overview
│
├── Architecture/                    # Master architecture & consolidated docs
│   ├── VOS4-Architecture-Master.md # CONSOLIDATED system architecture
│   ├── VOS4-PRD-Master.md          # CONSOLIDATED master PRD (all modules)
│   ├── VOS4-Roadmap-Master.md      # CONSOLIDATED roadmap
│   ├── VOS4-Implementation-Master.md # CONSOLIDATED implementation plan
│   │
│   ├── Apps/                        # App-specific architecture
│   │   ├── VoiceAccessibility/
│   │   │   ├── AccessibilityService-Architecture.md
│   │   │   ├── AccessibilityService-PRD.md
│   │   │   ├── AccessibilityService-Implementation.md
│   │   │   ├── AccessibilityService-Analysis-Enhancement.md
│   │   │   └── TODO.md
│   │   │
│   │   ├── SpeechRecognition/
│   │   │   ├── SpeechRecognition-Architecture.md
│   │   │   ├── SpeechRecognition-PRD.md
│   │   │   ├── SpeechRecognition-Implementation.md
│   │   │   ├── SpeechRecognition-Analysis-Code.md
│   │   │   ├── SpeechRecognition-Analysis-TOT.md
│   │   │   └── TODO.md
│   │   │
│   │   └── VoiceUI/
│   │       ├── VoiceUI-Architecture.md
│   │       ├── VoiceUI-PRD.md
│   │       ├── VoiceUI-Analysis-Merge.md
│   │       └── TODO.md
│   │
│   ├── Managers/                    # Manager-specific architecture
│   │   ├── CoreMGR/
│   │   │   ├── CoreMGR-Architecture.md
│   │   │   ├── CoreMGR-PRD.md
│   │   │   └── TODO.md
│   │   │
│   │   ├── CommandsMGR/
│   │   │   ├── CommandsMGR-Architecture.md
│   │   │   ├── CommandsMGR-PRD.md
│   │   │   └── TODO.md
│   │   │
│   │   ├── DataMGR/
│   │   │   ├── DataMGR-Architecture.md
│   │   │   ├── DataMGR-PRD-Complete.md
│   │   │   ├── DataMGR-Guide-Developer.md
│   │   │   └── TODO.md
│   │   │
│   │   ├── LocalizationMGR/
│   │   │   ├── LocalizationMGR-PRD.md
│   │   │   └── TODO.md
│   │   │
│   │   └── LicenseMGR/
│   │       ├── LicenseMGR-Specification.md
│   │       └── TODO.md
│   │
│   ├── Libraries/                   # Library architecture
│   │   ├── DeviceMGR/
│   │   │   └── TODO.md
│   │   ├── VoiceUIElements/
│   │   │   └── TODO.md
│   │   └── UUIDCreator/
│   │       └── TODO.md
│   │
│   └── System/                      # System-wide implementation
│       ├── VOS4-Implementation-ProcessingEnhancement.md
│       ├── VOS4-Implementation-NativeComponents.md
│       ├── Smartglasses-Architecture.md
│       └── Namespace-Architecture-Review.md
│
├── Planning/                        # NEW: All planning & strategy documents
│   ├── VOS4-Planning-Master.md     # Master planning document
│   ├── VOS4-Planning-Phases.md     # Phase planning (1,2,3 etc)
│   ├── VOS4-Planning-Timeline.md   # Timeline and milestones
│   ├── VOS4-Planning-Resources.md  # Resource allocation
│   ├── VOS4-Planning-Risks.md      # Risk analysis and mitigation
│   ├── VOS4-Planning-Decisions.md  # Key decisions log
│   │
│   ├── Sprints/                     # Sprint planning
│   │   ├── VOS4-Sprint-Current.md
│   │   ├── VOS4-Sprint-Backlog.md
│   │   └── Archive/
│   │       └── VOS4-Sprint-2025-01.md (etc)
│   │
│   └── Strategies/                  # Strategic planning
│       ├── Optimum-Approach-Decision.md
│       ├── Option-Analysis.md
│       └── AI-Implementation-Timeline.md
│
├── Status/                          # All status, migration, analysis reports
│   ├── Current/
│   │   ├── VOS4-Status-Comprehensive-2025-01-19.md
│   │   ├── VOS4-Status-DocumentReorganization.md
│   │   └── VOS4-Status-CodeCompleteness.md
│   │
│   ├── Analysis/
│   │   ├── VOS4-Analysis-PerformanceOverhead.md
│   │   ├── VOS4-Analysis-CPUOptimization.md
│   │   ├── VOS4-Analysis-Critical-Report.md
│   │   ├── Androidjdk-Analysis-Issues.md
│   │   └── Gradle-Analysis-FullReview.md
│   │
│   ├── Migration/
│   │   ├── VOS4-Migration-Complete-Summary.md
│   │   ├── VOS4-Migration-Results-Table.md
│   │   ├── VOS4-Migration-Tracker.md
│   │   ├── VOS4-Migration-Checklist.md
│   │   ├── Legacy-Migration-Status-Report.md
│   │   └── Phase[1-3]-*.md (all phase reports)
│   │
│   └── Archive/                     # Old/completed status reports
│       └── [Dated reports moved here after 30 days]
│
├── TODO/                            # Master TODO tracking
│   ├── VOS4-TODO-Master.md         # Overall system TODOs
│   ├── VOS4-TODO-CurrentSprint.md  # Active sprint items
│   └── VOS4-TODO-Backlog.md        # Future items
│
├── AI-Instructions/                 # Keep as-is
│   ├── CODING-STANDARDS.md
│   ├── DOCUMENT-STANDARDS.md       # UPDATED with new rules
│   ├── NAMESPACE-CLARIFICATION.md
│   ├── VOS3-DESIGN-SYSTEM.md
│   └── VOS3-PROJECT-SPECIFIC.md
│
└── Archive/                         # Historical/obsolete documents
    └── [Old documents moved here with date prefix]
```

---

## NEW MASTER DOCUMENTS TO CREATE

### 1. VOS4-PRD-Master.md (Architecture/)
```markdown
/**
 * VOS4-PRD-Master.md - Consolidated Product Requirements
 * Path: /ProjectDocs/Architecture/VOS4-PRD-Master.md
 * Version: 1.0.0
 * Last Updated: 2025-01-21
 * 
 * This document consolidates all module PRDs into a single master reference
 */

# VOS4 Master Product Requirements Document

## System Overview
[Consolidated system requirements]

## Module Requirements

### Apps
- VoiceAccessibility: See AccessibilityService-PRD.md
  - [Summary of key requirements]
- SpeechRecognition: See SpeechRecognition-PRD.md
  - [Summary of key requirements]
- VoiceUI: See VoiceUI-PRD.md
  - [Summary of key requirements]

### Managers
[Similar structure for all managers]

## Cross-Module Requirements
[Requirements that span multiple modules]

## Performance Requirements
[System-wide performance targets]

## Changelog
- v1.0.0 (2025-01-21): Initial consolidation
```

### 2. VOS4-Roadmap-Master.md (Architecture/)
```markdown
/**
 * VOS4-Roadmap-Master.md - Consolidated Implementation Roadmap
 * Path: /ProjectDocs/Architecture/VOS4-Roadmap-Master.md
 * Version: 1.0.0
 * Last Updated: 2025-01-21
 */

# VOS4 Master Roadmap

## Phase 1: Foundation (Weeks 1-2)
### System-Wide
- [ ] Performance optimizations
- [ ] Grammar cache implementation

### Module Milestones
- VoiceAccessibility: Event-driven UI
- SpeechRecognition: VAD implementation
[etc...]

## Phase 2: Core Features (Weeks 3-4)
[Similar structure]

## Phase 3: Advanced Features (Weeks 5-6)
[Similar structure]

## Phase 4: Polish & Integration (Weeks 7-8)
[Similar structure]

## Changelog
- v1.0.0 (2025-01-21): Initial roadmap
```

### 3. VOS4-Planning-Master.md (Planning/)
```markdown
/**
 * VOS4-Planning-Master.md - Master Planning Document
 * Path: /ProjectDocs/Planning/VOS4-Planning-Master.md
 * Version: 1.0.0
 * Last Updated: 2025-01-21
 */

# VOS4 Master Planning Document

## Planning Overview
- Current Phase: Implementation Phase 1
- Sprint: 2025-01-21 to 2025-01-28
- Focus: Performance Optimization

## Active Planning Documents
- Timeline: VOS4-Planning-Timeline.md
- Resources: VOS4-Planning-Resources.md
- Risks: VOS4-Planning-Risks.md
- Current Sprint: Sprints/VOS4-Sprint-Current.md

## Key Decisions Log
[Recent planning decisions with dates]

## Planning Calendar
[Month view of milestones]

## Changelog
- v1.0.0 (2025-01-21): Initial planning document
```

---

## FILES TO MOVE

### To Planning/ folder:
- AI-Implementation-Timeline-*.md → Planning/Strategies/
- Optimum-Approach-Decision-*.md → Planning/Strategies/
- Option-B-Risk-Analysis-*.md → Planning/Strategies/
- Phase1-All-Options-Detailed-*.md → Planning/Strategies/
- Phase2-Implementation-Plan-*.md → Planning/Sprints/Archive/
- PHASE-3-STATUS-*.md → Planning/Sprints/Archive/

### Keep in ProjectDocs root:
- DOCUMENT-CONTROL-MASTER.md (as requested)
- README.md

### Create new consolidated documents:
- Architecture/VOS4-PRD-Master.md (consolidate all PRDs)
- Architecture/VOS4-Roadmap-Master.md (consolidate roadmaps)
- Architecture/VOS4-Implementation-Master.md (consolidate implementations)
- Planning/VOS4-Planning-Master.md (new master planning doc)

---

## UPDATED AI INSTRUCTIONS

### Document Update Rules (to add to DOCUMENT-STANDARDS.md):

```markdown
## MANDATORY: Living Document Updates

### Documents Requiring Continuous Updates:

1. **After EVERY Phase Completion:**
   - Update: Architecture/VOS4-Roadmap-Master.md
   - Update: Planning/VOS4-Planning-Master.md
   - Update: Status/Current/VOS4-Status-Comprehensive.md
   - Update: Module TODO.md files

2. **After EVERY Sprint:**
   - Archive: Current sprint to Planning/Sprints/Archive/
   - Create: New Planning/Sprints/VOS4-Sprint-Current.md
   - Update: TODO/VOS4-TODO-CurrentSprint.md

3. **After EVERY Major Change:**
   - Update: Architecture/VOS4-Architecture-Master.md
   - Update: Architecture/VOS4-PRD-Master.md (if requirements change)
   - Update: DOCUMENT-CONTROL-MASTER.md (if docs added/moved)
   - Add changelog entry to affected documents

4. **Weekly Updates:**
   - Status/Current/VOS4-Status-Comprehensive.md
   - Planning/Sprints/VOS4-Sprint-Current.md
   - TODO/VOS4-TODO-Master.md

### Changelog Format:
```
## Changelog
- v1.2.0 (2025-01-21): Added performance optimization section
- v1.1.0 (2025-01-15): Updated Phase 1 completion status
- v1.0.0 (2025-01-01): Initial document
```

### Update Checklist:
- [ ] Update version number (semantic versioning)
- [ ] Add changelog entry with date
- [ ] Update Last Modified date in header
- [ ] Update any cross-references
- [ ] Update DOCUMENT-CONTROL-MASTER.md if needed
```

---

## BENEFITS OF REVISED STRUCTURE

1. **DOCUMENT-CONTROL-MASTER stays in root** - Easy to find
2. **Master consolidated documents** - Single view of PRD, Roadmap, Implementation
3. **Planning folder** - All strategic planning in one place
4. **Living documents** - Clear update requirements
5. **Sprint tracking** - Active and archived sprints
6. **Better organization** - Planning separate from Architecture

---

## APPROVAL CHECKLIST V2

Please confirm:
- [x] DOCUMENT-CONTROL-MASTER remains in root
- [x] Master consolidated docs in Architecture/
- [x] Planning folder structure is appropriate
- [x] Update rules are clear
- [ ] Ready to proceed with implementation

**Type "APPROVE V2" to proceed with this revised reorganization.**