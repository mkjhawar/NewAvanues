# AvaElements Unified Components v3.0

Clean, logical package structure separating **generic layout** from **branded Magic** components.

---

## Structure

```
unified/
├── build.gradle.kts                    # Multi-platform build config
│
└── src/
    ├── commonMain/kotlin/com/augmentalis/avaelements/
    │   │
    │   ├── layout/                     # Generic layout (NO prefix)
    │   │   ├── LayoutComponents.kt     # Package documentation
    │   │   ├── Row.kt                  ✅ Migrated
    │   │   ├── Column.kt               ✅ Migrated
    │   │   ├── Container.kt            ✅ Migrated
    │   │   ├── Stack.kt                ⏳ Pending
    │   │   ├── Padding.kt              ⏳ Pending
    │   │   ├── Align.kt                ⏳ Pending
    │   │   ├── Center.kt               ⏳ Pending
    │   │   ├── SizedBox.kt             ⏳ Pending
    │   │   ├── Flexible.kt             ⏳ Pending
    │   │   ├── Expanded.kt             ⏳ Pending
    │   │   ├── Positioned.kt           ⏳ Pending
    │   │   ├── FittedBox.kt            ⏳ Pending
    │   │   ├── Wrap.kt                 ⏳ Pending
    │   │   ├── Spacer.kt               ⏳ Pending
    │   │   ├── Grid.kt                 ⏳ Pending
    │   │   └── ConstrainedBox.kt       ⏳ Pending
    │   │
    │   └── magic/                      # Branded components (Magic* prefix)
    │       ├── MagicComponents.kt      # Package documentation
    │       │
    │       ├── tags/                   # Tag components
    │       │   ├── MagicTag.kt         ⏳ Pending
    │       │   ├── MagicInput.kt       ⏳ Pending
    │       │   ├── MagicFilter.kt      ⏳ Pending
    │       │   ├── MagicChoice.kt      ⏳ Pending
    │       │   └── MagicAction.kt      ⏳ Pending
    │       │
    │       ├── buttons/                # Button components
    │       │   ├── MagicButton.kt      ✅ Migrated
    │       │   ├── MagicIconButton.kt  ⏳ Pending
    │       │   └── MagicFloatingButton.kt ⏳ Pending
    │       │
    │       ├── cards/                  # Card components
    │       │   └── MagicCard.kt        ⏳ Pending
    │       │
    │       ├── inputs/                 # Input components
    │       │   ├── MagicTextField.kt   ⏳ Pending
    │       │   ├── MagicCheckbox.kt    ⏳ Pending
    │       │   ├── MagicSwitch.kt      ⏳ Pending
    │       │   ├── MagicRadioButton.kt ⏳ Pending
    │       │   ├── MagicSlider.kt      ⏳ Pending
    │       │   └── ...                 ⏳ Pending
    │       │
    │       ├── display/                # Display components
    │       │   ├── MagicText.kt        ⏳ Pending
    │       │   ├── MagicImage.kt       ⏳ Pending
    │       │   ├── MagicIcon.kt        ⏳ Pending
    │       │   ├── MagicAvatar.kt      ⏳ Pending
    │       │   └── ...                 ⏳ Pending
    │       │
    │       ├── navigation/             # Navigation components
    │       │   ├── MagicAppBar.kt      ⏳ Pending
    │       │   ├── MagicBottomNav.kt   ⏳ Pending
    │       │   ├── MagicDrawer.kt      ⏳ Pending
    │       │   └── ...                 ⏳ Pending
    │       │
    │       ├── feedback/               # Feedback components
    │       │   ├── MagicAlert.kt       ⏳ Pending
    │       │   ├── MagicModal.kt       ⏳ Pending
    │       │   ├── MagicToast.kt       ⏳ Pending
    │       │   └── ...                 ⏳ Pending
    │       │
    │       ├── lists/                  # List components
    │       │   ├── MagicList.kt        ⏳ Pending
    │       │   ├── MagicListTile.kt    ⏳ Pending
    │       │   └── ...                 ⏳ Pending
    │       │
    │       └── animation/              # Animation components
    │           ├── MagicAnimatedOpacity.kt ⏳ Pending
    │           ├── MagicAnimatedContainer.kt ⏳ Pending
    │           └── ...                 ⏳ Pending
    │
    ├── androidMain/kotlin/             # Android-specific code
    ├── iosMain/kotlin/                 # iOS-specific code
    ├── desktopMain/kotlin/             # Desktop-specific code
    ├── commonTest/kotlin/              # Common tests
    └── androidTest/kotlin/             # Android tests
```

---

## Usage

### Layout Components (No Prefix)

```kotlin
import com.augmentalis.avaelements.layout.*

Container {
    Row {
        Column {
            // Layout content
        }
    }
}
```

### Magic Components (Magic* Prefix)

```kotlin
import com.augmentalis.avaelements.magic.buttons.*
import com.augmentalis.avaelements.magic.tags.*
import com.augmentalis.avaelements.magic.inputs.*

Column {
    MagicButton(text = "Submit")
    MagicTextField(hint = "Enter name")
    Row {
        MagicTag(label = "Kotlin")
        MagicTag(label = "Compose")
    }
}
```

---

## Status

- ✅ **Structure created**
- ✅ **Build config ready**
- ✅ **4 prototype components** (Row, Column, Container, MagicButton)
- ⏳ **~76 components pending** migration
- ⏳ **Renderers not yet updated**
- ⏳ **Tests not yet migrated**

---

## Next Steps

1. Migrate remaining components from phase1, phase3, flutter-parity
2. Update all renderers (Android, iOS, Web, Desktop)
3. Update tests
4. Update build dependencies

---

## Documentation

- **Migration Guide:** `/docs/PACKAGE-STRUCTURE-GUIDE.md`
- **Analysis:** `/docs/PACKAGE-RESTRUCTURE-ANALYSIS.md`
- **Summary:** `/docs/PACKAGE-RESTRUCTURE-SUMMARY.md`
- **Quick Start:** `/docs/PACKAGE-RESTRUCTURE-QUICKSTART.md`

---

## Migration Script

```bash
# Preview migration
./scripts/migrate-packages-v3.sh --dry-run

# Execute migration
./scripts/migrate-packages-v3.sh --backup
```

---

**Version:** 3.0.0 | **Status:** Prototype Complete
