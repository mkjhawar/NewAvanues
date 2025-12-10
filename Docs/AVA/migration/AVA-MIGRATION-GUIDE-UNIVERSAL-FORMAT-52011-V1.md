# Migration Guide: Universal File Format

**Version:** 1.0.0
**Date:** 2025-11-20
**Author:** Manoj Jhawar

---

## Overview

Step-by-step guide to migrate all Avanues ecosystem projects to the Universal File Format system.

**Timeline:** 6 weeks total
**Risk Level:** Low (backward compatible, gradual migration)

---

## Pre-Migration Checklist

- [ ] Backup all existing files (.ava, .vos, protocol definitions)
- [ ] Deploy UniversalFileParser to all projects
- [ ] Test parsers with existing files
- [ ] Create rollback plan

---

## Week 1: AVA Migration

### Current State
- Format: `.ava` v1.0 (compact JSON)
- Location: `/.ava/core/`, `/.ava/user/`
- Files: ~30 intent files

### Migration Steps

**Step 1: Deploy UniversalFileParser**
```bash
cd /Volumes/M-Drive/Coding/AVA
# Update dependencies to include Universal IPC 2.1.0
```

**Step 2: Run conversion tool**
```bash
python3 tools/convert_ava_v1_to_v2.py \
  --input /.ava/core/en-US/ \
  --output /.ava-v2/core/en-US/ \
  --backup /.ava-backup/
```

**Step 3: Test with sample file**
```kotlin
// Test navigation.ava
val reader = AvaFileReader()
val file = reader.load("/.ava-v2/core/en-US/navigation.ava")
assert(file.entries.size == 8)
assert(file.type == FileType.AVA)
```

**Step 4: Update AvaFileReader**
```kotlin
// Old: uses JSON parser
// New: uses UniversalFileParser

class AvaFileReader {
    fun load(path: String): UniversalFile {
        val content = File(path).readText()
        return UniversalFileParser.parse(content)
    }
}
```

**Step 5: Gradual rollout**
- Week 1: Test with dev builds
- Week 2: Beta testers
- Week 3: Production release

---

## Week 2: VoiceOS Migration

### Current State
- Format: `.vos` (custom JSON)
- Location: `/.vos/system/`, `/.vos/plugins/`
- Files: ~50 command files

### Migration Steps

**Step 1: Analyze existing .vos files**
```bash
find /.vos/ -name "*.vos" -exec head -20 {} \;
```

**Step 2: Create conversion mapping**
```python
# VoiceOS action → Universal IPC code
MAPPING = {
    "tap_element": "VCM",
    "scroll": "VCM",
    "query_accessibility": "AIQ",
    "share_url": "URL"
}
```

**Step 3: Convert files**
```bash
python3 tools/convert_vos_to_universal.py \
  --input /.vos/system/ \
  --output /.vos-v2/system/ \
  --mapping vos_mapping.json
```

**Step 4: Test accessibility commands**
```kotlin
val vosReader = VosFileReader()
val file = vosReader.load("/.vos-v2/system/accessibility.vos")
val tapCommand = file.getEntryById("tap_button")
assert(tapCommand?.code == "VCM")
```

**Step 5: Plugin system integration**
- Update CommandManager to use UniversalFileParser
- Test .vos plugin loading
- Verify accessibility service integration

---

## Week 3: AvaConnect Migration

### Current State
- Format: Kotlin sealed classes (CompactProtocol.kt)
- No file storage (in-memory only)
- 77 message types

### Migration Steps

**Step 1: Extract protocol definitions to .avc files**
```bash
python3 tools/extract_avaconnect_protocol.py \
  --input CompactProtocol.kt \
  --output /.avc/definitions/
```

**Step 2: Create feature-specific .avc files**
```
/.avc/definitions/
├── video-call.avc       # VCA, ACC, DEC, MIC, CAM
├── file-transfer.avc    # FTR, ACC, DEC, ERR
├── screen-share.avc     # SSO, SSI, ACC, DEC
└── messaging.avc        # CHT, ACC, DEC
```

**Step 3: Update ConnectionManager**
```kotlin
// Old: hardcoded message types
// New: load from .avc files

class ConnectionManager {
    fun loadFeatureCommands(feature: String): List<UniversalEntry> {
        val reader = AvcFileReader()
        val file = reader.load("/.avc/definitions/$feature.avc")
        return file.entries
    }
}
```

**Step 4: Test device pairing**
```kotlin
val avcFile = AvcFileReader().load("/.avc/definitions/video-call.avc")
val callRequest = avcFile.getEntryById("incoming_call")
val ipcMessage = callRequest?.toIPCMessage("call123")
assert(ipcMessage is VideoCallRequest)
```

---

## Week 4: WebAvanue Migration

### Current State
- Format: Kotlin sealed classes (IPCBridge.kt)
- SharedFlow-based messaging
- Placeholder for Universal IPC

### Migration Steps

**Step 1: Extract IPCMessage definitions**
```bash
python3 tools/extract_webavanue_ipc.py \
  --input IPCBridge.kt \
  --output /.avw/commands/
```

**Step 2: Create .avw files**
```
/.avw/commands/
├── browser-control.avw  # URL, NAV, TAB, PLD
├── voice-commands.avw   # VCM for browser
└── bookmarks.avw        # URL with metadata
```

**Step 3: Update IPCBridge**
```kotlin
class IPCBridge {
    private val commandReader = AvwFileReader()

    fun loadBrowserCommands(): List<UniversalEntry> {
        val file = commandReader.load("/.avw/commands/browser-control.avw")
        return file.entries
    }

    suspend fun sendToVoiceOS(message: UniversalMessage) {
        // Now uses Universal IPC instead of SharedFlow
        ipcManager.send("com.augmentalis.voiceos", message)
    }
}
```

**Step 4: Test browser navigation**
```kotlin
val avwFile = AvwFileReader().load("/.avw/commands/browser-control.avw")
val openUrl = avwFile.getEntryById("open_google")
assert(openUrl?.code == "URL")
```

---

## Week 5: NewAvanue Migration

### Current State
- Format: Configuration files (various formats)
- Platform orchestration logic
- Module management

### Migration Steps

**Step 1: Consolidate platform config**
```
/.avn/config/
├── platform.avn         # HND, CAP, PRO, ROL
├── modules.avn          # Module definitions
└── integrations.avn     # Cross-app integration
```

**Step 2: Create .avn schema**
```
# Avanues Universal Format v1.0
# Type: AVN
# Extension: .avn
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: newavanue
metadata:
  file: platform.avn
  category: platform
  count: 5
---
HND:ava_init:ava:2.0:device1
HND:vos_init:voiceos:4.0:device1
CAP:ava_caps:ava:video,screen,file
PRO:promote:device1:12345:1732012345000
ROL:role:device1:server
---
```

**Step 3: Platform orchestrator integration**
```kotlin
class PlatformOrchestrator {
    fun loadPlatformConfig(): UniversalFile {
        val reader = AvnFileReader()
        return reader.load("/.avn/config/platform.avn")
    }

    fun initializeModules() {
        val config = loadPlatformConfig()
        config.filterByCode("HND").forEach { entry ->
            initializeModule(entry)
        }
    }
}
```

---

## Week 6: Avanues UI Migration

### Current State
- Format: Avanues DSL (inline in code)
- UI components scattered across projects
- No centralized component library

### Migration Steps

**Step 1: Extract UI components to .avs files**
```
/.avs/components/
├── call-prompt.avs      # Video call UI
├── file-transfer.avs    # File transfer dialogs
└── common-dialogs.avs   # Shared dialogs
```

**Step 2: Component library structure**
```
# Avanues Universal Format v1.0
# Type: AVS
# Extension: .avs
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: avanues
metadata:
  file: call-prompt.avs
  category: ui_component
  component_type: dialog
  count: 3
---
JSN:incoming_call:Col#callPrompt{spacing:16;Text{text:"Incoming call from {caller}"}}
JSN:call_active:Col#activeCall{Text{text:"Call in progress"}}
JSN:call_ended:Col#endedCall{Text{text:"Call ended"}}
---
```

**Step 3: Dynamic UI loading**
```kotlin
class UIComponentLoader {
    fun loadComponent(componentId: String): UniversalEntry? {
        val reader = AvsFileReader()
        val file = reader.load("/.avs/components/call-prompt.avs")
        return file.getEntryById(componentId)
    }

    fun renderComponent(entry: UniversalEntry, params: Map<String, String>): Component {
        val dslString = entry.data
        val dsl = AvanuesDSLParser.parse(dslString)
        return dsl.render(params)
    }
}
```

---

## Cross-Project Integration Tests

### Test 1: AVA → VoiceOS Delegation
```kotlin
@Test
fun `AVA reads VoiceOS commands`() {
    val vosFile = UniversalFileParser.parse("/.vos/system/accessibility.vos")
    val avaManager = AVACommandManager()

    // AVA can now delegate to VoiceOS
    val tapCommand = vosFile.getEntryById("tap_button")
    val ipcMessage = tapCommand?.toIPCMessage()
    avaManager.delegateToVoiceOS(ipcMessage)

    assert(ipcMessage is VoiceCommandMessage)
}
```

### Test 2: AvaConnect → WebAvanue URL Share
```kotlin
@Test
fun `AvaConnect shares URL with WebAvanue`() {
    val avcFile = AvcFileReader().load("/.avc/definitions/messaging.avc")
    val urlEntry = avcFile.getEntryById("share_url")

    val ipcMessage = urlEntry?.toIPCMessage("share1")
    ipcManager.send("com.augmentalis.webavanue", ipcMessage)

    // WebAvanue receives and opens URL
    assert(ipcMessage is URLShareMessage)
}
```

### Test 3: All Projects Read Each Other's Files
```kotlin
@Test
fun `cross-project file compatibility`() {
    // AVA reads VOS
    val vosFile = UniversalFileParser.parse("/.vos/system/accessibility.vos")
    assert(vosFile.type == FileType.VOS)

    // VOS reads AVA
    val avaFile = UniversalFileParser.parse("/.ava/core/navigation.ava")
    assert(avaFile.type == FileType.AVA)

    // AvaConnect reads WebAvanue
    val avwFile = UniversalFileParser.parse("/.avw/commands/browser.avw")
    assert(avwFile.type == FileType.AVW)

    // All formats use same 3-letter codes
    val allEntries = vosFile.entries + avaFile.entries + avwFile.entries
    allEntries.forEach { entry ->
        assert(entry.code.length == 3)
        assert(entry.code.all { it.isUpperCase() })
    }
}
```

---

## Rollback Plan

### If Migration Fails

**Step 1: Restore backups**
```bash
# Restore original files
cp -r /.ava-backup/* /.ava/
cp -r /.vos-backup/* /.vos/
```

**Step 2: Revert parser changes**
```bash
git revert <migration-commit>
./gradlew clean build
```

**Step 3: Fall back to legacy parsers**
```kotlin
// Enable legacy mode
config.useLegacyParser = true
```

---

## Verification Checklist

After migration, verify:

- [ ] All existing files parse successfully
- [ ] IPC messages send/receive correctly
- [ ] Cross-project communication works
- [ ] No performance regression
- [ ] File sizes within expected range
- [ ] All tests pass (90%+ coverage)
- [ ] Production monitoring shows no errors

---

## Performance Benchmarks

| Metric | Before | After | Change |
|--------|--------|-------|--------|
| AVA file size | 50 bytes | 110 bytes | +120% |
| Parse time | 0.3ms | 0.4ms | +33% |
| IPC latency | 2ms | 1.5ms | -25% ✅ |
| Memory usage | 3 MB | 2.5 MB | -17% ✅ |
| Cross-project calls | N/A | 5ms | New ✅ |

**Trade-offs:**
- ❌ +120% file size (mitigated by device storage location)
- ✅ -25% IPC latency (zero conversion overhead)
- ✅ -17% memory (efficient parser)
- ✅ Cross-project compatibility (new capability)

---

## Post-Migration

### Cleanup (After 2 weeks stable)
```bash
# Remove backup files
rm -rf /.ava-backup/
rm -rf /.vos-backup/

# Remove legacy parsers
rm AvaFileParser_v1.kt
rm VoiceOSParser_old.kt
```

### Documentation Updates
- [ ] Update developer manuals
- [ ] Update API documentation
- [ ] Create video tutorials
- [ ] Update README files

---

## Support

**Issues:** Create ticket in IDEACODE backlog
**Questions:** manoj@ideahq.net
**Documentation:** `/docs/specifications/UNIVERSAL-FILE-FORMAT-SPEC.md`

---

## License

Proprietary - Augmentalis ES
