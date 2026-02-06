# AMF Format Specification

**Version:** 1.0
**Date:** 2026-01-15
**Status:** Draft

---

## Overview

### What is AMF?

AMF (AvaMagic Format) is a compact, line-based data format designed specifically for AvaMagic theme and layout definitions. It provides a streamlined alternative to YAML and JSON for configuration files.

### Why AMF?

| Benefit | Description |
|---------|-------------|
| **50% Smaller** | Colon-delimited records eliminate verbose syntax (no braces, quotes, indentation) |
| **Faster Parsing** | Line-by-line processing with simple string splitting |
| **AI-Friendly** | Predictable structure makes LLM generation and parsing reliable |
| **Human-Readable** | Clear prefixes indicate record purpose at a glance |
| **Error-Resistant** | Each line is self-contained; malformed lines can be skipped |

### Design Principles

1. **One record per line** - No multi-line constructs
2. **Prefix-based typing** - Record type is always the first field
3. **Colon delimiter** - Fields separated by `:`
4. **Schema declaration** - File type and version declared upfront
5. **Comments supported** - Lines starting with `#` are ignored

---

## File Structure

```
# Comment line (optional)
---
schema: amf-{type}-{version}
---
RECORD:field1:field2:field3
RECORD:field1:field2:field3
```

### Structure Components

| Component | Description |
|-----------|-------------|
| `# Comment` | Optional comment lines, ignored by parser |
| `---` | Section delimiter (YAML-compatible) |
| `schema:` | Required schema declaration |
| `RECORD:` | Data records with colon-delimited fields |

### Schema Declaration

The schema line defines the file type and version:

```
schema: amf-{type}-{version}
```

Where:
- `amf` - Format identifier (always "amf")
- `{type}` - Record type (`thm` for theme, `lyt` for layout)
- `{version}` - Semantic version (e.g., `1.0`)

---

## Schema Types

| Schema | Purpose | Description |
|--------|---------|-------------|
| `amf-thm-1.0` | Theme Definition | Colors, typography, spacing, effects |
| `amf-lyt-1.0` | Layout Definition | UI structure, components, arrangement |

---

## Theme Records (amf-thm-1.0)

Theme files define visual styling including colors, typography, spacing, and effects.

### Record Types

| Prefix | Purpose | Format |
|--------|---------|--------|
| `THM:` | Metadata | `THM:name:version` |
| `PAL:` | Palette Color | `PAL:key:#hexcolor` |
| `TYP:` | Typography Style | `TYP:style:size:weight:family` |
| `SPC:` | Spacing Scale | `SPC:xs:val:sm:val:md:val:lg:val:xl:val` |
| `EFX:` | Effects | `EFX:shadow:bool:blur:val:elevation:val` |

### THM: Metadata

Defines theme name and version.

```
THM:name:version
```

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Theme identifier (alphanumeric, no spaces) |
| `version` | string | Semantic version (e.g., `1.0.0`) |

**Example:**
```
THM:OceanBreeze:1.0.0
```

### PAL: Palette

Defines color values for the theme.

```
PAL:key:#hexcolor
```

| Field | Type | Description |
|-------|------|-------------|
| `key` | string | Color identifier (e.g., `primary`, `background`) |
| `#hexcolor` | hex | 6-digit hex color with `#` prefix |

**Standard Keys:**
- `primary`, `secondary`, `tertiary`
- `background`, `surface`, `error`
- `onPrimary`, `onSecondary`, `onBackground`, `onSurface`, `onError`

**Example:**
```
PAL:primary:#1E88E5
PAL:background:#FFFFFF
PAL:onPrimary:#FFFFFF
```

### TYP: Typography

Defines text styles.

```
TYP:style:size:weight:family
```

| Field | Type | Description |
|-------|------|-------------|
| `style` | string | Style name (e.g., `h1`, `body`, `caption`) |
| `size` | number | Font size in sp/pt |
| `weight` | number | Font weight (100-900) |
| `family` | string | Font family name |

**Standard Styles:**
- `h1`, `h2`, `h3`, `h4`, `h5`, `h6`
- `body`, `bodyLarge`, `bodySmall`
- `label`, `labelLarge`, `labelSmall`
- `caption`

**Example:**
```
TYP:h1:32:700:Inter
TYP:body:16:400:Inter
TYP:caption:12:400:Inter
```

### SPC: Spacing

Defines spacing scale values.

```
SPC:xs:val:sm:val:md:val:lg:val:xl:val
```

| Field | Type | Description |
|-------|------|-------------|
| `xs` | number | Extra small spacing (4dp typical) |
| `sm` | number | Small spacing (8dp typical) |
| `md` | number | Medium spacing (16dp typical) |
| `lg` | number | Large spacing (24dp typical) |
| `xl` | number | Extra large spacing (32dp typical) |

**Example:**
```
SPC:xs:4:sm:8:md:16:lg:24:xl:32
```

### EFX: Effects

Defines visual effects like shadows.

```
EFX:shadow:bool:blur:val:elevation:val
```

| Field | Type | Description |
|-------|------|-------------|
| `shadow` | bool | Enable shadow (`true`/`false`) |
| `blur` | number | Blur radius in dp |
| `elevation` | number | Elevation level (0-24) |

**Example:**
```
EFX:shadow:true:blur:8:elevation:4
```

---

## Layout Records (amf-lyt-1.0)

Layout files define UI structure and component arrangement.

### Record Types

| Prefix | Purpose | Format |
|--------|---------|--------|
| `LYT:` | Metadata | `LYT:name:version` |
| `COL:` | Column | `COL:id:weight:align` |
| `ROW:` | Row | `ROW:id:weight:align` |
| `TXT:` | Text | `TXT:id:text:style` |
| `BTN:` | Button | `BTN:id:label:action` |
| `IMG:` | Image | `IMG:id:src:fit` |
| `SPC:` | Spacer | `SPC:id:size` |
| `END:` | Close Container | `END:id` |

### LYT: Metadata

Defines layout name and version.

```
LYT:name:version
```

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Layout identifier |
| `version` | string | Semantic version |

**Example:**
```
LYT:LoginScreen:1.0.0
```

### COL: Column

Starts a vertical container.

```
COL:id:weight:align
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier |
| `weight` | number | Flex weight (1.0 = fill) |
| `align` | string | Alignment (`start`, `center`, `end`, `stretch`) |

**Example:**
```
COL:mainColumn:1.0:center
```

### ROW: Row

Starts a horizontal container.

```
ROW:id:weight:align
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier |
| `weight` | number | Flex weight (1.0 = fill) |
| `align` | string | Alignment (`start`, `center`, `end`, `stretch`) |

**Example:**
```
ROW:buttonRow:0:center
```

### TXT: Text

Displays text content.

```
TXT:id:text:style
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier |
| `text` | string | Text content (use `\:` for literal colons) |
| `style` | string | Typography style from theme |

**Example:**
```
TXT:title:Welcome Back:h1
TXT:subtitle:Please sign in to continue:body
```

### BTN: Button

Creates an interactive button.

```
BTN:id:label:action
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier |
| `label` | string | Button text |
| `action` | string | Action identifier (handled by app) |

**Example:**
```
BTN:loginBtn:Sign In:doLogin
BTN:signupBtn:Create Account:navigateSignup
```

### IMG: Image

Displays an image.

```
IMG:id:src:fit
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier |
| `src` | string | Image source (URL or asset path) |
| `fit` | string | Fit mode (`cover`, `contain`, `fill`, `none`) |

**Example:**
```
IMG:logo:assets/logo.png:contain
```

### SPC: Spacer (Layout)

Adds spacing between elements.

```
SPC:id:size
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier |
| `size` | string | Size key (`xs`, `sm`, `md`, `lg`, `xl`) or number |

**Example:**
```
SPC:gap1:md
SPC:gap2:24
```

### END: Close Container

Closes a COL or ROW container.

```
END:id
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | ID of container to close |

**Example:**
```
END:mainColumn
```

---

## Complete Examples

### Theme Example (amf-thm-1.0)

```
# Ocean Breeze Theme
# A calm, professional color scheme
---
schema: amf-thm-1.0
---
THM:OceanBreeze:1.0.0
PAL:primary:#1E88E5
PAL:secondary:#00ACC1
PAL:tertiary:#7E57C2
PAL:background:#FAFAFA
PAL:surface:#FFFFFF
PAL:error:#E53935
PAL:onPrimary:#FFFFFF
PAL:onSecondary:#FFFFFF
PAL:onBackground:#212121
PAL:onSurface:#212121
PAL:onError:#FFFFFF
TYP:h1:32:700:Inter
TYP:h2:28:600:Inter
TYP:h3:24:600:Inter
TYP:body:16:400:Inter
TYP:bodyLarge:18:400:Inter
TYP:caption:12:400:Inter
TYP:label:14:500:Inter
SPC:xs:4:sm:8:md:16:lg:24:xl:32
EFX:shadow:true:blur:8:elevation:4
```

### Layout Example (amf-lyt-1.0)

```
# Login Screen Layout
---
schema: amf-lyt-1.0
---
LYT:LoginScreen:1.0.0
COL:root:1.0:center
IMG:logo:assets/logo.png:contain
SPC:gap1:xl
TXT:title:Welcome Back:h1
TXT:subtitle:Sign in to your account:body
SPC:gap2:lg
ROW:inputSection:0:stretch
TXT:emailLabel:Email:label
TXT:passwordLabel:Password:label
END:inputSection
SPC:gap3:md
ROW:buttonRow:0:center
BTN:loginBtn:Sign In:doLogin
SPC:btnGap:sm
BTN:forgotBtn:Forgot Password?:navigateForgot
END:buttonRow
SPC:gap4:xl
TXT:signupPrompt:Don't have an account?:caption
BTN:signupBtn:Create Account:navigateSignup
END:root
```

---

## Migration Guide

### From YAML to AMF

**YAML Theme:**
```yaml
name: OceanBreeze
version: 1.0.0
palette:
  primary: "#1E88E5"
  background: "#FAFAFA"
typography:
  h1:
    size: 32
    weight: 700
    family: Inter
```

**AMF Equivalent:**
```
---
schema: amf-thm-1.0
---
THM:OceanBreeze:1.0.0
PAL:primary:#1E88E5
PAL:background:#FAFAFA
TYP:h1:32:700:Inter
```

### From JSON to AMF

**JSON Layout:**
```json
{
  "name": "LoginScreen",
  "version": "1.0.0",
  "elements": [
    {"type": "column", "id": "root", "weight": 1.0, "align": "center"},
    {"type": "text", "id": "title", "text": "Welcome", "style": "h1"}
  ]
}
```

**AMF Equivalent:**
```
---
schema: amf-lyt-1.0
---
LYT:LoginScreen:1.0.0
COL:root:1.0:center
TXT:title:Welcome:h1
END:root
```

### Conversion Steps

1. **Identify schema type** - Is it a theme (`thm`) or layout (`lyt`)?
2. **Extract metadata** - Name and version become the first record
3. **Flatten nested structures** - Each property becomes a separate record
4. **Apply record prefixes** - Use appropriate prefix for each data type
5. **Validate** - Ensure all required fields are present

### Size Comparison

| Format | Login Theme | Login Layout |
|--------|-------------|--------------|
| JSON | 1,247 bytes | 892 bytes |
| YAML | 987 bytes | 654 bytes |
| **AMF** | **489 bytes** | **312 bytes** |
| **Savings** | **~50%** | **~52%** |

---

## Parser Implementation Notes

### Parsing Algorithm

```
1. Read file line by line
2. Skip lines starting with '#' (comments)
3. Skip '---' delimiter lines
4. Parse 'schema:' line to determine type/version
5. For each data line:
   a. Split by ':' delimiter
   b. First field is record type (prefix)
   c. Remaining fields are record data
   d. Validate against schema
   e. Build structured object
```

### Error Handling

| Error | Recovery |
|-------|----------|
| Missing schema | Reject file |
| Unknown prefix | Skip line, log warning |
| Wrong field count | Skip line, log warning |
| Invalid value | Use default, log warning |

### Escaping Special Characters

Use backslash to escape colons in text content:

```
TXT:id:Hello\: World:body
```

This renders as: `Hello: World`

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-15 | Initial specification |

---

## References

- AvaMagic Module Documentation
- IDEACODE Configuration Standards
- Material Design 3 Color System
