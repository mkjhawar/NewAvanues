# Avanues Ecosystem Documentation

**Project**: Avanues Ecosystem (VoiceOS + Avanue Platform)
**Version**: 8.4.0
**Methodology**: IDEACODE 8.4
**Last Updated**: 2025-11-19
**Status**: Living Document

---

## Quick Navigation

**New to the project?** Start here:

1. [CLAUDE.md](../CLAUDE.md) - Project quick reference (READ FIRST)
2. [GlobalDesignStandards](../GlobalDesignStandards/README.md) - Architecture & design standards
3. [IPC Foundation Integration Guide](./IPC-Foundation-Integration-Guide.md) - Cross-module communication
4. [Architecture Overview](#architecture--design-standards) - System design documentation

---

## Top 10 Most Important Documents

Quick links to essential documentation:

1. **[GlobalDesignStandards](../GlobalDesignStandards/README.md)** - Unified design standards for entire ecosystem
2. **[IPC Foundation Integration Guide](./IPC-Foundation-Integration-Guide.md)** - Service discovery and cross-process communication
3. **[IPC Foundation Demo](../apps/ipc-foundation-demo/README.md)** - Complete working example
4. **[AvaElements Unified Architecture](./architecture/AvaElements-Unified-Architecture-251109-1431.md)** - Core UI framework architecture
5. **[Avanues Master Capability Registry](./architecture/Avanues-Master-Capability-Registry-251110-0525.md)** - Service registration system
6. **[Avanues Dual Plugin Architecture](./architecture/Avanues-Dual-Plugin-Architecture-251110-0515.md)** - Internal/external plugin system
7. **[Database IPC Developer Manual](./Database-IPC-Developer-Manual.md)** - Database access via IPC
8. **[AvaCode Documentation](./avacode/README.md)** - DSL code generation system
9. **[IDEAMagic UI Developer Manual](./IDEAMAGIC-UI-DEVELOPER-MANUAL-251105.md)** - UI component development guide
10. **[Asset Manager](../modules/MagicIdea/Components/AssetManager/AssetManager/README.md)** - Icon and image management

---

## Documentation Structure

```
docs/
├── README.md (this file)           ⬅️ START HERE
│
├── 🎯 Global Design Standards
│   ├── GlobalDesignStandards/
│   │   ├── README.md                   - Standards overview
│   │   ├── GlobalDesignStandard-Module-Structure.md
│   │   ├── GlobalDesignStandard-IPC-Architecture.md
│   │   ├── GlobalDesignStandard-IPC-Integration-Guide.md
│   │   ├── GlobalDesignStandard-UI-Patterns.md
│   │   └── IPC-QUICK-REFERENCE.md
│   │
│   └── IPC-Foundation-Integration-Guide.md  - Complete IPC integration guide
│
├── 🏗️ Architecture & Design
│   ├── architecture/
│   │   ├── AvaElements-Unified-Architecture-251109-1431.md
│   │   ├── Avanues-Master-Capability-Registry-251110-0525.md
│   │   ├── Avanues-Dual-Plugin-Architecture-251110-0515.md
│   │   ├── Avanues-ARG-File-Registry-System-251110-0540.md
│   │   ├── MagicIdea-WebRenderer-Architecture-2511091400.md
│   │   ├── VOS-FILE-FORMAT.md
│   │   └── shared/WEBRENDERER-ARCHITECTURE.md
│   │
│   └── AUDIT-MagicIdea-AvaElements-Codebase-251109-1346.md
│
├── 📦 Component Documentation
│   ├── avacode/                      - AvaCode DSL Generator (12 files)
│   ├── Database-IPC-Developer-Manual.md
│   ├── IDEAMAGIC-UI-DEVELOPER-Manual-251105.md
│   └── Component-specific docs
│
├── 📱 Demo Applications
│   ├── apps/ipc-foundation-demo/
│   │   ├── README.md
│   │   ├── DEMO-COMPLETE-SUMMARY.md
│   │   └── docs/DEMO-QUICK-START.md
│   │
│   └── Future demos (TBD)
│
├── 📋 Project Planning
│   ├── IDEACODE5-MASTER-PLAN-251030-0302.md
│   ├── IDEACODE5-PROJECT-SPEC-251030-0304.md
│   ├── IDEACODE5-TASKS-251030-0304.md
│   ├── IDEACODE5-ARCHITECTURE-DECISIONS.md
│   └── Master-TODO-IDEAMagic.md
│
├── 📚 Developer Manuals
│   ├── Database-IPC-Developer-Manual.md
│   ├── IDEAMAGIC-UI-DEVELOPER-MANUAL-251105.md
│   ├── DEVELOPER-MANUAL-MAGICCODE-INJECTION-251031-2116.md
│   └── AI-Module-Porting-Guide.md
│
├── 📊 Status Reports & Sessions
│   ├── SESSION-CHECKPOINT-251109-1846.md     - Latest checkpoint
│   ├── SESSION-CHECKPOINT-251109-1410.md
│   ├── SESSION-FINAL-REPORT-251103-0600.md
│   └── Active/ (50+ session documents)
│
├── 📖 Books & Guides
│   └── book/                           - Comprehensive guides (16 chapters)
│       ├── Chapter-01-Introduction-Philosophy.md
│       ├── Chapter-02-Architecture-Overview.md
│       ├── Chapter-04-AvaUI-Runtime.md
│       ├── Chapter-05-CodeGen-Pipeline.md
│       └── [12 more chapters]
│
└── 🗂️ Context & Archive
    ├── context/                        - Session context files
    ├── archive/                        - Archived documents
    └── Future-Ideas/                   - Future enhancements

```

---

## By Category

### Architecture & Design Standards

**Global Design Standards** (Mandatory for all modules):
- [GlobalDesignStandards README](../GlobalDesignStandards/README.md) - Standards overview
- [Module Structure Standard](../GlobalDesignStandards/GlobalDesignStandard-Module-Structure.md) - KMP module organization
- [IPC Architecture Standard](../GlobalDesignStandards/GlobalDesignStandard-IPC-Architecture.md) - Communication patterns
- [IPC Integration Guide](../GlobalDesignStandards/GlobalDesignStandard-IPC-Integration-Guide.md) - Implementation guide
- [UI Patterns Standard](../GlobalDesignStandards/GlobalDesignStandard-UI-Patterns.md) - Compose best practices
- [IPC Quick Reference](../GlobalDesignStandards/IPC-QUICK-REFERENCE.md) - Quick lookup guide

**System Architecture**:
- [AvaElements Unified Architecture](./architecture/AvaElements-Unified-Architecture-251109-1431.md) - Core UI framework
- [Avanues Master Capability Registry](./architecture/Avanues-Master-Capability-Registry-251110-0525.md) - Service registry
- [Avanues Dual Plugin Architecture](./architecture/Avanues-Dual-Plugin-Architecture-251110-0515.md) - Plugin system
- [ARG File Registry System](./architecture/Avanues-ARG-File-Registry-System-251110-0540.md) - File-based registry
- [MagicIdea WebRenderer Architecture](./architecture/MagicIdea-WebRenderer-Architecture-2511091400.md) - Web renderer
- [VOS File Format](./architecture/VOS-FILE-FORMAT.md) - Voice OS file format spec
- [WebRenderer Architecture](./architecture/shared/WEBRENDERER-ARCHITECTURE.md) - Shared web renderer

**Code Audits & Analysis**:
- [MagicIdea/AvaElements Codebase Audit](./AUDIT-MagicIdea-AvaElements-Codebase-251109-1346.md) - Complete audit
- [AvaElements Audit Summary](./AUDIT-SUMMARY-AvaElements-251109.md) - Executive summary
- [Component Gap Analysis](./COMPONENT-GAP-ANALYSIS-251104.md) - Missing components
- [Component Consolidation Plan](./COMPONENT-CONSOLIDATION-PLAN-251104.md) - Merge strategy

---

### Integration Guides

**IPC Foundation** (Service discovery & cross-process communication):
- [IPC Foundation Integration Guide](./IPC-Foundation-Integration-Guide.md) - Complete integration guide
- [IPC Foundation Demo App](../apps/ipc-foundation-demo/README.md) - Working example
- [IPC Foundation Demo Quick Start](../apps/ipc-foundation-demo/docs/DEMO-QUICK-START.md) - Get started
- [IPC Foundation Demo Summary](../apps/ipc-foundation-demo/DEMO-COMPLETE-SUMMARY.md) - Features overview
- [IPC Security Architecture](./IPC-SECURITY-ARCHITECTURE-SUMMARY-251101-0048.md) - Security considerations
- [IPC Module Plugin Data Exchange](./IPC-Module-Plugin-Data-Exchange-Flow.md) - Data flow patterns

**Database IPC**:
- [Database IPC Developer Manual](./Database-IPC-Developer-Manual.md) - Complete database access guide
- [Database IPC Implementation](./DATABASE-IPC-IMPLEMENTATION-251104.md) - Implementation details
- [Database Integration Complete](./Status-Database-Integration-Complete-251104-0600.md) - Status report

**Module Porting**:
- [AI Module Porting Guide](./AI-Module-Porting-Guide.md) - Automated porting
- [BrowserAvanue Template](./browseravanue/TEMPLATE-Module-Migration-Instructions-251103-1150.md) - Migration template

---

### Project Plans & Specs

**IDEACODE 5 Core Documents**:
- [IDEACODE5 Master Plan](./IDEACODE5-MASTER-PLAN-251030-0302.md) - 14-week development roadmap
- [IDEACODE5 Project Spec](./IDEACODE5-PROJECT-SPEC-251030-0304.md) - Technical specifications
- [IDEACODE5 Tasks](./IDEACODE5-TASKS-251030-0304.md) - 87 tasks with estimates
- [IDEACODE5 Architecture Decisions](./IDEACODE5-ARCHITECTURE-DECISIONS.md) - 19 ADRs

**Master TODOs**:
- [Master TODO IDEAMagic](./Master-TODO-IDEAMagic.md) - Project-wide task list

**Implementation Plans**:
- [Android First Implementation Plan](./ANDROID-FIRST-IMPLEMENTATION-PLAN-251030-0408.md)
- [Android Remaining Work](./ANDROID-REMAINING-WORK-251030-1350.md)
- [iOS Implementation Plan](./IOS-IMPLEMENTATION-PLAN-251030-1333.md)
- [AvaUI Implementation Plan](./MAGICUI-IMPLEMENTATION-PLAN-251101-0420.md)
- [Week 1 MagicIdea Execution Plan](./WEEK1-MAGICIDEA-EXECUTION-PLAN-251109-1300.md)

---

### Component Documentation

**AvaCode DSL Generator** (Multi-platform code generation):
- [AvaCode README](./avacode/README.md) - Entry point
- [AvaCode Document Map](./avacode/DOCUMENT_MAP.md) - Navigation guide
- [CodeGen Design Summary](./avacode/CODEGEN_DESIGN_SUMMARY.md) - Architecture
- [Target Framework Mappings](./avacode/TARGET_FRAMEWORK_MAPPINGS.md) - Platform mappings
- [Quick Reference](./avacode/QUICK_REFERENCE.md) - Cheat sheet
- [Code Generation Utilities](./avacode/CODE_GENERATION_UTILITIES.md) - Utility functions
- [TextField/Checkbox Guide](./avacode/TEXTFIELD_CHECKBOX_GUIDE.md) - Component guide
- [Test Output Examples](./avacode/test_output_examples.md) - Test examples
- [Completion Report](./avacode/COMPLETION_REPORT.md) - Status
- [Update Summary](./avacode/UPDATE_SUMMARY.md) - Recent changes
- [Context Handoff](./avacode/CONTEXT-HANDOFF-20251027.md) - Session context
- [MagicSession Context](./avacode/MAGICSESSION-CONTEXT-251030.md) - Magic session

**MagicIdea Components** (48 UI components):
- [IDEAMagic UI Developer Manual](./IDEAMAGIC-UI-DEVELOPER-MANUAL-251105.md) - Complete component guide
- [Component Base Types Complete](./COMPONENT-BASE-TYPES-COMPLETE-251105-0102.md) - Base type system
- [Component Consolidation Complete](./COMPONENT-CONSOLIDATION-COMPLETE-251104.md) - Merge status
- [Component Merge Analysis](./COMPONENT-MERGE-ANALYSIS-251102-0015.md) - Merge strategy

**AssetManager** (Icons & images):
- [AssetManager README](../modules/MagicIdea/Components/AssetManager/AssetManager/README.md) - Complete guide
- [AssetManager Integration](../modules/MagicIdea/Components/AssetManager/AssetManager/INTEGRATION.md) - Integration
- [AssetManager Examples](../modules/MagicIdea/Components/AssetManager/AssetManager/EXAMPLES.md) - Usage examples
- [AssetManager Quick Start](../modules/MagicIdea/Components/AssetManager/AssetManager/QUICK_START.md) - Get started

**ThemeBuilder** (Theme management):
- [ThemeBuilder README](../Universal/Libraries/AvaElements/ThemeBuilder/README.md) - Theme system
- [Avanue Theme Format (ATH)](./AVANUE-THEME-FORMAT-ATH-251031-1700.md) - Theme file format
- [GlassAvanue Theme Spec](./GLASSAVANUE-THEME-SPEC-251031-1633.md) - Glass theme

---

### Demo Apps

**IPC Foundation Demo** (Complete working example):
- [IPC Foundation Demo README](../apps/ipc-foundation-demo/README.md) - Overview
- [Demo Quick Start](../apps/ipc-foundation-demo/docs/DEMO-QUICK-START.md) - Get started quickly
- [Demo Complete Summary](../apps/ipc-foundation-demo/DEMO-COMPLETE-SUMMARY.md) - Feature list
- Features:
  - ARGScanner service discovery
  - VoiceCommandRouter command parsing
  - IPCConnector cross-process calls
  - HTML+JavaScript demo
  - Complete test coverage

---

### Developer Manuals

**Core Manuals**:
- [Database IPC Developer Manual](./Database-IPC-Developer-Manual.md) - Database access patterns
- [IDEAMagic UI Developer Manual](./IDEAMAGIC-UI-DEVELOPER-MANUAL-251105.md) - UI component development
- [AvaCode Injection Manual](./DEVELOPER-MANUAL-MAGICCODE-INJECTION-251031-2116.md) - Code generation
- [AI Module Porting Guide](./AI-Module-Porting-Guide.md) - Automated module porting

**Platform-Specific**:
- [Android 100% Complete](./ANDROID-100-COMPLETE-251030-0445.md) - Android platform status
- [iOS Renderer Complete](./iOS-RENDERER-COMPLETE-251102-0745.md) - iOS platform status
- [iOS Renderer Developer Guide](./guides/ios-renderer-developer-guide.md) - iOS developer documentation
- [iOS Renderer User Guide](./guides/ios-renderer-user-guide.md) - iOS user documentation
- [iOS Renderer Phase 1 Changelog](./changelogs/2025-11-19-ios-renderer-phase1.md) - Latest iOS work

---

### Books & Comprehensive Guides

**IDEAMagic Book** (16 chapters):
- [Book README](./book/README-BOOK.md) - Book overview
- [Chapter 01: Introduction & Philosophy](./book/Chapter-01-Introduction-Philosophy.md)
- [Chapter 02: Architecture Overview](./book/Chapter-02-Architecture-Overview.md)
- [Chapter 03: Design Decisions](./book/Chapter-03-Design-Decisions.md)
- [Chapter 04: AvaUI Runtime](./book/Chapter-04-AvaUI-Runtime.md)
- [Chapter 05: CodeGen Pipeline](./book/Chapter-05-CodeGen-Pipeline.md)
- [Chapter 06: Component Library](./book/Chapter-06-Component-Library.md)
- [Chapter 07: Android Compose](./book/Chapter-07-Android-Compose.md)
- [Chapter 08: iOS SwiftUI](./book/Chapter-08-iOS-SwiftUI.md)
- [Chapter 09: Web React](./book/Chapter-09-Web-React.md)
- [Chapter 10: VoiceAvanue Integration](./book/Chapter-10-VoiceAvanue-Integration.md)
- [Chapter 11: VoiceOS Bridge](./book/Chapter-11-VoiceOSBridge.md)
- [Chapter 12: Cross-Platform Communication](./book/Chapter-12-Cross-Platform-Communication.md)
- [Chapter 13: Web Interface](./book/Chapter-13-Web-Interface.md)
- [Chapter 14: P2P WebRTC](./book/Chapter-14-P2P-WebRTC.md)
- [Chapter 15: Plugin System](./book/Chapter-15-Plugin-System.md)
- [Chapter 16: Expansion & Future](./book/Chapter-16-Expansion-Future.md)

**Appendices**:
- [Appendix A: API Reference](./book/Appendix-A-API-Reference.md)
- [Appendix B: Code Examples](./book/Appendix-B-Code-Examples.md)
- [Appendix C: Troubleshooting](./book/Appendix-C-Troubleshooting.md)
- [Appendix D: Migration Guides](./book/Appendix-D-Migration-Guides.md)

**Specialized Guides**:
- [DSL Formats Clarification](./book/DSL-Formats-Clarification.md)
- [Unified DSL Format](./book/UNIFIED-DSL-FORMAT.md)
- [Framework Comparison](./book/Framework-Comparison-IDEAMagic-vs-Competitors.md)

---

### Status Reports & Sessions

**Latest Sessions**:
- [Session Checkpoint 251109-1846](./SESSION-CHECKPOINT-251109-1846.md) - Latest checkpoint
- [Session Checkpoint 251109-1410](./SESSION-CHECKPOINT-251109-1410.md)
- [Session Final Report 251103-0600](./SESSION-FINAL-REPORT-251103-0600.md)
- [Completion Report 251103-2110](./COMPLETION-REPORT-251103-2110.md)

**Phase Completion Reports**:
- [Phase 3 Complete](./Status-Phase3-Complete-251103-2122.md)
- [Phase 2 Complete Report](./PHASE-2-COMPLETE-REPORT-251102-0251.md)
- [Architecture Restructure Complete](./ARCHITECTURE-RESTRUCTURE-COMPLETE-251104.md)
- [Component Consolidation Complete](./COMPONENT-CONSOLIDATION-COMPLETE-251104.md)

**Build Status**:
- [Build Status 251103-0545](./BUILD-STATUS-251103-0545.md)
- [Build Configuration Fixes](./Status-Build-Configuration-Fixes-20251108-0756.md)
- [Consolidation Build Issues](./CONSOLIDATION-BUILD-ISSUES-251104.md)

**Active Development** (50+ session documents in `/docs/Active/`):
- Migration sessions, YOLO sessions, implementation progress
- Agent reports, analysis documents, strategy documents

---

### Context & Archive

**Context Files**:
- [Context Save 251102-1545](./context/context-save-251102-1545.md)
- [BrowserAvanue Migration Context](./context/CONTEXT-browseravanue-migration-251103-1153.md)
- [Checkpoint 251103-2122](./context/checkpoint-251103-2122.md)

**Archive**:
- [Context Handoff 20251027](./archive/CONTEXT-HANDOFF-20251027.md)

**Future Ideas**:
- [AVA Example: Ford Explorer Diagnostic](./Future-Ideas/AVA-Example-Ford-Explorer-Diagnostic-Workflow.md)
- [AVA AvaCode Integration Plan](./Future-Ideas/AVA-AvaCode-Integration-Plan.md)
- [AVA Multimodal Capabilities](./Future-Ideas/AVA-Multimodal-Capabilities.md)
- [IDEAMagic Future Features Roadmap](./Future-Ideas/IDEAMagic-Future-Features-Roadmap.md)

---

## Search by Keywords

### IPC & Communication
- IPC Foundation Integration Guide
- GlobalDesignStandard-IPC-Architecture
- GlobalDesignStandard-IPC-Integration-Guide
- IPC-QUICK-REFERENCE
- Database-IPC-Developer-Manual
- IPC-Security-Architecture
- IPC-Module-Plugin-Data-Exchange-Flow

### Architecture
- AvaElements-Unified-Architecture
- Avanues-Master-Capability-Registry
- Avanues-Dual-Plugin-Architecture
- ARG-File-Registry-System
- MagicIdea-WebRenderer-Architecture
- VOS-FILE-FORMAT

### UI Components
- IDEAMAGIC-UI-DEVELOPER-MANUAL
- Component-Base-Types-Complete
- GlobalDesignStandard-UI-Patterns
- AvaElements Core, Renderers

### Code Generation
- avacode/ (12 files)
- CodeGen-Design-Summary
- Target-Framework-Mappings
- AvaCode-Injection-Manual

### Assets & Themes
- AssetManager (README, Integration, Examples)
- ThemeBuilder README
- Avanue-Theme-Format-ATH
- GlassAvanue-Theme-Spec

### Testing & Quality
- Component tests, Integration tests
- Demo apps with test coverage
- Quality gates in GlobalDesignStandards

### Database
- Database-IPC-Developer-Manual
- Database-IPC-Implementation
- Database-Integration-Complete

### Platform-Specific
- Android: Android-100-Complete
- iOS: iOS-Renderer-Complete
- Web: WebRenderer-Architecture

---

## Document Conventions

### File Naming
- **Living documents** (no timestamp): `Project-Name.md`, `Master-Name.md`
- **Static documents** (with timestamp): `Name-YYMMDD-HHMM.md`
- **Session docs**: `SESSION-*-YYMMDD-HHMM.md`
- **Status docs**: `Status-*-YYMMDD-HHMM.md`

### Status Indicators
- ✅ Complete
- 🔄 In Progress
- ⏳ Planned / Not Started
- ❌ Blocked / Failed
- 🚧 Under Construction

### Priority Levels
- **P0**: Critical (blocking)
- **P1**: High (important)
- **P2**: Medium (nice-to-have)
- **P3**: Low (future)

---

## Current Project Status

### Overall Progress: Phase 3 Complete ✅

**Components**: 48/48 (100%)
- ✅ Layout (13): Column, Row, Container, ScrollView, Card, Grid, Stack, Spacer, Drawer, Tabs, AppBar, BottomNav, Divider
- ✅ Display (15): Text, Icon, Image, Badge, Chip, Avatar, Skeleton, Spinner, ProgressBar, Tooltip, StatCard, DataTable, Timeline, TreeView, DataGrid
- ✅ Input (12): Button, TextField, Checkbox, Switch, Slider, RangeSlider, DatePicker, TimePicker, RadioButton, RadioGroup, Dropdown, Autocomplete
- ✅ Advanced (8): ColorPicker, FileUpload, ImagePicker, Rating, SearchBar, MultiSelect, TagInput, ToggleButtonGroup

**Platforms**:
- ✅ Android (Jetpack Compose) - 100% (92 mappers)
- ✅ iOS (SwiftUI Bridge) - 100% (81+ mappers, OptimizedSwiftUIRenderer)
- 🔄 Web (React) - 80% (90 components)
- 🔄 Desktop (Compose Desktop) - 60%

**Core Systems**:
- ✅ AvaElements Core - 100%
- ✅ Android Renderer - 100%
- ✅ iOS Bridge - 100%
- ✅ State Management - 100%
- ✅ Theme Manager - 100%
- 🔄 AssetManager - 30% (Material Icons, Font Awesome, Search needed)
- 🔄 ThemeBuilder UI - 20%

**IPC Foundation**:
- ✅ ARGScanner (service discovery) - 100%
- ✅ VoiceCommandRouter (command parsing) - 100%
- ✅ IPCConnector (cross-process) - 100%
- ✅ Database IPC - 100%
- ✅ Demo App - 100%

### Next Priorities

**Week 1-2** (Current):
1. Complete AssetManager (Material Icons, Font Awesome, Search)
2. Complete ThemeBuilder UI (visual theme editor)
3. Write comprehensive tests (target 80% coverage)

**Week 3-4**:
1. AvaCode enhancements (improved code generation)
2. Performance optimization
3. Documentation updates

---

## Getting Help

### For Development Questions
1. Check [GlobalDesignStandards](../GlobalDesignStandards/README.md) first
2. Review [IPC Foundation Integration Guide](./IPC-Foundation-Integration-Guide.md)
3. Check relevant component documentation
4. Review demo apps for working examples

### For Architecture Questions
1. Review [AvaElements Unified Architecture](./architecture/AvaElements-Unified-Architecture-251109-1431.md)
2. Check [GlobalDesignStandard-Module-Structure](../GlobalDesignStandards/GlobalDesignStandard-Module-Structure.md)
3. Review [IDEACODE5-ARCHITECTURE-DECISIONS](./IDEACODE5-ARCHITECTURE-DECISIONS.md)

### For IPC Questions
1. Read [IPC Foundation Integration Guide](./IPC-Foundation-Integration-Guide.md)
2. Check [IPC Quick Reference](../GlobalDesignStandards/IPC-QUICK-REFERENCE.md)
3. Review [IPC Foundation Demo](../apps/ipc-foundation-demo/README.md)
4. Consult [Database IPC Developer Manual](./Database-IPC-Developer-Manual.md)

### For UI Component Questions
1. Read [IDEAMagic UI Developer Manual](./IDEAMAGIC-UI-DEVELOPER-MANUAL-251105.md)
2. Check [GlobalDesignStandard-UI-Patterns](../GlobalDesignStandards/GlobalDesignStandard-UI-Patterns.md)
3. Review component examples in demo apps

---

## Contributing

### Documentation Updates

**When to update**:
- After completing any phase or sprint
- When adding new features or components
- When architecture changes
- When fixing bugs or issues

**What to update**:
- Status indicators in this README
- Component documentation
- Architecture decision records
- Integration guides
- Demo applications

### Code Quality Gates

Before merging code, verify:
- [ ] Follows GlobalDesignStandards
- [ ] Module structure correct
- [ ] IPC integration correct (if applicable)
- [ ] UI accessibility verified
- [ ] Tests written and passing (80%+ coverage)
- [ ] Documentation updated
- [ ] Demo app updated (if applicable)

---

## External Resources

### Framework Documentation
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [SwiftUI](https://developer.apple.com/documentation/swiftui)
- [React](https://react.dev/)

### Tools & Libraries
- [Material Design 3](https://m3.material.io/)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Kotlin Serialization](https://kotlinlang.org/docs/serialization.html)

---

## Document Status

**Status**: ✅ COMPLETE
**Maintained By**: Agent 1 (Documentation & Assets)
**Last Updated**: 2025-11-19
**Next Review**: 2025-11-26 (weekly)
**Version**: 8.4.0

---

**Created by Manoj Jhawar, manoj@ideahq.net**
