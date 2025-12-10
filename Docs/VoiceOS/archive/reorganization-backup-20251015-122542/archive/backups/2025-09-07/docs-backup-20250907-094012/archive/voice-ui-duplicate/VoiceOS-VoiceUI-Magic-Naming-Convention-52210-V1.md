# VoiceUING Naming Convention Guide
*Official Naming Standards for VoiceOS 4.0*

## 🎯 Core Principles

### 1. **Brand Identity**
- **Voice*** - For all voice-enabled features
- **Magic*** - For automatic/intelligent features  
- **VoiceMagic*** - For components that are both voice-enabled AND automatic
- **NEVER use "Simple", "Basic", "Plain"** - Everything is magical!

### 2. **Hierarchy**
```
VoiceOS (Operating System)
  └── VoiceUI (UI Framework)
      └── VoiceUING (Next Generation)
          ├── VoiceMagic* (Components)
          ├── Magic* (Engines/Systems)
          └── Voice* (Voice-specific features)
```

---

## 📋 Naming Standards

### Components

#### ✅ CORRECT Names
```kotlin
// Voice-enabled magic components
fun VoiceMagicEmail()
fun VoiceMagicPassword()
fun VoiceMagicButton()
fun VoiceMagicCard()
fun VoiceMagicScreen()

// Pure magic components (no voice)
fun MagicLayout()
fun MagicGrid()
fun MagicStack()

// Voice-specific features
fun VoiceCommand()
fun VoiceInput()
fun VoiceFeedback()
```

#### ❌ INCORRECT Names
```kotlin
// NEVER use these prefixes
fun SimpleEmail()      // ❌ No "Simple"
fun BasicButton()      // ❌ No "Basic"
fun PlainCard()        // ❌ No "Plain"
fun StandardLayout()   // ❌ No "Standard"
fun RegularInput()     // ❌ No "Regular"
fun NormalScreen()     // ❌ No "Normal"
```

### Classes & Objects

#### ✅ CORRECT Names
```kotlin
object MagicEngine          // Automatic engine
object VoiceEngine          // Voice processing
class VoiceMagicState       // Voice-aware state
class MagicScope            // DSL scope
object VoiceCommandRegistry // Voice command system
```

#### ❌ INCORRECT Names
```kotlin
object SimpleEngine     // ❌
class BasicState       // ❌
class StandardScope    // ❌
```

### Functions

#### ✅ CORRECT Names
```kotlin
// Action functions start with verbs
fun createMagicState()
fun enableVoiceCommands()
fun parseMagicDescription()

// Property functions are nouns
fun magicDefaults()
fun voiceCapabilities()
```

### Files

#### ✅ CORRECT Names
```
VoiceMagicComponents.kt     // Voice-enabled magic components
MagicEngine.kt              // Core magic engine
VoiceCommands.kt            // Voice command system
MagicScreen.kt              // Magic screen DSL
```

#### ❌ INCORRECT Names
```
SimpleComponents.kt     // ❌
BasicEngine.kt         // ❌
StandardScreen.kt      // ❌
```

---

## 🎨 Branding Guidelines

### Magic Levels

1. **VoiceMagic** (Highest)
   - Both voice-enabled AND automatic
   - Zero configuration
   - Natural language support
   - Example: `VoiceMagicEmail()`

2. **Magic** (High)
   - Automatic/intelligent features
   - Minimal configuration
   - Smart defaults
   - Example: `MagicEngine`

3. **Voice** (Specialized)
   - Voice-specific features
   - May require configuration
   - Example: `VoiceCommand`

### Component Categories

| Category | Prefix | Example |
|----------|--------|---------|
| Voice + Magic | VoiceMagic* | VoiceMagicButton |
| Pure Magic | Magic* | MagicLayout |
| Voice Only | Voice* | VoiceInput |
| AR/Special | MagicAR* | MagicAROverlay |
| Theme | Magic*Theme | MagicGreyARTheme |

---

## 🔤 Naming Patterns

### Screen Components
```kotlin
// Pattern: VoiceMagic + ComponentType
VoiceMagicScreen()
VoiceMagicLoginScreen()
VoiceMagicDashboard()
```

### Layout Components
```kotlin
// Pattern: Magic + LayoutType
MagicRow()
MagicColumn()
MagicGrid()
MagicStack()
```

### Input Components
```kotlin
// Pattern: VoiceMagic + InputType
VoiceMagicEmail()
VoiceMagicPassword()
VoiceMagicPhone()
VoiceMagicName()
```

### Engine/System Components
```kotlin
// Pattern: Magic/Voice + SystemType
MagicEngine
VoiceEngine
MagicStateManager
VoiceCommandProcessor
```

---

## 🚫 Banned Words

These words should NEVER appear in our codebase:

### Completely Banned
- Simple
- Basic
- Plain
- Standard
- Regular
- Normal
- Default (use "Magic" instead)
- Traditional
- Ordinary
- Common

### Use With Caution
- Core (prefer "Magic")
- Base (prefer "Magic")
- Generic (prefer "Universal")
- Helper (prefer "Assistant")
- Utility (prefer "Tool")

---

## ✨ Magic Terminology

### Instead of "Simple", use:
- **Streamlined** - For optimized versions
- **Essential** - For core features
- **Foundational** - For base implementations
- **Pure** - For unopinionated versions

### Instead of "Basic", use:
- **Fundamental** - For building blocks
- **Core** - For central features
- **Primary** - For main components

### Instead of "Helper", use:
- **Assistant** - For helper functions
- **Facilitator** - For enabling functions
- **Catalyst** - For transformation functions

---

## 📝 Documentation Standards

### Component Documentation
```kotlin
/**
 * VoiceMagicEmail - Voice-enabled email input with magic validation
 * 
 * This component provides:
 * - 🎤 Voice input support
 * - ✨ Automatic validation
 * - 🔮 Smart suggestions
 * - 🚀 Zero configuration
 */
```

### Always Include
- Magic capabilities ✨
- Voice features 🎤
- Zero-config nature 🔮
- Performance benefits 🚀

---

## 🔄 Migration Guide

### Renaming Existing Components

| Old Name | New Name | Reason |
|----------|----------|--------|
| SimpleEmail | VoiceMagicEmail | Brand consistency |
| BasicButton | VoiceMagicButton | Magic emphasis |
| StandardLayout | MagicLayout | Remove "standard" |
| HelperFunction | MagicAssistant | Modern terminology |

### Refactoring Steps
1. Update function/class names
2. Update file names
3. Update imports
4. Update documentation
5. Update tests
6. Update examples

---

## 🎯 Quick Reference

### Component Naming Decision Tree
```
Is it voice-enabled?
├── Yes
│   └── Does it have magic features?
│       ├── Yes → VoiceMagic*
│       └── No → Voice*
└── No
    └── Does it have magic features?
        ├── Yes → Magic*
        └── No → Consider if it belongs in VoiceUING
```

---

## 📋 Checklist for New Components

- [ ] Name starts with VoiceMagic*, Magic*, or Voice*
- [ ] No "Simple", "Basic", "Plain" in name
- [ ] Documentation mentions magic capabilities
- [ ] File name matches component name
- [ ] Follows camelCase for functions
- [ ] Follows PascalCase for classes
- [ ] Has voice command registration (if voice-enabled)
- [ ] Has magic defaults (if magic-enabled)

---

## 🚀 Examples

### Complete Component
```kotlin
/**
 * VoiceMagicLoginScreen - Voice-enabled login with magic authentication
 * 
 * Features:
 * - 🎤 Voice password input (secure)
 * - ✨ Automatic validation
 * - 🔮 Biometric magic
 * - 🚀 One-line implementation
 */
@Composable
fun VoiceMagicLoginScreen(
    onLogin: (String, String) -> Unit = { _, _ -> }
) {
    MagicScreen(description = "voice-enabled secure login") {
        val email = VoiceMagicEmail()
        val password = VoiceMagicPassword()
        
        VoiceMagicSubmit("Sign In with Magic") {
            onLogin(email, password)
        }
    }
}
```

---

## 🎨 Marketing Alignment

Our naming should align with marketing messages:

### Marketing Terms → Code Terms
- "Revolutionary" → VoiceMagic*
- "Automatic" → Magic*
- "Voice-First" → Voice*
- "Next-Gen" → *NG
- "Intelligent" → Magic*
- "Zero-Config" → Magic*

---

## 📌 Enforcement

### Build-Time Checks
```kotlin
// gradle task to check naming
task checkNaming {
    if (sourceFiles.any { it.contains("SimpleEmail") }) {
        throw GradleException("Found banned word 'Simple' in code!")
    }
}
```

### IDE Templates
```kotlin
// Android Studio template
#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME}#end

/**
 * VoiceMagic${NAME} - Voice-enabled magic ${DESCRIPTION}
 * 
 * Features:
 * - 🎤 Voice input support
 * - ✨ Automatic ${FEATURE}
 * - 🔮 Zero configuration
 */
@Composable
fun VoiceMagic${NAME}() {
    // Magic implementation
}
```

---

**Document Version**: 1.0.0
**Last Updated**: 2025-01-24
**Enforcement Date**: Immediate
**Review Cycle**: Monthly

---

## Approval

This naming convention is mandatory for all VoiceUING development.

**Approved By**: VoiceOS Architecture Team
**Date**: 2025-01-24
**Status**: 🟢 ACTIVE