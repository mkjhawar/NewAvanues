package com.augmentalis.magicelements.renderer.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.augmentalis.magicelements.core.Component
import com.augmentalis.magicelements.core.Renderer
import com.augmentalis.magicelements.core.Theme
import com.augmentalis.magicelements.dsl.*
import com.augmentalis.magicelements.renderer.android.mappers.*

/**
 * ComposeRenderer - Converts MagicElements components to Jetpack Compose UI
 *
 * This renderer maps the MagicElements component tree to native Jetpack Compose
 * functions, supporting all modifiers, theming, and state management.
 */
class ComposeRenderer : Renderer {
    override val platform = Renderer.Platform.Android

    private val themeConverter = ThemeConverter()
    private var currentTheme: Theme? = null

    // Component mappers
    private val columnMapper = ColumnMapper()
    private val rowMapper = RowMapper()
    private val containerMapper = ContainerMapper()
    private val scrollViewMapper = ScrollViewMapper()
    private val cardMapper = CardMapper()
    private val textMapper = TextMapper()
    private val buttonMapper = ButtonMapper()
    private val textFieldMapper = TextFieldMapper()
    private val checkboxMapper = CheckboxMapper()
    private val switchMapper = SwitchMapper()
    private val iconMapper = IconMapper()
    private val imageMapper = ImageMapper()

    override fun render(component: Component): Any {
        return when (component) {
            is ColumnComponent -> columnMapper.map(component, this)
            is RowComponent -> rowMapper.map(component, this)
            is ContainerComponent -> containerMapper.map(component, this)
            is ScrollViewComponent -> scrollViewMapper.map(component, this)
            is CardComponent -> cardMapper.map(component, this)
            is TextComponent -> textMapper.map(component, this)
            is ButtonComponent -> buttonMapper.map(component, this)
            is TextFieldComponent -> textFieldMapper.map(component, this)
            is CheckboxComponent -> checkboxMapper.map(component, this)
            is SwitchComponent -> switchMapper.map(component, this)
            is IconComponent -> iconMapper.map(component, this)
            is ImageComponent -> imageMapper.map(component, this)
            else -> throw IllegalArgumentException("Unsupported component type: ${component::class.simpleName}")
        }
    }

    override fun applyTheme(theme: Theme) {
        currentTheme = theme
    }

    fun getTheme(): Theme? = currentTheme

    /**
     * Render a component as a @Composable function
     */
    @Composable
    fun RenderComponent(component: Component) {
        val composable = render(component) as @Composable () -> Unit
        composable()
    }

    /**
     * Render component with theme applied
     */
    @Composable
    fun RenderWithTheme(component: Component, theme: Theme? = null) {
        val activeTheme = theme ?: currentTheme

        if (activeTheme != null) {
            themeConverter.WithMaterialTheme(activeTheme) {
                RenderComponent(component)
            }
        } else {
            RenderComponent(component)
        }
    }
}

/**
 * Base mapper interface for component conversion
 */
interface ComponentMapper<T : Component> {
    @Composable
    fun map(component: T, renderer: ComposeRenderer): @Composable () -> Unit
}

/**
 * CompositionLocal for accessing the renderer in nested components
 */
val LocalComposeRenderer = compositionLocalOf<ComposeRenderer> {
    error("No ComposeRenderer provided")
}
