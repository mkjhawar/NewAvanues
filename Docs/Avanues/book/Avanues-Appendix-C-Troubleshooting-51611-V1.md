# Appendix C: Troubleshooting Guide

**Version:** 5.3.0
**Date:** 2025-11-02
**Author:** Manoj Jhawar, manoj@ideahq.net

---

## Common Issues

### Build Errors

#### Issue: "Component type not found"
**Cause:** Component not registered in ComponentRegistry

**Solution:**
```kotlin
// Ensure component is registered
registry.register(ComponentDescriptor(
    type = "CustomComponent",
    properties = mapOf(...)
))
```

#### Issue: "JAVA_HOME not set"
**Solution:**
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
```

---

### Runtime Errors

#### Issue: "InstantiationException: Unknown component type"
**Cause:** Component not in registry or typo in JSON

**Solution:**
1. Check component type spelling
2. Verify component is registered
3. Check logs: `adb logcat | grep AvaUI`

#### Issue: "EventBus buffer overflow"
**Cause:** Too many events, slow collectors

**Solution:**
```kotlin
// Increase buffer
private val _events = MutableSharedFlow<ComponentEvent>(
    replay = 0,
    extraBufferCapacity = 500 // Increased from 100
)
```

---

### Voice Command Issues

#### Issue: "Voice command not matching"
**Cause:** Confidence threshold too high

**Solution:**
```kotlin
// Lower threshold
val match = router.match(voiceInput)
if (match != null && match.confidence > 0.6f) { // Lowered from 0.7
    // Handle
}
```

#### Issue: "VoiceOSBridge not found"
**Cause:** VoiceOSBridge not implemented (known issue)

**Status:** ⚠️ VoiceOSBridge is EMPTY - implementation pending

---

### Platform-Specific Issues

#### Android

**Issue: "ClassNotFoundException: androidx.compose"**
**Solution:**
```gradle
dependencies {
    implementation "androidx.compose.ui:ui:1.5.0"
    implementation "androidx.compose.material3:material3:1.1.0"
}
```

**Issue: "Manifest merger failed"**
**Solution:**
```xml
<application tools:replace="android:theme">
```

#### iOS

**Issue: "Kotlin/Native framework not found"**
**Solution:**
```bash
./gradlew :Universal:IDEAMagic:AvaUI:linkDebugFrameworkIosX64
```

**Issue: "C-interop bridging error"**
**Status:** ⚠️ Known issue - 27 TODOs in iOS bridge

#### Web

**Issue: "Module not found: '@mui/material'"**
**Solution:**
```bash
npm install @mui/material @emotion/react @emotion/styled
```

---

## Performance Issues

### Slow Code Generation
**Solution:**
```kotlin
// Cache generators
private val generatorCache = mutableMapOf<Platform, CodeGenerator>()

fun getGenerator(platform: Platform): CodeGenerator {
    return generatorCache.getOrPut(platform) {
        CodeGeneratorFactory.create(platform)
    }
}
```

### Memory Leaks
**Check:**
- Unsubscribe from event bus
- Release resources in lifecycle.onDestroy()
- Clear component references

---

## Debugging Tips

### Enable Verbose Logging
```kotlin
Logger.setLevel(LogLevel.DEBUG)
```

### Inspect AST
```kotlin
val screen = parser.parseScreen(json).getOrThrow()
println(screen.toDebugString())
```

### Test Components Individually
```kotlin
@Test
fun testButtonGeneration() {
    val component = ComponentNode(
        id = "btn1",
        type = ComponentType.BUTTON,
        properties = mapOf("text" to "Test")
    )
    val code = generator.generateComponent(component)
    assertTrue(code.contains("Button"))
}
```

---

## Getting Help

### Documentation
- README: `/docs/README.md`
- Architecture: Chapter 2
- API Reference: Appendix A

### Community
- GitHub Issues: https://github.com/augmentalis/avamagic/issues
- Discord: https://discord.gg/avamagic
- Email: support@ideahq.net

### Reporting Bugs
Include:
1. IDEAMagic version
2. Platform (Android/iOS/Web)
3. Minimal reproduction code
4. Error logs
5. Expected vs actual behavior

---

**Created by Manoj Jhawar, manoj@ideahq.net**
