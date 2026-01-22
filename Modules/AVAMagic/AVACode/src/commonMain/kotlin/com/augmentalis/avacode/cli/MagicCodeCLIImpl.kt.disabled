package com.augmentalis.avacode.cli

import com.augmentalis.avacode.dsl.JsonDSLParser
import com.augmentalis.avacode.generators.*

/**
 * AvaCodeCLIImpl - Complete CLI implementation with file I/O
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class AvaCodeCLIImpl {

    private val parser = JsonDSLParser()

    fun execute(args: Array<String>): Int {
        if (args.isEmpty()) {
            printHelp()
            return 0
        }

        return when (args[0]) {
            "generate", "gen", "g" -> handleGenerate(args.drop(1))
            "validate", "val", "v" -> handleValidate(args.drop(1))
            "version" -> handleVersion()
            "help", "--help", "-h" -> {
                printHelp()
                0
            }
            else -> {
                println("Unknown command: ${args[0]}")
                println("Run 'avacode help' for usage information")
                1
            }
        }
    }

    private fun handleGenerate(args: List<String>): Int {
        var inputFile: String? = null
        var outputFile: String? = null
        var platform: Platform = Platform.ANDROID
        var language: Language? = null

        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "-i", "--input" -> {
                    inputFile = args.getOrNull(++i)
                }
                "-o", "--output" -> {
                    outputFile = args.getOrNull(++i)
                }
                "-p", "--platform" -> {
                    val platformStr = args.getOrNull(++i)
                    platform = when (platformStr?.lowercase()) {
                        "android" -> Platform.ANDROID
                        "ios" -> Platform.IOS
                        "web" -> Platform.WEB
                        "desktop" -> Platform.DESKTOP
                        else -> {
                            println("Invalid platform: $platformStr")
                            return 1
                        }
                    }
                }
                "-l", "--language" -> {
                    val langStr = args.getOrNull(++i)
                    language = when (langStr?.lowercase()) {
                        "kotlin" -> Language.KOTLIN
                        "swift" -> Language.SWIFT
                        "typescript", "ts" -> Language.TYPESCRIPT
                        "javascript", "js" -> Language.JAVASCRIPT
                        else -> {
                            println("Invalid language: $langStr")
                            return 1
                        }
                    }
                }
                else -> {
                    println("Unknown option: ${args[i]}")
                    return 1
                }
            }
            i++
        }

        if (inputFile == null) {
            println("Error: Input file required (-i/--input)")
            return 1
        }

        return generate(inputFile, outputFile, platform, language)
    }

    private fun generate(
        inputFile: String,
        outputFile: String?,
        platform: Platform,
        language: Language?
    ): Int {
        try {
            // Check if input file exists
            if (!FileIO.fileExists(inputFile)) {
                println("Error: Input file not found: $inputFile")
                return 1
            }

            // Read input file
            println("Reading: $inputFile")
            val dslContent = FileIO.readFile(inputFile)

            // Parse DSL
            println("Parsing DSL...")
            val screenResult = parser.parseScreen(dslContent)
            if (screenResult.isFailure) {
                println("Parse error: ${screenResult.exceptionOrNull()?.message}")
                return 1
            }

            val screen = screenResult.getOrThrow()
            println("✓ Parsed screen: ${screen.name}")

            // Generate code
            println("Generating ${platform.name} code...")
            val generator = CodeGeneratorFactory.create(platform, language)
            val generated = generator.generate(screen)

            // Determine output file
            val output = outputFile ?: "${screen.name}.${getFileExtension(generated.language)}"

            // Write output
            println("Writing: $output")
            FileIO.writeFile(output, generated.code)

            // Success message
            println()
            println("✅ Code generation successful!")
            println("   Platform: ${generated.platform}")
            println("   Language: ${generated.language}")
            println("   Output: $output")
            println("   Lines: ${generated.code.lines().size}")

            // Print dependencies if any
            if (generated.dependencies.isNotEmpty()) {
                println()
                println("Dependencies:")
                generated.dependencies.forEach { dep ->
                    println("   - $dep")
                }
            }

            return 0

        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
            return 1
        }
    }

    private fun handleValidate(args: List<String>): Int {
        val inputFile = args.getOrNull(0)
        if (inputFile == null) {
            println("Error: Input file required")
            return 1
        }

        try {
            // Check if file exists
            if (!FileIO.fileExists(inputFile)) {
                println("Error: File not found: $inputFile")
                return 1
            }

            // Read file
            println("Validating: $inputFile")
            val dslContent = FileIO.readFile(inputFile)

            // Validate
            val result = parser.validate(dslContent)

            if (result.isValid) {
                println()
                println("✅ Validation passed")
                if (result.warnings.isNotEmpty()) {
                    println()
                    println("⚠️  Warnings (${result.warnings.size}):")
                    result.warnings.forEach { warning ->
                        println("   Line ${warning.line}: ${warning.message}")
                    }
                }
                return 0
            } else {
                println()
                println("❌ Validation failed (${result.errors.size} errors)")
                println()
                println("Errors:")
                result.errors.forEach { error ->
                    println("   Line ${error.line}: ${error.message}")
                }

                if (result.warnings.isNotEmpty()) {
                    println()
                    println("Warnings:")
                    result.warnings.forEach { warning ->
                        println("   Line ${warning.line}: ${warning.message}")
                    }
                }

                return 1
            }

        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
            return 1
        }
    }

    private fun handleVersion(): Int {
        println()
        println("╔════════════════════════════════════════╗")
        println("║     AvaCode CLI v1.0.0               ║")
        println("║     AvaUI DSL Code Generator         ║")
        println("║     Part of IDEACODE 5.0 Framework     ║")
        println("╚════════════════════════════════════════╝")
        println()
        println("Supported platforms:")
        println("  • Android (Jetpack Compose)")
        println("  • iOS (SwiftUI)")
        println("  • Web (React + TypeScript)")
        println("  • Desktop (Compose Desktop)")
        println()
        println("Supported languages:")
        println("  • Kotlin")
        println("  • Swift")
        println("  • TypeScript")
        println("  • JavaScript")
        println()
        println("Created by Manoj Jhawar, manoj@ideahq.net")
        println()
        return 0
    }

    private fun printHelp() {
        println("""
╔════════════════════════════════════════════════════════════════════╗
║                    AvaCode - AvaUI DSL Code Generator          ║
╚════════════════════════════════════════════════════════════════════╝

USAGE:
    avacode <command> [options]

COMMANDS:
    generate (gen, g)     Generate code from AvaUI DSL
    validate (val, v)     Validate AvaUI DSL syntax
    version               Show version information
    help                  Show this help message

OPTIONS (for generate):
    -i, --input <file>    Input DSL file (required)
    -o, --output <file>   Output code file (optional)
    -p, --platform <p>    Target platform: android|ios|web|desktop
    -l, --language <l>    Output language: kotlin|swift|typescript|javascript

EXAMPLES:
    # Generate Android Compose code
    avacode gen -i screen.json -p android -o Screen.kt

    # Generate SwiftUI code
    avacode gen -i screen.json -p ios -o ScreenView.swift

    # Generate React TypeScript code
    avacode gen -i screen.json -p web -l typescript -o Screen.tsx

    # Validate DSL
    avacode validate screen.json

PLATFORMS:
    android     Generates Kotlin code with Jetpack Compose
    ios         Generates Swift code with SwiftUI
    web         Generates TypeScript/JavaScript with React
    desktop     Generates Kotlin code with Compose Desktop

LANGUAGES:
    kotlin      For Android and Desktop platforms
    swift       For iOS platform
    typescript  For Web platform (default)
    javascript  For Web platform

For more information:
    Documentation: https://docs.ideahq.net/avacode
    Examples: https://github.com/ideahq/avaui-examples
    Issues: https://github.com/ideahq/avaui/issues
        """.trimIndent())
    }

    private fun getFileExtension(language: Language): String {
        return when (language) {
            Language.KOTLIN -> "kt"
            Language.SWIFT -> "swift"
            Language.TYPESCRIPT -> "tsx"
            Language.JAVASCRIPT -> "jsx"
        }
    }
}

/**
 * Main function for CLI execution
 */
fun main(args: Array<String>) {
    val cli = AvaCodeCLIImpl()
    val exitCode = cli.execute(args)
    // Actual implementation would call System.exit(exitCode)
}
