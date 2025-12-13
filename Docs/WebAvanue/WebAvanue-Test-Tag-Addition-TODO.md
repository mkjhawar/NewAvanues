# Test Tag Addition - Manual TODO

**File:** `/Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/settings/SettingsScreen.kt`

**Line:** ~1137

---

## Current Code

```kotlin
                // Category list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
```

## Required Change

```kotlin
                // Category list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .testTag("category_list"),  // ← ADD THIS LINE
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
```

---

## Note

This tag is needed for the landscape layout test:

```kotlin
@Test
fun landscapeLayout_showsCategoryNavigation() {
    // Verify category list exists
    composeTestRule.onNodeWithTag("category_list")
        .assertIsDisplayed()
}
```

**Why not added automatically:** File linter was actively modifying the file during edit attempts.

**Action required:** Add manually when file is stable, or disable linter temporarily.
