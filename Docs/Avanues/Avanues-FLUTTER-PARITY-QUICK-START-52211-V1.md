# Flutter Parity Components - Quick Start Guide

**Get productive in 5 minutes**

**Version:** 3.0.0-flutter-parity
**Last Updated:** 2025-11-22
**Target Audience:** Developers (all levels)

---

## 🚀 5-Minute Quick Start

### Step 1: Project Setup (30 seconds)

```bash
# Clone or create AVAMagic project
cd /path/to/your/project

# Add Flutter Parity dependency to build.gradle.kts
dependencies {
    implementation("com.augmentalis:avaelements-flutter-parity:3.0.0")
}
```

### Step 2: Import Components (10 seconds)

```kotlin
import com.augmentalis.avaelements.flutter.animation.*
import com.augmentalis.avaelements.flutter.layout.*
import com.augmentalis.avaelements.flutter.material.chips.*
import com.augmentalis.avaelements.flutter.material.lists.*
```

### Step 3: Use Your First Component (2 minutes)

```kotlin
@Composable
fun MyFirstFlutterParityComponent() {
    var selected by remember { mutableStateOf(false) }

    AnimatedContainer(
        duration = Duration.milliseconds(300),
        width = if (selected) Size.dp(200f) else Size.dp(100f),
        height = if (selected) Size.dp(200f) else Size.dp(100f),
        color = if (selected) Colors.Blue else Colors.Red,
        curve = Curves.EaseInOut,
        child = Text("Tap Me"),
        onClick = { selected = !selected }
    )
}
```

**Done!** You're now using Flutter Parity components! 🎉

---

## 📚 Component Quick Reference

### Animations (8 components)

Copy-paste these examples and run immediately:

#### 1. AnimatedContainer - Smooth Property Changes

```kotlin
var big by remember { mutableStateOf(false) }

AnimatedContainer(
    duration = Duration.milliseconds(300),
    width = if (big) Size.dp(200f) else Size.dp(100f),
    color = if (big) Colors.Blue else Colors.Gray,
    child = Text("Click to expand")
)
```

**When to use:** Expanding cards, color changes, size transitions

---

#### 2. Hero - Shared Element Transitions

```kotlin
// Screen 1
Hero(
    tag = "profile-photo",
    child = Image("avatar.png", size = Size.dp(50f))
)

// Screen 2
Hero(
    tag = "profile-photo",  // Same tag!
    child = Image("avatar.png", size = Size.dp(200f))
)
```

**When to use:** Image galleries, product details, profile expansion

---

#### 3. FadeTransition - Fade In/Out

```kotlin
val animationController = rememberAnimationController()
val fadeAnimation = Tween(begin = 0f, end = 1f)
    .animate(animationController)

FadeTransition(
    opacity = fadeAnimation,
    child = Image("logo.png")
)
```

**When to use:** Loading indicators, success messages, tooltips

---

### Layouts (10 components)

#### 4. Wrap - Tag Clouds

```kotlin
Wrap(
    spacing = Spacing.all(8f),
    runSpacing = Spacing.all(4f),
    children = listOf(
        Chip("Flutter"),
        Chip("Kotlin"),
        Chip("Android"),
        Chip("iOS"),
        Chip("Web")
    )
)
```

**When to use:** Tags, chips, dynamic content, filters

---

#### 5. Expanded - Responsive Layouts

```kotlin
Row {
    Expanded(
        flex = 2,
        child = Button("Takes 2/3 width")
    )
    Expanded(
        flex = 1,
        child = Button("Takes 1/3 width")
    )
}
```

**When to use:** Sidebar layouts, responsive columns, dashboard panels

---

#### 6. SizedBox - Fixed Dimensions or Spacing

```kotlin
Column {
    Text("Title")
    SizedBox(height = Size.dp(16f))  // Spacer
    Text("Subtitle")
}

// Or fixed size container
SizedBox(
    width = Size.dp(100f),
    height = Size.dp(100f),
    child = Image("logo.png")
)
```

**When to use:** Spacing, fixed-size containers, avatars

---

### Scrolling (7 components)

#### 7. ListView.builder - Efficient Long Lists

```kotlin
ListViewBuilder(
    itemCount = 1000,
    itemBuilder = { index ->
        ListTile(
            title = "Item $index",
            leading = Icon("star"),
            onTap = { handleTap(index) }
        )
    }
)
```

**When to use:** Product catalogs, news feeds, contact lists, any list with 50+ items

---

#### 8. GridView.builder - Photo Grids

```kotlin
GridViewBuilder(
    itemCount = 100,
    gridDelegate = SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount = 3,  // 3 columns
        mainAxisSpacing = 8f,
        crossAxisSpacing = 8f
    ),
    itemBuilder = { index ->
        Card(child = Image("photo_$index.jpg"))
    }
)
```

**When to use:** Photo galleries, product grids, app launchers

---

#### 9. PageView - Swipeable Pages

```kotlin
PageView(
    children = listOf(
        OnboardingPage1(),
        OnboardingPage2(),
        OnboardingPage3()
    ),
    onPageChanged = { page ->
        println("Now on page $page")
    }
)
```

**When to use:** Onboarding, image carousels, step-by-step wizards

---

### Material Design (18 components)

#### 10. FilterChip - Multi-Select Filters

```kotlin
var categories by remember { mutableStateOf(setOf<String>()) }

Wrap {
    FilterChip(
        label = "Electronics",
        selected = "electronics" in categories,
        onSelected = { selected ->
            categories = if (selected) {
                categories + "electronics"
            } else {
                categories - "electronics"
            }
        }
    )
    // Add more FilterChips...
}
```

**When to use:** Filtering, tags, multi-select options

---

#### 11. ExpansionTile - Collapsible Menus

```kotlin
ExpansionTile(
    title = "Settings",
    leading = "settings",
    children = listOf(
        ListTile(title = "Notifications"),
        ListTile(title = "Privacy"),
        ListTile(title = "Account")
    )
)
```

**When to use:** Settings, FAQs, nested navigation

---

#### 12. PopupMenuButton - Context Menus

```kotlin
PopupMenuButton(
    icon = "more_vert",
    items = listOf(
        PopupMenuItem(value = "edit", label = "Edit", icon = "edit"),
        PopupMenuItem(value = "delete", label = "Delete", icon = "delete"),
        PopupMenuItem(value = "share", label = "Share", icon = "share")
    ),
    onSelected = { value ->
        when (value) {
            "edit" -> editItem()
            "delete" -> deleteItem()
            "share" -> shareItem()
        }
    }
)
```

**When to use:** Options menu, context actions, overflow menu

---

## 🎯 Common Recipes

### Recipe 1: Animated Expanding Card (2 minutes)

```kotlin
@Composable
fun ExpandingCard() {
    var expanded by remember { mutableStateOf(false) }

    AnimatedContainer(
        duration = Duration.milliseconds(300),
        width = if (expanded) Size.dp(300f) else Size.dp(150f),
        height = if (expanded) Size.dp(400f) else Size.dp(200f),
        curve = Curves.EaseInOut,
        decoration = BoxDecoration(
            color = Colors.White,
            borderRadius = BorderRadius.circular(Size.dp(16f)),
            boxShadow = listOf(BoxShadow(blurRadius = 10f))
        ),
        onClick = { expanded = !expanded }
    ) {
        Column {
            Image("product.jpg")
            Text("Product Name")
            if (expanded) {
                Text("Full description goes here...")
                Button("Add to Cart")
            }
        }
    }
}
```

**Result:** Professional expanding product card!

---

### Recipe 2: Infinite Scrolling Feed (3 minutes)

```kotlin
@Composable
fun NewsFeed() {
    var items by remember { mutableStateOf(loadInitialItems()) }
    var loading by remember { mutableStateOf(false) }

    RefreshIndicator(
        onRefresh = {
            items = loadInitialItems()
        }
    ) {
        ListViewBuilder(
            itemCount = items.size + 1,  // +1 for loading indicator
            itemBuilder = { index ->
                if (index < items.size) {
                    NewsCard(items[index])
                } else {
                    // Load more when reaching end
                    LaunchedEffect(Unit) {
                        if (!loading) {
                            loading = true
                            items += loadMoreItems()
                            loading = false
                        }
                    }
                    CircularProgressIndicator()
                }
            }
        )
    }
}
```

**Result:** Instagram-style infinite feed with pull-to-refresh!

---

### Recipe 3: Category Filter System (4 minutes)

```kotlin
@Composable
fun ProductFilterScreen() {
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var products by remember { mutableStateOf(allProducts) }

    Column {
        // Filter Chips
        Wrap(
            spacing = Spacing.all(8f),
            runSpacing = Spacing.all(4f)
        ) {
            FilterChip(
                label = "Electronics",
                selected = "electronics" in selectedCategories,
                onSelected = { toggleCategory("electronics") }
            )
            FilterChip(
                label = "Books",
                selected = "books" in selectedCategories,
                onSelected = { toggleCategory("books") }
            )
            FilterChip(
                label = "Clothing",
                selected = "clothing" in selectedCategories,
                onSelected = { toggleCategory("clothing") }
            )
        }

        // Product Grid
        GridViewBuilder(
            itemCount = products.size,
            gridDelegate = SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount = 2,
                mainAxisSpacing = 16f,
                crossAxisSpacing = 16f
            ),
            itemBuilder = { index ->
                ProductCard(products[index])
            }
        )
    }

    // Update products when filters change
    LaunchedEffect(selectedCategories) {
        products = if (selectedCategories.isEmpty()) {
            allProducts
        } else {
            allProducts.filter { it.category in selectedCategories }
        }
    }

    fun toggleCategory(category: String) {
        selectedCategories = if (category in selectedCategories) {
            selectedCategories - category
        } else {
            selectedCategories + category
        }
    }
}
```

**Result:** Amazon-style multi-category filter with real-time updates!

---

### Recipe 4: Onboarding Flow with Page Indicators (3 minutes)

```kotlin
@Composable
fun OnboardingFlow() {
    var currentPage by remember { mutableStateOf(0) }
    val pages = listOf(
        OnboardingPage(
            image = "welcome.png",
            title = "Welcome to MyApp",
            description = "Your journey starts here"
        ),
        OnboardingPage(
            image = "features.png",
            title = "Amazing Features",
            description = "Discover what we offer"
        ),
        OnboardingPage(
            image = "start.png",
            title = "Get Started",
            description = "Let's begin!"
        )
    )

    Column {
        // Page View
        PageView(
            children = pages.map { page ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(page.image, width = Size.dp(200f))
                    Spacer(height = Size.dp(32f))
                    Text(page.title, style = TextStyle.headlineLarge)
                    Spacer(height = Size.dp(16f))
                    Text(page.description, style = TextStyle.bodyMedium)
                }
            },
            onPageChanged = { page -> currentPage = page }
        )

        // Page Indicators
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            pages.indices.forEach { index ->
                AnimatedContainer(
                    duration = Duration.milliseconds(300),
                    width = if (index == currentPage) Size.dp(24f) else Size.dp(8f),
                    height = Size.dp(8f),
                    decoration = BoxDecoration(
                        color = if (index == currentPage) Colors.Blue else Colors.Gray,
                        borderRadius = BorderRadius.circular(Size.dp(4f))
                    )
                )
                if (index < pages.size - 1) {
                    Spacer(width = Size.dp(8f))
                }
            }
        }

        // Skip / Next / Done Button
        Button(
            text = when (currentPage) {
                pages.size - 1 -> "Get Started"
                else -> "Next"
            },
            onPressed = {
                if (currentPage == pages.size - 1) {
                    completeOnboarding()
                } else {
                    currentPage++
                }
            }
        )
    }
}
```

**Result:** Professional onboarding flow with animated indicators!

---

## 🎨 Voice DSL Examples

All components also work with Voice DSL for ultra-compact code:

### Animated Container

```
AnimatedContainer {
  duration: 300ms
  width: selected ? 200 : 100
  height: selected ? 200 : 100
  color: selected ? blue : red
  curve: easeInOut

  Text "Tap Me"
}
```

### Hero Transition

```
Hero tag="product-1" {
  Image url="photo.jpg" size=200
}
```

### Filter Chips

```
Wrap spacing=8 runSpacing=4 {
  FilterChip "Electronics" selected=true
  FilterChip "Books"
  FilterChip "Clothing"
}
```

### ListView

```
ListView itemCount=1000 builder {
  ListTile {
    title: "Item $index"
    leading: Icon "star"
    onTap: "handleTap($index)"
  }
}
```

---

## 🔧 Troubleshooting

### Issue 1: "Unresolved reference: AnimatedContainer"

**Solution:**
```kotlin
// Add import
import com.augmentalis.avaelements.flutter.animation.AnimatedContainer
```

### Issue 2: "Type mismatch: inferred type is Double, required Size"

**Solution:**
```kotlin
// Wrong
width = 200.0

// Correct
width = Size.dp(200f)  // Note the 'f' for Float
```

### Issue 3: "None of the following functions can be called with arguments supplied"

**Solution:**
```kotlin
// Wrong: Using colon
Button(text: "Click")

// Correct: Using equals
Button(text = "Click")
```

### Issue 4: "Hero transition not working"

**Solution:**
- Ensure **both** screens have Hero with the **same tag**
- Hero tags must be unique per screen
- Navigation must use AVAMagic's navigation library

---

## 📖 Next Steps

### Beginner Path
1. Try all 12 quick examples above (30 minutes)
2. Build Recipe 1 (Expanding Card) from scratch (15 minutes)
3. Build your first complete screen using 3-4 components (1 hour)

### Intermediate Path
1. Build all 4 recipes (1-2 hours)
2. Combine recipes into a complete app (2-3 hours)
3. Add voice commands to your components (30 minutes)

### Advanced Path
1. Read full [Developer Manual Chapter](/docs/manuals/DEVELOPER-MANUAL-FLUTTER-PARITY-CHAPTER.md)
2. Explore [Migration Guide](/docs/FLUTTER-TO-AVAMAGIC-MIGRATION.md)
3. Build custom renderers for new platforms

---

## 📚 Resources

### Documentation
- **Full API Reference:** [Developer Manual Chapter 30](/docs/manuals/DEVELOPER-MANUAL-FLUTTER-PARITY-CHAPTER.md)
- **User Guide:** [User Manual Chapter 18](/docs/manuals/USER-MANUAL-ADVANCED-COMPONENTS-CHAPTER.md)
- **Migration Guide:** [Flutter to AVAMagic](/docs/FLUTTER-TO-AVAMAGIC-MIGRATION.md)

### Code Examples
- **Source Code:** `/Universal/Libraries/AvaElements/components/flutter-parity/`
- **Tests:** `/Universal/Libraries/AvaElements/components/flutter-parity/src/commonTest/`
- **Renderers:** `/Universal/Libraries/AvaElements/Renderers/Android/.../flutterparity/`

### Support
- **Discord:** discord.gg/avamagic
- **Email:** support@avamagic.io
- **GitHub Issues:** github.com/augmentalis/avamagic/issues

---

## ✅ Quick Checklist

Use this to verify you're ready to build with Flutter Parity components:

- [ ] Project setup complete (dependencies added)
- [ ] Imports working (no errors)
- [ ] First component renders (AnimatedContainer example)
- [ ] Understand Size.dp() syntax
- [ ] Understand named parameters with `=`
- [ ] Can create Lists and Grids
- [ ] Can add Animations
- [ ] Can use Material Design components
- [ ] Voice DSL syntax understood (optional)

**All checked?** You're ready to build amazing UIs! 🚀

---

**Document Status:** ✅ COMPLETE
**Last Updated:** 2025-11-22
**Maintained By:** Manoj Jhawar (manoj@ideahq.net)
**Estimated Reading Time:** 10 minutes
**Hands-On Time:** 30-60 minutes
