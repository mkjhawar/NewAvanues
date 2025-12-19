# Building Custom Components

**Version:** 2.0.0
**Last Updated:** 2025-11-13
**Difficulty:** Intermediate to Advanced

---

## Table of Contents

1. [Introduction](#introduction)
2. [Component Interface](#component-interface)
3. [Creating a Simple Component](#creating-a-simple-component)
4. [Creating a Complex Component](#creating-a-complex-component)
5. [Component Styling](#component-styling)
6. [State Management](#state-management)
7. [Platform-Specific Renderers](#platform-specific-renderers)
8. [Testing Custom Components](#testing-custom-components)
9. [Best Practices](#best-practices)
10. [Publishing Components](#publishing-components)

---

## Introduction

AvaElements allows you to create **custom components** that work seamlessly across all platforms (Android, iOS, Web).

### When to Create Custom Components

- **Reusable UI patterns** not covered by the 67 built-in components
- **Business-specific widgets** (e.g., ProductCard, OrderSummary)
- **Complex compositions** of existing components
- **Custom animations or interactions**

### Component Lifecycle

```
1. Define Component (data class)
   ↓
2. Implement Renderers (Android, iOS, Web)
   ↓
3. Register with Renderer
   ↓
4. Use in Your App
```

---

## Component Interface

All components must implement the `Component` interface:

```kotlin
interface Component {
    val id: String?                          // Optional unique identifier
    val style: ComponentStyle?               // Optional styling
    val modifiers: List<Modifier>            // Platform-agnostic modifiers

    fun render(renderer: Renderer): @Composable (() -> Unit)
}
```

**Key Concepts:**

- **id**: Optional identifier for testing/debugging
- **style**: Optional custom styling that overrides theme
- **modifiers**: Platform-agnostic layout modifiers
- **render()**: Returns a composable function when given a renderer

---

## Creating a Simple Component

### Example: StatusBadge

Let's create a `StatusBadge` component that shows order status.

**Step 1: Define the Component**

```kotlin
package com.myapp.components

import com.augmentalis.avaelements.core.*
import kotlinx.serialization.Transient

data class StatusBadge(
    override val id: String? = null,
    val status: String,  // "pending", "shipped", "delivered", "cancelled"
    val size: String = "medium",  // "small", "medium", "large"
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}
```

**Step 2: Create Android Renderer**

```kotlin
package com.myapp.renderers.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.avaelements.core.Theme
import com.myapp.components.StatusBadge

@Composable
fun RenderStatusBadge(c: StatusBadge, theme: Theme) {
    val (backgroundColor, textColor, label) = when (c.status) {
        "pending" -> Triple(
            androidx.compose.ui.graphics.Color(0xFFFFF4E5),  // Light orange
            androidx.compose.ui.graphics.Color(0xFFE65100),  // Dark orange
            "Pending"
        )
        "shipped" -> Triple(
            androidx.compose.ui.graphics.Color(0xFFE3F2FD),  // Light blue
            androidx.compose.ui.graphics.Color(0xFF1976D2),  // Dark blue
            "Shipped"
        )
        "delivered" -> Triple(
            androidx.compose.ui.graphics.Color(0xFFE8F5E9),  // Light green
            androidx.compose.ui.graphics.Color(0xFF2E7D32),  // Dark green
            "Delivered"
        )
        "cancelled" -> Triple(
            androidx.compose.ui.graphics.Color(0xFFFFEBEE),  // Light red
            androidx.compose.ui.graphics.Color(0xFFC62828),  // Dark red
            "Cancelled"
        )
        else -> Triple(
            androidx.compose.ui.graphics.Color(0xFFF5F5F5),  // Gray
            androidx.compose.ui.graphics.Color(0xFF757575),  // Dark gray
            c.status.capitalize()
        )
    }

    val padding = when (c.size) {
        "small" -> 4.dp to 8.dp   // vertical to horizontal
        "large" -> 12.dp to 24.dp
        else -> 8.dp to 16.dp     // medium (default)
    }

    val fontSize = when (c.size) {
        "small" -> 11.sp
        "large" -> 16.sp
        else -> 14.sp
    }

    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = padding.second, vertical = padding.first)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = fontSize,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}
```

**Step 3: Register with Renderer**

```kotlin
// In ComposeRenderer.kt
class ComposeRenderer(override val theme: Theme) : Renderer {
    override fun render(component: Component): @Composable (() -> Unit) = {
        when (component) {
            // ... existing components
            is StatusBadge -> RenderStatusBadge(component, theme)
            else -> { Text("Unknown: ${component::class.simpleName}") }
        }
    }
}
```

**Step 4: Use the Component**

```kotlin
val badge = StatusBadge(
    status = "shipped",
    size = "medium"
)

// Render
val renderer = ComposeRenderer()
badge.render(renderer)()
```

---

## Creating a Complex Component

### Example: ProductCard

Let's create a more complex component that combines multiple elements.

**Step 1: Define the Component**

```kotlin
package com.myapp.components

import com.augmentalis.avaelements.core.*
import kotlinx.serialization.Transient

data class ProductCard(
    override val id: String? = null,
    val productName: String,
    val price: Double,
    val imageUrl: String?,
    val rating: Float = 0f,  // 0.0 to 5.0
    val inStock: Boolean = true,
    val discount: Int? = null,  // Percentage discount (0-100)
    @Transient val onAddToCart: (() -> Unit)? = null,
    @Transient val onClick: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)

    // Helper computed properties
    val discountedPrice: Double
        get() = if (discount != null && discount > 0) {
            price * (1 - discount / 100.0)
        } else {
            price
        }

    val hasDiscount: Boolean
        get() = discount != null && discount > 0
}
```

**Step 2: Create Android Renderer**

```kotlin
package com.myapp.renderers.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.augmentalis.avaelements.core.Theme
import com.myapp.components.ProductCard
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RenderProductCard(c: ProductCard, theme: Theme) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { c.onClick?.invoke() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Image Section
            Box {
                c.imageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = c.productName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentScale = ContentScale.Crop
                    )
                } ?: run {
                    // Placeholder if no image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(androidx.compose.ui.graphics.Color(0xFFF5F5F5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Product image placeholder",
                            modifier = Modifier.size(64.dp),
                            tint = androidx.compose.ui.graphics.Color(0xFFBDBDBD)
                        )
                    }
                }

                // Discount Badge
                if (c.hasDiscount) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        color = androidx.compose.ui.graphics.Color(0xFFE53935),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "-${c.discount}%",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Product Name
            Text(
                text = c.productName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rating
            if (c.rating > 0f) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        modifier = Modifier.size(16.dp),
                        tint = androidx.compose.ui.graphics.Color(0xFFFFC107)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", c.rating),
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color(0xFF757575)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Price Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (c.hasDiscount) {
                    // Original price (crossed out)
                    Text(
                        text = formatPrice(c.price),
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color(0xFF757575),
                        textDecoration = TextDecoration.LineThrough
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Discounted price
                    Text(
                        text = formatPrice(c.discountedPrice),
                        style = MaterialTheme.typography.titleMedium,
                        color = androidx.compose.ui.graphics.Color(0xFFE53935),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                } else {
                    // Regular price
                    Text(
                        text = formatPrice(c.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add to Cart Button
            Button(
                onClick = { c.onAddToCart?.invoke() },
                modifier = Modifier.fillMaxWidth(),
                enabled = c.inStock
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add to cart",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (c.inStock) "Add to Cart" else "Out of Stock")
            }
        }
    }
}

private fun formatPrice(price: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    return formatter.format(price)
}
```

**Step 3: Usage**

```kotlin
val productCard = ProductCard(
    productName = "Wireless Bluetooth Headphones",
    price = 99.99,
    imageUrl = "https://example.com/headphones.jpg",
    rating = 4.5f,
    inStock = true,
    discount = 20,
    onAddToCart = {
        println("Added to cart!")
        // Add to cart logic
    },
    onClick = {
        println("Navigate to product details")
        // Navigation logic
    }
)

// Render
productCard.render(renderer)()
```

---

## Component Styling

### Using ComponentStyle

```kotlin
data class ComponentStyle(
    val backgroundColor: Color? = null,
    val color: Color? = null,           // Text/foreground color
    val fontSize: Float? = null,         // in SP
    val fontWeight: String? = null,      // "normal", "bold", "light"
    val padding: Float? = null,          // in DP
    val margin: Float? = null,           // in DP
    val borderRadius: Float? = null,     // in DP
    val elevation: Float? = null         // in DP (shadow/elevation)
)
```

**Example:**

```kotlin
val styledCard = ProductCard(
    productName = "Premium Product",
    price = 199.99,
    style = ComponentStyle(
        backgroundColor = Color.fromHex("#F5F5F5"),
        padding = 20f,
        borderRadius = 12f,
        elevation = 4f
    )
)
```

**Applying Style in Renderer:**

```kotlin
@Composable
fun RenderProductCard(c: ProductCard, theme: Theme) {
    val backgroundColor = c.style?.backgroundColor?.toCompose()
        ?: theme.colorScheme.surface.toCompose()

    val padding = c.style?.padding?.dp ?: 12.dp
    val borderRadius = c.style?.borderRadius?.dp ?: 8.dp
    val elevation = c.style?.elevation?.dp ?: 2.dp

    Card(
        modifier = Modifier.padding(padding),
        shape = RoundedCornerShape(borderRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        // ... rest of card content
    }
}
```

---

## State Management

### Internal State vs External State

**Internal State:** Managed by the renderer (e.g., expanded/collapsed)

```kotlin
@Composable
fun RenderExpandableCard(c: ExpandableCard, theme: Theme) {
    var expanded by remember { mutableStateOf(false) }

    Card(onClick = { expanded = !expanded }) {
        Column {
            Text(text = c.title)
            if (expanded) {
                Text(text = c.content)
            }
        }
    }
}
```

**External State:** Managed by the component/app

```kotlin
data class ExpandableCard(
    val title: String,
    val content: String,
    val expanded: Boolean = false,
    @Transient val onToggle: ((Boolean) -> Unit)? = null,
    // ...
) : Component

// Usage
var isExpanded by remember { mutableStateOf(false) }

ExpandableCard(
    title = "Click to expand",
    content = "Hidden content",
    expanded = isExpanded,
    onToggle = { newState -> isExpanded = newState }
)
```

### Best Practice: Prefer External State

External state makes components **testable, predictable, and reusable**.

---

## Platform-Specific Renderers

### Android (Jetpack Compose)

```kotlin
@Composable
fun RenderMyComponent(c: MyComponent, theme: Theme) {
    // Compose-specific implementation
}
```

### iOS (SwiftUI) - Template

```swift
struct RenderMyComponent: View {
    let component: MyComponent
    let theme: Theme

    var body: some View {
        // SwiftUI-specific implementation
    }
}
```

### Web (React) - Template

```typescript
function RenderMyComponent({ component, theme }: {
    component: MyComponent,
    theme: Theme
}) {
    // React-specific implementation
    return <div>...</div>;
}
```

---

## Testing Custom Components

### Unit Testing Component Logic

```kotlin
class ProductCardTest {
    @Test
    fun `discountedPrice calculates correctly`() {
        val card = ProductCard(
            productName = "Test Product",
            price = 100.0,
            discount = 20
        )

        assertEquals(80.0, card.discountedPrice, 0.01)
    }

    @Test
    fun `hasDiscount returns true when discount is set`() {
        val card = ProductCard(
            productName = "Test Product",
            price = 100.0,
            discount = 15
        )

        assertTrue(card.hasDiscount)
    }

    @Test
    fun `hasDiscount returns false when discount is null`() {
        val card = ProductCard(
            productName = "Test Product",
            price = 100.0,
            discount = null
        )

        assertFalse(card.hasDiscount)
    }
}
```

### Testing Android Renderer

```kotlin
class ProductCardRendererTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders product name correctly`() {
        val card = ProductCard(
            productName = "Test Product",
            price = 99.99
        )

        composeTestRule.setContent {
            RenderProductCard(card, ThemeProvider.getCurrentTheme())
        }

        composeTestRule
            .onNodeWithText("Test Product")
            .assertExists()
    }

    @Test
    fun `shows discount badge when discount is present`() {
        val card = ProductCard(
            productName = "Test Product",
            price = 100.0,
            discount = 25
        )

        composeTestRule.setContent {
            RenderProductCard(card, ThemeProvider.getCurrentTheme())
        }

        composeTestRule
            .onNodeWithText("-25%")
            .assertExists()
    }

    @Test
    fun `add to cart button triggers callback`() {
        var clicked = false
        val card = ProductCard(
            productName = "Test Product",
            price = 99.99,
            onAddToCart = { clicked = true }
        )

        composeTestRule.setContent {
            RenderProductCard(card, ThemeProvider.getCurrentTheme())
        }

        composeTestRule
            .onNodeWithText("Add to Cart")
            .performClick()

        assertTrue(clicked)
    }
}
```

---

## Best Practices

### 1. Keep Components Focused

**✅ GOOD:** Single responsibility

```kotlin
data class PriceLabel(
    val price: Double,
    val currency: String = "USD"
) : Component
```

**❌ BAD:** Too many responsibilities

```kotlin
data class ProductInfo(
    val name: String,
    val price: Double,
    val description: String,
    val reviews: List<Review>,
    val relatedProducts: List<Product>
    // ... too much in one component
) : Component
```

### 2. Use Composition Over Complexity

**✅ GOOD:** Compose smaller components

```kotlin
data class ProductCard(
    val name: String,
    val price: PriceLabel,
    val rating: RatingStars,
    val badge: StatusBadge?
) : Component
```

### 3. Make Components Serializable

Use `@Transient` for non-serializable properties (lambdas, functions).

```kotlin
data class Button(
    val text: String,
    @Transient val onClick: (() -> Unit)? = null
) : Component
```

### 4. Provide Sensible Defaults

```kotlin
data class Card(
    val title: String,
    val elevation: Float = 2f,        // Default elevation
    val cornerRadius: Float = 8f,     // Default corner radius
    val padding: Float = 16f          // Default padding
) : Component
```

### 5. Document Your Components

```kotlin
/**
 * ProductCard - Displays product information with image, price, rating, and add-to-cart button.
 *
 * @param productName The name of the product (max 2 lines)
 * @param price The original price in USD
 * @param imageUrl Optional product image URL (shows placeholder if null)
 * @param rating Product rating from 0.0 to 5.0 (hidden if 0)
 * @param inStock Whether the product is available for purchase
 * @param discount Optional percentage discount (0-100)
 * @param onAddToCart Callback invoked when "Add to Cart" is clicked
 * @param onClick Callback invoked when card is clicked
 *
 * @sample
 * ProductCard(
 *     productName = "Wireless Headphones",
 *     price = 99.99,
 *     rating = 4.5f,
 *     discount = 20,
 *     onAddToCart = { addToCart(product) }
 * )
 */
data class ProductCard(...) : Component
```

### 6. Handle Edge Cases

```kotlin
@Composable
fun RenderPriceLabel(c: PriceLabel, theme: Theme) {
    val formattedPrice = try {
        formatCurrency(c.price, c.currency)
    } catch (e: Exception) {
        "Price unavailable"  // Fallback for invalid currency
    }

    Text(text = formattedPrice)
}
```

### 7. Test Across Themes

```kotlin
@Preview(name = "Light Theme")
@Composable
fun PreviewProductCardLight() {
    val theme = MaterialLightTheme
    RenderProductCard(sampleProductCard, theme)
}

@Preview(name = "Dark Theme")
@Composable
fun PreviewProductCardDark() {
    val theme = MaterialDarkTheme
    RenderProductCard(sampleProductCard, theme)
}
```

---

## Publishing Components

### 1. Package Structure

```
mycompany-components/
├── src/
│   └── commonMain/
│       └── kotlin/
│           └── com/mycompany/components/
│               ├── ProductCard.kt
│               ├── StatusBadge.kt
│               └── PriceLabel.kt
├── renderers/
│   ├── android/
│   │   └── src/androidMain/kotlin/
│   │       └── com/mycompany/renderers/android/
│   │           ├── ProductCardRenderer.kt
│   │           ├── StatusBadgeRenderer.kt
│   │           └── PriceLabelRenderer.kt
│   ├── ios/
│   └── web/
└── build.gradle.kts
```

### 2. Create Library Module

```kotlin
// build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("maven-publish")
}

kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    js(IR) {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.augmentalis:avaelements-core:2.0.0")
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.mycompany"
            artifactId = "custom-components"
            version = "1.0.0"
        }
    }
}
```

### 3. Document Your Library

Create a README with:
- Component list
- Installation instructions
- Usage examples
- Screenshots/GIFs
- API documentation

### 4. Share on Component Marketplace (Future)

AvaElements will have a component marketplace where you can publish and discover custom components.

---

## Next Steps

📖 **Tutorials:**
- [Tutorial 05 - Building a Custom Login Form](../tutorials/05-Custom-Login-Form.md)
- [Tutorial 06 - Advanced State Management](../tutorials/06-State-Management.md)

📚 **Reference:**
- [Component Guide](./02-Component-Guide.md) - Study built-in components
- [Android Renderer](./03-Android-Renderer.md) - Renderer implementation details
- [Theme System](./04-Theme-System.md) - Theming your custom components

---

**Version:** 2.0.0
**Components Built:** 48 (built-in) + unlimited (custom)
**Framework:** AvaElements on Kotlin Multiplatform
**Methodology:** IDEACODE 7.2.0
**Author:** Manoj Jhawar (manoj@ideahq.net)
