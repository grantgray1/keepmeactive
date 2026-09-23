package com.keepmeactive

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * All settings and state live here: one small SharedPreferences file, no database.
 * Declared as top-level extensions so everything in the package can just say `context.recipient`.
 */

private const val PREFS_FILE = "keep_me_active"

private const val K_RECIPIENT = "recipient"
private const val K_MESSAGE = "message"
private const val K_INTERVAL_DAYS = "interval_days"
private const val K_ENABLED = "enabled"
private const val K_SUB_ID = "sub_id"
private const val K_LAST_SENT_AT = "last_sent_at"
private const val K_LAST_RESULT = "last_result"

/** "Send from whichever SIM the system treats as default." */
const val NO_SUB_ID = -1

private fun Context.prefs() = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

var Context.recipient: String
    get() = prefs().getString(K_RECIPIENT, "") ?: ""
    set(v) = prefs().edit().putString(K_RECIPIENT, v.trim()).apply()

var Context.message: String
    get() = prefs().getString(K_MESSAGE, "Keeping this number active.") ?: ""
    set(v) = prefs().edit().putString(K_MESSAGE, v).apply()

var Context.intervalDays: Int
    get() = prefs().getInt(K_INTERVAL_DAYS, 21)
    set(v) = prefs().edit().putInt(K_INTERVAL_DAYS, v.coerceIn(1, 180)).apply()

var Context.enabled: Boolean
    get() = prefs().getBoolean(K_ENABLED, false)
    set(v) = prefs().edit().putBoolean(K_ENABLED, v).apply()

var Context.subId: Int
    get() = prefs().getInt(K_SUB_ID, NO_SUB_ID)
    set(v) = prefs().edit().putInt(K_SUB_ID, v).apply()

var Context.lastSentAt: Long
    get() = prefs().getLong(K_LAST_SENT_AT, 0L)
    set(v) = prefs().edit().putLong(K_LAST_SENT_AT, v).apply()

var Context.lastResult: String
    get() = prefs().getString(K_LAST_RESULT, "") ?: ""
    set(v) = prefs().edit().putString(K_LAST_RESULT, v).apply()

/** When the next send is due. Never sent before means "a minute from now". */
fun Context.nextDueAt(): Long {
    val last = lastSentAt
    if (last == 0L) return System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1)
    return last + TimeUnit.DAYS.toMillis(intervalDays.toLong())
}

fun Context.isDue(): Boolean = System.currentTimeMillis() >= nextDueAt()

fun formatTime(millis: Long): String =
    if (millis <= 0L) "never"
    else SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))

/**
 * Set after a failed attempt so we try again in a few hours instead of waiting out the
 * whole interval. Zero (or any past time) means "no pending retry".
 */
var Context.retryAt: Long
    get() = prefs().getLong(K_RETRY_AT, 0L)
    set(v) = prefs().edit().putLong(K_RETRY_AT, v).apply()

private const val K_RETRY_AT = "retry_at"

/** The moment the alarm should next fire: a pending retry if there is one, else the interval. */
fun Context.nextAttemptAt(): Long {
    val retry = retryAt
    return if (retry > System.currentTimeMillis()) retry else nextDueAt()
}
