package com.augmentalis.ava.features.actions.handlers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.augmentalis.ava.features.actions.ActionResult
import com.augmentalis.ava.features.actions.IntentActionHandler
import com.augmentalis.ava.features.actions.entities.MessageEntityExtractor
import com.augmentalis.ava.features.actions.entities.PhoneNumberEntityExtractor
import com.augmentalis.ava.features.actions.entities.RecipientEntityExtractor

/**
 * Action handler for sending text messages (SMS) with Intelligent App Resolution.
 *
 * Extracts recipient and optional message from utterance and launches SMS composer.
 * Uses AppResolverService to determine which messaging app to use.
 *
 * Intent: send_text (communication.aot)
 * Utterances: "text mom", "send text to John saying hello", "message dad"
 * Entities: recipient (required), message (optional)
 *
 * Examples:
 * - "text mom" → Opens SMS composer in preferred app
 * - "text John saying I'm running late" → SMS to John with message
 * - "send message to 555-1234 that I'll be there soon" → SMS with message
 *
 * App Resolution (Chapter 71):
 * - If user has set preference → use that app
 * - If only one SMS app → auto-select
 * - If multiple apps → return NeedsResolution for UI to prompt
 *
 * Priority: P0 (Week 1)
 * Effort: 3 hours
 *
 * Author: Manoj Jhawar
 */
class SendTextActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "SendTextHandler"
        const val CAPABILITY = "sms"
    }

    override val intent = "send_text"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Sending text for utterance: '$utterance'")

            // Extract recipient (name or phone number)
            val recipient = RecipientEntityExtractor.extract(utterance)

            if (recipient == null) {
                Log.w(TAG, "Could not extract recipient from: $utterance")
                return ActionResult.Failure("I couldn't figure out who to text. Try: 'text mom' or 'text 555-1234'")
            }

            // Extract optional message
            val message = MessageEntityExtractor.extract(utterance)

            // Get the address
            val address = recipient.phoneNumber ?: recipient.name ?: recipient.email ?: ""

            // Return NeedsResolution with SMS params for ActionsManager to handle
            return ActionResult.NeedsResolution(
                capability = CAPABILITY,
                data = mapOf(
                    "address" to address,
                    "body" to (message ?: "")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process text request", e)
            ActionResult.Failure(
                message = "Failed to process text request: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Execute SMS action with a specific package.
     *
     * Called by ActionsManager after resolving the preferred app.
     *
     * @param context Android context
     * @param packageName The messaging app's package name
     * @param address Recipient address (phone number or contact name)
     * @param body Optional message body
     * @return ActionResult indicating success or failure
     */
    fun executeWithPackage(
        context: Context,
        packageName: String?,
        address: String,
        body: String?
    ): ActionResult {
        return try {
            Log.d(TAG, "Opening SMS to $address with package: $packageName")

            val smsUri = Uri.parse("smsto:$address")
            val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                body?.takeIf { it.isNotBlank() }?.let { putExtra("sms_body", it) }
                packageName?.let { setPackage(it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(smsIntent)

            val responseMessage = if (body?.isNotBlank() == true) {
                "Opening text to $address with message"
            } else {
                "Opening text to $address"
            }

            Log.i(TAG, responseMessage)
            ActionResult.Success(message = responseMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open messaging app", e)
            ActionResult.Failure(
                message = "Failed to open messaging app: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for making phone calls.
 *
 * Extracts phone number or contact name from utterance and launches phone dialer.
 *
 * Intent: make_call (communication.aot)
 * Utterances: "call mom", "phone John", "dial 555-1234"
 * Entities: recipient (required), phone_number (optional)
 *
 * Examples:
 * - "call mom" → Opens dialer with "mom"
 * - "dial 555-1234" → Opens dialer with number ready
 * - "phone John at 555-1234" → Opens dialer with number
 *
 * Priority: P0 (Week 1)
 * Effort: 2 hours
 *
 * Note: Uses ACTION_DIAL (opens dialer) instead of ACTION_CALL (requires CALL_PHONE permission)
 */
class MakeCallActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "MakeCallHandler"
    }

    override val intent = "make_call"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Making call for utterance: '$utterance'")

            // Try to extract phone number first
            val phoneNumber = PhoneNumberEntityExtractor.extract(utterance)

            // If no phone number, try to extract recipient name
            val recipient = if (phoneNumber != null) {
                phoneNumber
            } else {
                RecipientEntityExtractor.extract(utterance)?.let { r ->
                    r.phoneNumber ?: r.name ?: r.email
                }
            }

            if (recipient == null) {
                Log.w(TAG, "Could not extract phone number or contact name from: $utterance")
                return ActionResult.Failure("I couldn't figure out who to call. Try: 'call mom' or 'dial 555-1234'")
            }

            // Build phone intent (ACTION_DIAL doesn't require CALL_PHONE permission)
            val phoneUri = Uri.parse("tel:$recipient")
            val dialIntent = Intent(Intent.ACTION_DIAL, phoneUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(dialIntent)

            Log.i(TAG, "Opened dialer for: $recipient")
            ActionResult.Success(message = "Calling $recipient")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to make call", e)
            ActionResult.Failure(
                message = "Failed to open phone dialer: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for sending emails with Intelligent App Resolution.
 *
 * Extracts recipient email, optional subject and message from utterance.
 * Uses AppResolverService to determine which email app to use.
 *
 * Intent: send_email (communication.aot)
 * Utterances: "email John", "send email to alice@example.com", "email bob about meeting"
 * Entities: recipient (email required), subject (optional), message (optional)
 *
 * Examples:
 * - "email alice@example.com" → Opens email composer in preferred app
 * - "send email to bob@work.com about meeting" → Email with subject
 * - "email john saying I'll be late" → Email with message
 *
 * App Resolution (Chapter 71):
 * - If user has set preference → use that app
 * - If only one email app → auto-select
 * - If multiple apps → return NeedsResolution for UI to prompt
 *
 * Priority: P1 (Week 2)
 * Effort: 4 hours
 *
 * Author: Manoj Jhawar
 */
class SendEmailActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "SendEmailHandler"
        const val CAPABILITY = "email"
    }

    override val intent = "send_email"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Sending email for utterance: '$utterance'")

            // Extract recipient (email address)
            val recipient = RecipientEntityExtractor.extract(utterance)

            if (recipient?.email == null) {
                Log.w(TAG, "Could not extract email address from: $utterance")
                return ActionResult.Failure("I couldn't find an email address. Try: 'email alice@example.com'")
            }

            // Extract optional subject from "about X" pattern
            val subjectPattern = Regex("about (.+?)(?:saying|$)", RegexOption.IGNORE_CASE)
            val subject = subjectPattern.find(utterance)?.groupValues?.getOrNull(1)?.trim()

            // Extract optional message
            val message = MessageEntityExtractor.extract(utterance)

            // Return NeedsResolution with email params for ActionsManager to handle
            // ActionsManager will resolve the app and call executeWithPackage
            return ActionResult.NeedsResolution(
                capability = CAPABILITY,
                data = mapOf(
                    "email" to recipient.email,
                    "subject" to (subject ?: ""),
                    "body" to (message ?: "")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process email request", e)
            ActionResult.Failure(
                message = "Failed to process email request: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Execute email action with a specific package.
     *
     * Called by ActionsManager after resolving the preferred app.
     *
     * @param context Android context
     * @param packageName The email app's package name
     * @param email Recipient email address
     * @param subject Optional email subject
     * @param body Optional email body
     * @return ActionResult indicating success or failure
     */
    fun executeWithPackage(
        context: Context,
        packageName: String?,
        email: String,
        subject: String?,
        body: String?
    ): ActionResult {
        return try {
            Log.d(TAG, "Opening email to $email with package: $packageName")

            val mailto = Uri.parse("mailto:$email")
            val emailIntent = Intent(Intent.ACTION_SENDTO, mailto).apply {
                subject?.takeIf { it.isNotBlank() }?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                body?.takeIf { it.isNotBlank() }?.let { putExtra(Intent.EXTRA_TEXT, it) }
                packageName?.let { setPackage(it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(emailIntent)

            val responseMessage = buildString {
                append("Opening email to $email")
                subject?.takeIf { it.isNotBlank() }?.let { append(" about $it") }
                body?.takeIf { it.isNotBlank() }?.let { append(" with message") }
            }

            Log.i(TAG, responseMessage)
            ActionResult.Success(message = responseMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open email app", e)
            ActionResult.Failure(
                message = "Failed to open email app: ${e.message}",
                exception = e
            )
        }
    }
}
