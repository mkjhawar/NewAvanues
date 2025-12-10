# Developer Manual - Chapter 66: MagicUI & MagicCode DSL

## Overview

MagicUI and MagicCode are AVA's domain-specific languages (DSL) for generating UI components and code through natural language. This chapter covers the DSL syntax, the LLM integration, and how to extend these systems.

---

## MagicUI DSL

### Purpose

MagicUI enables users to describe UI components in natural language, which AVA converts to the MagicUI DSL format. The DSL is then rendered natively on each platform (Compose, SwiftUI, React).

### DSL Syntax

```
ComponentType#id{property:value;child1;child2}
```

### Example

```
// Natural language: "Create a settings toggle row"
// MagicUI output:
Row#settingRow{@pad:16;spacing:12;@align:center;
  Col#labels{
    Text#title{text:"{settingName}";size:16;color:textPrimary};
    Text#desc{text:"{description}";size:12;color:textSecondary}
  };
  Spacer{flex:1};
  Switch#toggle{checked:{isEnabled};onToggle:onSettingChanged}
}
```

---

## Component Reference

### Layout Components

| Component | Properties | Description |
|-----------|------------|-------------|
| Col | spacing, @pad, @align, @bg | Vertical stack |
| Row | spacing, @pad, @align | Horizontal stack |
| Card | @pad, @radius, @bg, elevation | Elevated container |
| Surface | @bg, @radius, border | Background layer |
| Scaffold | topBar, content, bottomBar | Screen scaffold |
| ScrollCol | spacing, @pad | Scrollable column |
| LazyCol | items, itemTemplate | Virtualized list |

### Content Components

| Component | Properties | Description |
|-----------|------------|-------------|
| Text | text, size, weight, color, maxLines | Text label |
| Img | src, @size, fit, @radius | Image |
| Icon | name, color, size | Material icon |
| Avatar | src, @size, fallback | User avatar |
| Badge | count, color | Count badge |
| Chip | label, icon, selected | Tag/chip |
| Divider | thickness, color | Separator |
| Spacer | height, width, flex | Empty space |

### Input Components

| Component | Properties | Description |
|-----------|------------|-------------|
| Btn | label, icon, color, variant, onClick | Button |
| IconBtn | icon, color, onClick | Icon button |
| FAB | icon, color, onClick, extended | Floating action |
| Input | hint, value, type, onChange, error | Text field |
| Switch | checked, onToggle | Toggle |
| Slider | value, min, max, onChange | Range slider |
| Checkbox | checked, label, onToggle | Checkbox |
| Dropdown | options, selected, onChange | Select menu |

### Navigation

| Component | Properties | Description |
|-----------|------------|-------------|
| TopBar | title, navIcon, actions | App bar |
| BottomNav | items, selected, onChange | Bottom nav |
| TabRow | tabs, selected, onChange | Tab bar |

---

## Shorthand Properties

| Shorthand | Expands To | Example |
|-----------|------------|---------|
| @pad | padding | @pad:16 |
| @m | margin | @m:8 |
| @align | alignment | @align:center |
| @size | width, height | @size:48 |
| @radius | cornerRadius | @radius:12 |
| @bg | backgroundColor | @bg:surface10 |

---

## Ocean Glass Theme

### Color Tokens

| Token | Hex | Usage |
|-------|-----|-------|
| oceanDeep | #0A1628 | Screen background |
| oceanMid | #1E3A5F | Card background |
| surface5 | white 5% | Subtle surface |
| surface10 | white 10% | Card surface |
| surface20 | white 20% | Elevated surface |
| border10 | white 10% | Subtle border |
| border20 | white 20% | Visible border |
| coralBlue | #3B82F6 | Primary/actions |
| seafoamGreen | #10B981 | Success |
| coralRed | #EF4444 | Error |
| sunsetOrange | #F59E0B | Warning |
| textPrimary | white 90% | Headings |
| textSecondary | white 70% | Body text |
| textTertiary | white 50% | Hints |

### Alignment Values

`start`, `center`, `end`, `spaceBetween`, `spaceAround`, `spaceEvenly`

---

## MagicCode DSL

### Purpose

MagicCode generates production-ready Kotlin/Swift code following AVA architecture patterns. Users describe what they need, and AVA generates complete, properly structured code.

### Output Format

```yaml
---
schema: avu-1.0
type: amc
project: magiccode
---
GEN:{generator_type}:{description}
PKG:{package_path}
---
{kotlin_code}
```

### Generator Types

| Type | Description | Output |
|------|-------------|--------|
| data_class | Data model | Data class with properties |
| viewmodel | ViewModel | MVVM ViewModel with StateFlow |
| repository | Repository | Interface + Implementation |
| usecase | Use case | Business logic interactor |
| screen | Compose screen | Full screen composable |
| component | UI component | Reusable composable |
| service | Android service | Background service |
| module | DI module | Koin module definition |

---

## Architecture Patterns

### Clean Architecture

```
com.augmentalis.ava.features.{feature}/
├── data/
│   ├── repository/     # Repository implementations
│   └── model/          # DTOs, entities
├── domain/
│   ├── model/          # Domain models
│   └── usecase/        # Business logic
└── presentation/
    ├── viewmodel/      # ViewModels
    └── ui/             # Composables
```

### Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Data Class | {Name}Entity, {Name}Dto | UserEntity |
| ViewModel | {Feature}ViewModel | SettingsViewModel |
| Repository | {Domain}Repository | UserRepository |
| UseCase | {Action}{Entity}UseCase | GetUserUseCase |
| Screen | {Name}Screen | SettingsScreen |
| State | {Feature}UiState | ChatUiState |

---

## LLM Integration

### Intent Routing

```kotlin
// MagicUI/MagicCode intents route to LlmActionService
when (intentId) {
    "create_ui_component" -> llmActionService.process(
        promptTemplate = "create_ui_component.avp",
        userInput = userInput
    )
    "generate_viewmodel" -> llmActionService.process(
        promptTemplate = "generate_code.avp",
        userInput = userInput
    )
}
```

### Prompt Template Loading

```kotlin
class PromptTemplateLoader(private val context: Context) {
    fun load(intentId: String, locale: String = "en-US"): PromptTemplate {
        val path = ".ava/prompts/$locale/$intentId.avp"
        val content = context.assets.open(path).bufferedReader().use { it.readText() }
        return parseAvpTemplate(content)
    }
}
```

### Response Parsing

```kotlin
fun parseAvuResponse(response: String): ActionResult {
    // Parse AVU-1.0 format
    val lines = response.split("\n")
    val metadata = mutableMapOf<String, String>()
    var content = ""

    var inContent = false
    for (line in lines) {
        when {
            line == "---" -> inContent = !inContent
            !inContent && line.contains(":") -> {
                val (key, value) = line.split(":", limit = 2)
                metadata[key.trim()] = value.trim()
            }
            inContent -> content += line + "\n"
        }
    }

    return ActionResult.Generated(
        type = metadata["type"] ?: "unknown",
        content = content.trim()
    )
}
```

---

## Example: Creating a Settings Screen

### User Input
```
"Create a settings screen with dark mode toggle and notification settings"
```

### LLM Processing

1. Intent classified as `create_ui_screen`
2. Prompt template `create_ui_screen.avp` loaded
3. LLM generates MagicUI DSL
4. DSL parsed and rendered

### Generated Output

```
---
schema: avu-1.0
type: ami
project: magicui
---
SCREEN:settings:Settings
DESC:Settings screen with toggles
---
Scaffold#settings{
  topBar:TopBar{title:"Settings";navIcon:arrow_back};
  content:ScrollCol{@pad:16;spacing:8;
    Card#appearance{@pad:0;@radius:12;@bg:surface10;
      Col{
        Row#darkMode{@pad:16;spacing:12;@align:center;
          Icon{name:dark_mode;color:coralBlue};
          Col{flex:1;
            Text{text:"Dark Mode";color:textPrimary};
            Text{text:"Use dark theme";size:12;color:textSecondary}
          };
          Switch{checked:{isDarkMode};onToggle:toggleDarkMode}
        };
        Divider{color:border10};
        Row#notifications{@pad:16;spacing:12;@align:center;
          Icon{name:notifications;color:coralBlue};
          Col{flex:1;
            Text{text:"Notifications";color:textPrimary};
            Text{text:"Push notifications";size:12;color:textSecondary}
          };
          Switch{checked:{notificationsEnabled};onToggle:toggleNotifications}
        }
      }
    }
  }
}
```

---

## Extending the System

### Adding New Components

1. Update `create_ui_component.avp` with new component
2. Add renderer in platform-specific code
3. Update DSL parser

### Adding New Code Generators

1. Add generator type to `generate_code.avp`
2. Create examples in prompt template
3. Update `MagicCodeParser` if needed

---

## Best Practices

| Practice | Description |
|----------|-------------|
| Use theme tokens | Always use Ocean Glass tokens, not raw colors |
| Include accessibility | contentDesc for interactive elements |
| Min touch target | 48dp for all buttons/switches |
| Use shorthand | @pad, @bg, @radius for cleaner DSL |
| Test on device | Verify generated UI renders correctly |

---

## Author

Manoj Jhawar
