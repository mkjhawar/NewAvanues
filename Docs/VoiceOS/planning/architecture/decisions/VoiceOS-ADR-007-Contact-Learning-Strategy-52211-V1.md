# ADR-007: Contact Learning & Management Strategy

**Date:** 2025-11-22
**Status:** Accepted
**Decision Makers:** VoiceOS Architecture Team
**Architectural Significance:** High

## Summary

LearnApp will be enhanced to discover and manage contacts from communication apps (WhatsApp, Teams, Slack, etc.), enabling natural voice commands like "call Mike Johnson on Teams". This decision establishes the architecture for unified contact management with intelligent deduplication, privacy-first design, and progressive permission model.

## Context

### Problem Statement

Users want to use voice commands to interact with contacts in communication apps, but:

1. **No Cross-App Contact Management**: Contacts are siloed within each app (WhatsApp, Teams, Slack)
2. **Voice Commands Don't Work**: "Call Mike Johnson on Teams" fails because LearnApp only learns UI elements, not contact data
3. **Duplicate Contact Problem**: Same person exists in multiple apps with different names/metadata
4. **Privacy Concerns**: Users hesitant to grant full system contacts permission for accessibility features

### Background

**Current System State:**
- LearnApp successfully scrapes app UI (buttons, screens, navigation)
- UI data stored in Room database (screen_states, elements, navigation_edges)
- Voice commands work for UI navigation ("Open Instagram", "Go to settings")
- NO contact data collection or management

**Business Requirements:**
- Enable contact-based voice commands without modifying third-party apps
- Respect user privacy (opt-in, local storage, no cloud upload)
- Support multiple communication apps (WhatsApp, Teams, Slack, Discord, etc.)
- Intelligent deduplication across apps

**Technical Constraints:**
- Android accessibility API limitations (read-only, UI scraping only)
- No access to app databases or system contacts (without permission)
- Must work offline (local-first architecture)
- Room database with KSP (VoiceOS standard)

**Stakeholder Concerns:**
- **Privacy advocates**: Concerned about contact data collection
- **Power users**: Want unified contact management across all apps
- **Enterprise users**: Need Teams/Slack integration without system permission

### Scope

**Included:**
- Contact scraping from communication apps (WhatsApp, Teams, Slack, etc.)
- Unified contact storage with deduplication
- Voice command integration for contact-based actions
- Export/import functionality (VOS Compact format)
- Progressive permission model (Accessibility → System Linked → Full Sync)
- AVA NLU integration for intelligent matching (v2.0)

**Excluded:**
- System contacts management (handled by Android)
- Cloud sync (future consideration)
- Contact editing within VoiceOS (v1.0 - read-only)
- Real-time contact updates (future consideration)

## Decision

**LearnApp will implement Contact Learning & Management as a separate feature layer on top of UI learning, using accessibility-based scraping with progressive permission escalation.**

### Core Components

#### Component 1: Contact Scraper

**Purpose**: Extract contact data from app UI using accessibility service

**Approach**:
- After UI learning completes, prompt user for contact learning consent
- Navigate to app's contacts/people screen using learned UI map
- Scrape visible contact data (names, profile pictures, visible metadata)
- Store per-app in `contact_sources` table

**Key Design Decisions**:
- Separate consent from UI learning (user can decline contact learning)
- Use heuristics to find contacts screen ("Contacts", "People", "Chats")
- Adaptive storage (only save visible fields, not predefined schema)
- No access to message history or call logs

#### Component 2: Contact Deduplicator

**Purpose**: Merge contacts across apps to create unified contact database

**Approach**:
- Normalize contact names ("Mike Johnson" → "mikejohnson")
- Group sources by normalized name
- Create `unified_contacts` entries
- Link `contact_sources` to unified contacts via foreign key

**Normalization Algorithm**:
```kotlin
fun normalizeContactName(name: String): String {
    // Handle "LastName, FirstName" → "FirstName LastName"
    val parts = name.split(",").map { it.trim() }
    val reordered = if (parts.size == 2) "${parts[1]} ${parts[0]}" else name

    // Lowercase and remove non-alphabetic
    return reordered.lowercase().replace(Regex("[^a-z]"), "")
}
```

**Deduplication Rules**:
1. Exact normalized name match → Merge
2. User can override (set preferred source)
3. Metadata merged (phone numbers, emails combined)
4. Best profile picture selected (highest resolution)

#### Component 3: Progressive Permission Model

**Level 0: No Permission**
- Contact learning not available
- Only UI voice commands work

**Level 1: Accessibility Only (Default)**
- Scrape visible contact data from approved apps
- Store in local database
- Voice commands work ("call Mike on Teams")
- Export/import VOS format
- NO system contacts permission required

**Level 2: System Linked (Optional Upgrade)**
- Requires READ_CONTACTS permission
- Match learned contacts with phone contacts
- Enrich contact data (add phone numbers, emails from system)
- Show "Also in Phone Contacts" tag
- One-way sync (read only)

**Level 3: Full Sync (Future - v2.0)**
- Requires WRITE_CONTACTS permission
- Bi-directional sync with system contacts
- Update system contacts from learned apps
- Automatic duplicate merging

#### Component 4: VOS Compact Export Format

**Purpose**: Efficient contact export/import for backup and portability

**Format**: JSON with 3-letter keys (inspired by IPC protocol)

**Key Advantages**:
- 44% size reduction vs standard JSON
- Human-readable (unlike binary formats)
- Schema versioned ("vos-cnt-v1")
- Supports encryption (v2.0)

**Example Structure**:
```json
{
  "SCH": "vos-cnt-v1",
  "VER": "1.0.0",
  "CNT": [
    {
      "CID": "u1",
      "NAM": "Mike Johnson",
      "NRM": "mikejohnson",
      "SRC": [...],
      "MRG": {...}
    }
  ]
}
```

### Implementation Approach

**Phase 1: Core Contact Learning (Q1 2026)**
1. Contact scraper implementation
2. Database schema (4 new tables)
3. Deduplication engine
4. Basic voice command integration
5. VOS export functionality

**Phase 2: System Integration (Q2 2026)**
1. READ_CONTACTS permission flow
2. System contact matching
3. Contact enrichment
4. VOS import functionality

**Phase 3: AVA NLU Integration (Q3 2026)**
1. Intelligent contact matching
2. Context-aware disambiguation
3. WRITE_CONTACTS support
4. Multi-device sync

## Alternatives Considered

### Alternative 1: Use Android ContentProvider for Contacts

**Rationale**: Android's ContentProvider system designed for contact management

**Benefits**:
- Standard Android approach
- Well-documented API
- Built-in deduplication
- Sync framework integration

**Drawbacks**:
- Requires READ_CONTACTS permission upfront
- No progressive permission model
- Can't distinguish app-specific contacts
- Violates privacy-first principle (must request permission before offering feature)

**Rejected Because**: Forces users to grant system permission before they can try contact learning, conflicts with privacy-first design.

### Alternative 2: Per-App Contact Storage (No Unification)

**Rationale**: Store contacts separately for each app, no cross-app merging

**Benefits**:
- Simpler implementation (no deduplication)
- Clearer data ownership
- No name matching ambiguity
- Smaller database footprint

**Drawbacks**:
- User must specify app every time ("call Mike on Teams")
- Duplicate contacts visible in UI
- No intelligent matching across apps
- Poor user experience

**Rejected Because**: Defeats purpose of unified contact management, creates friction in voice commands.

### Alternative 3: Cloud-Based Contact Sync

**Rationale**: Upload contacts to VoiceOS cloud for sync across devices

**Benefits**:
- Multi-device support
- Backup and restore built-in
- Real-time updates across devices
- Can use server-side deduplication

**Drawbacks**:
- Privacy violation (data leaves device)
- Requires internet connection
- Hosting costs and infrastructure
- User trust concerns
- GDPR/privacy regulation complexity

**Rejected Because**: Violates core VoiceOS principle of local-first, offline-capable architecture.

## Consequences

### Positive Outcomes

**Benefit 1: Natural Voice Commands for Contacts**
- Impact: Users can say "call Mike Johnson on Teams" instead of complex navigation
- Measurement: Voice command success rate for contact actions (target: 95%+)

**Benefit 2: Privacy-First by Default**
- Impact: No system permission required for contact learning
- Measurement: Permission grant rate (expect <30% users grant READ_CONTACTS)

**Benefit 3: Cross-App Contact Unification**
- Impact: One contact record for "Mike Johnson" across WhatsApp, Teams, Slack
- Measurement: Deduplication rate (target: 70-80% contacts merged)

**Benefit 4: Portable Contact Data**
- Impact: Users can export/import contacts for backup or migration
- Measurement: Export/import success rate (target: 99%+)

### Negative Impacts

**Challenge 1: Complex Deduplication Logic**
- Description: Name matching across different formats ("Mike Johnson" vs "Johnson, Mike")
- Mitigation: Implement robust normalization algorithm with user override
- Fallback: Allow users to manually merge/unmerge contacts

**Challenge 2: Increased Database Size**
- Description: Contact data adds ~5-10MB per 1000 contacts
- Mitigation: Adaptive storage (only save visible fields), lazy loading, pagination
- Monitoring: Track database size growth, warn at 100MB

**Challenge 3: Contact Scraping Reliability**
- Description: Apps may change contact screen layout, breaking scraper
- Mitigation: Heuristic-based screen detection, fallback patterns, user can trigger re-learning
- Monitoring: Track scraping success rate per app

### Trade-offs

**Trade-off 1: Privacy vs Convenience**
- What we lose: Can't access full phone contact data without permission
- What we gain: Users trust VoiceOS more, no permission barrier to entry
- Decision: Privacy wins - start with accessibility only, offer upgrade later

**Trade-off 2: Storage vs Performance**
- What we lose: ~10MB database growth per user
- What we gain: Fast local queries (no network), offline capability
- Decision: Storage is cheap, performance matters more

**Trade-off 3: Simplicity vs Accuracy**
- What we lose: Simple exact-match deduplication
- What we gain: 70-80% more contacts merged correctly
- Decision: Accuracy wins - invest in smart normalization

### Risk Assessment

#### High Risk

**Risk**: Contact scraping breaks when apps update UI
- **Mitigation**: Heuristic-based detection (multiple patterns), fallback strategies
- **Contingency**: User can manually trigger re-learning, community contributions for app-specific patterns

**Risk**: Deduplication false positives (merging different people with same name)
- **Mitigation**: Allow users to unmerge contacts, show source apps for verification
- **Contingency**: Add "undo merge" feature, manual merge/unmerge controls

#### Medium Risk

**Risk**: Performance degradation with >1000 contacts
- **Mitigation**: Pagination, lazy loading, indexed queries, background deduplication

**Risk**: User confusion about permission levels
- **Mitigation**: Clear UI explanations, progressive disclosure, in-app education

#### Low Risk

**Risk**: Export format incompatibility across versions
- **Mitigation**: Semantic versioning, backward compatibility, migration scripts

## Implementation

### Prerequisites

- [x] LearnApp v1.1 (Aggressive Exploration Mode) deployed
- [x] Room database with KSP configured
- [ ] Voice command parser updated for contact commands
- [ ] Permission request flow implemented
- [ ] Export/import UI screens designed

### Implementation Plan

#### Phase 1: Core Contact Learning (Q1 2026 - 8 weeks)

**Week 1-2: Database Schema**
- [x] Create `unified_contacts` table
- [x] Create `contact_sources` table
- [x] Create `system_integration_settings` table
- [x] Create `contact_export_history` table
- [ ] Write migration scripts
- [ ] Add database indices for performance
- **Milestone**: Database schema deployed and tested

**Week 3-4: Contact Scraper**
- [ ] Implement `ContactScraper.kt`
- [ ] Heuristic screen detection (findContactsScreen)
- [ ] Contact extraction (extractContactElements)
- [ ] Adaptive metadata parsing
- [ ] Unit tests (11 test cases)
- **Milestone**: Contact scraper successfully extracts data from WhatsApp

**Week 5-6: Deduplication Engine**
- [ ] Implement `ContactDeduplicator.kt`
- [ ] Name normalization algorithm
- [ ] Contact merging logic
- [ ] Preference system (user-selected source)
- [ ] Unit tests (8 test cases)
- **Milestone**: Deduplication merges WhatsApp + Teams contacts correctly

**Week 7-8: Voice Command Integration**
- [ ] Implement `ContactCommandHandler.kt`
- [ ] Command parsing ("call Mike on Teams")
- [ ] Contact search/matching
- [ ] Disambiguation UI
- [ ] Integration tests (5 scenarios)
- **Milestone**: "Call Mike Johnson on Teams" executes correctly

#### Phase 2: Export/Import & System Integration (Q2 2026 - 4 weeks)

**Week 1-2: VOS Export**
- [ ] Implement `ContactExporter.kt`
- [ ] VOS Compact format builder
- [ ] SHA-256 hash calculation
- [ ] Export history tracking
- [ ] Format validation tests
- **Milestone**: Export generates valid voiceos-contacts.vos file

**Week 2-3: VOS Import**
- [ ] Implement `ContactImporter.kt`
- [ ] VOS format parser
- [ ] Schema version validation
- [ ] Import conflict resolution (merge/replace/skip)
- [ ] Import tests
- **Milestone**: Import restores contacts from backup

**Week 3-4: System Contact Integration**
- [ ] READ_CONTACTS permission flow
- [ ] System contact matching
- [ ] Contact enrichment (add phone/email from system)
- [ ] "Also in Phone Contacts" UI indicator
- **Milestone**: Learned contacts linked to phone contacts

#### Phase 3: AVA NLU Integration (Q3 2026 - 6 weeks)

**Week 1-2: AVA NLU Integration**
- [ ] Intelligent contact matching
- [ ] Context-aware disambiguation
- [ ] Multi-language support
- **Milestone**: AVA NLU resolves "call my boss" to correct contact

**Week 3-4: Full Sync**
- [ ] WRITE_CONTACTS permission flow
- [ ] Bi-directional sync implementation
- [ ] Automatic duplicate merging
- [ ] Conflict resolution
- **Milestone**: Changes in VoiceOS reflect in system contacts

**Week 5-6: Encryption & Multi-Device**
- [ ] AES-256 export encryption
- [ ] Password-protected imports
- [ ] Multi-device sync protocol
- **Milestone**: Encrypted backup/restore works

### Success Criteria

#### Quantitative Metrics

- **Metric 1**: Contact scraping success rate >= 90% (across top 10 apps)
- **Metric 2**: Deduplication accuracy >= 80% (merge rate for same person)
- **Metric 3**: Voice command success rate >= 95% (for learned contacts)
- **Metric 4**: Export/import success rate >= 99% (file integrity)
- **Metric 5**: Database query performance < 100ms (for 1000 contacts)

#### Qualitative Metrics

- **Quality 1**: Users report contact voice commands "just work"
- **Quality 2**: Privacy concerns reduced (no permission barrier)
- **Quality 3**: UI is intuitive for managing contacts

### Rollback Plan

**Trigger Conditions**:
- Contact scraping success rate < 70% (too unreliable)
- Database corruption detected
- Performance degradation (queries > 500ms)
- Critical privacy vulnerability discovered

**Rollback Steps**:
1. Disable contact learning consent dialog
2. Add feature flag to disable contact features
3. Preserve existing contact data (don't delete)
4. Revert to UI-only voice commands
5. Communicate rollback to users via notification

**Recovery Time**: 2-4 hours (feature flag toggle + database migration)

## Monitoring and Review

### Key Metrics to Track

**Performance Metrics**:
- Contact scraping duration (target: <2 minutes per app)
- Deduplication processing time (target: <5 seconds for 100 contacts)
- Database query latency (target: <100ms)
- Database size growth (warn at 100MB)

**Quality Metrics**:
- Scraping success rate per app
- Deduplication accuracy (manual sampling)
- Voice command success rate for contact actions
- Export/import success rate

**User Impact Metrics**:
- Adoption rate (% users enabling contact learning)
- Permission grant rate (Level 1 → Level 2 upgrade)
- Contact usage frequency (voice commands per day)
- User satisfaction (in-app surveys)

### Review Schedule

- **30-Day Review**: Scraping reliability across top 10 apps, deduplication accuracy
- **90-Day Review**: Voice command usage patterns, permission grant trends
- **Annual Review**: Feature adoption, privacy impact, technical debt assessment

### Success Indicators

- 50%+ of LearnApp users enable contact learning
- 30%+ of users upgrade to Level 2 (system linked)
- Voice contact commands used 10+ times per day per user
- Zero privacy-related complaints or incidents

### Failure Indicators

- <20% adoption rate (users don't see value)
- Scraping success rate < 70% (too unreliable)
- Database size > 200MB per user (storage concerns)
- Privacy backlash or negative press

## Stakeholder Impact

### Internal Teams

| Team | Impact | Required Actions | Timeline |
|------|--------|------------------|----------|
| LearnApp Dev | High | Implement contact learning features | Q1-Q3 2026 |
| UI/UX Design | Medium | Design consent dialogs, contact management UI | Q1 2026 |
| QA Team | High | Test scraping across 20+ apps, edge cases | Q1-Q3 2026 |
| Privacy Team | High | Review privacy implications, update policy | Q1 2026 |
| Voice Command | Medium | Update parser for contact commands | Q1 2026 |

### External Dependencies

**Dependency 1: Android Accessibility API**
- Impact: Changes to accessibility API may affect scraping reliability
- Coordination: Monitor Android release notes, test on beta versions

**Dependency 2: Third-Party App Updates**
- Impact: App UI changes may break contact scraping
- Coordination: Community contributions for app-specific patterns, heuristic fallbacks

## Communication Plan

### Announcement

- **When**: 2025-12-01 (with LearnApp v1.1 release)
- **How**: In-app notification, blog post, release notes
- **Audience**: All VoiceOS users

### Training Requirements

**Audience 1: End Users**
- In-app tutorial for contact learning
- Video demonstration (YouTube)
- FAQ section in user manual

**Audience 2: Developers**
- Developer manual chapter (completed)
- Code examples and API reference
- Architecture diagrams

### Documentation Updates

- [x] Developer manual: Contact Learning & Management chapter
- [x] User manual: Voice Contacts section
- [ ] Privacy policy update (contact data handling)
- [ ] Release notes (v2.0 features)
- [ ] API documentation (LearnApp repository)

## Related Documents

- **Developer Manual**: [Contact Learning & Management](../../modules/LearnApp/developer-manual.md#contact-learning--management)
- **User Manual**: [Voice Contacts](../../modules/LearnApp/user-manual.md#voice-contacts)
- **Related ADRs**:
  - [ADR-008: VOS Compact Format](./ADR-008-VOS-Compact-Format-251122-1619.md)
  - [ADR-009: Progressive Permission Model](./ADR-009-Progressive-Permission-Model-251122-1619.md)
- **Feature Specification**: `/docs/Active/Contact-Learning-Feature-Spec-251122-1446.md`

## Lessons Learned

### What Worked Well

*(To be completed after implementation)*

### What Could Be Improved

*(To be completed after implementation)*

### Recommendations for Future ADRs

*(To be completed after implementation)*

## Review and Updates

| Date | Change | Reason | By |
|------|--------|---------|-----|
| 2025-11-22 | ADR Created | Initial contact learning architecture decision | Claude Code |
|  |  |  |  |

---

**Template Version:** 1.0
**ADR Version:** 1.0
**Last Updated:** 2025-11-22
