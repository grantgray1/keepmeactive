package com.keepmeactive

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.concurrent.TimeUnit

object Scheduler {

    const val ACTION_TICK = "com.keepmeactive.TICK"
    private const val REQUEST_CODE = 1001

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).setAction(ACTION_TICK)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    /**
     * Re-arms the alarm for the next due time. Safe to call as often as you like.
     * Uses the inexact `setAndAllowWhileIdle`, which needs no special permission and still
     * fires through Doze. Drifting by an hour on a 21-day schedule does not matter.
     */
    fun reschedule(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context)

        alarmManager.cancel(pi)
        if (!context.enabled) return

        // Never schedule in the past; give the system at least a minute's notice.
        val due = maxOf(
            context.nextAttemptAt(),
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30)
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pi)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, due, pi)
        }
    }
}
