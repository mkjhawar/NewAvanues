package com.augmentalis.nlu.aon

import android.content.Context
import com.augmentalis.ava.core.common.Result
import com.augmentalis.nlu.nluLogWarn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Parser for AVA Ontology Format 2.0 (.aot) files
 *
 * Parses .aot files from assets and converts them to SemanticIntentOntologyData objects
 * for database storage.
 *
 * AVA Ontology Format (.aot):
 * - Extends .ava format with semantic metadata
 * - Supports zero-shot intent classification
 * - Defines multi-step action sequences
 * - Includes capability requirements and entity schemas
 *
 * Example usage:
 * ```kotlin
 * val parser = AonFileParser(context)
 * val result = parser.parseAonFile("assets/ontology/en-US/communication.aot")
 * when (result) {
 *     is Result.Success -> {
 *         val ontologies = result.data.ontologies
 *         // Insert into database
 *     }
 *     is Result.Error -> {
 *         nluLogError(TAG, "Failed to parse: ${result.message}")
 *     }
 * }
 * ```
 */
class AonFileParser(private val context: Context) {

    companion object {
        private const val TAG = "AonFileParser"
        private const val EXPECTED_SCHEMA = "ava-ontology-3.0"
    }

    /**
     * Parse a .aot file from assets
     *
     * @param assetPath Path to .aot file in assets (e.g., "ontology/en-US/communication.aot")
     * @return Result containing parsed ontologies or error
     */
    suspend fun parseAonFile(assetPath: String): Result<AonFile> = withContext(Dispatchers.IO) {
        try {
            // Read file from assets
            val jsonString = context.assets.open(assetPath).bufferedReader().use { it.readText() }

            // Parse JSON
            val jsonObject = JSONObject(jsonString)

            // Validate schema
            val schema = jsonObject.optString("schema", "")
            if (schema != EXPECTED_SCHEMA) {
                return@withContext Result.Error(
                    exception = IllegalArgumentException("Invalid schema: $schema, expected: $EXPECTED_SCHEMA"),
                    message = "Invalid .aot file schema"
                )
            }

            // Parse root fields
            val version = jsonObject.getString("version")
            val locale = jsonObject.getString("locale")
            val metadata = parseMetadata(jsonObject.getJSONObject("metadata"))

            // Parse ontology array
            val ontologyArray = jsonObject.getJSONArray("ontology")
            val ontologies = mutableListOf<SemanticIntentOntologyData>()

            for (i in 0 until ontologyArray.length()) {
                val ontologyJson = ontologyArray.getJSONObject(i)
                val entity = parseOntologyEntry(ontologyJson, locale, assetPath)
                ontologies.add(entity)
            }

            // Parse optional fields
            val globalSynonyms = parseGlobalSynonyms(jsonObject.optJSONObject("global_synonyms"))
            val capabilityMappings = parseCapabilityMappings(jsonObject.optJSONObject("capability_mappings"))

            Result.Success(
                AonFile(
                    schema = schema,
                    version = version,
                    locale = locale,
                    metadata = metadata,
                    ontologies = ontologies,
                    globalSynonyms = globalSynonyms,
                    capabilityMappings = capabilityMappings,
                    sourceFile = assetPath
                )
            )
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to parse .aot file: ${e.message}"
            )
        }
    }

    /**
     * Parse a .aot file from a File object
     */
    suspend fun parseAonFile(file: File): Result<AonFile> = withContext(Dispatchers.IO) {
        try {
            val jsonString = file.readText()
            val jsonObject = JSONObject(jsonString)

            // Similar parsing logic as above...
            val schema = jsonObject.optString("schema", "")
            if (schema != EXPECTED_SCHEMA) {
                return@withContext Result.Error(
                    exception = IllegalArgumentException("Invalid schema"),
                    message = "Invalid .aot file schema"
                )
            }

            val version = jsonObject.getString("version")
            val locale = jsonObject.getString("locale")
            val metadata = parseMetadata(jsonObject.getJSONObject("metadata"))

            val ontologyArray = jsonObject.getJSONArray("ontology")
            val ontologies = mutableListOf<SemanticIntentOntologyData>()

            for (i in 0 until ontologyArray.length()) {
                val ontologyJson = ontologyArray.getJSONObject(i)
                val entity = parseOntologyEntry(ontologyJson, locale, file.absolutePath)
                ontologies.add(entity)
            }

            val globalSynonyms = parseGlobalSynonyms(jsonObject.optJSONObject("global_synonyms"))
            val capabilityMappings = parseCapabilityMappings(jsonObject.optJSONObject("capability_mappings"))

            Result.Success(
                AonFile(
                    schema = schema,
                    version = version,
                    locale = locale,
                    metadata = metadata,
                    ontologies = ontologies,
                    globalSynonyms = globalSynonyms,
                    capabilityMappings = capabilityMappings,
                    sourceFile = file.absolutePath
                )
            )
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to parse .aot file: ${e.message}"
            )
        }
    }

    /**
     * Parse metadata object
     */
    private fun parseMetadata(metadataJson: JSONObject): AonMetadata {
        return AonMetadata(
            filename = metadataJson.getString("filename"),
            category = metadataJson.getString("category"),
            name = metadataJson.getString("name"),
            description = metadataJson.getString("description"),
            ontologyCount = metadataJson.getInt("ontology_count"),
            author = metadataJson.optString("author", ""),
            createdAt = metadataJson.optString("created_at", "")
        )
    }

    /**
     * Parse a single ontology entry
     */
    private fun parseOntologyEntry(
        ontologyJson: JSONObject,
        locale: String,
        sourceFile: String
    ): SemanticIntentOntologyData {
        val intentId = ontologyJson.getString("id")
        val canonicalForm = ontologyJson.getString("canonical_form")
        val description = ontologyJson.getString("description")

        // Parse synonyms array
        val synonymsArray = ontologyJson.getJSONArray("synonyms")
        val synonyms = mutableListOf<String>()
        for (i in 0 until synonymsArray.length()) {
            synonyms.add(synonymsArray.getString(i))
        }

        val actionType = ontologyJson.getString("action_type")

        // Parse action_sequence array
        val actionSequenceArray = ontologyJson.getJSONArray("action_sequence")
        val actionSequence = mutableListOf<String>()
        for (i in 0 until actionSequenceArray.length()) {
            val actionStep = actionSequenceArray.getJSONObject(i)
            actionSequence.add(actionStep.getString("action"))
        }

        // Parse required_capabilities array
        val capabilitiesArray = ontologyJson.getJSONArray("required_capabilities")
        val requiredCapabilities = mutableListOf<String>()
        for (i in 0 until capabilitiesArray.length()) {
            requiredCapabilities.add(capabilitiesArray.getString(i))
        }

        return SemanticIntentOntologyData(
            intentId = intentId,
            locale = locale,
            canonicalForm = canonicalForm,
            description = description,
            synonyms = synonyms,
            actionType = actionType,
            actionSequence = actionSequence,
            requiredCapabilities = requiredCapabilities,
            ontologyFileSource = sourceFile
        )
    }

    /**
     * Parse global synonyms
     */
    private fun parseGlobalSynonyms(globalSynonymsJson: JSONObject?): Map<String, List<String>> {
        if (globalSynonymsJson == null) return emptyMap()

        val synonymMap = mutableMapOf<String, List<String>>()
        val keys = globalSynonymsJson.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            val synonymArray = globalSynonymsJson.getJSONArray(key)
            val synonyms = mutableListOf<String>()

            for (i in 0 until synonymArray.length()) {
                synonyms.add(synonymArray.getString(i))
            }

            synonymMap[key] = synonyms
        }

        return synonymMap
    }

    /**
     * Parse capability mappings
     */
    private fun parseCapabilityMappings(capabilityMappingsJson: JSONObject?): Map<String, CapabilityMapping> {
        if (capabilityMappingsJson == null) return emptyMap()

        val mappings = mutableMapOf<String, CapabilityMapping>()
        val keys = capabilityMappingsJson.keys()

        while (keys.hasNext()) {
            val capabilityType = keys.next()
            val mappingJson = capabilityMappingsJson.getJSONObject(capabilityType)

            // Parse apps array
            val appsArray = mappingJson.getJSONArray("apps")
            val apps = mutableListOf<AppInfo>()

            for (i in 0 until appsArray.length()) {
                val appJson = appsArray.getJSONObject(i)
                val actionsArray = appJson.getJSONArray("actions")
                val actions = mutableListOf<String>()

                for (j in 0 until actionsArray.length()) {
                    actions.add(actionsArray.getString(j))
                }

                apps.add(
                    AppInfo(
                        packageName = appJson.getString("package"),
                        name = appJson.getString("name"),
                        actions = actions
                    )
                )
            }

            mappings[capabilityType] = CapabilityMapping(apps = apps)
        }

        return mappings
    }

    /**
     * Load all .aot files from a directory in assets
     *
     * @param assetDirectory Directory in assets (e.g., "ontology/en-US")
     * @return List of parsed .aot files
     */
    suspend fun loadAllAonFiles(assetDirectory: String): Result<List<AonFile>> = withContext(Dispatchers.IO) {
        try {
            val files = context.assets.list(assetDirectory) ?: emptyArray()
            val aonFiles = files.filter { it.endsWith(".aot") }

            val results = mutableListOf<AonFile>()

            for (filename in aonFiles) {
                val filePath = "$assetDirectory/$filename"
                when (val result = parseAonFile(filePath)) {
                    is Result.Success -> results.add(result.data)
                    is Result.Error -> {
                        nluLogWarn(TAG, "Failed to parse $filePath: ${result.message}")
                    }
                }
            }

            Result.Success(results)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to load .aot files: ${e.message}"
            )
        }
    }
}

/**
 * Parsed .aot file data
 */
data class AonFile(
    val schema: String,
    val version: String,
    val locale: String,
    val metadata: AonMetadata,
    val ontologies: List<SemanticIntentOntologyData>,
    val globalSynonyms: Map<String, List<String>>,
    val capabilityMappings: Map<String, CapabilityMapping>,
    val sourceFile: String
)

/**
 * Metadata from .aot file
 */
data class AonMetadata(
    val filename: String,
    val category: String,
    val name: String,
    val description: String,
    val ontologyCount: Int,
    val author: String,
    val createdAt: String
)

/**
 * Capability mapping for an app type
 */
data class CapabilityMapping(
    val apps: List<AppInfo>
)

/**
 * Information about an app that provides a capability
 */
data class AppInfo(
    val packageName: String,
    val name: String,
    val actions: List<String>
)

/**
 * Data class for semantic intent ontology (replaces Room entity)
 * Used for parsing .aot files before inserting into SQLDelight
 */
data class SemanticIntentOntologyData(
    val intentId: String,
    val locale: String,
    val canonicalForm: String,
    val description: String,
    val synonyms: List<String>,
    val actionType: String,
    val actionSequence: List<String>,
    val requiredCapabilities: List<String>,
    val ontologyFileSource: String? = null
)
