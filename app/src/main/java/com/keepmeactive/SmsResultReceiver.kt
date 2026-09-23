package com.keepmeactive

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.concurrent.TimeUnit

/** The radio's verdict on the send. This is what decides whether the clock resets. */
class SmsResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SmsSender.ACTION_SMS_SENT) return

        val manual = intent.getBooleanExtra("manual", false)
        val ok = resultCode == Activity.RESULT_OK
        val description = SmsSender.describeResultCode(resultCode)
        val now = System.currentTimeMillis()

        context.lastResult = "$description (${formatTime(now)})"

        if (ok) {
            // A manual test still counts as network activity, so it resets the clock too.
            context.lastSentAt = now
            context.retryAt = 0L
            Notifier.show(
                context,
                if (manual) "Test text sent" else "Keep-alive text sent",
                "Sent to ${context.recipient}. Next one due ${formatTime(context.nextDueAt())}."
            )
        } else {
            context.retryAt = now + TimeUnit.HOURS.toMillis(6)
            Notifier.show(
                context,
                if (manual) "Test text failed" else "Keep-alive text failed",
                "$description. Retrying in 6 hours."
            )
        }

        Scheduler.reschedule(context)
    }
}
