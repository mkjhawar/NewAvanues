package com.augmentalis.intentactions.actions

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.augmentalis.intentactions.EntityType
import com.augmentalis.intentactions.ExtractedEntities
import com.augmentalis.intentactions.IIntentAction
import com.augmentalis.intentactions.IntentCategory
import com.augmentalis.intentactions.IntentResult
import com.augmentalis.intentactions.PlatformContext
import com.augmentalis.intentactions.UriSanitizer

/**
 * Sends an email using the device's mail app.
 *
 * Extracts recipient email and optional subject/body from entities.
 * Falls back to utterance-based extraction if entities are not pre-populated.
 */
object SendEmailAction : IIntentAction {
    private const val TAG = "SendEmailAction"

    override val intentId = "send_email"
    override val category = IntentCategory.COMMUNICATION
    override val requiredEntities = listOf(EntityType.RECIPIENT)

    override suspend fun execute(platformCtx: PlatformContext, entities: ExtractedEntities): IntentResult {
        val context = platformCtx.android
        return try {
            Log.d(TAG, "Sending email ${entities.toSafeString()}")

            val emailAddress = entities.recipientEmail
            if (emailAddress.isNullOrBlank()) {
                return IntentResult.NeedsMoreInfo(
                    missingEntity = EntityType.RECIPIENT,
                    prompt = "Who would you like to email? Please provide an email address."
                )
            }

            if (!UriSanitizer.isValidEmail(emailAddress)) {
                return IntentResult.Failed(reason = "Invalid email address")
            }

            val subject = entities.query // reuse query for subject context
            val body = entities.message

            val mailto = Uri.parse("mailto:$emailAddress")
            val emailIntent = Intent(Intent.ACTION_SENDTO, mailto).apply {
                subject?.takeIf { it.isNotBlank() }?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                body?.takeIf { it.isNotBlank() }?.let { putExtra(Intent.EXTRA_TEXT, it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(emailIntent)

            val responseMessage = buildString {
                append("Opening email to $emailAddress")
                subject?.takeIf { it.isNotBlank() }?.let { append(" about $it") }
                body?.takeIf { it.isNotBlank() }?.let { append(" with message") }
            }

            Log.i(TAG, responseMessage)
            IntentResult.Success(message = responseMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send email", e)
            IntentResult.Failed(
                reason = "Failed to open email app: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Sends a text message (SMS) using the device's messaging app.
 *
 * Extracts recipient phone number or contact name and optional message body.
 */
object SendTextAction : IIntentAction {
    private const val TAG = "SendTextAction"

    override val intentId = "send_text"
    override val category = IntentCategory.COMMUNICATION
    override val requiredEntities = listOf(EntityType.RECIPIENT)

    override suspend fun execute(platformCtx: PlatformContext, entities: ExtractedEntities): IntentResult {
        val context = platformCtx.android
        return try {
            Log.d(TAG, "Sending text ${entities.toSafeString()}")

            val rawAddress = entities.phoneNumber ?: entities.recipientName
            if (rawAddress.isNullOrBlank()) {
                return IntentResult.NeedsMoreInfo(
                    missingEntity = EntityType.RECIPIENT,
                    prompt = "Who would you like to text? Say a name or phone number."
                )
            }

            val address = UriSanitizer.sanitizeSmsAddress(rawAddress)
                ?: return IntentResult.Failed(reason = "Invalid SMS address")

            val body = entities.message

            val smsUri = Uri.parse("smsto:$address")
            val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                body?.takeIf { it.isNotBlank() }?.let { putExtra("sms_body", it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(smsIntent)

            val responseMessage = if (body?.isNotBlank() == true) {
                "Opening text to $address with message"
            } else {
                "Opening text to $address"
            }

            Log.i(TAG, responseMessage)
            IntentResult.Success(message = responseMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send text", e)
            IntentResult.Failed(
                reason = "Failed to open messaging app: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Makes a phone call using the device's dialer.
 *
 * Uses ACTION_DIAL (opens dialer UI) rather than ACTION_CALL (which requires CALL_PHONE permission).
 */
object MakeCallAction : IIntentAction {
    private const val TAG = "MakeCallAction"

    override val intentId = "make_call"
    override val category = IntentCategory.COMMUNICATION
    override val requiredEntities = listOf(EntityType.RECIPIENT)

    override suspend fun execute(platformCtx: PlatformContext, entities: ExtractedEntities): IntentResult {
        val context = platformCtx.android
        return try {
            Log.d(TAG, "Making call ${entities.toSafeString()}")

            val rawRecipient = entities.phoneNumber ?: entities.recipientName
            if (rawRecipient.isNullOrBlank()) {
                return IntentResult.NeedsMoreInfo(
                    missingEntity = EntityType.RECIPIENT,
                    prompt = "Who would you like to call? Say a name or phone number."
                )
            }

            val recipient = UriSanitizer.sanitizePhoneNumber(rawRecipient)
                ?: return IntentResult.Failed(reason = "Invalid phone number")

            val phoneUri = Uri.parse("tel:$recipient")
            val dialIntent = Intent(Intent.ACTION_DIAL, phoneUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(dialIntent)

            Log.i(TAG, "Opened dialer for: $recipient")
            IntentResult.Success(message = "Calling $recipient")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to make call", e)
            IntentResult.Failed(
                reason = "Failed to open phone dialer: ${e.message}",
                exception = e
            )
        }
    }
}
