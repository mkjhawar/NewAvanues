# AvanueUI Module UI Requirements Specification

**Version:** 1.0
**Date:** 2026-02-09
**Scope:** Cross-cutting requirement for ALL Avanues modules
**Module:** AvanueUI (MasterDocs)

---

## 1. Responsive Layout Requirements

- Every screen MUST support both portrait AND landscape orientations.
- Use `BoxWithConstraints` or separate composable branches for orientation detection.
- NEVER hardcode widths/heights. Use `fillMaxWidth()`, `weight()`, or fraction-based sizing.
- Minimum supported width: 260dp (flip phone cover screen).

## 2. Foldable Device Requirements

- Screens using list-detail patterns MUST use `ListDetailPaneScaffold` (Material3 Adaptive) or `GroupedListDetailScaffold` (AvanueUI).
- Content MUST NOT render under hinge crease. Use `FoldableDeviceManager.shouldAvoidCrease()` for custom layouts.
- Test matrix targets: phone, tablet, foldable cover screen, foldable unfolded, half-fold tabletop.

## 3. Display Profile Integration

- All sizing MUST use `DisplayUtils.minTouchTarget` for touch targets (auto-scales per DisplayProfile).
- Icon sizes: use `AvanueTheme.displayProfile` to select appropriate dp values.
- Font scaling is handled automatically via `AvanueThemeProvider` density override.

## 4. Theming Requirements

- Import `AvanueTheme.colors.*` for all color references (NOT `MaterialTheme.colorScheme` directly for custom surfaces).
- Glass components: `com.augmentalis.avanueui.components.glass.*`
- Settings components: `com.augmentalis.avanueui.components.settings.*`
- Navigation patterns: `com.augmentalis.avanueui.components.navigation.*`
- BANNED imports: `com.avanueui.*`, `com.augmentalis.avamagic.ui.foundation.*`

## 5. Icon and Label Standards

- All toolbar/navigation icons MUST use consistent family (`Icons.Default.*` / `Icons.AutoMirrored.Filled.*`).
- All interactive icons MUST have text labels (below icon or beside).
- Minimum icon size: 20dp (portrait), 24dp (landscape/tablet).
- Minimum touch target: `DisplayUtils.minTouchTarget` (48dp on phone, scales on glass).

## 6. Command Bar / Toolbar Standards

- All toolbar actions MUST be contextual (disabled when inapplicable, not hidden).
- No duplicate actions across address bar and command bar.
- Support horizontal AND vertical layout via user preference.
- Minimum button spacing: 8dp between items.

## 7. Accessibility

- All interactive elements MUST have `contentDescription`.
- Touch targets >= 48dp (enforced by `DisplayUtils`).
- Color contrast: use AvanueTheme tokens which guarantee WCAG AA compliance.

## 8. Testing Matrix

| Device | Width | Profile | Layout |
|--------|-------|---------|--------|
| Phone portrait | ~360dp | PHONE | Single pane |
| Phone landscape | ~640dp | PHONE | Single pane / adapted |
| Tablet portrait | ~800dp | TABLET | Two pane |
| Tablet landscape | ~1200dp | TABLET | Two pane |
| Fold cover | ~300dp | PHONE | Single pane compact |
| Fold unfolded | ~900dp | TABLET | Two pane, hinge-aware |
| Fold tabletop | ~900dp | TABLET | Top content visible |
| Flip cover | ~260dp | PHONE | Compact single pane |
