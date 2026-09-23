package com.keepmeactive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.concurrent.TimeUnit

/** Fires on the schedule. Sends only if a send is genuinely due, then re-arms. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Scheduler.ACTION_TICK) return
        if (!context.enabled) return

        val retryPending = context.retryAt > 0L && System.currentTimeMillis() >= context.retryAt
        if (!context.isDue() && !retryPending) {
            // Woke up early (Doze windows, clock changes). Just re-arm.
            Scheduler.reschedule(context)
            return
        }

        val problem = SmsSender.send(context, manual = false)
        if (problem != null) {
            context.lastResult = problem
            context.retryAt = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(6)
            Notifier.show(context, "Keep-alive text not sent", "$problem Retrying in 6 hours.")
            Scheduler.reschedule(context)
        }
        // On success the radio calls back into SmsResultReceiver, which records it and re-arms.
    }
}
