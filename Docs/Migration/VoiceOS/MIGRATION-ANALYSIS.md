# VoiceOS Migration Analysis

Date: 2025-12-06
Status: **Phase 1 Complete, Phase 2 Pending**

---

## File Count Discrepancy Analysis

### Summary
| Category | Original VoiceOS | Migrated | Difference | Location |
|----------|------------------|----------|------------|----------|
| App code | 2308 | 2308 | 0 | `android/apps/VoiceOS/app/` ✅ |
| Libraries | 129 | 0 | 129 | **Need to move to Common/** ⚠️ |
| Modules | 886 | 0 | 886 | **Need to move to Common/** ⚠️ |
| **Total** | **3323** | **2308** | **1015** | |

### Files Needing Relocation

#### 1. Shared Libraries (129 files)
**Current:** `android/apps/VoiceOS/libraries/`
**Should be:** `Common/Libraries/VoiceOS/`
**Reason:** Shared libraries should be in Common/ for reuse across apps

```bash
android/apps/VoiceOS/libraries/
├── IntentManager/
├── NLUCore/
├── Utils/
└── VoiceCore/
```

#### 2. Feature Modules (886 files)
**Current:** `android/apps/VoiceOS/modules/`
**Should be:** `Modules/VoiceOS/` or `Common/Modules/VoiceOS/`
**Reason:** Feature modules should be in Modules/ for organization

```bash
android/apps/VoiceOS/modules/
├── accessibility/
├── automation/
├── commands/
└── voice/
```

---

## Additional Folders to Relocate

| Folder | Files | Current Location | Target Location | Reason |
|--------|-------|------------------|-----------------|--------|
| `docs/` | 182 files | `android/apps/VoiceOS/docs/` | `Docs/VoiceOS/Technical/` | Already moved partially, cleanup remaining |
| `protocols/` | Unknown | `android/apps/VoiceOS/protocols/` | `Docs/VoiceOS/Protocols/` | Documentation |
| `tests/` | Unknown | `android/apps/VoiceOS/tests/` | `android/apps/VoiceOS/app/src/test/` | Gradle convention |
| `tools/` | Scripts | `android/apps/VoiceOS/tools/` | `scripts/voiceos/` | Build tools |
| `templates/` | Templates | `android/apps/VoiceOS/templates/` | `Shared/Templates/VoiceOS/` | Shared resources |

---

## Recommended Phase 2 Actions

### 1. Move Shared Libraries
```bash
mkdir -p Common/Libraries/VoiceOS
mv android/apps/VoiceOS/libraries/* Common/Libraries/VoiceOS/
```

### 2. Move Feature Modules
```bash
mkdir -p Modules/VoiceOS
mv android/apps/VoiceOS/modules/* Modules/VoiceOS/
```

### 3. Move Documentation
```bash
mv android/apps/VoiceOS/docs/* Docs/VoiceOS/Technical/
mv android/apps/VoiceOS/protocols/* Docs/VoiceOS/Protocols/
```

### 4. Move Tools & Scripts
```bash
mkdir -p scripts/voiceos
mv android/apps/VoiceOS/tools/* scripts/voiceos/
mv android/apps/VoiceOS/templates/* Shared/Templates/VoiceOS/
```

### 5. Update build.gradle
- Update module dependencies
- Update source sets
- Update library references

---

## Verification After Phase 2

```bash
# Should return 0
find android/apps/VoiceOS/libraries -type f 2>/dev/null | wc -l

# Should return 0
find android/apps/VoiceOS/modules -type f 2>/dev/null | wc -l

# Should return 129
find Common/Libraries/VoiceOS -name "*.kt" -o -name "*.java" | wc -l

# Should return 886
find Modules/VoiceOS -name "*.kt" -o -name "*.java" | wc -l
```

---

## Questions for User

1. **Libraries:** Move to `Common/Libraries/VoiceOS/` or keep in app?
2. **Modules:** Move to `Modules/VoiceOS/` or keep in app?
3. **Tests:** Merge into app/src/test/ or keep separate?
4. **Third-party (Vosk, vivoka):** Move to `Common/ThirdParty/` or keep in app?

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking build.gradle | HIGH | Test build after each move |
| Breaking module deps | HIGH | Update gradually, test each |
| Git history loss | NONE | Already preserved via subtree |
| Duplicate files | LOW | Verify no duplicates before moving |

---

**Next Step:** Await user approval for Phase 2 actions
