# ADR-009: Progressive Permission Model for Contact Learning

**Date:** 2025-11-22
**Status:** Accepted
**Decision Makers:** VoiceOS Architecture Team, Privacy Team
**Architectural Significance:** High

## Summary

Contact learning will use a **Progressive Permission Model** with three levels: (1) Accessibility Only (default, no system permission), (2) System Linked (optional READ_CONTACTS), and (3) Full Sync (future WRITE_CONTACTS). Users start with minimal permissions and upgrade only if they want additional features, respecting privacy-first principles.

## Context

### Problem Statement

Traditional Android contact management requires upfront system permissions, creating friction:

1. **Permission Barrier**: Users must grant READ_CONTACTS before seeing value
2. **All-or-Nothing**: Can't offer partial features without full permission
3. **Privacy Concerns**: Many users hesitant to grant contact access to accessibility apps
4. **Enterprise Restrictions**: Corporate devices often block contact permissions

### Background

**Android Permission Model**:
- `READ_CONTACTS`: Read system contacts (dangerous permission, user approval required)
- `WRITE_CONTACTS`: Modify system contacts (dangerous permission)
- Accessibility Service: No contact access without above permissions

**User Research**:
- 70% of users hesitant to grant contact permissions to non-contact apps
- 40% abandon feature when permission requested upfront
- 85% willing to grant if they see value first

**Privacy Principle**:
VoiceOS commitment: "No unnecessary permissions, local-first, user control"

## Decision

**Implement three-tier progressive permission model where users start with accessibility-only contact learning (no system permission) and optionally upgrade to system integration (READ_CONTACTS) or full sync (WRITE_CONTACTS) as they see value.**

### Permission Levels

#### Level 0: No Permission (Default State)

**What User Has**:
- Standard UI voice commands work
- No contact learning available

**User Can**:
- Use LearnApp for UI navigation only
- Decline contact learning permanently

**System State**:
- No contact-related permissions requested
- No contact data collection

#### Level 1: Accessibility Only (Default for Contact Learning)

**What User Gets**:
- ✅ Learn contacts from approved apps (WhatsApp, Teams, etc.)
- ✅ Voice commands work ("call Mike on Teams")
- ✅ Export/import contacts (VOS format)
- ✅ Unified contact management
- ✅ Cross-app deduplication
- ❌ Can't link with phone contacts
- ❌ Can't enrich contact data from system

**Permissions Required**:
- Accessibility Service (already granted for VoiceOS)
- NO READ_CONTACTS
- NO WRITE_CONTACTS

**User Flow**:
1. User approves app learning (WhatsApp)
2. UI learning completes
3. Dialog: "Learn contacts from WhatsApp?" → User clicks "Yes"
4. Contacts scraped via accessibility (no system permission)
5. Stored in `contact_sources` table
6. Voice commands work immediately

**Technical Implementation**:
```kotlin
// Contact scraping using accessibility service only
fun scrapeContacts(packageName: String) {
    // Navigate to contacts screen
    val contactsScreen = findContactsScreen(packageName)

    // Extract visible contact data (accessibility API)
    val contacts = extractContactElements(contactsScreen)

    // Store in local database (no system contacts access)
    contacts.forEach { contact ->
        repository.saveContactSource(contact)
    }
}
```

#### Level 2: System Linked (Optional Upgrade)

**What User Gets**:
- ✅ All Level 1 features
- ✅ Match learned contacts with phone contacts
- ✅ Enrich contact data (add phone numbers/emails from system)
- ✅ "Also in Phone Contacts" UI indicator
- ✅ One-way sync (read system contacts, don't write)
- ❌ Can't update system contacts

**Permissions Required**:
- Accessibility Service ✓
- READ_CONTACTS (user must approve)
- NO WRITE_CONTACTS

**User Flow (Upgrade)**:
1. User in contact management UI
2. Sees "Link with Phone Contacts" button
3. Explanation dialog: "This lets VoiceOS match your learned contacts with phone contacts to add phone numbers and emails."
4. User taps "Link Now"
5. System permission dialog (READ_CONTACTS)
6. If granted: Background job matches contacts
7. UI shows "Also in Phone Contacts" tags

**Technical Implementation**:
```kotlin
// System contact matching (requires READ_CONTACTS)
suspend fun linkSystemContacts() {
    if (!hasReadContactsPermission()) {
        requestReadContactsPermission()
        return
    }

    val systemContacts = getSystemContacts()
    val learnedContacts = repository.getAllUnifiedContacts()

    learnedContacts.forEach { learned ->
        val matched = findSystemContactMatch(learned, systemContacts)
        if (matched != null) {
            repository.linkSystemContact(learned.contactId, matched.id)
            enrichContactData(learned, matched)
        }
    }
}
```

#### Level 3: Full Sync (Future - v2.0)

**What User Gets**:
- ✅ All Level 2 features
- ✅ Update system contacts from learned apps
- ✅ Bi-directional sync (VoiceOS ↔ System Contacts)
- ✅ Automatic duplicate merging
- ✅ Multi-device sync (future)

**Permissions Required**:
- Accessibility Service ✓
- READ_CONTACTS ✓
- WRITE_CONTACTS (user must approve)

**User Flow (Upgrade)**:
1. User in contact management UI
2. Sees "Enable Full Sync" button
3. Explanation dialog: "This lets VoiceOS update your phone contacts with information from learned apps."
4. User taps "Enable"
5. System permission dialog (WRITE_CONTACTS)
6. If granted: Two-way sync enabled

**Implementation**: Planned for v2.0

### Upgrade Flow Diagram

```
┌──────────────────────┐
│ Level 0:             │
│ No Permission        │
│ - No contact access  │
└──────────┬───────────┘
           │
           │ User approves "Learn contacts from WhatsApp"
           ▼
┌──────────────────────────────────────┐
│ Level 1:                             │
│ Accessibility Only (DEFAULT)         │
│ - Scrape visible contact data        │
│ - Store in local database            │
│ - Voice commands work                │
│ - Export/Import VOS format           │
│ - NO system contact access           │
└──────────┬───────────────────────────┘
           │
           │ User taps "Link to Phone Contacts" (OPTIONAL)
           ▼
┌──────────────────────────────────────┐
│ Level 2:                             │
│ System Linked (OPTIONAL)             │
│ - READ_CONTACTS permission granted   │
│ - Can match with phone contacts      │
│ - Enrich data from system contacts   │
│ - Show "Also in Phone Contacts" tag  │
│ - One-way sync (read only)           │
└──────────┬───────────────────────────┘
           │
           │ User enables "Full Sync" (FUTURE - v2.0)
           ▼
┌──────────────────────────────────────┐
│ Level 3:                             │
│ Full Sync (FUTURE)                   │
│ - WRITE_CONTACTS permission granted  │
│ - Bi-directional sync                │
│ - Update system contacts             │
│ - Merge duplicates automatically     │
└──────────────────────────────────────┘
```

### Permission Request Strategy

**Never Ask Upfront**:
- ❌ Don't request READ_CONTACTS when enabling contact learning
- ❌ Don't show permission dialog before user sees value

**Progressive Disclosure**:
- ✅ User enables contact learning (Level 1, no permission)
- ✅ User uses feature, sees value
- ✅ UI offers upgrade: "Want phone numbers? Link with Phone Contacts"
- ✅ User decides to upgrade → Permission requested

**Clear Value Proposition**:
```
Dialog Text (Level 1 → Level 2 Upgrade):

"Link with Phone Contacts?"

This will:
• Add phone numbers and emails to your learned contacts
• Show which contacts are also in your phone
• Enrich contact information

This requires permission to read your phone contacts.

VoiceOS will never modify your phone contacts.

[Cancel]  [Link Now]
```

## Alternatives Considered

### Alternative 1: Require READ_CONTACTS Upfront

**Rationale**: Traditional Android approach - ask for permission first

**Benefits**:
- Simpler implementation (one code path)
- Full feature set immediately
- No upgrade flow needed

**Drawbacks**:
- 40% abandonment rate (user research)
- Violates privacy-first principle
- Barrier to entry for feature trial

**Rejected**: Conflicts with VoiceOS privacy commitment, creates friction

### Alternative 2: Two-Level Model (Skip Accessibility-Only)

**Rationale**: Only offer (1) No Permission or (2) Full System Access

**Benefits**:
- Simpler than three levels
- Clearer choice for users
- Less code complexity

**Drawbacks**:
- Forces users to grant permission to try feature
- No middle ground for privacy-conscious users
- Can't demonstrate value before asking

**Rejected**: Loses key advantage of accessibility-based scraping

### Alternative 3: Always Request WRITE_CONTACTS

**Rationale**: Plan for future bi-directional sync from day one

**Benefits**:
- One permission request for all future features
- Avoid second permission request later

**Drawbacks**:
- Users scared by write permission
- Permission fatigue
- Many users only need read-only features

**Rejected**: Over-permissioned for most users, violates principle of minimal permissions

## Consequences

### Positive Outcomes

**Benefit 1: Zero Permission Barrier to Entry**
- Impact: Users can try contact learning without granting system permission
- Measurement: Feature adoption rate (target: 50%+ of LearnApp users)

**Benefit 2: Privacy-First by Default**
- Impact: VoiceOS reputation as privacy-respecting platform
- Measurement: App store reviews mentioning privacy (target: 80% positive)

**Benefit 3: Flexible Upgrade Path**
- Impact: Users choose their privacy/convenience trade-off
- Measurement: Upgrade rate (expect 20-30% upgrade to Level 2)

**Benefit 4: Enterprise-Friendly**
- Impact: Works on corporate devices with contact permission restrictions
- Measurement: Enterprise adoption (pilot program feedback)

### Negative Impacts

**Challenge 1: Complex Implementation**
- Description: Three code paths instead of one, more testing needed
- Mitigation: Clear separation of concerns, comprehensive test suite
- Cost: +20% development time

**Challenge 2: User Confusion**
- Description: Users may not understand permission levels
- Mitigation: Clear UI explanations, in-app help, progressive disclosure
- Monitoring: Track support tickets about permissions

**Challenge 3: Incomplete Data at Level 1**
- Description: Phone numbers/emails missing without system access
- Mitigation: Clear communication ("Want phone numbers? Upgrade to System Linked")
- User Impact: Most users okay with contact names only for voice commands

### Trade-offs

**Trade-off 1: Complexity vs Privacy**
- What we lose: Simple single-permission model
- What we gain: Privacy-first approach, zero barrier to entry
- Decision: Privacy wins - worth the complexity

**Trade-off 2: Feature Completeness vs Adoption**
- What we lose: Full feature set at Level 1
- What we gain: 50%+ adoption vs 30% with upfront permission
- Decision: Adoption wins - get users in the door first

## Implementation

### Technical Architecture

**Database Schema** (already designed):
- `system_integration_settings` table tracks permission level
- Foreign key `systemContactId` in `unified_contacts` (null at Level 1)

**Permission Level Tracking**:
```kotlin
enum class ContactPermissionLevel {
    NONE,                 // Level 0
    ACCESSIBILITY_ONLY,   // Level 1
    SYSTEM_LINKED,        // Level 2
    FULL_SYNC             // Level 3 (future)
}

class ContactPermissionManager {
    suspend fun getCurrentLevel(): ContactPermissionLevel {
        val settings = repository.getIntegrationSettings()

        return when {
            !settings.contactLearningEnabled -> NONE
            !hasReadContactsPermission() -> ACCESSIBILITY_ONLY
            !hasWriteContactsPermission() -> SYSTEM_LINKED
            else -> FULL_SYNC
        }
    }

    suspend fun requestUpgrade(targetLevel: ContactPermissionLevel) {
        when (targetLevel) {
            SYSTEM_LINKED -> requestReadContactsPermission()
            FULL_SYNC -> requestWriteContactsPermission()
            else -> { /* No permission needed */ }
        }
    }
}
```

### UI/UX Design

**Level 1 → Level 2 Upgrade UI**:
```
Contact Management Screen

┌─────────────────────────────────────┐
│ 📱 Your Voice Contacts              │
├─────────────────────────────────────┤
│                                     │
│ 👤 Mike Johnson                     │
│    Found in: WhatsApp, Teams        │
│                                     │
│ 👤 Sarah Williams                   │
│    Found in: Slack                  │
│                                     │
│ ... (167 contacts)                  │
│                                     │
├─────────────────────────────────────┤
│ 💡 Want phone numbers?              │
│                                     │
│ Link with Phone Contacts to add:   │
│ • Phone numbers                     │
│ • Email addresses                   │
│ • See which contacts are in phone   │
│                                     │
│         [Link Now] [Not Now]        │
└─────────────────────────────────────┘
```

### Success Criteria

**Quantitative Metrics**:
- Feature adoption rate >= 50% (Level 1)
- Upgrade rate to Level 2: 20-30%
- Permission grant rate (when requested): 80%+
- Support tickets about permissions: <5% of users

**Qualitative Metrics**:
- User feedback: "I love that I didn't have to grant permission"
- Privacy reviews: "VoiceOS respects my privacy"
- Enterprise feedback: "Works on our corporate devices"

## Related Documents

- **ADR-007**: [Contact Learning Strategy](./ADR-007-Contact-Learning-Strategy-251122-1619.md)
- **User Manual**: [Progressive Permission Levels](../../modules/LearnApp/user-manual.md#progressive-permission-levels)
- **Developer Manual**: [Progressive Permission Model](../../modules/LearnApp/developer-manual.md#progressive-permission-model)

## Review and Updates

| Date | Change | Reason | By |
|------|--------|---------|-----|
| 2025-11-22 | ADR Created | Progressive permission model specification | Claude Code |

---

**Template Version:** 1.0
**ADR Version:** 1.0
**Last Updated:** 2025-11-22
