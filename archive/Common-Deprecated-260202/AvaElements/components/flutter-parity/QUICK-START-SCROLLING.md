# Quick Start Guide - Flutter Parity Scrolling Components

**Version:** 2.1.0
**Last Updated:** 2025-11-22

---

## Table of Contents
1. [Installation](#installation)
2. [ListView.builder](#listviewbuilder)
3. [GridView.builder](#gridviewbuilder)
4. [ListView.separated](#listviewseparated)
5. [PageView](#pageview)
6. [ReorderableListView](#reorderablelistview)
7. [CustomScrollView](#customscrollview)
8. [Slivers](#slivers)
9. [Common Patterns](#common-patterns)
10. [Performance Tips](#performance-tips)

---

## Installation

### Gradle Setup

Add to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":Universal:Libraries:AvaElements:components:flutter-parity"))
}
```

For Android rendering:

```kotlin
dependencies {
    implementation(project(":Universal:Libraries:AvaElements:Renderers:Android"))
}
```

---

## ListView.builder

### Basic Usage

```kotlin
// Simple list
ListViewBuilderComponent(
    itemCount = 100,
    itemBuilder = "myItemBuilder"
)
```

### With Scroll Controller

```kotlin
val controller = ScrollController(
    initialScrollOffset = 0f,
    keepScrollOffset = true
)

ListViewBuilderComponent(
    itemCount = 100,
    itemBuilder = "myItemBuilder",
    controller = controller
)
```

### Horizontal Scrolling

```kotlin
ListViewBuilderComponent(
    itemCount = 50,
    itemBuilder = "myItemBuilder",
    scrollDirection = ScrollDirection.Horizontal
)
```

### Infinite Scrolling

```kotlin
ListViewBuilderComponent(
    itemCount = null,  // null = infinite
    itemBuilder = "myItemBuilder"
)
```

### Fixed Item Height (Performance Optimization)

```kotlin
ListViewBuilderComponent(
    itemCount = 10000,
    itemBuilder = "myItemBuilder",
    itemExtent = 50f  // All items are 50dp tall
)
```

### Flutter Equivalent

```dart
ListView.builder(
  itemCount: 100,
  itemBuilder: (context, index) {
    return ListTile(title: Text('Item $index'));
  },
)
```

---

## GridView.builder

### Fixed Column Count

```kotlin
val delegate = SliverGridDelegate.WithFixedCrossAxisCount(
    crossAxisCount = 3,
    mainAxisSpacing = 8f,
    crossAxisSpacing = 8f,
    childAspectRatio = 1.0f
)

GridViewBuilderComponent(
    gridDelegate = delegate,
    itemCount = 100,
    itemBuilder = "myGridItemBuilder"
)
```

### Adaptive Sizing

```kotlin
val delegate = SliverGridDelegate.WithMaxCrossAxisExtent(
    maxCrossAxisExtent = 150f,  // Max width per tile
    mainAxisSpacing = 4f,
    crossAxisSpacing = 4f
)

GridViewBuilderComponent(
    gridDelegate = delegate,
    itemCount = 50,
    itemBuilder = "myGridItemBuilder"
)
```

### Flutter Equivalent

```dart
GridView.builder(
  gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
    crossAxisCount: 3,
    mainAxisSpacing: 8.0,
    crossAxisSpacing: 8.0,
  ),
  itemCount: 100,
  itemBuilder: (context, index) {
    return Card(child: Center(child: Text('$index')));
  },
)
```

---

## ListView.separated

### Basic Usage with Dividers

```kotlin
ListViewSeparatedComponent(
    itemCount = 20,
    itemBuilder = "myItemBuilder",
    separatorBuilder = "myDividerBuilder"  // Called n-1 times
)
```

### Flutter Equivalent

```dart
ListView.separated(
  itemCount: 20,
  itemBuilder: (context, index) {
    return ListTile(title: Text('Item $index'));
  },
  separatorBuilder: (context, index) {
    return Divider();
  },
)
```

---

## PageView

### Horizontal Pages

```kotlin
PageViewComponent(
    itemBuilder = "myPageBuilder",
    itemCount = 5
)
```

### Vertical Pages

```kotlin
PageViewComponent(
    itemBuilder = "myPageBuilder",
    itemCount = 5,
    scrollDirection = ScrollDirection.Vertical
)
```

### With Page Controller (Preview Effect)

```kotlin
val controller = PageController(
    initialPage = 1,
    viewportFraction = 0.8f  // Show 80% of page, preview others
)

PageViewComponent(
    controller = controller,
    itemBuilder = "myPageBuilder",
    itemCount = 10
)
```

### Infinite Pages

```kotlin
PageViewComponent(
    itemBuilder = "myPageBuilder",
    itemCount = -1  // -1 = infinite
)
```

### Flutter Equivalent

```dart
PageView.builder(
  itemCount: 5,
  itemBuilder: (context, index) {
    return Container(
      color: colors[index],
      child: Center(child: Text('Page $index')),
    );
  },
)
```

---

## ReorderableListView

### Basic Drag-to-Reorder

```kotlin
ReorderableListViewComponent(
    itemCount = 10,
    itemBuilder = "myItemBuilder",
    onReorder = "handleReorder"  // (oldIndex, newIndex) -> Unit
)
```

### With Custom Drag Handles

```kotlin
ReorderableListViewComponent(
    itemCount = 10,
    itemBuilder = "myItemBuilder",
    onReorder = "handleReorder",
    buildDefaultDragHandles = false  // Use custom handles
)
```

### Reorder Handler Example

```kotlin
// In your handler:
fun handleReorder(oldIndex: Int, newIndex: Int) {
    var adjustedNewIndex = newIndex
    if (oldIndex < newIndex) {
        adjustedNewIndex -= 1
    }
    val item = items.removeAt(oldIndex)
    items.add(adjustedNewIndex, item)
}
```

### Flutter Equivalent

```dart
ReorderableListView(
  children: items.map((item) => ListTile(
    key: Key(item.id),
    title: Text(item.title),
  )).toList(),
  onReorder: (oldIndex, newIndex) {
    if (oldIndex < newIndex) newIndex -= 1;
    final item = items.removeAt(oldIndex);
    items.insert(newIndex, item);
  },
)
```

---

## CustomScrollView

### Basic Sliver Composition

```kotlin
CustomScrollViewComponent(
    slivers = listOf(
        SliverList(
            delegate = SliverChildDelegate.Builder(
                builder = "myItemBuilder",
                childCount = 50
            )
        )
    )
)
```

### App Bar + List

```kotlin
CustomScrollViewComponent(
    slivers = listOf(
        SliverAppBar(
            title = "My App",
            pinned = true,
            expandedHeight = 200f
        ),
        SliverList(
            delegate = SliverChildDelegate.Builder(
                builder = "myItemBuilder",
                childCount = 100
            )
        )
    )
)
```

### Multiple Content Types

```kotlin
CustomScrollViewComponent(
    slivers = listOf(
        SliverToBoxAdapter(child = "header"),
        SliverList(/* ... */),
        SliverGrid(/* ... */),
        SliverFillRemaining(child = "footer")
    )
)
```

### Flutter Equivalent

```dart
CustomScrollView(
  slivers: [
    SliverAppBar(
      title: Text('My App'),
      pinned: true,
      expandedHeight: 200.0,
    ),
    SliverList(
      delegate: SliverChildBuilderDelegate(
        (context, index) => ListTile(title: Text('Item $index')),
        childCount: 100,
      ),
    ),
  ],
)
```

---

## Slivers

### SliverList

```kotlin
SliverList(
    delegate = SliverChildDelegate.Builder(
        builder = "myItemBuilder",
        childCount = 50
    )
)
```

### SliverGrid

```kotlin
SliverGrid(
    gridDelegate = SliverGridDelegate.WithFixedCrossAxisCount(
        crossAxisCount = 3
    ),
    delegate = SliverChildDelegate.Builder(
        builder = "myGridItemBuilder",
        childCount = 30
    )
)
```

### SliverFixedExtentList (Performance Optimized)

```kotlin
SliverFixedExtentList(
    itemExtent = 50f,  // All items 50dp tall
    delegate = SliverChildDelegate.Builder(
        builder = "myItemBuilder",
        childCount = 1000
    )
)
```

### SliverAppBar (Collapsing Header)

```kotlin
SliverAppBar(
    title = "My Title",
    expandedHeight = 200f,
    floating = true,   // Shows on scroll up
    pinned = true,     // Stays at top
    snap = true        // Snaps open/closed (requires floating)
)
```

### SliverToBoxAdapter (Single Widget)

```kotlin
SliverToBoxAdapter(
    child = "myHeaderWidget"
)
```

### SliverPadding

```kotlin
SliverPadding(
    padding = Spacing.all(16f),
    sliver = SliverList(/* ... */)
)
```

### SliverFillRemaining (Footer)

```kotlin
SliverFillRemaining(
    child = "myFooterWidget",
    hasScrollBody = false
)
```

---

## Common Patterns

### Pattern 1: Feed with Header

```kotlin
CustomScrollViewComponent(
    slivers = listOf(
        SliverToBoxAdapter(child = "feedHeader"),
        SliverList(
            delegate = SliverChildDelegate.Builder(
                builder = "feedItemBuilder",
                childCount = 100
            )
        )
    )
)
```

### Pattern 2: Photo Gallery

```kotlin
GridViewBuilderComponent(
    gridDelegate = SliverGridDelegate.WithFixedCrossAxisCount(
        crossAxisCount = 3,
        mainAxisSpacing = 2f,
        crossAxisSpacing = 2f
    ),
    itemCount = photos.size,
    itemBuilder = "photoTileBuilder"
)
```

### Pattern 3: Chat List

```kotlin
ListViewBuilderComponent(
    itemCount = messages.size,
    itemBuilder = "messageBuilder",
    reverse = true,  // Latest at bottom
    controller = scrollController
)
```

### Pattern 4: Settings Screen

```kotlin
ListViewSeparatedComponent(
    itemCount = settingsItems.size,
    itemBuilder = "settingsItemBuilder",
    separatorBuilder = "dividerBuilder"
)
```

### Pattern 5: Onboarding Screens

```kotlin
val pageController = PageController(
    initialPage = 0,
    viewportFraction = 0.9f  // Show peek of next page
)

PageViewComponent(
    controller = pageController,
    itemCount = onboardingPages.size,
    itemBuilder = "onboardingPageBuilder"
)
```

### Pattern 6: Expandable App Bar

```kotlin
CustomScrollViewComponent(
    slivers = listOf(
        SliverAppBar(
            title = "Profile",
            expandedHeight = 200f,
            flexibleSpace = FlexibleSpaceBar(
                title = "User Name",
                background = "profileHeaderImage",
                collapseMode = CollapseMode.Parallax
            ),
            pinned = true
        ),
        SliverList(/* user data */)
    )
)
```

---

## Performance Tips

### 1. Use itemExtent for uniform items

```kotlin
// GOOD - 20% faster scrolling
ListViewBuilderComponent(
    itemCount = 10000,
    itemBuilder = "builder",
    itemExtent = 50f  // All items same height
)

// AVOID - Slower for uniform items
ListViewBuilderComponent(
    itemCount = 10000,
    itemBuilder = "builder"
    // No itemExtent specified
)
```

### 2. Use SliverFixedExtentList for large sliver lists

```kotlin
// GOOD - Optimized for uniform items
SliverFixedExtentList(
    itemExtent = 50f,
    delegate = SliverChildDelegate.Builder(/* ... */)
)

// AVOID - Slower for uniform items
SliverList(
    delegate = SliverChildDelegate.Builder(/* ... */)
)
```

### 3. Use GridView adaptive sizing wisely

```kotlin
// GOOD - Scales with screen size
SliverGridDelegate.WithMaxCrossAxisExtent(
    maxCrossAxisExtent = 150f
)

// GOOD - Fixed, predictable
SliverGridDelegate.WithFixedCrossAxisCount(
    crossAxisCount = 3
)
```

### 4. Avoid shrinkWrap unless necessary

```kotlin
// GOOD - Better performance
ListViewBuilderComponent(
    itemCount = 100,
    itemBuilder = "builder",
    shrinkWrap = false  // Default
)

// AVOID - Use only when needed
ListViewBuilderComponent(
    itemCount = 100,
    itemBuilder = "builder",
    shrinkWrap = true  // Measures all items upfront
)
```

### 5. Use appropriate scroll physics

```kotlin
// For chat (always show scrollbar)
physics = ScrollPhysics.AlwaysScrollable

// For fixed content (disable scrolling)
physics = ScrollPhysics.NeverScrollable

// For natural feel (iOS-style bounce on iOS, clamp on Android)
physics = ScrollPhysics.Platform
```

### 6. Optimize reorderable lists

```kotlin
// GOOD - Up to 500 items
ReorderableListViewComponent(
    itemCount = 500,
    itemBuilder = "builder",
    onReorder = "handler"
)

// AVOID - Performance degrades above 500 items
ReorderableListViewComponent(
    itemCount = 5000,  // Too many!
    itemBuilder = "builder",
    onReorder = "handler"
)
```

### 7. Use PageView for large image galleries

```kotlin
// GOOD - Only renders current page + buffer
PageViewComponent(
    itemBuilder = "imagePageBuilder",
    itemCount = 1000
)

// AVOID - Loads all images upfront
CustomScrollViewComponent(
    slivers = listOf(
        SliverToBoxAdapter(child = "allImagesInOneWidget")
    )
)
```

---

## Troubleshooting

### Issue: List not scrolling

**Problem:** Set `physics = ScrollPhysics.NeverScrollable`

**Solution:**
```kotlin
physics = ScrollPhysics.AlwaysScrollable  // Default
```

### Issue: Separator count wrong

**Problem:** Expected n separators for n items

**Solution:** ListView.separated creates n-1 separators
```kotlin
// 10 items = 9 separators
ListViewSeparatedComponent(
    itemCount = 10,
    itemBuilder = "items",
    separatorBuilder = "separators"  // Called 9 times
)
```

### Issue: PageView requires both children and builder

**Problem:** Provided both `children` and `itemBuilder`

**Solution:** Use only one
```kotlin
// Either this
PageViewComponent(
    children = listOf("page1", "page2", "page3")
)

// OR this
PageViewComponent(
    itemBuilder = "builder",
    itemCount = 3
)

// NOT both!
```

### Issue: ReorderableListView items not unique

**Problem:** Items need unique keys

**Solution:** Ensure each item has a unique identifier
```kotlin
// In your item builder, provide unique keys
// This is handled by the renderer, but ensure data has unique IDs
```

### Issue: SliverAppBar snap not working

**Problem:** `snap = true` but `floating = false`

**Solution:**
```kotlin
SliverAppBar(
    title = "Title",
    floating = true,  // Required for snap
    snap = true
)
```

---

## Examples Repository

Complete working examples:
- `/examples/scrolling/ListViewExample.kt`
- `/examples/scrolling/GridViewExample.kt`
- `/examples/scrolling/PageViewExample.kt`
- `/examples/scrolling/ReorderableExample.kt`
- `/examples/scrolling/CustomScrollExample.kt`

---

## API Reference

Full API documentation:
- KDoc: See inline documentation in source files
- Performance: See `PERFORMANCE-BENCHMARK-REPORT.md`
- Deliverable: See `SCROLLING-COMPONENTS-DELIVERABLE.md`

---

## Support

For issues or questions:
1. Check inline KDoc documentation
2. Review test files for usage examples
3. Consult performance benchmark report
4. Contact AVA AI Development Team

---

**Version:** 2.1.0
**Last Updated:** 2025-11-22
**License:** Proprietary
