package net.ideahq.avamagic.codegen.generators

import net.ideahq.avamagic.codegen.ast.*

/**
 * CodeGenerator - Base interface for code generators
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
interface CodeGenerator {
    /**
     * Generate complete screen code
     */
    fun generate(screen: ScreenNode): GeneratedCode

    /**
     * Generate single component code
     */
    fun generateComponent(component: ComponentNode): String
}

/**
 * Generated Code Result
 */
data class GeneratedCode(
    val code: String,
    val language: Language,
    val platform: Platform,
    val imports: List<String> = emptyList(),
    val dependencies: List<String> = emptyList()
)

/**
 * Programming Language
 */
enum class Language {
    KOTLIN,
    SWIFT,
    TYPESCRIPT,
    JAVASCRIPT
}

/**
 * Target Platform
 */
enum class Platform {
    ANDROID,
    IOS,
    WEB,
    DESKTOP
}

/**
 * Code Generator Factory
 */
object CodeGeneratorFactory {
    fun create(platform: Platform, language: Language? = null): CodeGenerator {
        return when (platform) {
            Platform.ANDROID -> KotlinComposeGenerator()
            Platform.IOS -> SwiftUIGenerator()
            Platform.WEB -> {
                when (language) {
                    Language.TYPESCRIPT -> ReactTypeScriptGenerator()
                    else -> ReactTypeScriptGenerator() // Default to TypeScript
                }
            }
            Platform.DESKTOP -> KotlinComposeGenerator() // Compose Desktop
        }
    }
}
