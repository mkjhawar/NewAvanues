# AVU Prefix Registry

**Official registry of all AVU format 3-character prefixes**

| Attribute | Value |
|-----------|-------|
| Version | 1.0.0 |
| Updated | 2026-01-15 |
| Total Prefixes | 58 |

---

## Registry Structure

Each prefix entry follows this format:

| Field | Description |
|-------|-------------|
| Prefix | 3 uppercase letters |
| Category | Parent AVU type |
| Purpose | What this prefix represents |
| Format | Expected data structure |
| Since | Version when added |
| Status | active/deprecated |

---

## Universal Prefixes (All Types)

| Prefix | Purpose | Format | Since | Status |
|--------|---------|--------|-------|--------|
| `VER` | Version information | `VER:major:minor:patch` | 1.0.0 | active |
| `MET` | Metadata entry | `MET:key:value` | 1.0.0 | active |
| `REF` | Reference/link | `REF:id:url_or_path` | 1.0.0 | active |
| `CMT` | Comment (ignored by parser) | `CMT:any text` | 1.0.0 | active |

---

## Configuration Prefixes (AVU:CONFIG)

Schema: `avu-cfg-1.0`

| Prefix | Purpose | Format | Since | Status |
|--------|---------|--------|-------|--------|
| `PRJ` | Project definition | `PRJ:id:name:type:version` | 1.0.0 | active |
| `CFG` | Config entry | `CFG:key:value:type` | 1.0.0 | active |
| `PRF` | Profile definition | `PRF:name:p1:p2:p3:p4` | 1.0.0 | active |
| `GAT` | Quality gate | `GAT:name:threshold:enforce` | 1.0.0 | active |
| `THR` | Threshold | `THR:name:value:action` | 1.0.0 | active |
| `SWM` | Swarm config | `SWM:type:count:ratio:p1:p2:p3` | 1.0.0 | active |
| `PTH` | Path definition | `PTH:name:path` | 1.0.0 | active |
| `REG` | Registry mapping | `REG:name:pattern:location` | 1.0.0 | active |
| `FNM` | File naming rule | `FNM:id:filename:location` | 1.0.0 | active |
| `MOD` | Module definition | `MOD:name:path:status` | 1.0.0 | active |

---

## Voice Command Prefixes (AVU:VOICE)

Schema: `avu-vos-1.0`

| Prefix | Purpose | Format | Since | Status |
|--------|---------|--------|-------|--------|
| `CMD` | Voice command | `CMD:action:primary_text` | 1.0.0 | active |
| `CAT` | Category definition | `CAT:id:display_name:description` | 1.0.0 | active |
| `SYN` | Synonyms list | `SYN:action:[syn1,syn2,syn3]` | 1.0.0 | active |
| `ACT` | Action mapping | `ACT:action:handler:priority` | 1.0.0 | active |
| `LOC` | Locale marker | `LOC:locale_code` | 1.0.0 | active |
| `VAR` | Variable slot | `VAR:name:type:pattern` | 1.0.0 | active |

---

## Theme Prefixes (AVU:THEME)

Schema: `avu-thm-1.0`

| Prefix | Purpose | Format | Since | Status |
|--------|---------|--------|-------|--------|
| `THM` | Theme metadata | `THM:name:version` | 1.0.0 | active |
| `PAL` | Palette color | `PAL:key:#hexcolor` | 1.0.0 | active |
| `TYP` | Typography | `TYP:style:size:weight:family` | 1.0.0 | active |
| `SPC` | Spacing | `SPC:xs:v:sm:v:md:v:lg:v:xl:v` | 1.0.0 | active |
| `EFX` | Effects | `EFX:shadow:bool:blur:v:elevation:v` | 1.0.0 | active |
| `CMP` | Component style | `CMP:component:property:value` | 1.0.0 | active |

---

## State Prefixes (AVU:STATE)

Schema: `avu-sta-1.0`

| Prefix | Purpose | Format | Since | Status |
|--------|---------|--------|-------|--------|
| `APP` | App state | `APP:id:name:state` | 1.0.0 | active |
| `STA` | Status marker | `STA:key:value` | 1.0.0 | active |
| `SCR` | Screen state | `SCR:id:name:visible` | 1.0.0 | active |
| `ELM` | Element state | `ELM:id:type:enabled:value` | 1.0.0 | active |
| `NAV` | Navigation state | `NAV:screen:from:to` | 1.0.0 | active |
| `FCS` | Focus state | `FCS:element_id:focused` | 1.0.0 | active |

---

## Layout Prefixes (AVU:LAYOUT)

Schema: `avu-lyt-1.0`

| Prefix | Purpose | Format | Since | Status |
|--------|---------|--------|-------|--------|
| `LYT` | Layout metadata | `LYT:name:version` | 1.0.0 | active |
| `COL` | Column container | `COL:id:weight:align` | 1.0.0 | active |
| `ROW` | Row container | `ROW:id:weight:align` | 1.0.0 | active |
| `TXT` | Text component | `TXT:id:text:style` | 1.0.0 | active |
| `BTN` | Button component | `BTN:id:label:action` | 1.0.0 | active |
| `IMG` | Image component | `IMG:id:src:fit` | 1.0.0 | active |
| `CNT` | Generic container | `CNT:id:weight:align` | 1.0.0 | active |
| `STK` | Stack container | `STK:id:weight:align` | 1.0.0 | active |
| `SCR` | Scroll container | `SCR:id:direction:align` | 1.0.0 | active |
| `GRD` | Grid container | `GRD:id:columns:gap` | 1.0.0 | active |
| `END` | Close container | `END:id` | 1.0.0 | active |

---

## IPC Prefixes (AVU:IPC)

Schema: `avu-ipc-1.0`

| Prefix | Purpose | Format | Since | Status |
|--------|---------|--------|-------|--------|
| `REQ` | Request message | `REQ:id:type:payload` | 1.0.0 | active |
| `RES` | Response message | `RES:id:status:payload` | 1.0.0 | active |
| `EVT` | Event message | `EVT:type:source:data` | 1.0.0 | active |
| `ERR` | Error message | `ERR:code:message:details` | 1.0.0 | active |
| `ACK` | Acknowledgment | `ACK:request_id:status` | 1.0.0 | active |
| `BCT` | Broadcast | `BCT:channel:message` | 1.0.0 | active |

---

## Handover Prefixes (AVU:HANDOVER)

Schema: `avu-hov-1.0`

| Prefix | Purpose | Format | Since | Status |
|--------|---------|--------|-------|--------|
| `ARC` | Architecture context | `ARC:pattern:description` | 1.0.0 | active |
| `WIP` | Work in progress | `WIP:task:status:progress` | 1.0.0 | active |
| `BLK` | Blocker | `BLK:issue:severity:workaround` | 1.0.0 | active |
| `NXT` | Next step | `NXT:priority:task:context` | 1.0.0 | active |
| `USR` | User message | `USR:timestamp:message` | 1.0.0 | active |
| `FIL` | File reference | `FIL:path:action:description` | 1.0.0 | active |
| `DEC` | Decision made | `DEC:topic:choice:rationale` | 1.0.0 | active |
| `LEA` | Learning/insight | `LEA:category:insight` | 1.0.0 | active |
| `TSK` | Task/todo | `TSK:id:description:status` | 1.0.0 | active |
| `DEP` | Dependency | `DEP:module:version:status` | 1.0.0 | active |
| `API` | API change | `API:endpoint:method:change` | 1.0.0 | active |
| `BUG` | Known bug | `BUG:id:description:workaround` | 1.0.0 | active |
| `CTX` | Context marker | `CTX:key:value` | 1.0.0 | active |
| `PRI` | Priority item | `PRI:level:item:reason` | 1.0.0 | active |

---

## Reserved Prefix Ranges

| Range | Reserved For |
|-------|--------------|
| `Axx` | App/Application prefixes |
| `Bxx` | Browser/Build prefixes |
| `Cxx` | Configuration/Command prefixes |
| `Dxx` | Data/Device prefixes |
| `Exx` | Event/Error prefixes |
| `Fxx` | File/Feature prefixes |
| `Gxx` | Gate/Global prefixes |
| `Hxx` | Handover/Hook prefixes |
| `Ixx` | IPC/Intent prefixes |
| `Jxx` | Reserved (future) |
| `Kxx` | Reserved (future) |
| `Lxx` | Locale/Log prefixes |
| `Mxx` | Module/Message prefixes |
| `Nxx` | Navigation/Network prefixes |
| `Oxx` | Overlay/Output prefixes |
| `Pxx` | Project/Profile/Path prefixes |
| `Qxx` | Quality/Query prefixes |
| `Rxx` | Request/Response/Registry prefixes |
| `Sxx` | State/Screen/Swarm prefixes |
| `Txx` | Theme/Type/Task prefixes |
| `Uxx` | User/Universal prefixes |
| `Vxx` | Version/Voice/Variable prefixes |
| `Wxx` | Work/Web prefixes |
| `Xxx` | Reserved (internal) |
| `Yxx` | Reserved (internal) |
| `Zxx` | Reserved (internal) |

---

## Adding New Prefixes

### Process

1. Check this registry for conflicts
2. Follow reserved range conventions
3. Submit PR with prefix definition
4. Update UniversalAvuParser.kt
5. Update AVU-Format-Specification-V1.md

### Requirements

- Exactly 3 uppercase letters
- Not in RESERVED list (XXX, YYY, ZZZ, etc.)
- Clear, non-overlapping purpose
- Documented format specification

### Deprecation

Deprecated prefixes remain in parser for backward compatibility but:
- Are marked `deprecated` in this registry
- Emit warning during parsing
- Document migration path
- Removed in next major version

---

## Validation

```kotlin
object PrefixValidator {
    private val VALID = Regex("^[A-Z]{3}$")
    private val RESERVED = setOf("XXX", "YYY", "ZZZ", "___", "000")

    fun isValid(prefix: String): Boolean =
        prefix.matches(VALID) && prefix !in RESERVED
}
```

---

**Document Version:** 1.0.0
**Last Updated:** 2026-01-15
**Maintainer:** Augmentalis Engineering
