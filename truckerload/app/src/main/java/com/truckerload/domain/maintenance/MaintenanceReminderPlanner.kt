package com.truckerload.domain.maintenance

import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask
import com.truckerload.domain.notifications.QuietHours
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * When a maintenance reminder (oil change and the rest of the Reminders list) should ring,
 * and how Done / Snooze / a missed fire move the next occurrence.
 *
 * Pure: no AlarmManager, no Room. Callers persist [FireState] and arm [Occurrence]s.
 */
object MaintenanceReminderPlanner {

    const val ANCHOR_ID = -1L
    const val REMINDER_HOUR = 8
    const val REMINDER_MINUTE = 0
    const val SNOOZE_MILLIS = 60L * 60L * 1000L
    /** Audible user-visible alarms inside this window use setAlarmClock. */
    const val SOON_WINDOW_MILLIS = 24L * 60L * 60L * 1000L

    private val iso = DateTimeFormatter.ISO_LOCAL_DATE

    enum class Delivery {
        /** SCHEDULE_EXACT_ALARM is granted (or not required on this API). */
        EXACT,
        /** Exact alarms are blocked. Inexact alarm + WorkManager. */
        FALLBACK,
    }

    data class QuietWindow(
        val enabled: Boolean = false,
        val startHour: Int = 22,
        val endHour: Int = 7,
    )

    data class FireState(
        val fingerprint: String = "",
        val lastFiredCycleKey: String? = null,
        val consumedDue: String? = null,
        val snoozeUntilMillis: Long? = null,
        val nextNagAtMillis: Long? = null,
    )

    data class Occurrence(
        val alarmId: Long,
        val taskId: Long,
        val triggerAtMillis: Long,
        val cycleKey: String,
        val title: String,
        /** False for the daily miles check, which must not play a sound by itself. */
        val alerts: Boolean,
        val replay: Boolean = false,
    )

    data class TaskPlan(
        val taskId: Long,
        val state: FireState,
        val updatedTask: MaintenanceTask? = null,
        val notify: Boolean = false,
        val alarm: Occurrence? = null,
    )

    data class Evaluation(
        val plans: List<TaskPlan>,
        val alarms: List<Occurrence>,
    )

    fun delivery(canScheduleExactAlarms: Boolean): Delivery =
        if (canScheduleExactAlarms) Delivery.EXACT else Delivery.FALLBACK

    fun isRecurring(task: MaintenanceTask): Boolean = when (task.reminderType) {
        MaintenanceReminderType.MILES -> (task.intervalMiles ?: 0.0) > 0.0
        MaintenanceReminderType.DATE -> (task.repeatIntervalDays ?: 0) > 0
    }

    fun fingerprint(task: MaintenanceTask): String = listOf(
        task.reminderType.name,
        task.startDate.take(10),
        task.dueDate?.take(10).orEmpty(),
        task.intervalMiles?.toString().orEmpty(),
        task.repeatIntervalDays?.toString().orEmpty(),
        task.isCompleted.toString(),
    ).joinToString("|")

    /** Drop snooze / fired-cycle when the reminder itself was edited. */
    fun effectiveState(task: MaintenanceTask, state: FireState): FireState {
        val fp = fingerprint(task)
        return if (state.fingerprint == fp) state else FireState(fingerprint = fp)
    }

    fun evaluate(
        tasks: List<MaintenanceTask>,
        nowMillis: Long,
        zone: ZoneId,
        milesDue: Map<Long, Boolean>,
        states: Map<Long, FireState>,
        quiet: QuietWindow = QuietWindow(),
        notificationsEnabled: Boolean = true,
    ): Evaluation {
        if (!notificationsEnabled) return Evaluation(emptyList(), emptyList())
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val plans = ArrayList<TaskPlan>()
        var needsAnchor = false
        for (task in tasks) {
            if (task.isCompleted || task.id <= 0L) continue
            val state = effectiveState(task, states[task.id] ?: FireState())
            plans += planTask(
                task = task,
                state = state,
                nowMillis = nowMillis,
                zone = zone,
                milesDue = milesDue[task.id] == true,
                quiet = quiet,
                today = today,
            )
            if (task.reminderType == MaintenanceReminderType.MILES && (task.intervalMiles ?: 0.0) > 0.0) {
                needsAnchor = true
            }
        }
        val alarms = plans.mapNotNull { it.alarm }.toMutableList()
        if (needsAnchor) {
            val morning = nextMorningMillis(nowMillis, zone)
            alarms += Occurrence(
                alarmId = ANCHOR_ID,
                taskId = ANCHOR_ID,
                triggerAtMillis = audibleAt(morning, zone, quiet),
                cycleKey = "anchor:${Instant.ofEpochMilli(morning).atZone(zone).toLocalDate()}",
                title = "",
                alerts = false,
            )
        }
        return Evaluation(plans, alarms)
    }

    /**
     * Ids that were armed and are no longer desired, plus ids whose trigger or cycle changed
     * and must be replaced. Unchanged alarms are left out of [arm] so a sync does not
     * reset a pending exact alarm.
     */
    fun reconcile(
        armed: List<Occurrence>,
        desired: List<Occurrence>,
    ): Reconcile {
        val desiredById = desired.associateBy { it.alarmId }
        val armedById = armed.associateBy { it.alarmId }
        val cancel = armedById.keys.filter { id ->
            val next = desiredById[id] ?: return@filter true
            val prev = armedById[id] ?: return@filter true
            prev.triggerAtMillis != next.triggerAtMillis || prev.cycleKey != next.cycleKey
        }.toSet()
        val arm = desired.filter { next ->
            val prev = armedById[next.alarmId]
            prev == null ||
                prev.triggerAtMillis != next.triggerAtMillis ||
                prev.cycleKey != next.cycleKey
        }
        return Reconcile(cancel, arm)
    }

    data class Reconcile(
        val cancelIds: Set<Long>,
        val arm: List<Occurrence>,
    )

    fun afterSnooze(state: FireState, nowMillis: Long, zone: ZoneId, quiet: QuietWindow): FireState =
        state.copy(snoozeUntilMillis = audibleAt(nowMillis + SNOOZE_MILLIS, zone, quiet))

    fun afterDone(
        task: MaintenanceTask,
        state: FireState,
        today: LocalDate,
        estimatedOdometer: Double?,
        nowMillis: Long,
    ): Pair<MaintenanceTask, FireState> {
        if (task.isCompleted) return task to effectiveState(task, state)
        if (!isRecurring(task)) {
            val completed = task.copy(
                isCompleted = true,
                completedAt = nowMillis,
                notifiedAt = task.notifiedAt ?: nowMillis,
                updatedAt = nowMillis,
            )
            return completed to FireState(fingerprint = fingerprint(completed))
        }
        return when (task.reminderType) {
            MaintenanceReminderType.DATE -> {
                val due = parseDate(task.dueDate) ?: today
                val next = nextDueDate(due, task.repeatIntervalDays ?: 1, today)
                val advanced = task.copy(
                    startDate = today.format(iso),
                    dueDate = next.format(iso),
                    notifiedAt = null,
                    isCompleted = false,
                    completedAt = null,
                    updatedAt = nowMillis,
                )
                advanced to FireState(fingerprint = fingerprint(advanced))
            }
            MaintenanceReminderType.MILES -> {
                val rolledBase = estimatedOdometer
                    ?: ((task.odometerAtStart ?: 0.0) + (task.intervalMiles ?: 0.0))
                val rolled = task.copy(
                    startDate = today.format(iso),
                    odometerAtStart = rolledBase,
                    notifiedAt = null,
                    isCompleted = false,
                    completedAt = null,
                    updatedAt = nowMillis,
                )
                rolled to FireState(fingerprint = fingerprint(rolled))
            }
        }
    }

    fun nextDueDate(currentDue: LocalDate, intervalDays: Int, today: LocalDate): LocalDate {
        val step = intervalDays.coerceAtLeast(1).toLong()
        var next = currentDue.plusDays(step)
        while (!next.isAfter(today)) {
            next = next.plusDays(step)
        }
        return next
    }

    fun atLocalTime(date: LocalDate, zone: ZoneId): Long =
        date.atTime(REMINDER_HOUR, REMINDER_MINUTE).atZone(zone).toInstant().toEpochMilli()

    /** Next 08:00 strictly after [nowMillis]. */
    fun nextMorningMillis(nowMillis: Long, zone: ZoneId): Long {
        val zdt = Instant.ofEpochMilli(nowMillis).atZone(zone)
        var morning = zdt.toLocalDate().atTime(REMINDER_HOUR, REMINDER_MINUTE).atZone(zone)
        if (morning.toInstant().toEpochMilli() <= nowMillis) {
            morning = morning.plusDays(1)
        }
        return morning.toInstant().toEpochMilli()
    }

    /** Push a trigger that lands in quiet hours out to the end of the window so the sound is heard. */
    fun audibleAt(triggerAtMillis: Long, zone: ZoneId, quiet: QuietWindow): Long {
        if (!quiet.enabled) return triggerAtMillis
        var t = triggerAtMillis
        repeat(4) {
            val hour = Instant.ofEpochMilli(t).atZone(zone).hour
            if (!QuietHours.isActive(hour, quiet.startHour, quiet.endHour, true)) return t
            val zdt = Instant.ofEpochMilli(t).atZone(zone)
            var end = zdt.toLocalDate().atTime(quiet.endHour.coerceIn(0, 23), 0).atZone(zone)
            if (!end.toInstant().isAfter(Instant.ofEpochMilli(t))) {
                end = end.plusDays(1)
            }
            t = end.toInstant().toEpochMilli()
        }
        return t
    }

    private fun planTask(
        task: MaintenanceTask,
        state: FireState,
        nowMillis: Long,
        zone: ZoneId,
        milesDue: Boolean,
        quiet: QuietWindow,
        today: LocalDate,
    ): TaskPlan {
        val snooze = state.snoozeUntilMillis
        if (snooze != null && snooze > nowMillis) {
            val whenAt = audibleAt(snooze, zone, quiet)
            return TaskPlan(
                taskId = task.id,
                state = if (whenAt == snooze) state else state.copy(snoozeUntilMillis = whenAt),
                alarm = alarm(task, whenAt, "snooze:${task.id}", alerts = true, replay = true),
            )
        }
        val snoozeDue = snooze != null && snooze <= nowMillis
        if (snoozeDue) {
            val audible = audibleAt(nowMillis, zone, quiet)
            if (audible > nowMillis) {
                return TaskPlan(
                    taskId = task.id,
                    state = state.copy(snoozeUntilMillis = audible),
                    alarm = alarm(task, audible, "snooze:${task.id}", alerts = true, replay = true),
                )
            }
        }
        val cleared = if (snoozeDue) state.copy(snoozeUntilMillis = null) else state
        val planned = when (task.reminderType) {
            MaintenanceReminderType.DATE -> planDate(task, cleared, nowMillis, zone, quiet, today)
            MaintenanceReminderType.MILES -> planMiles(task, cleared, nowMillis, zone, milesDue, quiet)
        }
        return if (snoozeDue) planned.copy(notify = true) else planned
    }

    private fun planDate(
        task: MaintenanceTask,
        state: FireState,
        nowMillis: Long,
        zone: ZoneId,
        quiet: QuietWindow,
        today: LocalDate,
    ): TaskPlan {
        val due = parseDate(task.dueDate) ?: return TaskPlan(task.id, state)
        val cycle = "date:${due.format(iso)}"
        val natural = atLocalTime(due, zone)
        val already = state.lastFiredCycleKey == cycle
        if (!already && natural <= nowMillis) {
            val audible = audibleAt(nowMillis, zone, quiet)
            if (audible > nowMillis) {
                return TaskPlan(
                    taskId = task.id,
                    state = state,
                    alarm = alarm(task, audible, cycle, alerts = true),
                )
            }
            val fired = state.copy(lastFiredCycleKey = cycle, fingerprint = fingerprint(task))
            val nextAlarm = nextDateAlarm(task, due, today, zone, quiet)
            return TaskPlan(
                taskId = task.id,
                state = fired,
                updatedTask = task.copy(notifiedAt = nowMillis, updatedAt = nowMillis),
                notify = true,
                alarm = nextAlarm,
            )
        }
        if (!already) {
            return TaskPlan(
                taskId = task.id,
                state = state.copy(fingerprint = fingerprint(task)),
                alarm = alarm(task, audibleAt(natural, zone, quiet), cycle, alerts = true),
            )
        }
        val nextAlarm = nextDateAlarm(task, due, today, zone, quiet)
        return TaskPlan(task.id, state, alarm = nextAlarm)
    }

    private fun nextDateAlarm(
        task: MaintenanceTask,
        due: LocalDate,
        today: LocalDate,
        zone: ZoneId,
        quiet: QuietWindow,
    ): Occurrence? {
        if (!isRecurring(task)) return null
        val next = nextDueDate(due, task.repeatIntervalDays ?: 1, today)
        val trigger = audibleAt(atLocalTime(next, zone), zone, quiet)
        return alarm(task, trigger, "date:${next.format(iso)}", alerts = true)
    }

    private fun planMiles(
        task: MaintenanceTask,
        state: FireState,
        nowMillis: Long,
        zone: ZoneId,
        milesDue: Boolean,
        quiet: QuietWindow,
    ): TaskPlan {
        if (!milesDue) return TaskPlan(task.id, state.copy(fingerprint = fingerprint(task)))
        val cycle = milesCycle(task)
        val already = state.lastFiredCycleKey == cycle
        if (!already) {
            val audible = audibleAt(nowMillis, zone, quiet)
            if (audible > nowMillis) {
                return TaskPlan(
                    taskId = task.id,
                    state = state,
                    alarm = alarm(task, audible, cycle, alerts = true),
                )
            }
            val nagAt = audibleAt(nextMorningMillis(nowMillis, zone), zone, quiet)
            return TaskPlan(
                taskId = task.id,
                state = state.copy(
                    fingerprint = fingerprint(task),
                    lastFiredCycleKey = cycle,
                    nextNagAtMillis = nagAt,
                ),
                updatedTask = task.copy(notifiedAt = nowMillis, updatedAt = nowMillis),
                notify = true,
                alarm = alarm(task, nagAt, "nag:$cycle", alerts = true),
            )
        }
        val nagAt = state.nextNagAtMillis ?: audibleAt(nextMorningMillis(nowMillis, zone), zone, quiet)
        if (nagAt > nowMillis) {
            return TaskPlan(
                taskId = task.id,
                state = state.copy(nextNagAtMillis = nagAt),
                alarm = alarm(task, nagAt, "nag:$cycle", alerts = true),
            )
        }
        val audible = audibleAt(nowMillis, zone, quiet)
        if (audible > nowMillis) {
            return TaskPlan(
                taskId = task.id,
                state = state.copy(nextNagAtMillis = audible),
                alarm = alarm(task, audible, "nag:$cycle", alerts = true),
            )
        }
        val following = audibleAt(nextMorningMillis(nowMillis, zone), zone, quiet)
        return TaskPlan(
            taskId = task.id,
            state = state.copy(nextNagAtMillis = following),
            updatedTask = task.copy(notifiedAt = nowMillis, updatedAt = nowMillis),
            notify = true,
            alarm = alarm(task, following, "nag:$cycle", alerts = true),
        )
    }

    private fun milesCycle(task: MaintenanceTask): String =
        "miles:${task.startDate.take(10)}:${task.intervalMiles}"

    private fun alarm(
        task: MaintenanceTask,
        triggerAtMillis: Long,
        cycleKey: String,
        alerts: Boolean,
        replay: Boolean = false,
    ): Occurrence = Occurrence(
        alarmId = task.id,
        taskId = task.id,
        triggerAtMillis = triggerAtMillis,
        cycleKey = cycleKey,
        title = task.title,
        alerts = alerts,
        replay = replay,
    )

    private fun parseDate(raw: String?): LocalDate? {
        val text = raw?.trim()?.take(10) ?: return null
        if (text.length < 10) return null
        return runCatching { LocalDate.parse(text, iso) }.getOrNull()
    }
}
