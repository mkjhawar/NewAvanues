# Delta for api Specification

**Feature:** VoiceOS Command Delegation - Implement API for AVA to delegate multi-step command execution to VoiceOS AccessibilityService
**Feature ID:** 003
**Affected Spec:** `specs/api/spec.md`
**Created:** 2025-11-17

---

## Summary

This delta adds voiceos command delegation - implement api for ava to delegate multi-step command execution to voiceos accessibilityservice capability to the system. It introduces new requirements to support this functionality.

---

## ADDED Requirements

> New capabilities being introduced by this feature

### Requirement: VoiceOS Command Delegation API

The system SHALL provide delegation API in VoiceOSIntegration.kt to send command execution requests to VoiceOS's existing AccessibilityService.

**Rationale:** AVA should NOT duplicate VoiceOS's AccessibilityService. Instead, AVA (application layer) delegates UI automation to VoiceOS (platform layer), following Android best practices and clean architecture principles.

**Reference:** ADR-006 (VoiceOS Command Delegation Pattern), Developer Manual Chapter 36

**Priority:** P0 (Critical)

**Acceptance Criteria:**
- [ ] `delegateCommandExecution()` method added to VoiceOSIntegration.kt
- [ ] ExecutionResult sealed class implemented with 4 states (Success, Error, Timeout, VoiceOSNotInstalled)
- [ ] ContentProvider IPC communication established with VoiceOS
- [ ] Polling mechanism implemented (500ms intervals, 30s timeout)
- [ ] Broadcast receiver support implemented for async results
- [ ] All operations complete within 35 seconds maximum

#### Scenario: Successful command delegation to VoiceOS

**GIVEN** VoiceOS is installed on the device
**AND** AVA has successfully queried a command hierarchy from VoiceOS database
**WHEN** AVA calls `delegateCommandExecution(commandId = "cmd_call_teams", parameters = mapOf("contact" -> "John Thomas"))`
**THEN** the delegation succeeds with ExecutionResult.Success
**AND** VoiceOS returns execution_id within 100ms
**AND** command execution completes within 30 seconds
**AND** ExecutionResult.Success contains: message, executedSteps, executionTimeMs

**Test Data:**
- VoiceOS package: `com.avanues.voiceos` (installed)
- Command ID: `cmd_call_teams`
- Parameters: `{"contact": "John Thomas"}`

**Expected Result:**
- ExecutionResult.Success(message = "Command executed successfully", executedSteps = 3, executionTimeMs = 2500)

#### Scenario: VoiceOS not installed

**GIVEN** VoiceOS is NOT installed on the device
**WHEN** AVA calls `delegateCommandExecution(commandId = "cmd_any")`
**THEN** the delegation immediately returns ExecutionResult.VoiceOSNotInstalled
**AND** no ContentProvider queries are attempted

**Test Data:**
- VoiceOS package: NOT installed

**Expected Result:**
- ExecutionResult.VoiceOSNotInstalled (returned immediately)

#### Scenario: Command execution timeout

**GIVEN** VoiceOS is installed but command execution takes >30 seconds
**WHEN** AVA delegates a complex command
**AND** waits for result using polling mechanism
**THEN** after 30 seconds, ExecutionResult.Timeout is returned

**Test Data:**
- Command that intentionally delays (test mode)
- Timeout threshold: 30,000ms

**Expected Result:**
- ExecutionResult.Timeout (after 30 seconds)

#### Scenario: Command execution failure

**GIVEN** VoiceOS is installed
**WHEN** AVA delegates a command that VoiceOS cannot execute (e.g., element not found)
**THEN** ExecutionResult.Error is returned
**AND** error contains reason and failed step number

**Test Data:**
- Command ID: `cmd_invalid_element`

**Expected Result:**
- ExecutionResult.Error(reason = "Element not found: call_button", failedAtStep = 2)

---

### Requirement: ExecutionResult Sealed Class

The system MUST implement a type-safe ExecutionResult sealed class with four distinct states to represent all possible delegation outcomes.

**Rationale:** Type-safe result handling prevents runtime errors and enables exhaustive when() pattern matching in Kotlin.

**Priority:** P0 (Critical)

**Acceptance Criteria:**
- [ ] ExecutionResult sealed class created with 4 subclasses
- [ ] ExecutionResult.Success contains: message (String), executedSteps (Int), executionTimeMs (Long)
- [ ] ExecutionResult.Error contains: reason (String), failedAtStep (Int?)
- [ ] ExecutionResult.Timeout is object (no data)
- [ ] ExecutionResult.VoiceOSNotInstalled is object (no data)
- [ ] All states are exhaustive in when() expressions

#### Scenario: Type-safe result handling

**GIVEN** AVA receives ExecutionResult from delegation call
**WHEN** developer uses when() expression on result
**THEN** all four states are handled explicitly
**AND** compiler enforces exhaustive matching

**Test Data:**
```kotlin
when (result) {
    is ExecutionResult.Success -> log("Success: ${result.message}")
    is ExecutionResult.Error -> log("Error at step ${result.failedAtStep}: ${result.reason}")
    is ExecutionResult.Timeout -> log("Timeout after 30s")
    is ExecutionResult.VoiceOSNotInstalled -> log("VoiceOS not available")
}
```

**Expected Result:**
- Code compiles without errors
- No runtime casting needed

#### Scenario: Integration with existing workflows

**GIVEN** the system has existing workflows
**WHEN** voiceos command delegation - implement api for ava to delegate multi-step command execution to voiceos accessibilityservice is used within these workflows
**THEN** the feature integrates seamlessly
**AND** existing workflows continue to function correctly

---

### Requirement: ContentProvider IPC Communication

The system SHALL use Android ContentProvider for IPC communication between AVA and VoiceOS.

**Rationale:** ContentProvider is Android's standard IPC mechanism for querying and executing operations across app boundaries with proper permissions.

**Priority:** P0 (Critical)

**Acceptance Criteria:**
- [ ] AVA queries VoiceOS ContentProvider at authority: `com.avanues.voiceos.provider`
- [ ] Endpoint `/execute_command` accepts: command_id, parameters, requested_by, timestamp
- [ ] Endpoint `/execute_command` returns: execution_id (String)
- [ ] Endpoint `/execution_result/{id}` returns: status, message, executed_steps, execution_time_ms
- [ ] All ContentProvider calls use withContext(Dispatchers.IO)
- [ ] Proper error handling for NameNotFoundException, SecurityException

#### Scenario: ContentProvider execution request

**GIVEN** VoiceOS is installed with ContentProvider exposed
**WHEN** AVA queries `content://com.avanues.voiceos.provider/execute_command` with command data
**THEN** VoiceOS returns execution_id within 100ms
**AND** execution_id is non-null, non-empty String

**Test Data:**
- Authority: `com.avanues.voiceos.provider`
- URI: `content://com.avanues.voiceos.provider/execute_command`
- Values: `{command_id: "cmd_123", parameters: "{\"x\":\"y\"}", requested_by: "com.augmentalis.ava"}`

**Expected Result:**
- Cursor with execution_id: `"exec_20251116_153045_001"`

---

### Requirement: Delegation Performance

The system SHALL maintain acceptable performance with minimal IPC overhead.

**Rationale:** Delegation adds ~10ms IPC overhead (5ms request + 5ms result). Total execution time must remain under 35 seconds for complex commands.

**Priority:** P1 (High)

**Acceptance Criteria:**
- [ ] ContentProvider request completes within 100ms
- [ ] Polling queries execute every 500ms
- [ ] Total execution timeout: 30 seconds
- [ ] Maximum 60 polling attempts (30s / 500ms)
- [ ] IPC overhead ≤10ms total
- [ ] No blocking of main thread during delegation

#### Scenario: Performance under normal load

**GIVEN** the system is operating under normal load
**WHEN** user performs voiceos command delegation - implement api for ava to delegate multi-step command execution to voiceos accessibilityservice operation
**THEN** the operation completes within 2 seconds
**AND** system responsiveness is maintained

---

## MODIFIED Requirements

> No existing requirements modified by this feature

---

## REMOVED Requirements

> No requirements removed by this feature

---

## Impact Analysis

### Breaking Changes

None identified.

### Non-Breaking Changes

- Addition of voiceos command delegation - implement api for ava to delegate multi-step command execution to voiceos accessibilityservice capability
- New UI components/screens
- New data models (if applicable)

### Migration Required

- [ ] No migration required - this is a new feature

---

## Testing Requirements

### New Test Scenarios

- VoiceOS Command Delegation - Implement API for AVA to delegate multi-step command execution to VoiceOS AccessibilityService access and permissions
- VoiceOS Command Delegation - Implement API for AVA to delegate multi-step command execution to VoiceOS AccessibilityService core functionality
- VoiceOS Command Delegation - Implement API for AVA to delegate multi-step command execution to VoiceOS AccessibilityService integration with existing features
- VoiceOS Command Delegation - Implement API for AVA to delegate multi-step command execution to VoiceOS AccessibilityService performance under load

### Coverage Goals

- Unit test coverage: ≥80%
- Integration test coverage: ≥70%
- E2E test coverage: Critical paths

---

## Performance Impact

**Expected Changes:**
- Minimal impact to existing operations
- New operations complete within acceptable timeframes

**Benchmarking Required:**
- [ ] Measure voiceos command delegation - implement api for ava to delegate multi-step command execution to voiceos accessibilityservice operation time
- [ ] Verify no regression in existing features

---

## Security Impact

**New Security Considerations:**
- Authentication required for voiceos command delegation - implement api for ava to delegate multi-step command execution to voiceos accessibilityservice
- Authorization checks enforce appropriate permissions
- Input validation prevents malicious data

**Security Review Required:** Yes

---

## Documentation Updates Required

- [ ] User guide for voiceos command delegation - implement api for ava to delegate multi-step command execution to voiceos accessibilityservice feature
- [ ] API documentation (if applicable)
- [ ] Architecture documentation updates
- [ ] Release notes entry

---

## Merge Instructions

**When this feature is archived:**

1. **Apply ADDED requirements** to `specs/api/spec.md`
   - Add all three new requirements
   - Include all scenarios
   - Update spec version number

2. **Update spec metadata**
   - Increment version
   - Add entry to change history

---

## Validation Checklist

Before merging this delta:

- [ ] All ADDED requirements have ≥1 scenario
- [ ] All requirements use SHALL/MUST language
- [ ] All scenarios use GIVEN/WHEN/THEN structure
- [ ] Acceptance criteria are specific and testable
- [ ] Test coverage requirements are defined
- [ ] Documentation updates are identified

---

**Template Version:** 6.0.0
**Last Updated:** 2025-11-17
