package com.augmentalis.magicui.components.voicerouter

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.augmentalis.magicui.components.argscanner.*

/**
 * Android implementation of Intent Router
 */
actual class IntentRouter actual constructor(
    private val registry: ARGRegistry
) {

    private var packageManager: PackageManager? = null

    /**
     * Set PackageManager for intent resolution
     * Must be called before using canHandleIntent() or getHandlers()
     */
    fun setPackageManager(pm: PackageManager) {
        this.packageManager = pm
    }

    actual fun createIntent(match: VoiceCommandMatch): Any {
        return createIntent(match.app.packageName, match.capability, match.parameters)
    }

    actual fun createIntent(
        packageName: String,
        capability: Capability,
        parameters: Map<String, String>
    ): Any {
        // Create custom action based on capability ID
        val action = "com.augmentalis.voiceos.action.${capability.id.uppercase()}"

        val intent = Intent(action).apply {
            setPackage(packageName)

            // Add parameters as extras
            parameters.forEach { (key, value) ->
                putExtra("param_$key", value)
            }

            // Add capability metadata
            putExtra("capability_id", capability.id)
            putExtra("capability_name", capability.name)
            putExtra("capability_type", capability.type.name)

            // Add common flags
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Try to enhance with standard Android actions
        enhanceWithStandardActions(intent, capability, parameters)

        return intent
    }

    actual fun canHandleIntent(intent: Any): Boolean {
        if (intent !is Intent) return false
        val pm = packageManager ?: return false

        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo != null
    }

    actual fun getHandlers(intent: Any): List<String> {
        if (intent !is Intent) return emptyList()
        val pm = packageManager ?: return emptyList()

        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfos.map { it.activityInfo.packageName }
    }

    /**
     * Enhance intent with standard Android actions when applicable
     */
    private fun enhanceWithStandardActions(
        intent: Intent,
        capability: Capability,
        parameters: Map<String, String>
    ) {
        when {
            // Web browsing
            capability.id.contains("browse") || capability.id.contains("web") -> {
                parameters["url"]?.let { url ->
                    intent.action = Intent.ACTION_VIEW
                    intent.data = Uri.parse(if (url.startsWith("http")) url else "https://$url")
                }
            }

            // Search
            capability.id.contains("search") -> {
                parameters["query"]?.let { query ->
                    intent.action = Intent.ACTION_WEB_SEARCH
                    intent.putExtra("query", query)
                }
            }

            // Dial/Call
            capability.id.contains("dial") || capability.id.contains("call") -> {
                parameters["number"]?.let { number ->
                    intent.action = Intent.ACTION_DIAL
                    intent.data = Uri.parse("tel:$number")
                }
            }

            // Send/Share
            capability.id.contains("send") || capability.id.contains("share") -> {
                intent.action = Intent.ACTION_SEND
                parameters["text"]?.let { text ->
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, text)
                }
            }

            // View file
            capability.id.contains("view") || capability.id.contains("open") -> {
                parameters["file"]?.let { file ->
                    intent.action = Intent.ACTION_VIEW
                    intent.data = Uri.parse(file)
                }
            }
        }
    }

    /**
     * Create intent from capability's registered intent filters
     */
    fun createIntentFromFilter(
        packageName: String,
        filter: IntentFilter,
        data: String? = null
    ): Intent {
        return Intent(filter.action).apply {
            setPackage(packageName)

            // Add categories
            filter.categories.forEach { category ->
                addCategory(category)
            }

            // Set data if provided
            data?.let { dataStr ->
                this.data = Uri.parse(dataStr)
            }

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Create intent for AIDL service binding
     */
    fun createServiceIntent(
        packageName: String,
        service: ServiceEndpoint
    ): Intent {
        return Intent().apply {
            setPackage(packageName)
            action = service.aidlInterface
        }
    }
}

/**
 * Helper extension to check if Intent has a handler
 */
fun Intent.hasHandler(pm: PackageManager): Boolean {
    val resolveInfo = pm.resolveActivity(this, PackageManager.MATCH_DEFAULT_ONLY)
    return resolveInfo != null
}

/**
 * Helper extension to get all handlers for an Intent
 */
fun Intent.getHandlers(pm: PackageManager): List<String> {
    val resolveInfos = pm.queryIntentActivities(this, PackageManager.MATCH_DEFAULT_ONLY)
    return resolveInfos.map { it.activityInfo.packageName }
}
