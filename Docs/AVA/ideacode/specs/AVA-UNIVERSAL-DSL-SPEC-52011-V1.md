# Avanues Universal UI Component DSL Specification

**Version:** 2.0.0
**Status:** Stable
**Date:** 2025-11-20
**Author:** Manoj Jhawar (manoj@ideahq.net)

**Related Documents:**
- [Universal IPC Protocol Specification](UNIVERSAL-IPC-SPEC.md) - IPC protocol messages
- [IPC Research Summary](IPC-RESEARCH-SUMMARY.md) - Research and analysis of IPC implementations

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Design Goals](#2-design-goals)
3. [Syntax Overview](#3-syntax-overview)
4. [Grammar Definition](#4-grammar-definition)
5. [Type Aliases](#5-type-aliases)
6. [Property Names](#6-property-names)
7. [Modifiers](#7-modifiers)
8. [Values and Literals](#8-values-and-literals)
9. [Examples](#9-examples)
10. [Comparison with JSON](#10-comparison-with-json)
11. [IPC Integration](#11-ipc-integration)
12. [Implementation Notes](#12-implementation-notes)
13. [Appendix](#13-appendix)

---

## 1. Introduction

The Avanues Universal UI Component DSL is a domain-specific language for serializing UI component trees in the Avanues ecosystem. It provides a human-readable, efficient alternative to JSON for component serialization and cross-process UI transfer.

**Note:** This specification covers UI components only. For IPC protocol messages (video calls, chat, events, etc.), see [UNIVERSAL-IPC-SPEC.md](UNIVERSAL-IPC-SPEC.md).

### 1.1 Purpose

- **IPC Efficiency**: Reduce payload size for cross-process UI transfer
- **Human Readability**: Enable developers to read and debug serialized UI
- **Performance**: Fast parsing and serialization (<1ms typical)
- **Platform Agnostic**: Works on Android, iOS, Web, Desktop

### 1.2 Scope

This specification defines:
- Complete syntax and grammar
- Type alias mappings (60+ components)
- Property name conventions
- Modifier shortcuts
- Value encoding rules
- Serialization/deserialization behavior

---

## 2. Design Goals

### 2.1 Primary Goals

1. **Compact Size** - 40-73% smaller than equivalent JSON
2. **Readability** - Understandable by developers without tools
3. **Speed** - Sub-millisecond parsing for typical trees
4. **Completeness** - Express all UI component features

### 2.2 Non-Goals

- Replace JSON entirely (JSON still used for metadata)
- Support arbitrary data structures (UI components only)
- Human authoring (generated from component models)
- Runtime evaluation (static serialization)

---

## 3. Syntax Overview

### 3.1 Basic Structure

```
Component ::= TypeAlias ['#' ID] '{' Body '}'
Body ::= [Properties] [';' Modifiers] [';' Callbacks] [';' Children]
```

### 3.2 Simple Example

```
Col#main{spacing:16;@p(16);Text{text:"Hello"}}
```

**Breakdown:**
- `Col` - Type alias (Column)
- `#main` - Component ID
- `spacing:16` - Property
- `@p(16)` - Modifier (padding)
- `Text{text:"Hello"}` - Child component

### 3.3 Complete Example

```
Col#main{spacing:16;@p(16),bg(#FFFFFF);Text{text:"Hello World";fontSize:24};Btn#btn1{label:"Click Me";@onClick->handleClick}}
```

---

## 4. Grammar Definition

### 4.1 EBNF Grammar

```ebnf
(* Root *)
UITree ::= Metadata? Component+

(* Metadata (optional) *)
Metadata ::= '@avaui/' Version EOL
             ('id:' AppID EOL)?
             ('name:' AppName EOL)?
             ('theme:' ThemeName EOL)?
             EOL

(* Component *)
Component ::= TypeAlias ComponentID? '{' ComponentBody '}'

TypeAlias ::= [A-Z][a-zA-Z0-9]*
ComponentID ::= '#' Identifier
Identifier ::= [a-zA-Z_][a-zA-Z0-9_]*

(* Component Body *)
ComponentBody ::= PropertyList? (';' ModifierList)? (';' CallbackList)? (';' ChildList)?

(* Properties *)
PropertyList ::= Property (';' Property)*
Property ::= PropertyName ':' Value

PropertyName ::= [a-zA-Z][a-zA-Z0-9]*
Value ::= String | Number | Boolean | Color | Null

(* Modifiers *)
ModifierList ::= '@' Modifier (',' Modifier)*
Modifier ::= ModifierName '(' ArgList ')'

ModifierName ::= [a-z]+
ArgList ::= Value (',' Value)*

(* Callbacks *)
CallbackList ::= Callback (';' Callback)*
Callback ::= '@' EventName '->' HandlerName

EventName ::= [a-zA-Z][a-zA-Z0-9]*
HandlerName ::= [a-zA-Z_][a-zA-Z0-9_]*

(* Children *)
ChildList ::= Component (';' Component)*

(* Literals *)
String ::= '"' [^"]* '"'
Number ::= '-'? [0-9]+ ('.' [0-9]+)?
Boolean ::= 'true' | 'false'
Color ::= '#' [0-9A-Fa-f]{6} | '#' [0-9A-Fa-f]{8}
Null ::= 'null'

Version ::= [0-9]+ '.' [0-9]+
```

### 4.2 Lexical Rules

**Case Sensitivity:**
- Type aliases: PascalCase (e.g., `Col`, `Text`, `Btn`)
- Property names: camelCase (e.g., `text`, `fontSize`, `bgColor`)
- Modifier names: lowercase (e.g., `p`, `bg`, `r`)

**Separators:**
- Component parts: `;` (semicolon)
- Modifier arguments: `,` (comma)
- No whitespace required (but allowed)

**Reserved Characters:**
- `#` - Component ID prefix
- `{` `}` - Component delimiters
- `@` - Modifier/callback prefix
- `:` - Property assignment
- `;` - Part separator
- `->` - Callback arrow
- `()` - Modifier arguments
- `""` - String delimiters

---

## 5. Type Aliases

### 5.1 Complete Alias Mapping

#### Layout Components (14)

| Alias | Full Type | Description |
|-------|-----------|-------------|
| `Col` | Column | Vertical layout |
| `Row` | Row | Horizontal layout |
| `Box` | Box | Z-axis stack |
| `Stack` | Stack | Generic stack |
| `Cont` | Container | Container wrapper |
| `Card` | Card | Material card |
| `Surf` | Surface | Material surface |
| `Scaffold` | Scaffold | Page scaffold |
| `Scroll` | ScrollView | Scrollable container |
| `LazyCol` | LazyColumn | Lazy vertical list |
| `LazyRow` | LazyRow | Lazy horizontal list |
| `Grid` | Grid | Grid layout |
| `Spacer` | Spacer | Empty space |
| `Div` | Divider | Dividing line |

#### Basic Components (8)

| Alias | Full Type | Description |
|-------|-----------|-------------|
| `Text` | Text | Text display |
| `Btn` | Button | Action button |
| `Field` | TextField | Text input |
| `Img` | Image | Image display |
| `Icon` | Icon | Icon display |
| `Check` | Checkbox | Checkbox input |
| `Switch` | Switch | Toggle switch |
| `Radio` | Radio | Radio button |

#### Input Components (9)

| Alias | Full Type | Description |
|-------|-----------|-------------|
| `Slider` | Slider | Value slider |
| `Drop` | Dropdown | Dropdown select |
| `DatePick` | DatePicker | Date selector |
| `TimePick` | TimePicker | Time selector |
| `Search` | SearchBar | Search input |
| `Rating` | Rating | Star rating |
| `Stepper` | Stepper | Numeric stepper |
| `Toggle` | Toggle | Toggle button |
| `ColorPick` | ColorPicker | Color selector |

#### Display Components (9)

| Alias | Full Type | Description |
|-------|-----------|-------------|
| `Avatar` | Avatar | User avatar |
| `Badge` | Badge | Notification badge |
| `Chip` | Chip | Material chip |
| `Tip` | Tooltip | Tooltip popup |
| `Progress` | ProgressBar | Linear progress |
| `Spinner` | ProgressCircle | Circular progress |
| `Spin` | Spinner | Loading spinner |
| `Skel` | Skeleton | Skeleton loader |
| `Stat` | StatCard | Statistic card |

#### Navigation Components (7)

| Alias | Full Type | Description |
|-------|-----------|-------------|
| `AppBar` | AppBar | Top app bar |
| `BotNav` | BottomNav | Bottom navigation |
| `Tabs` | TabBar | Tab bar |
| `Drawer` | Drawer | Navigation drawer |
| `NavRail` | NavigationRail | Navigation rail |
| `Crumb` | Breadcrumb | Breadcrumb nav |
| `Page` | Pagination | Pagination control |

#### Feedback Components (7)

| Alias | Full Type | Description |
|-------|-----------|-------------|
| `Alert` | Alert | Alert dialog |
| `Toast` | Toast | Toast notification |
| `Snack` | Snackbar | Snackbar message |
| `Modal` | Modal | Modal overlay |
| `Dialog` | Dialog | Dialog window |
| `Sheet` | BottomSheet | Bottom sheet |
| `Banner` | Banner | Banner message |

#### Data Components (6)

| Alias | Full Type | Description |
|-------|-----------|-------------|
| `Tile` | ListTile | List item tile |
| `Accord` | Accordion | Accordion panel |
| `Timeline` | Timeline | Timeline view |
| `DataGrid` | DataGrid | Data grid |
| `Table` | Table | Data table |
| `Tree` | TreeView | Tree view |

#### Button Variants (5)

| Alias | Full Type | Description |
|-------|-----------|-------------|
| `TextBtn` | TextButton | Text-only button |
| `OutBtn` | OutlinedButton | Outlined button |
| `FillBtn` | FilledButton | Filled button |
| `IconBtn` | IconButton | Icon button |
| `FAB` | FAB | Floating action button |

**Total: 60+ type aliases**

### 5.2 Alias Resolution

**Rules:**
1. Aliases are case-sensitive
2. Unmapped aliases are rejected (no fallthrough)
3. Full type names are NOT accepted in DSL (only aliases)

**Examples:**
```
Col{...}      ✓ Valid (maps to Column)
Column{...}   ✗ Invalid (use Col)
col{...}      ✗ Invalid (case mismatch)
```

---

## 6. Property Names

### 6.1 Standard Properties

#### Text Properties (8)

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `text` | String | Text content | `text:"Hello"` |
| `label` | String | Label text | `label:"Name"` |
| `placeholder` | String | Placeholder text | `placeholder:"Enter..."` |
| `title` | String | Title text | `title:"Dialog"` |
| `subtitle` | String | Subtitle text | `subtitle:"Details"` |
| `desc` | String | Description | `desc:"More info"` |
| `fontSize` | Number | Font size (sp/pt) | `fontSize:16` |
| `fontWeight` | String | Font weight | `fontWeight:"bold"` |

#### Color Properties (2)

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `color` | Color | Text/icon color | `color:#000000` |
| `bgColor` | Color | Background color | `bgColor:#FFFFFF` |

#### Layout Properties (8)

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `width` | Number | Width (dp/px) | `width:100` |
| `height` | Number | Height (dp/px) | `height:50` |
| `padding` | Number | Padding (dp/px) | `padding:16` |
| `margin` | Number | Margin (dp/px) | `margin:8` |
| `spacing` | Number | Item spacing | `spacing:12` |
| `align` | String | Alignment | `align:"center"` |
| `maxLines` | Number | Max text lines | `maxLines:3` |

#### State Properties (4)

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `value` | Any | Current value | `value:50` |
| `checked` | Boolean | Checked state | `checked:true` |
| `selected` | Boolean | Selected state | `selected:false` |
| `enabled` | Boolean | Enabled state | `enabled:true` |
| `visible` | Boolean | Visibility | `visible:true` |

#### Media Properties (2)

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `src` | String | Image source | `src:"logo.png"` |
| `icon` | String | Icon name | `icon:"favorite"` |

#### Range Properties (3)

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `min` | Number | Minimum value | `min:0` |
| `max` | Number | Maximum value | `max:100` |
| `step` | Number | Step increment | `step:5` |

**Total: 30+ standard properties**

### 6.2 Property Values

**String Values:**
```
text:"Hello World"
label:"User Name"
```

**Number Values:**
```
fontSize:16
width:100
spacing:12
```

**Boolean Values:**
```
checked:true
enabled:false
visible:true
```

**Color Values:**
```
color:#000000          # RGB (6 hex digits)
bgColor:#FF0000FF      # RGBA (8 hex digits)
```

**Null Values:**
```
icon:null
src:null
```

---

## 7. Modifiers

### 7.1 Modifier Syntax

**Format:** `@modifierName(arg1,arg2,...)`

**Multiple Modifiers:** `@mod1(args),mod2(args),mod3(args)`

### 7.2 Standard Modifiers

| Modifier | Arguments | Description | Example |
|----------|-----------|-------------|---------|
| `p()` | 1 or 4 numbers | Padding | `@p(16)` or `@p(16,8,16,8)` |
| `m()` | 1 or 4 numbers | Margin | `@m(8)` |
| `bg()` | Color | Background | `@bg(#FFFFFF)` |
| `fg()` | Color | Foreground | `@fg(#000000)` |
| `r()` | 1 or 4 numbers | Corner radius | `@r(8)` or `@r(8,8,0,0)` |
| `f()` | 2 numbers | Frame (width, height) | `@f(100,50)` |
| `sh()` | 1-4 numbers | Shadow | `@sh(4)` or `@sh(2,2,8,#000000)` |
| `op()` | 1 number (0-1) | Opacity | `@op(0.8)` |
| `clip()` | Shape name | Clip shape | `@clip(Circle)` |

### 7.3 Modifier Examples

**Single Modifier:**
```
Col{@p(16);Text{text:"Hello"}}
```

**Multiple Modifiers:**
```
Card{@p(16),bg(#FFFFFF),r(8);Text{text:"Content"}}
```

**Complex Modifiers:**
```
Box{@p(16,8,16,8),sh(2,2,8,#00000033),r(8,8,0,0);...}
```

---

## 8. Values and Literals

### 8.1 String Literals

**Syntax:** `"text"`

**Escaping:**
```
"Hello \"World\""        # Quote escaping
"Line 1\nLine 2"         # Newline (in data, not DSL)
"Tab\tSeparated"         # Tab
```

**Rules:**
- Double quotes required
- No single-quote strings
- Escape sequences: `\"`, `\\`, `\n`, `\t`, `\r`

### 8.2 Number Literals

**Integer:**
```
42
-10
0
```

**Float:**
```
3.14
-0.5
100.0
```

**Scientific Notation:**
```
1e6
2.5e-3
```

**Rules:**
- No leading zeros (except `0.x`)
- Decimal point requires digits after
- No trailing decimal point

### 8.3 Boolean Literals

**Values:** `true` | `false`

**Case:** Lowercase only

**Examples:**
```
checked:true
enabled:false
```

### 8.4 Color Literals

**RGB Format:** `#RRGGBB` (6 hex digits)

**RGBA Format:** `#RRGGBBAA` (8 hex digits)

**Examples:**
```
color:#FF0000          # Red
color:#00FF00          # Green
color:#0000FFFF        # Blue, 100% alpha
color:#00000080        # Black, 50% alpha
```

**Rules:**
- Hash prefix required
- Uppercase or lowercase hex
- No shorthand (e.g., `#FFF` invalid)

### 8.5 Null Literal

**Value:** `null`

**Usage:**
```
icon:null
src:null
callback:null
```

---

## 9. Examples

### 9.1 Simple Examples

**Text Component:**
```
Text{text:"Hello World"}
```

**Button with ID:**
```
Btn#submitBtn{label:"Submit"}
```

**Text with Styling:**
```
Text{text:"Title";fontSize:24;color:#000000}
```

### 9.2 Nested Components

**Column with Children:**
```
Col{spacing:16;Text{text:"Header"};Text{text:"Body"};Btn{label:"Action"}}
```

**Card with Content:**
```
Card{@p(16),r(8);Col{spacing:8;Text{text:"Title";fontSize:18};Text{text:"Description"}}}
```

### 9.3 Form Example

```
Col#loginForm{spacing:16;@p(16);Text{text:"Login";fontSize:24};Field#email{label:"Email";placeholder:"user@example.com"};Field#password{label:"Password";obscureText:true};Check#remember{label:"Remember me"};Btn#submit{label:"Sign In";@onClick->handleLogin}}
```

**Formatted (for readability):**
```
Col#loginForm{
  spacing:16;
  @p(16);
  Text{text:"Login";fontSize:24};
  Field#email{label:"Email";placeholder:"user@example.com"};
  Field#password{label:"Password";obscureText:true};
  Check#remember{label:"Remember me"};
  Btn#submit{label:"Sign In";@onClick->handleLogin}
}
```

### 9.4 Dashboard Example

```
Col#dashboard{spacing:16;@p(16);Text{text:"Dashboard";fontSize:28};Row{spacing:12;Stat{title:"Users";value:1250};Stat{title:"Revenue";value:"$12.5K"};Stat{title:"Growth";value:"+15%"}};Card{@p(16),r(8);Text{text:"Recent Activity"};LazyCol{spacing:8;Tile{title:"Order #1234"};Tile{title:"Order #1235"};Tile{title:"Order #1236"}}}}
```

### 9.5 With Metadata

```
@avaui/1.0
id:com.example.app
name:My Application
theme:Material3Light

Col#main{spacing:16;@p(16),bg(#FFFFFF);AppBar{title:"Home"};Scroll{Col{spacing:8;Text{text:"Welcome"};Btn{label:"Get Started"}}}}
```

---

## 10. Comparison with JSON

### 10.1 Size Comparison

**JSON (350 bytes):**
```json
{
  "type": "Column",
  "id": "main",
  "properties": {
    "spacing": 16
  },
  "modifiers": [
    {
      "type": "padding",
      "all": 16
    }
  ],
  "children": [
    {
      "type": "Text",
      "properties": {
        "text": "Hello World"
      }
    }
  ]
}
```

**Compact DSL (95 bytes):**
```
Col#main{spacing:16;@p(16);Text{text:"Hello World"}}
```

**Reduction: 73%**

### 10.2 Feature Comparison

| Feature | JSON | Compact DSL |
|---------|------|-------------|
| Size | Baseline | -40% to -73% |
| Human Readable | ✓ | ✓✓ |
| Parse Speed | Baseline | +30% to +50% |
| Type Safety | Schema required | Built-in |
| Streaming | ✓ | Limited |
| Tools | Excellent | Good |

### 10.3 Use Cases

**Use JSON When:**
- Need streaming parsing
- Require standard tooling
- External API integration
- Schema validation critical

**Use Compact DSL When:**
- IPC between AVAMagic apps
- Size matters (mobile, network)
- Human debugging needed
- UI-specific serialization

---

## 11. Implementation Notes

### 11.1 Parser Requirements

**Must Support:**
- Component ID extraction
- Property parsing (string, number, boolean, color, null)
- Modifier parsing with arguments
- Callback parsing (event -> handler)
- Nested component trees (recursive)
- Error recovery (optional)

**Performance Targets:**
- Parse time: <1ms for typical tree (20 components)
- Memory: <100KB for large tree (1000 components)
- Streaming: Not required

### 11.2 Serializer Requirements

**Must Generate:**
- Valid DSL syntax
- Compact representation (no unnecessary whitespace)
- Deterministic output (same input → same output)
- Type alias mapping
- Property name mapping

**Must Handle:**
- Circular references (reject or serialize once)
- Large component trees (efficient buffering)
- Special characters in strings (escaping)
- Invalid components (skip or error)

### 11.3 Validation

**Syntax Validation:**
- Type alias exists
- Property names valid
- Value types correct
- Braces balanced
- IDs unique (recommended)

**Semantic Validation:**
- Required properties present
- Value ranges checked
- Component hierarchy valid

### 11.4 Error Handling

**Parse Errors:**
```
Error: Unexpected token '}' at position 42
Error: Unknown type alias 'Foo'
Error: Invalid color format '#GGGGGG'
```

**Semantic Errors:**
```
Error: Required property 'text' missing in Text component
Error: Invalid value for 'fontSize': -10
Error: Duplicate component ID 'btn1'
```

---

## 11. IPC Integration

### 11.1 Sending UI Components Over IPC

To send UI components over IPC (inter-process communication), wrap them in the `JSN` protocol code.

**Format:**
```
JSN:requestId:UIComponentDSL
```

**Example:**
```
JSN:ui1:Col#callPrompt{spacing:16;Text{text:"Incoming call from Manoj"};Row{spacing:12;Btn#accept{label:"Accept"};Btn#decline{label:"Decline"}}}
```

**Breakdown:**
- `JSN` - IPC protocol code for JSON/UI DSL (see [UNIVERSAL-IPC-SPEC.md](UNIVERSAL-IPC-SPEC.md))
- `ui1` - Request ID for tracking
- `Col#callPrompt{...}` - UI Component DSL (this specification)

### 11.2 Detection and Parsing

The IPC system auto-detects message types:

```kotlin
val message = "JSN:ui1:Col{Text{text:\"Hello\"}}"

val parsed = UniversalDSL.parse(message)
when (parsed) {
    is ParseResult.WrappedUI -> {
        val requestId = parsed.requestId  // "ui1"
        val dsl = parsed.dsl              // "Col{Text{text:\"Hello\"}}"

        // Parse UI DSL (this spec)
        val component = AvanuesDSLParser.parse(dsl)
        renderComponent(component)
    }
}
```

### 11.3 Complete Workflow Example

**Scenario:** App A sends custom UI prompt to App B

**Step 1: App A creates UI component**
```kotlin
val ui = """
Col#prompt{
    spacing:16;
    @p(16),bg(#FFFFFF),r(12);
    Text{text:"Incoming video call";fontSize:20};
    Text{text:"From: Manoj on Pixel 7";fontSize:14;color:#666666};
    Row{
        spacing:12;
        Btn#decline{label:"Decline";@bg(#F44336)};
        Btn#accept{label:"Accept";@bg(#4CAF50)}
    }
}
""".trimIndent()
```

**Step 2: App A wraps in JSN and sends via IPC**
```kotlin
val message = UIComponentMessage(
    requestId = "call1",
    componentDSL = ui
)
ipcManager.send("com.augmentalis.ava", message)
// Serializes to: JSN:call1:Col#prompt{...}
```

**Step 3: App B receives and parses**
```kotlin
ipcManager.subscribe<UIComponentMessage>().collect { msg ->
    val component = AvanuesDSLParser.parse(msg.componentDSL)
    renderComponent(component)
}
```

### 11.4 Message Flow Diagram

```
App A                                    App B
──────                                   ──────

1. Create UI component (DSL)
   Col{Text{...};Row{Btn{...}}}

2. Wrap in JSN protocol
   JSN:ui1:Col{...}                →

3. Parse IPC message                     Receive
   - Code: JSN                           - Extract requestId
   - ID: ui1                             - Extract DSL
   - DSL: Col{...}

4. Parse UI DSL                          Parse
   - Type: Col                           - Properties
   - Children: Text, Row                 - Modifiers
                                         - Children

5. Render component                      Render
                                         - Create Col layout
                                         - Add Text
                                         - Add Row with Btns
```

### 11.5 Size Comparison

**Without JSN wrapper (local rendering):**
```
Col#prompt{spacing:16;Text{text:"Incoming call"};Row{Btn#accept{label:"Accept"};Btn#decline{label:"Decline"}}}
```
Size: 115 bytes

**With JSN wrapper (IPC transfer):**
```
JSN:ui1:Col#prompt{spacing:16;Text{text:"Incoming call"};Row{Btn#accept{label:"Accept"};Btn#decline{label:"Decline"}}}
```
Size: 124 bytes (+9 bytes for IPC wrapper)

**Equivalent JSON (VoiceOS UIIPCProtocol):**
```json
{
  "id": "ui1",
  "action": "ui.render",
  "sourceAppId": "com.app1",
  "targetAppId": "com.app2",
  "payload": {
    "dsl": "Col#prompt{spacing:16;Text{text:\"Incoming call\"};Row{Btn#accept{label:\"Accept\"};Btn#decline{label:\"Decline\"}}}",
    "options": {"animate": true, "cacheEnabled": true}
  },
  "timestamp": 1732012345000
}
```
Size: 395 bytes

**Savings: 68% smaller with JSN wrapper**

### 11.6 Protocol Reference

For complete IPC protocol specification (all 77 codes including JSN), see:
- [UNIVERSAL-IPC-SPEC.md](UNIVERSAL-IPC-SPEC.md)

---

## 12. Implementation Notes

### 13.1 Parser Requirements

```
UITree
  ├─ Metadata? ──┐
  └─ Component+ ─┘

Component
  ├─ TypeAlias ────┐
  ├─ ComponentID? ─┤
  ├─ '{' ──────────┤
  ├─ ComponentBody ┤
  └─ '}' ──────────┘

ComponentBody
  ├─ PropertyList? ──┐
  ├─ ModifierList? ──┤
  ├─ CallbackList? ──┤
  └─ ChildList? ─────┘
```

### 13.2 Reserved Keywords

**Type Aliases:** All 60+ component aliases (case-sensitive)

**Modifier Names:** `p`, `m`, `bg`, `fg`, `r`, `f`, `sh`, `op`, `clip`

**Literals:** `true`, `false`, `null`

**Special Tokens:** `@`, `#`, `->`, `:`, `;`, `{`, `}`, `(`, `)`, `"`

### 13.3 MIME Type

**Recommended:** `application/vnd.avamagic.dsl+text`

**Alternative:** `text/x-avamagic-dsl`

**File Extension:** `.avadsl` or `.dsl`

### 13.4 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-11-19 | Initial specification |
| 0.9.0 | 2025-11-18 | Beta release (ultracompact) |
| 0.8.0 | 2025-11-17 | Alpha release |

### 13.5 References

- **Implementation:** `modules/AVAMagic/IPC/DSLSerializer/src/commonMain/kotlin/com/augmentalis/avamagic/ipc/dsl/DSLSerializer.kt`
- **Tests:** `modules/AVAMagic/IPC/DSLSerializer/src/commonTest/kotlin/`
- **README:** `modules/AVAMagic/IPC/DSLSerializer/README.md`
- **Developer Manual:** `docs/manuals/DEVELOPER-MANUAL.md` (Chapter 14a)

### 13.6 License

**Proprietary - Augmentalis ES**

All rights reserved. This specification is confidential and proprietary to Augmentalis ES.

---

**END OF SPECIFICATION**

**Maintained By:** Manoj Jhawar (manoj@ideahq.net)
**IDEACODE Version:** 8.4
**Document Version:** 1.0.0
**Last Updated:** 2025-11-19
