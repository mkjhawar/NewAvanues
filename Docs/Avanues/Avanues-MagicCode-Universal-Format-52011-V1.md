# MagicCode - Universal Format v2.0 Implementation

**Last Updated:** 2025-11-20
**Status:** Active
**Format Version:** 2.0

---

## Overview

MagicCode is the voice-activated code generation engine for the Avanues ecosystem. It uses **Universal Format v2.0** (.amc files) to define all 50+ code templates for multi-platform development (Android, iOS, Web, KMP).

### Key Features

1. **Voice-Activated Code Generation:** Generate code using natural voice commands
2. **Multi-Platform Support:** Android (Kotlin), iOS (Swift), Web (React/Vue/Angular), KMP
3. **Universal Format:** Same file format as all Avanues projects
4. **Template Library:** 50+ pre-defined code templates
5. **Pattern Integration:** Common design patterns and best practices

---

## Architecture

### Code Generation Flow

```
User Voice Input
      ↓
Speech Recognition (STT)
      ↓
AVA NLU Engine
      ↓
Code Generation Intent
      ↓
MagicCode Template Selector
      ↓
Template Generator
      ↓
Generated Code
      ↓
File System / Clipboard
```

### File Structure

```
magiccode-templates/
├── magiccode-templates.amc  # All 50 code templates
└── README.md                # Format documentation
```

---

## Universal Format v2.0 Structure

### File Header

```
# Avanues Universal Format v1.0
# Type: AMC - MagicCode Template Definitions
# Extension: .amc
# Project: MagicCode (Code Generation Templates)
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: magiccode
metadata:
  file: magiccode-templates.amc
  category: code-templates
  name: MagicCode Template Library
  description: Code generation templates for multi-platform development
  priority: 1
  count: 50
---
```

### Template Entries

Format: `CODE:template_id:description`

```
# ANDROID KOTLIN TEMPLATES
ACT:activity:Android Activity
FRG:fragment:Android Fragment
VML:viewmodel:ViewModel class
REP:repository:Repository class
DAO:dao:Room DAO interface
ENT:entity:Room Entity
SRV:service:Android Service
RCV:receiver:BroadcastReceiver
WRK:worker:WorkManager Worker
CMP:composable:Jetpack Compose component

# IOS SWIFT TEMPLATES
VWC:viewcontroller:iOS ViewController
SWV:swiftui_view:SwiftUI View
MOD:model:Swift Model
SVC:service:Swift Service
MGR:manager:Manager class

# WEB JAVASCRIPT/TYPESCRIPT TEMPLATES
RCT:react_component:React Component
VUE:vue_component:Vue Component
ANG:angular_component:Angular Component
STO:store:State Store (Redux/Vuex)
```

### Global Synonyms

```
---
synonyms:
  template: [blueprint, scaffold, boilerplate]
  class: [type, object, entity]
  function: [method, procedure, routine]
  test: [spec, suite, case]
  api: [endpoint, route, service]
  database: [db, table, schema]
```

---

## Template Categories (50 Templates)

### 1. Android Kotlin Templates (10)

| Code | Template | Purpose | Voice Command Example |
|------|----------|---------|----------------------|
| ACT | Activity | Android Activity | "Generate an activity for settings" |
| FRG | Fragment | Android Fragment | "Create a fragment for user list" |
| VML | ViewModel | ViewModel class | "Generate a viewmodel for profile" |
| REP | Repository | Repository class | "Create a repository for users" |
| DAO | DAO | Room DAO interface | "Generate a DAO for messages" |
| ENT | Entity | Room Entity | "Create an entity for tasks" |
| SRV | Service | Android Service | "Generate a service for downloads" |
| RCV | Receiver | BroadcastReceiver | "Create a receiver for network changes" |
| WRK | Worker | WorkManager Worker | "Generate a worker for sync" |
| CMP | Composable | Jetpack Compose | "Create a composable for login form" |

### 2. iOS Swift Templates (7)

| Code | Template | Purpose | Voice Command Example |
|------|----------|---------|----------------------|
| VWC | ViewController | iOS ViewController | "Generate a viewcontroller for settings" |
| SWV | SwiftUI View | SwiftUI View | "Create a SwiftUI view for profile" |
| MOD | Model | Swift Model | "Generate a model for user" |
| SVC | Service | Swift Service | "Create a service for authentication" |
| MGR | Manager | Manager class | "Generate a manager for networking" |
| EXT | Extension | Swift Extension | "Create an extension for String" |
| PRO | Protocol | Swift Protocol | "Generate a protocol for datasource" |

### 3. Web JavaScript/TypeScript Templates (9)

| Code | Template | Purpose | Voice Command Example |
|------|----------|---------|----------------------|
| RCT | React Component | React Component | "Generate a React component for header" |
| VUE | Vue Component | Vue Component | "Create a Vue component for sidebar" |
| ANG | Angular Component | Angular Component | "Generate an Angular component for footer" |
| STO | State Store | Redux/Vuex Store | "Create a store for user state" |
| ACN | Action | Redux Action | "Generate an action for login" |
| RED | Reducer | Redux Reducer | "Create a reducer for cart" |
| MID | Middleware | Middleware | "Generate middleware for logging" |
| HKS | Custom Hooks | React Hooks | "Create a hook for authentication" |
| CTX | Context | React Context | "Generate a context for theme" |

### 4. Kotlin Multiplatform Templates (4)

| Code | Template | Purpose | Voice Command Example |
|------|----------|---------|----------------------|
| KMP | KMP Module | KMP Module | "Generate a KMP module for networking" |
| EXP | Expect Class | Expect Class | "Create an expect class for platform storage" |
| ACT | Actual Class | Actual Implementation | "Generate actual implementation for Android" |
| SHR | Shared Interface | Shared Interface | "Create a shared interface for logger" |

### 5. Database Templates (5)

| Code | Template | Purpose | Voice Command Example |
|------|----------|---------|----------------------|
| TBL | Table Schema | Database Table | "Generate a table schema for users" |
| MIG | Migration | Database Migration | "Create a migration to add email column" |
| QRY | SQL Query | SQL Query | "Generate a query to find active users" |
| IDX | Index | Database Index | "Create an index on email column" |
| TRG | Trigger | Database Trigger | "Generate a trigger for updated_at" |

### 6. API Templates (7)

| Code | Template | Purpose | Voice Command Example |
|------|----------|---------|----------------------|
| RTE | Route | API Route/Endpoint | "Generate a route for user profile" |
| CTR | Controller | API Controller | "Create a controller for authentication" |
| MDL | Middleware | API Middleware | "Generate middleware for authentication" |
| VAL | Validator | Request Validator | "Create a validator for signup request" |
| RES | Response | API Response Model | "Generate a response model for user" |
| REQ | Request | API Request Model | "Create a request model for login" |
| DTR | DTO | Data Transfer Object | "Generate a DTO for user profile" |

### 7. Testing Templates (7)

| Code | Template | Purpose | Voice Command Example |
|------|----------|---------|----------------------|
| TST | Unit Test | Unit Test | "Generate a test for user repository" |
| ITS | Integration Test | Integration Test | "Create an integration test for API" |
| E2E | End-to-End Test | E2E Test | "Generate an e2e test for login flow" |
| MOK | Mock | Mock Class | "Create a mock for API service" |
| STB | Stub | Stub Implementation | "Generate a stub for user repository" |
| SPY | Spy | Spy Object | "Create a spy for analytics tracker" |
| FXT | Fixture | Test Fixture | "Generate fixtures for user data" |

### 8. Utility Templates (8)

| Code | Template | Purpose | Voice Command Example |
|------|----------|---------|----------------------|
| UTL | Utility Class | Utility Class | "Generate a utility for date formatting" |
| HLP | Helper Function | Helper Function | "Create a helper for email validation" |
| EXT | Extension | Extension Method | "Generate an extension for List" |
| DEC | Decorator | Decorator Pattern | "Create a decorator for logger" |
| FAC | Factory | Factory Pattern | "Generate a factory for viewmodels" |
| SNG | Singleton | Singleton Pattern | "Create a singleton for app config" |
| OBS | Observer | Observer Pattern | "Generate an observer for data changes" |
| ADT | Adapter | Adapter Pattern | "Create an adapter for legacy API" |

---

## Implementation

### Kotlin Template Generator (Example)

```kotlin
package com.augmentalis.magiccode.generator

import com.augmentalis.avamagic.ipc.UniversalFileParser
import com.augmentalis.avamagic.ipc.FileType

class MagicCodeGenerator {

    private val templates: Map<String, CodeTemplate>

    init {
        // Load templates from .amc file
        val amcContent = loadAssetFile("magiccode-templates/magiccode-templates.amc")
        val parsed = UniversalFileParser.parse(amcContent, FileType.AMC)
        templates = parseTemplates(parsed)
    }

    fun generateCode(
        templateCode: String,
        name: String,
        params: Map<String, String> = emptyMap()
    ): GeneratedCode {
        val template = templates[templateCode]
            ?: error("Unknown template: $templateCode")

        return when (templateCode) {
            "ACT" -> generateActivity(name, params)
            "FRG" -> generateFragment(name, params)
            "VML" -> generateViewModel(name, params)
            "CMP" -> generateComposable(name, params)
            "RCT" -> generateReactComponent(name, params)
            "TST" -> generateTest(name, params)
            else -> error("Template not implemented: $templateCode")
        }
    }

    private fun generateActivity(name: String, params: Map<String, String>): GeneratedCode {
        val className = "${name}Activity"
        val packageName = params["package"] ?: "com.example.app"
        val layoutName = "activity_${name.lowercase()}"

        val code = """
package $packageName

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class $className : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.$layoutName)

        setupUI()
    }

    private fun setupUI() {
        // Initialize views
    }
}
""".trimIndent()

        return GeneratedCode(
            fileName = "$className.kt",
            code = code,
            language = "kotlin",
            platform = "android"
        )
    }

    private fun generateComposable(name: String, params: Map<String, String>): GeneratedCode {
        val functionName = name.replaceFirstChar { it.uppercase() }
        val packageName = params["package"] ?: "com.example.app"

        val code = """
package $packageName

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun $functionName(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "$name Screen")
        // Add your UI components here
    }
}

@Preview(showBackground = true)
@Composable
fun ${functionName}Preview() {
    MaterialTheme {
        $functionName()
    }
}
""".trimIndent()

        return GeneratedCode(
            fileName = "$functionName.kt",
            code = code,
            language = "kotlin",
            platform = "android-compose"
        )
    }

    private fun generateTest(name: String, params: Map<String, String>): GeneratedCode {
        val className = "${name}Test"
        val targetClass = name
        val packageName = params["package"] ?: "com.example.app"

        val code = """
package $packageName

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class $className {

    @Mock
    private lateinit var mockDependency: SomeDependency

    private lateinit var $targetClass: $name

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        $targetClass = $name(mockDependency)
    }

    @Test
    fun `test basic functionality`() {
        // Given

        // When

        // Then
        assertTrue(true)
    }
}
""".trimIndent()

        return GeneratedCode(
            fileName = "$className.kt",
            code = code,
            language = "kotlin",
            platform = "test"
        )
    }
}

data class GeneratedCode(
    val fileName: String,
    val code: String,
    val language: String,
    val platform: String
)
```

---

## Voice Command Examples

### Android Development

```
User: "Generate an activity for user profile"
→ Template: ACT:user_profile
→ Generated: UserProfileActivity.kt

User: "Create a viewmodel for settings"
→ Template: VML:settings
→ Generated: SettingsViewModel.kt

User: "Generate a composable for login form"
→ Template: CMP:login_form
→ Generated: LoginForm.kt
```

### iOS Development

```
User: "Generate a SwiftUI view for dashboard"
→ Template: SWV:dashboard
→ Generated: DashboardView.swift

User: "Create a model for user"
→ Template: MOD:user
→ Generated: User.swift
```

### Web Development

```
User: "Generate a React component for navbar"
→ Template: RCT:navbar
→ Generated: Navbar.tsx

User: "Create a custom hook for authentication"
→ Template: HKS:authentication
→ Generated: useAuthentication.ts
```

### Testing

```
User: "Generate a test for user repository"
→ Template: TST:user_repository
→ Generated: UserRepositoryTest.kt

User: "Create a mock for API service"
→ Template: MOK:api_service
→ Generated: MockApiService.kt
```

---

## Integration with AVA

### Voice-Activated Code Generation

```kotlin
// User says: "Generate an activity for settings"

// AVA Intent: generate_code
val intent = AvaIntent(
    id = "generate_code",
    action = "CODE_GENERATION",
    params = mapOf(
        "template" = "activity",
        "name" = "settings"
    )
)

// MagicCode processes intent
val generator = MagicCodeGenerator()
val generatedCode = generator.generateCode(
    templateCode = "ACT",
    name = "Settings",
    params = mapOf("package" = "com.example.app")
)

// Code written to file
File("${projectPath}/SettingsActivity.kt").writeText(generatedCode.code)

// Confirmation to user
AVA.speak("Settings activity generated successfully")
```

---

## Testing

### Unit Tests

```kotlin
@Test
fun `parse AMC file successfully`() {
    val content = loadTestResource("magiccode-templates.amc")
    val parsed = UniversalFileParser.parse(content, FileType.AMC)

    assertEquals("avu-1.0", parsed.schema)
    assertEquals("magiccode", parsed.project)
    assertEquals(50, parsed.metadata["count"])
}

@Test
fun `generate activity code`() {
    val generator = MagicCodeGenerator()
    val code = generator.generateCode("ACT", "Settings")

    assertTrue(code.code.contains("class SettingsActivity"))
    assertTrue(code.code.contains("AppCompatActivity"))
    assertEquals("SettingsActivity.kt", code.fileName)
}

@Test
fun `generate composable code`() {
    val generator = MagicCodeGenerator()
    val code = generator.generateCode("CMP", "Profile")

    assertTrue(code.code.contains("@Composable"))
    assertTrue(code.code.contains("fun Profile"))
    assertEquals("Profile.kt", code.fileName)
}
```

---

## References

- **Universal Format Spec:** `/Volumes/M-Drive/Coding/Avanues/docs/specifications/UNIVERSAL-FILE-FORMAT-FINAL.md`
- **Master Guide:** `/Volumes/M-Drive/Coding/Avanues/docs/Universal-Format-v2.0-Master-Guide.md`
- **UniversalFileParser:** `/Volumes/M-Drive/Coding/Avanues/modules/AVAMagic/IPC/UniversalIPC/`
- **Templates File:** `/Volumes/M-Drive/Coding/Avanues/magiccode-templates/magiccode-templates.amc`

---

**Status:** ✅ Production Ready
**Format:** Universal v2.0 (.amc)
**Total Templates:** 50
**Categories:** 8
**Platforms:** Android (Kotlin), iOS (Swift), Web (React/Vue/Angular), KMP
