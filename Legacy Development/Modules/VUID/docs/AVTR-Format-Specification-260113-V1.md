# AVTR Format Specification

**Format Name:** AVID Type Registry (AVTR)
**Version:** 1.0
**Date:** 2026-01-13
**Status:** DRAFT
**Extension:** `.avtr`

---

## 1. Overview

AVTR (AVID Type Registry) is a companion format to AVID that maps generic type codes to platform-specific UI classes. This enables AVID files to remain platform-agnostic while allowing viewer applications to display platform-specific information.

### Purpose

| Use Case | Description |
|----------|-------------|
| **Scanning** | Map detected class names → generic type codes |
| **Display** | Expand type codes → platform-specific class names |
| **Validation** | Verify elements match expected class patterns |
| **Documentation** | Reference for supported UI patterns |

### Design Principles

- Platform-agnostic type codes (3 characters)
- Multiple class mappings per platform
- Regex support for pattern matching
- Hierarchical class inheritance support

---

## 2. File Structure

```
# AVID Type Registry v1.0
# Type: AVTR
# Extension: .avtr
---
schema: avtr-1.0
version: 1.0.0
name: VoiceOS Standard Types
description: Standard type mappings for VoiceOS platform
metadata:
  types: {count}
  platforms: [and, ios, web, dsk]
  updated: {ISO8601_timestamp}
---
# Type records
TYP:code:name:description
AND:code:class1,class2,class3
IOS:code:class1,class2,class3
WEB:code:selector1,selector2
DSK:code:class1,class2

# Action mappings
ACT:action:name:description:platforms
---
aliases:
  typ: [type, kind, category]
  and: [android, droid]
  ios: [apple, iphone, ipad]
  web: [browser, html, dom]
  dsk: [desktop, jvm, compose]
  act: [action, verb, operation]
```

---

## 3. Record Types

### 3.1 TYP - Type Definition

**Format:** `TYP:code:name:description`

Defines a generic type code with human-readable name and description.

| Field | Required | Description |
|-------|----------|-------------|
| code | Yes | 3-char uppercase type code |
| name | Yes | Human-readable name |
| description | Yes | Brief description |

**Example:**
```
TYP:BTN:Button:Clickable action elements
```

### 3.2 Platform Class Mappings

Platform records map type codes to platform-specific class names.

**Available platform record types:**

| Record | Platform | Code in AVID |
|--------|----------|--------------|
| AND | Android | A |
| IOS | iOS | I |
| WEB | Web | W |
| MAC | macOS | M |
| WIN | Windows | X |
| LNX | Linux | L |

### 3.3 AND - Android Classes

**Format:** `AND:code:class1,class2,class3,...`

Maps type code to Android class names (View classes and Compose components).

| Field | Required | Description |
|-------|----------|-------------|
| code | Yes | Type code from TYP record |
| classes | Yes | Comma-separated class names |

**Matching Rules:**
- Simple name match: `Button` matches `android.widget.Button`
- Contains match: `Button` matches `MaterialButton`, `ImageButton`
- Exact match with prefix `=`: `=Button` matches only `Button`
- Regex with prefix `~`: `~.*Button$` matches any ending in Button

**Example:**
```
AND:BTN:Button,ImageButton,MaterialButton,FloatingActionButton,ExtendedFloatingActionButton,IconButton,TextButton,OutlinedButton,ElevatedButton
```

### 3.3 IOS - iOS Classes

**Format:** `IOS:code:class1,class2,class3,...`

Maps type code to iOS class names (UIKit and SwiftUI).

**Example:**
```
IOS:BTN:UIButton,UIBarButtonItem,Button
```

### 3.4 WEB - Web Selectors

**Format:** `WEB:code:selector1,selector2,...`

Maps type code to CSS selectors and HTML elements.

**Selector Types:**
- Element: `button`, `input`
- Class: `.btn`, `.button`
- Attribute: `[type=submit]`, `[role=button]`
- Combined: `a.btn`, `input[type=button]`

**Example:**
```
WEB:BTN:button,input[type=submit],input[type=button],a.btn,[role=button]
```

### 3.5 MAC - macOS Classes

**Format:** `MAC:code:class1,class2,...`

Maps type code to macOS classes (AppKit, SwiftUI for Mac).

**Example:**
```
MAC:BTN:NSButton,Button
```

### 3.6 WIN - Windows Classes

**Format:** `WIN:code:class1,class2,...`

Maps type code to Windows classes (WinUI, WPF, UWP).

**Example:**
```
WIN:BTN:Button,AppBarButton,HyperlinkButton
```

### 3.7 LNX - Linux Classes

**Format:** `LNX:code:class1,class2,...`

Maps type code to Linux classes (GTK, Qt).

**Example:**
```
LNX:BTN:GtkButton,QPushButton
```

### 3.8 DSK - Desktop Generic (Deprecated)

**Format:** `DSK:code:class1,class2,...`

Generic desktop classes for Compose Multiplatform shared code.
Use MAC/WIN/LNX for platform-specific mappings.

**Example:**
```
DSK:BTN:Button,IconButton,TextButton
```

### 3.9 ACT - Action Definition

**Format:** `ACT:action:name:description:platforms`

Defines supported actions and their platform availability.

| Field | Required | Description |
|-------|----------|-------------|
| action | Yes | Action code (lowercase) |
| name | Yes | Human-readable name |
| description | Yes | Brief description |
| platforms | Yes | Supported platforms (and,ios,web,dsk or *) |

**Example:**
```
ACT:click:Click:Tap or click element:*
ACT:long_press:Long Press:Press and hold:and,ios
ACT:hover:Hover:Mouse hover:web,dsk
```

---

## 4. Complete Registry Example

```
# AVID Type Registry v1.0
# Type: AVTR
# Extension: .avtr
#
# Standard type mappings for VoiceOS platform
# Covers Android, iOS, Web, and Desktop
---
schema: avtr-1.0
version: 1.0.0
name: VoiceOS Standard Types
description: Standard UI element type mappings for the Avanues ecosystem
metadata:
  types: 28
  platforms: [and, ios, web, dsk]
  updated: 2026-01-13T10:00:00Z
---
# ============================================================================
# BUTTON TYPES
# ============================================================================
TYP:BTN:Button:Clickable action elements that trigger operations
AND:BTN:Button,ImageButton,MaterialButton,FloatingActionButton,ExtendedFloatingActionButton,AppCompatButton,IconButton,TextButton,OutlinedButton,ElevatedButton,FilledTonalButton
IOS:BTN:UIButton,UIBarButtonItem,Button,BorderlessButton
WEB:BTN:button,input[type=submit],input[type=button],a.btn,.button,[role=button]
DSK:BTN:Button,IconButton,TextButton,OutlinedButton,JButton

TYP:FAB:FAB:Floating action buttons for primary actions
AND:FAB:FloatingActionButton,ExtendedFloatingActionButton,SmallFloatingActionButton,LargeFloatingActionButton
IOS:FAB:
WEB:FAB:button.fab,.floating-action-button,[data-fab]
DSK:FAB:FloatingActionButton,ExtendedFloatingActionButton

# ============================================================================
# INPUT TYPES
# ============================================================================
TYP:INP:Input:Text entry and input fields
AND:INP:EditText,TextInputEditText,AutoCompleteTextView,MultiAutoCompleteTextView,SearchView,TextField,OutlinedTextField,BasicTextField
IOS:INP:UITextField,UITextView,UISearchBar,TextField,TextEditor,SecureField
WEB:INP:input[type=text],input[type=email],input[type=password],input[type=search],input[type=tel],input[type=url],input[type=number],textarea,.input-field
DSK:INP:TextField,OutlinedTextField,BasicTextField,JTextField,JTextArea

TYP:CHK:Checkbox:Checkable selection elements
AND:CHK:CheckBox,AppCompatCheckBox,MaterialCheckBox,Checkbox,TriStateCheckbox
IOS:CHK:UISwitch,Toggle
WEB:CHK:input[type=checkbox],.checkbox,[role=checkbox]
DSK:CHK:Checkbox,TriStateCheckbox,JCheckBox

TYP:RDO:Radio:Radio button selection elements
AND:RDO:RadioButton,AppCompatRadioButton,MaterialRadioButton,RadioButton
IOS:RDO:
WEB:RDO:input[type=radio],.radio,[role=radio]
DSK:RDO:RadioButton,JRadioButton

TYP:SWT:Switch:Toggle switch elements
AND:SWT:Switch,SwitchCompat,SwitchMaterial,Switch
IOS:SWT:UISwitch,Toggle
WEB:SWT:input[type=checkbox].switch,.toggle-switch,[role=switch]
DSK:SWT:Switch

TYP:SLR:Slider:Range selection sliders
AND:SLR:SeekBar,Slider,RangeSlider,AppCompatSeekBar
IOS:SLR:UISlider,Slider
WEB:SLR:input[type=range],.slider,[role=slider]
DSK:SLR:Slider,RangeSlider

TYP:SEL:Select:Dropdown and picker elements
AND:SEL:Spinner,AppCompatSpinner,MaterialSpinner,DropdownMenu,ExposedDropdownMenu
IOS:SEL:UIPickerView,Picker,UISegmentedControl
WEB:SEL:select,.dropdown,.picker,[role=listbox],[role=combobox]
DSK:SEL:DropdownMenu,ExposedDropdownMenu,JComboBox

# ============================================================================
# TEXT & DISPLAY TYPES
# ============================================================================
TYP:TXT:Text:Static text and label elements
AND:TXT:TextView,AppCompatTextView,MaterialTextView,Text
IOS:TXT:UILabel,Text
WEB:TXT:p,span,label,h1,h2,h3,h4,h5,h6,.text,[role=text]
DSK:TXT:Text,JLabel

TYP:HDR:Header:Section header elements
AND:HDR:Toolbar,ActionBar,TopAppBar,AppBarLayout
IOS:HDR:UINavigationBar
WEB:HDR:header,h1,h2,.header,[role=banner]
DSK:HDR:TopAppBar

TYP:FTR:Footer:Section footer elements
AND:FTR:
IOS:FTR:
WEB:FTR:footer,.footer,[role=contentinfo]
DSK:FTR:

TYP:IMG:Image:Image and icon elements
AND:IMG:ImageView,AppCompatImageView,ShapeableImageView,Icon,Image
IOS:IMG:UIImageView,Image
WEB:IMG:img,svg,picture,.image,[role=img]
DSK:IMG:Image,Icon

TYP:LNK:Link:Hyperlink elements
AND:LNK:
IOS:LNK:UITextView,Link
WEB:LNK:a,[role=link]
DSK:LNK:

# ============================================================================
# CONTAINER TYPES
# ============================================================================
TYP:LST:List:Scrollable list containers
AND:LST:RecyclerView,ListView,LazyColumn,LazyRow,LazyVerticalGrid,LazyHorizontalGrid
IOS:LST:UITableView,UICollectionView,List,ScrollView,LazyVStack,LazyHStack
WEB:LST:ul,ol,.list,[role=list],[role=listbox]
DSK:LST:LazyColumn,LazyRow,JList

TYP:ITM:Item:List item elements
AND:ITM:~.*ViewHolder.*,ListItem,LazyListScope
IOS:ITM:UITableViewCell,UICollectionViewCell
WEB:ITM:li,.list-item,[role=listitem],[role=option]
DSK:ITM:ListItem

TYP:CRD:Card:Card container elements
AND:CRD:CardView,MaterialCardView,Card,ElevatedCard,OutlinedCard
IOS:CRD:
WEB:CRD:.card,article,[role=article]
DSK:CRD:Card,ElevatedCard,OutlinedCard

TYP:SCR:Screen:Full screen containers
AND:SCR:Activity,Fragment,Scaffold,Surface,ComposeView
IOS:SCR:UIViewController,UINavigationController,UITabBarController,View
WEB:SCR:body,main,.page,[role=main]
DSK:SCR:Window,Scaffold,Surface

TYP:DIA:Dialog:Modal dialog elements
AND:DIA:AlertDialog,BottomSheetDialog,DialogFragment,Dialog,AlertDialog
IOS:DIA:UIAlertController,UIActionSheet,Alert,.sheet
WEB:DIA:dialog,.modal,[role=dialog],[role=alertdialog]
DSK:DIA:AlertDialog,Dialog

TYP:MNU:Menu:Menu and popup elements
AND:MNU:PopupMenu,ContextMenu,DropdownMenu,Menu
IOS:MNU:UIMenu,UIContextMenuInteraction,Menu
WEB:MNU:menu,nav,.menu,[role=menu]
DSK:MNU:DropdownMenu,ContextMenu

# ============================================================================
# NAVIGATION TYPES
# ============================================================================
TYP:NAV:Navigation:Navigation container elements
AND:NAV:NavigationView,NavigationRailView,BottomNavigationView,NavigationBar,NavigationRail
IOS:NAV:UINavigationBar,UITabBar,TabView,NavigationView
WEB:NAV:nav,.navigation,[role=navigation]
DSK:NAV:NavigationBar,NavigationRail

TYP:TAB:Tab:Tab navigation elements
AND:TAB:TabLayout,TabItem,Tab,TabRow
IOS:TAB:UITabBarItem,TabView
WEB:TAB:.tab,[role=tab],[role=tablist]
DSK:TAB:Tab,TabRow

# ============================================================================
# MEDIA TYPES
# ============================================================================
TYP:VID:Video:Video player elements
AND:VID:VideoView,PlayerView,ExoPlayerView
IOS:VID:AVPlayerViewController,VideoPlayer
WEB:VID:video,.video-player,[role=video]
DSK:VID:VideoPlayer

TYP:AUD:Audio:Audio player elements
AND:AUD:
IOS:AUD:
WEB:AUD:audio,.audio-player
DSK:AUD:

TYP:MAP:Map:Map view elements
AND:MAP:MapView,GoogleMap,MapboxMapView
IOS:MAP:MKMapView,Map
WEB:MAP:.map,[role=application].map
DSK:MAP:

TYP:CAM:Camera:Camera view elements
AND:CAM:PreviewView,CameraView,SurfaceView
IOS:CAM:AVCaptureVideoPreviewLayer
WEB:CAM:video.camera
DSK:CAM:

# ============================================================================
# INDICATOR TYPES
# ============================================================================
TYP:PRG:Progress:Progress indicator elements
AND:PRG:ProgressBar,CircularProgressIndicator,LinearProgressIndicator
IOS:PRG:UIActivityIndicatorView,UIProgressView,ProgressView
WEB:PRG:progress,.spinner,.loading,[role=progressbar]
DSK:PRG:CircularProgressIndicator,LinearProgressIndicator

TYP:CHT:Chart:Chart and graph elements
AND:CHT:
IOS:CHT:Chart
WEB:CHT:canvas.chart,.chart,[role=img].chart
DSK:CHT:

TYP:WBV:WebView:Embedded web content
AND:WBV:WebView,WebViewCompat
IOS:WBV:WKWebView,SFSafariViewController
WEB:WBV:iframe
DSK:WBV:

# ============================================================================
# ACTION DEFINITIONS
# ============================================================================
ACT:click:Click:Tap or click element:*
ACT:focus:Focus:Set input focus:*
ACT:scroll:Scroll:Scroll in direction:*
ACT:type:Type:Enter text:*
ACT:long_press:Long Press:Press and hold:and,ios,dsk
ACT:double_tap:Double Tap:Tap twice quickly:and,ios
ACT:swipe:Swipe:Swipe gesture:and,ios
ACT:pinch:Pinch:Pinch zoom gesture:and,ios
ACT:hover:Hover:Mouse hover:web,dsk
ACT:right_click:Right Click:Context menu click:web,dsk
ACT:drag:Drag:Drag and drop:*
ACT:select:Select:Select from options:*
ACT:clear:Clear:Clear input content:*
ACT:submit:Submit:Submit form:*
---
aliases:
  typ: [type, kind, category, element]
  and: [android, droid, kotlin, java]
  ios: [apple, iphone, ipad, swift, swiftui, uikit]
  web: [browser, html, dom, css, javascript]
  dsk: [desktop, jvm, compose, swing, javafx]
  act: [action, verb, operation, gesture]
```

---

## 5. Kotlin Implementation

### 5.1 Data Models

```kotlin
data class AvtrFile(
    val schema: String,
    val version: String,
    val name: String,
    val description: String,
    val types: List<TypeDefinition>,
    val actions: List<ActionDefinition>
)

data class TypeDefinition(
    val code: String,
    val name: String,
    val description: String,
    val android: List<String>,
    val ios: List<String>,
    val web: List<String>,
    val desktop: List<String>
)

data class ActionDefinition(
    val code: String,
    val name: String,
    val description: String,
    val platforms: Set<String>  // "and", "ios", "web", "dsk", or "*"
)
```

### 5.2 Type Resolver

```kotlin
class TypeResolver(private val registry: AvtrFile) {

    /**
     * Resolves a generic type code to platform-specific display info.
     */
    fun resolve(typeCode: String, platform: String): TypeDisplay {
        val type = registry.types.find { it.code == typeCode.uppercase() }
            ?: return TypeDisplay(typeCode, typeCode, emptyList())

        val classes = when (platform.lowercase()) {
            "and" -> type.android
            "ios" -> type.ios
            "web" -> type.web
            "dsk" -> type.desktop
            else -> emptyList()
        }

        return TypeDisplay(
            code = type.code,
            name = type.name,
            classes = classes,
            description = type.description
        )
    }

    /**
     * Finds type code from a platform-specific class name.
     * Used during scanning to classify detected UI elements.
     */
    fun classToType(className: String, platform: String): String? {
        for (type in registry.types) {
            val patterns = when (platform.lowercase()) {
                "and" -> type.android
                "ios" -> type.ios
                "web" -> type.web
                "dsk" -> type.desktop
                else -> continue
            }

            for (pattern in patterns) {
                if (matchesPattern(className, pattern)) {
                    return type.code
                }
            }
        }
        return null
    }

    private fun matchesPattern(className: String, pattern: String): Boolean {
        return when {
            // Exact match
            pattern.startsWith("=") ->
                className == pattern.substring(1)

            // Regex match
            pattern.startsWith("~") ->
                Regex(pattern.substring(1)).containsMatchIn(className)

            // CSS selector (web)
            pattern.contains("[") || pattern.contains(".") || pattern.contains("#") ->
                false  // Handled separately for web

            // Contains match (default)
            else ->
                className.contains(pattern, ignoreCase = true)
        }
    }

    /**
     * Gets all supported actions for a platform.
     */
    fun getActions(platform: String): List<ActionDefinition> {
        return registry.actions.filter { action ->
            action.platforms.contains("*") ||
            action.platforms.contains(platform.lowercase())
        }
    }
}

data class TypeDisplay(
    val code: String,
    val name: String,
    val classes: List<String>,
    val description: String = ""
)
```

### 5.3 Parser

```kotlin
object AvtrParser {

    fun parse(content: String): AvtrFile {
        val lines = content.lines()
        var inHeader = false
        var inRecords = false

        val headerLines = mutableListOf<String>()
        val types = mutableMapOf<String, MutableTypeBuilder>()
        val actions = mutableListOf<ActionDefinition>()

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.isBlank() || trimmed.startsWith("#") -> continue
                trimmed == "---" -> {
                    when {
                        !inHeader && !inRecords -> inHeader = true
                        inHeader -> { inHeader = false; inRecords = true }
                        else -> inRecords = false
                    }
                }
                inHeader -> headerLines.add(trimmed)
                inRecords -> parseRecord(trimmed, types, actions)
            }
        }

        val metadata = parseYamlHeader(headerLines)

        return AvtrFile(
            schema = metadata["schema"] ?: "avtr-1.0",
            version = metadata["version"] ?: "1.0.0",
            name = metadata["name"] ?: "",
            description = metadata["description"] ?: "",
            types = types.values.map { it.build() },
            actions = actions
        )
    }

    private fun parseRecord(
        line: String,
        types: MutableMap<String, MutableTypeBuilder>,
        actions: MutableList<ActionDefinition>
    ) {
        val parts = line.split(":", limit = 3)
        if (parts.size < 2) return

        val recordType = parts[0].uppercase()
        val code = parts[1].uppercase()
        val value = parts.getOrNull(2) ?: ""

        when (recordType) {
            "TYP" -> {
                val typeParts = line.split(":", limit = 4)
                types[code] = MutableTypeBuilder(
                    code = code,
                    name = typeParts.getOrNull(2) ?: code,
                    description = typeParts.getOrNull(3) ?: ""
                )
            }
            "AND" -> types[code]?.android = value.split(",").map { it.trim() }
            "IOS" -> types[code]?.ios = value.split(",").map { it.trim() }
            "WEB" -> types[code]?.web = value.split(",").map { it.trim() }
            "DSK" -> types[code]?.desktop = value.split(",").map { it.trim() }
            "ACT" -> {
                val actParts = line.split(":", limit = 5)
                if (actParts.size >= 4) {
                    actions.add(ActionDefinition(
                        code = actParts[1].lowercase(),
                        name = actParts[2],
                        description = actParts[3],
                        platforms = actParts.getOrNull(4)
                            ?.split(",")
                            ?.map { it.trim().lowercase() }
                            ?.toSet()
                            ?: setOf("*")
                    ))
                }
            }
        }
    }

    private class MutableTypeBuilder(
        val code: String,
        val name: String,
        val description: String
    ) {
        var android: List<String> = emptyList()
        var ios: List<String> = emptyList()
        var web: List<String> = emptyList()
        var desktop: List<String> = emptyList()

        fun build() = TypeDefinition(
            code = code,
            name = name,
            description = description,
            android = android,
            ios = ios,
            web = web,
            desktop = desktop
        )
    }
}
```

---

## 6. Usage Examples

### 6.1 During Scanning

```kotlin
val resolver = TypeResolver(avtrRegistry)

// AccessibilityService detects a node
val className = "androidx.appcompat.widget.AppCompatButton"
val platform = "and"

val typeCode = resolver.classToType(className, platform)
// typeCode = "BTN"

// Now create AVID element record
val element = AvidElement(
    avid = currentAppAvid,
    version = currentAppVersion,
    elemId = nextElemId++,
    type = typeCode ?: "UNK",  // Unknown if not matched
    resourceId = node.viewIdResourceName,
    name = node.text?.toString()
)
```

### 6.2 During Display

```kotlin
val resolver = TypeResolver(avtrRegistry)

// Reading AVID file with element type "BTN"
val element = avidFile.elements.first()
val platform = avidFile.platform

val display = resolver.resolve(element.type, platform)
// display.name = "Button"
// display.classes = ["Button", "ImageButton", "MaterialButton", ...]

println("${element.elemId}: ${display.name} (${display.classes.firstOrNull() ?: "?"})")
// Output: "1: Button (Button)"
```

---

## 7. Extending the Registry

### 7.1 Adding New Types

```
TYP:NEW:NewType:Description of the new type
AND:NEW:AndroidClass1,AndroidClass2
IOS:NEW:iOSClass1,iOSClass2
WEB:NEW:selector1,selector2
DSK:NEW:DesktopClass1
```

### 7.2 Custom Project Registry

Projects can override or extend the standard registry:

```
# Project-specific AVTR extensions
---
schema: avtr-1.0
version: 1.0.0
name: MyApp Custom Types
extends: voiceos-standard-1.0
---
# Custom types for this project
TYP:CUS:CustomWidget:App-specific custom widget
AND:CUS:com.myapp.CustomView,com.myapp.SpecialButton
```

---

## 8. Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-13 | Initial AVTR specification |

---

**End of Specification**
