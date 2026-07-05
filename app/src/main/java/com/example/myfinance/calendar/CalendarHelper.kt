package com.example.myfinance.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val CALENDAR_NAME = "Мои Рейсы"

object CalendarHelper {

    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get or create "Мои Рейсы" calendar. Falls back to first writable device calendar.
     */
    fun getOrCreateCalendar(context: Context): Long? {
        if (!hasPermission(context)) return null
        val resolver = context.contentResolver

        // 1. Find existing "Мои Рейсы" calendar
        val projection = arrayOf(CalendarContract.Calendars._ID)
        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} = ?",
            arrayOf(CALENDAR_NAME),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }

        // 2. Try to create "Мои Рейсы" using existing account
        val calProjection = arrayOf(
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )
        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            calProjection,
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
            arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val accountName = cursor.getString(0) ?: return@use
                val accountType = cursor.getString(1) ?: return@use
                val values = ContentValues().apply {
                    put(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                    put(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
                    put(CalendarContract.Calendars.NAME, CALENDAR_NAME)
                    put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDAR_NAME)
                    put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_EDITOR)
                    put(CalendarContract.Calendars.SYNC_EVENTS, 1)
                    put(CalendarContract.Calendars.VISIBLE, 1)
                }
                val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
                    .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
                    .build()
                resolver.insert(uri, values)?.let { return ContentUris.parseId(it) }
            }
        }

        // 3. Fallback: use first writable device calendar (integrates with Google Calendar, Samsung Calendar, etc.)
        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND ${CalendarContract.Calendars.SYNC_EVENTS} = 1",
            arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        // Last resort: any writable calendar
        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
            arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        return null
    }

    /**
     * Create event in device calendar. Event appears in Google Calendar, Samsung Calendar, etc.
     */
    fun createEvent(context: Context, trip: com.example.myfinance.data.Trip): String? {
        val calendarId = getOrCreateCalendar(context) ?: return null
        val (startMillis, endMillis) = parseTripTimes(trip)

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, "${trip.pointA} → ${trip.pointB}")
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.DESCRIPTION, buildDescription(trip))
            put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
            put(CalendarContract.Events.VISIBLE, 1)
        }
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        val eventId = uri?.lastPathSegment
        if (eventId != null) {
            // Add reminder: 1 day before
            addReminder(context, eventId.toLongOrNull() ?: return eventId, 24 * 60) // minutes
        }
        return eventId
    }

    private fun addReminder(context: Context, eventId: Long, minutes: Int) {
        try {
            val values = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, minutes)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
        } catch (_: Exception) { /* ignore */ }
    }

    /**
     * Update existing event.
     */
    fun updateEvent(context: Context, eventId: String, trip: com.example.myfinance.data.Trip): Boolean {
        if (!hasPermission(context)) return false
        val (startMillis, endMillis) = parseTripTimes(trip)
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, "${trip.pointA} → ${trip.pointB}")
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.DESCRIPTION, buildDescription(trip))
        }
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId.toLongOrNull() ?: return false)
        return context.contentResolver.update(uri, values, null, null) > 0
    }

    /**
     * Delete event.
     */
    fun deleteEvent(context: Context, eventId: String): Boolean {
        if (!hasPermission(context)) return false
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId.toLongOrNull() ?: return false)
        return context.contentResolver.delete(uri, null, null) > 0
    }

    private fun parseTripTimes(trip: com.example.myfinance.data.Trip): Pair<Long, Long> {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        try {
            val d = df.parse(trip.date.take(10))
            if (d != null) cal.time = d
        } catch (_: Exception) {}
        cal.set(Calendar.HOUR_OF_DAY, 8)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMillis = cal.timeInMillis
        cal.add(Calendar.HOUR, 2) // default 2h duration
        val endMillis = cal.timeInMillis
        return startMillis to endMillis
    }

    private fun buildDescription(trip: com.example.myfinance.data.Trip): String {
        val parts = mutableListOf<String>()
        if (trip.miles > 0) parts.add("${trip.miles} mi")
        if (trip.cost > 0) parts.add("$${String.format(Locale.US, "%,.2f", trip.cost)}")
        if (trip.orderNumber.isNotBlank() && trip.orderNumber != "—") parts.add("Order: ${trip.orderNumber}")
        return parts.joinToString(" • ")
    }
}
