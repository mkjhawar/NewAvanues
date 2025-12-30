package com.augmentalis.avaelements.state.examples

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.dsl.*
import com.augmentalis.avaelements.state.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay

/**
 * Example 1: Simple Counter with State
 *
 * Demonstrates basic state management with a counter that increments and decrements.
 */
fun counterExample() {
    val counter = mutableStateOf(0)

    val ui = object : Component {
        override val id: String? = "counter_example"
        override val style: ComponentStyle? = null
        override val modifiers: List<Modifier> = emptyList()

        override fun render(renderer: Renderer): Any {
            return ColumnComponent(
                id = "counter_column",
                style = ComponentStyle(padding = Spacing.all(16f)),
                children = listOf(
                    TextComponent(
                        text = "Count: ${counter.current()}",
                        font = Font.Title,
                        color = Color.Black
                    ),
                    RowComponent(
                        id = "button_row",
                        arrangement = Arrangement.SpaceEvenly,
                        children = listOf(
                            ButtonComponent(
                                text = "Decrement",
                                buttonStyle = ButtonScope.ButtonStyle.Secondary,
                                onClick = { counter.update { it - 1 } }
                            ),
                            ButtonComponent(
                                text = "Reset",
                                buttonStyle = ButtonScope.ButtonStyle.Tertiary,
                                onClick = { counter.setValue(0) }
                            ),
                            ButtonComponent(
                                text = "Increment",
                                buttonStyle = ButtonScope.ButtonStyle.Primary,
                                onClick = { counter.update { it + 1 } }
                            )
                        )
                    )
                )
            ).render(renderer)
        }
    }
}

/**
 * Example 2: Form with Validation
 *
 * Demonstrates form state management with validation, error handling,
 * and two-way data binding.
 */
fun loginFormExample() {
    val form = buildForm {
        field("email", "") {
            required(true)
            email("Please enter a valid email address")
        }
        field("password", "") {
            required(true)
            minLength(8, "Password must be at least 8 characters")
        }
    }

    val emailField = form.getField<String>("email")!!
    val passwordField = form.getField<String>("password")!!

    val ui = object : Component {
        override val id: String? = "login_form"
        override val style: ComponentStyle? = null
        override val modifiers: List<Modifier> = emptyList()

        override fun render(renderer: Renderer): Any {
            return ColumnComponent(
                id = "form_column",
                style = ComponentStyle(padding = Spacing.all(24f)),
                children = listOf(
                    TextComponent(
                        text = "Login",
                        font = Font.Title,
                        color = Color.Black
                    ),
                    TextFieldComponent(
                        value = emailField.value.value,
                        placeholder = "Email",
                        label = "Email Address",
                        isError = emailField.error.value != null,
                        errorMessage = emailField.error.value,
                        onValueChange = { newValue ->
                            emailField.setValue(newValue)
                            if (emailField.isTouched.value) {
                                emailField.validate()
                            }
                        }
                    ),
                    TextFieldComponent(
                        value = passwordField.value.value,
                        placeholder = "Password",
                        label = "Password",
                        isError = passwordField.error.value != null,
                        errorMessage = passwordField.error.value,
                        onValueChange = { newValue ->
                            passwordField.setValue(newValue)
                            if (passwordField.isTouched.value) {
                                passwordField.validate()
                            }
                        }
                    ),
                    ButtonComponent(
                        text = if (form.isSubmitting.value) "Logging in..." else "Login",
                        buttonStyle = ButtonScope.ButtonStyle.Primary,
                        enabled = !form.isSubmitting.value,
                        onClick = {
                            if (form.validate()) {
                                // Submit form
                                println("Email: ${emailField.getValue()}")
                                println("Password: ${passwordField.getValue()}")
                            }
                        }
                    )
                )
            ).render(renderer)
        }
    }
}

/**
 * Example 3: ViewModel with Async Operations
 *
 * Demonstrates using MagicViewModel for managing complex state with
 * asynchronous operations and loading states.
 */
class UserProfileViewModel : AsyncViewModel() {
    private val _username = mutableState("")
    val username: StateFlow<String> = _username.asState()

    private val _bio = mutableState("")
    val bio: StateFlow<String> = _bio.asState()

    private val _avatar = mutableState("")
    val avatar: StateFlow<String> = _avatar.asState()

    private val _followers = mutableState(0)
    val followers: StateFlow<Int> = _followers.asState()

    init {
        loadProfile()
    }

    fun loadProfile() {
        launchAsync {
            // Simulate API call
            delay(1000)
            _username.value = "john_doe"
            _bio.value = "Software developer and tech enthusiast"
            _avatar.value = "https://example.com/avatar.jpg"
            _followers.value = 1234
        }
    }

    fun updateBio(newBio: String) {
        launchAsync {
            _bio.value = newBio
            // Simulate API call to update
            delay(500)
        }
    }

    fun followUser() {
        launchAsync {
            _followers.value += 1
            // Simulate API call
            delay(300)
        }
    }
}

/**
 * Example 4: Reactive List with State
 *
 * Demonstrates managing a list of items with reactive updates.
 */
fun todoListExample() {
    data class TodoItem(
        val id: String,
        val text: String,
        val completed: Boolean = false
    )

    val todos = collectionBindingOf<TodoItem>()
    val inputText = mutableStateOf("")

    val ui = object : Component {
        override val id: String? = "todo_list"
        override val style: ComponentStyle? = null
        override val modifiers: List<Modifier> = emptyList()

        override fun render(renderer: Renderer): Any {
            return ColumnComponent(
                id = "todo_column",
                style = ComponentStyle(padding = Spacing.all(16f)),
                children = listOf(
                    TextComponent(
                        text = "Todo List (${todos.size()} items)",
                        font = Font.Title,
                        color = Color.Black
                    ),
                    RowComponent(
                        id = "input_row",
                        children = listOf(
                            TextFieldComponent(
                                value = inputText.current(),
                                placeholder = "Add a new todo...",
                                onValueChange = { inputText.setValue(it) }
                            ),
                            ButtonComponent(
                                text = "Add",
                                buttonStyle = ButtonScope.ButtonStyle.Primary,
                                onClick = {
                                    if (inputText.current().isNotEmpty()) {
                                        todos.add(TodoItem(
                                            id = System.currentTimeMillis().toString(),
                                            text = inputText.current()
                                        ))
                                        inputText.setValue("")
                                    }
                                }
                            )
                        )
                    ),
                    // Render todo items
                    ColumnComponent(
                        id = "todo_items",
                        children = todos.get().mapIndexed { index, item ->
                            RowComponent(
                                id = "todo_${item.id}",
                                children = listOf(
                                    CheckboxComponent(
                                        label = item.text,
                                        checked = item.completed,
                                        onCheckedChange = { checked ->
                                            todos.updateAt(index, item.copy(completed = checked))
                                        }
                                    ),
                                    ButtonComponent(
                                        text = "Delete",
                                        buttonStyle = ButtonScope.ButtonStyle.Text,
                                        onClick = { todos.removeAt(index) }
                                    )
                                )
                            )
                        }
                    )
                )
            ).render(renderer)
        }
    }
}

/**
 * Example 5: Derived State
 *
 * Demonstrates computed state that derives from other states.
 */
fun shoppingCartExample() {
    data class CartItem(
        val name: String,
        val price: Double,
        val quantity: Int
    )

    val cartItems = collectionBindingOf<CartItem>()

    // Derived state: total price
    val totalPrice = derivedStateOf {
        cartItems.get().sumOf { it.price * it.quantity }
    }

    // Derived state: item count
    val itemCount = derivedStateOf {
        cartItems.get().sumOf { it.quantity }
    }

    val ui = object : Component {
        override val id: String? = "shopping_cart"
        override val style: ComponentStyle? = null
        override val modifiers: List<Modifier> = emptyList()

        override fun render(renderer: Renderer): Any {
            return ColumnComponent(
                id = "cart_column",
                style = ComponentStyle(padding = Spacing.all(16f)),
                children = listOf(
                    TextComponent(
                        text = "Shopping Cart",
                        font = Font.Title,
                        color = Color.Black
                    ),
                    TextComponent(
                        text = "Items: ${itemCount.value}",
                        font = Font.Body,
                        color = Color.Gray
                    ),
                    // Cart items list
                    ColumnComponent(
                        id = "cart_items",
                        children = cartItems.get().map { item ->
                            RowComponent(
                                id = "item_${item.name}",
                                arrangement = Arrangement.SpaceBetween,
                                children = listOf(
                                    TextComponent(
                                        text = item.name,
                                        font = Font.Body,
                                        color = Color.Black
                                    ),
                                    TextComponent(
                                        text = "$${item.price * item.quantity}",
                                        font = Font.Body,
                                        color = Color.Black
                                    )
                                )
                            )
                        }
                    ),
                    // Total
                    RowComponent(
                        id = "total_row",
                        arrangement = Arrangement.SpaceBetween,
                        children = listOf(
                            TextComponent(
                                text = "Total:",
                                font = Font.Headline,
                                color = Color.Black
                            ),
                            TextComponent(
                                text = "$${totalPrice.value}",
                                font = Font.Headline,
                                color = Color.Black
                            )
                        )
                    ),
                    ButtonComponent(
                        text = "Checkout",
                        buttonStyle = ButtonScope.ButtonStyle.Primary,
                        enabled = cartItems.size() > 0,
                        onClick = { /* Handle checkout */ }
                    )
                )
            ).render(renderer)
        }
    }
}

/**
 * Example 6: State Persistence
 *
 * Demonstrates saving and restoring state across app restarts.
 */
fun settingsExample() {
    val stateManager = StateManager(InMemoryStatePersistence())

    val darkMode = mutableStateOf(false).persist("dark_mode", stateManager)
    val fontSize = mutableStateOf(16).persist("font_size", stateManager)
    val notifications = mutableStateOf(true).persist("notifications", stateManager)

    val ui = object : Component {
        override val id: String? = "settings"
        override val style: ComponentStyle? = null
        override val modifiers: List<Modifier> = emptyList()

        override fun render(renderer: Renderer): Any {
            return ColumnComponent(
                id = "settings_column",
                style = ComponentStyle(padding = Spacing.all(16f)),
                children = listOf(
                    TextComponent(
                        text = "Settings",
                        font = Font.Title,
                        color = Color.Black
                    ),
                    RowComponent(
                        id = "dark_mode_row",
                        arrangement = Arrangement.SpaceBetween,
                        children = listOf(
                            TextComponent(
                                text = "Dark Mode",
                                font = Font.Body,
                                color = Color.Black
                            ),
                            SwitchComponent(
                                checked = darkMode.current(),
                                onCheckedChange = { darkMode.setValue(it) }
                            )
                        )
                    ),
                    RowComponent(
                        id = "notifications_row",
                        arrangement = Arrangement.SpaceBetween,
                        children = listOf(
                            TextComponent(
                                text = "Notifications",
                                font = Font.Body,
                                color = Color.Black
                            ),
                            SwitchComponent(
                                checked = notifications.current(),
                                onCheckedChange = { notifications.setValue(it) }
                            )
                        )
                    ),
                    TextComponent(
                        text = "Font Size: ${fontSize.current()}",
                        font = Font.Body,
                        color = Color.Black
                    )
                )
            ).render(renderer)
        }
    }
}

/**
 * Example 7: Conditional Rendering
 *
 * Demonstrates showing/hiding UI based on state.
 */
fun conditionalRenderingExample() {
    val isLoggedIn = mutableStateOf(false)
    val username = mutableStateOf("")

    val ui = object : Component {
        override val id: String? = "conditional_example"
        override val style: ComponentStyle? = null
        override val modifiers: List<Modifier> = emptyList()

        override fun render(renderer: Renderer): Any {
            return ColumnComponent(
                id = "main_column",
                style = ComponentStyle(padding = Spacing.all(16f)),
                children = if (isLoggedIn.current()) {
                    // Logged in view
                    listOf(
                        TextComponent(
                            text = "Welcome, ${username.current()}!",
                            font = Font.Title,
                            color = Color.Black
                        ),
                        ButtonComponent(
                            text = "Logout",
                            buttonStyle = ButtonScope.ButtonStyle.Secondary,
                            onClick = { isLoggedIn.setValue(false) }
                        )
                    )
                } else {
                    // Login view
                    listOf(
                        TextComponent(
                            text = "Please log in",
                            font = Font.Title,
                            color = Color.Black
                        ),
                        TextFieldComponent(
                            value = username.current(),
                            placeholder = "Username",
                            onValueChange = { username.setValue(it) }
                        ),
                        ButtonComponent(
                            text = "Login",
                            buttonStyle = ButtonScope.ButtonStyle.Primary,
                            onClick = { isLoggedIn.setValue(true) }
                        )
                    )
                }
            ).render(renderer)
        }
    }
}

/**
 * Example 8: Complex State with StatefulViewModel
 *
 * Demonstrates managing complex UI state with a ViewModel.
 */
data class ChatState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isTyping: Boolean = false,
    val error: String? = null
)

data class Message(
    val id: String,
    val text: String,
    val sender: String,
    val timestamp: Long
)

class ChatViewModel : StatefulViewModel<ChatState>(ChatState()) {

    fun updateInput(text: String) {
        updateState { copy(inputText = text) }
    }

    fun sendMessage() {
        val text = currentState().inputText
        if (text.isBlank()) return

        launchAsync {
            updateState { copy(isTyping = true) }

            val newMessage = Message(
                id = System.currentTimeMillis().toString(),
                text = text,
                sender = "You",
                timestamp = System.currentTimeMillis()
            )

            updateState {
                copy(
                    messages = messages + newMessage,
                    inputText = "",
                    isTyping = false
                )
            }

            // Simulate bot response
            delay(1000)
            val botMessage = Message(
                id = (System.currentTimeMillis() + 1).toString(),
                text = "Thanks for your message!",
                sender = "Bot",
                timestamp = System.currentTimeMillis()
            )

            updateState {
                copy(messages = messages + botMessage)
            }
        }
    }

    fun clearMessages() {
        updateState { copy(messages = emptyList()) }
    }
}
