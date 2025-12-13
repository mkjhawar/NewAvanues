# VoiceOS JDK Configuration

**Project:** VoiceOS
**Document ID:** VoiceOS-JDK-Configuration-51213-V1
**Created:** 2025-12-13
**Status:** Active

---

## Required JDK Version

**JDK 17** (Java SE 17 LTS)

---

## Why JDK 17?

- **KMP Compatibility**: Kotlin Multiplatform requires JDK 17 for optimal compatibility
- **Android Build**: Android builds target JVM 17 (see `jvmTarget = "17"` in build.gradle.kts)
- **Gradle 8.5**: Compatible with JDK 17-21, optimal on 17
- **Long-Term Support**: JDK 17 is an LTS release (supported until 2029)

---

## Setting JAVA_HOME

### For This Terminal Session

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
```

### Permanent Setup (Shell Configuration)

Add to `~/.zshrc` or `~/.bash_profile`:

```bash
# NewAvanues-VoiceOS requires JDK 17
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

### Using jEnv (Recommended for Multi-Project Setups)

```bash
# Install jenv
brew install jenv

# Add JDK 17
jenv add /Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home

# Set local JDK for this project
cd /Volumes/M-Drive/Coding/NewAvanues-VoiceOS
jenv local 17
```

---

## Verification

```bash
# Check Java version
java -version
# Expected: java version "17.0.x"

# Check JAVA_HOME
echo $JAVA_HOME
# Expected: /Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home

# Test Gradle build
./gradlew :Modules:VoiceOS:core:database:compileCommonMainKotlinMetadata
# Expected: BUILD SUCCESSFUL (no JDK warnings)
```

---

## Available JDKs on This System

| Version | Path | Use Case |
|---------|------|----------|
| 17.0.13 | `/Library/Java/JavaVirtualMachines/jdk-17.jdk/` | **NewAvanues (PRIMARY)** |
| 21.0.6 | `/Library/Java/JavaVirtualMachines/jdk-21.jdk/` | Other projects |
| 24 | `/Library/Java/JavaVirtualMachines/jdk-24.jdk/` | Latest features (not for NewAvanues) |

---

## Project-Specific Files

| File | Purpose | Status |
|------|---------|--------|
| `.java-version` | jEnv/asdf version marker | ✅ Created (contains "17") |
| `gradle.properties` | Gradle JVM settings | ✅ Configured (uses JDK 17) |
| All `build.gradle.kts` | `jvmTarget = "17"` | ✅ Consistent across project |

---

## Troubleshooting

### Error: "Kotlin does not yet support 24 JDK target"

**Cause:** System default JDK is 24, but project needs 17
**Fix:** Set `JAVA_HOME` to JDK 17 (see above)

### Error: "Unresolved reference: Volatile"

**Cause:** Using JVM-specific APIs in KMP common code
**Fix:** Already fixed (replaced with KMP-compatible alternatives)
**See:** VoiceOS-Plan-P1-Fixes-51213-V1.md

---

## Related Files

- `/Volumes/M-Drive/Coding/NewAvanues-VoiceOS/Modules/VoiceOS/core/database/build.gradle.kts` - JVM target configuration
- `/Volumes/M-Drive/Coding/NewAvanues-VoiceOS/gradle.properties` - Gradle JVM settings

---

**Author:** Claude Code
**Last Updated:** 2025-12-13
