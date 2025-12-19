package com.augmentalis.avanues.avaelements.examples

import com.augmentalis.avanues.avaelements.core.*
import com.augmentalis.avanues.avaelements.dsl.*
import com.augmentalis.avanues.avamagic.ui.core.navigation.*
import com.augmentalis.avanues.avamagic.ui.core.data.*

/**
 * Comprehensive examples demonstrating all Phase 3.3 (Navigation) and 3.4 (Data Display) components.
 *
 * These examples show real-world usage patterns and can serve as templates for application development.
 */
object NavigationAndDataExample {

    /**
     * Example 1: Complete App with AppBar, BottomNav, and Drawer
     *
     * A typical mobile app structure with top bar, bottom navigation, and side drawer.
     */
    fun completeAppStructure() = AvaUI {
        theme = Themes.Material3Light

        Column {
            arrangement = Arrangement.Start
            fillMaxSize()

            // Top App Bar
            AppBar(title = "My Application") {
                navigationIcon = "menu"
                elevation = 2

                action("search", "Search") {
                    println("Search clicked")
                }
                action("notifications", "Notifications") {
                    println("Notifications clicked")
                }

                onNavigationClick = {
                    println("Open drawer")
                }

                fillMaxWidth()
            }

            // Main Content Area
            Column {
                weight(1f)
                padding(16f)

                Text("Welcome to the app!") {
                    font = Font.Title
                    color = Color.Black
                }

                Text("This is your main content area") {
                    font = Font.Body
                    color = Color(128, 128, 128)
                }
            }

            // Bottom Navigation
            BottomNav(
                items = listOf(
                    bottomNavItem("home", "Home"),
                    bottomNavItem("explore", "Explore"),
                    bottomNavItem("favorites", "Favorites", badge = "5"),
                    bottomNavItem("profile", "Profile")
                )
            ) {
                selectedIndex = 0
                onItemSelected = { index ->
                    println("Navigate to: $index")
                }

                fillMaxWidth()
            }
        }
    }

    /**
     * Example 2: Side Drawer Navigation
     *
     * A comprehensive drawer with header, navigation items, and footer.
     */
    fun drawerNavigation() = Drawer(isOpen = true) {
        position = DrawerPosition.Left

        // Header component would be defined here
        header = ContainerComponent(
            id = "drawer_header",
            style = ComponentStyle(
                padding = Spacing.all(16f),
                backgroundColor = Color(33, 150, 243)
            ),
            modifiers = emptyList(),
            alignment = Alignment.Center,
            child = TextComponent(
                text = "User Name",
                id = null,
                style = null,
                modifiers = emptyList(),
                font = Font.Heading,
                color = Color.White,
                textAlign = TextScope.TextAlign.Center,
                maxLines = null,
                overflow = TextScope.TextOverflow.Clip
            )
        )

        // Navigation items
        item("dashboard", "Dashboard", icon = "dashboard")
        item("projects", "Projects", icon = "folder", badge = "12")
        item("team", "Team", icon = "people")
        item("settings", "Settings", icon = "settings")
        item("help", "Help & Feedback", icon = "help")

        onItemClick = { id ->
            println("Navigate to: $id")
        }

        onDismiss = {
            println("Close drawer")
        }
    }

    /**
     * Example 3: Multi-Tab Interface
     *
     * Tabbed navigation for organizing content into separate views.
     */
    fun multiTabInterface() = AvaUI {
        theme = Themes.iOS26LiquidGlass

        Column {
            fillMaxSize()

            // Tab Navigation
            Tabs(
                tabs = listOf(
                    tab("Overview", icon = "dashboard"),
                    tab("Analytics", icon = "bar_chart"),
                    tab("Reports", icon = "description"),
                    tab("Settings", icon = "settings")
                )
            ) {
                selectedIndex = 0
                onTabSelected = { index ->
                    println("Switch to tab: $index")
                }

                fillMaxWidth()
            }

            // Tab Content
            ScrollView {
                Column {
                    padding(16f)

                    Text("Tab Content") {
                        font = Font.Heading
                    }

                    Text("Content for the selected tab would appear here.") {
                        font = Font.Body
                    }
                }
            }
        }
    }

    /**
     * Example 4: Data Table with Pagination
     *
     * A complete data table showing users with pagination controls.
     */
    fun dataTableWithPagination() = AvaUI {
        theme = Themes.Material3Light

        Column {
            padding(16f)
            fillMaxSize()

            Text("User Management") {
                font = Font.Title
            }

            // Data Table
            Table(
                columns = listOf(
                    tableColumn("id", "ID", sortable = false),
                    tableColumn("name", "Name", sortable = true),
                    tableColumn("email", "Email", sortable = true),
                    tableColumn("role", "Role", sortable = true),
                    tableColumn("status", "Status", sortable = true)
                ),
                rows = listOf(
                    tableRow("1", listOf(
                        tableCell("1"),
                        tableCell("John Doe"),
                        tableCell("john.doe@example.com"),
                        tableCell("Admin"),
                        tableCell("Active")
                    )),
                    tableRow("2", listOf(
                        tableCell("2"),
                        tableCell("Jane Smith"),
                        tableCell("jane.smith@example.com"),
                        tableCell("User"),
                        tableCell("Active")
                    )),
                    tableRow("3", listOf(
                        tableCell("3"),
                        tableCell("Bob Johnson"),
                        tableCell("bob.j@example.com"),
                        tableCell("User"),
                        tableCell("Inactive")
                    ))
                )
            ) {
                sortable = true
                hoverable = true
                striped = true
                onRowClick = { index ->
                    println("Edit user at row: $index")
                }

                fillMaxWidth()
            }

            // Pagination Controls
            Pagination(totalPages = 5) {
                currentPage = 1
                showFirstLast = true
                showPrevNext = true
                maxVisible = 5

                onPageChange = { page ->
                    println("Load page: $page")
                }

                padding(vertical = 16f)
            }
        }
    }

    /**
     * Example 5: User List with Avatars
     *
     * A contact list showing users with avatars and status.
     */
    fun userListWithAvatars() = AvaUI {
        theme = Themes.iOS26LiquidGlass

        Column {
            fillMaxSize()

            AppBar(title = "Contacts") {
                navigationIcon = "arrow_back"
                action("add", "Add") {
                    println("Add contact")
                }

                fillMaxWidth()
            }

            List(
                items = listOf(
                    listItem(
                        id = "1",
                        primary = "Alice Johnson",
                        secondary = "Software Engineer • Online",
                        avatar = "https://example.com/avatar1.jpg"
                    ),
                    listItem(
                        id = "2",
                        primary = "Bob Smith",
                        secondary = "Product Manager • Away",
                        avatar = "https://example.com/avatar2.jpg"
                    ),
                    listItem(
                        id = "3",
                        primary = "Carol Williams",
                        secondary = "Designer • Offline",
                        avatar = "https://example.com/avatar3.jpg"
                    ),
                    listItem(
                        id = "4",
                        primary = "David Brown",
                        secondary = "Developer • Online",
                        icon = "person"
                    )
                )
            ) {
                selectable = false
                onItemClick = { index ->
                    println("Open contact: $index")
                }

                fillMaxWidth()
                weight(1f)
            }
        }
    }

    /**
     * Example 6: File Explorer with TreeView
     *
     * A hierarchical file/folder browser using TreeView.
     */
    fun fileExplorer() = AvaUI {
        theme = Themes.Material3Light

        Column {
            fillMaxSize()

            AppBar(title = "File Explorer") {
                navigationIcon = "menu"
                action("search", "Search") {
                    println("Search files")
                }

                fillMaxWidth()
            }

            TreeView(
                nodes = listOf(
                    treeNode(
                        id = "1",
                        label = "Documents",
                        icon = "folder",
                        children = listOf(
                            treeNode(
                                id = "1.1",
                                label = "Work",
                                icon = "folder",
                                children = listOf(
                                    treeNode("1.1.1", "Project.docx", icon = "description"),
                                    treeNode("1.1.2", "Report.pdf", icon = "picture_as_pdf")
                                )
                            ),
                            treeNode("1.2", "Personal", icon = "folder")
                        )
                    ),
                    treeNode(
                        id = "2",
                        label = "Pictures",
                        icon = "folder",
                        children = listOf(
                            treeNode("2.1", "Vacation.jpg", icon = "image"),
                            treeNode("2.2", "Family.png", icon = "image")
                        )
                    ),
                    treeNode("3", "Downloads", icon = "folder")
                )
            ) {
                expandedIds = setOf("1", "1.1")
                onNodeClick = { id ->
                    println("Open: $id")
                }
                onToggle = { id ->
                    println("Toggle: $id")
                }

                fillMaxWidth()
                weight(1f)
            }
        }
    }

    /**
     * Example 7: Product Carousel
     *
     * An image carousel for showcasing products or featured content.
     */
    fun productCarousel() = AvaUI {
        theme = Themes.iOS26LiquidGlass

        Column {
            fillMaxSize()

            Text("Featured Products") {
                font = Font.Heading
                padding(16f)
            }

            Carousel(
                items = listOf(
                    ImageComponent(
                        source = "https://example.com/product1.jpg",
                        id = null,
                        style = null,
                        modifiers = emptyList(),
                        contentDescription = "Product 1",
                        contentScale = ImageScope.ContentScale.Crop
                    ),
                    ImageComponent(
                        source = "https://example.com/product2.jpg",
                        id = null,
                        style = null,
                        modifiers = emptyList(),
                        contentDescription = "Product 2",
                        contentScale = ImageScope.ContentScale.Crop
                    ),
                    ImageComponent(
                        source = "https://example.com/product3.jpg",
                        id = null,
                        style = null,
                        modifiers = emptyList(),
                        contentDescription = "Product 3",
                        contentScale = ImageScope.ContentScale.Crop
                    )
                )
            ) {
                currentIndex = 0
                autoPlay = true
                interval = 5000
                showIndicators = true
                showControls = true

                onSlideChange = { index ->
                    println("Slide changed to: $index")
                }

                fillMaxWidth()
            }

            Text("Swipe to see more products") {
                font = Font.Caption
                padding(8f)
            }
        }
    }

    /**
     * Example 8: Settings Accordion
     *
     * Collapsible settings sections using Accordion.
     */
    fun settingsAccordion() = AvaUI {
        theme = Themes.Material3Light

        Column {
            fillMaxSize()

            AppBar(title = "Settings") {
                navigationIcon = "arrow_back"
                fillMaxWidth()
            }

            ScrollView {
                Accordion(
                    items = listOf(
                        accordionItem(
                            id = "account",
                            title = "Account Settings",
                            content = ColumnComponent(
                                id = null,
                                style = ComponentStyle(padding = Spacing.all(16f)),
                                modifiers = emptyList(),
                                arrangement = Arrangement.Start,
                                horizontalAlignment = Alignment.Start,
                                children = listOf(
                                    TextComponent(
                                        text = "Manage your account preferences",
                                        id = null,
                                        style = null,
                                        modifiers = emptyList(),
                                        font = Font.Body,
                                        color = Color.Black,
                                        textAlign = TextScope.TextAlign.Start,
                                        maxLines = null,
                                        overflow = TextScope.TextOverflow.Clip
                                    )
                                )
                            )
                        ),
                        accordionItem(
                            id = "privacy",
                            title = "Privacy & Security",
                            content = ColumnComponent(
                                id = null,
                                style = ComponentStyle(padding = Spacing.all(16f)),
                                modifiers = emptyList(),
                                arrangement = Arrangement.Start,
                                horizontalAlignment = Alignment.Start,
                                children = listOf(
                                    TextComponent(
                                        text = "Control your privacy settings",
                                        id = null,
                                        style = null,
                                        modifiers = emptyList(),
                                        font = Font.Body,
                                        color = Color.Black,
                                        textAlign = TextScope.TextAlign.Start,
                                        maxLines = null,
                                        overflow = TextScope.TextOverflow.Clip
                                    )
                                )
                            )
                        ),
                        accordionItem(
                            id = "notifications",
                            title = "Notifications",
                            content = ColumnComponent(
                                id = null,
                                style = ComponentStyle(padding = Spacing.all(16f)),
                                modifiers = emptyList(),
                                arrangement = Arrangement.Start,
                                horizontalAlignment = Alignment.Start,
                                children = listOf(
                                    TextComponent(
                                        text = "Manage notification preferences",
                                        id = null,
                                        style = null,
                                        modifiers = emptyList(),
                                        font = Font.Body,
                                        color = Color.Black,
                                        textAlign = TextScope.TextAlign.Start,
                                        maxLines = null,
                                        overflow = TextScope.TextOverflow.Clip
                                    )
                                )
                            )
                        )
                    )
                ) {
                    expandedIndices = setOf(0)
                    allowMultiple = false
                    onToggle = { index ->
                        println("Toggle section: $index")
                    }

                    fillMaxWidth()
                }
            }
        }
    }

    /**
     * Example 9: Onboarding Stepper
     *
     * Multi-step onboarding flow using Stepper.
     */
    fun onboardingStepper() = AvaUI {
        theme = Themes.iOS26LiquidGlass

        Column {
            fillMaxSize()
            padding(16f)

            Text("Account Setup") {
                font = Font.Title
            }

            Stepper(
                steps = listOf(
                    step(
                        "Personal Info",
                        description = "Enter your details",
                        status = StepStatus.Complete
                    ),
                    step(
                        "Verification",
                        description = "Verify your email",
                        status = StepStatus.Active
                    ),
                    step(
                        "Preferences",
                        description = "Set your preferences",
                        status = StepStatus.Pending
                    ),
                    step(
                        "Complete",
                        description = "Finish setup",
                        status = StepStatus.Pending
                    )
                )
            ) {
                currentStep = 1
                orientation = Orientation.Horizontal

                onStepClick = { index ->
                    println("Navigate to step: $index")
                }

                fillMaxWidth()
                padding(vertical = 16f)
            }

            Text("Step 2: Verify Your Email") {
                font = Font.Heading
            }

            Text("We've sent a verification code to your email address.") {
                font = Font.Body
            }
        }
    }

    /**
     * Example 10: Activity Timeline
     *
     * Chronological activity feed using Timeline.
     */
    fun activityTimeline() = AvaUI {
        theme = Themes.Material3Light

        Column {
            fillMaxSize()

            AppBar(title = "Activity Feed") {
                navigationIcon = "menu"
                fillMaxWidth()
            }

            ScrollView {
                Timeline(
                    items = listOf(
                        timelineItem(
                            id = "1",
                            timestamp = "2 hours ago",
                            title = "Order Shipped",
                            description = "Your order #12345 has been shipped",
                            icon = "local_shipping",
                            color = Color.Blue
                        ),
                        timelineItem(
                            id = "2",
                            timestamp = "5 hours ago",
                            title = "Payment Confirmed",
                            description = "Payment of $99.99 received",
                            icon = "check_circle",
                            color = Color.Green
                        ),
                        timelineItem(
                            id = "3",
                            timestamp = "1 day ago",
                            title = "Order Placed",
                            description = "Order #12345 placed successfully",
                            icon = "shopping_cart",
                            color = Color(128, 128, 128)
                        ),
                        timelineItem(
                            id = "4",
                            timestamp = "2 days ago",
                            title = "Account Created",
                            description = "Welcome to our store!",
                            icon = "person_add",
                            color = Color(33, 150, 243)
                        )
                    )
                ) {
                    orientation = Orientation.Vertical
                    fillMaxWidth()
                    padding(16f)
                }
            }
        }
    }

    /**
     * Example 11: Empty State Screens
     *
     * Various empty state examples for different scenarios.
     */
    fun emptyStateScreens() = AvaUI {
        theme = Themes.iOS26LiquidGlass

        Column {
            fillMaxSize()

            // Empty inbox
            EmptyState(title = "No Messages") {
                icon = "inbox"
                description = "You don't have any messages yet. Start a conversation to see it here."
                action = ButtonComponent(
                    text = "Compose Message",
                    id = null,
                    style = null,
                    modifiers = emptyList(),
                    buttonStyle = ButtonScope.ButtonStyle.Primary,
                    enabled = true,
                    onClick = { println("Compose") },
                    leadingIcon = "edit",
                    trailingIcon = null
                )

                fillMaxWidth()
                padding(32f)
            }
        }
    }

    /**
     * Example 12: Loading States with Skeleton
     *
     * Skeleton placeholders for better perceived performance.
     */
    fun loadingSkeletons() = AvaUI {
        theme = Themes.Material3Light

        Column {
            padding(16f)
            fillMaxSize()

            // User profile skeleton
            Row {
                // Avatar skeleton
                Skeleton {
                    variant = SkeletonVariant.Circular
                    width = Size.Fixed(48f)
                    height = Size.Fixed(48f)
                    animation = SkeletonAnimation.Pulse
                }

                Column {
                    padding(horizontal = 12f)

                    // Name skeleton
                    Skeleton {
                        variant = SkeletonVariant.Text
                        width = Size.Fixed(150f)
                        height = Size.Fixed(20f)
                        animation = SkeletonAnimation.Wave
                    }

                    // Description skeleton
                    Skeleton {
                        variant = SkeletonVariant.Text
                        width = Size.Fixed(200f)
                        height = Size.Fixed(16f)
                        animation = SkeletonAnimation.Wave
                    }
                }
            }

            // Content skeleton
            Skeleton {
                variant = SkeletonVariant.Rectangular
                width = Size.Fill
                height = Size.Fixed(200f)
                animation = SkeletonAnimation.Pulse

                padding(top = 16f)
            }
        }
    }

    /**
     * Example 13: Tag/Chip Management
     *
     * Managing tags with Chip components.
     */
    fun tagManagement() = AvaUI {
        theme = Themes.Material3Light

        Column {
            padding(16f)

            Text("Interests") {
                font = Font.Heading
            }

            Row {
                arrangement = Arrangement.Start
                padding(vertical = 8f)

                Chip(label = "Technology") {
                    icon = "computer"
                    deletable = true
                    selected = true
                    onClick = { println("Technology clicked") }
                    onDelete = { println("Remove Technology") }
                }

                Chip(label = "Design") {
                    icon = "palette"
                    deletable = true
                    selected = false
                    onClick = { println("Design clicked") }
                    onDelete = { println("Remove Design") }
                }

                Chip(label = "Sports") {
                    icon = "sports_soccer"
                    deletable = true
                    selected = false
                    onClick = { println("Sports clicked") }
                    onDelete = { println("Remove Sports") }
                }
            }
        }
    }

    /**
     * Example 14: Advanced Data Grid
     *
     * Feature-rich data grid with sorting, pagination, and selection.
     */
    fun advancedDataGrid() = AvaUI {
        theme = Themes.Material3Light

        Column {
            fillMaxSize()
            padding(16f)

            Text("Employee Directory") {
                font = Font.Title
            }

            DataGrid(
                columns = listOf(
                    dataGridColumn("id", "ID", sortable = false, align = TextAlign.Start),
                    dataGridColumn("name", "Name", sortable = true, align = TextAlign.Start),
                    dataGridColumn("department", "Department", sortable = true, align = TextAlign.Start),
                    dataGridColumn("salary", "Salary", sortable = true, align = TextAlign.End),
                    dataGridColumn("status", "Status", sortable = true, align = TextAlign.Center)
                ),
                rows = listOf(
                    dataGridRow("1", mapOf(
                        "id" to "E001",
                        "name" to "Alice Johnson",
                        "department" to "Engineering",
                        "salary" to "$120,000",
                        "status" to "Active"
                    )),
                    dataGridRow("2", mapOf(
                        "id" to "E002",
                        "name" to "Bob Smith",
                        "department" to "Marketing",
                        "salary" to "$95,000",
                        "status" to "Active"
                    )),
                    dataGridRow("3", mapOf(
                        "id" to "E003",
                        "name" to "Carol Williams",
                        "department" to "Design",
                        "salary" to "$105,000",
                        "status" to "On Leave"
                    ))
                )
            ) {
                pageSize = 20
                currentPage = 1
                sortBy = "name"
                sortOrder = SortOrder.Ascending
                selectable = true

                onSort = { column, order ->
                    println("Sort by $column $order")
                }

                onPageChange = { page ->
                    println("Load page $page")
                }

                onSelectionChange = { selectedIds ->
                    println("Selected: $selectedIds")
                }

                fillMaxWidth()
                weight(1f)
            }
        }
    }

    /**
     * Example 15: Breadcrumb Navigation
     *
     * Hierarchical navigation breadcrumbs.
     */
    fun breadcrumbNavigation() = AvaUI {
        theme = Themes.iOS26LiquidGlass

        Column {
            fillMaxSize()

            // Breadcrumb trail
            Breadcrumb(
                items = listOf(
                    breadcrumbItem("Home", "/") { println("Go to Home") },
                    breadcrumbItem("Products", "/products") { println("Go to Products") },
                    breadcrumbItem("Electronics", "/products/electronics") { println("Go to Electronics") },
                    breadcrumbItem("Laptops")
                )
            ) {
                separator = "/"
                padding(16f)
                fillMaxWidth()
            }

            Divider {
                orientation = Orientation.Horizontal
                thickness = 1f
            }

            // Page content
            Column {
                padding(16f)

                Text("Laptops") {
                    font = Font.Title
                }

                Text("Browse our selection of laptops") {
                    font = Font.Body
                }
            }
        }
    }
}
