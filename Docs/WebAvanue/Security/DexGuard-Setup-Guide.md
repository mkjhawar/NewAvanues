# DexGuard Setup Guide for WebAvanue

**Purpose:** Protect JavaScript bridge code and sensitive logic from reverse engineering.

**Date:** 2026-01-10
**Status:** Planning (not yet implemented)

---

## What is DexGuard?

DexGuard is a commercial Android app protection tool by Guardsquare that provides:
- Code obfuscation (stronger than ProGuard)
- String encryption
- Class encryption
- Native library protection
- Tamper detection
- Root detection
- Debug detection

**License:** Commercial ($$$) - Contact Guardsquare for pricing.

---

## Installation Steps

### 1. Obtain License

Contact Guardsquare: https://www.guardsquare.com/dexguard

You will receive:
- `dexguard-license.txt`
- DexGuard SDK (zip/aar)

### 2. Add DexGuard to Project

**settings.gradle.kts:**
```kotlin
pluginManagement {
    repositories {
        maven {
            url = uri("path/to/dexguard/lib")
        }
    }
}
```

**build.gradle.kts (app module):**
```kotlin
plugins {
    id("com.guardsquare.dexguard") version "9.x.x"
}

dexguard {
    license = file("dexguard-license.txt")
    configuration = file("dexguard-rules.pro")
}
```

### 3. Create DexGuard Configuration

**dexguard-rules.pro:**
```proguard
# ============================================
# WebAvanue JavaScript Protection
# ============================================

# Encrypt all strings in the VoiceOS bridge classes
-encryptstrings class com.augmentalis.webavanue.voiceos.** {
    *;
}

# Encrypt the DOMScraperBridge specifically
-encryptstrings class com.augmentalis.webavanue.voiceos.DOMScraperBridge {
    public static final java.lang.String SCRAPER_SCRIPT;
}

# Encrypt VoiceCommandGenerator
-encryptstrings class com.augmentalis.webavanue.voiceos.VoiceCommandGenerator {
    *;
}

# ============================================
# Class Encryption (Additional Protection)
# ============================================

# Encrypt entire classes for maximum protection
-encryptclasses com.augmentalis.webavanue.voiceos.DOMScraperBridge
-encryptclasses com.augmentalis.webavanue.voiceos.SecureScriptLoader

# ============================================
# Code Obfuscation
# ============================================

# Aggressive obfuscation for security classes
-obfuscatecode class com.augmentalis.webavanue.voiceos.** {
    *;
}

# Control flow obfuscation
-addcontrolflowobfuscation class com.augmentalis.webavanue.voiceos.** {
    *;
}

# ============================================
# Anti-Tampering
# ============================================

# Detect if app has been modified
-detecttampering

# Detect if running on rooted device
-detectroot

# Detect if debugger is attached
-detectdebugger

# ============================================
# Keep Rules (Don't Obfuscate)
# ============================================

# Keep JavaScript interface methods (called from JS)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep data classes for serialization
-keep class com.augmentalis.webavanue.voiceos.DOMElement { *; }
-keep class com.augmentalis.webavanue.voiceos.DOMScrapeResult { *; }
-keep class com.augmentalis.webavanue.voiceos.ElementBounds { *; }
```

---

## String Encryption Details

### How It Works

1. **Build Time:** DexGuard encrypts string literals with AES
2. **Runtime:** Strings are decrypted only when accessed
3. **Memory:** Decrypted strings can be cleared from memory after use

### What Gets Protected

| Before DexGuard | After DexGuard |
|-----------------|----------------|
| `const val SCRAPER_SCRIPT = "(function()..."` | Encrypted bytes + decryption stub |
| Visible in APK with jadx/apktool | Not directly readable |

### Limitations

- Strings exist in memory when used (memory dump still possible)
- Determined attacker with debugger can still capture
- Not a silver bullet, but raises the bar significantly

---

## Build Variants

**build.gradle.kts:**
```kotlin
android {
    buildTypes {
        debug {
            // No DexGuard for debug builds (faster builds)
            isMinifyEnabled = false
        }
        release {
            // Full DexGuard protection
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "dexguard-rules.pro"
            )
        }
    }
}
```

---

## Testing DexGuard Protection

### Verify Strings Are Encrypted

```bash
# Decompile APK
apktool d app-release.apk -o decompiled

# Search for JavaScript (should NOT find plain text)
grep -r "function()" decompiled/

# Use jadx to view classes
jadx app-release.apk
# DOMScraperBridge should show encrypted/obfuscated code
```

### Verify Class Encryption

```bash
# Classes should not be readable in jadx
# Should see decryption stubs instead of actual code
```

---

## Cost Estimate

| License Type | Approximate Cost | Notes |
|--------------|------------------|-------|
| Starter | $2,000-5,000/year | Basic protection |
| Professional | $10,000-20,000/year | Full features |
| Enterprise | Custom pricing | Multi-app, support |

**Alternative:** ProGuard (free) provides basic obfuscation but NO string encryption.

---

## Current Implementation (Without DexGuard)

Until DexGuard is purchased, we use **Option 4: Dynamic JS Generation**:
- JavaScript is split into encrypted fragments
- Assembled at runtime
- Not stored as single readable string

See: `SecureScriptLoader.kt`

---

## Migration Path

1. **Now:** Dynamic JS Generation (Option 4)
2. **Short-term:** Add JS minification/obfuscation
3. **Long-term:** Purchase DexGuard for full protection

---

## References

- DexGuard Documentation: https://www.guardsquare.com/manual/dexguard
- ProGuard (free alternative): https://www.guardsquare.com/proguard
- Android Security Best Practices: https://developer.android.com/topic/security/best-practices

---

**Author:** Claude Code
**Version:** 1.0
