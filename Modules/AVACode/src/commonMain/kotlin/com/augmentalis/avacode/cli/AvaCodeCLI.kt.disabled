package com.augmentalis.avacode.cli

import com.augmentalis.avacode.dsl.VosParser
import com.augmentalis.avacode.generators.*

/**
 * AvaCode CLI - Command-line interface for AvaUI code generation
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class AvaCodeCLI {

    private val parser = VosParser()

    /**
     * Main entry point for CLI
     */
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
            // Read input file (simplified - would use actual file I/O)
            val dslContent = readFile(inputFile)

            // Parse DSL
            val screenResult = parser.parseScreen(dslContent)
            if (screenResult.isFailure) {
                println("Parse error: ${screenResult.exceptionOrNull()?.message}")
                return 1
            }

            val screen = screenResult.getOrThrow()

            // Generate code
            val generator = CodeGeneratorFactory.create(platform, language)
            val generated = generator.generate(screen)

            // Write output
            val output = outputFile ?: "${screen.name}.${getFileExtension(generated.language)}"
            writeFile(output, generated.code)

            println("✅ Generated ${generated.language} code for ${generated.platform}")
            println("   Output: $output")
            println("   Lines: ${generated.code.lines().size}")

            return 0

        } catch (e: Exception) {
            println("Error: ${e.message}")
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
            val dslContent = readFile(inputFile)
            val result = parser.validate(dslContent)

            if (result.isValid) {
                println("✅ Validation passed")
                if (result.warnings.isNotEmpty()) {
                    println("\n⚠️  Warnings:")
                    result.warnings.forEach { warning ->
                        println("   Line ${warning.line}: ${warning.message}")
                    }
                }
                return 0
            } else {
                println("❌ Validation failed")
                println("\nErrors:")
                result.errors.forEach { error ->
                    println("   Line ${error.line}: ${error.message}")
                }
                return 1
            }

        } catch (e: Exception) {
            println("Error: ${e.message}")
            return 1
        }
    }

    private fun handleVersion(): Int {
        println("AvaCode CLI v1.0.0")
        println("AvaUI DSL Code Generator")
        println("Part of IDEACODE 5.0 Framework")
        println()
        println("Supported platforms: Android, iOS, Web, Desktop")
        println("Supported languages: Kotlin, Swift, TypeScript, JavaScript")
        return 0
    }

    private fun printHelp() {
        println("""
            AvaCode - AvaUI DSL Code Generator

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

            For more information, visit: https://docs.ideahq.net/avacode
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

    // Placeholder file I/O methods (would be implemented platform-specifically)
    private fun readFile(path: String): String {
        // Production implementation would read actual file
        return "{}"
    }

    private fun writeFile(path: String, content: String) {
        // Production implementation would write actual file
        println("Would write to: $path")
    }
}

/**
 * Main function for CLI execution
 */
fun main(args: Array<String>) {
    val cli = AvaCodeCLI()
    val exitCode = cli.execute(args)
    // System.exit(exitCode) // Uncomment for actual CLI usage
}
