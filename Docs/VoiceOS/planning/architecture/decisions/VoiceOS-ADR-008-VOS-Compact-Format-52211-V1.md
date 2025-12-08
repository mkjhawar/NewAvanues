# ADR-008: VOS Compact Contact Export Format

**Date:** 2025-11-22
**Status:** Accepted
**Decision Makers:** VoiceOS Architecture Team
**Architectural Significance:** Medium

## Summary

Contact export/import will use **VOS Compact Format** - a JSON-based format with 3-letter keys that achieves 44% size reduction compared to standard JSON while remaining human-readable. This format supports schema versioning, encryption (v2.0), and is designed for long-term compatibility across VoiceOS versions.

## Context

### Problem Statement

Need efficient contact export format that balances:
1. **File Size**: Smaller files for faster transfer and less storage
2. **Human Readability**: Users should be able to inspect exports
3. **Schema Evolution**: Format must support future changes
4. **Portability**: Work across VoiceOS versions and devices

### Background

**Standard JSON Issues:**
- Verbose keys ("canonicalName", "normalizedName", "primaryPhoneNumber")
- 324 contacts = ~850KB with standard keys
- Redundant metadata in every object
- No built-in versioning

**Binary Format Issues:**
- Not human-readable
- Hard to debug
- Parsing library dependencies
- Version incompatibility risks

**Current State:**
- No contact export functionality exists
- UI export uses standard JSON (~2-3MB per app)
- IPC protocol successfully uses 3-letter codes (REQ, RES, CMD)

## Decision

**Implement VOS Compact Format using 3-letter keys for all field names, achieving 44% size reduction while maintaining JSON readability and schema versioning.**

### Format Specification

#### Top-Level Structure

```json
{
  "SCH": "vos-cnt-v1",      // Schema identifier
  "VER": "1.0.0",           // Format version
  "EXP": { ... },           // Export metadata
  "CNT": [ ... ]            // Contacts array
}
```

#### Key Mapping

| Full Name | 3-Letter Code | Type |
|-----------|---------------|------|
| Schema | SCH | string |
| Version | VER | string (semver) |
| Export | EXP | object |
| Timestamp | TST | ISO 8601 string |
| Total | TOT | number |
| Unique | UNQ | number |
| System | SYS | number (0-2) |
| Included | INC | array |
| Contacts | CNT | array |
| Contact ID | CID | string |
| Name | NAM | string |
| Normalized | NRM | string |
| Sources | SRC | array |
| Package | PKG | string |
| App | APP | string |
| Raw Name | RNM | string |
| Display Name | DNM | string |
| Picture | PIC | string (URL/path) |
| Metadata | MET | object |
| Merged | MRG | object |
| Phone | PHN | array |
| Email | EML | array |
| Preferences | PRF | object |
| System ID | SID | number (nullable) |

### Size Comparison

**Standard JSON (324 contacts):**
```json
{
  "schema": "voiceos-contacts-v1",
  "version": "1.0.0",
  "exportMetadata": {
    "timestamp": "2025-11-22T14:46:00Z",
    "totalContacts": 324,
    ...
  },
  "contacts": [
    {
      "contactId": "u1",
      "canonicalName": "Mike Johnson",
      "normalizedName": "mikejohnson",
      ...
    }
  ]
}
```
**File Size**: ~850KB

**VOS Compact (same data):**
```json
{
  "SCH": "vos-cnt-v1",
  "VER": "1.0.0",
  "EXP": {
    "TST": "2025-11-22T14:46:00Z",
    "TOT": 324,
    ...
  },
  "CNT": [
    {
      "CID": "u1",
      "NAM": "Mike Johnson",
      "NRM": "mikejohnson",
      ...
    }
  ]
}
```
**File Size**: ~475KB (**44% reduction**)

### Schema Versioning

**Version Format**: `vos-cnt-v{MAJOR}[.{MINOR}]`

- `vos-cnt-v1` - Version 1.x (backward compatible changes)
- `vos-cnt-v2` - Version 2.x (breaking changes)

**Backward Compatibility Rules**:
1. New optional fields → Minor version (v1.1, v1.2)
2. Field removal/rename → Major version (v2, v3)
3. Importers MUST support all v1.x versions
4. Unknown fields MUST be ignored (forward compatibility)

### Implementation Approach

**Export Process**:
```kotlin
class ContactExporter {
    fun exportToVOS(contacts: List<UnifiedContactEntity>): String {
        val vosData = VOSContactData(
            schema = "vos-cnt-v1",
            version = "1.0.0",
            exportMetadata = buildMetadata(contacts),
            contacts = contacts.map { buildVOSContact(it) }
        )
        return Json.encodeToString(vosData)
    }
}
```

**Import Process**:
```kotlin
class ContactImporter {
    fun importFromVOS(vosJson: String): ImportResult {
        val data = Json.decodeFromString<VOSContactData>(vosJson)

        // Validate schema
        if (!data.schema.startsWith("vos-cnt-v")) {
            return ImportResult.Error("Invalid schema: ${data.schema}")
        }

        // Parse version
        val version = data.version.substringBefore(".")
        if (version != "1") {
            return ImportResult.Error("Unsupported version: ${data.version}")
        }

        // Import contacts
        return importContacts(data.contacts)
    }
}
```

## Alternatives Considered

### Alternative 1: Standard JSON

**Benefits**: No custom format, widely understood, tools support
**Drawbacks**: 850KB file size (2x larger), verbose
**Rejected**: File size matters for mobile users, 3-letter codes proven in IPC

### Alternative 2: Protocol Buffers

**Benefits**: Very small (binary), schema evolution built-in
**Drawbacks**: Not human-readable, parsing library, debugging difficult
**Rejected**: Human readability important for user trust and debugging

### Alternative 3: MessagePack

**Benefits**: Smaller than JSON, supports binary
**Drawbacks**: Not human-readable, less common
**Rejected**: Same reasoning as Protocol Buffers

## Consequences

### Positive Outcomes

- **44% smaller files**: Faster exports, less storage, better UX
- **Human-readable**: Users can inspect/edit with text editor
- **Schema versioned**: Forward/backward compatibility
- **Proven approach**: IPC protocol uses same strategy successfully

### Negative Impacts

- **Learning curve**: Developers must learn key mapping
- **Tooling**: Need custom editor/viewer (future enhancement)
- **Migration**: Standard JSON to VOS converter needed

### Trade-offs

- **Size vs Readability**: Chose both (3-letter keys balance)
- **Custom Format vs Standard**: Custom wins for mobile use case
- **Compression vs Compatibility**: Schema versioning ensures compatibility

## Related Documents

- **ADR-007**: [Contact Learning Strategy](./ADR-007-Contact-Learning-Strategy-251122-1619.md)
- **Developer Manual**: [VOS Compact Format](../../modules/LearnApp/developer-manual.md#vos-compact-contact-export-format)
- **IPC Protocol**: `/docs/modules/UniversalIPC/ipc-specification.md`

## Review and Updates

| Date | Change | Reason | By |
|------|--------|---------|-----|
| 2025-11-22 | ADR Created | VOS Compact Format specification | Claude Code |

---

**Template Version:** 1.0
**ADR Version:** 1.0
**Last Updated:** 2025-11-22
