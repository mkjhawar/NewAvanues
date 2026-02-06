package com.augmentalis.voiceoscore.dsl.plugin

import com.augmentalis.voiceoscore.currentTimeMillis
import com.augmentalis.voiceoscore.dsl.ast.AvuDslFileType
import com.augmentalis.voiceoscore.dsl.lexer.AvuDslLexer
import com.augmentalis.voiceoscore.dsl.parser.AvuDslParser
import com.augmentalis.voiceoscore.dsl.registry.CodePermissionMap

/**
 * Loads and validates .avp plugin files.
 *
 * Pipeline: `.avp text → Lex → Parse → Extract manifest → Validate → LoadedPlugin`
 *
 * ## Validation Steps
 * 1. **Parse** — lexer + parser produce AST (reports syntax errors)
 * 2. **Type check** — file must declare `type: plugin`
 * 3. **Manifest extraction** — header metadata → [PluginManifest]
 * 4. **Manifest validation** — required fields present, valid identifiers
 * 5. **Permission check** — declared codes covered by declared permissions
 * 6. **Sandbox assignment** — trust level → [SandboxConfig]
 */
object PluginLoader {

    /**
     * Load a plugin from .avp text content.
     *
     * @param avpContent The raw text of the .avp file
     * @param trustLevel Optional override for trust level (auto-detected if null)
     * @return [PluginLoadResult] with either the loaded plugin or error details
     */
    fun load(avpContent: String, trustLevel: PluginTrustLevel? = null): PluginLoadResult {
        // Step 1: Parse
        val tokens = AvuDslLexer(avpContent).tokenize()
        val parseResult = AvuDslParser(tokens).parse()

        if (parseResult.hasErrors) {
            return PluginLoadResult.ParseError(
                errors = parseResult.errors.map { "${it.line}:${it.column}: ${it.message}" }
            )
        }

        val ast = parseResult.file

        // Step 2: Verify file type
        if (ast.header.type != AvuDslFileType.PLUGIN) {
            return PluginLoadResult.ValidationError(
                errors = listOf("File type must be 'plugin', got '${ast.header.type.name.lowercase()}'")
            )
        }

        // Step 3: Extract manifest
        val manifest = PluginManifest.fromHeader(ast.header)
        val manifestValidation = manifest.validate()
        if (!manifestValidation.isValid) {
            return PluginLoadResult.ValidationError(errors = manifestValidation.errors)
        }

        // Step 4: Validate code permissions
        val codeValidation = CodePermissionMap.validateCodePermissions(
            declaredCodes = manifest.codes.keys,
            grantedPermissions = manifest.permissions
        )
        if (!codeValidation.isValid) {
            val errors = codeValidation.violations.map { v ->
                "Code '${v.code}' requires permissions " +
                    "${v.missingPermissions.map { it.name }} not declared in plugin permissions"
            }
            return PluginLoadResult.PermissionError(errors = errors)
        }

        // Step 5: Determine sandbox config
        val resolvedTrust = trustLevel ?: PluginSandbox.determineTrustLevel(manifest)
        val sandboxConfig = PluginSandbox.configForTrustLevel(resolvedTrust)

        // Step 6: Build LoadedPlugin
        val plugin = LoadedPlugin(
            manifest = manifest,
            ast = ast,
            sandboxConfig = sandboxConfig,
            state = PluginState.VALIDATED,
            loadedAtMs = currentTimeMillis()
        )

        return PluginLoadResult.Success(plugin)
    }
}

/**
 * Result of loading a plugin.
 */
sealed class PluginLoadResult {
    data class Success(val plugin: LoadedPlugin) : PluginLoadResult()
    data class ParseError(val errors: List<String>) : PluginLoadResult()
    data class ValidationError(val errors: List<String>) : PluginLoadResult()
    data class PermissionError(val errors: List<String>) : PluginLoadResult()

    val isSuccess: Boolean get() = this is Success

    fun pluginOrNull(): LoadedPlugin? = (this as? Success)?.plugin
}
