# AvaElements v3.0 Package Restructure - Quick Start

**TL;DR:** Generic layout components get NO prefix. Branded interactive components get Magic* prefix.

---

## Quick Reference

### Layout Components (No Prefix)

```kotlin
import com.augmentalis.avaelements.layout.*

Row { }        // NOT MagicRow
Column { }     // NOT MagicColumn
Container { }  // NOT MagicContainer
Stack { }      // NOT MagicStack
```

### Magic Components (Magic* Prefix)

```kotlin
import com.augmentalis.avaelements.magic.buttons.*
import com.augmentalis.avaelements.magic.tags.*

MagicButton(text = "Click")     // NOT Button
MagicTag(label = "Kotlin")      // NOT Chip
MagicTextField(hint = "Name")   // NOT TextField
MagicCard { }                   // NOT Card
```

---

## Before → After

```kotlin
// BEFORE
import com.augmentalis.avaelements.components.phase1.layout.Row
import com.augmentalis.avaelements.components.phase1.form.Button
import com.augmentalis.avaelements.components.phase3.display.Chip

Row {
    Button(text = "Submit")
    Chip(label = "Kotlin")
}

// AFTER
import com.augmentalis.avaelements.layout.*
import com.augmentalis.avaelements.magic.buttons.*
import com.augmentalis.avaelements.magic.tags.*

Row {  // Layout - no prefix
    MagicButton(text = "Submit")  // Magic component
    MagicTag(label = "Kotlin")    // Magic component
}
```

---

## Run Migration

```bash
# Preview changes
./scripts/migrate-packages-v3.sh --dry-run --verbose

# Migrate with backup
./scripts/migrate-packages-v3.sh --backup

# Platform-specific
./scripts/migrate-packages-v3.sh --platform android --backup
```

---

## Component Name Changes

| Old | New | Package |
|-----|-----|---------|
| `Button` | `MagicButton` | `magic.buttons` |
| `TextField` | `MagicTextField` | `magic.inputs` |
| `Chip` | `MagicTag` | `magic.tags` |
| `Card` | `MagicCard` | `magic.cards` |
| `Row` | `Row` | `layout` |
| `Column` | `Column` | `layout` |
| `Container` | `Container` | `layout` |

---

## Key Files

- **Guide:** `/docs/PACKAGE-STRUCTURE-GUIDE.md`
- **Analysis:** `/docs/PACKAGE-RESTRUCTURE-ANALYSIS.md`
- **Summary:** `/docs/PACKAGE-RESTRUCTURE-SUMMARY.md`
- **Script:** `/scripts/migrate-packages-v3.sh`
- **Prototype:** `/components/unified/`

---

## Full Documentation

See `/docs/PACKAGE-STRUCTURE-GUIDE.md` for complete migration guide.

---

**Version:** 3.0.0 | **Status:** Ready for Migration
