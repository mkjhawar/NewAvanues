# VOS4 Documentation Reorganization Plan

## PROPOSED NEW STRUCTURE

```
/VOS4/ProjectDocs/
│
├── DOCUMENT-CONTROL-MASTER.md (keep)
├── README.md (keep)
│
├── Architecture/                    # All architecture, design, PRD, implementation
│   ├── VOS4-Architecture-Master.md
│   ├── VOS4-Roadmap-Master.md
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
│   ├── DOCUMENT-STANDARDS.md
│   ├── NAMESPACE-CLARIFICATION.md
│   ├── VOS3-DESIGN-SYSTEM.md
│   └── VOS3-PROJECT-SPECIFIC.md
│
└── Archive/                         # Historical/obsolete documents
    └── [Old documents moved here with date prefix]
```

---

## FILES TO MOVE/CONSOLIDATE

### From Root to Architecture/System/:
- VOS4-Final-Architecture.md → Architecture/VOS4-Architecture-Master.md
- VOS4-Implementation-Roadmap.md → Architecture/VOS4-Roadmap-Master.md
- Smartglasses-Architecture.md → Architecture/System/
- Namespace-Architecture-Review.md → Architecture/System/

### From Root to Status/:
- VOS4-Pre-Compaction-*.md → Status/Migration/
- VOS4-Migration-*.md → Status/Migration/
- VOS4-Critical-Analysis-Report.md → Status/Analysis/
- PHASE-3-STATUS-*.md → Status/Migration/
- STATUS_CHECK.md → Status/Current/
- POST-COMPACTION-SUMMARY-*.md → Status/Migration/

### From Root to Status/Migration/:
- COMPLETE_MIGRATION_CHECKLIST.md → Status/Migration/VOS4-Migration-Checklist.md
- VOS4_MIGRATION_TRACKER.md → Status/Migration/VOS4-Migration-Tracker.md

### From Root to Architecture/Apps/VoiceUI/:
- VoiceUI-Merge-Analysis.md → Architecture/Apps/VoiceUI/

### From PRD folder → Architecture/[Module]/:
- PRD-ACCESSIBILITY.md → Architecture/Apps/VoiceAccessibility/AccessibilityService-PRD.md
- PRD-COMMANDS.md → Architecture/Managers/CommandsMGR/CommandsMGR-PRD.md
- PRD-DATA-COMPLETE.md → Architecture/Managers/DataMGR/DataMGR-PRD-Complete.md
- [etc for all PRDs]

### From Implementation folder → Architecture/System/:
- VOS4-Implementation-ProcessingEnhancement.md → Architecture/System/
- VOS4-Implementation-NativeComponents.md → Architecture/System/

### From Analysis folder:
- AccessibilityService-Enhancement-Plan.md → Architecture/Apps/VoiceAccessibility/
- SpeechRecognition-*.md → Architecture/Apps/SpeechRecognition/
- VOS4-Analysis-*.md → Status/Analysis/
- Phase*.md → Status/Migration/

### Files to Archive:
- DOCUMENTATION-STRUCTURE.md (obsolete, replaced by DOCUMENT-CONTROL-MASTER)
- MASTER-DOCUMENTATION-INDEX.md (obsolete, replaced by DOCUMENT-CONTROL-MASTER)
- MODULE_ANALYSIS.md (old, content moved to module folders)
- LICENSE.md (move to root VOS4 folder if needed)

---

## NEW TODO SYSTEM

### Master TODO (/TODO/VOS4-TODO-Master.md):
```markdown
# VOS4 Master TODO List

## Active Sprint (2025-01-21 to 2025-01-28)
See: VOS4-TODO-CurrentSprint.md

## Module TODOs
- [ ] VoiceAccessibility: See Architecture/Apps/VoiceAccessibility/TODO.md
- [ ] SpeechRecognition: See Architecture/Apps/SpeechRecognition/TODO.md
[etc...]

## System-Wide TODOs
- [ ] Performance optimization (Phase 1) - Due: 2025-02-01
- [ ] Native components integration - Due: 2025-02-15
[etc...]
```

### Module TODO (e.g., Architecture/Apps/VoiceAccessibility/TODO.md):
```markdown
# VoiceAccessibility TODO List

## Current Sprint
- [ ] Implement event-driven UI updates
- [ ] Add gesture learning system

## Backlog
- [ ] Smart context awareness
- [ ] Macro recording system

## Completed
- [x] 2025-01-21: Basic accessibility service
```

---

## BENEFITS OF THIS STRUCTURE

1. **Single Source of Truth**: Each module has ONE folder with ALL its documents
2. **Clear Hierarchy**: Apps → Managers → Libraries → System
3. **No Duplication**: One location per document type
4. **Easy Navigation**: Find any module's docs in one place
5. **Living Documents**: Each document updated in place with changelog
6. **TODO Tracking**: Centralized and module-specific TODO lists
7. **Status Clarity**: All status/analysis/migration in one place
8. **Archive System**: Old documents archived, not deleted

---

## IMPLEMENTATION STEPS

1. **Create new folder structure** (mkdir commands)
2. **Move and rename files** according to mapping above
3. **Create TODO.md** in each module folder
4. **Update DOCUMENT-CONTROL-MASTER.md** with new locations
5. **Create README.md** for each code module
6. **Archive obsolete documents**
7. **Update all cross-references**
8. **Verify no broken links**

---

## APPROVAL CHECKLIST

Please confirm:
- [ ] Folder structure meets your needs
- [ ] File consolidation plan is correct
- [ ] TODO system is appropriate
- [ ] Archive strategy is acceptable
- [ ] Ready to proceed with implementation

**Type "APPROVE" to proceed with reorganization or provide feedback for adjustments.**