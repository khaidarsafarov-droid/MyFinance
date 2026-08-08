package com.truckerload.domain.model

import com.truckerload.utils.dateStringToEndOfDayMillis
import com.truckerload.utils.parseScheduledTimeToMillis
import java.util.Calendar
import java.util.Locale

/**
 * Manual finish override on [Load.actualFinishDate].
 *
 * Formats:
 * - legacy `"YYYY-MM-DD"` → end of that local day
 * - `"YYYY-MM-DD HH:mm"` → exact local clock (routing / yield)
 */
object ActualFinishDate {
    private val DATE_ONLY = Regex("""^(\d{4}-\d{2}-\d{2})$""")
    private val DATE_TIME = Regex("""^(\d{4}-\d{2}-\d{2})\s+(\d{1,2}):(\d{1,2})""")

    /** Normalize user/input text for Room. Null if blank/invalid. */
    fun normalize(raw: String?): String? {
        val t = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        DATE_TIME.find(t)?.let { m ->
            val date = m.groupValues[1]
            val hour = m.groupValues[2].toInt().coerceIn(0, 23)
            val minute = m.groupValues[3].toInt().coerceIn(0, 59)
            return "%s %02d:%02d".format(Locale.US, date, hour, minute)
        }
        DATE_ONLY.find(t.take(10))?.let { return it.groupValues[1] }
        return null
    }

    fun datePart(raw: String?): String? =
        normalize(raw)?.take(10)

    fun hasTime(raw: String?): Boolean =
        normalize(raw)?.let { it.length > 10 } == true

    /** Hour/minute from a datetime override; null if date-only / invalid. */
    fun timeParts(raw: String?): Pair<Int, Int>? {
        val n = normalize(raw) ?: return null
        DATE_TIME.find(n)?.let { m ->
            return m.groupValues[2].toInt() to m.groupValues[3].toInt()
        }
        return null
    }

    fun combine(dateIso: String, hour: Int, minute: Int): String {
        val date = dateIso.trim().take(10)
        return "%s %02d:%02d".format(
            Locale.US,
            date,
            hour.coerceIn(0, 23),
            minute.coerceIn(0, 59),
        )
    }

    fun combineNow(dateIso: String, nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = nowMillis }
        return combine(dateIso, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    /**
     * Epoch millis for an override string.
     * Date-only → end of local day (legacy). Datetime → exact clock.
     */
    fun toMillis(raw: String?): Long? {
        val n = normalize(raw) ?: return null
        if (n.length > 10) {
            parseScheduledTimeToMillis(n)?.let { return it }
        }
        return dateStringToEndOfDayMillis(n.take(10))
    }

    /** Display label: full datetime when set with time, else date. */
    fun displayLabel(raw: String?): String? = normalize(raw)
}

/** Last DEL stop scheduled datetime as `"YYYY-MM-DD HH:mm"` when parseable. */
fun Load.lastDelDateTimeFromStops(): String? {
    // FIX: align year with load.date for yearless Relay `MM/DD` stop times
    val yearHint = date.take(4).toIntOrNull()
    val best = stops
        .filter { it.type == StopType.DEL }
        .mapNotNull { stop ->
            val ms = parseScheduledTimeToMillis(stop.scheduledTime, yearHint) ?: return@mapNotNull null
            stop to ms
        }
        .maxByOrNull { it.second }
        ?: return lastDelDateFromStops()
    val scheduled = best.first.scheduledTime
    ActualFinishDate.normalize(scheduled)?.let { return it }
    // Relay `08/21 15:00 EDT` is not ISO — rebuild from resolved millis.
    val ms = best.second
    val cal = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = ms }
    return "%04d-%02d-%02d %02d:%02d".format(
        Locale.US,
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.HOUR_OF_DAY),
        cal.get(Calendar.MINUTE),
    )
}
