# Backlog: Secure Master Key Rotation

**Priority:** High (before production release)
**Estimated Effort:** 2-3 hours
**Blocked By:** None
**Created:** 2025-11-23

---

## Summary

Implement secure master key rotation for AON file HMAC signatures. Currently using placeholder key that's hardcoded in source code.

---

## Problem

**Current Implementation (INSECURE):**
```kotlin
// In AONFileManager.kt
private const val MASTER_KEY = "AVA-AON-HMAC-SECRET-KEY-V1-CHANGE-IN-PRODUCTION"
```

**Security Risks:**
- ❌ Visible in Git history
- ❌ Extractable via APK decompilation
- ❌ Same key forever (no rotation)
- ❌ If leaked, ALL models compromised

---

## Solution

### Step 1: Generate Secure Key
```bash
# Generate cryptographically random 256-bit key
openssl rand -hex 32 > ~/.ava-aon-master-key.txt

# Store in password manager (LastPass, 1Password, etc.)
```

### Step 2: Environment Variable
```bash
# Add to ~/.zshrc or ~/.bashrc
export AVA_AON_MASTER_KEY="$(cat ~/.ava-aon-master-key.txt)"

# For CI/CD:
# GitHub Actions: Settings → Secrets → AVA_AON_MASTER_KEY
# GitLab CI: Settings → CI/CD → Variables → AVA_AON_MASTER_KEY
```

### Step 3: Build Configuration
```kotlin
// In app/build.gradle.kts
android {
    buildTypes {
        release {
            buildConfigField(
                "String",
                "AON_MASTER_KEY",
                "\"${System.getenv("AVA_AON_MASTER_KEY") ?: error("AVA_AON_MASTER_KEY not set")}\""
            )
        }
        debug {
            // Use different key for debug builds
            buildConfigField(
                "String",
                "AON_MASTER_KEY",
                "\"AVA-AON-DEBUG-KEY-NOT-FOR-PRODUCTION\""
            )
        }
    }
}
```

### Step 4: Update AONFileManager
```kotlin
// In AONFileManager.kt
- private const val MASTER_KEY = "AVA-AON-HMAC-SECRET-KEY-V1-CHANGE-IN-PRODUCTION"
+ private val MASTER_KEY = BuildConfig.AON_MASTER_KEY
```

### Step 5: ProGuard Obfuscation
```proguard
# In proguard-rules.pro
-keep class com.augmentalis.ava.features.rag.embeddings.AONFileManager {
    public <methods>;
}
-obfuscate
-repackageclasses 'o'
```

---

## Rotation Schedule

### Quarterly (Recommended)
```bash
# January 2025
export AVA_AON_MASTER_KEY_V1="key_2025_q1"

# April 2025 - ROTATE
export AVA_AON_MASTER_KEY_V2="key_2025_q2"

# Wrap new models with V2
# Old models with V1 still work (grace period)
```

### Emergency Rotation
Rotate immediately if:
- 🚨 Key leaked (committed to Git)
- 🚨 Employee with access leaves
- 🚨 Security breach

---

## Testing

```bash
# Verify key is set
echo $AVA_AON_MASTER_KEY

# Build with new key
./gradlew assembleRelease

# Verify key is NOT in APK source
apktool d app-release.apk
grep -r "AVA-AON" app-release/

# Should NOT find the literal key string
```

---

## Acceptance Criteria

- [ ] Master key stored in environment variable
- [ ] BuildConfig injects key at build time
- [ ] No hardcoded key in source code
- [ ] ProGuard obfuscation enabled
- [ ] CI/CD secrets configured
- [ ] Documentation updated
- [ ] Team trained on key management

---

## References

- AON File Format Spec: `docs/AON-FILE-FORMAT.md`
- AONFileManager: `Universal/AVA/Features/RAG/src/androidMain/.../AONFileManager.kt`
- Security Best Practices: `docs/AON-FILE-FORMAT.md#security-best-practices`
