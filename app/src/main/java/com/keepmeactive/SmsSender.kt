package com.keepmeactive

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

object SmsSender {

    const val ACTION_SMS_SENT = "com.keepmeactive.SMS_SENT"
    private const val REQUEST_CODE = 2002

    fun hasSmsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Sends the configured message now. Returns null on success (handed to the radio),
     * or a human-readable reason it could not even be attempted.
     *
     * Delivery is confirmed asynchronously by [SmsResultReceiver]; `lastSentAt` is only
     * written once the radio reports the send actually succeeded.
     */
    fun send(context: Context, manual: Boolean): String? {
        val number = context.recipient
        if (number.isBlank()) return "No recipient number set."
        if (!hasSmsPermission(context)) return "SMS permission not granted."

        val body = context.message.ifBlank { "Keeping this number active." }

        val smsManager = resolveSmsManager(context, context.subId)
            ?: return "Could not access the SMS service."

        val sentIntent = Intent(ACTION_SMS_SENT)
            .setPackage(context.packageName)
            .putExtra("manual", manual)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        val sentPendingIntent =
            PendingIntent.getBroadcast(context, REQUEST_CODE, sentIntent, flags)

        return try {
            // Keep the body short enough to be a single segment; no multipart needed.
            smsManager.sendTextMessage(number, null, body, sentPendingIntent, null)
            null
        } catch (e: Exception) {
            "Send failed: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    private fun resolveSmsManager(context: Context, subId: Int): SmsManager? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val base = context.getSystemService(SmsManager::class.java)
            if (subId != NO_SUB_ID) base.createForSubscriptionId(subId) else base
        } else {
            @Suppress("DEPRECATION")
            if (subId != NO_SUB_ID) SmsManager.getSmsManagerForSubscriptionId(subId)
            else SmsManager.getDefault()
        }
    } catch (_: Exception) {
        null
    }

    fun describeResultCode(code: Int): String = when (code) {
        android.app.Activity.RESULT_OK -> "Sent"
        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Failed: generic radio error"
        SmsManager.RESULT_ERROR_NO_SERVICE -> "Failed: no network service"
        SmsManager.RESULT_ERROR_NULL_PDU -> "Failed: null PDU"
        SmsManager.RESULT_ERROR_RADIO_OFF -> "Failed: radio off (flight mode?)"
        else -> "Failed: error code $code"
    }
}
