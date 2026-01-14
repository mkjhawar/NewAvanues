# AVAMagic - Logic Wiring System

**Version:** v1.0.0
**Module:** AVAMagic
**Feature:** Logic Wiring & State Management
**Created:** 2025-11-16
**Status:** Active

---

## Executive Summary

This document defines the **Logic Wiring System** for AVAMagic Magic* components, enabling declarative state management, event handling, data binding, and business logic orchestration with minimal code.

**Quick Stats:**
- **Code Reduction:** 90-95% less logic code vs manual implementation
- **Wiring Methods:** 6 (Bind, On, Computed, Effect, Workflow, API)
- **State Management:** Automatic reactivity (no manual setState)
- **Performance:** Zero overhead (compile-time optimization)

---

## 1. Overview

### 1.1 What is Logic Wiring?

**Logic Wiring** connects UI components to:
1. **State** - Data that changes over time
2. **Events** - User interactions (tap, swipe, type)
3. **Computed Values** - Derived data (e.g., total = price * quantity)
4. **Side Effects** - API calls, database operations
5. **Workflows** - Multi-step business logic
6. **Real-time Data** - WebSocket, database listeners

**Example (Before):**
```kotlin
// Manual state management (30+ lines)
@Composable
fun LoginScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val isValid = email.isNotEmpty() && password.length >= 8

    Column {
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Button(
            onClick = {
                isLoading = true
                viewModelScope.launch {
                    try {
                        val result = authRepo.login(email, password)
                        if (result.success) {
                            navigate("home")
                        } else {
                            error = result.message
                        }
                    } catch (e: Exception) {
                        error = e.message
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = isValid && !isLoading
        ) {
            Text(if (isLoading) "Signing In..." else "Sign In")
        }
    }
}
```

**Example (After - Magic Mode with Logic Wiring):**
```kotlin
// Automatic state management (3 lines)
MagicScreen.Login {
    MagicTextField.Email(bind: user.email)
    MagicTextField.Password(bind: user.password)
    MagicButton.Positive("Sign In") on: click -> auth.login(user)
}
```

**Code Reduction:** 30 lines → 3 lines = **90% reduction**

---

## 2. Wiring Methods

### 2.1 **Bind** - Two-Way Data Binding

**Purpose:** Connect component value to state variable (automatic sync)

**Syntax:**
```kotlin
bind: <state-variable>
```

**Example:**
```kotlin
// State
val user = MagicState {
    var email: String = ""
    var password: String = ""
    var age: Int = 0
}

// Binding
MagicTextField.Email(bind: user.email)        // Auto-syncs email
MagicTextField.Password(bind: user.password)  // Auto-syncs password
MagicForm.Slider(bind: user.age, range: 1..100)  // Auto-syncs age
```

**How It Works:**
- Component displays `user.email` value
- User types → `user.email` updates automatically
- `user.email` changes elsewhere → Component updates automatically
- **Bidirectional sync** (no manual `onValueChange`)

**Supported Components:**
- All `MagicTextField.*` (Email, Password, Phone, etc.)
- All `MagicForm.*` (Checkbox, Switch, Slider, DatePicker, etc.)
- `MagicData.Table` (selected rows)
- `MagicMedia.Camera` (captured image)

---

### 2.2 **On** - Event Handling

**Purpose:** Execute logic when event occurs

**Syntax:**
```kotlin
on: <event> -> <action>
```

**Events:**
- `click` - Button tap/click
- `longPress` - Long press (1+ seconds)
- `doubleClick` - Double tap/click
- `swipeLeft` - Swipe left gesture
- `swipeRight` - Swipe right gesture
- `change` - Value changed
- `focus` - Component focused
- `blur` - Component lost focus
- `submit` - Form submitted
- `scroll` - Scrolled
- `drag` - Dragged
- `drop` - Drop occurred

**Examples:**

**Simple Action:**
```kotlin
MagicButton.Positive("Save") on: click -> saveData()
```

**Multiple Actions:**
```kotlin
MagicButton.Positive("Save") on: click -> {
    validateForm()
    saveData()
    showToast("Saved!")
}
```

**Conditional Logic:**
```kotlin
MagicButton.Positive("Save") on: click -> {
    if (form.isValid) {
        saveData()
        navigate("success")
    } else {
        showError("Please fix errors")
    }
}
```

**Event Data:**
```kotlin
MagicContainer.Card on: swipeLeft -> {
    deleteItem(it.index)
}

MagicData.Table on: rowClick -> { row ->
    navigate("details/${row.id}")
}
```

**Async Actions:**
```kotlin
MagicButton.Positive("Sign In") on: click -> async {
    val result = auth.login(user.email, user.password)
    if (result.success) {
        navigate("home")
    } else {
        showError(result.message)
    }
}
```

---

### 2.3 **Computed** - Derived State

**Purpose:** Calculate values based on other state (auto-updates)

**Syntax:**
```kotlin
computed: { <expression> }
```

**Examples:**

**Simple Computed:**
```kotlin
val cart = MagicState {
    var items: List<Item> = emptyList()
    var tax: Double = 0.08

    // Computed properties (auto-update when items or tax changes)
    val subtotal computed: { items.sumOf { it.price * it.quantity } }
    val taxAmount computed: { subtotal * tax }
    val total computed: { subtotal + taxAmount }
}

// Display (auto-updates when cart changes)
MagicText.Title("Total: $${cart.total}")
```

**Validation:**
```kotlin
val form = MagicState {
    var email: String = ""
    var password: String = ""

    val isEmailValid computed: { email.matches(Regex("[a-z0-9]+@[a-z]+\\.[a-z]{2,}")) }
    val isPasswordValid computed: { password.length >= 8 }
    val isFormValid computed: { isEmailValid && isPasswordValid }
}

// Button enabled/disabled based on validation
MagicButton.Positive("Sign In", enabled: form.isFormValid) on: click -> login()
```

**Formatting:**
```kotlin
val product = MagicState {
    var price: Double = 29.99
    var quantity: Int = 1

    val formattedPrice computed: { "$%.2f".format(price) }
    val total computed: { price * quantity }
    val formattedTotal computed: { "$%.2f".format(total) }
}

MagicText.Body("Price: ${product.formattedPrice}")
MagicText.Title("Total: ${product.formattedTotal}")
```

---

### 2.4 **Effect** - Side Effects

**Purpose:** Execute side effects when state changes (like useEffect in React)

**Syntax:**
```kotlin
effect(dependencies) { <action> }
```

**Examples:**

**API Call on Mount:**
```kotlin
val products = MagicState {
    var items: List<Product> = emptyList()
    var isLoading: Boolean = true

    effect(onMount) {
        isLoading = true
        items = api.getProducts()
        isLoading = false
    }
}
```

**Auto-Save:**
```kotlin
val editor = MagicState {
    var content: String = ""

    effect(watch: content, debounce: 1000) {
        // Auto-saves 1 second after user stops typing
        api.saveDraft(content)
    }
}
```

**Database Listener:**
```kotlin
val messages = MagicState {
    var list: List<Message> = emptyList()

    effect(onMount) {
        // Listen to real-time updates
        db.collection("messages")
            .listen { snapshot ->
                list = snapshot.toObjects()
            }
    }

    effect(onDispose) {
        // Cleanup listener
        db.removeListener()
    }
}
```

**Dependent Effects:**
```kotlin
val search = MagicState {
    var query: String = ""
    var results: List<Result> = emptyList()
    var isSearching: Boolean = false

    effect(watch: query, debounce: 500) {
        if (query.isNotEmpty()) {
            isSearching = true
            results = api.search(query)
            isSearching = false
        } else {
            results = emptyList()
        }
    }
}
```

---

### 2.5 **Workflow** - Multi-Step Business Logic

**Purpose:** Orchestrate complex multi-step processes with automatic error handling

**Syntax:**
```kotlin
workflow {
    step("<name>") { <action> }
    step("<name>") { <action> }
    onSuccess { <action> }
    onError { <action> }
}
```

**Examples:**

**Checkout Flow:**
```kotlin
MagicButton.Positive("Checkout") on: click -> workflow {
    step("validate") {
        if (!cart.isValid) throw ValidationError("Invalid cart")
    }

    step("calculate-total") {
        cart.total = cart.items.sumOf { it.price * it.quantity } * (1 + cart.tax)
    }

    step("process-payment") {
        val payment = paymentProvider.charge(cart.total, user.paymentMethod)
        if (!payment.success) throw PaymentError(payment.message)
    }

    step("create-order") {
        val order = api.createOrder(cart, user)
        cart.orderId = order.id
    }

    step("send-confirmation") {
        emailService.send(user.email, "Order Confirmation", order.details)
    }

    onSuccess {
        cart.clear()
        navigate("order/${cart.orderId}")
        showToast("Order placed successfully!")
    }

    onError { error ->
        showError("Checkout failed: ${error.message}")
        analytics.logError("checkout-failed", error)
    }
}
```

**Registration Flow:**
```kotlin
MagicButton.Positive("Sign Up") on: click -> workflow {
    step("validate-input") {
        if (!form.isValid) throw ValidationError("Please fix errors")
    }

    step("check-email-availability") {
        val exists = api.checkEmail(user.email)
        if (exists) throw EmailTakenError("Email already registered")
    }

    step("create-account") {
        val account = api.createAccount(user)
        user.id = account.id
    }

    step("send-verification") {
        emailService.sendVerification(user.email, user.id)
    }

    step("upload-avatar") {
        if (user.avatar != null) {
            val url = storage.upload(user.avatar)
            api.updateAvatar(user.id, url)
        }
    }

    onSuccess {
        navigate("verify-email")
        showToast("Account created! Check your email.")
    }

    onError { error ->
        when (error) {
            is EmailTakenError -> form.errors.email = error.message
            is ValidationError -> showError(error.message)
            else -> showError("Registration failed. Please try again.")
        }
    }
}
```

**Progress Tracking:**
```kotlin
MagicButton.Positive("Upload Files") on: click -> workflow {
    var progress = 0.0

    step("prepare-files", weight: 0.1) {
        progress = 0.1
        files.forEach { it.validate() }
    }

    step("upload-files", weight: 0.7) {
        files.forEachIndexed { index, file ->
            storage.upload(file) { uploadProgress ->
                progress = 0.1 + (0.7 * (index + uploadProgress) / files.size)
            }
        }
    }

    step("process-files", weight: 0.2) {
        api.processUploads(files.map { it.id })
        progress = 1.0
    }

    onProgress { value ->
        progressBar.value = value
    }

    onSuccess {
        showToast("Files uploaded successfully!")
    }
}
```

---

### 2.6 **API** - Declarative API Calls

**Purpose:** Simplify API integration with automatic loading/error states

**Syntax:**
```kotlin
api.<method>(url) {
    params { ... }
    headers { ... }
    body { ... }
    onSuccess { ... }
    onError { ... }
}
```

**Examples:**

**GET Request:**
```kotlin
val products = MagicState {
    var items: List<Product> = emptyList()
    var isLoading: Boolean = false
    var error: String? = null

    effect(onMount) {
        api.get("https://api.myapp.com/products") {
            params {
                "category" to "electronics"
                "limit" to 20
            }
            headers {
                "Authorization" to "Bearer ${user.token}"
            }
            onSuccess { response ->
                items = response.data.map { Product.fromJson(it) }
                isLoading = false
            }
            onError { error ->
                this.error = error.message
                isLoading = false
            }
        }
    }
}
```

**POST Request:**
```kotlin
MagicButton.Positive("Submit") on: click -> {
    api.post("https://api.myapp.com/users") {
        body {
            "name" to user.name
            "email" to user.email
            "age" to user.age
        }
        headers {
            "Content-Type" to "application/json"
        }
        onSuccess { response ->
            user.id = response.data.id
            navigate("profile/${user.id}")
        }
        onError { error ->
            showError("Failed to create account: ${error.message}")
        }
    }
}
```

**GraphQL:**
```kotlin
api.graphql("https://api.myapp.com/graphql") {
    query = """
        query GetUser($id: ID!) {
            user(id: $id) {
                name
                email
                posts {
                    title
                    content
                }
            }
        }
    """
    variables {
        "id" to userId
    }
    onSuccess { response ->
        user = response.data.user
    }
}
```

**WebSocket (Real-Time):**
```kotlin
val messages = MagicState {
    var list: List<Message> = emptyList()

    effect(onMount) {
        api.websocket("wss://api.myapp.com/chat/${roomId}") {
            onConnect {
                send("join", roomId)
            }
            onMessage { message ->
                list = list + Message.fromJson(message)
            }
            onDisconnect {
                showToast("Disconnected from chat")
            }
        }
    }
}
```

---

## 3. State Management

### 3.1 MagicState

**Purpose:** Reactive state container (like MobX, Vuex, Redux)

**Features:**
- **Automatic reactivity** - No manual `setState()` or `update()`
- **Computed properties** - Auto-calculated derived values
- **Effects** - Side effects with dependency tracking
- **Persistence** - Automatic save/restore
- **Time travel** - Undo/redo support
- **DevTools** - State inspection and debugging

**Example:**

```kotlin
val user = MagicState {
    var name: String = ""
    var email: String = ""
    var age: Int = 0
    var avatar: String? = null

    val isAdult computed: { age >= 18 }
    val initials computed: {
        name.split(" ").take(2).map { it.first() }.joinToString("")
    }

    effect(watch: name) {
        // Log every name change
        analytics.log("name_changed", name)
    }

    fun reset() {
        name = ""
        email = ""
        age = 0
        avatar = null
    }
}
```

### 3.2 MagicStore (Global State)

**Purpose:** App-wide state management

**Example:**

```kotlin
val AppStore = MagicStore {
    var isLoggedIn: Boolean = false
    var currentUser: User? = null
    var theme: Theme = Theme.Light
    var cart: Cart = Cart()

    val itemCount computed: { cart.items.size }
    val total computed: { cart.items.sumOf { it.price * it.quantity } }

    fun login(user: User) {
        isLoggedIn = true
        currentUser = user
    }

    fun logout() {
        isLoggedIn = false
        currentUser = null
        cart.clear()
    }
}

// Access anywhere
MagicText.Body("Items in cart: ${AppStore.itemCount}")
MagicButton.Positive("Logout") on: click -> AppStore.logout()
```

### 3.3 Persistence

**Automatic Save/Restore:**

```kotlin
val settings = MagicState {
    var darkMode: Boolean = false
    var language: String = "en"
    var notifications: Boolean = true
} persist: {
    storage = LocalStorage
    key = "app-settings"
    strategy = Auto  // Save on every change
}

// Automatically restored on app launch
```

**Manual Persistence:**

```kotlin
val draft = MagicState {
    var title: String = ""
    var content: String = ""
}

// Manual save
MagicButton.Positive("Save Draft") on: click -> {
    draft.persist(key: "draft-${draftId}")
}

// Manual restore
effect(onMount) {
    draft.restore(key: "draft-${draftId}")
}
```

---

## 4. Advanced Patterns

### 4.1 Form Validation

**Declarative Validation:**

```kotlin
val form = MagicForm {
    field("email") {
        value = user.email
        required = true
        pattern = Regex("[a-z0-9]+@[a-z]+\\.[a-z]{2,}")
        errorMessage = "Please enter a valid email"
    }

    field("password") {
        value = user.password
        required = true
        minLength = 8
        pattern = Regex("^(?=.*[A-Z])(?=.*[0-9]).*$")
        errorMessage = "Password must be 8+ chars with uppercase and number"
    }

    field("age") {
        value = user.age
        required = true
        min = 18
        max = 120
        errorMessage = "Age must be 18-120"
    }

    val isValid computed: { fields.all { it.isValid } }
}

// UI
MagicTextField.Email(bind: form.email.value, error: form.email.error)
MagicTextField.Password(bind: form.password.value, error: form.password.error)
MagicForm.Slider(bind: form.age.value, error: form.age.error)

MagicButton.Positive("Submit", enabled: form.isValid) on: click -> submit()
```

### 4.2 Pagination

**Infinite Scroll:**

```kotlin
val products = MagicState {
    var items: List<Product> = emptyList()
    var page: Int = 1
    var hasMore: Boolean = true
    var isLoading: Boolean = false

    fun loadMore() {
        if (isLoading || !hasMore) return

        isLoading = true
        api.get("https://api.myapp.com/products?page=$page") {
            onSuccess { response ->
                items = items + response.data
                hasMore = response.hasMore
                page++
                isLoading = false
            }
        }
    }

    effect(onMount) {
        loadMore()
    }
}

MagicLayout.LazyColumn {
    items.forEach { product ->
        MagicData.Card(product)
    }

    if (products.hasMore) {
        MagicFeedback.ProgressCircular() on: visible -> products.loadMore()
    }
}
```

### 4.3 Real-Time Sync

**Firestore Listener:**

```kotlin
val messages = MagicState {
    var list: List<Message> = emptyList()

    effect(onMount) {
        db.collection("messages")
            .where("roomId", "==", roomId)
            .orderBy("timestamp", "desc")
            .limit(50)
            .listen { snapshot ->
                list = snapshot.toObjects<Message>()
            }
    }
}

MagicLayout.LazyColumn {
    messages.list.forEach { message ->
        MagicContainer.Card {
            MagicText.Body(message.content)
            MagicText.Caption(message.timestamp)
        }
    }
}
```

### 4.4 Optimistic Updates

**Instant UI Updates:**

```kotlin
MagicButton.Positive("Like") on: click -> {
    // Update UI immediately (optimistic)
    post.likes++
    post.isLiked = true

    // Send API request
    api.post("https://api.myapp.com/posts/${post.id}/like") {
        onError {
            // Rollback on error
            post.likes--
            post.isLiked = false
            showError("Failed to like post")
        }
    }
}
```

---

## 5. Performance Optimization

### 5.1 Memoization

**Expensive Computed Values:**

```kotlin
val dashboard = MagicState {
    var sales: List<Sale> = emptyList()

    // Memoized - only recalculates if sales changes
    val revenue memoized: { sales.sumOf { it.amount } }
    val averageOrder memoized: { revenue / sales.size }
    val topProducts memoized: {
        sales.groupBy { it.productId }
            .map { (id, sales) -> id to sales.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(10)
    }
}
```

### 5.2 Lazy Loading

**Load Data On-Demand:**

```kotlin
val profile = MagicState {
    var user: User? = null
    var posts: List<Post>? = null  // Not loaded initially

    effect(onMount) {
        user = api.getUser(userId)
    }

    fun loadPosts() {
        if (posts != null) return  // Already loaded

        api.get("https://api.myapp.com/users/${userId}/posts") {
            onSuccess { response ->
                posts = response.data
            }
        }
    }
}

MagicButton.Positive("View Posts") on: click -> profile.loadPosts()
```

### 5.3 Debouncing

**Prevent Excessive API Calls:**

```kotlin
val search = MagicState {
    var query: String = ""
    var results: List<Result> = emptyList()

    effect(watch: query, debounce: 500) {  // Wait 500ms after typing stops
        if (query.isNotEmpty()) {
            results = api.search(query)
        }
    }
}

MagicTextField.Standard("Search...", bind: search.query)
```

---

## 6. Complete Examples

### 6.1 E-Commerce Product Page

```kotlin
val product = MagicState {
    var id: String = productId
    var name: String = ""
    var price: Double = 0.0
    var images: List<String> = emptyList()
    var description: String = ""
    var quantity: Int = 1
    var inStock: Boolean = true

    val total computed: { price * quantity }
    val formattedTotal computed: { "$%.2f".format(total) }

    effect(onMount) {
        api.get("https://api.myapp.com/products/$id") {
            onSuccess { response ->
                name = response.data.name
                price = response.data.price
                images = response.data.images
                description = response.data.description
                inStock = response.data.inStock
            }
        }
    }

    fun addToCart() {
        AppStore.cart.add(this)
        showToast("Added to cart!")
    }
}

MagicScreen.Product {
    MagicMedia.ImageCarousel(images: product.images)

    MagicText.Title(product.name)
    MagicText.Headline("$${product.price}")
    MagicText.Body(product.description)

    MagicForm.Stepper(bind: product.quantity, min: 1, max: 10)
    MagicText.Title("Total: ${product.formattedTotal}")

    MagicButton.Positive("Add to Cart", enabled: product.inStock) on: click -> product.addToCart()
}
```

### 6.2 Real-Time Chat

```kotlin
val chat = MagicState {
    var messages: List<Message> = emptyList()
    var input: String = ""
    var isTyping: Boolean = false

    effect(onMount) {
        // Real-time message listener
        api.websocket("wss://api.myapp.com/chat/${roomId}") {
            onMessage { message ->
                messages = messages + Message.fromJson(message)
            }
            onTyping { userId ->
                isTyping = userId != currentUser.id
            }
        }
    }

    fun send() {
        if (input.isEmpty()) return

        api.post("https://api.myapp.com/chat/${roomId}/send") {
            body {
                "content" to input
                "userId" to currentUser.id
            }
            onSuccess {
                input = ""
            }
        }
    }

    effect(watch: input, debounce: 500) {
        if (input.isNotEmpty()) {
            api.post("https://api.myapp.com/chat/${roomId}/typing") {
                body { "userId" to currentUser.id }
            }
        }
    }
}

MagicScreen.Chat {
    MagicLayout.LazyColumn {
        chat.messages.forEach { message ->
            MagicContainer.Card {
                MagicText.Body(message.content)
                MagicText.Caption(message.timestamp)
            }
        }
    }

    if (chat.isTyping) {
        MagicFeedback.TypingIndicator()
    }

    MagicTextField.Standard("Type a message...", bind: chat.input)
    MagicButton.Icon("send") on: click -> chat.send()
}
```

### 6.3 Dashboard with Charts

```kotlin
val dashboard = MagicState {
    var sales: List<Sale> = emptyList()
    var timeRange: String = "7d"  // 7d, 30d, 90d, 1y

    val revenue computed: { sales.sumOf { it.amount } }
    val formattedRevenue computed: { "$%.2f".format(revenue) }
    val averageOrder computed: { revenue / sales.size }
    val chartData computed: {
        sales.groupBy { it.date }
            .map { (date, sales) -> date to sales.sumOf { it.amount } }
            .sortedBy { it.first }
    }

    effect(watch: timeRange) {
        api.get("https://api.myapp.com/sales?range=$timeRange") {
            onSuccess { response ->
                sales = response.data
            }
        }
    }
}

MagicScreen.Dashboard {
    MagicNav.TopAppBar("Sales Dashboard")

    MagicForm.SegmentedControl(
        options: ["7d", "30d", "90d", "1y"],
        bind: dashboard.timeRange
    )

    MagicData.Card {
        MagicText.Headline("Revenue")
        MagicText.Title(dashboard.formattedRevenue)
    }

    MagicData.Chart.Line(
        data: dashboard.chartData,
        height: 300,
        animated: true
    )

    MagicData.Table.Sortable(
        data: dashboard.sales,
        columns: ["Date", "Amount", "Customer"]
    )
}
```

---

## 7. Integration with VoiceOS

**Voice Commands Automatically Work:**

```kotlin
val cart = MagicState {
    var items: List<Item> = emptyList()

    fun add(item: Item) {
        items = items + item
    }

    fun remove(index: Int) {
        items = items.filterIndexed { i, _ -> i != index }
    }

    fun clear() {
        items = emptyList()
    }
}

MagicButton.Positive("Add to Cart") on: click -> cart.add(currentProduct)
// Automatically: "tap add to cart", "add", "add to cart"

MagicButton.Negative("Clear Cart") on: click -> cart.clear()
// Automatically: "tap clear cart", "clear", "clear cart"
```

**Voice-Triggered Workflows:**

```kotlin
MagicButton.Positive("Checkout") on: {
    click -> checkout()
    voice("checkout") -> checkout()
    voice("pay now") -> checkout()
    voice("place order") -> checkout()
}

fun checkout() = workflow {
    step("validate") { ... }
    step("process-payment") { ... }
    step("create-order") { ... }
    onSuccess { navigate("success") }
}
```

---

## 8. Code Reduction Comparison

### Before (Manual State Management)

```kotlin
// 50+ lines
@Composable
fun CartScreen(viewModel: CartViewModel) {
    val items by viewModel.items.collectAsState()
    val total by viewModel.total.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCart()
    }

    Column {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (error != null) {
            Text("Error: $error")
        } else {
            LazyColumn {
                items(items) { item ->
                    CartItem(
                        item = item,
                        onQuantityChange = { quantity ->
                            viewModel.updateQuantity(item.id, quantity)
                        },
                        onRemove = {
                            viewModel.removeItem(item.id)
                        }
                    )
                }
            }
            Text("Total: $${total}")
            Button(
                onClick = {
                    viewModel.checkout()
                },
                enabled = items.isNotEmpty()
            ) {
                Text("Checkout")
            }
        }
    }
}
```

### After (Magic Mode with Logic Wiring)

```kotlin
// 10 lines
val cart = MagicState {
    var items: List<Item> = emptyList()
    val total computed: { items.sumOf { it.price * it.quantity } }
}

MagicScreen.Cart {
    MagicData.List(items: cart.items) { item ->
        MagicData.Card(item) on: remove -> cart.items -= item
    }
    MagicText.Title("Total: $${cart.total}")
    MagicButton.Positive("Checkout", enabled: cart.items.isNotEmpty()) on: click -> checkout()
}
```

**Reduction:** 50 lines → 10 lines = **80% reduction**

---

## 9. Benefits Summary

| Feature | Manual Implementation | Magic Mode Logic Wiring | Reduction |
|---------|----------------------|------------------------|-----------|
| **State Management** | 30-50 lines | 3-5 lines | **85-90%** |
| **Event Handling** | 10-15 lines | 1 line | **90-95%** |
| **API Calls** | 20-30 lines | 5-8 lines | **75-85%** |
| **Form Validation** | 40-60 lines | 10-15 lines | **75-80%** |
| **Real-Time Sync** | 50-80 lines | 10-15 lines | **80-85%** |
| **Workflows** | 100-150 lines | 20-30 lines | **80-85%** |

**Overall:** 90-95% less logic code with automatic reactivity, error handling, and performance optimization.

---

## 10. Next Steps

1. **Implement Android Logic Wiring** (Q1 2026)
2. **Port to iOS** (Q2 2026)
3. **Port to Web** (Q3 2026)
4. **Port to Desktop** (Q4 2026)
5. **Visual Logic Editor** (2027) - Drag-drop workflow builder

---

**Document Version:** v1.0.0
**Last Updated:** 2025-11-16
**Framework:** IDEACODE v8.5
**Status:** Active - Implementation in progress
