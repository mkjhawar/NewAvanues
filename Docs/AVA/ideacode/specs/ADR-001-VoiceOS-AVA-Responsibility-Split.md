# ADR-001: VoiceOS and AVA Responsibility Split

**Status:** Accepted
**Date:** 2025-01-28
**Decision Makers:** Development Team

## Context

The VoiceOS SQLDelight migration identified features that belong in AVA rather than VoiceOS. A clear responsibility boundary is needed to prevent scope creep and ensure proper separation of concerns.

## Decision

### 1. VoiceOS Responsibility
VoiceOS is the **accessibility execution layer**. It:
- Receives commands via AIDL/IPC
- Executes accessibility actions (clicks, scrolls, typing)
- Manages device profiles for accessibility
- Stores command history for analytics
- Does NOT contain command intelligence or NLU logic

### 2. AVA Responsibility - NLU/LLM
AVA handles all **natural language understanding and command intelligence**:

#### UserSequence (Macros) - TO BE IMPLEMENTED
- User-defined command sequences belong in AVA's NLU/LLM module
- AVA interprets spoken trigger phrases
- Resolves sequences into individual commands
- Sends commands to VoiceOS for execution

**Flow:**
1. User speaks: "Do my morning routine"
2. AVA NLU recognizes trigger phrase
3. AVA resolves to sequence: [open_weather, open_calendar, open_email]
4. AVA sends each command to VoiceOS via AIDL
5. VoiceOS executes accessibility actions

**Implementation needed:**
- `UserSequenceManager` in NLU/LLM module
- Storage for user-defined sequences
- AIDL interface for VoiceOS communication

### 3. GDPR Consent - Shared Ecosystem Module
GDPR consent tracking will be a shared module for all MainAvanues apps:
- Namespace: `com.augmentalis.consent` (or similar)
- Uses AIDL/IPC to synchronize consent across apps
- Covers: `com.augmentalis.*` and `com.IDEAHQ.*` apps
- One consent dialog, all apps respect it

**Location:** Future shared library in `/Volumes/M-Drive/Coding/ideacode/libraries/`

## Implementation Roadmap

### Phase 1: AVA NLU/LLM (Future)
1. Create `UserSequenceManager` class
2. Design sequence storage schema
3. Implement trigger phrase matching
4. Add AIDL client for VoiceOS communication

### Phase 2: Shared Consent Module (Future)
1. Create `com.augmentalis.consent` library
2. Implement consent dialog UI
3. Create AIDL/ContentProvider for cross-app sync
4. Update all ecosystem apps to use shared module

## Consequences

### Positive
- AVA owns all NLU/LLM intelligence
- VoiceOS stays focused on accessibility execution
- Clear IPC boundaries between systems
- GDPR compliance is centralized

### Negative
- UserSequence requires implementing IPC layer
- GDPR module is a separate project
- Coordination needed between teams

## Related Documents

- VoiceOS ADR-001: `/Volumes/M-Drive/Coding/VoiceOS/docs/architecture/decisions/ADR-001-VoiceOS-AVA-Responsibility-Split.md`
- VoiceOS deprecated files:
  - `UserSequence.kt` - marked `@Deprecated`
  - `DatabaseModule.kt` - GDPR methods marked `@Deprecated`
