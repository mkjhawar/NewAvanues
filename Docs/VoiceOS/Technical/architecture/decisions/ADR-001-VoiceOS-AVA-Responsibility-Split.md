# ADR-001: VoiceOS and AVA Responsibility Split

**Status:** Accepted
**Date:** 2025-01-28
**Decision Makers:** Development Team

## Context

During the SQLDelight migration of VoiceDataManager, several features were identified as belonging to other systems in the MainAvanues ecosystem rather than VoiceOS:

1. **UserSequence** - User-defined command sequences (macros)
2. **GDPR Consent Tracking** - Privacy consent management

VoiceOS needs a clear responsibility boundary to prevent scope creep and ensure proper separation of concerns across the ecosystem.

## Decision

### 1. VoiceOS Responsibility
VoiceOS is the **accessibility execution layer**. It:
- Receives commands via AIDL/IPC
- Executes accessibility actions (clicks, scrolls, typing)
- Manages device profiles for accessibility
- Stores command history for analytics
- Does NOT contain command intelligence or NLU logic

### 2. UserSequence → AVA NLU/LLM
User-defined command sequences are moved to AVA's NLU/LLM system because:
- Sequences require natural language understanding
- AVA already handles command interpretation
- Flow: User speaks → AVA interprets → Resolves sequence → Sends individual commands to VoiceOS

**Location:** `/Volumes/M-Drive/Coding/AVA`

### 3. GDPR Consent → Shared Ecosystem Module
GDPR consent tracking will be a shared module for all MainAvanues apps:
- Namespace: `com.augmentalis.consent` (or similar)
- Uses AIDL/IPC to synchronize consent across apps
- Covers: `com.augmentalis.*` and `com.IDEAHQ.*` apps
- One consent dialog, all apps respect it

**Location:** Future shared library in `/Volumes/M-Drive/Coding/ideacode/libraries/`

## Consequences

### Positive
- Clear separation of concerns
- VoiceOS remains focused on accessibility execution
- GDPR compliance is centralized and consistent
- Reduces code duplication across apps

### Negative
- UserSequence feature requires AVA to be implemented
- GDPR module needs to be built as separate project
- Existing code marked as deprecated (backward compatibility)

## Implementation

1. **VoiceOS Changes (Completed):**
   - `UserSequence.kt` marked as `@Deprecated`
   - `DatabaseModule.kt` GDPR methods marked as `@Deprecated`
   - Documentation added explaining migration path

2. **AVA Changes (Future):**
   - Implement `UserSequenceManager` in NLU/LLM module
   - Add AIDL interface for VoiceOS communication

3. **Shared Consent Module (Future):**
   - Create `com.augmentalis.consent` library
   - Implement AIDL/ContentProvider for cross-app consent
   - Update all ecosystem apps to use shared module

## Related Files

- `/modules/managers/VoiceDataManager/src/main/java/com/augmentalis/datamanager/data/UserSequence.kt`
- `/modules/managers/VoiceDataManager/src/main/java/com/augmentalis/datamanager/core/DatabaseModule.kt`
