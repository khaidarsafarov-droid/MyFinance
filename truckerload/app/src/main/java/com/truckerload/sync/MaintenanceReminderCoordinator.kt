package com.truckerload.sync

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.truckerload.TruckerLoadApp
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toDomain
import com.truckerload.data.local.toEntity
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.domain.maintenance.MaintenanceMileageUseCase
import com.truckerload.domain.maintenance.MaintenanceReminderPlanner
import com.truckerload.domain.maintenance.MaintenanceReminderPlanner.FireState
import com.truckerload.domain.maintenance.MaintenanceReminderPlanner.Occurrence
import com.truckerload.domain.maintenance.MaintenanceReminderPlanner.QuietWindow
import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask
import com.truckerload.sync.MaintenanceAlarmStore.PendingAction
import com.truckerload.sync.MaintenanceAlarmStore.UserAlarms
import com.truckerload.utils.DirectBoot
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Keeps Room reminders, exact alarms, and the WorkManager safety net on the same schedule.
 * Edits, deletes, backup restore, boot, and clock changes all come through [sync].
 */
object MaintenanceReminderCoordinator {

    private val lock = Any()
    private val executor = Executors.newSingleThreadExecutor()

    fun enqueue(context: Context) {
        val app = context.applicationContext
        executor.execute {
            runCatching { syncBlocking(app) }
                .onFailure { error -> Log.w(TAG, "sync failed", error) }
        }
    }

    suspend fun sync(context: Context) = withContext(Dispatchers.IO) {
        syncBlocking(context.applicationContext)
    }

    fun syncBlocking(context: Context) {
        synchronized(lock) {
            doSync(context.applicationContext)
        }
    }

    fun onBroadcast(context: Context, intent: Intent) {
        val app = context.applicationContext
        val action = intent.action ?: return
        when (action) {
            MaintenanceAlarmScheduler.ACTION_DONE -> onAction(app, intent, PendingAction.Kind.DONE)
            MaintenanceAlarmScheduler.ACTION_SNOOZE -> onAction(app, intent, PendingAction.Kind.SNOOZE)
            MaintenanceAlarmScheduler.ACTION_FIRE -> onFire(app, intent)
            else -> onClockOrBoot(app)
        }
    }

    private fun onClockOrBoot(app: Context) {
        if (!DirectBoot.isUserUnlocked(app)) {
            rearmFuture(app)
            return
        }
        ensureAppStarted(app)
        syncBlocking(app)
    }

    private fun onFire(app: Context, intent: Intent) {
        val userId = intent.getStringExtra(MaintenanceAlarmScheduler.EXTRA_USER_ID).orEmpty()
        val alarmId = intent.getLongExtra(MaintenanceAlarmScheduler.EXTRA_ALARM_ID, Long.MIN_VALUE)
        if (!DirectBoot.isUserUnlocked(app)) {
            synchronized(lock) {
                if (userId.isNotEmpty() && alarmId != Long.MIN_VALUE) {
                    deliverWhileLocked(app, userId, alarmId)
                } else {
                    rearmFuture(app)
                }
            }
            return
        }
        ensureAppStarted(app)
        syncBlocking(app)
    }

    private fun onAction(app: Context, intent: Intent, kind: PendingAction.Kind) {
        val userId = intent.getStringExtra(MaintenanceAlarmScheduler.EXTRA_USER_ID).orEmpty()
        val taskId = intent.getLongExtra(MaintenanceAlarmScheduler.EXTRA_TASK_ID, Long.MIN_VALUE)
        if (userId.isEmpty() || taskId == Long.MIN_VALUE) return
        MaintenanceReminderNotifier.cancel(app, userId, taskId)
        synchronized(lock) {
            val store = MaintenanceAlarmStore(app)
            val current = store.read(userId)
            store.write(
                userId,
                current.copy(
                    pending = current.pending + PendingAction(
                        taskId = taskId,
                        kind = kind,
                        atMillis = System.currentTimeMillis(),
                    ),
                ),
            )
            if (!DirectBoot.isUserUnlocked(app)) {
                if (kind == PendingAction.Kind.SNOOZE) {
                    applyLockedSnooze(app, userId, taskId)
                } else {
                    MaintenanceAlarmScheduler.cancel(app, userId, taskId)
                }
                return
            }
        }
        ensureAppStarted(app)
        syncBlocking(app)
    }

    private fun doSync(app: Context) {
        if (!DirectBoot.isUserUnlocked(app)) {
            rearmFuture(app)
            return
        }
        val userId = runCatching { AuthStore(app).currentUserIdOrNull() }.getOrNull() ?: return
        val settings = SettingsDataStore(app)
        val notificationsOn = runBlocking { settings.getNotifyMaintenanceOnce() }
        val quiet = runBlocking {
            QuietWindow(
                enabled = settings.getQuietHoursEnabledOnce(),
                startHour = settings.getQuietHoursStartOnce(),
                endHour = settings.getQuietHoursEndOnce(),
            )
        }
        val store = MaintenanceAlarmStore(app)
        val saved = store.read(userId)
        if (!notificationsOn) {
            cancelAlarms(app, userId, saved.alarms)
            store.write(userId, saved.copy(alarms = emptyList(), pending = emptyList()))
            MaintenanceAlarmScheduler.ensureSafetyNet(app)
            return
        }
        val db = runCatching { AppDatabase.getInstance(app, userId) }.getOrElse { error ->
            Log.w(TAG, "database unavailable", error)
            rearmFuture(app)
            return
        }
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        var tasks = runBlocking { db.maintenanceDao().getAllTasksOnce() }.map { it.toDomain() }
        var states = saved.states.toMutableMap()
        for (action in saved.pending) {
            val task = tasks.find { it.id == action.taskId } ?: continue
            val state = MaintenanceReminderPlanner.effectiveState(
                task,
                states[task.id] ?: FireState(),
            )
            if (action.kind == PendingAction.Kind.SNOOZE) {
                states[task.id] = MaintenanceReminderPlanner.afterSnooze(state, action.atMillis, zone, quiet)
            } else {
                val loads = loadInputs(db)
                val estimated = MaintenanceMileageUseCase.progressForTask(task, loads, today).estimatedOdometer
                val (updated, nextState) = MaintenanceReminderPlanner.afterDone(
                    task = task,
                    state = state,
                    today = Instant.ofEpochMilli(action.atMillis).atZone(zone).toLocalDate(),
                    estimatedOdometer = estimated,
                    nowMillis = action.atMillis,
                )
                runBlocking { db.maintenanceDao().updateTask(updated.toEntity()) }
                tasks = tasks.map { if (it.id == updated.id) updated else it }
                states[task.id] = nextState
            }
        }
        val loads = loadInputs(db)
        val milesDue = tasks.filter { it.reminderType == MaintenanceReminderType.MILES }
            .associate { task ->
                task.id to MaintenanceMileageUseCase.progressForTask(task, loads, today).isDue
            }
        val evaluation = MaintenanceReminderPlanner.evaluate(
            tasks = tasks,
            nowMillis = now,
            zone = zone,
            milesDue = milesDue,
            states = states,
            quiet = quiet,
            notificationsEnabled = true,
        )
        val canPost = MaintenanceReminderNotifier.canPost(app)
        val desired = ArrayList<Occurrence>()
        val titles = tasks.associate { it.id to it.title }
        for (plan in evaluation.plans) {
            if (plan.notify && !canPost) {
                saved.alarm(plan.taskId)?.let { desired += it }
                continue
            }
            val title = plan.updatedTask?.title ?: titles[plan.taskId].orEmpty()
            if (plan.notify && title.isNotBlank()) {
                val posted = MaintenanceReminderNotifier.show(app, userId, plan.taskId, title)
                if (!posted) {
                    saved.alarm(plan.taskId)?.let { desired += it }
                    continue
                }
            }
            states[plan.taskId] = plan.state
            plan.updatedTask?.let { updated ->
                runBlocking { db.maintenanceDao().updateTask(updated.toEntity()) }
            }
            plan.alarm?.let { desired += it }
        }
        evaluation.alarms.find { it.alarmId == MaintenanceReminderPlanner.ANCHOR_ID }?.let { desired += it }
        val liveIds = tasks.map { it.id }.toSet()
        states.keys.retainAll(liveIds)
        val reconcile = MaintenanceReminderPlanner.reconcile(saved.alarms, desired)
        reconcile.cancelIds.forEach { id ->
            MaintenanceAlarmScheduler.cancel(app, userId, id)
        }
        for (old in saved.alarms) {
            if (old.alarmId == MaintenanceReminderPlanner.ANCHOR_ID) continue
            val task = tasks.find { it.id == old.taskId }
            if (task == null || task.isCompleted) {
                MaintenanceReminderNotifier.cancel(app, userId, old.taskId)
            }
        }
        reconcile.arm.forEach { occurrence ->
            MaintenanceAlarmScheduler.arm(app, userId, occurrence)
        }
        store.write(
            userId,
            UserAlarms(states = states, alarms = desired, pending = emptyList()),
        )
        MaintenanceAlarmScheduler.ensureSafetyNet(app)
    }

    private fun cancelAlarms(app: Context, userId: String, alarms: List<Occurrence>) {
        alarms.forEach { occurrence ->
            MaintenanceAlarmScheduler.cancel(app, userId, occurrence.alarmId, occurrence.taskId)
        }
    }

    private fun rearmFuture(app: Context) {
        val now = System.currentTimeMillis()
        MaintenanceAlarmStore(app).allAlarms().forEach { (userId, occurrence) ->
            if (occurrence.triggerAtMillis > now) {
                MaintenanceAlarmScheduler.arm(app, userId, occurrence)
            }
        }
    }

    private fun deliverWhileLocked(app: Context, userId: String, alarmId: Long) {
        val store = MaintenanceAlarmStore(app)
        val saved = store.read(userId)
        val occurrence = saved.alarms.find { it.alarmId == alarmId } ?: return
        if (!occurrence.alerts) return
        val state = saved.states[occurrence.taskId] ?: FireState()
        if (state.lastFiredCycleKey == occurrence.cycleKey) return
        if (occurrence.title.isNotBlank()) {
            MaintenanceReminderNotifier.show(app, userId, occurrence.taskId, occurrence.title)
        }
        val states = saved.states.toMutableMap()
        states[occurrence.taskId] = state.copy(lastFiredCycleKey = occurrence.cycleKey)
        val remaining = saved.alarms.filter { it.alarmId != alarmId || it.triggerAtMillis > System.currentTimeMillis() }
        store.write(userId, saved.copy(states = states, alarms = remaining))
        MaintenanceAlarmScheduler.cancel(app, userId, alarmId, occurrence.taskId)
    }

    private fun applyLockedSnooze(app: Context, userId: String, taskId: Long) {
        val store = MaintenanceAlarmStore(app)
        val saved = store.read(userId)
        val state = saved.states[taskId] ?: FireState()
        val zone = ZoneId.systemDefault()
        val snoozed = MaintenanceReminderPlanner.afterSnooze(
            state,
            System.currentTimeMillis(),
            zone,
            QuietWindow(),
        )
        val until = snoozed.snoozeUntilMillis ?: return
        val previous = saved.alarms.find { it.taskId == taskId }
        val occurrence = Occurrence(
            alarmId = taskId,
            taskId = taskId,
            triggerAtMillis = until,
            cycleKey = "snooze:$taskId",
            title = previous?.title.orEmpty(),
            alerts = true,
            replay = true,
        )
        val alarms = saved.alarms.filter { it.alarmId != taskId } + occurrence
        val states = saved.states.toMutableMap()
        states[taskId] = snoozed
        store.write(
            userId,
            saved.copy(
                states = states,
                alarms = alarms,
                pending = saved.pending.filterNot {
                    it.taskId == taskId && it.kind == PendingAction.Kind.SNOOZE
                },
            ),
        )
        MaintenanceAlarmScheduler.arm(app, userId, occurrence)
    }

    private fun loadInputs(db: AppDatabase): List<MaintenanceMileageUseCase.LoadInput> =
        runBlocking { db.loadDao().getAllLoadsOnce() }.map { entity ->
            MaintenanceMileageUseCase.LoadInput(
                tripId = entity.tripId,
                id = entity.id,
                miles = entity.totalMiles,
                date = entity.date,
                actualFinishDate = entity.actualFinishDate,
                lastDelMillis = entity.lastDelMillis,
            )
        }

    private fun ensureAppStarted(app: Context) {
        val application = app as? TruckerLoadApp ?: return
        if (application.isUnlockStartDone()) return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            application.startAfterUnlock()
            return
        }
        val latch = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post {
            try {
                application.startAfterUnlock()
            } finally {
                latch.countDown()
            }
        }
        latch.await(15, TimeUnit.SECONDS)
    }

    private const val TAG = "MaintReminder"
}
