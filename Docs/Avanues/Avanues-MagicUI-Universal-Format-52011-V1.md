# MagicUI - Universal Format v2.0 Implementation

**Last Updated:** 2025-11-20
**Status:** Active
**Format Version:** 2.0

---

## Overview

MagicUI is the voice-first UI component library and DSL (Domain Specific Language) for the Avanues ecosystem. It uses **Universal Format v2.0** (.ami files) to define all 60+ UI components in a human-readable, code-generation-ready format.

### Key Features

1. **Voice-First UI Creation:** Build UIs using natural voice commands
2. **Multi-Platform:** Generate code for Android (Compose), iOS (SwiftUI), Web (React)
3. **Universal Format:** Same file format as all Avanues projects
4. **Component Library:** 60+ pre-defined UI components
5. **DSL Integration:** Direct mapping to Avanues UI DSL

---

## Architecture

### UI Creation Flow

```
User Voice Input
      ↓
Speech Recognition (STT)
      ↓
AVA NLU Engine
      ↓
UI Intent Classification
      ↓
MagicUI DSL Generator
      ↓
UI DSL String
      ↓
Code Generator (Compose/SwiftUI/React)
      ↓
Platform-Specific UI Code
```

### File Structure

```
magicui-components/
├── magicui-components.ami  # All 60 UI components
└── README.md               # Format documentation
```

---

## Universal Format v2.0 Structure

### File Header

```
# Avanues Universal Format v1.0
# Type: AMI - MagicUI Component Definitions
# Extension: .ami
# Project: MagicUI (Avanues UI DSL Components)
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: magicui
metadata:
  file: magicui-components.ami
  category: ui-components
  name: MagicUI Component Library
  description: UI component definitions in Avanues DSL format
  priority: 1
  count: 60
---
```

### Component Entries

Format: `CODE:component_id:description`

```
# LAYOUT COMPONENTS
COL:column:Vertical column layout
ROW:row:Horizontal row layout
BOX:box:Generic container box
STK:stack:Layered stack layout
GRD:grid:Grid layout
LST:list:Scrollable list
SPC:spacer:Spacing element
DVR:divider:Visual divider

# TEXT COMPONENTS
TXT:text:Plain text display
HDR:header:Header text
LBL:label:Text label
CAP:caption:Caption text
TIT:title:Title text
SUB:subtitle:Subtitle text
PAR:paragraph:Paragraph text
```

### Global Synonyms

```
---
synonyms:
  text: [label, title, heading]
  button: [btn, action, click]
  input: [field, textbox, entry]
  list: [menu, items, options]
  image: [picture, photo, icon]
  card: [panel, container, box]
  dialog: [modal, popup, overlay]
  progress: [loader, loading, spinner]
```

---

## Component Categories (60 Components)

### 1. Layout Components (8)

| Code | Component | Purpose | DSL Example |
|------|-----------|---------|-------------|
| COL | Column | Vertical layout | `COL#main{children}` |
| ROW | Row | Horizontal layout | `ROW#toolbar{children}` |
| BOX | Box | Generic container | `BOX#container{children}` |
| STK | Stack | Layered layout | `STK#layers{children}` |
| GRD | Grid | Grid layout | `GRD#gallery{cols:3;children}` |
| LST | List | Scrollable list | `LST#items{children}` |
| SPC | Spacer | Spacing | `SPC{height:16}` |
| DVR | Divider | Visual divider | `DVR{color:gray}` |

### 2. Text Components (7)

| Code | Component | Purpose | DSL Example |
|------|-----------|---------|-------------|
| TXT | Text | Plain text | `TXT#label{text:Hello}` |
| HDR | Header | Header text | `HDR#page{text:Settings}` |
| LBL | Label | Text label | `LBL#field{text:Name}` |
| CAP | Caption | Caption text | `CAP#hint{text:Optional}` |
| TIT | Title | Title text | `TIT#screen{text:Profile}` |
| SUB | Subtitle | Subtitle | `SUB#info{text:Details}` |
| PAR | Paragraph | Paragraph | `PAR#desc{text:Long text}` |

### 3. Input Components (13)

| Code | Component | Purpose | DSL Example |
|------|-----------|---------|-------------|
| INP | Input | Text input | `INP#name{hint:Enter name}` |
| TFA | TextArea | Multi-line input | `TFA#bio{rows:5}` |
| PWD | Password | Password input | `PWD#pass{hint:Password}` |
| NUM | Number | Number input | `NUM#age{min:0;max:120}` |
| EML | Email | Email input | `EML#email{hint:Email}` |
| PHN | Phone | Phone input | `PHN#phone{hint:Phone}` |
| DAT | Date | Date picker | `DAT#dob{format:yyyy-MM-dd}` |
| TIM | Time | Time picker | `TIM#alarm{format:HH:mm}` |
| CHK | Checkbox | Checkbox | `CHK#agree{text:I agree}` |
| RDO | Radio | Radio button | `RDO#option1{text:Option 1}` |
| SWT | Switch | Toggle switch | `SWT#wifi{text:WiFi}` |
| SLD | Slider | Range slider | `SLD#volume{min:0;max:100}` |

### 4. Button Components (6)

| Code | Component | Purpose | DSL Example |
|------|-----------|---------|-------------|
| BTN | Button | Generic button | `BTN#submit{text:Submit}` |
| ICB | IconButton | Icon button | `ICB#menu{icon:menu}` |
| FAB | FloatingActionButton | FAB | `FAB#add{icon:add}` |
| TGB | ToggleButton | Toggle button | `TGB#bold{icon:format_bold}` |
| CHI | Chip | Chip button | `CHI#filter{text:Active}` |
| SEG | Segmented | Segmented control | `SEG{options:[Day,Week,Month]}` |

### 5. Display Components (13)

| Code | Component | Purpose | DSL Example |
|------|-----------|---------|-------------|
| IMG | Image | Image display | `IMG#photo{src:photo.jpg}` |
| ICN | Icon | Icon display | `ICN#check{name:check_circle}` |
| AVT | Avatar | User avatar | `AVT#user{src:avatar.jpg}` |
| BDG | Badge | Notification badge | `BDG#notif{count:5}` |
| TAG | Tag | Label tag | `TAG#new{text:New}` |
| CRD | Card | Content card | `CRD#item{children}` |
| DLG | Dialog | Modal dialog | `DLG#alert{children}` |
| SHT | Sheet | Bottom sheet | `SHT#options{children}` |
| SNK | Snackbar | Snackbar | `SNK#msg{text:Saved}` |
| TLT | Tooltip | Tooltip | `TLT#help{text:Help text}` |
| PRG | Progress | Progress bar | `PRG#load{value:0.5}` |
| SPN | Spinner | Loading spinner | `SPN#loading{size:large}` |

### 6. Navigation Components (7)

| Code | Component | Purpose | DSL Example |
|------|-----------|---------|-------------|
| TAB | Tabs | Tab navigation | `TAB{tabs:[Home,Profile,Settings]}` |
| NAV | NavBar | Navigation bar | `NAV#top{title:App Name}` |
| DRW | Drawer | Side drawer | `DRW#menu{children}` |
| MNU | Menu | Dropdown menu | `MNU{items:[Edit,Delete]}` |
| BRD | Breadcrumb | Breadcrumb trail | `BRD{path:[Home,Users,Profile]}` |
| PGN | Pagination | Page navigation | `PGN{pages:10;current:1}` |
| STR | Stepper | Step indicator | `STR{steps:[Step 1,Step 2,Step 3]}` |

### 7. Media Components (6)

| Code | Component | Purpose | DSL Example |
|------|-----------|---------|-------------|
| VID | Video | Video player | `VID#player{src:video.mp4}` |
| AUD | Audio | Audio player | `AUD#track{src:audio.mp3}` |
| CAM | Camera | Camera view | `CAM#capture{mode:photo}` |
| MIC | Microphone | Microphone input | `MIC#record{duration:60}` |
| GAL | Gallery | Image gallery | `GAL#photos{images:[...]}` |
| CRO | Carousel | Carousel slider | `CRO#slides{children}` |

### 8. Data Display (6)

| Code | Component | Purpose | DSL Example |
|------|-----------|---------|-------------|
| TBL | Table | Data table | `TBL{columns:[...];rows:[...]}` |
| CHT | Chart | Chart visualization | `CHT#sales{type:bar;data:[...]}` |
| GRA | Graph | Graph visualization | `GRA#network{nodes:[...]}` |
| TRE | Tree | Tree view | `TRE#files{root:...}` |
| TML | Timeline | Timeline view | `TML#events{items:[...]}` |
| CAL | Calendar | Calendar view | `CAL#schedule{events:[...]}` |

### 9. Feedback Components (5)

| Code | Component | Purpose | DSL Example |
|------|-----------|---------|-------------|
| ALR | Alert | Alert message | `ALR#warning{text:Warning!}` |
| TST | Toast | Toast notification | `TST{text:Success;duration:2000}` |
| SKL | Skeleton | Skeleton loader | `SKL{type:text;lines:3}` |
| EMP | Empty | Empty state | `EMP{text:No items found}` |
| ERR | Error | Error state | `ERR{text:Failed to load}` |

---

## Implementation

### Kotlin DSL Parser (Example)

```kotlin
package com.augmentalis.magicui.parser

import com.augmentalis.avamagic.ipc.UniversalFileParser
import com.augmentalis.avamagic.ipc.FileType

class MagicUIParser {

    private val components: Map<String, UIComponent>

    init {
        // Load components from .ami file
        val amiContent = loadAssetFile("magicui-components/magicui-components.ami")
        val parsed = UniversalFileParser.parse(amiContent, FileType.AMI)
        components = parseComponents(parsed)
    }

    fun parseDSL(dsl: String): UITree {
        // Parse DSL string like "COL#main{TXT#title{text:Hello}}"
        val root = parseNode(dsl)
        return UITree(root)
    }

    private fun parseNode(dsl: String): UINode {
        val (code, id, props, children) = extractParts(dsl)
        val component = components[code] ?: error("Unknown component: $code")

        return UINode(
            componentCode = code,
            id = id,
            properties = props,
            children = children.map { parseNode(it) }
        )
    }
}

data class UINode(
    val componentCode: String,
    val id: String?,
    val properties: Map<String, String>,
    val children: List<UINode>
)
```

### Code Generators

#### Jetpack Compose Generator

```kotlin
package com.augmentalis.magicui.codegen

class ComposeGenerator {

    fun generate(tree: UITree): String {
        val code = StringBuilder()
        code.append("@Composable\nfun GeneratedUI() {\n")
        code.append(generateNode(tree.root, indent = 1))
        code.append("}\n")
        return code.toString()
    }

    private fun generateNode(node: UINode, indent: Int): String {
        val i = "    ".repeat(indent)
        return when (node.componentCode) {
            "COL" -> """
$i Column(modifier = Modifier${node.id?.let { ".testTag(\"$it\")" } ?: ""}) {
${node.children.joinToString("\n") { generateNode(it, indent + 1) }}
$i}
"""
            "ROW" -> """
$i Row(modifier = Modifier${node.id?.let { ".testTag(\"$it\")" } ?: ""}) {
${node.children.joinToString("\n") { generateNode(it, indent + 1) }}
$i}
"""
            "TXT" -> """
$i Text(
$i    text = "${node.properties["text"] ?: ""}",
$i    modifier = Modifier${node.id?.let { ".testTag(\"$it\")" } ?: ""}
$i)
"""
            "BTN" -> """
$i Button(
$i    onClick = { /* action */ },
$i    modifier = Modifier${node.id?.let { ".testTag(\"$it\")" } ?: ""}
$i) {
$i    Text("${node.properties["text"] ?: ""}")
$i}
"""
            else -> ""
        }
    }
}
```

#### SwiftUI Generator

```swift
class SwiftUIGenerator {
    func generate(tree: UITree) -> String {
        var code = "struct GeneratedView: View {\n"
        code += "    var body: some View {\n"
        code += generateNode(tree.root, indent: 2)
        code += "    }\n"
        code += "}\n"
        return code
    }

    private func generateNode(_ node: UINode, indent: Int) -> String {
        let i = String(repeating: " ", count: indent * 4)
        switch node.componentCode {
        case "COL":
            return """
\(i)VStack {
\(node.children.map { generateNode($0, indent: indent + 1) }.joined(separator: "\n"))
\(i)}
"""
        case "ROW":
            return """
\(i)HStack {
\(node.children.map { generateNode($0, indent: indent + 1) }.joined(separator: "\n"))
\(i)}
"""
        case "TXT":
            return """
\(i)Text("\(node.properties["text"] ?? "")")
"""
        case "BTN":
            return """
\(i)Button("\(node.properties["text"] ?? "")") {
\(i)    // action
\(i)}
"""
        default:
            return ""
        }
    }
}
```

---

## Voice UI Creation Examples

### Example 1: Login Screen

```
User: "Create a login screen with email, password, and sign in button"

→ AVA generates DSL:
COL#login{
  TIT#title{text:Sign In}
  SPC{height:24}
  INP#email{type:email;hint:Email Address}
  SPC{height:16}
  PWD#password{hint:Password}
  SPC{height:24}
  BTN#submit{text:Sign In;color:primary}
}

→ Code generated for Compose/SwiftUI/React
→ UI rendered on screen
```

### Example 2: User Profile Card

```
User: "Show user profile with avatar, name, email, and edit button"

→ AVA generates DSL:
CRD#profile{
  ROW{
    AVT#avatar{src:user.jpg;size:64}
    SPC{width:16}
    COL{
      TXT#name{text:John Doe;style:headline}
      SPC{height:4}
      CAP#email{text:john@example.com}
    }
  }
  SPC{height:16}
  DVR
  SPC{height:16}
  ROW#actions{
    BTN#edit{text:Edit Profile;style:outlined}
  }
}
```

### Example 3: Settings Screen

```
User: "Create settings with dark mode toggle and notification switch"

→ AVA generates DSL:
COL#settings{
  TIT#title{text:Settings}
  SPC{height:24}
  SWT#dark_mode{text:Dark Mode;checked:false}
  DVR
  SWT#notifications{text:Notifications;checked:true}
  DVR
  SLD#volume{text:Volume;min:0;max:100;value:50}
}
```

---

## Testing

### Unit Tests

```kotlin
@Test
fun `parse AMI file successfully`() {
    val content = loadTestResource("magicui-components.ami")
    val parsed = UniversalFileParser.parse(content, FileType.AMI)

    assertEquals("avu-1.0", parsed.schema)
    assertEquals("magicui", parsed.project)
    assertEquals(60, parsed.metadata["count"])
}

@Test
fun `parse simple DSL string`() {
    val parser = MagicUIParser()
    val dsl = "COL#main{TXT#title{text:Hello}}"
    val tree = parser.parseDSL(dsl)

    assertEquals("COL", tree.root.componentCode)
    assertEquals("main", tree.root.id)
    assertEquals(1, tree.root.children.size)
    assertEquals("TXT", tree.root.children[0].componentCode)
    assertEquals("Hello", tree.root.children[0].properties["text"])
}

@Test
fun `generate Compose code from DSL`() {
    val parser = MagicUIParser()
    val generator = ComposeGenerator()

    val dsl = "COL{TXT{text:Hello}BTN{text:Click}}"
    val tree = parser.parseDSL(dsl)
    val code = generator.generate(tree)

    assertTrue(code.contains("Column"))
    assertTrue(code.contains("Text(text = \"Hello\")"))
    assertTrue(code.contains("Button"))
}
```

---

## References

- **Universal Format Spec:** `/Volumes/M-Drive/Coding/Avanues/docs/specifications/UNIVERSAL-FILE-FORMAT-FINAL.md`
- **UI DSL Spec:** `/Volumes/M-Drive/Coding/Avanues/docs/specifications/UNIVERSAL-DSL-SPEC.md`
- **Master Guide:** `/Volumes/M-Drive/Coding/Avanues/docs/Universal-Format-v2.0-Master-Guide.md`
- **UniversalFileParser:** `/Volumes/M-Drive/Coding/Avanues/modules/AVAMagic/IPC/UniversalIPC/`
- **Components File:** `/Volumes/M-Drive/Coding/Avanues/magicui-components/magicui-components.ami`

---

**Status:** ✅ Production Ready
**Format:** Universal v2.0 (.ami)
**Total Components:** 60
**Categories:** 9
**Platforms:** Android (Compose), iOS (SwiftUI), Web (React)
