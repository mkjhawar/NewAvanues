# Third-Party App UUID Generation via Accessibility Scraping

**Date**: 2025-10-07
**Status**: Architectural Design
**Priority**: Critical for VoiceOS Universal Voice Control

---

## 🎯 Objective

Generate **stable, deterministic UUIDs** for UI elements in third-party Android apps using VoiceOS's accessibility scraping service, enabling voice control of any app without requiring developer integration.

---

## 🔑 Key Requirements

### 1. **Stable UUIDs Across Sessions**
- Same UI element in third-party app must get same UUID across app launches
- UUIDs persist even after device restart
- Enables voice command learning ("submit button" → UUID mapping persists)

### 2. **Package Name + Version Integration**
- UUID incorporates app package name (e.g., `com.instagram.android`)
- UUID incorporates app version (e.g., `v12.0.0`)
- Different app versions can have different UUIDs (handles UI changes)
- Prevents collisions between apps

### 3. **Deterministic Generation**
- Given same AccessibilityNodeInfo properties, generate same UUID
- Uses content-based hashing (not random generation)
- Survives app updates if UI element unchanged

### 4. **Collision Prevention**
- UUIDs unique per app (no conflicts between different apps)
- UUIDs unique per app version
- Handles namespace isolation

---

## 📐 UUID Format for Third-Party Apps

### **Format Pattern**

```
{packageName}.v{version}.{elementType}-{contentHash}
```

### **Example UUIDs**

```kotlin
// Instagram submit button, app version 12.0.0
"com.instagram.android.v12.0.0.button-a7f3e2c1d4b5"

// Twitter text field, app version 9.5.1
"com.twitter.android.v9.5.1.textfield-c3d8f1a2e6b9"

// Gmail image view, app version 2023.08.20
"com.google.android.gm.v2023.08.20.imageview-f9e2a1c4d7b3"
```

### **Component Breakdown**

| Component | Example | Purpose |
|-----------|---------|---------|
| **Package Name** | `com.instagram.android` | App identifier |
| **Version** | `v12.0.0` | App version (handles UI changes) |
| **Element Type** | `button` | Component type (for organization) |
| **Content Hash** | `a7f3e2c1d4b5` | Deterministic hash of element properties |

---

## 🔨 Hash Generation Strategy

### **Hashable Properties from AccessibilityNodeInfo**

```kotlin
data class AccessibilityFingerprint(
    // Primary identifiers
    val resourceId: String?,
    val viewIdResourceName: String?,
    val className: String?,

    // Content
    val text: String?,
    val contentDescription: String?,
    val hintText: String?,

    // Hierarchy position
    val hierarchyPath: String,        // Path from root (e.g., "0/2/1")
    val siblingIndex: Int,

    // Visual properties (for stability)
    val boundsInScreen: Rect?,
    val drawingOrder: Int?,

    // Interaction properties
    val isClickable: Boolean,
    val isFocusable: Boolean,
    val isCheckable: Boolean,
    val isEditable: Boolean,

    // Package context
    val packageName: String,
    val appVersion: String
)
```

### **Hash Algorithm**

```kotlin
/**
 * Generate deterministic content hash for AccessibilityNodeInfo
 *
 * Uses SHA-256 of concatenated properties for stable UUID generation.
 *
 * @param fingerprint Accessibility element fingerprint
 * @return 12-character hex hash (first 48 bits of SHA-256)
 */
fun generateContentHash(fingerprint: AccessibilityFingerprint): String {
    val concatenated = buildString {
        // Primary identifiers (highest weight)
        append(fingerprint.resourceId ?: "")
        append("|")
        append(fingerprint.viewIdResourceName ?: "")
        append("|")
        append(fingerprint.className ?: "")
        append("|")

        // Content (medium weight)
        append(fingerprint.text ?: "")
        append("|")
        append(fingerprint.contentDescription ?: "")
        append("|")

        // Hierarchy (medium weight - survives minor UI reordering)
        append(fingerprint.hierarchyPath)
        append("|")
        append(fingerprint.siblingIndex)
        append("|")

        // Package context
        append(fingerprint.packageName)
        append("|")
        append(fingerprint.appVersion)
    }

    // SHA-256 hash
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(concatenated.toByteArray())

    // Take first 48 bits (6 bytes) = 12 hex characters
    return hashBytes.take(6).joinToString("") { "%02x".format(it) }
}
```

---

## 🏗️ Implementation Architecture

### **Component Structure**

```
/vos4/modules/libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/
├── thirdParty/
│   ├── thirdPartyUuidGenerator.kt          # Generate UUIDs for 3rd party apps
│   ├── accessibilityFingerprint.kt         # Extract fingerprint from AccessibilityNodeInfo
│   ├── contentHasher.kt                    # Generate deterministic hash
│   ├── packageVersionResolver.kt           # Get app version from PackageManager
│   ├── uuidStabilityTracker.kt             # Track UUID stability across app updates
│   └── thirdPartyUuidCache.kt              # Cache generated UUIDs for performance
```

### **Implementation: ThirdPartyUuidGenerator**

```kotlin
/**
 * Third-Party App UUID Generator
 *
 * Generates stable, deterministic UUIDs for UI elements in third-party apps
 * using accessibility scraping.
 *
 * UUID Format: {packageName}.v{version}.{type}-{contentHash}
 *
 * Features:
 * - Stable across app sessions
 * - Version-aware (different UUIDs for different app versions)
 * - Deterministic (same element = same UUID)
 * - Collision-free (package name isolation)
 *
 * @since 1.0.0
 */
class ThirdPartyUuidGenerator(
    private val context: Context,
    private val packageVersionResolver: PackageVersionResolver,
    private val contentHasher: ContentHasher,
    private val cache: ThirdPartyUuidCache
) {

    /**
     * Generate UUID for AccessibilityNodeInfo
     *
     * Steps:
     * 1. Extract accessibility fingerprint
     * 2. Resolve app version
     * 3. Generate content hash
     * 4. Format UUID
     * 5. Cache for reuse
     *
     * @param node AccessibilityNodeInfo from third-party app
     * @return Deterministic UUID
     */
    suspend fun generateUuid(node: AccessibilityNodeInfo): String {
        val packageName = node.packageName?.toString()
            ?: throw IllegalArgumentException("Node has no package name")

        // Check cache first
        val cached = cache.get(packageName, node)
        if (cached != null) return cached

        // Extract fingerprint
        val fingerprint = extractFingerprint(node, packageName)

        // Resolve app version
        val version = packageVersionResolver.getVersion(packageName)

        // Generate content hash
        val contentHash = contentHasher.generateHash(fingerprint)

        // Determine element type
        val elementType = getElementType(node)

        // Format UUID
        val uuid = formatThirdPartyUuid(
            packageName = packageName,
            version = version,
            elementType = elementType,
            contentHash = contentHash
        )

        // Cache for reuse
        cache.put(packageName, node, uuid)

        return uuid
    }

    /**
     * Extract accessibility fingerprint from node
     *
     * @param node AccessibilityNodeInfo
     * @param packageName App package name
     * @return Accessibility fingerprint
     */
    private suspend fun extractFingerprint(
        node: AccessibilityNodeInfo,
        packageName: String
    ): AccessibilityFingerprint {
        val version = packageVersionResolver.getVersion(packageName)

        return AccessibilityFingerprint(
            // Primary identifiers
            resourceId = node.viewIdResourceName,
            viewIdResourceName = node.viewIdResourceName,
            className = node.className?.toString(),

            // Content
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            hintText = node.hintText?.toString(),

            // Hierarchy
            hierarchyPath = calculateHierarchyPath(node),
            siblingIndex = calculateSiblingIndex(node),

            // Visual properties
            boundsInScreen = Rect().also { node.getBoundsInScreen(it) },
            drawingOrder = node.drawingOrder,

            // Interaction properties
            isClickable = node.isClickable,
            isFocusable = node.isFocusable,
            isCheckable = node.isCheckable,
            isEditable = node.isEditable,

            // Package context
            packageName = packageName,
            appVersion = version
        )
    }

    /**
     * Calculate hierarchy path from root
     *
     * Example: Root → Container (index 0) → Button (index 2) → "0/2"
     *
     * @param node AccessibilityNodeInfo
     * @return Hierarchy path string
     */
    private fun calculateHierarchyPath(node: AccessibilityNodeInfo): String {
        val path = mutableListOf<Int>()
        var current: AccessibilityNodeInfo? = node

        while (current != null) {
            val parent = current.parent
            if (parent != null) {
                // Find index of current in parent's children
                val index = (0 until parent.childCount).find { i ->
                    parent.getChild(i)?.equals(current) == true
                } ?: 0
                path.add(0, index)
            }
            current = parent
        }

        return path.joinToString("/")
    }

    /**
     * Calculate sibling index (position among siblings)
     *
     * @param node AccessibilityNodeInfo
     * @return Sibling index (0-based)
     */
    private fun calculateSiblingIndex(node: AccessibilityNodeInfo): Int {
        val parent = node.parent ?: return 0

        return (0 until parent.childCount).indexOfFirst { i ->
            parent.getChild(i)?.equals(node) == true
        }
    }

    /**
     * Get element type from AccessibilityNodeInfo
     *
     * Maps Android class names to semantic types.
     *
     * @param node AccessibilityNodeInfo
     * @return Element type (button, textfield, image, etc.)
     */
    private fun getElementType(node: AccessibilityNodeInfo): String {
        val className = node.className?.toString() ?: return "view"

        return when {
            className.contains("Button", ignoreCase = true) -> "button"
            className.contains("EditText", ignoreCase = true) -> "textfield"
            className.contains("TextView", ignoreCase = true) -> "text"
            className.contains("ImageView", ignoreCase = true) -> "image"
            className.contains("ImageButton", ignoreCase = true) -> "imagebutton"
            className.contains("CheckBox", ignoreCase = true) -> "checkbox"
            className.contains("RadioButton", ignoreCase = true) -> "radio"
            className.contains("Switch", ignoreCase = true) -> "switch"
            className.contains("Spinner", ignoreCase = true) -> "dropdown"
            className.contains("RecyclerView", ignoreCase = true) -> "list"
            className.contains("ListView", ignoreCase = true) -> "list"
            className.contains("ViewGroup", ignoreCase = true) -> "container"
            className.contains("LinearLayout", ignoreCase = true) -> "container"
            className.contains("FrameLayout", ignoreCase = true) -> "container"
            else -> className.substringAfterLast(".").lowercase()
        }
    }

    /**
     * Format third-party UUID
     *
     * @param packageName App package name
     * @param version App version
     * @param elementType Element type
     * @param contentHash Deterministic hash
     * @return Formatted UUID
     */
    private fun formatThirdPartyUuid(
        packageName: String,
        version: String,
        elementType: String,
        contentHash: String
    ): String {
        // Sanitize version (remove spaces, special chars)
        val sanitizedVersion = version.replace(Regex("[^a-zA-Z0-9.]"), "")

        return "$packageName.v$sanitizedVersion.$elementType-$contentHash"
    }
}
```

### **Implementation: PackageVersionResolver**

```kotlin
/**
 * Package Version Resolver
 *
 * Resolves app version from PackageManager for third-party UUID generation.
 *
 * @since 1.0.0
 */
class PackageVersionResolver(private val context: Context) {

    private val versionCache = ConcurrentHashMap<String, String>()

    /**
     * Get app version
     *
     * Returns versionName (e.g., "12.0.0") if available,
     * otherwise versionCode (e.g., "12000").
     *
     * @param packageName App package name
     * @return Version string
     */
    suspend fun getVersion(packageName: String): String {
        // Check cache
        versionCache[packageName]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(packageName, 0)
                }

                // Prefer versionName (human-readable)
                val version = packageInfo.versionName ?: run {
                    // Fallback to versionCode
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toString()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toString()
                    }
                }

                // Cache
                versionCache[packageName] = version
                version

            } catch (e: Exception) {
                // Fallback to "unknown" if package not found
                "unknown"
            }
        }
    }

    /**
     * Clear version cache
     *
     * Call when app updates detected.
     */
    fun clearCache() {
        versionCache.clear()
    }

    /**
     * Clear version for specific package
     *
     * @param packageName Package to clear
     */
    fun clearCache(packageName: String) {
        versionCache.remove(packageName)
    }
}
```

### **Implementation: UuidStabilityTracker**

```kotlin
/**
 * UUID Stability Tracker
 *
 * Tracks UUID stability across app updates:
 * - Detects when third-party app updates change UI
 * - Remaps old UUIDs to new UUIDs
 * - Maintains voice command compatibility
 *
 * Example:
 * - User teaches "submit button" → UUID `com.app.v1.0.button-abc123`
 * - App updates to v2.0, button UUID changes to `com.app.v2.0.button-def456`
 * - Tracker maps old UUID → new UUID
 * - Voice command "submit button" still works
 *
 * @since 1.0.0
 */
class UuidStabilityTracker(
    private val database: UuidCreatorDatabase
) {

    /**
     * Track app update
     *
     * When third-party app updates:
     * 1. Detect version change
     * 2. Re-scan UI with accessibility service
     * 3. Generate new UUIDs for new version
     * 4. Map old UUIDs → new UUIDs (best-effort matching)
     *
     * @param packageName Updated app package
     * @param oldVersion Previous version
     * @param newVersion New version
     */
    suspend fun trackAppUpdate(
        packageName: String,
        oldVersion: String,
        newVersion: String
    ) {
        // Get all UUIDs for old version
        val oldUuids = database.elementDao()
            .findByPackageVersion(packageName, oldVersion)

        // Clear old UUIDs (they're now invalid)
        database.elementDao().deleteByPackageVersion(packageName, oldVersion)

        // Log update
        database.auditDao().insert(
            UuidAuditEntity(
                uuid = "$packageName.*",
                operation = "APP_UPDATE",
                detailsJson = Json.encodeToString(
                    mapOf(
                        "packageName" to packageName,
                        "oldVersion" to oldVersion,
                        "newVersion" to newVersion,
                        "oldUuidCount" to oldUuids.size
                    )
                )
            )
        )

        // Trigger re-scan (VoiceAccessibility will generate new UUIDs)
        // This is handled by VoiceAccessibilityService
    }

    /**
     * Find matching UUID in new version
     *
     * Best-effort matching using content similarity:
     * 1. Check if element with same resourceId exists
     * 2. Check if element with same text/description exists
     * 3. Check if element at same hierarchy position exists
     *
     * @param oldUuid Old UUID
     * @param newElements New version elements
     * @return New UUID or null if no match
     */
    suspend fun findMatchingUuid(
        oldUuid: String,
        newElements: List<UUIDElement>
    ): String? {
        // Parse old UUID
        val parts = oldUuid.split(".")
        if (parts.size < 4) return null

        val packageName = parts.dropLast(2).joinToString(".")
        val elementInfo = parts.last().split("-")
        val elementType = elementInfo.first()

        // Get old element details
        val oldElement = database.elementDao().findByUuid(oldUuid)?.toUUIDElement()
            ?: return null

        // Try to find match in new elements
        // Strategy 1: Same resourceId
        oldElement.metadata?.resourceId?.let { resourceId ->
            newElements.find {
                it.metadata?.resourceId == resourceId && it.type == elementType
            }?.let { return it.uuid }
        }

        // Strategy 2: Same text + description
        val oldText = oldElement.name
        val oldDescription = oldElement.description
        if (oldText != null || oldDescription != null) {
            newElements.find {
                it.name == oldText &&
                it.description == oldDescription &&
                it.type == elementType
            }?.let { return it.uuid }
        }

        // Strategy 3: Same hierarchy position
        oldElement.metadata?.hierarchyPath?.let { hierarchyPath ->
            newElements.find {
                it.metadata?.hierarchyPath == hierarchyPath && it.type == elementType
            }?.let { return it.uuid }
        }

        // No match found
        return null
    }
}
```

---

## 🔄 Integration with VoiceAccessibility

### **Accessibility Service Integration**

```kotlin
/**
 * VoiceAccessibilityService - Enhanced with third-party UUID generation
 */
class VoiceAccessibilityService : AccessibilityService() {

    private lateinit var thirdPartyUuidGenerator: ThirdPartyUuidGenerator
    private lateinit var uuidStabilityTracker: UuidStabilityTracker
    private lateinit var uuidCreator: UUIDCreator

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Initialize third-party UUID generator
        thirdPartyUuidGenerator = ThirdPartyUuidGenerator(
            context = this,
            packageVersionResolver = PackageVersionResolver(this),
            contentHasher = ContentHasher(),
            cache = ThirdPartyUuidCache()
        )

        uuidStabilityTracker = UuidStabilityTracker(
            database = UuidCreatorDatabase.getInstance(this)
        )

        uuidCreator = UUIDCreator.getInstance(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // New window opened - scan and generate UUIDs
                val rootNode = event.source ?: rootInActiveWindow ?: return
                scanAndRegisterThirdPartyApp(rootNode)
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // UI changed - update UUIDs if needed
                val rootNode = event.source ?: return
                updateThirdPartyUuids(rootNode)
            }
        }
    }

    /**
     * Scan third-party app and generate UUIDs for all elements
     *
     * @param rootNode Root AccessibilityNodeInfo
     */
    private suspend fun scanAndRegisterThirdPartyApp(rootNode: AccessibilityNodeInfo) {
        val packageName = rootNode.packageName?.toString() ?: return

        // Check if this is a third-party app (not VoiceOS or system)
        if (isSystemApp(packageName) || isVoiceOsApp(packageName)) {
            return
        }

        // Recursively generate UUIDs for all nodes
        generateUuidsRecursive(rootNode)
    }

    /**
     * Generate UUIDs recursively for node tree
     *
     * @param node Current node
     */
    private suspend fun generateUuidsRecursive(node: AccessibilityNodeInfo) {
        // Generate UUID for this node
        val uuid = thirdPartyUuidGenerator.generateUuid(node)

        // Create UUIDElement
        val element = UUIDElement(
            uuid = uuid,
            name = node.text?.toString() ?: node.contentDescription?.toString(),
            type = getElementType(node),
            description = node.contentDescription?.toString(),
            position = UUIDPosition.fromAccessibilityNode(node),
            metadata = UUIDMetadata(
                accessibility = UUIDAccessibility(
                    contentDescription = node.contentDescription?.toString(),
                    isClickable = node.isClickable,
                    isFocusable = node.isFocusable,
                    isScrollable = node.isScrollable
                ),
                // Store accessibility-specific metadata
                resourceId = node.viewIdResourceName,
                className = node.className?.toString(),
                hierarchyPath = calculateHierarchyPath(node)
            )
        )

        // Register with UUIDCreator
        uuidCreator.register(element)

        // Recursively process children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                generateUuidsRecursive(child)
            }
        }
    }

    /**
     * Check if app is system app
     */
    private fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if app is VoiceOS app
     */
    private fun isVoiceOsApp(packageName: String): Boolean {
        return packageName.startsWith("com.augmentalis.")
    }
}
```

---

## 📊 UUID Stability Examples

### **Example 1: Same Element, Same Version**

```kotlin
// Session 1
val uuid1 = generateUuid(submitButton)
// Result: "com.instagram.android.v12.0.0.button-a7f3e2c1d4b5"

// Session 2 (app relaunched)
val uuid2 = generateUuid(submitButton)
// Result: "com.instagram.android.v12.0.0.button-a7f3e2c1d4b5"

// ✅ UUIDs match - stable across sessions
```

### **Example 2: Same Element, Different Version**

```kotlin
// Instagram v12.0.0
val uuid1 = generateUuid(submitButton)
// Result: "com.instagram.android.v12.0.0.button-a7f3e2c1d4b5"

// Instagram v13.0.0 (app updated)
val uuid2 = generateUuid(submitButton)
// Result: "com.instagram.android.v13.0.0.button-c9e5f3a2b7d1"

// ⚠️ UUIDs differ - version changed
// UuidStabilityTracker maps old → new for voice command compatibility
```

### **Example 3: Different Apps, Same Element Type**

```kotlin
// Instagram submit button
val uuid1 = generateUuid(instagramSubmit)
// Result: "com.instagram.android.v12.0.0.button-a7f3e2c1d4b5"

// Twitter submit button
val uuid2 = generateUuid(twitterSubmit)
// Result: "com.twitter.android.v9.5.1.button-c3d8f1a2e6b9"

// ✅ No collision - package name isolation
```

---

## 🎯 Benefits

### **For Users**
- Voice control works for **any** Android app (no developer integration required)
- Commands persist across app sessions ("open Instagram, click submit button")
- Learned commands survive app updates (with stability tracking)

### **For VoiceOS**
- Universal voice control without third-party SDKs
- Stable UUID generation enables voice command learning
- Version-aware UUIDs handle app updates gracefully

### **For Third-Party Developers**
- Zero integration effort required
- Voice control "just works" via accessibility
- Optional: Can use UUIDCreator SDK for better UUIDs (resourceIds, semantic names)

---

## 🔧 Configuration Options

### **UUID Generation Strategies**

```kotlin
enum class ThirdPartyUuidStrategy {
    /**
     * Full fingerprint - Most stable, includes all properties
     */
    FULL_FINGERPRINT,

    /**
     * Minimal fingerprint - Less stable, only resourceId + text
     * (faster, less likely to change)
     */
    MINIMAL_FINGERPRINT,

    /**
     * Hierarchy-based - Stable for fixed layouts
     * (good for apps with static UI structure)
     */
    HIERARCHY_BASED,

    /**
     * Content-based - Stable for dynamic content
     * (good for apps with changing layouts but stable content)
     */
    CONTENT_BASED
}
```

### **Configurable Settings**

```kotlin
data class ThirdPartyUuidConfig(
    /**
     * UUID generation strategy
     */
    val strategy: ThirdPartyUuidStrategy = ThirdPartyUuidStrategy.FULL_FINGERPRINT,

    /**
     * Include app version in UUID
     */
    val includeVersion: Boolean = true,

    /**
     * Cache UUIDs for performance
     */
    val enableCaching: Boolean = true,

    /**
     * Track UUID stability across app updates
     */
    val trackStability: Boolean = true,

    /**
     * Exclude system apps
     */
    val excludeSystemApps: Boolean = true
)
```

---

## 🧪 Testing Strategy

### **Unit Tests**

```kotlin
class ThirdPartyUuidGeneratorTest {

    @Test
    fun `same node generates same UUID`() {
        val node1 = createMockNode(text = "Submit", resourceId = "submit_btn")
        val node2 = createMockNode(text = "Submit", resourceId = "submit_btn")

        val uuid1 = generator.generateUuid(node1)
        val uuid2 = generator.generateUuid(node2)

        assertEquals(uuid1, uuid2)
    }

    @Test
    fun `different nodes generate different UUIDs`() {
        val node1 = createMockNode(text = "Submit")
        val node2 = createMockNode(text = "Cancel")

        val uuid1 = generator.generateUuid(node1)
        val uuid2 = generator.generateUuid(node2)

        assertNotEquals(uuid1, uuid2)
    }

    @Test
    fun `different app versions generate different UUIDs`() {
        val node = createMockNode(text = "Submit", packageName = "com.app")

        // Version 1.0
        packageVersionResolver.setVersion("com.app", "1.0")
        val uuid1 = generator.generateUuid(node)

        // Version 2.0
        packageVersionResolver.setVersion("com.app", "2.0")
        val uuid2 = generator.generateUuid(node)

        assertNotEquals(uuid1, uuid2)
        assertTrue(uuid1.contains(".v1.0."))
        assertTrue(uuid2.contains(".v2.0."))
    }
}
```

### **Integration Tests**

```kotlin
class VoiceAccessibilityIntegrationTest {

    @Test
    fun `accessibility service generates UUIDs for third-party app`() {
        // Launch third-party app (e.g., Instagram)
        val app = launchApp("com.instagram.android")

        // Wait for accessibility event
        val rootNode = getRootNodeFromAccessibility()

        // Scan and generate UUIDs
        service.scanAndRegisterThirdPartyApp(rootNode)

        // Verify UUIDs registered
        val elements = uuidCreator.findByPackageName("com.instagram.android")
        assertTrue(elements.isNotEmpty())

        // Verify UUID format
        elements.forEach { element ->
            assertTrue(element.uuid.startsWith("com.instagram.android.v"))
        }
    }
}
```

---

## 📝 Documentation Requirements

### **Developer Guides**

1. **`thirdPartyUuidGuide.md`** - How third-party UUID generation works
2. **`accessibilityScraping.md`** - Accessibility scraping implementation
3. **`uuidStabilityGuide.md`** - How UUID stability works across app updates

### **API Reference**

1. Document `ThirdPartyUuidGenerator` public API
2. Document `UuidStabilityTracker` public API
3. Document configuration options

---

## 🚀 Implementation Priority

**Phase 2.5** (Insert between Phase 2 and Phase 3):
- Implement `ThirdPartyUuidGenerator`
- Implement `PackageVersionResolver`
- Implement `UuidStabilityTracker`
- Integrate with VoiceAccessibility
- Add configuration options
- Write tests

**Estimated Effort**: 12-15 Claude sessions

---

*This enables universal voice control for any Android app without requiring third-party integration.*
