# VoiceOSCore Documentation

## Overview
Documentation for the VoiceOSCore module - the core application module of VoiceOS that integrates accessibility services, voice recognition, AI context inference, and system-wide voice control capabilities.

## Recent Updates

### Phase 2 & 2.5 AI Context Inference (October 2025)
**Completed:** 2025-10-18

Major enhancements to accessibility scraping with advanced AI context capabilities:

**Phase 2 Features:**
- Screen-level context classification (login, checkout, settings, etc.)
- Form purpose detection (registration, payment, address, contact)
- Primary action inference (submit, search, browse, purchase)
- Navigation depth tracking
- Input validation pattern recognition (email, phone, URL, credit card, etc.)
- Screen visit frequency tracking

**Phase 2.5 Features:**
- Form field grouping and relationship modeling
- Button-to-form relationship inference
- Label-to-input field mapping
- Screen transition tracking and user flow analysis
- Enhanced validation pattern detection (80-90% accuracy)

**Database:** Upgraded to v7 with comprehensive migration history

## Module Status

**Current State:** Active Development
- **Database Version:** 7
- **Build Status:** Kotlin compilation successful
- **Phase:** Phase 2.5 complete, ready for Phase 3 (User Interaction Tracking)
- **Last Updated:** 2025-10-18

**Key Capabilities:**
- Accessibility service scraping with hash-based deduplication
- Screen context understanding and classification
- Form relationship modeling and inference
- Navigation flow tracking
- AI-powered element analysis
- UUID-based persistent identification
- LearnApp mode support
- Multi-app accessibility tracking

## Key Features

### Accessibility Scraping
- Real-time UI element capture via AccessibilityService
- Hash-based deduplication (MD5)
- Hierarchy preservation with depth tracking
- Window event handling
- Multi-app support

### AI Context Inference
**Element-Level Analysis:**
- Semantic role inference (login button, email input, etc.)
- Input type detection (email, password, phone, URL)
- Visual weight classification (primary, secondary, danger)
- Required field detection
- Validation pattern recognition
- Placeholder text extraction

**Screen-Level Understanding:**
- Screen type classification (10+ types: login, checkout, settings, home, search, profile, cart, detail, list, form)
- Form context detection (registration, payment, address, contact, feedback)
- Primary action inference
- Navigation level tracking (0 = main screen, 1+ = nested)
- Back button detection
- Visit frequency analytics

**Relationship Modeling:**
- Form field grouping with stable group IDs
- Button-to-form submission relationships
- Label-to-input field mapping
- Element adjacency and hierarchy-based inference
- Confidence scoring (0.7-0.9)

### Screen Context Tracking
- Screen hash identification (MD5-based)
- Visit count tracking
- First/last scraped timestamps
- Element count per screen
- Activity name and window title capture

### Element Relationships
- Form group membership
- Submit button associations
- Label-input linkage
- Relationship type categorization
- Confidence-based filtering

### Navigation Flow Tracking
- Screen transition recording
- Transition frequency analysis
- Average transition time calculation
- User journey mapping
- Navigation pattern detection

## Database Schema

**Current Version:** v7

**Migration History:**
- v1 → v2: Element hash deduplication
- v2 → v3: LearnApp mode support
- v3 → v4: UUID integration
- v4 → v5: Phase 1 AI context (semantic role, input type, visual weight, required)
- v5 → v6: Phase 2 AI context (screen context, relationships, validation)
- v6 → v7: Phase 2.5 enhancements (screen transitions)

**Entities (8):**
1. `ScrapedAppEntity` - Application metadata
2. `ScrapedElementEntity` - UI elements with AI inference
3. `ElementHierarchyEntity` - Parent-child relationships
4. `VoiceCommandEntity` - Voice command associations
5. `ScreenContextEntity` - Screen-level context (Phase 2)
6. `ElementRelationshipEntity` - Element relationships (Phase 2.5)
7. `ScreenTransitionEntity` - Navigation flows (Phase 2.5)
8. UUID integration entities

**DAOs (7):**
- ScrapedAppDao, ScrapedElementDao, ElementHierarchyDao
- VoiceCommandDao, ScreenContextDao, ElementRelationshipDao
- ScreenTransitionDao

## Documentation Structure
- `architecture/` - Module design and architecture
- `changelog/` - Version history and changes
- `developer-manual/` - Development guides
- `diagrams/` - Visual documentation
- `implementation/` - Implementation details
- `module-standards/` - Module-specific standards
- `project-management/` - Planning and management docs
- `reference/` - Technical reference
  - `api/` - API documentation (Overlay API, Scraping API)
- `roadmap/` - Future plans
- `status/` - Status reports
- `testing/` - Test documentation
- `user-manual/` - User guides

## Quick Links
- [Changelog](./changelog/CHANGELOG.md)
- [API Reference](./reference/api/)
  - [Overlay API Reference](./reference/api/Overlay-API-Reference-251009-0403.md)
- [Developer Guide](./developer-manual/)
- [Architecture Documentation](./architecture/)
  - [Integration Architecture](./architecture/Integration-Architecture-251010-1126.md)
  - [UUID-Hash Persistence](./architecture/UUID-Hash-Persistence-Architecture-251010-0157.md)
- [Recent Updates](../../Active/)
  - [Phase 2 Implementation](../../Active/AI-Context-Phase2-Implementation-Complete-251018-2208.md)
  - [Phase 2.5 Enhancements](../../Active/AI-Context-Phase25-Enhancements-Complete-251018-2225.md)

## Performance Characteristics

**Scraping Performance:**
- Element inference overhead: ~2-3ms per element
- Screen context lookup: O(1) hash-based
- Relationship inference: ~5-10ms for typical forms
- Total overhead: < 10% increase in scraping time

**Memory Impact:**
- Minimal - keyword-based inference (no ML models)
- Screen contexts cached by hash
- Database size: ~1-2KB per unique screen

### Garbage Text Filtering & Icon Commands (February 2026)
**Completed:** 2026-02-02

New voice command quality improvements:

**Garbage Text Filtering:**
- Filters repetitive patterns like "comma comma com"
- Removes CSS class names, Base64 strings, UUIDs
- Filters programming artifacts ([object Object], null, undefined)
- Multi-language support (en, de, es, fr, zh, ja)

**Icon Command Support:**
- Single-word commands for navigation icons
- Recognizes icons by size, class name, and contentDescription
- Localized navigation icon labels (Menu, More, Search, etc.)
- Generates numbered commands for unlabeled icons

**Documentation:** [Garbage Filtering & Icon Commands](./developer-manual/VoiceOS-Garbage-Filtering-Icon-Commands-50202-V1.md)

## What's Next

**Phase 3: User Interaction Tracking (Planned)**
- Click count tracking
- Visibility duration
- State transition tracking
- Personalization features
- Estimated effort: 21-30 hours

## Contributing
See the [Developer Manual](./developer-manual/) for contribution guidelines.

---

**Last Updated:** 2025-10-18 22:55 PDT
**Database Version:** 7
**Module Status:** Active Development - Phase 2.5 Complete
